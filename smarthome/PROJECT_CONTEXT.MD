Here are the updated documentation files that fully reflect the current P2P architecture, the file structure provided, and the specific features (Charts, Persistence, Java/TCP Mock) we have implemented.

### 1\. `PROJECT_CONTEXT.MD`

This file has been rewritten to serve as the definitive guide to the project's architecture, directory structure, and protocols.

````markdown
# 🏠 SmartHome P2P: Project Context & Architecture

## 1. 🚀 Project Overview
**SmartHome P2P** is a decentralized smart home management system built on the **Aether** communication platform.

Unlike traditional centralized architectures (Client -> Middleware Server -> Device), this project implements a **Direct P2P (Peer-to-Peer)** model. The GUI Client (running in a browser) connects directly to the hardware Commutator via an encrypted Aether channel, ensuring low latency and privacy.

**Key Architectural Pillars:**
* **No Middleware:** There is no "SmartHomeService". Logic resides entirely in the Client (UI/Controller) and the Commutator (Device).
* **Transport Agnostic:**
    * **GUI Client:** Connects via **WebSocket** (Browser restriction).
    * **Commutator (Java):** Connects via **TCP** (Native performance) or WebSocket.
* **Aether Core:** Used strictly for initial registration, NAT traversal, and signaling.

---

## 2. 📂 Project Structure
The project is a hybrid **Java (Backend/Test)** and **TypeScript (Frontend)** monorepo.

