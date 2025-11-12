# Aether Cloud Client

The Aether Cloud Client is the official Java library for connecting to the Aether network. Aether is a secure, high-performance communication platform designed for efficient, stateful client-to-client and client-to-server interactions.

This library is designed to abstract the significant complexity of the Aether binary protocol. It automatically handles:

* **Secure Registration:** A multi-step asynchronous registration handshake, including Proof-of-Work (PoW) calculation.
* **End-to-End Encryption:** Manages all symmetric and asymmetric cryptography for all communications.
* **Binary Serialization:** Handles the complex binary serialization format.
* **Connection Management:** Maintains persistent connections to "Work Servers" and handles automatic recovery.
* **Automatic Request Batching:** Intelligently batches data requests (like fetching user info or access rights) to minimize network overhead.
* **Client-to-Client Messaging:** Provides a simple API for sending and receiving messages via `MessageNode` streams.

## Features

* Full implementation of the Aether Registration and Work protocols.
* Simple, future-based async API (`ARFuture`).
* High-level API for client-to-client messaging (`sendMessage`, `onMessage`).
* High-level API for Access Control (`createAccessGroup`, `checkAccess`).
* Automatic and transparent batching of data lookups.
* Built-in `ClientStateInMemory` for easy setup, with support for file-based persistence.

---

## Installation

Installation is a **three-step process**:

1. Add the Aether repository.
2. Add the core `cloud-client` dependency.
3. Add at least one **cryptography module** dependency.

### Step 1: Add the Aether Repository

You must add the Aether-specific Maven repository to your `settings.gradle` or `build.gradle` file.

**Note:** The repository uses `http`. You must enable insecure protocols for this entry.

    // settings.gradle
    pluginManagement {
        repositories {
            gradlePluginPortal()
            maven {
                url = 'http://nexus.aethernet.io/maven/releases/'
                allowInsecureProtocol = true
            }
        }
    }
    rootProject.name = 'my-aether-app'

### Step 2: Add the Core Client Dependency

Add the `cloud-client` library to your project's `build.gradle` file.

    // build.gradle
    dependencies {
        // Aether Cloud Client (Core Library)
        implementation 'io.aether:cloud-client:+'
    }

### Step 3: Add Cryptography Dependencies

The Aether Cloud Client delegates all cryptographic operations to external modules. You must include at least one of the following dependencies in your project for the client to function.

These modules are loaded automatically at runtime. By simply adding the dependency, the crypto provider registers itself with the `CryptoProviderFactory` using the `ModuleAutoRun` system.

#### Cryptographic Module Options

You can choose a module depending on your JDK requirements and performance needs.

##### 1. Sodium (JNI)

**Description:** Uses JNI (Java Native Interface) to connect to the native libsodium library.

**Compatibility:** JDK 11+

**Dependency:**

    implementation 'io.aether.crypto:sodium:+'

##### 2. Hydrogen (FFM / "Fast")

**Description:** Uses the modern Java Foreign Function & Memory (FFM) API (JEP 454) for high-performance native calls to libhydrogen without JNI.

**Compatibility:** JDK 22+ (or JDK 21 with preview flags)

**Dependency:**

    implementation 'io.aether.crypto:hydrogen-fast:+'

You can include both libraries. The client will use the one specified in `ClientState` during initialization.

## Core Concepts

### AetherCloudClient

This is the main class and entry point for the entire library. You create one instance of this class for your application. It manages the client state, network connections, and all API interactions.

### ClientState

The client requires a `ClientState` implementation to store its identity (UID, alias, master key), information about known servers, and registration URI.

The library provides `ClientStateInMemory` for quick startup, which also supports saving and loading state from a file, allowing the client to persist its UID between restarts.

### Parent UUID

Parent UUID defines the application or service namespace that your clients are part of. When creating `ClientState`, you must specify a Parent UUID.

For development and testing, you can use public UIDs from the `io.aether.StandardUUIDs` class, such as `StandardUUIDs.TEST_UID` or `StandardUUIDs.ROOT_UID`.

### MessageNode

To send a message to another client, you use a `MessageNode`. `AetherCloudClient` manages them for you. When you call `client.sendMessage()`, the client gets or creates a `MessageNode` that manages the data flow to that specific client UUID.

## Quick Start: Point-to-Point (P2P) Example

