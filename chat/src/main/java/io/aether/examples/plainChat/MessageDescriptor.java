package io.aether.examples.plainChat;

import java.util.UUID;

public class MessageDescriptor {
    public final UUID uid;
    public final String message;

    public MessageDescriptor(UUID uid, String message) {
        this.uid = uid;
        this.message = message;
    }

    @Override
    public String toString() {
        return uid + ": " + message;
    }
}
