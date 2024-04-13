package io.aether.cloud.client;

import io.aether.common.Message;
import io.aether.utils.ConcurrentHashSet;
import io.aether.utils.RU;
import io.aether.utils.interfaces.AConsumer;
import io.aether.utils.slots.EventSourceConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public class MessageRequest {
	public static final Strategy STRATEGY_DEFAULT = e -> {
		if (e.status == Status.GOT_SERVER) {
			if (!e.request.isDone()) {
				e.request.sendToNextServer();
			}
		}
	};
	private final static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final Message body;
	private final EventSourceConsumer<Event> onEvent = new EventSourceConsumer<>();
	private final Queue<ServerDescriptorOnClient> targetServers = new ConcurrentLinkedQueue<>();
	private final Set<ServerDescriptorOnClient> usedServers = new ConcurrentHashSet<>();
	private final AtomicReference<Status> totalStatus = new AtomicReference<>(Status.NEW);
	private final AetherCloudClient client;
	public MessageRequest(AetherCloudClient client, UUID to, byte[] body) {
		this(client, new Message(client.nextMsgId(to), to, RU.time(), body));
	}
	public MessageRequest(AetherCloudClient client, Message body) {
		this.client = client;
		this.body = body;
	}
	public Message getBody() {
		return body;
	}
	public Queue<ServerDescriptorOnClient> getTargetServers() {
		return targetServers;
	}
	public Set<ServerDescriptorOnClient> getUsedServers() {
		return usedServers;
	}
	public void getCloud() {
		if (log.isTraceEnabled()) log.trace("get position for {}", body.uid());
		client.getCloud(body.uid()).run(c -> {
			if (log.isTraceEnabled()) log.trace("get position for {} is done", body.uid());
			totalStatus.set(Status.GET_CLOUD);
			for (var sid : c) {
				client.resolveServer(sid).to(sd -> {
					targetServers.add(sd);
					fire(sd, Status.GOT_SERVER);
				});
			}
		});
	}
	public void requestByDefaultStrategy() {
		requestByStrategy(STRATEGY_DEFAULT);
	}
	public void requestByStrategy(Strategy strategy) {
		onEvent(strategy);
		getCloud();
	}
	public void onEvent(AConsumer<Event> e) {
		onEvent.runPermanent(e);
	}
	public Status getTotalStatus() {
		return this.totalStatus.get();
	}
	public boolean isDone() {
		return getTotalStatus() == Status.DONE;
	}
	public boolean sendToNextServer() {
		var s = targetServers.poll();
		if (s == null) {
			return false;
		}
		var connection = client.getConnection(s);
		connection.sendMessage(this);
		usedServers.add(s);
		return true;
	}
	void fire(ServerDescriptorOnClient serverDescriptorId, Status type) {
		var current = totalStatus.get();
		if (current.order <= type.order) {
			totalStatus.compareAndSet(current, type);
		}
		onEvent.accept(new Event(this, serverDescriptorId, type));
	}
	public int id() {
		return body.id();
	}
	public UUID uid() {
		return body.uid();
	}
	@Override
	public String toString() {
		return "msg(" + getTotalStatus() + ")";
	}
	public enum Status {
		NEW(0),
		TRY_SEND(1),
		GET_CLOUD(2),
		GOT_SERVER(3),
		SEND(4),
		SENT(5),
		BAD_SERVER(6),
		DONE(6),
		DELIVERY(7),
		;
		final int order;
		Status(int order) {
			this.order = order;
		}
	}

	public interface Strategy extends AConsumer<Event> {
	}

	public record Event(MessageRequest request, ServerDescriptorOnClient server, Status status) {
	}
}
