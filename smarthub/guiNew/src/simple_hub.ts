// FILE: simple_hub.ts
import { SmartHubController, DeviceUpdate } from './SmartHubController';
import { UUID } from 'aether-client/build/aether_client';
import { DeviceRecord } from './aether_api';
import Chart from 'chart.js/auto';

// Элементы UI
const btnConnect = document.getElementById('btn-connect')!;
const btnRestore = document.getElementById('btn-restore')!;
const btnDisconnect = document.getElementById('btn-disconnect')!;
const btnRefresh = document.getElementById('btn-refresh')!;
const btnSubscribe = document.getElementById('btn-subscribe')!;

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

// Контроллер
const controller = new SmartHubController();

// Состояние
let selectedDeviceUuid: string | null = null;
let tempChart: Chart | null = null;
let chartData: { labels: string[], values: number[] } = { labels: [], values: [] };

// Вспомогательные функции
function addLog(msg: string) {
    const time = new Date().toLocaleTimeString();
    const entry = document.createElement('div');
    entry.className = 'log-entry';
    entry.textContent = `[${time}] ${msg}`;
    eventLog.prepend(entry);
    
    // Ограничим количество логов
    while (eventLog.children.length > 50) {
        eventLog.removeChild(eventLog.lastChild!);
    }
}

function updateStatus(state: 'disconnected' | 'connecting' | 'connected' | 'error') {
    statusDot.className = 'status-dot';
    switch(state) {
        case 'disconnected':
            statusDot.classList.add('gray');
            statusText.textContent = 'Disconnected';
            break;
        case 'connecting':
            statusDot.classList.add('blue');
            statusText.textContent = 'Connecting...';
            break;
        case 'connected':
            statusDot.classList.add('green');
            statusText.textContent = 'Connected';
            break;
        case 'error':
            statusDot.classList.add('red');
            statusText.textContent = 'Error';
            break;
    }
}

function renderDeviceList(devices: UUID[]) {
    devicesCount.textContent = devices.length.toString();
    
    if (devices.length === 0) {
        deviceList.innerHTML = '<div class="device-item" style="text-align: center; color: #666;">No devices found</div>';
        return;
    }
    
    deviceList.innerHTML = devices.map(device => {
        const deviceUuid = device.toString();
        const cached = controller.getCachedDeviceData(deviceUuid);
        const lastTemp = cached && cached.length > 0 
            ? ((cached[0].value / 3.0) - 30.0).toFixed(1) + '°C'
            : '--°C';
        const isSelected = selectedDeviceUuid === deviceUuid ? 'selected' : '';
        
        return `
            <div class="device-item ${isSelected}" data-uuid="${deviceUuid}">
                <div class="device-id">${deviceUuid.substring(0, 8)}...</div>
                <div class="device-preview">
                    <span class="temp-value">${lastTemp}</span>
                    <span class="time-value">${cached ? new Date().toLocaleTimeString() : 'No data'}</span>
                </div>
            </div>
        `;
    }).join('');
    
    // Добавляем обработчики кликов
    document.querySelectorAll('.device-item').forEach(el => {
        el.addEventListener('click', () => {
            const uuid = el.getAttribute('data-uuid');
            if (uuid) selectDevice(uuid);
        });
    });
}

function selectDevice(uuid: string) {
    selectedDeviceUuid = uuid;
    selectedDeviceId.textContent = `Device: ${uuid}`;
    
    // Обновляем стиль списка
    document.querySelectorAll('.device-item').forEach(el => {
        el.classList.remove('selected');
        if (el.getAttribute('data-uuid') === uuid) {
            el.classList.add('selected');
        }
    });
    
    // Показываем детали
    deviceDetails.style.display = 'block';
    
    // Загружаем данные
    loadDeviceData(uuid);
}

async function loadDeviceData(uuid: string) {
    try {
        const records = await controller.requestDeviceData(uuid, 20);
        updateDeviceDisplay(uuid, records);
    } catch (e) {
        addLog(`Failed to load device data: ${e}`);
    }
}

function updateDeviceDisplay(uuid: string, records: DeviceRecord[]) {
    if (uuid !== selectedDeviceUuid) return;
    
    if (records.length > 0) {
        // Конвертируем значение обратно в температуру
        const temp = (records[0].value / 3.0) - 30.0;
        currentTemp.textContent = temp.toFixed(1) + '°C';
        lastUpdate.textContent = new Date().toLocaleTimeString();
        
        // Обновляем график
        updateChart(records);
    }
    
    // Обновляем список устройств (для отображения последнего значения)
    renderDeviceList(controller.getCachedDevices());
}

