import { SmartHomeController } from './SmartHomeController';
import { Record } from './aether_api';

const CORE_URI = "ws://reg-dev.aethernet.io:9011";
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
const uuidDisplay = document.getElementById('display-uuid')!;
const locationDisplay = document.getElementById('display-location')!;
const linkAdvanced = document.getElementById('link-advanced') as HTMLAnchorElement;

const controller = new SmartHomeController();
let currentDevice: SingleDeviceState | null = null;
let isCoreReady = false;

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

function showDisplayScreen(uuid: string) {
    screenConnect.classList.remove('visible');
    screenDisplay.classList.add('visible');
    uuidDisplay.textContent = uuid;

    // ИСПРАВЛЕНО: Ссылка на temp_test.html
    if (linkAdvanced) {
        linkAdvanced.href = `temp_test.html?uuid=${uuid}`;
    }

    currentDevice = {
        uuid: uuid,
        isActive: true
    };
}

function startDeviceWork() {
    if (currentDevice && isCoreReady) {
        controller.connectDevice(currentDevice.uuid);
    }
}

async function pollingLoop() {
    if (!currentDevice || !currentDevice.isActive) return;

    while (currentDevice.isActive) {
        try {
            const timeoutPromise = new Promise<never>((_, reject) =>
                setTimeout(() => reject(new Error("TIMEOUT")), POLL_TIMEOUT_MS)
            );

            const requestPromise = controller.requestRecords(currentDevice.uuid, 1);
            const records = await Promise.race([requestPromise, timeoutPromise]);

            if (records && records.length > 0) {
                const rec = records[0];

                const rawVal = rec.value & 0xFF;
                const temp = (rawVal / 3.0) - 30.0;

                updateUI(temp);
                updateStatus('Live', 'st-green');
            }

        } catch (e) {
            updateStatus('Timeout', 'st-red');
        }

        await new Promise(r => setTimeout(r, POLL_PERIOD_MS));
    }
}

function updateUI(temp: number) {
    tempDisplay.innerHTML = `${temp.toFixed(1)}<span class="unit">°C</span>`;
    const now = new Date();
    updateTimeEl.textContent = `Updated at ${now.toLocaleTimeString()}`;
}

function updateStatus(text: string, cssClass: string) {
    statusText.textContent = text;
    statusDot.className = `icon-status ${cssClass}`;
}

document.addEventListener('DOMContentLoaded', init);