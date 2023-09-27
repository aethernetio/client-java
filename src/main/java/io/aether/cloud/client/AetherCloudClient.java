package io.aether.cloud.client;

import io.aether.common.*;
import io.aether.sodium.ChaCha20Poly1305;
import io.aether.utils.*;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.interfaces.AConsumer;
import io.aether.utils.slots.EventSourceConsumer;
import io.aether.utils.slots.SlotConsumer;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static io.aether.utils.streams.AStream.streamOf;

public final class AetherCloudClient {
	private static final Logger log= LoggerFactory.getLogger(AetherCloudClient.class);
	public static final ADebug.Counter getConnectionBegin = ADebug.key("getConnectionBegin");
	public static final ADebug.Counter getConnectionEnd = ADebug.key("getConnectionEnd");
	public static final ADebug.Counter getUserPositionBegin = ADebug.key("getUserPositionBegin");
	public static final ADebug.Counter getUserPositionEnd = ADebug.key("getUserPositionEnd");
	public static final ADebug.Counter requestPositionBegin = ADebug.key("requestPositionBegin");
	public static final ADebug.Counter requestPositionEnd = ADebug.key("requestPositionEnd");
	public final AtomicBoolean beginCreateUser = new AtomicBoolean();
	public final SlotConsumer<Message> onMessage = new SlotConsumer<>();
	public final AFuture startFuture = new AFuture();
	final Map<Integer, Connection> connections = new ConcurrentHashMap<>();
	final Map<UUID, EventSourceConsumer<int[]>> clouds = new ConcurrentHashMap<>();
	final AtomicBoolean tryReg = new AtomicBoolean();
	
	final Set<UUID> requestClientClouds = new ConcurrentHashSet<>();
	public Map<Integer, ARFuture<ServerDescriptorOnClient>> getResolvedServers() {
		return resolvedServers;
	}
	public Set<UUID> getRequestClientClouds() {
		return requestClientClouds;
	}
	final Set<Integer> requestsResolveServers = new ConcurrentHashSet<>();
	public Set<Integer> getRequestsResolveServers() {
		return requestsResolveServers;
	}
	final AtomicBoolean successfulAuthorization = new AtomicBoolean();
	
