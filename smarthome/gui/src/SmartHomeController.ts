// --- 1. Импорты из библиотеки aether-client ---
import {
    AetherCloudClient,
    ClientStateInMemory,
    MessageEventListenerDefault,
    aCrypto,
    UUID,
    URI,
    AFuture,
    ARFuture,
    MessageNode,
    aetherApi,
    FastApiContext // <-- Все еще нужен для типа
} from 'aether-client/build/aether_client'; // Прямой импорт из сборки

// --- 2. Импорты сгенерированного API и DTOs ---
import {
    SmartHomeServiceApi,
    SmartHomeClientApi,
    Device,
    Actor,
    PendingPairing,
    ClientType,
    DeviceStateData,
    HardwareSensor,
    HardwareActor,
    SmartHomeClientApiRemote,
    SmartHomeServiceApiRemote
} from './aether_api'; //

// --- 3. Вспомогательный класс для событий ---
// (Без изменений)
type Listener<T> = (data: T) => void;
class EventEmitter<T> {
    private listeners: Listener<T>[] = [];
    add(listener: Listener<T>) { this.listeners.push(listener); }
    fire(data: T) { this.listeners.forEach(l => l(data)); }
}

/**
 * -----------------------------------------------------------------
 * ГЛАВНЫЙ КЛАСС КОНТРОЛЛЕРА GUI (V4 - Использует toApi)
 * -----------------------------------------------------------------
 */
export class SmartHomeController {
    // --- Публичные События ---
    // (Без изменений)
    public onConnectionStateChange = new EventEmitter<'connecting' | 'connected' | 'error'>();
    public onDeviceListUpdate = new EventEmitter<Device[]>();
    public onDeviceStateChanged = new EventEmitter<Device>();
    public onPairingListUpdate = new EventEmitter<PendingPairing[]>();
    public onPairingRequested = new EventEmitter<PendingPairing>();

    // --- Внутренние переменные Aether ---
    private client!: AetherCloudClient;
    private serviceNode!: MessageNode;
    private apiContext!: FastApiContext;     // Контекст, который создаст toApi
    private serviceApi!: SmartHomeServiceApiRemote; // Удаленный API Сервиса
    private localApi!: SmartHomeClientApi;   // Локальная реализация API Клиента

    private serviceUuid!: UUID;

    constructor() {
        this.localApi = this.createLocalApi();
    }

    /**
     * 1. Подключение к Aether и к Хабу "Умного Дома"
     */
    async connect(serviceUuidStr: string, registrationUriStr: string) {
        console.log("Connecting to Aether network...");
        this.onConnectionStateChange.fire('connecting');

        try {
            this.serviceUuid = UUID.fromString(serviceUuidStr);
            const registrationUri: URI[] = [registrationUriStr];

            const clientConfig = new ClientStateInMemory(this.serviceUuid, registrationUri, undefined, aetherApi.CryptoLib.SODIUM);
            this.client = new AetherCloudClient(clientConfig, "SmartHomeGUI");

            await this.client.connect().toPromise(30000);

            // 3. Получаем "трубу" (MessageNode) к нашему Сервису (Хабу)
            this.serviceNode = this.client.getMessageNode(this.serviceUuid, MessageEventListenerDefault);

            // 4. 🔥 НОВЫЙ ЧИСТЫЙ СПОСОБ: Используем toApi
            //
            // Эта строка делает ДВЕ вещи:
            // 1. Создает FastApiContext (this.apiContext), который знает, как
            //    отправлять (flush) данные обратно через этот serviceNode.
            // 2. "Привязывает" входящие PUSH-вызовы (bufferIn) к нашей
            //    локальной реализации (this.localApi).
            //
            this.apiContext = this.serviceNode.toApi(SmartHomeClientApi.META, this.localApi);

            // 5. 🔥 Cоздаем "заглушку" (stub) для вызова API Сервиса
            //    Мы используем apiContext, который был создан на шаге 4.
            this.serviceApi = SmartHomeServiceApi.META.makeRemote(this.apiContext);

            // 6. Регистрируемся на Сервисе
            this.serviceApi.register(ClientType.GUI_CLIENT, [], []);
            this.apiContext.flush(AFuture.make()); // Принудительно отправляем (register)

            console.log("Successfully connected and registered with SmartHomeService!");
            this.onConnectionStateChange.fire('connected');

        } catch (e) {
            console.error("Failed to connect", e);
            this.onConnectionStateChange.fire('error');
        }
    }

    // --- 2. Методы, которые будет вызывать UI ---
    // (Без изменений, кроме вызова flush)

    async fetchAllDevices() {
        if (!this.serviceApi) return;
        try {
            console.log("Fetching all devices...");
            const devices = await this.serviceApi.getAllDevices().toPromise(10000);
            console.log("Got devices:", devices);
            this.onDeviceListUpdate.fire(devices);
            // .flush() не нужен, т.к. .toPromise() неявно его вызывает
        } catch (e) {
            console.error("Failed to fetch devices", e);
        }
    }

    async executeCommand(commutatorId: UUID, localActorId: number, commandPkg: Uint8Array) {
        if (!this.serviceApi) return;
        try {
            this.serviceApi.executeActorCommand(commutatorId, localActorId, commandPkg);
            this.apiContext.flush(AFuture.make()); // Отправляем fire-and-forget
        } catch (e) {
            console.error("Failed to execute command", e);
        }
    }

    async fetchPendingPairings() {
        if (!this.serviceApi) return;
        try {
            const pairings = await this.serviceApi.getPendingPairings().toPromise(10000);
            this.onPairingListUpdate.fire(pairings);
        } catch (e) {
            console.error("Failed to fetch pending pairings", e);
        }
    }

    async approvePairing(commutatorUuid: UUID) {
        if (!this.serviceApi) return;
        try {
            this.serviceApi.approvePairing(commutatorUuid);
            this.apiContext.flush(AFuture.make()); // Отправляем fire-and-forget

            // (Логика обновления UI без изменений)
            this.fetchPendingPairings();
            this.fetchAllDevices();
        } catch (e) {
            console.error("Failed to approve pairing", e);
        }
    }

    async refreshAllSensors() {
        if (!this.serviceApi) return;
        try {
            console.log("Requesting sensor refresh...");
            this.serviceApi.refreshAllSensorStates();
            this.apiContext.flush(AFuture.make()); // Отправляем fire-and-forget
        } catch (e) {
            console.error("Failed to request refresh", e);
        }
    }

    /**
     * 3. Реализация PUSH API (SmartHomeClientApi)
     * (Без изменений)
     */
    private createLocalApi(): SmartHomeClientApi {
        const self = this;

        return new (class implements SmartHomeClientApi {
            getRemoteApi(): SmartHomeClientApiRemote {
                throw new Error('Method not implemented.');
            }

            deviceStateUpdated(device: Device): void {
                console.log("PUSH received: deviceStateUpdated", device.name);
                self.onDeviceStateChanged.fire(device);
            }

            pairingRequested(pairingInfo: PendingPairing): void {
                console.log("PUSH received: pairingRequested", pairingInfo.commutatorId.toString());
                self.onPairingRequested.fire(pairingInfo);
            }
        })();
    }
}