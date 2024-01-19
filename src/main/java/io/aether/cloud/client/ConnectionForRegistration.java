package io.aether.cloud.client;

import io.aether.api.DataPrepareApi;
import io.aether.api.DataPrepareApiImpl;
import io.aether.api.clientApi.ClientApiSafe;
import io.aether.api.clientApi.ClientApiUnsafe;
import io.aether.api.serverRegistryApi.RegistrationRequest;
import io.aether.api.serverRegistryApi.RootApi;
import io.aether.api.serverRegistryApi.WorkProofDTO;
import io.aether.api.serverRegistryApi.WorkProofUtil;
import io.aether.client.AetherClientFactory;
import io.aether.common.*;
import io.aether.net.ApiProcessorConsumer;
import io.aether.net.Protocol;
import io.aether.net.ProtocolConfig;
import io.aether.net.RemoteApi;
import io.aether.net.impl.bin.ApiProcessor;
import io.aether.sodium.AsymCrypt;
import io.aether.utils.futures.AFuture;
import io.aether.utils.streams.AStream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class ConnectionForRegistration extends DataPrepareApiImpl<ClientApiSafe> implements ClientApiUnsafe, ApiProcessorConsumer {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final AetherCloudClient client;
	private final AFuture keysFuture = new AFuture();
	private Protocol<ClientApiUnsafe, RootApi> protocol;
	private ClientApiSafe clientApiSafe;
	public ConnectionForRegistration(AetherCloudClient client, URI uri) {
		setSubApiFactory(this::getClientApiSafe);
		this.client = client;
		log.debug("try reg to: " + uri);
		var con = AetherClientFactory.make(uri,
				ProtocolConfig.of(ClientApiUnsafe.class, RootApi.class, AetherCodec.BINARY),
				(p) -> {
					protocol = p;
					return this;
				});
		con.to((p) -> {
			var keys = p.getRemoteApi().getKeys(Set.of(KeyType.CURVE25519, KeyType.SODIUM_SIGN), Set.of(KeyType.SODIUM_SIGN));
			keys.to((kk) -> {
				var mapKey = AStream.streamOf(kk.keys()).toMap(KeyRecord::type, k -> k);
				var c = getConfig();
				c.asymCrypt = new AsymCrypt(mapKey.get(KeyType.CURVE25519).signedKey().key());
				c.signer = SignChecker.of(mapKey.get(KeyType.SODIUM_SIGN).signedKey().key());
				var safeApi = protocol.getRemoteApi().enter().curve25519();
				safeApi.requestWorkProofData(client.getParent())
						.to(wpd -> {
							var passwords = WorkProofUtil.generateProofOfWorkPool(wpd.proofSalt(), wpd.config().maxHashVal(), (int) wpd.config().poolSize(), (int) wpd.config().costBCrypt(),
									5000);
							protocol.getRemoteApi().enter().curve25519()
									.registration(passwords, new RegistrationRequest(client.getMasterKey()))
									.to(client::confirmRegistration);
							protocol.flush();
						});
				protocol.flush();
			});
			p.flush();
		});
	}
	@Override
	public void setApiProcessor(ApiProcessor apiProcessor) {
		super.setApiProcessor(apiProcessor);
		var remoteApi = (RootApi) apiProcessor.getRemoteApi();
		((RemoteApi) remoteApi).setOnSubApi(a -> {
			switch (a.methodName) {
				case "enter" -> DataPrepareApi.prepareRemote((DataPrepareApi<?>) a, getConfig());
			}
		});
	}
	public ClientApiSafe getClientApiSafe() {
		if (clientApiSafe == null) clientApiSafe = new MyClientApiSafe();
		return clientApiSafe;
	}
	@Override
	public void sendServerKeys(SignedKey asymPublicKey, SignedKey signKey) {
		//TODO check
		this.getConfig().signer = SignChecker.of(signKey.key());
		this.getConfig().asymCrypt = new AsymCrypt(asymPublicKey.key());
		keysFuture.done();
	}
	private List<byte[]> workProof(WorkProofDTO d) {
		try {
			return WorkProofUtil.generateProofOfWorkPool(d.proofSalt(), d.config().maxHashVal(), d.config().poolSize(), d.config().costBCrypt(), 3000);
		} catch (TimeoutException e) {
			throw new RuntimeException(e);
		}
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
