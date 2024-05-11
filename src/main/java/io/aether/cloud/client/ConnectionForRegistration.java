package io.aether.cloud.client;

import io.aether.api.DataPrepareApi;
import io.aether.api.DataPrepareApiImpl;
import io.aether.api.clientApi.ClientApiSafe;
import io.aether.api.clientApi.ClientApiUnsafe;
import io.aether.api.serverRegistryApi.CryptType;
import io.aether.api.serverRegistryApi.RootApi;
import io.aether.api.serverRegistryApi.WorkProofUtil;
import io.aether.client.AetherClientFactory;
import io.aether.common.*;
import io.aether.net.ApiProcessorConsumer;
import io.aether.net.Protocol;
import io.aether.net.ProtocolConfig;
import io.aether.net.RemoteApi;
import io.aether.net.impl.bin.DeserializerStaticBytes;
import io.aether.sodium.AsymCrypt;
import io.aether.utils.futures.AFuture;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.List;
import java.util.UUID;

public class ConnectionForRegistration extends DataPrepareApiImpl<ClientApiSafe> implements ClientApiUnsafe, ApiProcessorConsumer {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final AetherCloudClient client;
	private final AFuture keysFuture = new AFuture();
	AFuture connectFuture;
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
			var keys = p.getRemoteApi().getKeys(CryptType.CURVE25519, SignType.AE_ED25519)
					.updateDeserializer(d -> {
						d.getSub(DeserializerStaticBytes.class, "key").setLength(CryptType.CURVE25519.size);
						d.getSub(DeserializerStaticBytes.class, "sign").setLength(SignType.AE_ED25519.size);
					});
			DataPrepareApi.prepareRemote(p.getRemoteApi(), getConfig());
			keys.to((signedKey) -> {
				var config = getConfig();
				if (!client.getClientConfig().globalSigner.check(signedKey.toSignedKeyPlain(KeyType.CURVE25519, SignType.AE_ED25519))) {
					throw new RuntimeException();
				}
				config.asymCrypt = new AsymCrypt(Key.of(KeyType.CURVE25519, signedKey.key()));
				var safeApi = protocol.getRemoteApi().curve25519();
				safeApi.requestWorkProofData2(client.getParent(), CryptType.CURVE25519, SignType.AE_ED25519)
						.updateDeserializer(d -> {
							var globalKey = d.getSub("globalKey");
							globalKey.getSub(DeserializerStaticBytes.class, "key").setLength(CryptType.CURVE25519.size);
							globalKey.getSub(DeserializerStaticBytes.class, "sign").setLength(SignType.AE_ED25519.size);
						})
						.to(wpd -> {
							var passwords = WorkProofUtil.generateProofOfWorkPool(
									wpd.salt(),
									wpd.suffix(),
									wpd.maxHashVal(),
									wpd.poolSize(),
									5000);
							RemoteApi.of(protocol.getRemoteApi()).onMethodInvoke(m -> {
							});
							var globalClientApi = protocol.getRemoteApi().curve25519()
									.registration(client.getParent(), wpd.salt(), wpd.suffix(), passwords, ).curve25519();
							globalClientApi.setMasterKey(client.getMasterKey().toTypedKey());
							globalClientApi.requestCloud();
							protocol.flush();
						});
				protocol.flush();
			});
			p.flush();
		}).toFuture();
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
	private static class MyClientApiSafe implements ClientApiSafe {
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
	}
}
