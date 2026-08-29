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

        Log.printConsolePlain(
            new LogFilter()
                .traceOff()
                .debugOff()
                .infoOff()
        );

        await applySodium();

        let phase = 'parse service UUID';

        try {
            if (this.client) {
                phase = 'close previous Aether client';

                await this.client
                    .destroy(true)
                    .toPromise(5000);

                this.client = null;

                this.serviceConnection = null;
                this.deviceDataCache.clear();
                this.devices = [];

            }

            const serviceUuid = UUID.fromString(serviceUuidStr);

            phase = 'load/create persistent client state';

            const state = new ClientStateInLocalStorage(
                serviceUuid,
                [wsUri as any],
                undefined,
                aetherApi.CryptoLib.SODIUM,
                'smarthub_gui_state_' + serviceUuidStr
            );

            phase = 'create AetherCloudClient';

            this.client =
                new AetherCloudClient(
                    state,
                    "SmartHubClient"
                );

            this.client.tryAcquireWebRtcMessageNode =
                () => false;



            phase = 'connect to Aether network';

            await this.client
                .connect()
                .toPromise(30000);

            phase = 'open SmartHub API stream';

            await this.connectToService(
                serviceUuid
            );



            phase = 'connected';

            this.onConnectionStateChange.fire(
                'connected'
            );
        } catch (e: any) {
            if (this.client) {
                try {
                    await this.client
                        .destroy(true)
                        .toPromise(5000);
                } catch {
                }

                this.client = null;
            }

            this.serviceConnection = null;

            const message =
                e instanceof Error
                    ? e.message
                    : String(e);

            console.error(
                `[SmartHub] Connection failed during ${phase}`,
                e
            );

            this.onError.fire(
                `Connection failed during ${phase}: ${message}`
            );

            this.onConnectionStateChange.fire(
                'error'
            );

            throw e;
        }
    }

    








    private async connectToService(serviceUuid: UUID): Promise<void> {
        if (!this.client) {
            throw new Error("Client not initialized");
        }

        const node =
            this.client.getMessageNode(
                serviceUuid,
                MessageEventListenerDefault
            );

        console.log(
            '[SmartHub] MessageNode created for service',
            serviceUuid.toAString().toString()
        );

        const localGuiApi: SmartHomeClientGuiApi = {
            deviceStateUpdated: (
                deviceUid: UUID,
                records: SensorRecord[]
            ) => {
                const deviceUidStr =
                    deviceUid.toAString().toString();

                console.log(
                    '[SmartHub] deviceStateUpdated',
                    deviceUidStr,
                    'records=' + records.length
                );

                let history =
                    this.deviceDataCache.get(deviceUidStr)
                    || [];

                history =
                    [...history, ...records]
                        .slice(-50);

                this.deviceDataCache.set(
                    deviceUidStr,
                    history
                );


                this.onDeviceDataUpdate.fire({
                    deviceUid: deviceUidStr,
                    records: history,
                    timestamp: Date.now()
                });


            },

            onGetDevicesResult: (
                devices: UUID[]
            ) => {
                console.log(
                    '[SmartHub] onGetDevicesResult received',
                    'devices=' + devices.length
                );

                this.devices = devices;

                this.onDeviceListUpdate.fire(
                    devices
                );
            },

            onRequestHistoryResult: (
                deviceUid: UUID,
                records: SensorRecord[]
            ) => {
                const deviceUidStr =
                    deviceUid.toAString().toString();

                console.log(
                    '[SmartHub] onRequestHistoryResult',
                    deviceUidStr,
                    'records=' + records.length
                );


                const history =
                    records.slice().reverse().slice(-50);


                this.deviceDataCache.set(
                    deviceUidStr,
                    history
                );

                this.onDeviceDataUpdate.fire({
                    deviceUid: deviceUidStr,
                    records: history,
                    timestamp: Date.now()
                });
            }
        };

        const rootCtx =
            node.toApiR(
                SmartHomeClientGuiApi.META,
                (_ctx: MetaContext) => localGuiApi
            );

        const hubRegistry =
            SmartHomeHubRegistryApi.META
                .makeRemote(rootCtx);

        const guiApi =
            hubRegistry.openGui(
                () => localGuiApi,
                data => data
            );

        this.serviceConnection = {
            uuid: serviceUuid
                .toAString()
                .toString(),
            hub: guiApi
        };

        console.log(
            '[SmartHub] Service connection established'
        );

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