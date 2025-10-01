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
import io.aether.utils.streams.Value;

import java.net.URI;

public class ConnectionRegistration extends Connection<ClientApiRegUnsafe, RegistrationRootApi, RegistrationRootApiRemote> implements ClientApiRegUnsafe {

    private final AKey.Symmetric tempKey = CryptoProviderFactory.getProvider(client.getCryptLib().name()).createSymmetricKey();
    private final KeySymmetric tempKeyNative = KeyUtil.of(tempKey);
    private final CryptoEngine tempKeyCp = tempKey.toCryptoEngine();
    private final FastApiContext ctxSafe = new FastApiContext() {
        @Override
        public AFuture flush() {
            return null;
        }
    };
    private final FastApiContext globalCtx = new FastApiContext() {
        @Override
        public AFuture flush() {
            return null;
        }
    };
    private CryptoEngine gcp;

    public ConnectionRegistration(AetherCloudClient client, URI uri) {
        super(client, uri, ClientApiRegUnsafe.META, RegistrationRootApi.META);
        connect();
    }

    private void connect() {
        rootApi.getAsymmetricPublicKey(client.getCryptLib()).to(this::regProcess);
        rootApiContext.flushToGate(d -> {
            AFuture res = new AFuture();
            gate.send(Value.ofForce(d).linkFuture(res));
            return res;
        });
    }

    private void regProcess(SignedKey signedKey) {
        Log.info("asym public key was got");
        if (!client.verifySign(signedKey)) {
            throw new IllegalStateException("Key verification exception");
        }
        var asymCE = SignedKeyUtil.of(signedKey).key().asAsymmetric().toCryptoEngine();
        rootApi.enter(client.getCryptLib(), new ServerRegistrationApiStream(ctxSafe, asymCE::encrypt, api -> {
            api.requestWorkProofData(client.getParent(), PowMethod.AE_BCRYPT_CRC32, tempKeyNative)
                    .to(wpd -> {
                        Log.info("WorkProofData has been received");
                        var passwords = WorkProofUtil.generateProofOfWorkPool(
                                wpd.getSalt(),
                                wpd.getSuffix(),
                                wpd.getMaxHashVal(),
                                wpd.getPoolSize(),
                                5000);
                        if (!client.verifySign(wpd.getGlobalKey())) {
                            throw new RuntimeException();
                        }
                        gcp = CryptoEngine.of(KeyUtil.of(wpd.getGlobalKey().getKey()).asAsymmetric().toCryptoEngine(), client.getMasterKey().toCryptoEngine());
                        rootApi.enter(client.getCryptLib(), new ServerRegistrationApiStream(ctxSafe, asymCE::encrypt,
                                a2 -> a2.registration(wpd.getSalt(), wpd.getSuffix(), passwords, client.getParent(), tempKeyNative,
                                        new GlobalApiStream(globalCtx, gcp::encrypt, gapi -> {
                                            gapi.setMasterKey(KeyUtil.of(client.getMasterKey()));
                                            gapi.finish()
                                                    .to(d -> {
                                                        Log.trace("registration step finish");
                                                        rootApi.enter(client.getCryptLib(), new ServerRegistrationApiStream(ctxSafe, asymCE::encrypt, a3 -> {
                                                            Log.trace("registration step resolve servers: $servers", "servers", d.getCloud());
                                                            a3.resolveServers(d.getCloud()).to(ss -> {
                                                                for (var s : ss) {
                                                                    client.servers.put((int) s.getId(), s);
                                                                }
                                                                client.confirmRegistration(d);
                                                            });
                                                        }));
                                                        rootApi.flush();
                                                    });
                                        }))));
                        rootApi.flush();
                    }, 5, () -> Log.warn("timeout requestWorkProofData"));
        }));
        rootApi.flush();
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
