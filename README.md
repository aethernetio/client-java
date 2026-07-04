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


## Quick Start: Using asClient / asServer

The library provides static factory methods `asClient` and `asServer` that encapsulate all the boilerplate: creating `ClientState`, connecting, setting up `MessageNode` and API bindings.

### Client (asClient)

A client connects to a parent service and communicates via strongly-typed generated API interfaces:

    import io.aether.cloud.client.AetherCloudClient;

    UUID parentUid = UUID.fromString("...");

    AetherCloudClient client = AetherCloudClient.asClient(
        parentUid, "MyChatClient",
        ServiceClientApi.META,       // local API (what client implements)
        ServiceServerApi.META,       // remote API (what server exposes)
        remoteApi -> new MyServiceClientApiImpl(remoteApi)
    );

The client is now connected and ready. `remoteApi` is fully wired — just call its methods. State auto-saved to `state-MyChatClient.bin`.

### Server (asServer)

A server registers API implementations and handles incoming client connections:

    AetherCloudClient server = AetherCloudClient.asServer(
        parentUid, "MyChatService",
        ServiceServerApi.META,
        ctx -> {
            var clientCallbackApi = ctx.makeRemote(ServiceClientApi.META);
            return new MyServiceServerApiImpl(clientCallbackApi);
        }
    );

The server is now listening for incoming client streams. State auto-saved to `state-MyChatService.bin`.

### Using Custom ClientState

For testing with custom registration URIs or crypto settings, pass a `ClientState` directly:

    ClientStateInMemory testState = new ClientStateInMemory(
        parentUid, List.of(URI.create("tcp://test-server:9010")), null, CryptoLib.SODIUM
    );

    AetherCloudClient client = AetherCloudClient.asClient(
        testState, "TestClient",
        MyApi.META, RemoteApi.META,
        remoteApi -> new MyApiImpl(remoteApi)
    );

### Method Signatures

    // asClient – with auto-created ClientStateInFile
    public static <LT, RT extends RemoteApi> AetherCloudClient asClient(
        UUID parentUid, String name,
        FastMetaApi<LT, ? extends LT> localMeta,
        FastMetaApi<?, RT> remoteMeta,
        AFunction<RT, LT> localApiFactory)

    // asClient – with custom regUri
    public static <LT, RT extends RemoteApi> AetherCloudClient asClient(
        UUID parentUid, URI regUri, String name,
        FastMetaApi<LT, ? extends LT> localMeta,
        FastMetaApi<?, RT> remoteMeta,
        AFunction<RT, LT> localApiFactory)

    // asClient – with custom ClientState
    public static <LT, RT extends RemoteApi> AetherCloudClient asClient(
        ClientState state, String name,
        FastMetaApi<LT, ? extends LT> localMeta,
        FastMetaApi<?, RT> remoteMeta,
        AFunction<RT, LT> localApiFactory)

    // asServer – with auto-created ClientStateInFile
    public static <LT> AetherCloudClient asServer(
        UUID parentUid, String name,
        FastMetaApi<LT, ? extends LT> serviceMeta,
        AFunction<MetaContext, LT> localApiFactory)

    // asServer – with custom regUri
    public static <LT> AetherCloudClient asServer(
        UUID parentUid, URI regUri, String name,
        FastMetaApi<LT, ? extends LT> serviceMeta,
        AFunction<MetaContext, LT> localApiFactory)

    // asServer – with custom ClientState
    public static <LT> AetherCloudClient asServer(
        ClientState state, String name,
        FastMetaApi<LT, ? extends LT> serviceMeta,
        AFunction<MetaContext, LT> localApiFactory)

### Legacy Low-Level API

The original manual approach is still available for advanced use cases:

    ClientStateInMemory config1 = new ClientStateInMemory(
        parentUid, List.of(URI.create("tcp://registration.aethernet.io:9010")), null, CryptoLib.SODIUM
    );
    AetherCloudClient client1 = new AetherCloudClient(config1, "client1");
    client1.connect();

    client1.startFuture.to(() -> {
        Log.info("Client registered with UID: " + client1.getUid());
        client1.sendMessage(targetUid, message);
    });


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