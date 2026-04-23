export interface DeviceUpdate {
    deviceUid: string;
    records: SensorRecord[];
    timestamp: number;
}




import { AFuture, ARFuture, AetherCloudClient, ClientStateInMemory, MessageEventListenerDefault, UUID, MessageNode, FastApiContext, FastApiContextLocal, Log, LogFilter, applySodium, EventConsumer, FlushReport, aetherApi } from 'aether-client';
import { Base64 } from 'js-base64';


import {
    SmartHomeHubRegistryApi,
    SmartHomeHubRegistryApiRemote,
    SmartHomeGuiApi,
    SmartHomeGuiApiRemote,
    GuiStream,
    DeviceStream,
    SmartHomeClientGuiApi,
    SensorRecord,
    SmartHomeClientGuiApiLocal,
} from './aether_api';


interface ServiceConnection {
    uuid: string;
    context: FastApiContext;
    hub: SmartHomeGuiApiRemote;
}

export class SmartHubController {
    
    public onConnectionStateChange = new EventConsumer<'disconnected' | 'connecting' | 'connected' | 'error'>();
    public onDeviceListUpdate = new EventConsumer<UUID[]>();
    public onDeviceDataUpdate = new EventConsumer<{deviceUid: string, records: SensorRecord[], timestamp: number}>();
    public onError = new EventConsumer<string>();
    
    public client: AetherCloudClient | null = null;
    private serviceConnection: ServiceConnection | null = null;
    private deviceDataCache = new Map<string, SensorRecord[]>();
    private devices: UUID[] = [];
    

    async connect(serviceUuidStr: string, wsUri: string = "wss://dbservice.aethernet.io:9013"): Promise<void> {
        this.onConnectionStateChange.fire('connecting');
        Log.printConsolePlain(new LogFilter());
        await applySodium();

        try {
            const serviceUuid = UUID.fromString(serviceUuidStr);
            const state = new ClientStateInMemory(
                serviceUuid,
                [wsUri as any],
                null,
                aetherApi.CryptoLib.SODIUM
            );


            this.client = new AetherCloudClient(state, "SmartHubClient");
            this.client.onMessage.add((uid, data) => {
                console.log('[SmartHub] Raw message from', uid.toAString().toString(), 'length', data.length);
            });


            await this.client.connect().toPromise(30000);

            
            await this.connectToService(serviceUuid);
            
            this.onConnectionStateChange.fire('connected');
        } catch (e: any) {
            console.error(e);
            this.onError.fire("Failed to connect to Aether Core");
            this.onConnectionStateChange.fire('error');
        }
    }

    








