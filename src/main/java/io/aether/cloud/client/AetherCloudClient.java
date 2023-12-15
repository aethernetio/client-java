package io.aether.cloud.client;

import io.aether.api.serverRegistryApi.RegistrationResponse;
import io.aether.common.Cloud;
import io.aether.common.Key;
import io.aether.common.Message;
import io.aether.common.ServerDescriptor;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static io.aether.utils.streams.AStream.streamOf;

public final class AetherCloudClient {
	public static final ADebug.Counter getConnectionBegin = ADebug.key("getConnectionBegin");
	public static final ADebug.Counter getConnectionEnd = ADebug.key("getConnectionEnd");
	public static final ADebug.Counter getUserPositionBegin = ADebug.key("getUserPositionBegin");
	public static final ADebug.Counter getUserPositionEnd = ADebug.key("getUserPositionEnd");
	public static final ADebug.Counter requestPositionBegin = ADebug.key("requestPositionBegin");
	public static final ADebug.Counter requestPositionEnd = ADebug.key("requestPositionEnd");
	private static final Logger log = LoggerFactory.getLogger(AetherCloudClient.class);
	public final AtomicBoolean beginCreateUser = new AtomicBoolean();
	public final SlotConsumer<Message> onMessage = new SlotConsumer<>();
	public final AFuture startFuture = new AFuture();
	final Map<Integer, Connection> connections = new ConcurrentHashMap<>();
	final Map<UUID, EventSourceConsumer<Cloud>> clouds = new ConcurrentHashMap<>();
	final AtomicBoolean tryReg = new AtomicBoolean();
	final Set<UUID> requestClientClouds = new ConcurrentHashSet<>();
	final Set<Integer> requestsResolveServers = new ConcurrentHashSet<>();
	final AtomicBoolean successfulAuthorization = new AtomicBoolean();
	final Map<Integer, ARFuture<ServerDescriptorOnClient>> resolvedServers = new ConcurrentHashMap<>();
	final AFuture registrationFuture = new AFuture();
	private final Map<UUID, AtomicInteger> idCounters = new ConcurrentHashMap<>();
	private final StoreWrap storeWrap;
	private final Collection<ScheduledFuture<?>> scheduledFutures = new HashSet<>();
	public SlotConsumer<Collection<UUID>> onNewChildren = new SlotConsumer<>();
	volatile Connection currentConnection;
	volatile long pingTime = -1;
	Key masterKey;
	private String name;
	public AetherCloudClient(UUID parent) {
		this(new StoreWrap(new StoreDefault()) {
			{
				parentUid.set(parent);
			}
		});
	}
	public AetherCloudClient(StoreWrap store) {
		this.storeWrap = store;
		connect();
	}
	public AetherCloudClient(Store store) {
		this(new StoreWrap(store));
	}
	public static AetherCloudClient start(@NotNull Store store) {
		return new AetherCloudClient(store);
	}
	public Map<Integer, ARFuture<ServerDescriptorOnClient>> getResolvedServers() {
		return resolvedServers;
	}
	public Set<UUID> getRequestClientClouds() {
		return requestClientClouds;
	}
	public Set<Integer> getRequestsResolveServers() {
		return requestsResolveServers;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
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
		putDescriptor(serverDescriptor);
		var c = connections.get(serverDescriptor.getId());
		if (c == null) {
			c = connections.computeIfAbsent(serverDescriptor.getId(),
					s -> new Connection(this, serverDescriptor));
		}
		return c;
	}
	void startScheduledTask() {
		RU.scheduleAtFixedRate(scheduledFutures, getPingTime(), TimeUnit.MILLISECONDS, () -> {
//			getConnection(Connection::scheduledWork);
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
	public void changeCloud(Cloud cloud) {
		var uid = getUid();
		assert uid != null;
		updateCloud(uid, cloud);
	}
	public void connect() {
		startScheduledTask();
		if (!isRegistered() && tryReg.compareAndSet(false, true)) {
			var uris = storeWrap.cloudFactoryUrl.get();
			var countServersForRegistration = storeWrap.countServersForRegistration.get(1);
			log.info("try registration by: {}", uris);
			streamOf(uris).shuffle().limit(countServersForRegistration)
					.to(sd -> new ConnectionForRegistration(this, sd));
		} else {
			var cloud = storeWrap.getCloud(getUid());
			if (cloud == null || cloud.length == 0) throw new UnsupportedOperationException();
			for (var serverId : cloud) {
				getConnection(storeWrap.getServerDescriptor(serverId, getMasterKey()));
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
	EventSourceConsumer<Cloud> getCloud(@NotNull UUID uid) {
		return clouds.computeIfAbsent(uid, k -> {
			if (requestClientClouds.add(uid)) {
				if (!Objects.equals(uid, getUid())) requestPositionBegin.add();
			}
			return new EventSourceConsumer<>();
		});
	}
	public void updateCloud(@NotNull UUID uid, @NotNull Cloud serverIds) {
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
		return storeWrap.uid.get(null) != null;
	}
	public void setCurrentConnection(@NotNull Connection connection) {
		currentConnection = connection;
		connections.put(connection.getServerDescriptor().getId(), connection);
	}
	public void confirmRegistration(RegistrationResponse cd) {
		if (!successfulAuthorization.compareAndSet(false, true)) return;
		log.trace("confirmRegistration: " + cd);
		storeWrap.uid.set(cd.uid());
		beginCreateUser.set(false);
		registrationFuture.done();
		assert isRegistered();
		streamOf(cd.cloud()).map(sd -> getConnection(ServerDescriptorOnClient.of(sd)).conFuture.toFuture()).allMap(AFuture::all).to(startFuture::tryDone);
	}
	public void updateCloud(@NotNull UUID uid, @NotNull ServerDescriptorOnClient @NotNull [] cloud) {
		if (uid.equals(getUid())) {
			currentConnection = null;
		}
		for (var s : cloud) {
			resolveServer(s.getId()).tryDone(s);
		}
		getCloud(uid).set(Cloud.of(streamOf(cloud).mapToInt(ServerDescriptorOnClient::getId).toShortArray()));
	}
	public AFuture sendMessage(@NotNull UUID address, byte[] data) {
		assert address != null;
		assert data != null;
		return sendMessage(new Message(nextMsgId(address), address, RU.time(), data));
	}
	public AFuture sendMessage(@NotNull Message message) {
		MessageRequest msgr = new MessageRequest(this, message);
		AFuture res = new AFuture();
		msgr.onEvent(e -> {
			if (e.status() == MessageRequest.Status.DONE) {
				res.tryDone();
			}
		});
		msgr.requestByDefaultStrategy();
		return res;
	}
	public AFuture stop(int secondsTimeOut) {
		streamOf(scheduledFutures).foreach(f -> f.cancel(true));
		return streamOf(connections.values()).map(c -> c.close(secondsTimeOut)).allMap(AFuture::all);
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
	public void onMessageRemove(AConsumer<Message> listener) {
		onMessage.remove(listener);
	}
	public UUID getParent() {
		return storeWrap.parentUid.get();
	}
	public Key getMasterKey() {
		Key res;
		res = masterKey;
		if (res != null) return res;
		res = storeWrap.masterKey.get(null);
		if (res == null) {
			res = ChaCha20Poly1305.generateSyncKey();
			storeWrap.masterKey.set(res);
		}
		masterKey = res;
		return res;
	}
	public AetherCloudClient waitStart(int timeout) {
		startFuture.waitDoneSeconds(timeout);
		return this;
	}
	private void putDescriptor(ServerDescriptorOnClient sd) {
		resolvedServers.computeIfAbsent(sd.getId(), k -> new ARFuture<>())
				.done(sd);
		requestsResolveServers.remove(sd.getId());
	}
	public void putServerDescriptor(ServerDescriptor sd) {
		var f = resolvedServers.computeIfAbsent((int) sd.id(), k -> new ARFuture<>());
		if (!f.tryDone(ServerDescriptorOnClient.of(sd, getMasterKey()))) {
			var sdc = f.get();
			sdc.setServerDescriptor(sd, getMasterKey());
		}
	}
}
