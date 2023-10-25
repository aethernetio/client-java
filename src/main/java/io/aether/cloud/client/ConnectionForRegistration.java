package io.aether.cloud.client;

import io.aether.api.clientApi.ClientApiSafe;
import io.aether.api.clientApi.ClientApiUnsafe;
import io.aether.api.serverApi.RegistrationRequest;
import io.aether.api.serverApi.ServerApiUnsafe;
import io.aether.client.AetherClientFactory;
import io.aether.common.AetherCodec;
import io.aether.common.Message;
import io.aether.common.ServerDescriptor;
import io.aether.common.SignedKey;
import io.aether.net.ApiProcessorConsumer;
import io.aether.net.Protocol;
import io.aether.net.ProtocolConfig;
import io.aether.net.RemoteApi;
import io.aether.net.impl.bin.ApiProcessor;
import io.aether.sodium.AsymCrypt;
import io.aether.sodium.ChaCha20Poly1305Pair;
import io.aether.sodium.Nonce;
import io.aether.utils.DataInOutStatic;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.VarHandle;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ConnectionForRegistration implements ClientApiUnsafe, ApiProcessorConsumer {
	private static final Logger log = LoggerFactory.getLogger(ConnectionForRegistration.class);
	private final URI uri;
	private final AetherCloudClient client;
	int serverId;
	private volatile AsymCrypt asymCryptByServerCrypt;
	private volatile ChaCha20Poly1305Pair chaCha20Poly1305;
	private Protocol<ClientApiUnsafe, ServerApiUnsafe> protocol;
	private SignedKey serverAsymPublicKey;
	public ConnectionForRegistration(AetherCloudClient client, URI uri) {
		this.client = client;
		this.uri = uri;
		short randomValue = (short) RU.RND.nextInt(Short.MIN_VALUE, Short.MAX_VALUE);
		var con = AetherClientFactory.make(uri,
				ProtocolConfig.of(ClientApiUnsafe.class, ServerApiUnsafe.class, AetherCodec.BINARY),
				(p) -> {
					protocol = p;
					((RemoteApi) protocol.getRemoteApi()).setOnSubApi(a -> {
						if (Objects.equals(a.methodName, "cryptBoxByServerKey")) {
							a.setDataPreparer(d -> {
								if (asymCryptByServerCrypt == null) {
									asymCryptByServerCrypt = new AsymCrypt(serverAsymPublicKey.key());
								}
								var v = d.toArray();
								var encoded = asymCryptByServerCrypt.encode(v);
								return new DataInOutStatic(encoded);
							});
						} else {
							throw new UnsupportedOperationException();
						}
					});
					return this;
				});
		con.to((p) -> {
			AFuture checkPublicAsymKey;
			if (serverAsymPublicKey == null) {
				checkPublicAsymKey = new AFuture();
				p.getRemoteApi().info(randomValue).to(info -> {
					if (info.clientRandomValue() != randomValue) throw new RuntimeException("Bad return random value");
//					info.randomProof();//TODO
					client.putDescriptor(info.serverDescriptor());
					serverAsymPublicKey = info.serverDescriptor().publicKey();
					VarHandle.fullFence();
					checkPublicAsymKey.done();
				}).onError((e) -> {
					log.info("Already registration: " + e.getMessage());
				});
				p.flush();
			} else {
				checkPublicAsymKey = AFuture.completed();
			}
			checkPublicAsymKey.to(() -> {
				protocol.getRemoteApi().cryptBoxByServerKey().registration2(new RegistrationRequest(
						(short) 0, client.getParent(), client.getMasterKey()
				)).to(client::confirmRegistration);
				protocol.flush();
			});
		});
	}
	@Override
	public void setApiProcessor(ApiProcessor apiProcessor) {
		apiProcessor.onExecuteCmdFromRemote = cmd -> {
			if (cmd.getMethod().getName().equals("chacha20poly1305")) {
				if (chaCha20Poly1305 == null) {
					chaCha20Poly1305 = ChaCha20Poly1305Pair.forClient(client.getMasterKey(), serverId, Nonce.of());
				}
				cmd.setSubApiBody(chaCha20Poly1305.decode(cmd.getSubApiBody()));
			}
		};
	}
	@Override
	public ClientApiSafe chacha20poly1305() {
		return new MyClientApiSafe();
	}
	private class MyClientApiSafe implements ClientApiSafe, ApiProcessorConsumer {
		@Override
		public void setApiProcessor(ApiProcessor apiProcessor) {
			apiProcessor.setApiResultConsumer(() -> {
				if (client.isRegistered()) {
					return protocol.getRemoteApi().chacha20poly1305(client.getUid());
				} else if (serverAsymPublicKey != null) {
					return protocol.getRemoteApi().cryptBoxByServerKey();
				} else {
					throw new UnsupportedOperationException();
				}
			});
		}
		@Override
		public void pushMessage(@NotNull Message message) {
			throw new UnsupportedOperationException();
		}
		@Override
		public void updatePosition(@NotNull UUID uid, int @NotNull [] cloud) {
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
