package io.aether.cloud.client;

import io.aether.api.clientserverregapi.*;
import io.aether.api.common.*;
import io.aether.crypto.AKey;
import io.aether.crypto.CryptoEngine;
import io.aether.crypto.CryptoProviderFactory;
import io.aether.logger.Log;
import io.aether.net.fastMeta.FastApiContext;
import io.aether.utils.WorkProofUtil;
import io.aether.utils.futures.AFuture;

import java.net.URI;

public class ConnectionRegistration extends Connection<ClientApiRegUnsafe, RegistrationRootApiRemote> implements ClientApiRegUnsafe {
    private final AKey.Symmetric tempKey = CryptoProviderFactory.getProvider(client.getCryptLib().name()).createSymmetricKey();
    private final KeySymmetric tempKeyNative = KeyUtil.of(tempKey);
    private final CryptoEngine tempKeyCp = tempKey.toCryptoEngine();
    private final FastApiContext ctxSafe = new FastApiContext() {
        @Override
        public void flush(AFuture sendFuture) {
            Log.debug("test");
        }
    };
    private final FastApiContext globalCtx = new FastApiContext();
    private CryptoEngine gcp;

    public ConnectionRegistration(AetherCloudClient client, URI uri) {
        super(client, uri, ClientApiRegUnsafe.META, RegistrationRootApi.META);
        connect();
    }

    private void connect() {
        Log.debug("RegConn: Starting async registration process.", "uri", uri);

        getRootApiFuture()
                .to(api -> {
                    Log.debug("RegConn: TCP connection successful, requesting asymmetric key.", "uri", uri);
                    api.getAsymmetricPublicKey(client.getCryptLib()).to(this::regProcess);
                    api.flush();
                })
                .onError(e -> {
                    Log.error("RegConn: Initial connection failed.", e, "uri", uri);
                });
    }

    private void regProcess(SignedKey signedKey) {
        Log.info("RegConn: Asym public key was received.");
        if (!client.verifySign(signedKey)) {
            Log.error("RegConn: Key verification failed.", "signedKey", signedKey);
            throw new IllegalStateException("Key verification exception");
        }
        var asymCE = SignedKeyUtil.of(signedKey).key().asAsymmetric().toCryptoEngine();

        getRootApiFuture().to(api->{

            if (api == null) {
                Log.error("RegConn: Root API is null after successful connection.");
                return;
            }

            api.enter(client.getCryptLib(), new ServerRegistrationApiStream(ctxSafe, asymCE::encrypt, apiInner -> {
                apiInner.requestWorkProofData(client.getParent(), PowMethod.AE_BCRYPT_CRC32, tempKeyNative)
                        .to(wpd -> {
                            Log.info("RegConn: WorkProofData has been received. Starting PoW calculation.");
                            var passwords = WorkProofUtil.generateProofOfWorkPool(
                                    wpd.getSalt(),
                                    wpd.getSuffix(),
                                    wpd.getMaxHashVal(),
                                    wpd.getPoolSize(),
                                    5000);
                            if (!client.verifySign(wpd.getGlobalKey())) {
                                Log.error("RegConn: Global key verification failed.");
                                throw new RuntimeException();
                            }
                            gcp = CryptoEngine.of(KeyUtil.of(wpd.getGlobalKey().getKey()).asAsymmetric().toCryptoEngine(), client.getMasterKey().toCryptoEngine());

                            api.enter(client.getCryptLib(), new ServerRegistrationApiStream(ctxSafe, asymCE::encrypt,
                                    a2 -> a2.registration(wpd.getSalt(), wpd.getSuffix(), passwords, client.getParent(), tempKeyNative,
                                            new GlobalApiStream(globalCtx, gcp::encrypt, gapi -> {
                                                gapi.setMasterKey(KeyUtil.of(client.getMasterKey()));
                                                gapi.finish()
                                                        .to(d -> {
                                                            Log.trace("RegConn: registration step finish.");
                                                            api.enter(client.getCryptLib(), new ServerRegistrationApiStream(ctxSafe, asymCE::encrypt, a3 -> {
                                                                Log.trace("RegConn: registration step resolve servers: $servers", "servers", d.getCloud());
                                                                a3.resolveServers(d.getCloud()).to(ss -> {
                                                                    for (var s : ss) {
                                                                        client.servers.putResolved((int) s.getId(), s);
                                                                    }
                                                                    client.confirmRegistration(d);
                                                                    Log.info("RegConn: Registration finished successfully.");
                                                                });
                                                            }));
                                                            api.flush();
                                                        });
                                            }))));
                            api.flush();
                        }, 5, () -> Log.warn("RegConn: timeout requestWorkProofData"));

            }));
            api.flush();
        });
    }

    @Override
    public void enterGlobal(GlobalRegClientApiStream stream) {
        stream.accept(globalCtx, gcp::decrypt, GlobalRegClientApi.EMPTY);
    }

    @Override
    public void enter(ClientApiRegSafeStream stream) {
        stream.accept(ctxSafe, tempKeyCp::decrypt, ClientApiRegSafe.EMPTY);
    }
}