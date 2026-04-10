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
            await this.client.connect().toPromise(15000);
            
            await this.connectToService(serviceUuid);
            
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

        // Единственный вызов toApiR – регистрируем локальную реализацию SmartHomeClientGuiApi
        const ctx = node.toApiR(SmartHomeClientGuiApi.META, (ctx: FastApiContextLocal<SmartHomeClientGuiApi>) => ({
            deviceStateUpdated: (deviceUid: UUID, records: SensorRecord[]) => {
                const deviceUidStr = deviceUid.toString();
                this.deviceDataCache.set(deviceUidStr, records);
                this.onDeviceDataUpdate.fire({
                    deviceUid: deviceUidStr,
                    records: records,
                    timestamp: Date.now()
                });
            },
            // deviceListUpdated removed because not in ADSL contract
            // this.devices = devices;
            // this.onDeviceListUpdate.fire(devices);
        })
    });

        // Из этого же контекста создаём удалённый прокси SmartHomeHubRegistryApi
        // const hub = SmartHomeHubRegistryApi.META.makeRemote(ctx);
        const hub = SmartHomeHubRegistryApi.META.makeRemote(ctx);


        this.serviceConnection = {
            uuid: serviceUuid.toString(),
            context: ctx,
            hub: hub
        };

        // Регистрируемся в хабе
        hub.gui(GuiStream.remoteApi(ctx, a => {}));
        ctx.flush(FlushReport.STUB);

        this.onConnectionStateChange.fire('connected');
        this.refreshDeviceList();
    }



    requestDeviceData(deviceUidStr: string, count: number = 10): ARFuture<SensorRecord[]> {
        if (!this.serviceConnection) throw new Error("Not connected to service");
        const deviceUid = UUID.fromString(deviceUidStr);
        const future = this.serviceConnection.hub.requestDeviceHistory(deviceUid, BigInt(count));
        this.serviceConnection.context.flush(FlushReport.STUB);
        return future.toConsumer((records: SensorRecord[]) => {
            this.deviceDataCache.set(deviceUidStr, records);
            return records;
        });
    }



    subscribeToDevice(deviceUidStr: string): ARFuture<boolean> {
        if (!this.serviceConnection) throw new Error("Not connected to service");
        const deviceUid = UUID.fromString(deviceUidStr);
        const future = this.serviceConnection.hub.subscribeToDevice(deviceUid);
        this.serviceConnection.context.flush(FlushReport.STUB);
        return future.toConsumer((result: boolean) => {
            return result;
        });
    }



    unsubscribeFromDevice(deviceUidStr: string): ARFuture<boolean> {
        if (!this.serviceConnection) throw new Error("Not connected to service");
        const deviceUid = UUID.fromString(deviceUidStr);
        const future = this.serviceConnection.hub.unsubscribeFromDevice(deviceUid);
        this.serviceConnection.context.flush(FlushReport.STUB);
        return future.toConsumer((result: boolean) => {
            return result;
        });
    }



    refreshDeviceList(): ARFuture<UUID[]> {
        if (!this.serviceConnection) throw new Error("Not connected to service");
        const future = this.serviceConnection.hub.getDevices();
        this.serviceConnection.context.flush(FlushReport.STUB);
        return future.toConsumer((devices: UUID[]) => {
            this.devices = devices;
            this.onDeviceListUpdate.fire(devices);
            return devices;
        });
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