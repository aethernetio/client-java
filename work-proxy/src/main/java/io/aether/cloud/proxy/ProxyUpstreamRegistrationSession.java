
package io.aether.cloud.proxy;

import io.aether.api.CryptoUtils;
import io.aether.api.clientserverapi.ClientApiRegSafe;
import io.aether.api.clientserverapi.ClientApiRegSafeStream;
import io.aether.api.clientserverapi.ClientApiRegUnsafe;
import io.aether.api.clientserverapi.FinishResult;
import io.aether.api.clientserverapi.GlobalRegClientApiStream;

import io.aether.api.clientserverapi.RecoveryResult;

import io.aether.api.clientserverapi.RegistrationRootApi;
import io.aether.api.clientserverapi.RegistrationRootApiRemote;
import io.aether.api.clientserverapi.ServerRegistrationApiRemote;
import io.aether.api.common.Cloud;
import io.aether.api.common.CryptoLib;
import io.aether.api.common.KeySymmetric;

import io.aether.api.common.Key;

import io.aether.api.common.PowMethod;
import io.aether.api.common.ServerDescriptor;
import io.aether.api.common.WorkProofDTO;
import io.aether.crypto.AKey;
import io.aether.crypto.CryptoEngine;
import io.aether.crypto.CryptoProviderFactory;
import io.aether.crypto.SignChecker;
import io.aether.logger.Log;
import io.aether.net.fastMeta.FastMetaNet;
import io.aether.net.fastMeta.MetaContext;
import io.aether.utils.futures.ARFuture;

import java.net.URI;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Upstream half of one client registration session. All operations return
 * futures; nothing blocks.
 */
public final class ProxyUpstreamRegistrationSession
        implements ClientApiRegUnsafe {

    private final URI upstreamRegistrationUri;
    private final CryptoLib cryptoLib;
    private final UUID parent;
    private final Collection<SignChecker> rootSigners;

    private volatile MetaContext context;
    private volatile RegistrationRootApiRemote rootApi;
    private volatile ServerRegistrationApiRemote safeApi;


    private final AKey.Symmetric tempKey;
    private final KeySymmetric tempKeyNative;
    private final CryptoEngine tempKeyEngine;


    private volatile CryptoEngine upstreamAsymEngine;
    private volatile WorkProofDTO workProof;
    private final AtomicBoolean closed = new AtomicBoolean();

    public ProxyUpstreamRegistrationSession(
            URI upstreamRegistrationUri,
            CryptoLib cryptoLib,
            UUID parent,
            Collection<SignChecker> rootSigners
    ) {

        this.upstreamRegistrationUri = upstreamRegistrationUri;
        this.cryptoLib = cryptoLib;
        this.parent = parent;
        this.rootSigners = rootSigners;
        this.tempKey = CryptoProviderFactory
                .getProvider(cryptoLib.name())
                .createSymmetricKey();
        this.tempKeyNative = CryptoUtils.of(tempKey);
        this.tempKeyEngine = tempKey.toCryptoEngine();

    }

    public void connect() {
        RegistrationRootApiRemote remote = FastMetaNet.INSTANCE.get()
                .makeClient(
                        upstreamRegistrationUri,
                        ClientApiRegUnsafe.META,
                        RegistrationRootApi.META,
                        ignored -> this
                );
        this.rootApi = remote;
        this.context = remote.getFastMetaContext();
    }

    public WorkProofDTO workProof() {
        return workProof;
    }

    public ARFuture<ServerDescriptor[]> resolveServers(Cloud cloud) {
        return safeApi.resolveServers(cloud);
    }

    private ARFuture<CryptoEngine> requestOriginalAsymmetricKey() {
        return rootApi.getAsymmetricPublicKey(cryptoLib).map(k -> {
            io.aether.crypto.SignedKey kk = CryptoUtils.of(k);
            boolean trusted = false;
            for (SignChecker checker : rootSigners) {
                if (checker.checkSign(
                        kk.key().getData(),
                        kk.sign()
                )) {
                    trusted = true;
                    break;
                }
            }
            if (!trusted) {
                Log.error(
                        "ProxyUpstreamReg: original key verification failed",
                        "key",
                        kk
                );
                throw new IllegalStateException(
                        "Original registration key verification failed"
                );
            }
            upstreamAsymEngine =
                    kk.key().asAsymmetric().toCryptoEngine();
            return upstreamAsymEngine;
        });
    }

    /**
     * Full async chain: fetch upstream asymmetric key, open safe stream,
     * set return key, request WPD.
     */
    public ARFuture<WorkProofDTO> requestWorkProofData() {
        return requestOriginalAsymmetricKey().mapRFuture(asymCE -> {
            safeApi = rootApi.openEnter(
                    cryptoLib,
                    remote -> ClientApiRegSafe.EMPTY,
                    asymCE::encrypt,
                    "proxy upstream reg safe"
            );
            safeApi.setReturnKey(tempKeyNative);
            return safeApi.requestWorkProofData(
                    parent,
                    PowMethod.AE_BCRYPT_CRC32
            ).map(wpd -> {
                workProof = wpd;
                return wpd;
            });
        });
    }


    public ARFuture<RecoveryResult> recovery(
            UUID uid,
            Key masterKey
    ) {
        return requestOriginalAsymmetricKey().mapRFuture(asymCE -> {
            safeApi = rootApi.openEnter(
                    cryptoLib,
                    remote -> ClientApiRegSafe.EMPTY,
                    asymCE::encrypt,
                    "proxy upstream recovery safe"
            );
            safeApi.setReturnKey(tempKeyNative);
            return safeApi.recovery(uid, masterKey);
        });
    }



    public ARFuture<FinishResult> finish(
            AKey.Symmetric masterKey,
            String salt,
            String suffix,
            int[] passwords
    ) {
        if (workProof == null) {
            return ARFuture.ofThrow(
                    new IllegalStateException(
                            "WorkProofData is not yet available"
                    )
            );
        }

        return safeApi.registrationDirect(
                        salt,
                        suffix,
                        passwords,
                        parent,
                        CryptoUtils.of(masterKey)
                )
                .map(result ->
                        new FinishResult(
                                result.getAlias(),
                                result.getUid(),
                                result.getCloud()
                        )
                );
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            if (context != null) {
                context.close();
            }
        } catch (RuntimeException error) {
            Log.warn(
                    "ProxyUpstreamReg: failed to close upstream context",
                    error
            );
        }
    }

    @Override
    public void enter(ClientApiRegSafeStream stream) {
        if (safeApi == null) {
            Log.warn("ProxyUpstreamReg: enter before safeApi ready");
            return;
        }
        stream.asIn()
                .ctx(safeApi.getFastMetaContext())
                .convert(tempKeyEngine::decrypt)
                .accept();
    }

    @Override
    public void enterGlobal(GlobalRegClientApiStream stream) {
        // Legacy ClientApiRegUnsafe compatibility.
        // Upstream registration now uses registrationDirect.
    }
}