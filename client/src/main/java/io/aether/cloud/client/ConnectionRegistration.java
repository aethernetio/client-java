package io.aether.cloud.client;

import io.aether.api.CryptoUtils;
import io.aether.api.clientserverapi.*;
import io.aether.api.common.Cloud;
import io.aether.api.common.KeySymmetric;
import io.aether.api.common.PowMethod;
import io.aether.crypto.AKey;
import io.aether.crypto.CryptoEngine;
import io.aether.crypto.CryptoProviderFactory;
import io.aether.logger.Log;
import io.aether.utils.WorkProofUtil;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;

import java.net.URI;
import java.util.Arrays;

public class ConnectionRegistration extends Connection<ClientApiRegUnsafe, RegistrationRootApiRemote> implements ClientApiRegUnsafe {
    final AFuture connectFuture = AFuture.make();
    private final AKey.Symmetric tempKey = CryptoProviderFactory.getProvider(client.getCryptLib().name()).createSymmetricKey();
    private final KeySymmetric tempKeyNative = CryptoUtils.of(tempKey);
    private final CryptoEngine tempKeyCp = tempKey.toCryptoEngine();
    volatile ServerRegistrationApiRemote safeApi;
    volatile GlobalRegServerApiRemote globalApi;
    private CryptoEngine gcp;

    public ConnectionRegistration(AetherCloudClient client, URI uri) {
        super(client, uri, ClientApiRegUnsafe.META, RegistrationRootApi.META);
    }

    private ARFuture<CryptoEngine> getAsymmetricPublicKey() {
        Log.debug("RegConn: TCP connection successful, requesting asymmetric key.", "uri", uri);
        return rootApi.getAsymmetricPublicKey(client.getCryptLib()).map(k -> {
            var kk = CryptoUtils.of(k);
            if (!client.verifySign(kk)) {
                Log.error("RegConn: Key verification failed.", "signedKey", kk);
                throw new IllegalStateException("Key verification exception");
            }
            return kk.key().asAsymmetric().toCryptoEngine();
        });
    }

    public AFuture registration() {
        Log.debug("RegConn: Starting async registration process.", "uri", uri);
        getAsymmetricPublicKey().to(this::regProcess);
        return connectFuture;
    }

    private void regProcess(CryptoEngine asymCE) {
        safeApi = rootApi.openEnter(client.getCryptLib(), r -> ClientApiRegSafe.EMPTY, asymCE::encrypt, "reg safe");
        Log.info("RegConn: Asym public key was received.");
        safeApi.setReturnKey(tempKeyNative);
        safeApi.requestWorkProofData(client.getParent(), PowMethod.AE_BCRYPT_CRC32)
                .to(wpd -> {
                    Log.info("RegConn: WorkProofData has been received. Starting PoW calculation.");
                    var passwords = WorkProofUtil.generateProofOfWorkPool(
                            wpd.getSalt(),
                            wpd.getSuffix(),
                            wpd.getMaxHashVal(),
                            wpd.getPoolSize(),
                            5300);
                    if (!client.verifySign(CryptoUtils.of(wpd.getGlobalKey()))) {
                        Log.error("RegConn: Global key verification failed.");
                        throw new RuntimeException();
                    }
                    gcp = CryptoEngine.of(CryptoUtils.of(wpd.getGlobalKey().getKey()).asAsymmetric().toCryptoEngine(), client.getMasterKey().toCryptoEngine());
                    safeApi.setReturnKey(tempKeyNative);
                    globalApi = safeApi.openRegistration(wpd.getSalt(), wpd.getSuffix(), passwords, client.getParent(), r -> GlobalRegClientApi.EMPTY, gcp::encrypt, "global");
                    try (var l = globalApi.getFastMetaContext().lock()) {
                        globalApi.setMasterKey(CryptoUtils.of(client.getMasterKey()));
                        globalApi.finish()
                                .to(d -> {
                                    Log.trace("RegConn: registration step finish.");
                                    Log.info("RegConn: Registration confirmed.");
                                    resolveCloud(d.getCloud(), asymCE).to(() -> {
                                        Log.info("RegConn: resolve cloud.");
                                        client.confirmRegistration(d);
                                    });
                                }).addListener((f) -> {
                                    if (!f.isDone()) {
                                        Log.error("flush task canceled 1! $f", "f", f);
                                    } else {
                                        connectFuture.done();
                                    }
                                });
                    }
                }, 6, () -> Log.warn("RegConn: timeout requestWorkProofData"));
    }

    public AFuture resolveCloud(Cloud cloud) {
        AFuture res = AFuture.make();
        getAsymmetricPublicKey().to(ce -> {
            resolveCloud(cloud, ce).to(res);
        }).onError(res);
        return res;
    }

    private AFuture resolveCloud(Cloud cloud, CryptoEngine asymCE) {
        if (!client.isRecoveryInProgress.compareAndSet(false, true)) {
            Log.debug("recovery procedure abort");
            return client.recoveryFuture;
        }
        AFuture result = client.recoveryFuture;
        Log.debug("Resolving cloud: " + cloud);
        Log.trace("RegConn: registration step resolve servers: $servers", "servers", cloud);
        safeApi.resolveServers(cloud)
                .to(ss -> {
                    Log.debug("Received server descriptors: " + Arrays.toString(ss));
                    for (var s : ss) {
                        Log.debug("Putting server descriptor: " + s);
                        client.putServerDescriptor(s);
                    }
                    result.tryDone();
                    Log.info("RegConn: Server descriptors resolved.");
                })
                .onError(e -> {
                    Log.error("Failed to resolve servers", e);
                    result.tryError(e);
                });
        return result;
    }


    @Override
    public void enterGlobal(GlobalRegClientApiStream stream) {
        stream.asIn()
                .convert(gcp::decrypt)
                .ctx(globalApi.getFastMetaContext())
                .accept();
    }

    @Override
    public void enter(ClientApiRegSafeStream stream) {
        stream.asIn()
                .ctx(safeApi.getFastMetaContext())
                .convert(tempKeyCp::decrypt)
                .accept();
    }
}