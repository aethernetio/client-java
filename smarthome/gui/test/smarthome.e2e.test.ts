import { SmartHomeController } from '../src/SmartHomeController';
import { Log, LogLevel, LogFilter, UUID } from 'aether-client/build/aether_client';
// Импортируем классы, чтобы получить доступ к их методам (например, getHardwareType)
import { DeviceStateData, HardwareActor, HardwareSensor, VariantString, VariantDouble, HardwareDevice } from '../src/aether_api';

// Настраиваем логирование (если не настроено в другом месте)
Log.printConsolePlain(new LogFilter().notLevel(LogLevel.TRACE));
jest.setTimeout(120 * 1000);

describe('SmartHome P2P E2E Test', () => {
    let controller: SmartHomeController;

    beforeEach(() => {
        controller = new SmartHomeController();
    });

    afterEach(async () => {
        // Всегда корректно отключаем клиента после каждого теста
        if (controller) await controller.disconnect();
    });

    test('test:e2e-p2p-basic', async () => {
        const regUri = process.env.REG_URI;
        const commutatorUuid = process.env.COMMUTATOR_UUID;

        if (!regUri || !commutatorUuid) {
            throw new Error("Missing ENV vars: REG_URI or COMMUTATOR_UUID");
        }

        // --- ШАГ 1: Connect и получение списка устройств ---

        // 1.1. Устранение гонки #1: Начинаем слушать событие ПЕРЕД вызовом connect.
        const devicesPromise = new Promise<HardwareDevice[]>((resolve) => {
             // Подписываемся на событие, которое fire()ется внутри fetchStructure
             controller.onDeviceListUpdate.add(list => resolve(list));
        });

        // 1.2. Connect и ожидание подключения (запускает fetchStructure)
        await controller.connectAetherCore( regUri);
        await controller.connectCommutatorP2P(commutatorUuid);
        // 1.3. Ожидание списка устройств.
        const devices = await devicesPromise;

        // --- ШАГ 2: Проверка и подготовка команды ---

        expect(devices.length).toBe(2);

        // ИСПРАВЛЕНИЕ: Используем метод getHardwareType() вместо прямого доступа к полю hardwareType.
        // d.getHardwareType() - это сгенерированный геттер, возвращающий "ACTOR" или "SENSOR".
        const actor = devices.find(d => d.getHardwareType() === 'ACTOR');

        expect(actor).toBeDefined();

        // Проверяем, что actor действительно является HardwareActor (для доступа к localId)
        if (!actor) throw new Error("Actor device not found in structure.");

        // --- ШАГ 3: Отправка команды и ожидание PUSH-ответа ---

        // 3.1. Устранение гонки #2: Создаем Promise для ожидания PUSH-события "ON"
        const pushUpdatePromise = new Promise<{id: number, state: DeviceStateData}>((resolve) => {
            // Подписываемся ПЕРЕД отправкой команды
            controller.onDeviceStateChanged.add(evt => {
                if (evt.id === actor.localId) resolve(evt);
            });
        });

        // 3.2. Отправляем команду и ждем, пока она уйдет по сети (executeCommand -> flush)
        // ВАЖНО: Мы полагаемся, что executeCommand возвращает Promise, связанный с flush().
        Log.info("Sending ON command...");
        await controller.executeCommand(actor.localId, "ON");

        // 3.3. Ждем PUSH-ответа (теперь мы гарантированно подписаны)
        const pushUpdate = await pushUpdatePromise;

        Log.info("Received PUSH update:", pushUpdate);

        // --- ШАГ 4: Финальная проверка ---

        // Проверяем, что внутри VariantString "ON"
        const payload = pushUpdate.state.payload;
        // Проверяем, что это VariantString и его значение "ON"
        expect(payload).toBeInstanceOf(VariantString);
        expect((payload as VariantString).value).toBe("ON");
    });
});