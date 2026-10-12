
package io.aether.cloud.proxy;

import io.aether.api.CryptoUtils;

import io.aether.api.clientserverapi.ClientApiUnsafeRemote;

import io.aether.api.clientserverapi.ServerRegistrationApi;

import io.aether.api.clientserverapi.RecoveryResult;

import io.aether.api.clientserverapi.RegistrationDirectResult;
import io.aether.api.common.Cloud;
import io.aether.api.common.CryptoLib;
import io.aether.api.common.IPAddress;
import io.aether.api.common.Key;
import io.aether.api.common.PowMethod;
import io.aether.api.common.ServerDescriptor;
import io.aether.api.common.SignedKey;
import io.aether.api.common.WorkProofDTO;
import io.aether.crypto.CryptoEngine;
import io.aether.crypto.AKey;
import io.aether.logger.Log;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.slots.AMFuture;

import java.util.UUID;

/**
 * Downstream-facing side of the registration flow: the proxy pretends to be
 * an ordinary Aether registration work server for one client.
 *
 * <p>PoW challenge is fetched from the original server via
 * {@link ProxyUpstreamRegistrationSession}; the same salt/suffix/passwords
 * are reused downstream. The only substitution is the global key: downstream
 * receives the PROXY's signed global key so that the next {@code GlobalApi}
 * stream is encrypted for the proxy, not the original server.
 */
public final class ProxyServerRegistrationApiImpl
        implements ServerRegistrationApi {

    final CryptoLib cryptoLib;
    final AMFuture<CryptoEngine> backCryptoEngine;

    private final ProxyService service;

    private final ClientApiUnsafeRemote remoteRootApi;

    private final IPAddress clientIp;

    private volatile ProxyUpstreamRegistrationSession upstream;
    private volatile WorkProofDTO originalWorkProof;
    private volatile WorkProofDTO proxyWorkProof;

    private UUID parent;
    private String cachedSalt;
    private String cachedSuffix;
    private int[] cachedPasswords;

    public ProxyServerRegistrationApiImpl(

            ClientApiUnsafeRemote remoteRootApi,

            ProxyService service,
            CryptoLib cryptoLib,
            AMFuture<CryptoEngine> backCryptoEngine,
            IPAddress clientIp
    ) {
        this.remoteRootApi = remoteRootApi;
        this.service = service;
        this.cryptoLib = cryptoLib;
        this.backCryptoEngine = backCryptoEngine;
        this.clientIp = clientIp;
    }

    @Override
    public void setReturnKey(Key key) {
        backCryptoEngine.set(
                CryptoUtils.of(key)
                        .asSymmetric()
                        .toCryptoEngine()
        );
    }


    @Override
    public void requestWorkProofData(UUID parent, PowMethod powMethods) {
        if (this.parent != null && !this.parent.equals(parent)) {
            Log.warn("Registration parent changed", "from", this.parent, "to", parent);
            return;
        }
        this.parent = parent;

        try {
            ProxyUpstreamRegistrationSession session =
                    new ProxyUpstreamRegistrationSession(
                            service.upstreamRegistrationUris().get(0),
                            cryptoLib,
                            parent,
                            service.rootSigners());
            session.connect();
            this.upstream = session;

            session.requestWorkProofData().to(
                    wpd -> {
                        originalWorkProof = wpd;
                        SignedKey proxyGlobalKey = CryptoUtils.of(
                                service.registrationKey(cryptoLib).getPublicKeySigned());
                        proxyWorkProof = new WorkProofDTO(
                                wpd.getSalt(),
                                wpd.getSuffix(),
                                wpd.getPoolSize(),
                                wpd.getMaxHashVal(),
                                proxyGlobalKey);
                        remoteRootApi.sendWorkProofData(parent, proxyWorkProof);
                    },
                    error -> Log.error("Proxy requestWorkProofData failed", error));
        } catch (RuntimeException error) {
            Log.error("Proxy requestWorkProofData failed", error);
        }
    }

    @Override
    public void resolveServers(Cloud serverIds) {
        short[] data = serverIds == null ? new short[0] : serverIds.getData();
        for (short sid : data) {
            if (sid == service.proxySid()) {
                remoteRootApi.sendResolvedServers(new ServerDescriptor[]{
                        service.proxyDescriptor()
                });
                return;
            }
        }
        if (upstream == null) {
            remoteRootApi.sendResolvedServers(new ServerDescriptor[0]);
            return;
        }
        upstream.resolveServers(serverIds).to(
                remoteRootApi::sendResolvedServers,
                error -> Log.error("Proxy resolveServers failed", error));
    }


    @Override
    public void recovery(UUID uid, Key oldMasterKey, Key newMasterKey) {
        ProxyUpstreamRegistrationSession session = null;
        try {
            session = new ProxyUpstreamRegistrationSession(
                    service.upstreamRegistrationUris().get(0),
                    cryptoLib,
                    null,
                    service.rootSigners());
            session.connect();

            ProxyUpstreamRegistrationSession finalSession = session;
            session.recovery(uid, oldMasterKey, newMasterKey).to(
                    recovered -> {
                        finalSession.close();
                        remoteRootApi.sendRecoveredClientState(recovered);
                    },
                    error -> {
                        finalSession.close();
                        Log.error("Proxy recovery failed", error);
                    });
        } catch (RuntimeException error) {
            if (session != null) session.close();
            Log.error("Proxy recovery failed", error);
        }
    }



            @Override
            public void registrationDirect(
                    String salt,
                    String suffix,
                    int[] passwords,
                    UUID parent,
                    Key masterKey
            ) {
                if (originalWorkProof == null) {
                    Log.error("Proxy registrationDirect before WorkProofData");
                    return;
                }
                if (this.parent != null && !this.parent.equals(parent)) {
                    Log.warn("Registration parent changed", "from", this.parent, "to", parent);
                    return;
                }
                ProxyUpstreamRegistrationSession session = upstream;
                if (session == null) {
                    Log.error("Upstream registration session is missing");
                    return;
                }
                this.parent = parent;

                session.finish(CryptoUtils.of(masterKey).asSymmetric(), salt, suffix, passwords)
                        .to(
                                result -> remoteRootApi.sendRegistrationDirect(
                                        new RegistrationDirectResult(
                                                result.getAlias(),
                                                result.getUid(),
                                                result.getCloud())),
                                error -> Log.error("Proxy registrationDirect failed", error));
            }
}