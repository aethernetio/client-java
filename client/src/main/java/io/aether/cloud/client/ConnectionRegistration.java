package io.aether.cloud.client;

import io.aether.clientServerRegApi.WorkProofUtil;
import io.aether.clientServerRegApi.clientApi.ClientApiRegSafe;
import io.aether.clientServerRegApi.clientApi.GlobalRegClientApi;
import io.aether.clientServerRegApi.serverRegistryApi.GlobalRegServerApi;
import io.aether.clientServerRegApi.serverRegistryApi.PowMethod;
import io.aether.clientServerRegApi.serverRegistryApi.RegistrationRootApi;
import io.aether.clientServerRegApi.serverRegistryApi.ServerRegistrationApi;
import io.aether.crypt.Key;
import io.aether.crypt.SignedKey;
import io.aether.logger.Log;
import io.aether.net.Remote;
import io.aether.net.StreamManager;
import io.aether.utils.streams.CryptoNode;
import io.aether.utils.streams.Value;

import java.net.URI;

public class ConnectionRegistration extends Connection<io.aether.clientServerRegApi.clientApi.ClientApiRegUnsafe, RegistrationRootApi> implements io.aether.clientServerRegApi.clientApi.ClientApiRegUnsafe {

    volatile CryptoNode<?> cp;
    volatile CryptoNode<?> gcp;

    public ConnectionRegistration(AetherCloudClient client, URI uri) {
        super(client, uri, io.aether.clientServerRegApi.clientApi.ClientApiRegUnsafe.class, RegistrationRootApi.class);
        connect();
    }

    private void workStep3(Remote<ServerRegistrationApi> api, Remote<GlobalRegServerApi> gapi) {
        Log.trace("registration step 3");
        gapi.run_flush(a -> {
            a.setMasterKey(client.getMasterKey());
            a.finish().to(d -> {
                Log.trace("registration step finish");
                api.run_flush(aa -> aa.resolveServers(d.cloud).to(ss -> {
                    Log.trace("registration step resolve servers: $servers", "servers", ss);
                    for (var s : ss) {
                        client.servers.set(s);
                    }
                    client.confirmRegistration(d);
                }));
            });
        });
    }

    private void workStep2(Remote<ServerRegistrationApi> api, Key tempKey) {
        api.run_flush(a -> {
            a.requestWorkProofData(client.getParent(), PowMethod.AE_BCRYPT_CRC32, tempKey)
                    .to(wpd -> {
                        Log.info("WorkProofData has been received");
                        var passwords = WorkProofUtil.generateProofOfWorkPool(
                                wpd.salt,
                                wpd.suffix,
                                wpd.maxHashVal,
                                wpd.poolSize,
                                5000);
                        if (!client.verifySign(wpd.globalKey)) {
                            throw new RuntimeException();
                        }
                        gcp = CryptoNode.of(wpd.globalKey.key().asymmetricProvider(), client.getMasterKey().symmetricProvider(), api,
                                        (a2, v) -> a2.registration(wpd.salt, wpd.suffix, passwords, client.getParent(), tempKey, v))
                                .setName("cloud client registration. global api");
                        workStep3(api, gcp.up().toApi(GlobalRegClientApi.META, GlobalRegServerApi.META, GlobalRegClientApi.EMPTY, StreamManager.forClient()).getRemoteApi());
                    }, 5, () -> Log.warn("timeout requestWorkProofData"));
        });
    }

    private void workStep1(Remote<RegistrationRootApi> remoteApi, SignedKey signedKey) {
        Log.info("asym public key was got");
        if (!client.verifySign(signedKey)) {
            throw new IllegalStateException("Key verification exception");
        }
        var tempKey = client.getCryptLib().env.makeSymmetricKey();
        cp = CryptoNode.of(
                        signedKey.key().getType().cryptoLib().env.asymmetric(signedKey.key()),
                        tempKey.symmetricProvider(), remoteApi, (a, v) -> a.enter(client.getCryptLib(), v))
                .setName("cloud client auth");
        var api = cp.up().toApi(ClientApiRegSafe.META, ServerRegistrationApi.META, r -> gcp.down().send(Value.of(r)), StreamManager.forClient());
        workStep2(api.getRemoteApi(), tempKey);
    }

    @Override
    public void enter(Value<byte[]> data) {
        cp.down().send(data);
    }

    @Override
    protected void onConnect(Remote<RegistrationRootApi> remoteApi) {
        Log.info("request asym public key");
        remoteApi.run_flush(a -> {
            a.getAsymmetricPublicKey(client.getCryptLib()).to(sk -> workStep1(remoteApi, sk));
        });
    }

}
