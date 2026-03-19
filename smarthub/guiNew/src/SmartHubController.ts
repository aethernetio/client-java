export interface DeviceUpdate {
    deviceUid: string;
    records: DeviceRecord[];
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
    DeviceRecord
} from './aether_api';

const ANONYMOUS_UID_STR = "237e2dc0-21a4-4e83-8184-c43052f93b79";

interface ServiceConnection {
    uuid: string;
    context: FastApiContext;
    hub: SmartHomeGuiApiRemote;
}

export class SmartHubController {
    
    public onConnectionStateChange = new EventConsumer<'disconnected' | 'connecting' | 'connected' | 'error'>();
    public onDeviceListUpdate = new EventConsumer<UUID[]>();
    public onDeviceDataUpdate = new EventConsumer<{deviceUid: string, records: DeviceRecord[], timestamp: number}>();
    public onError = new EventConsumer<string>();
    
    public client: AetherCloudClient | null = null;
    private serviceConnection: ServiceConnection | null = null;
    private deviceDataCache = new Map<string, DeviceRecord[]>();
    private devices: UUID[] = [];
    
    async connect(serviceUuidStr: string, wsUri: string = "wss://dbservice.aethernet.io:9013"): Promise<void> {
        this.onConnectionStateChange.fire('connecting');
        Log.printConsolePlain(new LogFilter());
        await applySodium();

        try {
            const state = new ClientStateInMemory(
                UUID.fromString("237e2dc0-21a4-4e83-8184-c43052f93b79"),
                [wsUri as any],
                null,
                aetherApi.CryptoLib.SODIUM
            );

            this.client = new AetherCloudClient(state, "SmartHubClient");
            await this.client.connect().toPromise(15000);
            
            await this.connectToService(UUID.fromString(serviceUuidStr));
            
            this.onConnectionStateChange.fire('connected');
        } catch (e) {
            console.error(e);
            this.onError.fire("Failed to connect to Aether Core");
            this.onConnectionStateChange.fire('error');
        }
    }
    
    private async connectToService(serviceUuid: UUID): Promise<void> {
        if (!this.client) throw new Error("Client not initialized");

        const node = this.client.getMessageNode(serviceUuid, MessageEventListenerDefault);

        // 1. Создаем контекст регистратора (Registry API) через фабрику
        const regCtx = node.toApiR(SmartHomeHubRegistryApi.META, (ctx: FastApiContextLocal<SmartHomeHubRegistryApi>) => ({
            device: (s: DeviceStream) => { },
            gui: (s: GuiStream) => { }
        }));

        // 2. Инициализируем GuiStream (ID - 16 байт)
        const guiStream = new GuiStream(new Uint8Array(16));

        // 3. Настраиваем Callback API для Push-уведомлений (Events)
        node.toApiR(SmartHomeClientGuiApi.META, (ctx: FastApiContextLocal<SmartHomeClientGuiApi>) => ({
            deviceStateUpdated: (deviceUid: UUID, records: DeviceRecord[]) => {
                const deviceUidStr = deviceUid.toString();
                this.deviceDataCache.set(deviceUidStr, records);
                this.onDeviceDataUpdate.fire({
                    deviceUid: deviceUidStr,
                    records: records,
                    timestamp: Date.now()
                });
            },
            deviceListUpdated: (devices: UUID[]) => {
                this.devices = devices;
                this.onDeviceListUpdate.fire(devices);
            }
        }));

        // 4. Инициализируем соединение. Используем toString() для получения примитива string.
        this.serviceConnection = {
            uuid: serviceUuid.toString(),
            context: regCtx,
            hub: SmartHomeGuiApi.META.makeRemote(regCtx)
        };

        // 5. Регистрируемся в хабе и отправляем данные
        SmartHomeHubRegistryApi.META.makeRemote(regCtx).gui(guiStream);
        regCtx.flush(FlushReport.STUB);

        this.onConnectionStateChange.fire('connected');
        this.refreshDeviceList();
    }

    async requestDeviceData(deviceUidStr: string, count: number = 10): Promise<DeviceRecord[]> {
        if (!this.serviceConnection) throw new Error("Not connected to service");
        const deviceUid = UUID.fromString(deviceUidStr);
        this.serviceConnection.hub.requestDeviceHistory(deviceUid, BigInt(count));
        this.serviceConnection.context.flush(FlushReport.STUB);
        return this.deviceDataCache.get(deviceUidStr) || [];
    }

    async subscribeToDevice(deviceUidStr: string): Promise<boolean> {
        if (!this.serviceConnection) throw new Error("Not connected to service");
        const deviceUid = UUID.fromString(deviceUidStr);
        this.serviceConnection.hub.subscribeToDevice(deviceUid);
        this.serviceConnection.context.flush(FlushReport.STUB);
        return true;
    }

    async unsubscribeFromDevice(deviceUidStr: string): Promise<boolean> {
        if (!this.serviceConnection) throw new Error("Not connected to service");
        const deviceUid = UUID.fromString(deviceUidStr);
        this.serviceConnection.hub.unsubscribeFromDevice(deviceUid);
        this.serviceConnection.context.flush(FlushReport.STUB);
        return true;
    }

    async refreshDeviceList(): Promise<UUID[]> {
        if (!this.serviceConnection) throw new Error("Not connected to service");
        this.serviceConnection.hub.getDevices();
        this.serviceConnection.context.flush(FlushReport.STUB);
        return this.devices;
    }

    getCachedDeviceData(deviceUidStr: string): DeviceRecord[] | null {
        return this.deviceDataCache.get(deviceUidStr) || null;
    }

    getCachedDevices(): UUID[] {
        return this.devices;
    }

    async disconnect(): Promise<void> {
        this.serviceConnection = null;
        this.deviceDataCache.clear();
        this.devices = [];
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
        } catch (e) {
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
        } catch (e) {
            Log.error("Session restore failed", e);
            localStorage.removeItem('smarthub_session_v1');
            return false;
        }
    }
}