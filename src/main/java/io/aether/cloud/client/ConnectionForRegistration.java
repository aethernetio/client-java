package io.aether.cloud.client;

import io.aether.Aether;
import io.aether.api.DataPrepareApi;
import io.aether.api.DataPrepareApiImpl;
import io.aether.api.DataPreparerConfig;
import io.aether.api.clientApi.ClientApiSafe;
import io.aether.api.clientApi.ClientApiUnsafe;
import io.aether.api.serverRegistryApi.CryptType;
import io.aether.api.serverRegistryApi.RegistrationResponse;
import io.aether.api.serverRegistryApi.RootApi;
import io.aether.api.serverRegistryApi.WorkProofUtil;
import io.aether.client.AetherClientFactory;
import io.aether.common.*;
import io.aether.net.ApiDeserializerConsumer;
import io.aether.net.Protocol;
import io.aether.net.ProtocolConfig;
import io.aether.sodium.AsymCrypt;
import io.aether.sodium.ChaCha20Poly1305Pair;
import io.aether.sodium.Nonce;
import io.aether.utils.futures.AFuture;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.List;
import java.util.UUID;

public class ConnectionForRegistration extends DataPrepareApiImpl<ClientApiSafe> implements ClientApiUnsafe, ApiDeserializerConsumer {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final AetherCloudClient client;
	private final AFuture keysFuture = new AFuture();
	AFuture connectFuture;
	DataPreparerConfig globalDataPreparerConfig;
	private Protocol<ClientApiUnsafe, RootApi> protocol;
	private ClientApiSafe clientApiSafe;
	public ConnectionForRegistration(AetherCloudClient client, URI uri) {
		assert uri != null;
		setSubApiFactory(this::getClientApiSafe);
		this.client = client;
		log.debug("try reg to: " + uri);
		var con = AetherClientFactory.make(uri,
				ProtocolConfig.of(ClientApiUnsafe.class, RootApi.class, AetherCodec.BINARY),
				(p) -> {
					protocol = p;
					return this;
				});
		connectFuture = con.to((p) -> {
			var keys = p.getRemoteApi().getKeys(CryptType.CURVE25519, SignType.AE_ED25519);
			DataPrepareApi.prepareRemote(p.getRemoteApi(), getConfig());
			keys.to((signedKey) -> {
				if (!client.getClientConfig().globalSigner.check(signedKey.toPlain())) {
					throw new RuntimeException();
				}
				getConfig().asymCrypt = new AsymCrypt(signedKey.key());
				var safeApi = p.getRemoteApi().curve25519();
				safeApi.requestWorkProofData2(client.getParent(), CryptType.CURVE25519, SignType.AE_ED25519)
						.to(wpd -> {
							var passwords = WorkProofUtil.generateProofOfWorkPool(
									wpd.salt(),
									wpd.suffix(),
									wpd.maxHashVal(),
									wpd.poolSize(),
									5000);
							RootApi remoteApi = p.getRemoteApi();
							getConfig().chaCha20Poly1305Pair = ChaCha20Poly1305Pair.forClientAndServer(client.getMasterKey(), Nonce.of());
							var globalClientApi0 = remoteApi
									.curve25519()
//									.prepare()
									.registration(client.getParent(), wpd.salt(), wpd.suffix(), passwords, client.getMasterKey().toTypedKey());
							DataPrepareApi.prepareRemote(globalClientApi0, getGlobalDataPreparerConfig());
							var globalClientApi = globalClientApi0.curve25519();
							globalClientApi.setMasterKey(client.getMasterKey().toTypedKey());
							globalClientApi.finish();
							protocol.flush();
						});
				protocol.flush();
			});
			p.flush();
		}).toFuture();
	}
	public DataPreparerConfig getGlobalDataPreparerConfig() {
		if (globalDataPreparerConfig == null) {
			globalDataPreparerConfig = new DataPreparerConfig();
			globalDataPreparerConfig.asymCrypt = Aether.globalAsym;
		}
		return globalDataPreparerConfig;
	}
	public ClientApiSafe getClientApiSafe() {
		if (clientApiSafe == null) clientApiSafe = new MyClientApiSafe();
		return clientApiSafe;
	}
	@Override
	public void sendServerKeys(SignedKey asymPublicKey, SignedKey signKey) {
		//TODO check
		this.getConfig().asymCrypt = new AsymCrypt(asymPublicKey.key());
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
		public void confirmRegistration(RegistrationResponse registrationResponse) {
			client.confirmRegistration(registrationResponse);
		}
	}
}
