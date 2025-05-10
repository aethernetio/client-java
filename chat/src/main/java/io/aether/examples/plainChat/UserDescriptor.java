package io.aether.examples.plainChat;

import io.aether.utils.AString;
import io.aether.utils.ToString;

import java.util.UUID;

public class UserDescriptor implements ToString {
    public final UUID uid;
    public final String name;

    @Override
    public void toString(AString sb) {
         sb.add(uid).add(':').add(name);
    }

    public UserDescriptor(UUID uid, String name) {
        this.uid = uid;
        this.name = name;
    }
}
