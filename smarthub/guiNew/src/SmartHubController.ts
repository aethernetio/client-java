export interface DeviceUpdate {
    deviceUid: string;
    records: SensorRecord[];
    timestamp: number;
}




import { AetherCloudClient, ClientStateInLocalStorage, MessageEventListenerDefault, UUID, MetaContext, Log, LogFilter, applySodium, EventConsumer, aetherApi } from 'aether-client';


import {
    SmartHomeHubRegistryApi,
    SmartHomeHubRegistryApiRemote,
    SmartHomeGuiApi,
    SmartHomeGuiApiRemote,

    SmartHomeClientGuiApi,
    SensorRecord,

} from './aether_api';



interface ServiceConnection {
    uuid: string;
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

            const state = new ClientStateInLocalStorage(
                serviceUuid,
                [wsUri as any],
                undefined,
                aetherApi.CryptoLib.SODIUM,
                'smarthub_gui_state_' + serviceUuidStr
            );

            this.client = new AetherCloudClient(state, "SmartHubClient");
            this.client.onMessage.add((uid, data) => {
                console.log('[SmartHub] Raw message from', uid.toAString().toString(), 'length', data.length);
            });

            await this.client.connect().toPromise(30000);
            await this.connectToService(serviceUuid);
            this.onConnectionStateChange.fire('connected');
        } catch (e: any) {
            console.error('[SmartHub] Connection failed', e);
            const message = e instanceof Error ? e.message : String(e);
            this.onError.fire(`Failed to connect to Aether Core: ${message}`);
            this.onConnectionStateChange.fire('error');
            throw e;
        }
    }

    








    private async connectToService(serviceUuid: UUID): Promise<void> {
        if (!this.client) throw new Error("Client not initialized");

        const node = this.client.getMessageNode(serviceUuid, MessageEventListenerDefault);
        console.log('[SmartHub] MessageNode created for service', serviceUuid.toAString().toString());

        const localGuiApi: SmartHomeClientGuiApi = {
            deviceStateUpdated: (deviceUid: UUID, records: SensorRecord[]) => {
                console.log('[SmartHub] deviceStateUpdated', deviceUid.toAString().toString(), records);
                const deviceUidStr = deviceUid.toAString().toString();
                let history = this.deviceDataCache.get(deviceUidStr) || [];
                history = [...history, ...records].slice(-50);
                this.deviceDataCache.set(deviceUidStr, history);

                this.onDeviceDataUpdate.fire({
                    deviceUid: deviceUidStr,
                    records: history,
                    timestamp: Date.now()
                });
            },

            onGetDevicesResult: (devices: UUID[]) => {
                console.log(
                    '[SmartHub] onGetDevicesResult received:',
                    devices.map(d => d.toAString().toString())
                );
                this.devices = devices;
                this.onDeviceListUpdate.fire(devices);
            },

            onRequestHistoryResult: (deviceUid: UUID, records: SensorRecord[]) => {
                console.log(
                    '[SmartHub] onRequestHistoryResult',
                    deviceUid.toAString().toString(),
                    records
                );
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

        const rootCtx = node.toApiR(
            SmartHomeClientGuiApi.META,
            (_ctx: MetaContext) => localGuiApi
        );

        const hubRegistry = SmartHomeHubRegistryApi.META.makeRemote(rootCtx);

        const guiApi = hubRegistry.openGui(
            () => localGuiApi,
            data => data
        );

        this.serviceConnection = {
            uuid: serviceUuid.toAString().toString(),
            hub: guiApi
        };

        console.log('[SmartHub] Service connection established');
        this.onConnectionStateChange.fire('connected');
        this.refreshDeviceList();
    }













    requestDeviceData(deviceUidStr: string, count: number = 10): void {
        if (!this.serviceConnection) throw new Error("Not connected to service");
        const deviceUid = UUID.fromString(deviceUidStr);
        this.serviceConnection.hub.requestDeviceHistory(deviceUid, BigInt(count));
    }



    refreshDeviceList(): void {
        if (!this.serviceConnection) throw new Error("Not connected to service");
        console.log('[SmartHubController] Calling getDevices');
        this.serviceConnection.hub.getDevices();
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
        this.client.state.save();
    }

    restoreSession(serviceUuidStr: string): boolean {
        if (!serviceUuidStr) return false;
        return localStorage.getItem('smarthub_gui_state_' + serviceUuidStr) !== null;
    }
}