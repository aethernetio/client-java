import { SmartHomeController } from './SmartHomeController';
import { HardwareDevice, HardwareSensor, DeviceStateData, VariantString, VariantDouble, VariantBool } from './aether_api';

// --- DOM REFERENCES ---
const statusText = document.getElementById('status-text');
const refreshButton = document.getElementById('refresh-button') as HTMLButtonElement;
const commutatorsTableBody = document.getElementById('commutators-table-body');
const devicesGrid = document.getElementById('devices-grid');
const currentCommutatorTitle = document.getElementById('current-commutator-title');

// CORE Connection Elements
const coreConnectionSection = document.getElementById('core-connection-section');
const uriInput = document.getElementById('reg-uri-input') as HTMLInputElement;
const connectCoreButton = document.getElementById('connect-core-button');

// APP Configuration Elements
const appConfigSection = document.getElementById('app-config-section');
const newUuidInput = document.getElementById('new-uuid-input') as HTMLInputElement;
const addButton = document.getElementById('add-commutator-button');

// --- STATE MANAGEMENT ---
interface CommutatorInfo {
    uuid: string;
    regUri: string;
    status: 'Disconnected' | 'Connecting' | 'Connected' | 'Error';
    deviceCount: number;
}

let knownDevices = new Map<number, HTMLElement>();
let knownCommutators = new Map<string, CommutatorInfo>();

let coreController: SmartHomeController | null = null;
let currentCommutatorUuid: string | null = null;
let isAetherCoreConnected = false;


// --- RENDERING UTILITIES ---

function formatState(state: DeviceStateData, device: HardwareDevice): { value: string, unit: string, time: string } {
    if (!state || !state.payload) return { value: 'N/A', unit: '', time: 'N/A' };

    let value: string;
    let unit = '';

    if (device instanceof HardwareSensor && device.unit) {
        unit = device.unit;
    }

    if (state.payload instanceof VariantDouble) {
        value = state.payload.value.toFixed(1);
    } else if (state.payload instanceof VariantString) {
        value = state.payload.value;
    } else if (state.payload instanceof VariantBool) {
        value = state.payload.value ? 'ON' : 'OFF';
    } else {
        value = 'UNKNOWN';
    }

    const time = new Date(state.timestamp.getTime()).toLocaleTimeString();

    return { value, unit, time };
}

function renderDeviceCard(device: HardwareDevice, initialState: DeviceStateData | null = null): HTMLElement {
    const isActor = device.getHardwareType() === 'ACTOR';
    const deviceKey = device.localId;
    let card = knownDevices.get(deviceKey) as HTMLElement | undefined;
    let state = initialState;

    if (!card) {
        card = document.createElement('div');
        card.className = `card ${isActor ? 'card-actor' : 'card-sensor'}`;
        card.innerHTML = `
            <h4>${device.descriptor} <small>(${isActor ? 'ACTOR' : 'SENSOR'})</small></h4>
            <p>Состояние: <strong data-role="value">N/A</strong> <span data-role="unit"></span></p>
            <p><small>Обновлено: <span data-role="time">N/A</span></small></p>
            ${isActor ? '<div class="controls" data-role="controls"></div>' : ''}
        `;
        knownDevices.set(deviceKey, card);

        if (isActor) {
            const controls = card.querySelector('[data-role="controls"]')!;

            const btnOn = document.createElement('button');
            btnOn.textContent = 'ВКЛ';
            btnOn.onclick = () => sendCommand(device.localId, 'ON');

            const btnOff = document.createElement('button');
            btnOff.textContent = 'ВЫКЛ';
            btnOff.onclick = () => sendCommand(device.localId, 'OFF');

            controls.appendChild(btnOn);
            controls.appendChild(btnOff);
        }
    }

    (card as any).updateState = (newState: DeviceStateData) => {
        state = newState || state;
        if (state) {
            const { value, unit, time } = formatState(state, device);
            card!.querySelector('[data-role="value"]')!.textContent = value;
            card!.querySelector('[data-role="unit"]')!.textContent = unit;
            card!.querySelector('[data-role="time"]')!.textContent = time;
        }
    };

    if (initialState) {
        (card as any).updateState(initialState);
    }

    return card;
}


function renderDeviceList(devices: HardwareDevice[], commutatorUuid: string) {
    devicesGrid!.innerHTML = '';
    knownDevices.clear();

    if (devices.length === 0) {
        devicesGrid!.innerHTML = '<p>Устройства не найдены.</p>';
        return;
    }

    currentCommutatorTitle!.style.display = 'block';
    currentCommutatorTitle!.textContent = `Устройства: ${commutatorUuid.substring(0, 8)}...`;

    devices.forEach(device => {
        const card = renderDeviceCard(device);
        devicesGrid!.appendChild(card);
    });
}

function renderCommutatorTable() {
    commutatorsTableBody!.innerHTML = '';
    knownCommutators.forEach(info => {
        const row = document.createElement('tr');
        const statusColor = info.status === 'Connected' ? 'green' : (info.status === 'Error' ? 'red' : 'orange');

        const actionDisabled = !isAetherCoreConnected || info.status === 'Connected' ? 'disabled' : '';
        const buttonText = info.status === 'Connected' ? 'Подключено' : (info.status === 'Connecting' ? '...' : 'Подключить');

        row.innerHTML = `
            <td>${info.uuid.substring(0, 8)}...</td>
            <td><span style="color: ${statusColor}">${info.status}</span></td>
            <td>${info.deviceCount}</td>
            <td class="action-column">
                <button onclick="window.connectCommutatorP2PHandler('${info.uuid}')"
                        ${actionDisabled}>${buttonText}</button>
            </td>
        `;
        commutatorsTableBody!.appendChild(row);
    });
}