```text
.
├── build.gradle                   # Java build configuration (for Mock & Tests)
├── PROJECT_CONTEXT.MD             # This documentation
├── src                            # JAVA SOURCES (Testing & API Definition)
│   ├── main/resources
│   │   └── smarthome.adsl.yaml    # SINGLE SOURCE OF TRUTH: API Contract (ADSL)
│   └── test/java/.../e2e
│       ├── AbstractTypeScriptTestExecutor.java # Base class for running Node/NPM from Java
│       ├── MockCommutatorClient.java           # Java emulation of a physical device (TCP Client)
│       └── SmartHomeE2ETest.java               # E2E Tests: Java launches Server + Mock + TS Client
├── gui                            # TYPESCRIPT SOURCES (Web Client)
│   ├── package.json               # JS Dependencies (chart.js, aether-client, etc.)
│   ├── webpack.config.js          # Bundling config
│   ├── tz.html                    # Technical Requirements (Web View)
│   ├── src
│   │   ├── smarthome.adsl.yaml    # Symlink to ../../src/main/resources/smarthome.adsl.yaml
│   │   ├── smarthome.gui.state.adsl.yaml # Schema for LocalStorage persistence
│   │   ├── aether_api.ts          # GENERATED API Code (Protocol)
│   │   ├── SmartHomeController.ts # Core Logic: Aether Connection & P2P Management
│   │   ├── PollingManager.ts      # Logic for RTT measurement and strict polling
│   │   ├── AppStateManager.ts     # Persistence logic (Binary serialization to LocalStorage)
│   │   └── index.ts               # UI Binding, Charts, and Entry Point
│   └── test
│       └── smarthome.e2e.test.ts  # TypeScript-side E2E tests
````

-----

## 3\. 📡 API Protocol (`smarthome.adsl.yaml`)

Communication is defined by the ADSL schema. The Aether Generator creates type-safe classes for both Java and TypeScript from this file.

### 3.1. Data Types

* **`HardwareDevice`**: Abstract base for all devices.
  * `HardwareSensor`: Read-only device (e.g., Temperature). Contains `unit`.
  * `HardwareActor`: Controllable device (e.g., Light Switch).
* **`DeviceStateData`**: Snapshot of a device's status (`payload` + `timestamp`).
* **`VariantData`**: Tagged union for polymorphic values (Double, Bool, String).

### 3.2. `SmartHomeCommutatorApi` (Implemented by Java/Hardware)

*Called by the GUI Client via P2P.*

| Method | Description |
| :--- | :--- |
| `getSystemStructure()` | Returns the full list of devices (`HardwareDevice[]`). |
| `executeActorCommand(id, cmd)` | Sends a control command to an actor. |
| `queryState(id)` | **Synchronous** request for a specific device state. Used for polling/RTT. |
| `queryAllSensorStates()` | **Asynchronous** trigger. Asks Commutator to PUSH all sensor data. |

### 3.3. `SmartHomeClientApi` (Implemented by GUI)

*Called by the Commutator via P2P (PUSH).*

| Method | Description |
| :--- | :--- |
| `deviceStateUpdated(id, state)` | Real-time callback. Fired whenever a physical device changes state. |

-----

## 4\. 🧩 Key System Components

### A. Java Mock Commutator (`MockCommutatorClient.java`)

Used for E2E testing and development.

* **Transport:** Uses **TCP** to connect to the local Aether server.
* **Function:** Simulates physical sensors (Temperature) and actors (Light).
* **Behavior:** Responds to P2P requests and broadcasts PUSH updates to all connected clients when state changes.

### B. TypeScript Client Controller (`SmartHomeController.ts`)

The "Brain" of the frontend.

1.  **Session Management:**
  * Connects to Aether Core via WebSocket.
  * **Persistence:** Saves/Restores session keys and UUID to `LocalStorage` to allow page reloads without re-registration.
2.  **P2P Management:**
  * Establishes a direct encrypted stream to the target Commutator UUID.
  * Exposes methods to query structure and state.
3.  **Event Handling:**
  * Dispatches events for UI updates (`onDeviceListUpdate`, `onDeviceStateChanged`).

### C. Polling & Metrics (`PollingManager.ts`)

Implements network diagnostics requirements.

* **Strict Sequential Polling:** Ensures `Request N+1` is sent only after `Request N` completes or times out.
* **RTT Calculation:** Measures exact round-trip time for `queryState()` calls.
* **Loss Detection:** Counts timeouts as packet loss.

### D. Visualization (`index.ts` + Chart.js)

* **RTT Chart:** Real-time graph of network latency.
* **Sensor History:** Real-time graph of sensor values.
* **Persistence:** Restores chart configuration and history from `LocalStorage` on startup.

-----

## 5\. 🔄 Workflow

1.  **Startup:**
  * GUI checks `LocalStorage`. If a session exists, it restores the Aether connection immediately.
  * If not, user enters Registration URI and connects.
2.  **Connection:**
  * User enters Commutator UUID.
  * `SmartHomeController` opens P2P channel.
  * GUI requests `getSystemStructure()`.
3.  **Operation:**
  * **Passive:** GUI listens for `deviceStateUpdated` PUSH messages to update cards.
  * **Active (Polling):** `PollingManager` pings a specific device every `N` ms to update RTT charts and detect packet loss.
4.  **Control:**
  * User clicks "ON". GUI sends `executeActorCommand`.
  * Commutator processes command, changes state, and sends PUSH update back to GUI.

<!-- end list -->

````

---

### 2. `tz.html`

This technical specification has been updated to match the implemented features (Charts, Persistence, P2P) and removes the outdated "Central Hub" concepts.

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TS: SmartHome GUI (P2P Direct)</title>
    <style>
        :root { --primary: #2c3e50; --secondary: #3498db; --accent: #27ae60; --text: #333; --bg: #f9f9f9; }
        body { font-family: 'Segoe UI', system-ui, sans-serif; line-height: 1.6; color: var(--text); max-width: 1000px; margin: 0 auto; padding: 20px; background: var(--bg); }
        header { background: var(--primary); color: white; padding: 2rem; border-radius: 8px; margin-bottom: 2rem; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
        h1 { margin: 0; color: white; }
        h2 { border-bottom: 2px solid var(--secondary); padding-bottom: 10px; color: var(--primary); margin-top: 2rem; }
        h3 { color: var(--secondary); margin-top: 1.5rem; }
        .card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); margin-bottom: 15px; border-left: 5px solid var(--secondary); }
        .feature-list li { margin-bottom: 8px; }
        code { background: #eef2f7; padding: 2px 6px; border-radius: 4px; font-family: monospace; color: #c7254e; }
        .tag { display: inline-block; background: var(--accent); color: white; padding: 2px 8px; border-radius: 12px; font-size: 0.8rem; font-weight: bold; }
    </style>
</head>
<body>

<header>
    <h1>Technical Specification: SmartHome GUI</h1>
    <p>Client-side application for direct P2P control of smart home devices via the Aether Network.</p>
</header>

<section>
    <h2>1. Architecture & Connectivity</h2>
    <p>The system operates on a <strong>Direct P2P</strong> model. The Browser (GUI) acts as an Aether Client, connecting directly to the Commutator (Device) Client.</p>
    <ul>
        <li><strong>Transport:</strong> Browser connects to Aether Core via WebSocket. Commutator (Hardware/Java) connects via TCP.</li>
        <li><strong>Discovery:</strong> Connection is established using the Commutator's **UUID**.</li>
        <li><strong>Security:</strong> End-to-End Encryption (E2EE) provided by Aether.</li>
    </ul>
</section>

<section>
    <h2>2. Core Functional Requirements</h2>

    <h3>2.1. Connection Management & Persistence</h3>
    <div class="card">
        <p><strong>Goal:</strong> Seamless user experience across page reloads.</p>
        <ul class="feature-list">
            <li><strong>Session Restoration:</strong> The application must save the Aether Client state (Keys, UID) to <code>LocalStorage</code>. On startup, it must attempt to restore the session to avoid re-registration.</li>
            <li><strong>Config Persistence:</strong> Application settings (Target UUID, Polling Timeout, Visible Graphs) must be saved and restored automatically.</li>
        </ul>
    </div>

    <h3>2.2. Device Dashboard</h3>
    <div class="card">
        <p><strong>Goal:</strong> Visualize and control the home structure.</p>
        <ul class="feature-list">
            <li><strong>Dynamic Rendering:</strong> Render devices based on the <code>getSystemStructure()</code> response.</li>
            <li><strong>Sensors:</strong> Display real-time values (e.g., "24.5 °C").</li>
            <li><strong>Actors:</strong> Provide interactive controls (ON/OFF buttons).</li>
            <li><strong>PUSH Updates:</strong> The UI must update instantly when receiving <code>deviceStateUpdated</code> events from the Commutator.</li>
        </ul>
    </div>
</section>

<section>
    <h2>3. Diagnostics & Statistics (New)</h2>

    <h3>3.1. Network Metrics (RTT & Loss)</h3>
    <div class="card">
        <span class="tag">Requirement</span>
        <p>The application must measure connection quality to the Commutator.</p>
        <ul>
            <li><strong>Strict Polling:</strong> Send `queryState` requests sequentially. A new request is sent only after the previous one completes or times out.</li>
            <li><strong>Configurable Timeout:</strong> The user can adjust the timeout (default 2000ms).</li>
            <li><strong>Loss Calculation:</strong> If a request times out, it counts as packet loss.</li>
            <li><strong>Visualization:</strong> Render a <strong>Live RTT Chart</strong> showing latency over time.</li>
        </ul>
    </div>

    <h3>3.2. Sensor History Graphs</h3>
    <div class="card">
        <span class="tag">Requirement</span>
        <p>Visualize sensor data trends.</p>
        <ul>
            <li><strong>Toggle Visibility:</strong> Each sensor card must have a checkbox "Show on Graph".</li>
            <li><strong>Live Plotting:</strong> When a sensor value updates (via Polling or PUSH), add the point to the <strong>Sensor History Chart</strong>.</li>
        </ul>
    </div>
</section>

<section>
    <h2>4. API Reference (ADSL)</h2>
    
    <h3>Outgoing (GUI -> Commutator)</h3>
    <ul>
        <li><code>getSystemStructure(): HardwareDevice[]</code> - Initial load.</li>
        <li><code>executeActorCommand(localId, command): DeviceStateData</code> - User action.</li>
        <li><code>queryState(localId): DeviceStateData</code> - Used by Polling Manager for RTT.</li>
    </ul>

    <h3>Incoming (Commutator -> GUI)</h3>
    <ul>
        <li><code>deviceStateUpdated(localId, state)</code> - PUSH notification.</li>
    </ul>
</section>

</body>
</html>
````