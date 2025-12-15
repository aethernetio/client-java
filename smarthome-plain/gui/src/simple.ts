import { SmartHomeController } from './SmartHomeController';

const CORE_URI = "wss://dbservice.aethernet.io:9013";
// const CORE_URI = "wss://localhost:9012";
const POLL_PERIOD_MS = 1000;
const POLL_TIMEOUT_MS = 2000;
const STORAGE_KEY = 'aether_single_device_v1';

interface SingleDeviceState {
    uuid: string;
    isActive: boolean;
}

const screenConnect = document.getElementById('screen-connect')!;
const screenDisplay = document.getElementById('screen-display')!;
const uuidInput = document.getElementById('uuid-input') as HTMLInputElement;
const btnConnect = document.getElementById('btn-connect')!;

const tempDisplay = document.getElementById('temp-display')!;
const statusDot = document.getElementById('status-dot')!;
const statusText = document.getElementById('status-text')!;
const updateTimeEl = document.getElementById('update-time')!;
const pingDisplay = document.getElementById('ping-display')!;
const uuidDisplay = document.getElementById('display-uuid')!;
const locationDisplay = document.getElementById('display-location')!;
const linkAdvanced = document.getElementById('link-advanced') as HTMLAnchorElement;

const controller = new SmartHomeController();
let currentDevice: SingleDeviceState | null = null;
let isCoreReady = false;

/**
 * Initializes the application state, checks URL parameters, and sets up event listeners.
 */
async function init() {
    const urlParams = new URLSearchParams(window.location.search);
    const urlUuid = urlParams.get('id') || urlParams.get('uuid');
    const urlLocation = urlParams.get('location');
    const storedUuid = localStorage.getItem(STORAGE_KEY);

    const targetUuid = urlUuid || storedUuid;

    if (urlLocation) {
        locationDisplay.textContent = urlLocation;
    } else {
        locationDisplay.textContent = "";
    }

    if (targetUuid) {
        showDisplayScreen(targetUuid);
    } else {
        screenConnect.classList.add('visible');
    }

    btnConnect.onclick = () => {
        const val = uuidInput.value.trim();
        if (val) {
            localStorage.setItem(STORAGE_KEY, val);
            const newUrl = window.location.protocol + "//" + window.location.host + window.location.pathname + '?uuid=' + val;
            window.history.pushState({path:newUrl},'',newUrl);

            showDisplayScreen(val);
            if (isCoreReady) startDeviceWork();
        }
    };

    controller.onConnectionState.add(state => {
        if (state === 'core_connected') {
            isCoreReady = true;
            updateStatus('Core Online', 'st-gray');
            startDeviceWork();
        } else if (state === 'disconnected') {
            isCoreReady = false;
            updateStatus('Disconnected', 'st-red');
        } else {
            updateStatus('Connecting...', 'st-gray');
        }
    });

    controller.onDeviceConnected.add(uuid => {
        if (currentDevice && currentDevice.uuid === uuid) {
            updateStatus('Live', 'st-green');
            pollingLoop();
        }
    });

    await controller.connectCore(CORE_URI);
}

/**
 * Transitions the UI to the display screen.
 * @param uuid - The UUID of the device to display.
 */
function showDisplayScreen(uuid: string) {
    screenConnect.classList.remove('visible');
    screenDisplay.classList.add('visible');
    uuidDisplay.textContent = uuid;

    if (linkAdvanced) {
        linkAdvanced.href = `temp_test.html?uuid=${uuid}`;
    }

    currentDevice = {
        uuid: uuid,
        isActive: true
    };
}

/**
 * Initiates the device connection if the core is ready.
 */
function startDeviceWork() {
    if (currentDevice && isCoreReady) {
        controller.connectDevice(currentDevice.uuid);
    }
}

/**
 * Periodically polls the device for data and updates the UI.
 * Calculates request latency.
 */
async function pollingLoop() {
    if (!currentDevice || !currentDevice.isActive) return;

    while (currentDevice.isActive) {
        try {
            const timeoutPromise = new Promise<never>((_, reject) =>
                setTimeout(() => reject(new Error("TIMEOUT")), POLL_TIMEOUT_MS)
            );

            const startTime = Date.now();

            const requestPromise = controller.requestRecords(currentDevice.uuid, 1);
            const records = await Promise.race([requestPromise, timeoutPromise]);

            const latency = Date.now() - startTime;

            if (records && records.length > 0) {
                const rec = records[0];

                const rawVal = rec.value & 0xFF;
                const temp = (rawVal / 3.0) - 30.0;

                updateUI(temp, latency);
                updateStatus('Live', 'st-green');
            }

        } catch (e) {
            updateStatus('Timeout', 'st-red');
            pingDisplay.textContent = '';
        }

        await new Promise(r => setTimeout(r, POLL_PERIOD_MS));
    }
}

/**
 * Updates the temperature display, timestamp, and latency information.
 * @param temp - The calculated temperature value.
 * @param latency - The round-trip time of the request in milliseconds.
 */
function updateUI(temp: number, latency: number) {
    tempDisplay.innerHTML = `${temp.toFixed(1)}<span class="unit">°C</span>`;
    const now = new Date();
    updateTimeEl.textContent = `Updated at ${now.toLocaleTimeString()}`;
    pingDisplay.textContent = `Latency: ${latency}ms`;
}

/**
 * Updates the status text and indicator color.
 * @param text - The status message to display.
 * @param cssClass - The CSS class for the status dot (e.g., 'st-green').
 */
function updateStatus(text: string, cssClass: string) {
    statusText.textContent = text;
    statusDot.className = `icon-status ${cssClass}`;
}

document.addEventListener('DOMContentLoaded', init);