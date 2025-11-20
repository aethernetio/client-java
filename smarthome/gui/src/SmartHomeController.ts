// FILE: SmartHomeController.ts
// =============================================================================================
import {
    AetherCloudClient,
    ClientStateInMemory,
    MessageEventListenerDefault,
    UUID,
    URI,
    AFuture,
    ARFuture,
    MessageNode,
    aetherApi,
    FastApiContext,
    Log,
    LogFilter,
    applySodium,
    EventConsumer,
} from 'aether-client/build/aether_client';

import {
    SmartHomeCommutatorApi,
    SmartHomeClientApi,
    DeviceStateData,
    HardwareDevice,
    SmartHomeCommutatorApiRemote,
    SmartHomeClientApiLocal,
    VariantData,
    VariantString,
    VariantBool
} from './aether_api';

// ANONYMOUS_UID используется как PARENT UID.
const ANONYMOUS_UID_STR = "237e2dc0-21a4-4e83-8184-c43052f93b79";

/**
 * P2P Controller.
 * Подключается напрямую к Коммутатору по UUID и URI.
 * UUID КЛИЕНТА назначается сервером Aether Core.
 */
export class SmartHomeController {

    public onConnectionStateChange = new EventConsumer<'connecting' | 'connected' | 'error'>();
    public onDeviceListUpdate = new EventConsumer<HardwareDevice[]>();
    public onDeviceStateChanged = new EventConsumer<{id: number, state: DeviceStateData}>();

    public client: AetherCloudClient | null = null;
    public commutatorNode: MessageNode | null = null;
    public apiContext: FastApiContext | null = null;
    public commutatorApi: SmartHomeCommutatorApiRemote | null = null;
    private localApi: SmartHomeClientApi;

    constructor() {
        this.localApi = this.createLocalApi();
    }

    /**
     * [РЕЖИМ CORE] Устанавливает соединение с сетью Aether (только регистрация).
     */
    async connectAetherCore(registrationUriStr: string): Promise<void> {
        Log.printConsolePlain(new LogFilter());
        await applySodium();
        this.onConnectionStateChange.fire('connecting');
        Log.info("Connecting to Aether network (Core Registration)...");

        try {
            const registrationUri: URI[] = [registrationUriStr as any];
            const parentUuid = UUID.fromString(ANONYMOUS_UID_STR);

            // Правильный конструктор (4 аргумента), как в вашем рабочем варианте
            const clientConfig = new ClientStateInMemory(
                parentUuid,
                registrationUri,
                null,
                aetherApi.CryptoLib.SODIUM
            );

            this.client = new AetherCloudClient(clientConfig, "SmartHomeGUI");

            // Подключаемся к Core
            await this.client.connect().toPromise(30000);

            const clientUid = this.client.getUid();
            Log.info("Aether Core connection successful.", { assignedUuid: clientUid ? clientUid.toString() : "null" });

            this.onConnectionStateChange.fire('connected');

        } catch (e) {
            Log.error("Failed to connect to Aether Core", e);
            this.onConnectionStateChange.fire('error');
            this.client = null;
            throw e;
        }
    }

    /**
     * [РЕЖИМ P2P] Устанавливает P2P-канал к конкретному Коммутатору.
     */
    async connectCommutatorP2P(targetUuidStr: string): Promise<void> {
        if (!this.client) {
            throw new Error("Aether Core is not connected. Call connectAetherCore first.");
        }

        Log.info("Attempting to open P2P channel to commutator: " + targetUuidStr);

        try {
            const targetUuid = UUID.fromString(targetUuidStr);

            this.commutatorNode = this.client.getMessageNode(targetUuid, MessageEventListenerDefault);
            this.apiContext = this.commutatorNode.toApi(SmartHomeClientApi.META, this.localApi);
            this.commutatorApi = SmartHomeCommutatorApi.META.makeRemote(this.apiContext);

            Log.info("P2P channel opened successfully.");

            await this.fetchStructure();

        } catch (e) {
            Log.error("Failed to open P2P channel or fetch structure", e);
            // Сбрасываем P2P состояние, но НЕ уничтожаем клиент
            await this.disconnectP2P();
            throw e;
        }
    }

    public async fetchStructure(): Promise<void> {
        if (!this.commutatorApi || !this.apiContext) return;

        try {
            const structureFuture = this.commutatorApi.getSystemStructure();
            this.commutatorApi.queryAllSensorStates();
            await this.apiContext.flush().toPromise(5000);

            const devices = await structureFuture.toPromise(5000);
            this.onDeviceListUpdate.fire(devices);
        } catch(e) {
            Log.error("Error fetching structure or subscribing to PUSH", e);
            throw e;
        }
    }

    public executeCommand(localActorId: number, commandStr: string): Promise<void> {
        if (!this.commutatorApi || !this.apiContext) return Promise.reject(new Error("P2P connection is not active."));

        const cmd = new VariantString(commandStr);
        this.commutatorApi.executeActorCommand(localActorId, cmd);
        return this.apiContext.flush().toPromise(5000);
    }

    public queryAllSensorStates(): Promise<void> {
        if (!this.commutatorApi || !this.apiContext) return Promise.reject(new Error("P2P connection is not active."));

        this.commutatorApi.queryAllSensorStates();
        return this.apiContext.flush().toPromise(5000);
    }

    private createLocalApi(): SmartHomeClientApi {
        const self = this;
        return new (class extends SmartHomeClientApiLocal<any> {
            constructor() { super(null as any); }

            deviceStateUpdated(localDeviceId: number, state: DeviceStateData): void {
                 Log.info(`PUSH received: ID=${localDeviceId} Val=${JSON.stringify(state)}`);
                 self.onDeviceStateChanged.fire({id: localDeviceId, state: state});
            }
        })();
    }

    /**
     * Сбрасывает только P2P-соединение (Remote API), оставляя подключение к облаку активным.
     */
    public async disconnectP2P(): Promise<void> {
        this.commutatorApi = null;
        this.apiContext = null;
        this.commutatorNode = null;
    }

    /**
     * Полностью закрывает соединение с Aether Core.
     */
    public async disconnect(): Promise<void> {
        if (this.client) await this.client.destroy(true).toPromise(5000);
        this.onConnectionStateChange.fire('connecting');
        this.client = null;
        this.commutatorApi = null;
        this.apiContext = null;
        this.commutatorNode = null;
    }
}