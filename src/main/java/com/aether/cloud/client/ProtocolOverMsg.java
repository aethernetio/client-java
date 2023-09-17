package com.aether.cloud.client;

import com.aether.common.Message;
import com.aether.net.AetherApi;
import com.aether.net.Protocol;
import com.aether.net.ProtocolConfig;
import com.aether.utils.DataIn;
import com.aether.utils.DataInOut;
import com.aether.utils.interfaces.AConsumer;

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
