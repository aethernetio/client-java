package io.aether.cloud.client;

import io.aether.common.AetherCodec;
import io.aether.common.Message;
import io.aether.net.AetherApi;
import io.aether.net.Protocol;
import io.aether.net.ProtocolConfig;
import io.aether.utils.DataIn;
import io.aether.utils.DataInOut;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerOverMessages<LT extends AetherApi, RT extends AetherApi> {
	final ApiFactory<LT> apiFactory;
	private final AetherCloudClient aetherClient;
	private final Map<UUID, Connection> connections = new ConcurrentHashMap<>();
	private final ProtocolConfig<LT, RT> protocolConfig;
	public ServerOverMessages(AetherCloudClient aetherClient,
	                          Class<LT> localApiClass,
	                          Class<RT> remoteApiClass,
	                          ApiFactory<LT> apiFactory) {
		protocolConfig = ProtocolConfig.of(localApiClass, remoteApiClass, AetherCodec.BINARY);
		this.apiFactory = apiFactory;
		this.aetherClient = aetherClient;
		aetherClient.startFuture.to(() -> {
			aetherClient.onMessage.add(v -> getConnectionApiBy(v.uid()).protocol.putFromRemote(v.data()));
		});
	}
	public AetherCloudClient getAetherClient() {
		return aetherClient;
	}
	private Connection getConnectionApiBy(UUID uid) {
		return connections.computeIfAbsent(uid, Connection::new);
	}
	public LT getApiBy(UUID uid) {
		return getConnectionApiBy(uid).protocol.getLocalApi();
	}
	public interface ApiFactory<LT extends AetherApi> {
		LT get(UUID uid, Message message);
	}

	private class Connection {
		final Protocol<LT, RT> protocol;
		final UUID uid;
		private final DataInOut current = new DataInOut();
		public Connection(UUID uid) {
			var localApi = apiFactory.get(uid, null);
			this.uid = uid;
			this.protocol = new Protocol<>(protocolConfig, localApi) {
				@Override
				protected void flush0() {
					var data = current.toArrayCopy();
					current.clear();
					aetherClient.sendMessage(new Message(aetherClient.nextMsgId(uid), uid, System.currentTimeMillis(), data));
				}
				@Override
				public boolean isActive() {
					return aetherClient.isConnected();
				}
				@Override
				protected void cmdToRemote(DataIn data) {
					current.write(data);
				}
			};
		}
	}
}