function updateChart(records: DeviceRecord[]) {
    if (!tempChart) {
        const ctx = (document.getElementById('temp-chart') as HTMLCanvasElement).getContext('2d');
        if (!ctx) return;
        
        tempChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [{
                    label: 'Temperature',
                    data: [],
                    borderColor: '#007aff',
                    backgroundColor: 'rgba(0, 122, 255, 0.1)',
                    tension: 0.4,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: {
                        grid: { color: '#333' },
                        ticks: { color: '#888' }
                    },
                    x: {
                        grid: { display: false },
                        ticks: { color: '#888' }
                    }
                }
            }
        });
    }
    
    // Обновляем данные
    const temps = records.map(r => (r.value / 3.0) - 30.0);
    const labels = records.map((_, i) => `-${records.length - i}s`);
    
    tempChart.data.labels = labels;
    tempChart.data.datasets[0].data = temps;
    tempChart.update();
}

// Инициализация
async function init() {

    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('uuid')) {
        addLog('Found UUID in URL, connecting automatically...');
        setTimeout(() => btnConnect.click(), 500);
    }

    // Загружаем сохраненный UUID если есть
    const savedUuid = localStorage.getItem('smarthub_last_service');
    if (savedUuid) {
    }
    
    // Проверяем, есть ли сохраненная сессия
    const hasSession = await controller.restoreSession();
    if (hasSession) {
        screenConnect.classList.remove('visible');
        screenDisplay.classList.add('visible');
        updateStatus('disconnected');
        addLog('Session restored, ready to connect');
    } else {
        screenConnect.classList.add('visible');
    }
    
    // Обработчики событий контроллера
    controller.onConnectionStateChange.add(state => {
        updateStatus(state);
        addLog(`Connection state: ${state}`);
    });
    
    controller.onDeviceListUpdate.add(devices => {
        renderDeviceList(devices);
        addLog(`Device list updated: ${devices.length} devices`);
        
        // Если есть устройства и ни одно не выбрано, выбираем первое
        if (devices.length > 0 && !selectedDeviceUuid) {
            selectDevice(devices[0].toString());
        }
    });
    
    controller.onDeviceDataUpdate.add((update: DeviceUpdate) => {
        addLog(`Data received from ${update.deviceUid.substring(0, 8)}... (${update.records.length} records)`);
        
        if (update.deviceUid === selectedDeviceUuid) {
            updateDeviceDisplay(update.deviceUid, update.records);
        } else {
            // Просто обновляем список устройств
            renderDeviceList(controller.getCachedDevices());
        }
    });
    
    controller.onError.add(error => {
        addLog(`Error: ${error}`);
    });
    
    // Обработчики кнопок
btnConnect.onclick = async () => {
    const urlParams = new URLSearchParams(window.location.search);
    const serviceUuid = urlParams.get('uuid') || localStorage.getItem('smarthub_last_service');
    const regUri = urlParams.get('reg') || 'wss://dbservice.aethernet.io:9013';
    
    if (!serviceUuid) {
        addLog('Error: No UUID provided. Use ?uuid=... in URL');
        return;
    }
        
        try {
            screenConnect.classList.remove('visible');
            screenDisplay.classList.add('visible');
            
            await controller.connect(serviceUuid, regUri);
            
            // Сохраняем UUID
            localStorage.setItem('smarthub_last_service', serviceUuid);
            controller.saveSession();
            
        } catch (e) {
            screenConnect.classList.add('visible');
            screenDisplay.classList.remove('visible');
            addLog(`Connection failed: ${e}`);
        }
    };
    
    btnRestore.onclick = async () => {
        const hasSession = await controller.restoreSession();
        if (hasSession) {
            screenConnect.classList.remove('visible');
            screenDisplay.classList.add('visible');
            updateStatus('disconnected');
            addLog('Session restored');
        } else {
            addLog('No session to restore');
        }
    };
    
    btnDisconnect.onclick = async () => {
        await controller.disconnect();
        screenConnect.classList.add('visible');
        screenDisplay.classList.remove('visible');
        selectedDeviceUuid = null;
        if (tempChart) {
            tempChart.destroy();
            tempChart = null;
        }
    };
    
    btnRefresh.onclick = async () => {
        if (selectedDeviceUuid) {
            await loadDeviceData(selectedDeviceUuid);
        }
    };
    
    btnSubscribe.onclick = async () => {
        if (selectedDeviceUuid) {
            try {
                const result = await controller.subscribeToDevice(selectedDeviceUuid);
                addLog(result ? 'Subscribed successfully' : 'Subscription failed');
            } catch (e) {
                addLog(`Subscription error: ${e}`);
            }
        }
    };
}

// Запуск
document.addEventListener('DOMContentLoaded', init);