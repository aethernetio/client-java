package io.aether.cloud.client;

import io.aether.api.EncryptionApi;
import io.aether.api.EncryptionApiConfig;
import io.aether.api.EncryptionApiImpl;
import io.aether.api.clientApi.ClientApiSafe;
import io.aether.api.clientApi.ClientApiUnsafe;
import io.aether.api.serverRegistryApi.RegistrationResponseLite;
import io.aether.api.serverRegistryApi.RootApi;
import io.aether.api.serverRegistryApi.WorkProofUtil;
import io.aether.client.AetherClientFactory;
import io.aether.common.*;
import io.aether.logger.Log;
import io.aether.net.ApiDeserializerConsumer;
import io.aether.net.Protocol;
import io.aether.net.ProtocolConfig;
import io.aether.utils.futures.AFuture;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public class ConnectionForRegistration extends EncryptionApiImpl<ClientApiSafe> implements ClientApiUnsafe, ApiDeserializerConsumer {
	private final AetherCloudClient client;
	private final AFuture keysFuture = new AFuture();
	AFuture connectFuture;
	EncryptionApiConfig globalEncryptionApiConfig;
	private Protocol<ClientApiUnsafe, RootApi> protocol;
	private ClientApiSafe clientApiSafe;
	@Override
	protected void selectLib(CryptoLib cryptoLib) {
		assert cryptoLib ==client.getCryptLib();
	}

	public ConnectionForRegistration(AetherCloudClient client, URI uri) {
		assert uri != null;
		setSubApiFactory(this::getClientApiSafe);
		this.client = client;
		Log.debug("try reg to: " + uri);
		var con = AetherClientFactory.make(uri,
				ProtocolConfig.of(ClientApiUnsafe.class, RootApi.class, AetherCodec.BINARY),
				(p) -> {
					protocol = p;
					return this;
				});
		connectFuture = con.to((p) -> {
			var key = p.getRemoteApi().getAsymmetricPublicKey(client.getCryptLib());
			EncryptionApi.prepareRemote(p.getRemoteApi(), getConfig());
			key.to((signedKey) -> {
				if (!signedKey.check()) {
					throw new RuntimeException();
				}
				getConfig().asymmetric = signedKey.key().getType().cryptoLib().env.asymmetric(signedKey.key());
				EncryptionApi.prepareRemote(p.getRemoteApi(), getConfig());
				var safeApi = p.getRemoteApi().asymmetric();
				safeApi.requestWorkProofData2(client.getParent(), client.getCryptLib())
						.to(wpd -> {
							var passwords = WorkProofUtil.generateProofOfWorkPool(
									wpd.salt(),
									wpd.suffix(),
									wpd.maxHashVal(),
									wpd.poolSize(),
									5000);
							RootApi remoteApi = p.getRemoteApi();
							EncryptionApi.prepareRemote(remoteApi, getConfig());
							getConfig().symmetric =client.getMasterKey().getType().cryptoLib().env.symmetricForClientAndServer(client.getMasterKey(),0);
							var globalClientApi0 = remoteApi
									.asymmetric()
									.registration(client.getParent(), wpd.salt(), wpd.suffix(), passwords, client.getMasterKey());
							if (!wpd.globalKey().check()) {
								throw new RuntimeException();
							}
							getGlobalDataPreparerConfig().asymmetric =wpd.globalKey().key().getType().cryptoLib().env.asymmetric(wpd.globalKey().key());
							EncryptionApi.prepareRemote(globalClientApi0, getGlobalDataPreparerConfig());
							var globalClientApi = globalClientApi0.asymmetric();
							globalClientApi.setMasterKey(client.getMasterKey());
							globalClientApi.finish();
							protocol.flush();
						});
				protocol.flush();
			});
			p.flush();
		}).toFuture();
	}
	public EncryptionApiConfig getGlobalDataPreparerConfig() {
		if (globalEncryptionApiConfig == null) {
			globalEncryptionApiConfig = new EncryptionApiConfig();
			globalEncryptionApiConfig.asymmetric = CryptoLib.SODIUM.env.asymmetric();
		}
		return globalEncryptionApiConfig;
	}
	public ClientApiSafe getClientApiSafe() {
		if (clientApiSafe == null) clientApiSafe = new MyClientApiSafe();
		return clientApiSafe;
	}
	@Override
	public void sendServerKeys(SignedKey asymPublicKey, SignedKey signKey) {
		//TODO check
		var k=asymPublicKey.key();
		this.getConfig().asymmetric = k.getType().cryptoLib().env.asymmetric(k);
		keysFuture.done();
	}
	private class MyClientApiSafe implements ClientApiSafe {
		@Override
		public void pushMessage(@NotNull Message message) {
			throw new UnsupportedOperationException();
		}
		@Override
		public void updateCloud(@NotNull UUID uid, @NotNull Cloud cloud) {
			throw new UnsupportedOperationException();
		}
		@Override
		public void updateServers(@NotNull ServerDescriptor @NotNull [] serverDescriptors) {
			throw new UnsupportedOperationException();
		}
		@Override
		public void newChildren(@NotNull List<UUID> newChildren) {
			throw new UnsupportedOperationException();
		}
		@Override
		public void confirmRegistration(RegistrationResponseLite registrationResponse) {
			client.confirmRegistration(registrationResponse);
		}
	}
}
