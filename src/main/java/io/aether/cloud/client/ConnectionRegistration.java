package io.aether.cloud.client;

import io.aether.api.clientApi.ClientApiRegSafe;
import io.aether.api.clientApi.ClientApiRegUnsafe;
import io.aether.api.serverRegistryApi.*;
import io.aether.logger.Log;
import io.aether.net.ApiGate;
import io.aether.net.RemoteApi;
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
        var apiCon=RemoteApi.of(remoteApi).getConnection();
        keyFuture.to((signedKey) -> {
            ((ApiGate<?,?>)apiCon).down().run(()->{
                Log.debug("asym public key was got");
                if (!signedKey.check()) {
                    throw new RuntimeException();
                }
                ApiNode<ClientApiRegSafe, ServerRegistrationApi, CryptoNode<?>> authStream =
                        ApiNode.of(ClientApiRegSafe.META, ServerRegistrationApi.META, CryptoNode.of(apiCon.newStream().base.up()).setName("cloud client auth"));
                authStream.gDown().inSide.find(CryptoNode.class)
                        .setCryptoEncoder(signedKey.key().getType().cryptoLib().env.asymmetric(signedKey.key()));
                remoteApi.enter(authStream, client.getCryptLib());
                var authApi = authStream
                        .forClient(ClientApiRegSafe.EMPTY_INSTANCE)
                        .getRemoteApi();
                var tempKey = client.getCryptLib().env.makeSymmetricKey();
                var cp = tempKey.symmetricProvider();
                authStream.findDown(CryptoNode.class).setCryptoDecoder(cp).setName("client enter");
                authApi.requestWorkProofData(client.getParent(), PowMethod.AE_BCRYPT_CRC32, tempKey)
                        .to(wpd -> {
                            Log.debug("WorkProofData has been received");
                            var passwords = WorkProofUtil.generateProofOfWorkPool(
                                    wpd.salt(),
                                    wpd.suffix(),
                                    wpd.maxHashVal(),
                                    wpd.poolSize(),
                                    5000);
                            ApiNode<GlobalRegClientApi, GlobalRegServerApi, CryptoNode<?>> globalApiNode =
                                    ApiNode.of(GlobalRegClientApi.META, GlobalRegServerApi.META, CryptoNode.of().setName("cloud client registration. global api"));
                            authApi.registration(globalApiNode, wpd.salt(), wpd.suffix(), passwords, client.getMasterKey());
                            if (!wpd.globalKey().check()) {
                                throw new RuntimeException();
                            }
                            var masterKey = client.getMasterKey();
                            globalApiNode.findDown(CryptoNode.class)
                                    .setCryptoEncoder(wpd.globalKey().key().asymmetricProvider())
                                    .setCryptoDecoder(masterKey.symmetricProvider())
                                    .setName("client global");
                            var globalClientApi = globalApiNode.forClient(GlobalRegClientApi.EMPTY).getRemoteApi();
                            globalClientApi.setMasterKey(masterKey);
                            globalClientApi.finish().to(d -> {
                                authApi.resolveServers(d.cloud()).to(ss -> {
                                    for (var s : ss) {
                                        client.servers.set(s);
                                    }
                                    client.confirmRegistration(d);
                                });
                                RemoteApi.of(authApi).flush();
                            });
                            globalApiNode.flush();
                        });
                RemoteApi.of(authApi).flush();
            });
        });
        apiCon.flush();
    }

}
