package io.aether.cloud.client;

import io.aether.api.clientApi.ClientApiRegSafe;
import io.aether.api.clientApi.ClientApiRegUnsafe;
import io.aether.api.serverRegistryApi.*;
import io.aether.logger.Log;
import io.aether.net.RemoteApi;
import io.aether.utils.futures.AFuture;
import io.aether.utils.streams.ApiNode;
import io.aether.utils.streams.CryptoNode;

import java.net.URI;

public class ConnectionRegistration extends Connection<ClientApiRegUnsafe, RegistrationRootApi> implements ClientApiRegUnsafe {

    public ConnectionRegistration(AetherCloudClient client, URI uri) {
        super(client, uri, ClientApiRegUnsafe.class, RegistrationRootApi.class);
        connect();
    }

    @Override
    protected void onConnect(RegistrationRootApi remoteApi) {
        Log.debug("request asym public key");
        var keyFuture = remoteApi.getAsymmetricPublicKey(client.getCryptLib());
        keyFuture.to((signedKey) -> {
            Log.debug("asym public key was got");
            if (!signedKey.check()) {
                throw new RuntimeException();
            }
            ApiNode<ClientApiRegSafe, ServerRegistrationApi, CryptoNode<?>> safeStream=ApiNode.of(ClientApiRegSafe.META, ServerRegistrationApi.META,
                    CryptoNode.of());
            remoteApi.enter(safeStream,client.getCryptLib());
            safeStream.down().findOut(CryptoNode.class)
                    .setCryptoEncoder(signedKey.key().getType().cryptoLib().env.asymmetric(signedKey.key()));
            var safeApi = safeStream
                    .forClient(new ClientApiRegSafe() {
                    })
                    .getRemoteApi();
            var tempKey = client.getCryptLib().env.makeSymmetricKey();
            var cp = tempKey.symmetricProvider();
            safeStream.findDown(CryptoNode.class).setCryptoDecoder(cp).setName("client enter");
            safeApi.requestWorkProofData(client.getParent(), PowMethod.AE_BCRYPT_CRC32, tempKey)
                    .to(wpd -> {
                        var passwords = WorkProofUtil.generateProofOfWorkPool(
                                wpd.salt(),
                                wpd.suffix(),
                                wpd.maxHashVal(),
                                wpd.poolSize(),
                                5000);
                        var globalApiNode = safeApi.registration(wpd.salt(), wpd.suffix(), passwords, client.getMasterKey());
                        if (!wpd.globalKey().check()) {
                            throw new RuntimeException();
                        }
                        var masterKey = client.getMasterKey();
                        globalApiNode.findDown(CryptoNode.class)
                                .setCryptoEncoder(wpd.globalKey().key().asymmetricProvider())
                                .setCryptoDecoder(masterKey.symmetricProvider())
                                .setName("client global");
                        var globalClientApi = globalApiNode.forClient(new GlobalRegClientApi() {
                        }).getRemoteApi();
                        globalClientApi.setMasterKey(masterKey);
                        globalClientApi.finish().to(d -> {
                            safeApi.resolveServers(d.cloud()).to(ss -> {
                                for (var s : ss) {
                                    client.servers.set(s);
                                }
                                client.confirmRegistration(d);
                            });
                            RemoteApi.of(safeApi).flush();
                        });
                        globalApiNode.flush();
                    });
            RemoteApi.of(safeApi).flush();
        });
        RemoteApi.of(remoteApi).flush();
    }

}
