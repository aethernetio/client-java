package io.aether.cloud.client;

import io.aether.api.clientApi.ClientApiSafe;
import io.aether.api.clientApi.ClientApiUnsafe;
import io.aether.api.serverApi.AuthorizedApi;
import io.aether.api.serverApi.LoginApi;
import io.aether.api.serverRegistryApi.RegistrationResponseLite;
import io.aether.common.AetherCodec;
import io.aether.common.Cloud;
import io.aether.common.Message;
import io.aether.common.ServerDescriptor;
import io.aether.logger.Log;
import io.aether.net.ApiDeserializerConsumer;
import io.aether.net.impl.bin.ApiLevel;
import io.aether.net.meta.ExceptionUnit;
import io.aether.net.meta.ResultUnit;
import io.aether.net.streams.ApiStream;
import io.aether.net.streams.DownStream;
import io.aether.net.streams.CryptoStream;
import io.aether.net.streams.SafeStream;
import io.aether.utils.RU;
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

public class ConnectionWork extends Connection<ClientApiUnsafe,ClientApiSafe,LoginApi> implements ClientApiUnsafe {

	//region counters
	public final AtomicLong lastBackPing = new AtomicLong(Long.MAX_VALUE);
	private final Set<UUID> requestClientCloudOld = new ConcurrentSkipListSet<>();
	private final Set<Integer> requestServerOld = new ConcurrentSkipListSet<>();
	private final ServerDescriptorOnClient serverDescriptor;
	private final Queue<MessageRequest> newMessages = new ConcurrentLinkedQueue<>();
	private final Map<Integer, MessageRequest> messages = new ConcurrentHashMap<>();
	final private AtomicBoolean inProcess = new AtomicBoolean();
	boolean basicStatus;
	long lastWorkTime;
	public ConnectionWork(AetherCloudClient client, ServerDescriptorOnClient s) {
		super(client,s.getURI(AetherCodec.BINARY),ClientApiUnsafe.class, LoginApi.class,new MyClientApiSafe(client));
		this.basicStatus = false;
		serverDescriptor = s;
		config=s.encryptionApiConfig;
		connect();
	}

	@Override
	protected void onConnect(LoginApi remoteApi) {
	}

	@Override
	public void setApiDeserializer(ApiLevel apiProcessor) {
		super.setApiDeserializer(apiProcessor);
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

	public void clearRequests() {
		requestClientCloudOld.clear();
	}
	public void newChildren(List<UUID> newChildren) {
		client.onNewChildren.fire(newChildren);
	}
	public void setBasic(boolean basic) {
		this.basicStatus = basic;
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
	AuthorizedApi authorizedApi;
	private void scheduledWork0() {
		try {
			var uid = client.getUid();
			if (apiProcessor == null) return;
			io.aether.net.AConnection<?, LoginApi> p = getApiProcessor().getConnection();
			if (uid == null || p == null || !p.isActive()) return;
			if (getConfig().symmetric == null) {
				return;
			}
			ApiStream<ClientApiSafe, AuthorizedApi, SafeStream<CryptoStream<DownStream>>> api =
					p.getRemoteApi().loginByUID(client.getUid());
			api.getDownStream().getStream().setCryptoProvider(getConfig().symmetric);
			authorizedApi=api.forClient(localSafeApi).getRemoteApi();
			sendRequests(uid, authorizedApi);
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
	private static class MyClientApiSafe implements ClientApiSafe, ApiDeserializerConsumer {
		private final AetherCloudClient client;
		private ApiLevel apiProcessor;
		@Override
		public void setApiDeserializer(ApiLevel apiProcessor) {
			this.apiProcessor=apiProcessor;
		}

		@Override
		public void sendResult(ResultUnit unit) {
			apiProcessor.sendResultFromRemote(unit);
		}

		@Override
		public void sendException(ExceptionUnit unit) {
			apiProcessor.sendResultFromRemote(unit);
		}

		public MyClientApiSafe(AetherCloudClient client) {
			this.client = client;
		}

		@Override
		public void confirmRegistration(RegistrationResponseLite registrationResponse) {
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
