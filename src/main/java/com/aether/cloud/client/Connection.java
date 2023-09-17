package com.aether.cloud.client;

import com.aether.api.clientApi.ClientApiSafe;
import com.aether.api.clientApi.ClientApiUnsafe;
import com.aether.api.serverApi.AuthorizedApi;
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
import com.aether.utils.futures.AFuture;
import com.aether.utils.futures.ARFuture;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.aether.utils.streams.AStream.streamOf;

public class Connection implements ClientApiUnsafe, AetherApiLocal {
	private static final Logger log= LoggerFactory.getLogger(Connection.class);
	//region counters
	public final AtomicLong lastBackPing = new AtomicLong(Long.MAX_VALUE);
	public final AetherCloudClient client;
	public final ARFuture<Protocol<ClientApiUnsafe, ServerUnsafeApi>> conFuture = new ARFuture<>();
	final ClientApiSafe clientApiSafe = new MyClientApiSafe();
	private final Set<UUID> requestClientCloudOld = new ConcurrentSkipListSet<>();
	private final Set<Integer> requestServerOld = new ConcurrentSkipListSet<>();
	private final ServerDescriptorOnClient serverDescriptor;
	public ServerDescriptorOnClient getServerDescriptor() {
		return serverDescriptor;
	}
	private final Queue<MessageRequest> newMessages = new ConcurrentLinkedQueue<>();
	public Queue<MessageRequest> getNewMessages() {
		return newMessages;
	}
	private final Map<Integer, MessageRequest> messages = new ConcurrentHashMap<>();
	final private AtomicBoolean inProcess = new AtomicBoolean();
	boolean basicStatus;
	long lastWorkTime;
	private volatile Protocol<ClientApiUnsafe, ServerUnsafeApi> protocol;
	private volatile AsymCrypt asymCryptByServerCrypt;
	private volatile ChaCha20Poly1305 chaCha20Poly1305;
	public Connection(AetherCloudClient aetherCloudClient, ServerDescriptorOnClient s) {
		this.client = aetherCloudClient;
		this.basicStatus = false;
		serverDescriptor = s;
		var codec = AetherCodec.BINARY;
		var con = AetherClientFactory.make(s.getInetSocketAddress(codec),
				ProtocolConfig.of(ClientApiUnsafe.class, ServerUnsafeApi.class, codec),
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
		con.to(conFuture);
	}
	@Override
	public void sendExceptionToRemote(ExceptionUnit unit) {
		var p = protocol;
		var uid = client.getUid();
		if (p == null || !p.isActive() || uid == null) {
			log.debug("Ignore exception unit to server");
			return;
		}
		p.getRemoteApi().chacha20poly1305(uid).sendException(unit);
	}
	@Override
	public void sendResultToRemote(ResultUnit unit) {
		var p = protocol;
		var uid = client.getUid();
		if (p == null || !p.isActive() || uid == null) {
			log.debug("Ignore exception unit to server");
			return;
		}
		p.getRemoteApi().chacha20poly1305(uid).sendResult(unit);
	}
	@Override
	public ClientApiSafe chacha20poly1305() {
		return clientApiSafe;
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
	public String toString() {
		return "C(" + lifeTime() + ")";
	}
	public void sendMessage(MessageRequest msgRequest) {
		assert msgRequest != null;
		newMessages.add(msgRequest);
		conFuture.to((p) -> {
			p.getRemoteApi().chacha20poly1305(client.getUid()).sendMessage(msgRequest.getBody()).to(r -> {
				if (r) {
					msgRequest.fire(serverDescriptor, MessageRequest.Status.DONE);
				} else {
					msgRequest.fire(serverDescriptor, MessageRequest.Status.BAD_SERVER);
				}
			});
			p.flush();
		});
	}
	public AFuture close(int time) {
		var res = new AFuture();
		conFuture.to(c -> {
					c.close();
					res.done();
				})
				.timeout(time, res::done);
		return res;
	}
	public void clearRequests() {
		requestClientCloudOld.clear();
	}
	public void newChildren(List<UUID> newChildren) {
		client.onNewChildren.fire(newChildren);
	}
	public void setBasic(boolean basic) {
		this.basicStatus = basic;
	}
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Connection that = (Connection) o;
		return serverDescriptor.equals(that.serverDescriptor);
	}
	@Override
	public int hashCode() {
		return serverDescriptor.hashCode();
	}
	public void receiveMessage(Message msg) {
		client.receiveMessage(msg);
	}
	public long lifeTime() {
		return System.currentTimeMillis() - lastBackPing.get();
	}
	public void onWritable() {
	}
	public boolean isConnected() {
		var p = protocol;
		return p != null && p.isActive();
	}
	public void scheduledWork() {
		var t = System.currentTimeMillis();
		if ((t - lastWorkTime < client.getPingTime() || !inProcess.compareAndSet(false, true))) return;
		try {
			lastWorkTime = t;
			scheduledWork0();
			client.startFuture.tryDone();
		} finally {
			inProcess.set(false);
		}
	}
	public void updateUserPosition(UUID uid, int[] serverIds) {
		client.updateCloud(uid, serverIds);
	}
	public void deliveryReport(long msgId) {
		var m = messages.remove((int) msgId);
		if (m != null) {
			m.fire(serverDescriptor, MessageRequest.Status.DELIVERY);
		}
	}
	public void changeCloud(int[] cloud) {
		client.changeCloud(cloud);
	}
	private void scheduledWork0() {
		try {
			var uid = client.getUid();
			var p = protocol;
			if (uid == null || p == null || !p.isActive()) return;
			sendRequests(uid, p.getRemoteApi().chacha20poly1305(uid));
			p.flush();
		} catch (Exception e) {
			log.error("", e);
		}
	}
	private boolean sendRequests(UUID uid, AuthorizedApi api) {
		boolean res = false;
		if (!client.getRequestClientClouds().isEmpty()) {
			var data = streamOf(client.getRequestClientClouds())
					.filter(requestClientCloudOld::add)
					.filterExclude(uid)
					.toArray(UUID.class);
			if (data.length > 0) {
				for (var r : data) {
					api.client(r).getPosition().to(p -> {
						client.getRequestClientClouds().remove(r);
						client.getCloud(r).set(p);
					});
				}
				res = true;
			}
		}
		if (!client.getRequestsResolveServers().isEmpty()) {
			int[] data = streamOf(client.getRequestsResolveServers())
					.filter(requestServerOld::add)
					.mapToInt(i -> i)
					.toArray();
			if (data.length > 0) {
				api.getServerDescriptor(data).to(sdd -> {
					for (var sd : sdd) {
						client.getRequestsResolveServers().remove(sd.id());
						client.getResolvedServers().computeIfAbsent(sd.id(), k -> new ARFuture<>())
								.done(new ServerDescriptorOnClient(sd.id(), sd.ipAddress(), sd.codersAndPorts()));
					}
				});
				res = true;
			}
		}
		ObjectCollection<MessageRequest> msgRequests = null;
		while (true) {
			var m = newMessages.poll();
			if (m == null) break;
			if (messages.get(m.id()) != null) {
				System.out.println("already msg: " + m);
				continue;
			}
			if (msgRequests == null) {
				msgRequests = new ObjectArrayList<>();
			}
			msgRequests.add(m);
		}
		if (msgRequests != null) {
			if (!msgRequests.isEmpty()) {
				for (var m : msgRequests) {
					m.fire(serverDescriptor, MessageRequest.Status.SEND);
					messages.put(m.id(), m);
					api.client(m.uid()).sendMessage(m.id(), m.getBody().time(), m.getBody().data()).to(r -> {
						var mm = messages.remove(m.id());
						if (mm != null) {
							if (r) {
								mm.fire(serverDescriptor, MessageRequest.Status.DONE);
							} else {
								mm.fire(serverDescriptor, MessageRequest.Status.BAD_SERVER);
							}
						}
					});
					m.fire(serverDescriptor, MessageRequest.Status.SENT);
				}
				res = true;
			}
		}
		api.messages().select().to(list -> {
			client.receiveMessages(list);
		});
		return res;
	}
	private class MyClientApiSafe implements ClientApiSafe {
		@Override
		public void pushMessage(@NotNull Message message) {
			client.receiveMessage(message);
		}
		@Override
		public void updatePosition(@NotNull UUID uid, int @NotNull [] cloud) {
			client.updateCloud(uid, cloud);
		}
		@Override
		public void updateServers(@NotNull ServerDescriptor @NotNull [] serverDescriptors) {
			throw new UnsupportedOperationException();
		}
		@Override
		public void newChildren(@NotNull List<UUID> newChildren) {
			client.onNewChildren.fire(newChildren);
		}
	}
}
