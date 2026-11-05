# Æthernet Java SDK

The Æthernet Java SDK provides secure asynchronous messaging between applications, devices, and backend services through the Æthernet network.

It handles registration, end-to-end encryption, connection recovery, binary serialization, request batching, device-to-device messaging, and access-control operations.

[Documentation](https://aethernet.io/documentation) · [Examples](https://github.com/aethernetio/aethernet-examples)

## Requirements

- Gradle;
- JDK 11 or newer when using the Sodium JNI provider;
- JDK 22 or newer for the Hydrogen FFM provider, or JDK 21 with the required preview configuration.

## Installation

Packages are currently served from the Æthernet Maven repository:

```groovy
repositories {
    mavenCentral()
    maven {
        url = uri("https://nexus.aethernet.io/maven/releases/")
    }
}

dependencies {
    implementation("io.aether:cloud-client:+")
    implementation("io.aether.crypto:sodium:+")
}
```

Use `hydrogen-fast` instead of `sodium` when your JDK and deployment support the Foreign Function & Memory API:

```groovy
implementation("io.aether.crypto:hydrogen-fast:+")
```

For reproducible production builds, replace `+` with a tested version.

## Core API

- `AetherCloudClient`: client lifecycle, network operations, and messaging;
- `ClientStateInMemory`: identity and server state, optionally persisted to a file;
- `AFuture` and `ARFuture<T>`: asynchronous completion and results;
- `sendMessage` and `onMessage`: peer-to-peer message delivery;
- access-group APIs: create groups and check permissions.

## Minimal lifecycle

```java
ClientStateInMemory state = new ClientStateInMemory(
    StandardUUIDs.TEST_UID,
    List.of(URI.create("tcp://registration.aethernet.io:9010")),
    null,
    CryptoLib.SODIUM
);

AetherCloudClient client = new AetherCloudClient(state, "example-client");

try {
    client.connect();
    client.startFuture.join();
    System.out.println("Client UID: " + client.getUid());
} finally {
    client.destroy(true);
}
```

Use a project-specific parent UUID and persistent state outside development examples.

## Build and test

```bash
git clone https://github.com/aethernetio/client-java.git
cd client-java
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

The repository also contains point-to-point, CLI, chat, smart-home, and echo projects.


## Server mode and nested services

When the process must accept connections from other clients, use `AetherCloudClient.asServer`. It installs a local API implementation on every incoming `MessageNode`:

    AetherCloudClient client = AetherCloudClient.asServer(
            serviceUid, "my-service",
            ServiceApi.META,
            (MetaContext ctx) -> new MyServiceApiImpl(ctx));
    client.connect();

Add the socket transport at runtime — without it the client cannot open connections:

    runtimeOnly "io.aether:net-virtual:+"

Register against a specific registration node:

    ClientStateInFile state = new ClientStateInFile(
            StandardUUIDs.ANONYMOUS_UID,
            List.of(URI.create("tcp://dbservice.aethernet.io:9010")));

## Behaviours worth knowing

- **The uid is assigned asynchronously.** `client.getUid()` returns `null` straight after the constructor. Always wait for `client.startFuture` (with a timeout) before reading the uid, the master key, or opening any stream.
- **Persist identity with `ClientStateInFile`.** `ClientStateInMemory` re-registers on every start and changes the uid; the file-backed state keeps a stable uid across restarts.
- **`rootSigners = null` uses the built-in roots.** Passing `null` accepts the bundled SODIUM, HYDROGEN and P256 root signers. Supply your own `Set<SignChecker>` only for a private registration root.
- **Server callbacks go through the `MetaContext`.** Inside the `toApiR` factory call `ctx.makeRemote(SomeApi.META)` to obtain a remote API over the same stream, so a server method can call back into the caller.
- **Verified pair:** `io.aether:cloud-client:0.0.81` with `aether-client@1.0.6` (TypeScript). Pin both sides together when a Java service and a TS client exchange messages.


## Maturity and security

The package coordinates and repository layout may change before a stable public release. Report security-sensitive issues privately rather than including keys, credentials, or production identifiers in a public issue.

## License

See [LICENSE](LICENSE).