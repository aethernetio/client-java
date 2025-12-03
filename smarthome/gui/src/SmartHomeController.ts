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

import { Base64 } from 'js-base64';

// ANONYMOUS_UID is used as PARENT UID.
const ANONYMOUS_UID_STR = "237e2dc0-21a4-4e83-8184-c43052f93b79";

// Interface for storing active connections
interface CommutatorConnection {
    uuid: string;
    node: MessageNode;
    context: FastApiContext;
    api: SmartHomeCommutatorApiRemote;
}

/**
 * P2P Controller.
 * Manages MULTIPLE connections to different Commutators.
 */
export class SmartHomeController {

    public onConnectionStateChange = new EventConsumer<'connecting' | 'connected' | 'error'>();

    // Payload includes UUID to know source of devices
    public onDeviceListUpdate = new EventConsumer<{uuid: string, devices: HardwareDevice[]}>();

    // Payload includes UUID to know source of update
    public onDeviceStateChanged = new EventConsumer<{uuid: string, id: number, state: DeviceStateData}>();

    public client: AetherCloudClient | null = null;

    // CHANGED: Storage for multiple connections
    public connections = new Map<string, CommutatorConnection>();

    constructor() {
    }

    /**
     * [CORE MODE] Creates a NEW connection to Aether network.
     */
    async connectAetherCore(registrationUriStr: string): Promise<void> {
        Log.printConsolePlain(new LogFilter());
        await applySodium();
        this.onConnectionStateChange.fire('connecting');
        Log.info("Connecting to Aether network (Core Registration)...");

        try {
            const registrationUri: URI[] = [registrationUriStr as any];
            const parentUuid = UUID.fromString(ANONYMOUS_UID_STR);

            const clientConfig = new ClientStateInMemory(
                parentUuid,
                registrationUri,
                null,
                aetherApi.CryptoLib.SODIUM
            );

            this.client = new AetherCloudClient(clientConfig, "SmartHomeGUI");

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

    async connect(): Promise<void> {
        if (!this.client) throw new Error("Client not initialized. Call connectAetherCore or restoreSession first.");
        this.onConnectionStateChange.fire('connecting');
        try {
            await this.client.connect().toPromise(30000);
            this.onConnectionStateChange.fire('connected');
        } catch (e) {
            Log.error("Failed to reconnect", e);
            this.onConnectionStateChange.fire('error');
            throw e;
        }
    }

    public saveSession(): void {
        if (!this.client) return;
        try {
            const stateBytes = this.client.state.save();
            localStorage.setItem('aether_session_v1', Base64.fromUint8Array(stateBytes));
        } catch (e) {
            Log.error("Failed to save Aether session", e);
        }
    }

    public async restoreSession(): Promise<boolean> {
        const stored = localStorage.getItem('aether_session_v1');
        if (!stored) return false;

        await applySodium();
        Log.printConsolePlain(new LogFilter());

        try {
            const bytes = Base64.toUint8Array(stored);
            const state = new ClientStateInMemory(bytes);
            this.client = new AetherCloudClient(state, "SmartHomeGUI");
            return true;
        } catch (e) {
            Log.error("Session restore failed", e);
            localStorage.removeItem('aether_session_v1');
            return false;
        }
    }

    /**
     * [P2P MODE] Opens a connection to a specific Commutator.
     * Stores result in `this.connections`.
     */
    async connectCommutatorP2P(targetUuidStr: string): Promise<void> {
        if (!this.client) throw new Error("Core not connected");

        const myUuid = this.client.getUid()?.toString().toString();
        if (myUuid === targetUuidStr) throw new Error("Cannot connect to self");

        if (this.connections.has(targetUuidStr)) {
            // Already connected, just refresh structure
            await this.fetchStructure(targetUuidStr);
            return;
        }

        Log.info("Opening P2P channel to: " + targetUuidStr);

        try {
            const targetUuid = UUID.fromString(targetUuidStr);
            const node = this.client.getMessageNode(targetUuid, MessageEventListenerDefault);

            // Create local API implementation bound to this specific connection UUID
            const localApi = this.createLocalApi(targetUuidStr);

            const context = node.toApi(SmartHomeClientApi.META, localApi);
            const api = SmartHomeCommutatorApi.META.makeRemote(context);

            const conn: CommutatorConnection = { uuid: targetUuidStr, node, context, api };
            this.connections.set(targetUuidStr, conn);

            Log.info(`P2P to ${targetUuidStr} established.`);

            await this.fetchStructure(targetUuidStr);

        } catch (e) {
            Log.error(`Failed to connect to ${targetUuidStr}`, e);
            this.connections.delete(targetUuidStr);
            throw e;
        }
    }

    public async fetchStructure(uuid: string): Promise<void> {
        const conn = this.connections.get(uuid);
        if (!conn) return;

        try {
            const structureFuture = conn.api.getSystemStructure();
            conn.api.queryAllSensorStates();
            await conn.context.flush().toPromise(5000);

            const devices = await structureFuture.toPromise(5000);

            // Fire event with UUID so UI knows where these devices belong
            this.onDeviceListUpdate.fire({ uuid, devices });
        } catch(e) {
            Log.error("Error fetching structure", e);
            throw e;
        }
    }

    public executeCommand(commutatorUuid: string, localActorId: number, commandStr: string): Promise<void> {
        const conn = this.connections.get(commutatorUuid);
        if (!conn) return Promise.reject(new Error(`No connection to ${commutatorUuid}`));

        const cmd = new VariantString(commandStr);
        conn.api.executeActorCommand(localActorId, cmd);
        return conn.context.flush().toPromise(5000);
    }

    public queryState(commutatorUuid: string, localDeviceId: number): Promise<DeviceStateData> {
        const conn = this.connections.get(commutatorUuid);
        if (!conn) return Promise.reject(new Error(`No connection to ${commutatorUuid}`));

        const future = conn.api.queryState(localDeviceId);
        conn.context.flush();
        return future.toPromise(5000);
    }

    public queryAllSensorStates(commutatorUuid?: string): Promise<void> {
        const promises: Promise<void>[] = [];

        const targets = commutatorUuid ? [this.connections.get(commutatorUuid)] : Array.from(this.connections.values());

        for (const conn of targets) {
            if (conn) {
                conn.api.queryAllSensorStates();
                promises.push(conn.context.flush().toPromise(5000));
            }
        }

        return Promise.all(promises).then(() => {});
    }

    private createLocalApi(peerUuid: string): SmartHomeClientApi {
        const self = this;
        return new (class extends SmartHomeClientApiLocal<any> {
            constructor() { super(null as any); }

            deviceStateUpdated(localDeviceId: number, state: DeviceStateData): void {
                 Log.info(`PUSH from ${peerUuid}: ID=${localDeviceId} Val=${JSON.stringify(state)}`);
                 self.onDeviceStateChanged.fire({ uuid: peerUuid, id: localDeviceId, state: state });
            }
        })();
    }

    public async disconnectP2P(uuid: string): Promise<void> {
        this.connections.delete(uuid);
    }

    public async disconnect(): Promise<void> {
        if (this.client) await this.client.destroy(true).toPromise(5000);
        this.onConnectionStateChange.fire('connecting');
        this.client = null;
        this.connections.clear();
    }
}