import {
    AetherCloudClient,
    ClientStateInMemory,
    MessageEventListenerDefault,
    UUID,
    MessageNode,
    aetherApi,
    FastApiContext,
    Log,
    LogFilter,
    applySodium,
    EventConsumer
} from 'aether-client';

// ВАЖНО: Импортируем Record из сгенерированного API
import {
    SimpleDeviceApiRemote,
    SimpleClientApi,
    SimpleClientApiLocal,
    SimpleDeviceApi,
    Record
} from './aether_api';

const ANONYMOUS_UID_STR = "237e2dc0-21a4-4e83-8184-c43052f93b79";

interface DeviceSession {
    uuid: string;
    context: FastApiContext;
    api: SimpleDeviceApiRemote;
    pendingResolver: ((data: Record[]) => void) | null; // <-- Изменился тип
}

export class SmartHomeController {
    public onConnectionState = new EventConsumer<'disconnected' | 'connecting' | 'core_connected'>();
    public onError = new EventConsumer<string>();
    public onDeviceConnected = new EventConsumer<string>();
    public onDeviceDisconnected = new EventConsumer<string>();

    public client: AetherCloudClient | null = null;
    private sessions = new Map<string, DeviceSession>();

    async connectCore(wsUri: string): Promise<void> {
        this.onConnectionState.fire('connecting');
        Log.printConsolePlain(new LogFilter());
        await applySodium();

        try {
            const state = new ClientStateInMemory(
                UUID.fromString(ANONYMOUS_UID_STR),
                [wsUri as any],
                null,
                aetherApi.CryptoLib.SODIUM
            );

            this.client = new AetherCloudClient(state, "SimpleClient");
            await this.client.connect().toPromise(15000);
            this.onConnectionState.fire('core_connected');
        } catch (e) {
            console.error(e);
            this.onError.fire("Failed to connect to Aether Core");
            this.onConnectionState.fire('disconnected');
        }
    }

    async connectDevice(targetUuidStr: string): Promise<void> {
        if (!this.client) {
            this.onError.fire("Core not connected");
            return;
        }
        if (this.sessions.has(targetUuidStr)) return;

        try {
            const targetUuid = UUID.fromString(targetUuidStr);
            const node = this.client.getMessageNode(targetUuid, MessageEventListenerDefault);

            const session: DeviceSession = {
                uuid: targetUuidStr,
                context: null as any,
                api: null as any,
                pendingResolver: null
            };

            const localApiImpl = new (class extends SimpleClientApiLocal<any> {
                constructor() { super(null as any); }

                // Метод ID 3: receiveStatus(value: Record[])
                receiveStatus(value: Record[]): void {
                    if (session.pendingResolver) {
                        const resolve = session.pendingResolver;
                        session.pendingResolver = null;
                        resolve(value);
                    }
                }
            })();

            const context = node.toApi(SimpleClientApi.META, localApiImpl);
            const api = SimpleDeviceApi.META.makeRemote(context);

            session.context = context;
            session.api = api;

            this.sessions.set(targetUuidStr, session);
            this.onDeviceConnected.fire(targetUuidStr);

        } catch (e) {
            console.error(e);
            this.onError.fire(`Failed to connect to ${targetUuidStr}`);
        }
    }

    async disconnectDevice(uuid: string) {
        if (this.sessions.has(uuid)) {
            this.sessions.delete(uuid);
            this.onDeviceDisconnected.fire(uuid);
        }
    }

    // Возвращаем Promise<Record[]>
    requestRecords(uuid: string, count: number): Promise<Record[]> {
        const session = this.sessions.get(uuid);
        if (!session) return Promise.reject(new Error("Device disconnected"));

        return new Promise((resolve, reject) => {
            session.pendingResolver = resolve;
            try {
                // count типа short
                const safeCount = Math.max(0, Math.min(32000, Math.floor(count)));
                session.api.requestRecords(safeCount);
                session.context.flush();
            } catch (e) {
                session.pendingResolver = null;
                reject(e);
            }
        });
    }
}