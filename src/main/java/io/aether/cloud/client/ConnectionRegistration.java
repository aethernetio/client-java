package io.aether.cloud.client;

import io.aether.api.EncryptionApi;
import io.aether.api.EncryptionApiConfig;
import io.aether.api.clientApi.ClientApiRegSafe;
import io.aether.api.clientApi.ClientApiRegUnsafe;
import io.aether.api.serverRegistryApi.RegistrationResponseLite;
import io.aether.api.serverRegistryApi.RegistrationRootApi;
import io.aether.api.serverRegistryApi.WorkProofUtil;
import io.aether.common.SignedKey;
import io.aether.net.ApiDeserializerConsumer;
import io.aether.net.impl.bin.ApiLevelDeserializer;
import io.aether.net.meta.ExceptionUnit;
import io.aether.net.meta.ResultUnit;
import io.aether.utils.futures.AFuture;

import java.net.URI;

public class ConnectionRegistration extends Connection<ClientApiRegUnsafe, ClientApiRegSafe, RegistrationRootApi> implements ClientApiRegUnsafe {
    private final EncryptionApiConfig globalDataPreparerConfig = new EncryptionApiConfig();
    private final AFuture keysFuture = new AFuture();

    public ConnectionRegistration(AetherCloudClient client, URI uri) {
        super(client, uri, ClientApiRegUnsafe.class, RegistrationRootApi.class, new MyClientApiSafe(client));
        connect();
    }

    @Override
    protected void onConnect(RegistrationRootApi remoteApi) {
        var key = remoteApi.getAsymmetricPublicKey(client.getCryptLib());
        EncryptionApi.prepareRemote(remoteApi, getConfig());
        key.to((signedKey) -> {
            if (!signedKey.check()) {
                throw new RuntimeException();
            }
            getConfig().asymmetric = signedKey.key().getType().cryptoLib().env.asymmetric(signedKey.key());
            EncryptionApi.prepareRemote(remoteApi, getConfig());
            var safeApi = remoteApi.asymmetric();
            safeApi.requestWorkProofData2(client.getParent(), client.getCryptLib())
                    .to(wpd -> {
                        var passwords = WorkProofUtil.generateProofOfWorkPool(
                                wpd.salt(),
                                wpd.suffix(),
                                wpd.maxHashVal(),
                                wpd.poolSize(),
                                5000);
                        EncryptionApi.prepareRemote(remoteApi, getConfig());
                        getConfig().symmetric = client.getMasterKey().getType().cryptoLib().env.symmetricForClientAndServer(client.getMasterKey());
                        var globalClientApi0 = remoteApi
                                .asymmetric()
                                .registration(client.getParent(), wpd.salt(), wpd.suffix(), passwords, client.getMasterKey());
                        if (!wpd.globalKey().check()) {
                            throw new RuntimeException();
                        }
                        globalDataPreparerConfig.asymmetric = wpd.globalKey().key().getType().cryptoLib().env.asymmetric(wpd.globalKey().key());
                        EncryptionApi.prepareRemote(globalClientApi0, globalDataPreparerConfig);
                        var globalClientApi = globalClientApi0.asymmetric();
                        globalClientApi.setMasterKey(client.getMasterKey());
                        globalClientApi.finish();
                        aConnection.flush();
                    });
            aConnection.flush();
        });
    }

    @Override
    public void sendServerKeys(SignedKey asymPublicKey, SignedKey signKey) {
        //TODO check
        var k = asymPublicKey.key();
        this.getConfig().asymmetric = k.getType().cryptoLib().env.asymmetric(k);
        keysFuture.done();
    }

    private static class MyClientApiSafe implements ClientApiRegSafe, ApiDeserializerConsumer {
        private final AetherCloudClient client;
        ApiLevelDeserializer apiProcessor;
        io.aether.net.AConnection
        @Override
        public void setApiDeserializer(ApiLevelDeserializer apiProcessor) {
            this.apiProcessor=apiProcessor;
        }

        @Override
        public void sendResult(ResultUnit unit) {
            apiProcessor.getConnection().sendResultFromRemote(unit);
        }

        @Override
        public void sendException(ExceptionUnit unit) {
            apiProcessor.sendResultFromRemote(unit);
        }

        public MyClientApiSafe(AetherCloudClient client) {
            this.client = client;
        }

        @Override
        public void confirmRegistration(RegistrationResponseLite registrationResponse) {
            client.confirmRegistration(registrationResponse);
        }
    }
}
