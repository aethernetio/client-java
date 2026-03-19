import { SmartHomeController } from './SmartHomeController';

const CORE_URI = "wss://dbservice.aethernet.io:9013";
const POLL_TIMEOUT_MS = 5000;
const STORAGE_KEY = 'aether_single_device_v1';

const tempDisplay = document.getElementById('temp-display')!;
const statusDot = document.getElementById('status-dot')!;
const statusText = document.getElementById('status-text')!;
const updateTimeEl = document.getElementById('update-time')!;
const pingDisplay = document.getElementById('ping-display')!;
const minPingDisplay = document.getElementById('min-ping-display')!;
const locationDisplay = document.getElementById('display-location')!;
const eventLog = document.getElementById('event-log')!;
const btnConnect = document.getElementById('btn-connect')!;

btnConnect.onclick = () => {
    const uuid = uuidInput.value.trim();
    if (uuid) {
        localStorage.setItem(STORAGE_KEY, uuid);
        currentDevice = { uuid: uuid, isActive: true };
        
        screenConnect.classList.remove('visible');
        screenDisplay.classList.add('visible');
        
        // Manual connect log (silent)
        controller.connectDevice(uuid);
        // pollingLoop triggered via onDeviceConnected listener
    }
};

const uuidInput = document.getElementById('uuid-input') as HTMLInputElement;
const screenConnect = document.getElementById('screen-connect')!;
const screenDisplay = document.getElementById('screen-display')!;

const controller = new SmartHomeController();

let minLatency = 60000;
let lastTemp: string = "--";
let lastLatency = 0;
let currentDevice: {uuid: string, isActive: boolean} | null = null;


function addLog(msg: string) {
    const time = new Date().toLocaleTimeString();
    const entry = document.createElement('div');
    entry.style.borderBottom = "1px solid #f0f0f0";
    entry.textContent = `[${time}] ${msg}`;
    eventLog.prepend(entry);
}


function updateStatus(text: string, cssClass: string, target: 'cloud' | 'sensor' = 'cloud') {
    statusText.textContent = text;
    const dotId = target === 'cloud' ? 'status-dot-cloud' : 'status-dot-sensor';
    const dot = document.getElementById(dotId)!;
    dot.className = `icon-status ${cssClass}`;
}

function updateUI(temp: number, latency: number) {
    tempDisplay.innerHTML = `${temp.toFixed(1)}<span class="unit">°C</span>`;
    updateTimeEl.textContent = `Updated: ${new Date().toLocaleTimeString()}`;
    pingDisplay.textContent = `Ping: ${latency}ms`;
    minPingDisplay.textContent = `Min: ${minLatency}ms`;
}




async function pollingLoop() {
    if (!currentDevice || !currentDevice.isActive) return;

    let responded = false;
    const timeout = setTimeout(() => {
        if (!responded) {
            updateStatus('Timeout', 'st-red', 'sensor');
            // Если за 3 сек не ответил — пробуем снова
            pollingLoop();
        }
    }, 3000);

    try {
        updateStatus('Requesting...', 'st-blue', 'sensor');
        const startTime = Date.now();
        const records = await controller.requestRecords(currentDevice.uuid, 1);
        
        responded = true;
        clearTimeout(timeout);
        
        const latency = Date.now() - startTime;
        if (records && records.length > 0) {
            const rawVal = records[0].value & 0xFF;
            const temp = (rawVal / 3.0) - 30.0;
            minLatency = Math.min(minLatency, latency);
            lastTemp = temp.toFixed(1);
            updateUI(temp, latency);
            updateStatus('Online', 'st-green', 'sensor');
            setTimeout(pollingLoop, 10000); // Успех: ждем 10с
        } else {
            throw new Error("Empty");
        }
    } catch (e) {
        responded = true;
        clearTimeout(timeout);
        updateStatus('Error', 'st-red', 'sensor');
        setTimeout(pollingLoop, 3000); // Ошибка: ждем 3с
    }
}



async function init() {
    const urlParams = new URLSearchParams(window.location.search);
    const targetUuid = urlParams.get('uuid') || localStorage.getItem(STORAGE_KEY);
    const urlLocation = urlParams.get('location');

    if (urlLocation) locationDisplay.textContent = urlLocation;
    
    minPingDisplay.ondblclick = () => {
        minLatency = 60000;
        addLog("Min Latency reset");
        if (lastTemp !== "--") updateUI(parseFloat(lastTemp), lastLatency);
    };

    if (targetUuid) {
        screenConnect.classList.remove('visible');
        screenDisplay.classList.add('visible');
        currentDevice = { uuid: targetUuid, isActive: true };
    } else {
        screenConnect.classList.add('visible');
    }


    controller.onConnectionState.add(state => {
        if (state === 'core_connected') {
            updateStatus('Cloud OK', 'st-green', 'cloud');
            if (currentDevice) controller.connectDevice(currentDevice.uuid);
        } else {
            updateStatus('Offline', 'st-red', 'cloud');
        }
    });


    controller.onDeviceConnected.add(uuid => {
        if (currentDevice?.uuid === uuid) pollingLoop();
    });

    await controller.connectCore(CORE_URI);
}

document.addEventListener('DOMContentLoaded', init);

// Safari wake-up fix
document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
        // Safari wake-up: Silent refresh
        if (currentDevice?.isActive) pollingLoop();
    }
});