// --- CORE LOGIC HANDLERS ---

async function connectAetherCore() {
    const regUri = uriInput.value.trim();
    if (!regUri) {
        statusText!.textContent = 'Ошибка: Пожалуйста, введите URI регистрации.';
        statusText!.style.color = 'red';
        return;
    }

    statusText!.textContent = `Статус: Подключение к Aether Core (${regUri})...`;
    connectCoreButton!.setAttribute('disabled', 'true');
    uriInput.setAttribute('disabled', 'true');

    // Полный сброс при новом подключении к Core
    if (coreController) {
        await coreController.disconnect().catch(console.warn);
    }

    try {
        coreController = new SmartHomeController();

        await coreController.connectAetherCore(regUri);

        statusText!.textContent = `Статус: УСПЕХ! Подключено к Aether Core. Клиент UUID: ${coreController.client!.getUid()?.toString().toString().substring(0, 8)}...`;
        statusText!.style.color = 'green';
        isAetherCoreConnected = true;

        coreConnectionSection!.style.display = 'none';
        appConfigSection!.style.display = 'flex';

        renderCommutatorTable();

        // Подписываемся на события ОДИН РАЗ при создании Core Controller
        coreController.onDeviceListUpdate.add((devices: HardwareDevice[]) => {
            if (currentCommutatorUuid) {
                renderDeviceList(devices, currentCommutatorUuid);
                updateCommutatorStatus(currentCommutatorUuid, 'Connected', devices.length);
            }
        });

        coreController.onDeviceStateChanged.add(evt => {
            const card = knownDevices.get(evt.id);
            if (card && (card as any).updateState) {
                (card as any).updateState(evt.state);
            }
        });

    } catch (e) {
        statusText!.textContent = `Статус: ОШИБКА. Не удалось подключиться к Aether Core. Проверьте URI и сервер.`;
        statusText!.style.color = 'red';
        connectCoreButton!.removeAttribute('disabled');
        uriInput.removeAttribute('disabled');
        isAetherCoreConnected = false;
        coreController = null;
        console.error('Aether Core Connection Failed:', e);
    }
}


// --- COMMUTATOR LOGIC HANDLERS (P2P) ---

(window as any).connectCommutatorP2PHandler = async function (uuid: string) {
    const info = knownCommutators.get(uuid);
    if (!info || info.status === 'Connecting' || info.status === 'Connected' || !isAetherCoreConnected) return;
    if (!coreController) return;

    // 1. Сброс предыдущего P2P-канала (без разрыва связи с Core)
    // ИСПРАВЛЕНИЕ: Используем disconnectP2P() вместо disconnect()
    if (currentCommutatorUuid) {
         updateCommutatorStatus(currentCommutatorUuid, 'Disconnected');
    }
    await coreController.disconnectP2P();

    currentCommutatorUuid = uuid;
    updateCommutatorStatus(uuid, 'Connecting');

    try {
        await coreController.connectCommutatorP2P(uuid);
        refreshButton!.removeAttribute('disabled');
    } catch (e) {
        console.error(e);
        updateCommutatorStatus(uuid, 'Error');
        refreshButton!.setAttribute('disabled', 'true');
    }
}

async function sendCommand(localId: number, command: string): Promise<void> {
    if (!coreController || !coreController.commutatorApi) {
        statusText!.textContent = 'Ошибка: P2P-соединение с коммутатором не активно.';
        statusText!.style.color = 'red';
        return;
    }
    try {
        statusText!.textContent = `Отправка команды ${command} на ID ${localId}...`;
        await coreController.executeCommand(localId, command);
    } catch (e) {
        statusText!.textContent = `Ошибка при отправке команды: ${(e as Error).message}`;
        console.error('Command execution failed:', e);
    }
}

function updateCommutatorStatus(uuid: string, status: CommutatorInfo['status'], deviceCount?: number) {
    const info = knownCommutators.get(uuid);
    if (info) {
        info.status = status;
        if (deviceCount !== undefined) {
            info.deviceCount = deviceCount;
        }
        renderCommutatorTable();
    }
}


// --- INITIALIZATION ---

function initApp() {
    connectCoreButton!.onclick = connectAetherCore;

    addButton!.onclick = () => {
        const uuid = newUuidInput.value.trim();
        const uri = uriInput.value.trim();

        if (!uuid) {
            statusText!.textContent = 'Ошибка: UUID не может быть пустым.';
            statusText!.style.color = 'red';
            return;
        }

        if (!isAetherCoreConnected) {
             statusText!.textContent = 'Ошибка: Сначала подключитесь к Aether Core.';
             statusText!.style.color = 'red';
             return;
        }

        if (uuid && uri && !knownCommutators.has(uuid)) {
            knownCommutators.set(uuid, {
                uuid,
                regUri: uri,
                status: 'Disconnected',
                deviceCount: 0,
            });
            newUuidInput.value = '';
            renderCommutatorTable();
            statusText!.textContent = 'Коммутатор добавлен в список. Нажмите "Подключить".';
        } else if (knownCommutators.has(uuid)) {
            statusText!.textContent = 'Ошибка: Коммутатор с таким UUID уже существует.';
        }
    };

    refreshButton!.onclick = async () => {
        if (!coreController || !coreController.commutatorApi) return;
        statusText!.textContent = 'Запрос последних состояний...';
        await coreController.queryAllSensorStates();
        statusText!.textContent = 'Запрос состояний отправлен.';
    };

    appConfigSection!.style.display = 'none';
    uriInput.value = 'ws://localhost:9011';
    renderCommutatorTable();
}

initApp();