package io.aether.examples.plainChat;

import java.util.UUID;

public record MessageDescriptor(UUID uid, String message) {
	@Override
	public String toString() {
		return uid + ": " + message;
	}
}
