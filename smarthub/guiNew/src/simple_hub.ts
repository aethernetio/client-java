import { SmartHubController, DeviceUpdate } from './SmartHubController';
import { UUID } from 'aether-client';
import { SensorRecord } from './aether_api';
import Chart from 'chart.js/auto';

// UI Elements
const btnConnect = document.getElementById('btn-connect')!;
const btnDisconnect = document.getElementById('btn-disconnect')!;
const btnRefresh = document.getElementById('btn-refresh')!;
const screenConnect = document.getElementById('screen-connect')!;
const screenDisplay = document.getElementById('screen-display')!;
const statusDot = document.getElementById('status-dot')!;
const statusText = document.getElementById('status-text')!;
const devicesCount = document.getElementById('devices-count')!;
const deviceList = document.getElementById('device-list')!;
const deviceDetails = document.getElementById('device-details')!;
const selectedDeviceId = document.getElementById('selected-device-id')!;
const currentTemp = document.getElementById('current-temp')!;
const lastUpdate = document.getElementById('last-update')!;
const eventLog = document.getElementById('event-log')!;

const controller = new SmartHubController();
let selectedDeviceUuid: string | null = null;
let dataPollingInterval: any = null;
let tempChart: Chart | null = null;

function addLog(msg: string) {
    const time = new Date().toLocaleTimeString();
    const entry = document.createElement('div');
    entry.className = 'log-entry';
    entry.textContent = `[${time}] ${msg}`;
    eventLog.prepend(entry);
    while (eventLog.children.length > 50) eventLog.removeChild(eventLog.lastChild!);
}

function updateStatus(state: 'disconnected' | 'connecting' | 'connected' | 'error') {
    statusDot.className = 'status-dot';
    switch(state) {
        case 'disconnected': statusDot.classList.add('gray'); statusText.textContent = 'Disconnected'; break;
        case 'connecting': statusDot.classList.add('blue'); statusText.textContent = 'Connecting...'; break;
        case 'connected': statusDot.classList.add('green'); statusText.textContent = 'Connected'; break;
        case 'error': statusDot.classList.add('red'); statusText.textContent = 'Error'; break;
    }
}

function renderDeviceList(devices: UUID[]) {
    devicesCount.textContent = devices.length.toString();
    
    if (devices.length === 0) {
        deviceList.innerHTML = '<div class="device-item" style="text-align: center; color: #666;">No devices found</div>';
        return;
    }
    deviceList.innerHTML = devices.map(device => {
        const deviceUuid = device.toAString().toString();
        const cached = controller.getCachedDeviceData(deviceUuid);
        const lastTemp = cached && cached.length > 0 ? (((cached[0].value & 0xFF) / 3.0) - 30.0).toFixed(1) + '°C' : '--°C';
        const isSelected = selectedDeviceUuid === deviceUuid ? 'selected' : '';
        return `<div class="device-item ${isSelected}" data-uuid="${deviceUuid}">
                    <div class="device-id">${deviceUuid.substring(0, 8)}...</div>
                    <div class="device-preview">
                        <span class="temp-value">${lastTemp}</span>
                        <span class="time-value">${cached ? new Date().toLocaleTimeString() : 'No data'}</span>
                    </div>
                </div>`;
    }).join('');
    document.querySelectorAll('.device-item').forEach(el => {
        el.addEventListener('click', () => {
            const uuid = el.getAttribute('data-uuid');
            if (uuid) {
                selectDevice(uuid);
                controller.requestDeviceData(uuid, 20);
            }
        });
    });
}


function selectDevice(uuid: string) {
    selectedDeviceUuid = uuid;
    selectedDeviceId.textContent = `Device: ${uuid}`;
    document.querySelectorAll('.device-item').forEach(el => el.classList.remove('selected'));
    const selectedEl = document.querySelector(`.device-item[data-uuid="${uuid}"]`);
    if (selectedEl) selectedEl.classList.add('selected');
    deviceDetails.style.display = 'block';
    // Запрашиваем историю устройства (результат придёт через событие onDeviceDataUpdate)
        if (dataPollingInterval) clearInterval(dataPollingInterval);
        
        controller.requestDeviceData(uuid, 20);
        
        dataPollingInterval = setInterval(() => {
            if (selectedDeviceUuid) {
                controller.requestDeviceData(selectedDeviceUuid, 20);
            }
        }, 5000);
    addLog(`Requested history for ${uuid.substring(0, 8)}...`);
}

