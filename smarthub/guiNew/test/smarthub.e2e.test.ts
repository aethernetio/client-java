import { describe, beforeAll, afterEach, test, expect } from '@jest/globals';
import { 
    AetherCloudClient, 
    ClientStateInMemory, 
    UUID, 
    AFuture,
    applySodium,
    Log,
    LogFilter,
    MessageEventListenerDefault
} from 'aether-client';
import { 
    SmartHomeClientToServiceApi,
    SmartHomeClientToServiceApiRemote,
    SmartHomeClientGuiApi,
    SmartHomeClientGuiApiLocal,
    DeviceRecord 
} from '../src/aether_api';

Log.printConsolePlain(new LogFilter());

const REG_URI = "ws://localhost:9011";
const SERVICE_UUID = "00000000-0000-0000-0000-000000000001";
const TEST_DEVICE_UUID = "00000000-0000-0000-0000-000000000002";

describe('SmartHubService E2E', () => {
    beforeAll(async () => {
        await applySodium();
    });

    let client: AetherCloudClient;

    afterEach(async () => {
        if (client) {
            await client.destroy(true).toPromise();
        }
    });

    test('should connect to service and get device list', async () => {
        // 1. Создаем клиента
        const parent = UUID.fromString("B1AC52C8-8D94-BD39-4C01-A631AC594165");
        const config = new ClientStateInMemory(parent, [REG_URI], undefined);
        client = new AetherCloudClient(config, "test-client");

        // 2. Подключаемся к сети
        await client.connect().toPromise(30000);
        expect(client.getUid()).toBeDefined();
        
        // 3. Создаем MessageNode для подключения к сервису
        const serviceUuid = UUID.fromString(SERVICE_UUID);
        const node = client.getMessageNode(serviceUuid, MessageEventListenerDefault);

        // 4. Создаем локальную реализацию API
        const receivedDevices = AFuture.make<UUID[]>();
        const localApi = new (class extends SmartHomeClientGuiApiLocal<any> {
            constructor() { super(null as any); }
            
            deviceStateUpdated(deviceUid: UUID, records: DeviceRecord[]): void {
                Log.info(`Received data from device ${deviceUid}`, { count: records.length });
            }
            
            deviceListUpdated(devices: UUID[]): void {
                Log.info(`Received device list`, { count: devices.length });
                receivedDevices.tryDone(devices);
            }
        })();

        // 5. Регистрируем API
        const context = node.toApi(SmartHomeClientGuiApi.META, localApi);
        const api = SmartHomeClientToServiceApi.META.makeRemote(context);

        // 6. Запрашиваем список устройств
        const devicesFuture = api.getDevices();
        await context.flush().toPromise(5000);
        
        const devices = await devicesFuture.toPromise(5000);
        Log.info(`Got devices: ${devices.length}`);
        
        expect(devices).toBeDefined();
        expect(Array.isArray(devices)).toBe(true);
        
        // Ждем возможного push-уведомления
        await Promise.race([
            receivedDevices.toPromise(2000),
            new Promise(resolve => setTimeout(resolve, 2000))
        ]);
    }, 30000);

    test('should request device history and subscribe to updates', async () => {
        // 1. Создаем клиента
        const parent = UUID.fromString("B1AC52C8-8D94-BD39-4C01-A631AC594166");
        const config = new ClientStateInMemory(parent, [REG_URI], undefined);
        client = new AetherCloudClient(config, "test-client2");

        // 2. Подключаемся к сети
        await client.connect().toPromise(30000);
        
        // 3. Подключаемся к сервису
        const serviceUuid = UUID.fromString(SERVICE_UUID);
        const node = client.getMessageNode(serviceUuid, MessageEventListenerDefault);

        // 4. Создаем локальную реализацию
        const receivedData = AFuture.make<DeviceRecord[]>();
        const localApi = new (class extends SmartHomeClientGuiApiLocal<any> {
            constructor() { super(null as any); }
            
            deviceStateUpdated(deviceUid: UUID, records: DeviceRecord[]): void {
                Log.info(`Received push data from ${deviceUid}`);
                receivedData.tryDone(records);
            }
            
            deviceListUpdated(devices: UUID[]): void {}
        })();

        const context = node.toApi(SmartHomeClientGuiApi.META, localApi);
        const api = SmartHomeClientToServiceApi.META.makeRemote(context);

        // 5. Запрашиваем историю
        const testDeviceUuid = UUID.fromString(TEST_DEVICE_UUID);
        const historyFuture = api.requestDeviceHistory(testDeviceUuid, 10);
        await context.flush().toPromise(5000);
        
        const history = await historyFuture.toPromise(5000);
        Log.info(`Got history: ${history.length} records`);
        
        expect(history).toBeDefined();
        expect(Array.isArray(history)).toBe(true);
        
        // 6. Подписываемся
        const subscribeFuture = api.subscribeToDevice(testDeviceUuid);
        await context.flush().toPromise(5000);
        
        const subscribed = await subscribeFuture.toPromise(5000);
        Log.info(`Subscribed: ${subscribed}`);
        expect(subscribed).toBe(true);
        
        // 7. Ждем возможного push-уведомления
        await Promise.race([
            receivedData.toPromise(3000),
            new Promise(resolve => setTimeout(resolve, 3000))
        ]);
        
        // 8. Отписываемся
        const unsubscribeFuture = api.unsubscribeFromDevice(testDeviceUuid);
        await context.flush().toPromise(5000);
        
        const unsubscribed = await unsubscribeFuture.toPromise(5000);
        Log.info(`Unsubscribed: ${unsubscribed}`);
        expect(unsubscribed).toBe(true);
    }, 45000);
});
