import {
    AetherCloudClient, aetherApi, applySodium, ARFuture, ClientStateInMemory,
    FlushReport, Log, LogFilter, MessageEventListenerDefault, URI, UUID
} from 'aether-client';
import {
    SmartHomeClientGuiApi, SmartHomeClientGuiApiLocal, SensorRecord, SmartHomeHubRegistryApi,
    GuiStream, SmartHomeHubRegistryApiRemote, SmartHomeGuiApiRemote
} from '../src/aether_api';

import { SmartHubController } from '../src/SmartHubController';


Log.printConsolePlain(new LogFilter());

describe('SmartHubService', () => {

    beforeAll(async () => {
        const { existsSync, mkdirSync, writeFileSync } = await import('fs');
        const { join } = await import('path');
        const tempDir = join(process.cwd(), 'temp_storage_');
        if (!existsSync(tempDir)) mkdirSync(tempDir, { recursive: true });
        const localStorageFile = join(tempDir, 'localstorage');
        if (!existsSync(localStorageFile)) writeFileSync(localStorageFile, '');
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
    /*
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
                    deviceStateUpdated(deviceUid: UUID, records: SensorRecord[]): void { }
                    // deviceListUpdated removed because not in ADSL contract
                    // receivedDevices.tryDone(devices);
                }
            };
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
*/

    test('SH_03: Should connect to SmartHubService and get device list (fixed)', async () => {
        const parent = UUID.fromString(SHARED_SERVICE_UID);
        const clientConfig = new ClientStateInMemory(parent, registrationUri, undefined, aetherApi.CryptoLib.SODIUM);
        client = new AetherCloudClient(clientConfig, "smarthub-test-client-fixed");

        await client.connect();

        const serviceUuid = UUID.fromString(SHARED_SERVICE_UID);
        const node = client.getMessageNode(serviceUuid, MessageEventListenerDefault);
        
        // const receivedDevices: ARFuture<UUID[]> = ARFuture.make();
        
        const contextOut = node.toApiR(SmartHomeClientGuiApi.META,
            ctx2 => {
                return new class extends SmartHomeClientGuiApiLocal<SmartHomeHubRegistryApiRemote> {
                    constructor() {
                        super(ctx2.makeRemote(SmartHomeHubRegistryApi.META));
                    }
                    deviceStateUpdated(deviceUid: UUID, records: SensorRecord[]): void {}
                };
            });
        
        const api = SmartHomeHubRegistryApi.META.makeRemote(contextOut);
        
        let devices: UUID[] = [];
        const devicesPromise = new Promise<UUID[]>((resolve) => {
            api.gui(GuiStream.remoteApi(contextOut, (guiApi: SmartHomeGuiApiRemote) => {
                guiApi.getDevices().toPromise().then(devs => {
                    devices = devs;
                    resolve(devs);
                }).catch(e => resolve([]));
            }));
            contextOut.flush(FlushReport.STUB);
        });
        
        const result = await devicesPromise;
        Log.info(`Got devices: ${result.length}`);
        expect(Array.isArray(result)).toBe(true);
        expect(result.length).toBeGreaterThan(0);
    }, 45000);




    // test('SH_05: Should receive device state updates from emulator', async () => {
    //     const parent = UUID.fromString(SHARED_SERVICE_UID);
    //     const clientConfig = new ClientStateInMemory(parent, registrationUri, undefined, aetherApi.CryptoLib.SODIUM);
    //     client = new AetherCloudClient(clientConfig, "smarthub-test-client-updates");
    //     await client.connect();
    //     const serviceUuid = UUID.fromString(SHARED_SERVICE_UID);
    //     const node = client.getMessageNode(serviceUuid, MessageEventListenerDefault);
    //     // Сначала получаем список устройств
    //     let devices: UUID[] = [];
    //     const contextOutList = node.toApiR(SmartHomeClientGuiApi.META,
    //         ctx2 => {
    //             return new class extends SmartHomeClientGuiApiLocal<SmartHomeHubRegistryApiRemote> {
    //                 constructor() {
    //                     super(ctx2.makeRemote(SmartHomeHubRegistryApi.META));
    //                 }
    //                 deviceStateUpdated(deviceUid: UUID, records: SensorRecord[]): void {}
    //             };
    //         });
    //     const apiList = SmartHomeHubRegistryApi.META.makeRemote(contextOutList);
    //     const devicesPromise = new Promise<UUID[]>((resolve) => {
    //         apiList.gui(GuiStream.remoteApi(contextOutList, (guiApi: SmartHomeGuiApiRemote) => {
    //             guiApi.getDevices().toPromise().then(devs => resolve(devs)).catch(e => resolve([]));
    //         }));
    //         contextOutList.flush(FlushReport.STUB);
    //     });
    //     devices = await devicesPromise;
    //     expect(devices.length).toBeGreaterThan(0);
    //     const targetDeviceUid = devices[0];
    //     Log.info(`Subscribing to device: ${targetDeviceUid}`);
    //     // Подписываемся на обновления первого устройства
    //     let receivedRecords: SensorRecord[] = [];
    //     const updatePromise = new Promise<SensorRecord[]>((resolve, reject) => {
    //         const timeout = setTimeout(() => reject(new Error("No device state update received within 15 seconds")), 15000);
    //         const contextOut = node.toApiR(SmartHomeClientGuiApi.META,
    //             ctx2 => {
    //                 return new class extends SmartHomeClientGuiApiLocal<SmartHomeHubRegistryApiRemote> {
    //                     constructor() {
    //                         super(ctx2.makeRemote(SmartHomeHubRegistryApi.META));
    //                     }
    //                     deviceStateUpdated(deviceUid: UUID, records: SensorRecord[]): void {
    //                         if (deviceUid.equals(targetDeviceUid)) {
    //                             receivedRecords = records;
    //                             clearTimeout(timeout);
    //                             resolve(records);
    //                         }
    //                     }
    //                 };
    //             });
    //         const api = SmartHomeHubRegistryApi.META.makeRemote(contextOut);
    //         api.gui(GuiStream.remoteApi(contextOut, (guiApi: SmartHomeGuiApiRemote) => {
    //             guiApi.subscribeToDevice(targetDeviceUid).toPromise().catch(e => reject(e));
    //         }));
    //         contextOut.flush(FlushReport.STUB);
    //     });
    //     const records = await updatePromise;
    //     Log.info(`Received device state update: ${records.length} records`);
    //     expect(records.length).toBeGreaterThan(0);
    //     expect(records[0].value).toBeGreaterThanOrEqual(0);
    //     expect(records[0].value).toBeLessThanOrEqual(127);
    //     expect(records[0].time).toBeDefined();
    // }, 45000);





/*
    //Следующий тест закоменнтирован. Его нужно переделать как предыдущий потому что сейчас используется неправильное устаревшее api.
        test('SH_02: Should request device history', async () => {
            const parent = UUID.fromString(SHARED_SERVICE_UID);
            const clientConfig = new ClientStateInMemory(parent, registrationUri, undefined, aetherApi.CryptoLib.SODIUM);
            client = new AetherCloudClient(clientConfig, "smarthub-test-client2");

            await new Promise((resolve, reject) => {
                const f = client.connect();
                const t = setTimeout(() => reject(new Error("Connect timeout")), 25000);
                f.addListener((future) => { if (future.isDone()) { clearTimeout(t); resolve(null); } });
            });

            const serviceUuid = UUID.fromString(SHARED_SERVICE_UID);
            const node = client.getMessageNode(serviceUuid, MessageEventListenerDefault);
            const receivedData = new ARFuture<SensorRecord[]>();

            const localApi = new (class extends SmartHomeClientGuiApiLocal<any> {
                constructor() { super(null as any); }
                deviceStateUpdated(deviceUid: UUID, records: SensorRecord[]): void {
                    receivedData.tryDone(records);
                }
                deviceListUpdated(devices: UUID[]): void {}
            })();

            const testDeviceUuid = UUID.fromString("00000000-0000-0000-0000-000000000002");
            // Используем один контекст, как и в SH_01
            const contextOut = node.toApiR(SmartHomeHubRegistryApi.META, () => localApi);
            const api = SmartHomeHubRegistryApi.META.makeRemote(contextOut);
            const historyPromise = api.requestDeviceHistory(testDeviceUuid, BigInt(10));
            contextOut.flush(FlushReport.STUB);
            const history = await historyPromise;

            Log.info(`Got history: ${history.length} records`);
            expect(Array.isArray(history)).toBe(true);

            api.subscribeToDevice(testDeviceUuid);
            contextOut.flush(FlushReport.STUB);

            api.unsubscribeFromDevice(testDeviceUuid);
            contextOut.flush(FlushReport.STUB);
        }, 45000);
        */

    // test('SH_04: Should get device list via SmartHubController', async () => {
    //     const controller = new SmartHubController();
    //     await controller.connect(SHARED_SERVICE_UID, "ws://localhost:9011");
    //     const devices = await controller.refreshDeviceList().toPromise();
    //     expect(Array.isArray(devices)).toBe(true);
    //     expect(devices.length).toBeGreaterThan(0);
    //     await controller.disconnect();
    // }, 45000);
});