This example, based on `PointToPointTest.java`, demonstrates how to set up two clients that connect to the Aether network, exchange messages, and shut down properly.

    import io.aether.StandardUUIDs;
    import io.aether.api.common.CryptoLib;
    import io.aether.cloud.client.AetherCloudClient;
    import io.aether.cloud.client.ClientStateInMemory;
    import io.aether.logger.Log;
    import io.aether.utils.futures.AFuture;

    import java.net.URI;
    import java.util.List;
    import java.util.UUID;

    public class P2PApplication {

        // 1. Define registration URI
        private static final List<URI> REGISTRATION_URI = List.of(
                URI.create("tcp://registration.aethernet.io:9010")
        );

        // 2. Define Parent UUID for your application
        // We'll use StandardUUIDs.TEST_UID for this example
        private static final UUID PARENT_UUID = StandardUUIDs.TEST_UID;

        public static void main(String[] args) {
            
            // 3. Configure state for two clients
            // Clients can use different crypto libraries
            ClientStateInMemory config1 = new ClientStateInMemory(
                    PARENT_UUID, REGISTRATION_URI, null, CryptoLib.SODIUM
            );
            ClientStateInMemory config2 = new ClientStateInMemory(
                    PARENT_UUID, REGISTRATION_URI, null, CryptoLib.HYDROGEN
            );

            // 4. Create client instances
            AetherCloudClient client1 = new AetherCloudClient(config1, "client1");
            AetherCloudClient client2 = new AetherCloudClient(config2, "client2");

            // Future that will complete when the test is done
            AFuture testDoneFuture = AFuture.make();

            try {
                Log.info("Starting clients and connecting to Aether network...");
                
                // 5. Start asynchronous connection/registration
                client1.connect();
                client2.connect();

                // 6. Wait until BOTH clients are ready
                // startFuture completes when registration and connection
                // to Work Server are done.
                AFuture.all(client1.startFuture, client2.startFuture).to(() -> {
                    
                    Log.info("Both clients successfully registered!");
                    Log.info("Client 1 UID: " + client1.getUid());
                    Log.info("Client 2 UID: " + client2.getUid());

                    AFuture messageReceivedFuture = AFuture.make();
                    byte[] testMessage = new byte[]{1, 2, 3, 4};

                    // 7. Set up message reception for client2
                    client2.onMessage((senderUuid, payload) -> {
                        if (senderUuid.equals(client1.getUid())) {
                            Log.info("Client 2 received message from Client 1!");
                            messageReceivedFuture.tryDone();
                        }
                    });

                    // 8. Send message from client1 to client2
                    Log.info("Client 1 sending message to Client 2...");
                    client1.sendMessage(client2.getUid(), testMessage)
                           .onError(testDoneFuture::error);

                    // 9. Wait for reception confirmation
                    messageReceivedFuture.to(() -> {
                        Log.info("P2P test completed successfully!");
                        testDoneFuture.done();
                    }).onError(testDoneFuture::error);

                }).onError(testDoneFuture::error);

                // Block here waiting for test completion
                testDoneFuture.join();

            } catch (Exception e) {
                Log.error("Error during P2P test execution", e);
            } finally {
                // 10. Always shut down clients properly
                Log.info("Shutting down clients...");
                client1.destroy(true);
                client2.destroy(true);
                Log.info("Test completed.");
            }
        }
    }

## Advanced Usage

### Access Control Example

The Aether Cloud Client provides high-level APIs for managing access control between clients.

    // Create an access group
    AFuture<UUID> accessGroupFuture = client.createAccessGroup("my-group");

    // Check if a client has access to the group
    accessGroupFuture.to(accessGroupId -> {
        client.checkAccess(otherClientUid, accessGroupId).to(hasAccess -> {
            if (hasAccess) {
                Log.info("Client has access to the group");
            } else {
                Log.info("Client does not have access to the group");
            }
        });
    });

### Error Handling

The library uses `AFuture` for asynchronous operations with comprehensive error handling:

    client.sendMessage(targetUid, messageData)
        .to(() -> {
            Log.info("Message sent successfully");
        })
        .onError(error -> {
            Log.error("Failed to send message", error);
        });

### State Persistence

`ClientStateInMemory` supports file-based persistence to maintain client identity between restarts:

    ClientStateInMemory config = new ClientStateInMemory(
        PARENT_UUID, 
        REGISTRATION_URI, 
        Path.of("client-state.json"), // Persistence file
        CryptoLib.SODIUM
    );

## API Reference

### Key Methods

- `AetherCloudClient.connect()` - Start connection and registration process
- `AetherCloudClient.sendMessage(UUID target, byte[] payload)` - Send message to another client
- `AetherCloudClient.onMessage(MessageListener listener)` - Set up message reception
- `AetherCloudClient.createAccessGroup(String name)` - Create a new access group
- `AetherCloudClient.checkAccess(UUID clientUid, UUID accessGroupId)` - Check client access to a group
- `AetherCloudClient.destroy(boolean wait)` - Shut down the client

### Important Classes

- `ClientStateInMemory` - In-memory state storage with optional file persistence
- `AFuture` - Asynchronous future implementation used throughout the API
- `CryptoLib` - Enumeration of available cryptography libraries (SODIUM, HYDROGEN)

## Troubleshooting

### Common Issues

1. **Registration fails:** Check your registration URI and network connectivity.
2. **Cryptography errors:** Ensure you have included at least one cryptography dependency.
3. **Client fails to start:** Verify that the Parent UUID is correct and accessible.

### Logging

The library uses the `io.aether.logger.Log` class for logging. Ensure your logging configuration is set to an appropriate level (INFO or DEBUG) to see detailed operation logs.

## Support

For issues and questions, please refer to the official Aether documentation or contact the development team.

---

*This documentation covers the basic usage of the Aether Cloud Client. For advanced features and detailed API reference, please see the JavaDoc documentation.*