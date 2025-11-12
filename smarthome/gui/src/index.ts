import { SmartHomeController } from './SmartHomeController';
import { UUID } from 'aether-client/build/aether_client';
import { Device, PendingPairing } from './aether_api';

// --- Константы (замени на свои) ---
const SERVICE_UUID_STRING = "A8348A48-64CC-A8EF-6902-090F446247C8"; // UUID твоего Java-сервиса (Хаба)
const REGISTRATION_URI = "ws://localhost:9011"; // URI твоего mock-сервера

// --- Глобальный контроллер ---
const controller = new SmartHomeController();

// --- Связывание с UI ---
document.addEventListener('DOMContentLoaded', () => {

    // Получаем ссылки на наши HTML-элементы
    const statusEl = document.getElementById('status-text')!;
    const refreshButton = document.getElementById('refresh-button')!;
    const commutatorsContainer = document.getElementById('commutators-container')!;
    const pairingsContainer = document.getElementById('pairings-container')!;
    const pairingsTitle = document.getElementById('pairings-title')!;

    // 1. Подписка на событие: Изменение статуса подключения
    controller.onConnectionStateChange.add(state => {
        statusEl.textContent = `Статус: ${state}`;
        if (state === 'connected') {
            statusEl.style.color = 'green';
            controller.fetchAllDevices();
            controller.fetchPendingPairings();
        } else if (state === 'error') {
            statusEl.style.color = 'red';
        }
    });

    // 2. Подписка на событие: Обновление списка устройств
    controller.onDeviceListUpdate.add(devices => {
        console.log("Updating device list", devices);
        renderDeviceTree(devices);
    });

    // 3. Подписка на событие: Обновление ОДНОГО устройства (PUSH)
    controller.onDeviceStateChanged.add(device => {
        console.log("Updating single device (PUSH)", device);
        const existingCard = document.getElementById(`device-${device.id}`);
        if (existingCard) {
            existingCard.replaceWith(createDeviceCard(device));
        } else {
            // Устройство новое, просто перезапрашиваем все
            controller.fetchAllDevices();
        }
    });

    // 4. Подписка на событие: Обновление списка сопряжений
    controller.onPairingListUpdate.add(pairings => {
        console.log("Updating pairing list", pairings);
        renderPairingList(pairings);
    });

    // 5. Подписка на событие: Новый запрос на сопряжение (PUSH)
    controller.onPairingRequested.add(pairing => {
        console.log("New pairing request (PUSH)", pairing);
        pairingsTitle.style.color = 'red'; // Привлекаем внимание
        // Просто запрашиваем полный список, чтобы избежать дубликатов
        controller.fetchPendingPairings();
    });

    // 6. Кнопка "Обновить"
    refreshButton.onclick = () => {
        controller.refreshAllSensors();
    };

    // --- Фабрики для HTML-элементов ---

    /**
     * "Рисует" древовидную структуру Коммутатор -> Устройства
     */
    function renderDeviceTree(devices: Device[]) {
        commutatorsContainer.innerHTML = ''; // Очистка

        if (devices.length === 0) {
            commutatorsContainer.innerHTML = '<p>Нет сопряженных устройств.</p>';
            return;
        }

        // Шаг 1: Группируем устройства по commutatorId
        const devicesByCommutator = new Map<string, Device[]>();
        for (const device of devices) {
            const commId = device.commutatorId.toString().toString();
            if (!devicesByCommutator.has(commId)) {
                devicesByCommutator.set(commId, []);
            }
            devicesByCommutator.get(commId)!.push(device);
        }

        // Шаг 2: "Рисуем" блоки для каждого коммутатора
        for (const [commId, deviceList] of devicesByCommutator.entries()) {
            const block = document.createElement('div');
            block.className = 'commutator-block';

            const title = document.createElement('h3');
            title.className = 'commutator-title';
            title.textContent = `Коммутатор: ${commId}`;

            const grid = document.createElement('div');
            grid.className = 'devices-grid';

            deviceList.forEach(device =>
                grid.appendChild(createDeviceCard(device))
            );

            block.appendChild(title);
            block.appendChild(grid);
            commutatorsContainer.appendChild(block);
        }
    }

    /**
     * "Рисует" список ожидающих сопряжения
     */
    function renderPairingList(pairings: PendingPairing[]) {
        pairingsContainer.innerHTML = ''; // Очистка

        if (pairings.length === 0) {
            pairingsTitle.style.color = 'black';
            pairingsContainer.innerHTML = '<p>Нет ожидающих устройств.</p>';
            return;
        }

        pairingsTitle.style.color = 'red';
        pairings.forEach(pairing => {
            const div = document.createElement('div');
            div.className = 'card pairing-card';

            const devicesStr = pairing.devices.map(d => `<li>${d.descriptor} (${d.getHardwareType()})</li>`).join('');

            div.innerHTML = `
                <h4>Новый Коммутатор</h4>
                <small>${pairing.commutatorId.toString()}</small>
                <p>Обнаруженные устройства:</p>
                <ul>${devicesStr}</ul>
            `;

            const btnApprove = document.createElement('button');
            btnApprove.textContent = "Одобрить";
            btnApprove.onclick = () => {
                controller.approvePairing(pairing.commutatorId);
            };

            div.appendChild(btnApprove);
            pairingsContainer.appendChild(div);
        });
    }

    /**
     * Создает HTML-карточку для одного устройства (Actor или Sensor)
     */
    function createDeviceCard(device: Device): HTMLElement {
        const div = document.createElement('div');
        div.className = 'card';
        div.id = `device-${device.id}`; // Глобальный уникальный ID

        const isActor = device.getDeviceType() === 'ACTOR';
        const unit = (device as any).unit || ''; // Для сенсоров

        div.innerHTML = `
            <h4>${device.name}</h4>
            <small>Comm-UUID: ${device.commutatorId.toString()}</small><br>
            <small>Local-ID: ${device.localDeviceId} | Global-ID: ${device.id}</small>
            <p>Тип: <b>${device.getDeviceType()}</b></p>
            <p>Состояние: <b>${device.lastState || 'N/A'}</b> ${unit}</p>
            <p>Обновлено: ${device.lastUpdated ? new Date(device.lastUpdated).toLocaleString() : 'N/A'}</p>
        `;

        if (isActor) {
            const controls = document.createElement('div');
            controls.style.marginTop = '10px';

            const btnOn = document.createElement('button');
            btnOn.textContent = "ВКЛ (ON)";
            btnOn.onclick = () => {
                const pkgOn = new Uint8Array([1]);
                controller.executeCommand(device.commutatorId, device.localDeviceId, pkgOn);
            };

            const btnOff = document.createElement('button');
            btnOff.textContent = "ВЫКЛ (OFF)";
            btnOff.onclick = () => {
                const pkgOff = new Uint8Array([0]);
                controller.executeCommand(device.commutatorId, device.localDeviceId, pkgOff);
            };

            controls.appendChild(btnOn);
            controls.appendChild(btnOff);
            div.appendChild(controls);
        }

        return div;
    }

    // --- 7. Запускаем! ---
    controller.connect(SERVICE_UUID_STRING, REGISTRATION_URI);

}); // Конец DOMContentLoaded