package io.aether.examples.plainChat;

import java.util.UUID;

public class UserDescriptor {
    public final UUID uid;
    public final String name;

    public UserDescriptor(UUID uid, String name) {
        this.uid = uid;
        this.name = name;
    }
}
