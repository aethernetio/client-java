# ☁️ Aether Cloud Client for TypeScript

The **Aether Cloud Client for TypeScript** is the official library for connecting to the Aether network. It provides a highly asynchronous, high-level API for secure peer-to-peer (P2P) messaging, access control management, and client-to-server interaction, all while abstracting the complexities of the low-level Aether binary protocol.

This library is designed for a modular architecture that seamlessly handles:

* **Secure Multi-Step Handshake**: Manages registration, key exchange, and the calculation of **Proof-of-Work (PoW)**.
* **End-to-End Encryption (E2EE)**: Handles all symmetric and asymmetric cryptography using pluggable providers (like **Sodium**).
* **Connection and State Management**: Maintains persistent connections to "Work Servers" and manages client identity, keys, and server lists (`ClientStateInMemory`).
* **Intelligent Request Batching**: Optimizes network traffic by consolidating multiple asynchronous requests (e.g., for **Clouds**, **Server Descriptors**, **Access Groups**) into single binary packets.
* **RPC Conversion**: Converts binary streams into concrete, type-safe Remote Procedure Call (RPC) interfaces for structured communication.

-----

## 🛠️ Installation

The Aether Cloud Client is installed via **NPM** or **Yarn**. Since the library is hosted on a **private registry** (`http://nexus.aethernet.io/npm-private/`), you must first configure your package manager to use this repository.

### Step 1: Configure the Private Registry

You must configure the Aether-specific NPM registry either globally or within your project's `.npmrc` file. Since the repository is accessible for reading without authentication, you only need to set the scope.

**Option A: Project-Level Configuration (`.npmrc`)**

Create a file named `.npmrc` in your project root with the following content:

```npmrc
@aethernet:registry=http://nexus.aethernet.io/npm-private/
```

**Option B: Global Configuration (using `npm` or `yarn`)**

```bash
# Set the scope registry to the private nexus
npm config set @aethernet:registry http://nexus.aethernet.io/npm-private/
```

### Step 2: Add Core and Dependency Packages

Add the core client library, published under the `@aethernet` scope, and its required runtime dependencies.

```bash
# Add the core client library (assuming the name from package.json)
npm install @aethernet/aether-client

# Add required runtime dependencies, including those for crypto/networking
npm install isomorphic-ws bcryptjs crc-32 libsodium-wrappers
```

The client relies on `libsodium-wrappers` for cryptographic operations.

### Step 3: Initialization

The library uses a modular approach for crypto providers. Before using the client, you must initialize the chosen provider (e.g., Sodium).

```typescript
import { applySodium } from '@aethernet/client-ts';
// Must be called once before client instantiation
await applySodium(); 
```

-----

## 🔑 Core Concepts: Asynchronicity (`AFuture` / `ARFuture`)

All interactions with the Aether network are non-blocking and managed using Future objects.

### 1\. `AFuture` (Asynchronous Future)

Represents a void asynchronous operation (no return value).

| Method | Description |
| :--- | :--- |
| `AFuture.make()` | Creates a new pending Future. |
| `f.tryDone()` | Marks the Future as successfully completed. |
| `f.error(e)` | Marks the Future as completed with an error. |
| `f.to(runnable)` | Executes a `runnable` upon successful completion. |
| `AFuture.all(...f)` | Completes when **all** input Futures are finalized successfully. |

### 2\. `ARFuture<T>` (Asynchronous **Result** Future)

Represents an asynchronous operation that completes with a result of type `T`.

| Method | Description |
| :--- | :--- |
| `ARFuture.make<T>()` | Creates a new pending Future for result `T`. |
| `f.tryDone(value)` | Marks the Future as completed with result `value`. |
| `f.map(func)` | **Transformation:** Applies a synchronous function `func(T)` to transform the result type to `E` (`ARFuture<E>`). |
| `f.mapRFuture(func)` | **Chaining:** Chains the operation by applying an asynchronous function `func(T) -> ARFuture<E>` (similar to `flatMap`). |
| `f.to(consumer)` | Executes `consumer(T)` upon successful completion. |
| `f.toPromise(ms)` | Converts to a native JavaScript `Promise<T>`. |

#### Example: Future Chaining

Use `mapRFuture` to safely chain asynchronous operations, such as resolving a peer's location (`Cloud`) and then resolving the specific server details (`ServerDescriptor`) from that location.

```typescript
client.getCloud(peerUid)
    // Chain step 1: Get the Cloud (ARFuture<Cloud>)
    .mapRFuture((cloud: Cloud) => {
        // Chain step 2: Use the Cloud data to fetch the server descriptor
        if (!cloud.data || cloud.data.length === 0) {
            return ARFuture.ofThrow(new Error("Cloud is empty"));
        }
        return client.getServer(cloud.data[0]);
    })
    .to((server: ServerDescriptor | null) => {
        console.log(`Peer server descriptor resolved: ${server?.id}`);
    })
    .onError(e => console.error("Chain failed:", e));
```

-----

## ⚡️ Client-to-Client Communication (`MessageNode`)

The **`MessageNode`** is the crucial object that manages the connection routing logic, outgoing message queue, and incoming data stream for a specific peer (`consumerUUID`).

### 1\. Sending and Receiving P2P Messages

