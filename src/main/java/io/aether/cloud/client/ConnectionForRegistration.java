package io.aether.cloud.client;

import io.aether.api.DataPrepareApi;
import io.aether.api.DataPrepareApiImpl;
import io.aether.api.clientApi.ClientApiSafe;
import io.aether.api.clientApi.ClientApiUnsafe;
import io.aether.api.serverRegistryApi.RegistrationRequest;
import io.aether.api.serverRegistryApi.WorkProofApi;
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
import io.aether.utils.futures.ARFuture;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class ConnectionForRegistration extends DataPrepareApiImpl<ClientApiSafe> implements ClientApiUnsafe, ApiProcessorConsumer {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final AetherCloudClient client;
	private final AFuture keysFuture = new AFuture();
	private final UUID token;
	private Protocol<ClientApiUnsafe, WorkProofApi> protocol;
	private ARFuture<WorkProofDTO> workProofDTOFuture = new ARFuture<>();
	private ClientApiSafe clientApiSafe;
	public ConnectionForRegistration(AetherCloudClient client, URI uri) {
		setSubApiFactory(this::getClientApiSafe);
		this.client = client;
		token = UUID.randomUUID();
		var con = AetherClientFactory.make(uri,
				ProtocolConfig.of(ClientApiUnsafe.class, WorkProofApi.class, AetherCodec.BINARY),
				(p) -> {
					protocol = p;
					return this;
				});
		con.to((p) -> {
			p.getRemoteApi().byToken(token);
			p.flush();
			workProofDTOFuture.to(d -> {
				List<byte[]> listPasswords = List.of();//workProof(d);
				protocol.getRemoteApi().byTokenDone(token, listPasswords)
						.curve25519()
						.registration(new RegistrationRequest(
								client.getParent(),
								client.getMasterKey()
						)).to(client::confirmRegistration);
				protocol.flush();
			});
		});
	}
	@Override
	public void setApiProcessor(ApiProcessor apiProcessor) {
		super.setApiProcessor(apiProcessor);
		var remoteApi = (WorkProofApi) apiProcessor.getRemoteApi();
		((RemoteApi) remoteApi).setOnSubApi(a -> {
			switch (a.methodName) {
				case "byTokenDone" -> {
					DataPrepareApi.prepareRemote((DataPrepareApi<?>) a, getConfig());
				}
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
		this.getConfig().signer = SignChecker.of(signKey.key(), null);
		this.getConfig().asymCrypt = new AsymCrypt(asymPublicKey.key());
		keysFuture.done();
	}
	private List<byte[]> workProof(WorkProofDTO d) {
		try {
			return WorkProofUtil.generateProofOfWorkPool(d.proofSalt(), d.maxHashVal(), d.poolSize(), 3000);
		} catch (TimeoutException e) {
			throw new RuntimeException(e);
		}
	}
	private class MyClientApiSafe implements ClientApiSafe {
		@Override
		public void sendWorkProofData(WorkProofDTO workProofDTO) {
			workProofDTOFuture.done(workProofDTO);
		}
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
