package io.aether.cloud.client;

import io.aether.api.EncryptionApi;
import io.aether.api.EncryptionApiImpl;
import io.aether.api.clientApi.ClientApiSafe;
import io.aether.api.clientApi.ClientApiUnsafe;
import io.aether.api.serverApi.AuthorizedApi;
import io.aether.api.serverApi.LoginApi;
import io.aether.api.serverRegistryApi.RegistrationResponse;
import io.aether.client.AetherClientFactory;
import io.aether.common.*;
import io.aether.logger.Log;
import io.aether.net.ApiDeserializerConsumer;
import io.aether.net.Protocol;
import io.aether.net.ProtocolConfig;
import io.aether.net.impl.bin.ApiLevelDeserializer;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static io.aether.utils.streams.AStream.streamOf;

public class Connection extends EncryptionApiImpl<ClientApiSafe> implements ClientApiUnsafe, ApiDeserializerConsumer {
	
	//region counters
	public final AtomicLong lastBackPing = new AtomicLong(Long.MAX_VALUE);
	public final AetherCloudClient client;
	public final ARFuture<Protocol<ClientApiUnsafe, LoginApi>> conFuture = new ARFuture<>();
	final ClientApiSafe clientApiSafe = new MyClientApiSafe();
	private final Set<UUID> requestClientCloudOld = new ConcurrentSkipListSet<>();
	private final Set<Integer> requestServerOld = new ConcurrentSkipListSet<>();
	private final ServerDescriptorOnClient serverDescriptor;
	private final Queue<MessageRequest> newMessages = new ConcurrentLinkedQueue<>();
	private final Map<Integer, MessageRequest> messages = new ConcurrentHashMap<>();
	final private AtomicBoolean inProcess = new AtomicBoolean();
	boolean basicStatus;
	long lastWorkTime;
	public Connection(AetherCloudClient aetherCloudClient, ServerDescriptorOnClient s) {
		this.client = aetherCloudClient;
		super.setSubApi(clientApiSafe);
		this.basicStatus = false;
		serverDescriptor = s;
		var codec = AetherCodec.BINARY;
		config = s.securityConfig;
		var con = AetherClientFactory.make(s.getURI(codec),
				ProtocolConfig.of(ClientApiUnsafe.class, LoginApi.class, codec),
				(p) -> this);
		con.to((p) -> {
			conFuture.done(p);
		});
	}
	@Override
	public void setApiDeserializer(ApiLevelDeserializer apiProcessor) {
		super.setApiDeserializer(apiProcessor);
		apiProcessor.getProtocol().onSubApi(cmd -> {
			if (cmd.api != apiProcessor.getRemoteApi()) return;
			switch (cmd.method.name()) {
				case "loginByUID", "loginByAlias" -> EncryptionApi.prepareRemote((EncryptionApi<?>) cmd.result, getConfig());
			}
		});
	}

    @Override
    protected void selectLib(CryptoLib cryptoLib) {
        assert cryptoLib ==client.getCryptLib();
    }

    @Override
	public void sendServerKeys(SignedKey asymPublicKey, SignedKey signKey) {
		assert client.getCryptLib()==asymPublicKey.getKey().getType().cryptoLib()
				&&client.getCryptLib()==signKey.key().getType().cryptoLib();
		this.getConfig().asymmetric = asymPublicKey.key().getType().cryptoLib().env.asymmetric(asymPublicKey.key());
	}
	public ServerDescriptorOnClient getServerDescriptor() {
		return serverDescriptor;
	}
	@Override
	public String toString() {
		return "C(" + lifeTime() + ")";
	}
	public void sendMessage(MessageRequest msgRequest, boolean immediate) {
		assert msgRequest != null;
		newMessages.add(msgRequest);
		if (immediate) {
			scheduledWorkForce();
		}
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
		return RU.time() - lastBackPing.get();
	}
	public void onWritable() {
	}
	public void scheduledWork() {
		var t = RU.time();
		if ((t - lastWorkTime < client.getPingTime() || !inProcess.compareAndSet(false, true))) return;
		try {
			lastWorkTime = t;
			scheduledWork0();
		} finally {
			inProcess.set(false);
		}
	}
	public void scheduledWorkForce() {
		var t = RU.time();
		if (!inProcess.compareAndSet(false, true)) return;
		try {
			lastWorkTime = t;
			scheduledWork0();
		} finally {
			inProcess.set(false);
		}
	}
	public void deliveryReport(long msgId) {
		var m = messages.remove((int) msgId);
		if (m != null) {
			m.fire(serverDescriptor, MessageRequest.Status.DELIVERY);
		}
	}
	public void changeCloud(Cloud cloud) {
		client.changeCloud(cloud);
	}
	private void scheduledWork0() {
		try {
			var uid = client.getUid();
			if (apiProcessor == null) return;
			Protocol<?, LoginApi> p = getApiProcessor().getProtocol();
			if (uid == null || p == null || !p.isActive()) return;
			if (getConfig().symmetric == null) {
				return;
			}
			var api = p.getRemoteApi().loginByUID(uid);
			EncryptionApi.prepareRemote(api, getConfig());
			sendRequests(uid, api.symmetric());
			p.flush();
		} catch (Exception e) {
			Log.error("", e);
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
						assert sd.id() > 0;
						client.getRequestsResolveServers().remove((int) sd.id());
						client.getResolvedServers().computeIfAbsent((int) sd.id(), k -> new ARFuture<>())
								.done(ServerDescriptorOnClient.of(sd, client.getMasterKey()));
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
		api.messages().select().to(client::receiveMessages);
		return res;
	}
	private class MyClientApiSafe implements ClientApiSafe {
		@Override
		public void confirmRegistration(RegistrationResponse registrationResponse) {
			client.confirmRegistration(registrationResponse);
		}
		@Override
		public void pushMessage(@NotNull Message message) {
			client.receiveMessage(message);
		}
		@Override
		public void updateCloud(@NotNull UUID uid, @NotNull Cloud cloud) {
			client.updateCloud(uid, cloud);
		}
		@Override
		public void updateServers(@NotNull ServerDescriptor @NotNull [] serverDescriptors) {
			for (var sd : serverDescriptors) {
				client.putServerDescriptor(sd);
			}
		}
		@Override
		public void newChildren(@NotNull List<UUID> newChildren) {
			client.onNewChildren.fire(newChildren);
		}
	}
}
