package com.aether.cloud.client;

import com.aether.api.clientApi.ClientApiSafe;
import com.aether.api.clientApi.ClientApiUnsafe;
import com.aether.api.serverApi.RegistrationRequest;
import com.aether.api.serverApi.ServerUnsafeApi;
import com.aether.client.AetherClientFactory;
import com.aether.common.AetherCodec;
import com.aether.common.Message;
import com.aether.common.ServerDescriptor;
import com.aether.net.*;
import com.aether.net.coders.CmdInvoke;
import com.aether.sodium.AsymCrypt;
import com.aether.sodium.ChaCha20Poly1305;
import com.aether.utils.DataInOutStatic;
import com.aether.utils.RU;
import com.aether.utils.futures.AFuture;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ConnectionForRegistration implements ClientApiUnsafe, AetherApiLocal {
	private static final Logger log= LoggerFactory.getLogger(ConnectionForRegistration.class);
	private final ServerDescriptorOnClient serverDescriptor;
	private final AetherCloudClient client;
	private volatile AsymCrypt asymCryptByServerCrypt;
	private volatile ChaCha20Poly1305 chaCha20Poly1305;
	private Protocol<ClientApiUnsafe, ServerUnsafeApi> protocol;
	public ConnectionForRegistration(AetherCloudClient client, ServerDescriptorOnClient serverDescriptor) {
		this.client = client;
		this.serverDescriptor = serverDescriptor;
		short randomValue = (short) RU.RND.nextInt(Short.MIN_VALUE, Short.MAX_VALUE);
		var address = serverDescriptor.ipAddress.get(0).toInetSocketAddress(AetherCodec.BINARY.getNetworkConfigurator().getDefaultPort());
		var con = AetherClientFactory.make(address,
				ProtocolConfig.of(ClientApiUnsafe.class, ServerUnsafeApi.class, AetherCodec.BINARY),
				(p) -> {
					protocol = p;
					((RemoteApi) protocol.getRemoteApi()).setOnSubApi(a -> {
						if (Objects.equals(a.methodName, "cryptBoxByServerKey")) {
							a.setDataPreparer(d -> {
								if (asymCryptByServerCrypt == null) {
									asymCryptByServerCrypt = new AsymCrypt(serverDescriptor.getServerAsymPublicKey());
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
			if (serverDescriptor.serverAsymPublicKey == null || serverDescriptor.id == 0) {
				checkPublicAsymKey = new AFuture();
				p.getRemoteApi().info(randomValue).to(info -> {
					if (info.clientRandomValue() != randomValue) throw new RuntimeException("Bad return random value");
//					info.randomProof();//TODO
					serverDescriptor.serverAsymPublicKey = info.serverPublicKey();
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
	public Object executeCmdFromRemote(CmdInvoke cmd) {
		if (cmd.getMethod().getName().equals("chacha20poly1305")) {
			if (chaCha20Poly1305 == null) {
				assert serverDescriptor.id != 0;
				serverDescriptor.initClientKeyAndNonce(client.getMasterKey());
				chaCha20Poly1305 = new ChaCha20Poly1305(serverDescriptor.keyAndNonce);
			}
			cmd.setSubApiBody(chaCha20Poly1305.decode(cmd.getSubApiBody()));
		}
		return AetherApiLocal.super.executeCmdFromRemote(cmd);
	}
	@Override
	public void sendExceptionToRemote(ExceptionUnit unit) {
	}
	@Override
	public void sendResultToRemote(ResultUnit unit) {
	}
	@Override
	public ClientApiSafe chacha20poly1305() {
		return new MyClientApiSafe();
	}
	private class MyClientApiSafe implements ClientApiSafe, AetherApiLocal {
		@Override
		public void sendExceptionToRemote(ExceptionUnit unit) {
			if (client.isRegistered()) {
				protocol.getRemoteApi().chacha20poly1305(client.getUid())
						.sendException(unit);
			} else if (serverDescriptor.serverAsymPublicKey != null) {
				protocol.getRemoteApi().cryptBoxByServerKey()
						.sendException(unit);
			} else {
				throw unit.exception();
			}
		}
		@Override
		public void sendResultToRemote(ResultUnit unit) {
			if (client.isRegistered()) {
				protocol.getRemoteApi().chacha20poly1305(client.getUid())
						.sendResult(unit);
			} else if (serverDescriptor.serverAsymPublicKey != null) {
				protocol.getRemoteApi().cryptBoxByServerKey()
						.sendResult(unit);
			} else {
				throw new UnsupportedOperationException();
			}
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