    private async connectToService(serviceUuid: UUID): Promise<void> {
        if (!this.client) throw new Error("Client not initialized");

        const node = this.client.getMessageNode(serviceUuid, MessageEventListenerDefault);
        console.log('[SmartHub] MessageNode created for service', serviceUuid.toAString().toString());

        // Локальная реализация для приёма результатов от сервера
        const rootCtx = node.toApiR(SmartHomeClientGuiApi.META, (ctx: FastApiContextLocal<SmartHomeClientGuiApi>) => {
            console.log('[SmartHub] rootCtx localApi factory called');
            return {
                deviceStateUpdated: (deviceUid: UUID, records: SensorRecord[]) => {
                    console.log('[SmartHub] deviceStateUpdated', deviceUid.toAString().toString(), records);
                    const deviceUidStr = deviceUid.toAString().toString();
                    let history = this.deviceDataCache.get(deviceUidStr) || [];
                    // Накапливаем данные и оставляем последние 50 для графика
                    history = [...history, ...records].slice(-50);
                    this.deviceDataCache.set(deviceUidStr, history);

                    this.onDeviceDataUpdate.fire({
                        deviceUid: deviceUidStr,
                        records: history, // Теперь передаем всю накопленную историю
                        timestamp: Date.now()
                    });
                },
                onGetDevicesResult: (devices: UUID[]) => {
                    console.log('[SmartHub] onGetDevicesResult received:', devices.map(d => d.toAString().toString()));
                    this.devices = devices;
                    this.onDeviceListUpdate.fire(devices);
                },
                onRequestHistoryResult: (deviceUid: UUID, records: SensorRecord[]) => {
                    console.log('[SmartHub] onRequestHistoryResult', deviceUid.toAString().toString(), records);
                    const deviceUidStr = deviceUid.toAString().toString();
                    const history = records.slice(-50);
                    this.deviceDataCache.set(deviceUidStr, history);
                    this.onDeviceDataUpdate.fire({
                        deviceUid: deviceUidStr,
                        records: history,
                        timestamp: Date.now()
                    });
                }
            };
        });

        const hubRegistry = SmartHomeHubRegistryApi.META.makeRemote(rootCtx);

        // В TS конструктор принимает только factory/impl, META не требуется
        const guiCtx = new FastApiContextLocal(() => ({} as any));


        guiCtx.flush = (report: FlushReport) => {
            try {
                // Извлекаем накопленные байты вызовов SmartHomeGuiApi
                const data = guiCtx.remoteDataToArrayAsArray();
                if (data && data.length > 0) {
                    console.log('[SmartHub] Sending GuiStream, length:', data.length);
                    // Упаковываем во вложенный стрим и отправляем через корневой реестр
                    hubRegistry.gui(new GuiStream(data));
                    // Реальная отправка пакета в сокет идет через flush основного контекста
                    hubRegistry.flush(report); 
                } else {
                    report.done();
                }
            } catch (e) {
                console.error("[SmartHub] guiCtx.flush error:", e);
                report.done();
            }
        };




        const guiApi = SmartHomeGuiApi.META.makeRemote(guiCtx);

        this.serviceConnection = {
            uuid: serviceUuid.toAString().toString(),
            context: guiCtx,
            hub: guiApi
        };

        console.log('[SmartHub] Service connection established, flushing rootCtx');
        rootCtx.flush(FlushReport.STUB);
        this.onConnectionStateChange.fire('connected');
        // Запрашиваем список устройств
        this.refreshDeviceList();
    }













    requestDeviceData(deviceUidStr: string, count: number = 10): void {
        if (!this.serviceConnection) throw new Error("Not connected to service");
        const deviceUid = UUID.fromString(deviceUidStr);
        this.serviceConnection.hub.requestDeviceHistory(deviceUid, BigInt(count));
        this.serviceConnection.context.flush(FlushReport.STUB);
    }



    refreshDeviceList(): void {
        if (!this.serviceConnection) throw new Error("Not connected to service");
        console.log('[SmartHubController] Calling getDevices');
        this.serviceConnection.hub.getDevices();
        this.serviceConnection.context.flush(FlushReport.STUB);
    }




    getCachedDeviceData(deviceUidStr: string): SensorRecord[] | null {
        return this.deviceDataCache.get(deviceUidStr) || null;
    }

    getCachedDevices(): UUID[] {
        return this.devices;
    }

    async disconnect(): Promise<void> {
        this.serviceConnection = null;
        this.deviceDataCache.clear();
        if (this.client) {
            await this.client.destroy(true).toPromise(5000);
            this.client = null;
        }
        this.onConnectionStateChange.fire('disconnected');
        Log.info("Disconnected from SmartHub");
    }

    saveSession(): void {
        if (!this.client) return;
        try {
            const stateBytes = this.client.state.save();
            localStorage.setItem('smarthub_session_v1', Base64.fromUint8Array(stateBytes));
        } catch (e: any) {
            Log.error("Failed to save session", e);
        }
    }

    async restoreSession(): Promise<boolean> {
        const stored = localStorage.getItem('smarthub_session_v1');
        if (!stored) return false;

        await applySodium();
        Log.printConsolePlain(new LogFilter());

        try {
            const bytes = Base64.toUint8Array(stored);
            const state = new ClientStateInMemory(bytes);
            this.client = new AetherCloudClient(state, "SmartHubGUI");
            return true;
        } catch (e: any) {
            Log.error("Session restore failed", e);
            localStorage.removeItem('smarthub_session_v1');
            return false;
        }
    }
}