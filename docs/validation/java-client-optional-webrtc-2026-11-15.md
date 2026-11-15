# Optional WebRTC Java client build — 2026-11-15

`-PwithWebRtc=false` excludes `OnvoidTransport.java` and its native integration test, omits direct WebRTC dependencies, and excludes the transitive `webrtc-java` dependency of `net-virtual`. It publishes under `io.aether:cloud-client-no-webrtc`, preserving the existing `io.aether:cloud-client` default artifact.

Verified with JDK 25 and Gradle 9.5: `:client:verifyWebRtcConfiguration` passes with WebRTC both on and off; the off runtime dependency graph has no `webrtc-java`, while the on graph includes it. Generated off POM has the new artifact ID and a WebRTC exclusion on `net-virtual`; generated on POM retains the original artifact ID and WebRTC dependencies. `MessageNodeWebRtcPeer.java` compiles against the no-WebRTC compile classpath and a standalone runtime check reports `isSupported()==false` without WebRTC.

Full `:client:test -PwithWebRtc=false` is blocked before Java compilation by the existing `:client:aetherProtocol` generator under Gradle 9.5: `No signature of method: java.lang.String.get() ... [types]`. The installed Gradle 8.1.1 cannot load the Java 25 protocol plugin. The full test suite and publish-to-repository task were not claimed as passed. This build switch does not disable the SDK's separate non-WebRTC peer route.
Standalone runtime checks using the modified class and published SDK artifacts also pass: without WebRTC, isSupported() is false; with WebRTC, isSupported() is true and the native provider resolves. These are scoped checks, not a full SDK suite.
