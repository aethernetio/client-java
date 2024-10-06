package io.aether.cloud.client;

import io.aether.common.AetherCodec;
import io.aether.common.Message;
import io.aether.net.AConnection;
import io.aether.net.AConnectionConfig;
import io.aether.utils.DataIn;
import io.aether.utils.DataInOut;
import io.aether.utils.RU;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

public class ServerOverMessages<LT, RT> {
	final ApiFactory<LT> apiFactory;
	private final AetherCloudClient aetherClient;
	private final Map<UUID, Connection> connections = new ConcurrentHashMap<>();
	private final AConnectionConfig<LT, RT> connectionConfig;
	public ServerOverMessages(AetherCloudClient aetherClient,
	                          Class<LT> localApiClass,
	                          Class<RT> remoteApiClass,
	                          ApiFactory<LT> apiFactory) {
		if (!localApiClass.isInterface()) throw new RuntimeException("localApiClass is not interface");
		if (!remoteApiClass.isInterface()) throw new RuntimeException("remoteApiClass is not interface");
		connectionConfig = AConnectionConfig.of(localApiClass, remoteApiClass, AetherCodec.BINARY);
		this.apiFactory = requireNonNull(apiFactory);
		this.aetherClient = requireNonNull(aetherClient);
		aetherClient.startFuture.to(() -> {
			aetherClient.onMessage.add(v -> getConnectionApiBy(v.uid()).AConnection.putFromRemote(v.data()));
		});
	}
	public AetherCloudClient getAetherClient() {
		return aetherClient;
	}
	private Connection getConnectionApiBy(UUID uid) {
		return connections.computeIfAbsent(uid, Connection::new);
	}
	public LT getLocalApiBy(UUID uid) {
		return getConnectionApiBy(uid).AConnection.getLocalApi();
	}
	public RT getRemoteApiBy(UUID uid) {
		return getConnectionApiBy(uid).AConnection.getRemoteApi();
	}
	public interface ApiFactory<LT> {
		LT get(UUID uid, Message message);
	}

	private class Connection {
		final io.aether.net.AConnection<LT, RT> AConnection;
		final UUID uid;
		private final DataInOut current = new DataInOut();
		public Connection(UUID uid) {
			var localApi = apiFactory.get(uid, null);
			this.uid = uid;
			this.AConnection = new AConnection<>(connectionConfig, localApi) {
				@Override
				protected void flush0() {
					var data = current.toArrayCopy();
					current.clear();
					aetherClient.sendMessage(new Message(aetherClient.nextMsgId(uid), uid, RU.time(), data));
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
