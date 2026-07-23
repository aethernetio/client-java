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

## Maturity and security

The package coordinates and repository layout may change before a stable public release. Report security-sensitive issues privately rather than including keys, credentials, or production identifiers in a public issue.

## License

See [LICENSE](LICENSE).
