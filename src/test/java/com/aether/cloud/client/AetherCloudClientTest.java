package com.aether.cloud.client;

import com.aether.cloud.server.AetherCloudServer;
import com.aether.common.Message;
import com.aether.common.ServerDescriptor;
import com.aether.dbservice.AetherDBService;
import com.aether.dbservice.DBServiceConstants;
import com.aether.utils.ADebug;
import com.aether.utils.ConcurrentHashSet;
import com.aether.utils.Store;
import com.aether.utils.StoreDefault;
import com.aether.utils.futures.AFuture;
import com.aether.utils.futures.ARFuture;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.aether.utils.streams.AStream.streamOf;

@Slf4j
class AetherCloudClientTest {
	private final static int DELAY = 100;
	private final static int CLIENTS_COUNT = 10;
	private final static int SERVER_PORT = 9192;
	private final static int SERVERS_COUNT = 1;
	private final static int MSG_COUNT = 1000;
	private final static int MSG_SIZE = 10;
	final byte[] transportData = "1".repeat(MSG_SIZE).getBytes(StandardCharsets.UTF_8);
	final Set<AetherCloudServer> servers = ObjectSets.synchronize(new ObjectOpenHashSet<>());
	final AtomicInteger serverPort = new AtomicInteger(SERVER_PORT);
	final AtomicInteger idmsg = new AtomicInteger(0);
	final Set<ServerDescriptor> serverDescriptors = new ObjectOpenHashSet<>();
	final List<Client> clients = ObjectLists.synchronize(new ObjectArrayList<>());
	final Store clientStore = new StoreDefault();
	ARFuture<AetherDBService> dbService;
	int totalMessagesSent;
	void makeServer() throws UnknownHostException {
		var server = AetherCloudServer.buildServer().get();
		var serverSettings = (StoreDefault) server.getSettings();
		serverSettings.set("main.url.cloud", "localhost");
		var port = serverPort.getAndIncrement();
		serverSettings.set("aether-server.tcp.port", port);
		servers.add(server);
		serverDescriptors.add(server.getDescriptor());
		server.start().timeout(10, () -> System.err.println("timeout create server")).waitDone();
	}
	void sendMsgs() throws InterruptedException {
		var begin = System.currentTimeMillis();
		log.info("Send messages");
		List<Client> rclients = new ObjectArrayList<>(clients);
		for (int i = 0; i < rclients.size(); i++) {
			Client cFrom = rclients.get(i);
			Client cTo;
			int cToIndex;
			if (i == rclients.size() - 1) {
				cTo = rclients.get(0);
			} else {
				cToIndex = i + 1;
				cTo = rclients.get(cToIndex);
			}
			for (var mi = 0; mi < MSG_COUNT; mi++) {
				var msgRequest = new Message(idmsg.incrementAndGet(),
						cTo.storeWrap.uid.get(),
						System.currentTimeMillis(),
						transportData);
				cTo.waitMessages.add(msgRequest.id());
				cFrom.sendMessage(msgRequest);
			}
		}
		totalMessagesSent += MSG_COUNT * rclients.size();
		int cc = 0;
		boolean flag = false;
		while (!rclients.isEmpty()) {
			cc++;
			if (cc % 2 == 0 && cc > 5) {
				warn(rclients.size());
			}
			if (cc > 2000) {
				throw new RuntimeException();
			}
			if (rclients.removeIf(Client::testMsgs)) {
				cc = 0;
				continue;
			}
			for (var c : rclients) {
				c.client.ping();
			}
			if (rclients.isEmpty()) {
				if (flag) {
					warn(0);
				}
				break;
			}
			Thread.sleep(500);
			flag = true;
		}
		var time = System.currentTimeMillis() - begin;
		var totalCount = clients.size() * MSG_COUNT;
		log.info(("""
				all messages was received:
				time %d
				msg count: %d
				time by one msg: %d""").formatted(
				time,
				totalCount,
				time / totalCount));
	}
	private void warn(int countClients) {
		var msgq = streamOf(servers).mapToInt(AetherCloudServer::getCountMessagesInQueue).sum();
		log.warn("""
											
						ping clients. count {}
						total messages: {}
						msgRequest in q  : {}
						ping conf : {}
						DebugMetrics:
						{}
						""",
				countClients,
				totalMessagesSent,
				msgq,
				streamOf(clients).mapToInt(c -> c.client.getConnections().size()).filterNot(c -> c == servers.size()).sum(),
				ADebug.getReport());
	}
	private void addClients() throws InterruptedException {
		log.info("add client {}", CLIENTS_COUNT);
		List<AFuture> startFutures = new ArrayList<>();
		for (int i = 0; i < CLIENTS_COUNT; i++) {
			var c = new Client(clientStore);
			startFutures.add(c.client.startFuture);
			clients.add(c);
		}
		AFuture.all(startFutures).waitDoneSeconds(5);
		log.info("all clients was made. Total {}", clients.size());
	}
	private void addServers() throws Exception {
		log.info("add servers {}", SERVERS_COUNT);
		for (int i = 0; i < SERVERS_COUNT; i++) {
			makeServer();
		}
		log.info("all servers was made. Total {}", servers.size());
	}
	@Test
	void testMultiServers1() throws Exception {
		System.out.println("PID: " + ProcessHandle.current().pid());
		System.setProperty("db-service.port", String.valueOf(DBServiceConstants.DEFAULT_PORT));
		System.setProperty("db-service.db.name", "aetherglobal");
		System.setProperty("db-service.db.user", "angel");
		System.setProperty("db-service.db.password", "1");
		dbService = AetherDBService.start();
		dbService.waitDone();
		addServers();
		streamOf(servers).map(AetherCloudServer::start).map(ARFuture::toFuture).allMap(AFuture::all).waitDoneSeconds(DELAY);
		log.info("ALL SERVER IS STARTED");
		addClients();
		sendMsgs();
		stopAll();
	}
	@Test
	void testMultiServers2() throws Exception {
		System.out.println("PID: " + ProcessHandle.current().pid());
		System.setProperty("db-service.port", String.valueOf(DBServiceConstants.DEFAULT_PORT));
		System.setProperty("db-service.db.name", "aetherglobal");
		System.setProperty("db-service.db.user", "angel");
		System.setProperty("db-service.db.password", "1");
		dbService = AetherDBService.start();
		dbService.waitDone();
		addServers();
		streamOf(servers).map(AetherCloudServer::start).map(ARFuture::toFuture).allMap(AFuture::all).waitDoneSeconds(DELAY);
		log.info("ALL SERVER IS STARTED");
		addClients();
		sendMsgs();
		stopAll();
	}
	@Test
	void testMultiServers3() throws Exception {
		System.out.println("PID: " + ProcessHandle.current().pid());
		System.setProperty("db-service.port", String.valueOf(DBServiceConstants.DEFAULT_PORT));
		System.setProperty("db-service.db.name", "aetherglobal");
		System.setProperty("db-service.db.user", "angel");
		System.setProperty("db-service.db.password", "1");
		dbService = AetherDBService.start();
		dbService.waitDone();
		addServers();
		addClients();
		sendMsgs();
//		addClients();
//		addServers();
//		addClients();
//		sendMsgs();
		stopAll();
	}
	private void stopAll() {
		log.info("stop services");
		streamOf().addAll(streamOf(clients).map(Client::stop)).addAll(streamOf(servers).map(AetherCloudServer::stop)).add(dbService.getNowElse(null).stop()).cast(AFuture.class).allMap(AFuture::all);
	}
	private static class Client {
		static int gid;
		final AetherCloudClient.StoreWrap storeWrap;
		final Queue<Message> messages = new ConcurrentLinkedQueue<>();
		private final AetherCloudClient client;
		public Set<Integer> waitMessages = new ConcurrentHashSet<>();
		int id = gid++;
		public Client(Store store) {
			var storeSecond = new StoreDefault(store);
			AetherCloudClient c = AetherCloudClient.start(storeSecond);
			this.client = c;
			this.storeWrap = c.getStoreWrap();
			c.setName("client" + id);
			c.onMessage.add(e -> {
				messages.add(e);
				waitMessages.remove(e.id());
			});
			c.connect();
		}
		public AetherCloudClient client() {
			return client;
		}
		@Override
		public String toString() {
			return "Client " + id + " [mw: " + ", mr: " + messages.size() + "]";
		}
		public boolean testMsgs() {
			return true;
		}
		public void sendMessage(Message msgRequest) {
			client.sendMessage(msgRequest);
		}
		public AFuture stop() {
			return client.stop(10000);
		}
	}
}