async function loadDeviceData(uuid: string) {
    // Метод оставлен для совместимости, но теперь он просто вызывает запрос
    controller.requestDeviceData(uuid, 20);
    addLog(`Requested history for ${uuid.substring(0, 8)}...`);
}


function updateDeviceDisplay(uuid: string, records: SensorRecord[]) {
    if (uuid !== selectedDeviceUuid) return;
    if (records.length > 0) {
        const temp = ((records[0].value & 0xFF) / 3.0) - 30.0;
        currentTemp.textContent = temp.toFixed(1) + '°C';
        lastUpdate.textContent = new Date().toLocaleTimeString();
        updateChart(records);
    }
    renderDeviceList(controller.getCachedDevices());
}

function updateChart(records: SensorRecord[]) {
    if (!tempChart) {
        const ctx = (document.getElementById('temp-chart') as HTMLCanvasElement).getContext('2d');
        if (!ctx) return;
        tempChart = new Chart(ctx, {
            type: 'line',
            data: { labels: [], datasets: [{ label: 'Temperature', data: [], borderColor: '#007aff', backgroundColor: 'rgba(0, 122, 255, 0.1)', tension: 0.4, fill: true }] },
            options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { grid: { color: '#333' }, ticks: { color: '#888' } }, x: { grid: { display: false }, ticks: { color: '#888' } } } }
        });
    }
    const temps = records.map(r => ((r.value & 0xFF) / 3.0) - 30.0);
    const labels = records.map((_, i) => `-${records.length - i}s`);
    tempChart.data.labels = labels;
    tempChart.data.datasets[0].data = temps;
    tempChart.update();
}


async function init() {
    const urlParams = new URLSearchParams(window.location.search);
    const serviceUuid = urlParams.get('uuid') || localStorage.getItem('smarthub_last_service');
    const regUri = urlParams.get('reg') || 'wss://dbservice.aethernet.io:9013';
    if (serviceUuid) localStorage.setItem('smarthub_last_service', serviceUuid);
    if (serviceUuid) setTimeout(() => btnConnect.click(), 100);

    controller.onConnectionStateChange.add(state => updateStatus(state));

    controller.onDeviceListUpdate.add(devices => {
        renderDeviceList(devices);
        if (devices.length > 0 && !selectedDeviceUuid) selectDevice(devices[0].toAString().toString());
    });
    controller.onDeviceDataUpdate.add((update: DeviceUpdate) => {
        if (update.deviceUid === selectedDeviceUuid) updateDeviceDisplay(update.deviceUid, update.records);
        else renderDeviceList(controller.getCachedDevices());
    });
    controller.onError.add(error => addLog(`Error: ${error}`));

    btnConnect.onclick = () => {
        const uuidInput = document.getElementById('service-uuid') as HTMLInputElement;
        const uuid = uuidInput ? uuidInput.value : serviceUuid;
        if (!uuid) { addLog('No service UUID provided'); return; }
        screenConnect.classList.remove('visible');
        screenDisplay.classList.add('visible');
        controller.connect(uuid, regUri).then(() => {
            controller.saveSession();
        }).catch(e => {
            screenConnect.classList.add('visible');
            screenDisplay.classList.remove('visible');
            addLog(`Connection failed: ${e}`);
        });
    };

    btnDisconnect.onclick = () => {
        controller.disconnect().then(() => {
            screenConnect.classList.add('visible');
            if (dataPollingInterval) { clearInterval(dataPollingInterval); dataPollingInterval = null; }
            screenDisplay.classList.remove('visible');
            selectedDeviceUuid = null;
            if (tempChart) { tempChart.destroy(); tempChart = null; }
        });
    };

    btnRefresh.onclick = () => {
        if (selectedDeviceUuid) {
            controller.requestDeviceData(selectedDeviceUuid, 20);
            addLog(`Refreshing data for ${selectedDeviceUuid.substring(0, 8)}...`);
        } else {
            // Если метода getDevices нет, вызываем тот, что инициирует обновление списка
            controller.connect(localStorage.getItem('smarthub_last_service') || '', regUri);
            addLog('Refreshing connection/devices...');
        }
    };

    const hasSession = controller.restoreSession();
    hasSession.then(has => {
        if (has) {
            screenConnect.classList.remove('visible');
            screenDisplay.classList.add('visible');
            updateStatus('disconnected');
        } else {
            screenConnect.classList.add('visible');
        }
    });
}


document.addEventListener('DOMContentLoaded', init);