You obtain a `MessageNode` from the main client, which handles the necessary peer discovery (resolving the peer's `Cloud` and `ServerDescriptor`) and connection routing.

| Method | Description |
| :--- | :--- |
| `client.getMessageNode(peerUid)` | Gets or creates the bidirectional channel manager for the peer. |
| `node.send(data)` | Queues a raw binary message. The returned `AFuture` completes when the message is accepted for sending. |
| `client.onMessage.add(handler)` | Global event consumer for all incoming P2P messages (`senderUid`, `payload`). |
| `node.toConsumer(consumer)` | Binds a consumer to the node's incoming binary stream (`bufferIn`). |

### 2\. Bidirectional RPC Conversion (`toApi` and `toApiR`)

The most advanced feature of `MessageNode` is its ability to convert a raw binary stream into a type-safe Remote Procedure Call (RPC) layer. This is essential for structured communication between clients.

The **`node.toApi(meta, localApi)`** method binds your local implementation of an API interface (`localApi`) to the incoming stream. When the peer sends a message (an RPC request), the binary data is automatically deserialized and invokes the corresponding method on your `localApi` object. Return values are then serialized and sent back.

#### `toApiR<LT>(metaLt, localApiFactory)`

This method is specifically designed to manage a complete, dedicated RPC context for the peer.

1.  **Creates** a FastMeta Context (`FastApiContextLocal`).
2.  **Overrides** the context's internal `flush()` method to use **`node.send(data)`** for all outgoing RPC requests.
3.  **Binds** the incoming stream (`bufferIn`) to the local API methods.

<!-- end list -->

```typescript
// Assume the interface and its FastMeta definition are ready:
import { MyApi, MyApiMeta } from './api_defs';

// 1. Create a MessageNode for the peer
const node = client.getMessageNode(peerUid);

// 2. Define the factory for the local API implementation
const localApiFactory = (remoteApi: MyApi) => {
    return {
        // Implement method: called when peer requests it
        getPeerName: () => ARFuture.of("Peer's Name"),
        
        // Implement method: called when peer requests it
        logEvent: (level, msg) => { console.log(`[${level}] Peer: ${msg}`); return AFuture.of(); }
    } as MyApi; // Cast to ensure type compatibility
};

// 3. Create the RPC Context using toApiR
// The result is the FastApiContext that handles both I/O
const rpcContext = node.toApiR(MyApiMeta, localApiFactory);

// If MyApi includes remote calls, they are now routed through rpcContext.flush() -> node.send()
// Example: Execute a remote call on the peer:
const remoteApi = rpcContext.remoteApi; // Access the peer's remote API interface
remoteApi.logEvent("INFO", "I am sending a remote event now"); 
rpcContext.flush(); // Triggers node.send() with the serialized call
```

-----

## 🏛️ Advanced Architecture: Batching Map (`BMap`)

The `BMap<K, V>` (Batching Map) is the client's reactive and network-aware cache layer. It is used for all core data lookups to ensure network efficiency.

| Component (BMap Instance) | Purpose |
| :--- | :--- |
| `client.servers` | Caches `ServerDescriptor`s by ID. |
| `client.clouds` | Caches a peer's `Cloud` (list of server IDs) by UUID. |
| `client.accessGroups` | Caches `AccessGroup` definitions. |
| `client.accessCheckCache` | Caches the result of access permissions checks. |

### Mechanism:

1.  **Requesting**: Calling `bmap.getFuture(key)` places the `key` into the batch queue (`allRequests`).
2.  **Batching**: The client's background scheduler (or an explicit **`client.flush()`**) checks the queues of all `BMap` instances.
3.  **Flushing**: It extracts all pending keys from the queues via `bmap.getRequestsFor(sender)` and sends a single RPC request (e.g., `resolverClouds(keys)` or `resolverServers(keys)`) to the work server.

### Access Control Operations

Access control mutations (adding/removing group members) are also batched using internal queues (`client.accessOperationsAdd`/`Remove`).

* `client.createAccessGroup(...uids)`: Creates a new group, returning a mutable interface (`ARFuture<AccessGroupI>`).
* `accessGroupI.add(uuid)` / `accessGroupI.remove(uuid)`: Queues the operation into the appropriate map. **The network operation is deferred until the next `client.flush()`**.
* `client.checkAccess(uid1, uid2)`: Performs a batched lookup in the `accessCheckCache`.

-----

## 📢 Logging (`Log` & `LNode`)

The client uses a unified contextual logger (`Log`) for structured and high-performance output.

### Contextual Logging Example

Use the `using` syntax (with appropriate TypeScript configuration) or `try/finally` with `Log.context()` to attach structured data to all logs within a block.

```typescript
import { Log, LogFilter, LogLevel } from '@aethernet/client-ts';

// Set up the printer (e.g., enable DEBUG and above)
Log.printConsoleColored(new LogFilter().notLevel(LogLevel.TRACE)); 

// Use context() to capture component details for all nested logs
using _l = Log.context({ 
    component: "WorkConnection", 
    threadId: 5
});
{
    Log.info("Starting login sequence"); 

    try {
        // ... network call ...
        Log.debug("Serialization complete", { bytes: 1024 }); 
    } catch (e) {
        Log.error("Login failed unexpectedly", e as Error); 
    }
}
// Context is automatically popped.
```