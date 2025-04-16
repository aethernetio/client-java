package io.aether.cloud.client;

import io.aether.common.Cloud;
import io.aether.common.ServerDescriptor;

public interface MessageEventListener {
    MessageEventListener DEFAULT = new MessageEventListener() {
        @Override
        public void setConsumerCloud(MessageNode messageNode, Cloud cloud) {
            messageNode.addConsumerServerOut(cloud.data()[0]);
        }

        @Override
        public void onResolveConsumerServer(MessageNode messageNode, ServerDescriptor serverDescriptor) {
            messageNode.addConsumerServerOut(serverDescriptor);
        }

        @Override
        public void onResolveConsumerConnection(MessageNode messageNode, ConnectionWork connection) {
            messageNode.addConsumerConnectionOut(connection);
        }
    };

    void setConsumerCloud(MessageNode messageNode, Cloud cloud);

    void onResolveConsumerServer(MessageNode messageNode, ServerDescriptor serverDescriptor);

    void onResolveConsumerConnection(MessageNode messageNode, ConnectionWork connection);

}
