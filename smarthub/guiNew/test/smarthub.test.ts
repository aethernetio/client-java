import {
    AetherCloudClient, aetherApi, applySodium, ARFuture, ClientStateInMemory,
    FlushReport, Log, LogFilter, MessageEventListenerDefault, URI, UUID
} from 'aether-client';
import {
    SmartHomeClientGuiApi, SmartHomeClientGuiApiLocal, DeviceRecord, SmartHomeHubRegistryApi,
    GuiStream, SmartHomeHubRegistryApiRemote
} from '../src/aether_api';

Log.printConsolePlain(new LogFilter());

describe('SmartHubService', () => {
    beforeAll(async () => {
        await applySodium();
    });

    let client: AetherCloudClient;
    let serviceUuid: UUID;

    afterEach(async () => {
        if (client) {
            await client.destroy(true).toPromise();
            // @ts-ignore
            client = undefined;
        }
    });

    const registrationUri: URI[] = ["ws://localhost:9011"];
    const SHARED_SERVICE_UID = process.env.SERVICE_UID;

    if (!SHARED_SERVICE_UID) throw new Error('SHARED_SERVICE_UID is not specified')
    Log.info("Effective SHARED_SERVICE_UID:", { uid: SHARED_SERVICE_UID });
    test('SH_01: Should connect to SmartHubService and get device list', async () => {
        const parent = UUID.fromString(SHARED_SERVICE_UID);
        const clientConfig = new ClientStateInMemory(parent, registrationUri, undefined, aetherApi.CryptoLib.SODIUM);
        client = new AetherCloudClient(clientConfig, "smarthub-test-client");

        // Надежное подключение через addListener
        await new Promise((resolve, reject) => {
            const f = client.connect();
            const t = setTimeout(() => reject(new Error("Connect timeout")), 25000);
            f.addListener((future) => { if (future.isDone()) { clearTimeout(t); resolve(null); } });
        });

        const serviceUuid = UUID.fromString(SHARED_SERVICE_UID);
        const node = client.getMessageNode(serviceUuid, MessageEventListenerDefault);
        const receivedDevices = new ARFuture<UUID[]>();
        const contextOut = node.toApiR(SmartHomeClientGuiApi.META,
            ctx2 => {
                return new (class extends SmartHomeClientGuiApiLocal<SmartHomeHubRegistryApiRemote> {
                    constructor() {
                        super(ctx2.makeRemote(SmartHomeHubRegistryApi.META));
                    }
                    deviceStateUpdated(deviceUid: UUID, records: DeviceRecord[]): void { }
                    deviceListUpdated(devices: UUID[]): void {
                        receivedDevices.tryDone(devices);
                    }
                })();
            });
        let api = SmartHomeHubRegistryApi.META.makeRemote(contextOut);
        //Это правильный вариант вызова api второго уровня:
        //Используем тот же контекст для второго уровня. Для теста это приемлемо. В рабочем же продукте
        api.gui(GuiStream.remoteApi(contextOut, a => {
            a.getDevices().toConsumer(devices => {
                Log.info(`Got devices: ${devices.length}`);
                expect(Array.isArray(devices)).toBe(true);
            });
        }));
        contextOut.flush(FlushReport.STUB);

    }, 45000);


    //Следующий тест закоменнтирован. Его нужно переделать как предыдущий потому что сейчас используется неправильное устаревшее api.
    //     test('SH_02: Should request device history', async () => {
    //         const parent = UUID.fromString(SHARED_SERVICE_UID);
    //         const clientConfig = new ClientStateInMemory(parent, registrationUri, undefined, aetherApi.CryptoLib.SODIUM);
    //         client = new AetherCloudClient(clientConfig, "smarthub-test-client2");
    //
    //         await new Promise((resolve, reject) => {
    //             const f = client.connect();
    //             const t = setTimeout(() => reject(new Error("Connect timeout")), 25000);
    //             f.addListener((future) => { if (future.isDone()) { clearTimeout(t); resolve(null); } });
    //         });
    //
    //         const serviceUuid = UUID.fromString(SHARED_SERVICE_UID);
    //         const node = client.getMessageNode(serviceUuid, MessageEventListenerDefault);
    //         const receivedData = new ARFuture<DeviceRecord[]>();
    //
    //         const localApi = new (class extends SmartHomeClientGuiApiLocal<any> {
    //             constructor() { super(null as any); }
    //             deviceStateUpdated(deviceUid: UUID, records: DeviceRecord[]): void {
    //                 receivedData.tryDone(records);
    //             }
    //             deviceListUpdated(devices: UUID[]): void {}
    //         })();
    //
    //         const testDeviceUuid = UUID.fromString("00000000-0000-0000-0000-000000000002");
    //         // Используем один контекст, как и в SH_01
    //         const contextOut = node.toApiR(SmartHomeHubRegistryApi.META, () => localApi);
    //         const api = SmartHomeHubRegistryApi.META.makeRemote(contextOut);
    //         const historyPromise = api.requestDeviceHistory(testDeviceUuid, BigInt(10));
    //         contextOut.flush(FlushReport.STUB);
    //         const history = await historyPromise;
    //
    //         Log.info(`Got history: ${history.length} records`);
    //         expect(Array.isArray(history)).toBe(true);
    //
    //         api.subscribeToDevice(testDeviceUuid);
    //         contextOut.flush(FlushReport.STUB);
    //
    //         api.unsubscribeFromDevice(testDeviceUuid);
    //         contextOut.flush(FlushReport.STUB);
    //     }, 45000);
});