	final Map<Integer, ARFuture<ServerDescriptorOnClient>> resolvedServers = new ConcurrentHashMap<>();
	private final Map<UUID, AtomicInteger> idCounters = new ConcurrentHashMap<>();
	final AFuture registrationFuture = new AFuture();
	private final StoreWrap storeWrap;
	private final Collection<ScheduledFuture<?>> scheduledFutures = new HashSet<>();
	public SlotConsumer<Collection<UUID>> onNewChildren = new SlotConsumer<>();
	volatile Connection currentConnection;
	private String name;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	volatile long pingTime = -1;
	public AetherCloudClient() {
		this(new StoreDefault());
	}
	public AetherCloudClient(Store store) {
		this.storeWrap = new StoreWrap(store);
		connect();
	}
	public static AetherCloudClient start(@NotNull Store store) {
		return new AetherCloudClient(store);
	}
	public ARFuture<ServerDescriptorOnClient> resolveServer(int serverId) {
		return resolvedServers.computeIfAbsent(serverId, i -> {
			requestsResolveServers.add(i);
			return new ARFuture<>();
		});
	}
	@ThreadSafe
	public void getServerForUid(@NotNull UUID uid, AConsumer<ServerDescriptorOnClient> t) {
		if (uid != getUid()) getUserPositionBegin.add();
		getCloud(uid).run(10, p -> {
			if (uid != getUid()) getUserPositionEnd.add();
			if (requestClientClouds.remove(uid)) {
				requestPositionEnd.add();
			}
			for (var pp : p) {
				resolveServer(pp).to(t);
			}
		});
	}
	void getConnection(@NotNull UUID uid, @NotNull AConsumer<Connection> t) {
		if (uid.equals(getUid()) && currentConnection != null) {
			t.accept(currentConnection);
			return;
		}
		if (uid != getUid()) getConnectionBegin.add();
		if (ADebug.DEBUG) {
			var to = TimeoutChecker.error20();
			getServerForUid(uid, sd -> {
				if (uid != getUid()) getConnectionEnd.add();
				to.done();
				var c = getConnection(sd);
				t.accept(c);
			});
		} else {
			getServerForUid(uid, sd -> {
				if (uid != getUid()) getConnectionEnd.add();
				t.accept(getConnection(sd));
			});
		}
	}
	Connection getConnection(@NotNull ServerDescriptorOnClient serverDescriptor) {
		var c = connections.get(serverDescriptor.id);
		if (c == null) {
			c = connections.computeIfAbsent(serverDescriptor.id, s -> new Connection(this, serverDescriptor));
		}
		return c;
	}
	void startScheduledTask() {
		RU.scheduleAtFixedRate(scheduledFutures, getPingTime(), TimeUnit.MILLISECONDS, () -> {
			for (Connection connection : getConnections()) {
				connection.scheduledWork();
			}
		});
		RU.scheduleAtFixedRate(scheduledFutures, 3, TimeUnit.SECONDS, () -> {
			for (Connection connection : getConnections()) {
				connection.clearRequests();
			}
		});
	}
	public void ping() {
		getConnection(Connection::scheduledWork);
	}
	public void changeCloud(int[] cloud) {
		var uid = getUid();
		assert uid != null;
		updateCloud(uid, cloud);
	}
	public boolean tryFirstConnection() {
		return !isRegistered() && tryReg.compareAndSet(false, true);
	}
	public void connect() {
		startScheduledTask();
		if (!isRegistered()) {
			try {
				var defaultPortBinary = storeWrap.getDefaultPortForCodec(AetherCodec.BINARY);
				var defaultPortWebsocket = storeWrap.getDefaultPortForCodec(AetherCodec.WEBSOCKET);
				var addresses = InetAddress.getAllByName(storeWrap.cloudFactoryUrl.get());
				var countServersForRegistration = storeWrap.countServersForRegistration.get(2);
				streamOf(addresses)
						.map(a -> new ServerDescriptorOnClient(
								0,
								List.of(IPAddress.of(a.getAddress())),
								List.of(new CoderAndPort(AetherCodec.BINARY, defaultPortBinary),
										new CoderAndPort(AetherCodec.WEBSOCKET, defaultPortWebsocket))
						))
						.ifEmpty(() -> {
							throw new RuntimeException(new UnknownHostException(storeWrap.cloudFactoryUrl.get()));
						})
						.shuffle()
						.limit(countServersForRegistration)
						.to(sd -> {
							new ConnectionForRegistration(this, sd);
						});
			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			}
		} else {
			var cloud = storeWrap.getCloud(getUid());
			if (cloud == null || cloud.length == 0) throw new UnsupportedOperationException();
			for (var serverId : cloud) {
				getConnection(storeWrap.getServerDescriptor(serverId));
			}
		}
	}
	public UUID getUid() {
		return storeWrap.uid.get();
	}
	void receiveMessages(@NotNull Collection<Message> list) {
		for (var m : list) {
			onMessage.fire(m);
		}
	}
	void receiveMessage(@NotNull Message msg) {
		receiveMessages(ObjectList.of(msg));
	}
	void getConnection(@NotNull AConsumer<Connection> t) {
		if (currentConnection != null) {
			t.accept(currentConnection);
		} else {
			getConnection(Objects.requireNonNull(getUid()), c -> {
				c.setBasic(true);
				t.accept(c);
			});
		}
	}
	public Collection<Connection> getConnections() {
		return connections.values();
	}
	EventSourceConsumer<int[]> getCloud(@NotNull UUID uid) {
		return clouds.computeIfAbsent(uid, k -> {
			if (requestClientClouds.add(uid)) {
				if (!Objects.equals(uid, getUid())) requestPositionBegin.add();
				getConnection(Connection::scheduledWork);
			}
			return new EventSourceConsumer<>();
		});
	}
	public void updateCloud(@NotNull UUID uid, int @NotNull [] serverIds) {
		storeWrap.setCloud(uid, serverIds);
		if (uid.equals(getUid())) {
			currentConnection = null;
		}
		getCloud(uid).set(serverIds);
	}
	public long getPingTime() {
		if (pingTime == -1) {
			pingTime = storeWrap.pingDuration.get(1000);
		}
		return pingTime;
	}
	public boolean isRegistered() {
		return getUid() != null;
	}
	public void setCurrentConnection(@NotNull Connection connection) {
		currentConnection = connection;
		connections.put(connection.getServerDescriptor().id, connection);
	}
	public void confirmRegistration(@NotNull ClientDescriptorForReg cd) {
		if (!successfulAuthorization.compareAndSet(false, true)) return;
		if (log.isTraceEnabled()) log.trace("confirmRegistration");
		storeWrap.uid.set(cd.uid());
		beginCreateUser.set(false);
		assert isRegistered();
		registrationFuture.done();
	}
	public void updateCloud(@NotNull UUID uid, @NotNull ServerDescriptorOnClient @NotNull [] cloud) {
		if (uid.equals(getUid())) {
			currentConnection = null;
		}
		for (var s : cloud) {
			resolveServer(s.id).tryDone(s);
		}
		getCloud(uid)
				.set(streamOf(cloud)
						.mapToInt(ServerDescriptorOnClient::getId)
						.toArray());
	}
	public AFuture sendMessage(@NotNull UUID address, byte[] data) {
		assert address != null;
		assert data != null;
		return sendMessage(new Message(nextMsgId(address), address, System.currentTimeMillis(), data));
	}
	public AFuture sendMessage(@NotNull Message message) {
		MessageRequest msgr = new MessageRequest(this, message);
		AFuture res = new AFuture();
		msgr.onEvent(e -> {
			if (e.status() == MessageRequest.Status.DONE) {
				res.done();
			}
		});
		msgr.requestByDefaultStrategy();
		return res;
	}
	public AFuture stop(int secondsTimeOut) {
		streamOf(scheduledFutures).foreach(f -> f.cancel(true));
		return streamOf(connections.values())
				.map(c -> c.close(secondsTimeOut))
				.allMap(AFuture::all);
	}
	public int nextMsgId(UUID uid) {
		return idCounters.computeIfAbsent(uid, k -> new AtomicInteger()).incrementAndGet();
	}
	public boolean isConnected() {
		return getUid() != null;
	}
	public void onMessage(AConsumer<Message> consumer) {
		onMessage.add(consumer);
	}
	public void removeOnMessage(AConsumer<Message> listener) {
		onMessage.remove(listener);
	}
	public UUID getParent() {
		return storeWrap.parentUid.get();
	}
	public Key getMasterKey() {
		return storeWrap.masterKey.get();
	}
	public static class StoreWrap {
		public final Store.PropertyLong pingDuration;
		public final Store.Property<UUID> uid;
		public final Store.Property<UUID> parentUid;
		public final Store.Property<Key> masterKey;
		public final Store.Property<String> cloudFactoryUrl;
		public final Store.PropertyInt countServersForRegistration;
		private final Store store;
		public StoreWrap(Store store) {
			this.store = store;
			pingDuration = store.getPropertyLong("settings.ping.duration");
			countServersForRegistration = store.getPropertyInt("settings.countServersForRegistration");
			uid = store.getProperty("main.uid", UUID::fromString);
			parentUid = store.getProperty("main.parentUid", UUID::fromString);
			masterKey = store.getProperty("main.masterKey", Key::of);
			cloudFactoryUrl = store.getProperty("main.url.cloud");
		}
		public void setServerDescriptor(ServerDescriptorOnClient serverDescriptor) {
			var prefix = "main.servers." + serverDescriptor.getId() + ".";
			store.set(prefix + "clientKey", serverDescriptor.clientKey.toHexString());
			store.set(prefix + "clientNonce", serverDescriptor.nonce.toHexString());
			store.set(prefix + "serverKey", serverDescriptor.serverKey.toHexString());
			store.set(prefix + "serverAsymPublicKey", serverDescriptor.serverAsymPublicKey.toHexString());
			store.set(prefix + "ipAddresses", streamOf(serverDescriptor.ipAddress).joinD());
			store.set(prefix + "codersAndPorts", streamOf(serverDescriptor.codersAndPorts).joinD());
		}
		public ServerDescriptorOnClient getServerDescriptor(int serverId) {
			var prefix = "main.servers." + serverId + ".";
			var res = new ServerDescriptorOnClient();
			res.id = serverId;
			res.clientKey = store.get(prefix + "clientKey", null, Key::of);
			res.serverKey = store.get(prefix + "serverKey", null, Key::of);
			res.serverAsymPublicKey = store.get(prefix + "serverAsymPublicKey", null, Key::of);
			res.nonce = store.get(prefix + "clientNonce", null, ChaCha20Poly1305.Nonce::of);
			res.keyAndNonce = new ChaCha20Poly1305.KeyAndNonce(res.clientKey, res.nonce);
			var ipAddresses = streamOf(store.get(prefix + "ipAddresses", "").split(",")).map(IPAddress::of).toList();
			List<CoderAndPort> codersAndPorts = streamOf(store.get(prefix + "codersAndPorts", "").split(",")).map(e -> {
				var ee = e.split(":");
				return new CoderAndPort(AetherCodec.valueOf(ee[0]), Integer.parseInt(ee[1]));
			}).toList();
			res.ipAddress = ipAddresses;
			res.codersAndPorts = codersAndPorts;
			return res;
		}
		public void setDefaultPortForCodec(AetherCodec codec, int port) {
			store.set("main.protocol." + codec.getName() + ".defaultPort", port);
		}
		public int getDefaultPortForCodec(AetherCodec codec) {
			return store.get("main.protocol." + codec.getName() + ".defaultPort", codec.getNetworkConfigurator().getDefaultPort());
		}
		public void setCloud(UUID uid, int[] cloud) {
			if (cloud == null) {
				store.delete("main.clouds." + uid);
			} else {
				store.set("main.clouds." + uid, streamOf(cloud).join(","));
			}
		}
		public int[] getCloud(UUID uid) {
			return store.get("main.clouds." + uid, s -> {
				if (s == null) return null;
				return streamOf(s.split(",")).mapToInt(Integer::parseInt).toArray();
			});
		}
	}
}
