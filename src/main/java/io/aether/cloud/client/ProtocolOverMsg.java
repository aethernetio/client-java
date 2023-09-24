package io.aether.cloud.client;

import io.aether.common.Message;
import io.aether.net.AetherApi;
import io.aether.net.Protocol;
import io.aether.net.ProtocolConfig;
import io.aether.utils.DataIn;
import io.aether.utils.DataInOut;
import io.aether.utils.interfaces.AConsumer;

import java.util.UUID;

public class ProtocolOverMsg<LT extends AetherApi, RT extends AetherApi> extends Protocol<LT, RT> {
	public final UUID address;
	private final AetherCloudClient client;
	private final DataInOut current = new DataInOut();
	private final AConsumer<Message> listener;
	public ProtocolOverMsg(ProtocolConfig<LT, RT> config, AetherCloudClient client, LT localApi, UUID address) {
		super(config, localApi);
		this.client = client;
		this.address = address;
		listener = m -> {
			if (m.uid().equals(this.address)) {
				putFromRemote(m.data());
			}
		};
		client.onMessage(listener);
		onDisconnect.to(() -> client.removeOnMessage(listener));
	}
	@Override
	public void flush0() {
		var data = current.toArrayCopy();
		current.clear();
		client.sendMessage(new Message(client.nextMsgId(address), address, System.currentTimeMillis(), data));
	}
	@Override
	public boolean isActive() {
		return client.isConnected();
	}
	@Override
	protected void cmdToRemote(DataIn data) {
	}
	@Override
	public void onActive() {
	}
}
