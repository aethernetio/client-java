import  {
    AFuture, ARFuture, DataIn, DataOut, DataInOut, DataInOutStatic, FastMetaType, FastFutureContext, RemoteApi, FastMeta, SerializerPackNumber, DeserializerPackNumber, FastApiContextLocal, FastMetaApi, BytesConverter, RemoteApiFuture, UUID, URI, AConsumer, ToString, AString
}
from 'aether-client';
import * as Impl from './aether_api_impl';
// This is always relative
/**
 * The type of client connecting to the service.
 */
export enum ClientType  {
    GUI_CLIENT = 'GUI_CLIENT', COMMUTATOR = 'COMMUTATOR' 
}
export namespace ClientType  {
    export const META: FastMetaType<ClientType> = new Impl.ClientTypeMetaImpl();
    
}
/**
 * Base type for a "Logical" device known to the Service. This is the main DTO for the GUI.
 */
export abstract class Device implements ToString  {
    public abstract getDeviceType(): string;
    public readonly id: number;
    public readonly name: string;
    public readonly commutatorId: UUID;
    public readonly localDeviceId: number;
    public readonly lastState: string | null;
    public readonly lastUpdated: Date | null;
    public abstract getAetherTypeId(): number;
    public static readonly META: FastMetaType<Device> = new Impl.DeviceMetaImpl();
    /**
     * Creates an instance of Device.
     * @param id - number
     * @param name - string
     * @param commutatorId - UUID
     * @param localDeviceId - number
     * @param lastState - string
     * @param lastUpdated - Date
     */
    constructor(id: number, name: string, commutatorId: UUID, localDeviceId: number, lastState: string, lastUpdated: Date)  {
        this.id = id;
        this.name = name;
        this.commutatorId = commutatorId;
        this.localDeviceId = localDeviceId;
        this.lastState = lastState;
        this.lastUpdated = lastUpdated;
        
    }
    public getId(): number  {
        return this.id;
        
    }
    public getName(): string  {
        return this.name;
        
    }
    public getCommutatorId(): UUID  {
        return this.commutatorId;
        
    }
    public getLocalDeviceId(): number  {
        return this.localDeviceId;
        
    }
    public getLastState(): string | null  {
        return this.lastState;
        
    }
    public getLastUpdated(): Date | null  {
        return this.lastUpdated;
        
    }
    /**
     * Calculates a hash code for a static instance of Device.
     * @param {Device | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: Device | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        return (obj.constructor as any).META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of Device with another object.
     * @param {Device | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: Device | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        return (v1.constructor as any).META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public abstract hashCode(): number;
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public abstract equals(other: any): boolean;
    public abstract toString(result: AString): AString;
    
}
/**
 * Base type for a physical device attached to a Commutator.
 */
export abstract class HardwareDevice implements ToString  {
    public abstract getHardwareType(): string;
    public readonly localId: number;
    public readonly descriptor: string;
    public abstract getAetherTypeId(): number;
    public static readonly META: FastMetaType<HardwareDevice> = new Impl.HardwareDeviceMetaImpl();
    /**
     * Creates an instance of HardwareDevice.
     * @param localId - number
     * @param descriptor - string
     */
    constructor(localId: number, descriptor: string)  {
        this.localId = localId;
        this.descriptor = descriptor;
        
    }
    public getLocalId(): number  {
        return this.localId;
        
    }
    public getDescriptor(): string  {
        return this.descriptor;
        
    }
    /**
     * Calculates a hash code for a static instance of HardwareDevice.
     * @param {HardwareDevice | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: HardwareDevice | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        return (obj.constructor as any).META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of HardwareDevice with another object.
     * @param {HardwareDevice | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: HardwareDevice | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        return (v1.constructor as any).META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public abstract hashCode(): number;
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public abstract equals(other: any): boolean;
    public abstract toString(result: AString): AString;
    
}
/**
 * A Logical Device that represents an Actor.
 *
 * @aetherTypeId 2
 */
export class Actor extends Device implements ToString  {
    public override getDeviceType(): string  {
        return "ACTOR";
        
    }
    public override getAetherTypeId(): number  {
        return 2;
        
    }
    public static readonly META_BODY: FastMetaType<Actor> = new Impl.ActorMetaBodyImpl();
    public static readonly META: FastMetaType<Actor> = new Impl.ActorMetaImpl();
    /**
     * Creates an instance of Actor.
     * @param id - number
     * @param name - string
     * @param commutatorId - UUID
     * @param localDeviceId - number
     * @param lastState - string
     * @param lastUpdated - Date
     */
    constructor(id: number, name: string, commutatorId: UUID, localDeviceId: number, lastState: string, lastUpdated: Date)  {
        super(id, name, commutatorId, localDeviceId, lastState, lastUpdated);
        
    }
    /**
     * Calculates a hash code for a static instance of Actor.
     * @param {Actor | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: Actor | null | undefined): number  {
        return Actor.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of Actor with another object.
     * @param {Actor | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: Actor | null | undefined, v2: any | null | undefined): boolean  {
        return Actor.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return Actor.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return Actor.staticEquals(this, other);
        
    }
    public toString(result: AString): AString  {
        Actor.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * A generic structure for reporting a device's state at a specific time.
 */
export class DeviceStateData implements ToString  {
    public readonly value: string;
    public readonly timestamp: Date;
    public static readonly META_BODY: FastMetaType<DeviceStateData> = new Impl.DeviceStateDataMetaBodyImpl();
    public static readonly META: FastMetaType<DeviceStateData> = DeviceStateData.META_BODY;
    /**
     * Creates an instance of DeviceStateData.
     * @param value - string
     * @param timestamp - Date
     */
    constructor(value: string, timestamp: Date)  {
        this.value = value;
        this.timestamp = timestamp;
        
    }
    public getValue(): string  {
        return this.value;
        
    }
    public getTimestamp(): Date  {
        return this.timestamp;
        
    }
    /**
     * Calculates a hash code for a static instance of DeviceStateData.
     * @param {DeviceStateData | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: DeviceStateData | null | undefined): number  {
        return DeviceStateData.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of DeviceStateData with another object.
     * @param {DeviceStateData | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: DeviceStateData | null | undefined, v2: any | null | undefined): boolean  {
        return DeviceStateData.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return DeviceStateData.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return DeviceStateData.staticEquals(this, other);
        
    }
    public toString(result: AString): AString  {
        DeviceStateData.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * A physical device that performs actions.
 *
 * @aetherTypeId 2
 */
export class HardwareActor extends HardwareDevice implements ToString  {
    public override getHardwareType(): string  {
        return "ACTOR";
        
    }
    public override getAetherTypeId(): number  {
        return 2;
        
    }
    public static readonly META_BODY: FastMetaType<HardwareActor> = new Impl.HardwareActorMetaBodyImpl();
    public static readonly META: FastMetaType<HardwareActor> = new Impl.HardwareActorMetaImpl();
    /**
     * Creates an instance of HardwareActor.
     * @param localId - number
     * @param descriptor - string
     */
    constructor(localId: number, descriptor: string)  {
        super(localId, descriptor);
        
    }
    /**
     * Calculates a hash code for a static instance of HardwareActor.
     * @param {HardwareActor | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: HardwareActor | null | undefined): number  {
        return HardwareActor.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of HardwareActor with another object.
     * @param {HardwareActor | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: HardwareActor | null | undefined, v2: any | null | undefined): boolean  {
        return HardwareActor.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return HardwareActor.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return HardwareActor.staticEquals(this, other);
        
    }
    public toString(result: AString): AString  {
        HardwareActor.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * A physical device that reports data.
 *
 * @aetherTypeId 1
 */
export class HardwareSensor extends HardwareDevice implements ToString  {
    public readonly unit: string | null;
    public override getHardwareType(): string  {
        return "SENSOR";
        
    }
    public override getAetherTypeId(): number  {
        return 1;
        
    }
    public static readonly META_BODY: FastMetaType<HardwareSensor> = new Impl.HardwareSensorMetaBodyImpl();
    public static readonly META: FastMetaType<HardwareSensor> = new Impl.HardwareSensorMetaImpl();
    /**
     * Creates an instance of HardwareSensor.
     * @param localId - number
     * @param descriptor - string
     * @param unit - string
     */
    constructor(localId: number, descriptor: string, unit: string)  {
        super(localId, descriptor);
        this.unit = unit;
        
    }
    public getUnit(): string | null  {
        return this.unit;
        
    }
    /**
     * Calculates a hash code for a static instance of HardwareSensor.
     * @param {HardwareSensor | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: HardwareSensor | null | undefined): number  {
        return HardwareSensor.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of HardwareSensor with another object.
     * @param {HardwareSensor | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: HardwareSensor | null | undefined, v2: any | null | undefined): boolean  {
        return HardwareSensor.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return HardwareSensor.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return HardwareSensor.staticEquals(this, other);
        
    }
    public toString(result: AString): AString  {
        HardwareSensor.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * A DTO for summarizing a Commutator that is awaiting pairing approval.
 */
export class PendingPairing implements ToString  {
    public readonly commutatorId: UUID;
    public readonly devices: HardwareDevice[];
    public static readonly META_BODY: FastMetaType<PendingPairing> = new Impl.PendingPairingMetaBodyImpl();
    public static readonly META: FastMetaType<PendingPairing> = PendingPairing.META_BODY;
    /**
     * Creates an instance of PendingPairing.
     * @param commutatorId - UUID
     * @param devices - HardwareDevice[]
     */
    constructor(commutatorId: UUID, devices: HardwareDevice[])  {
        this.commutatorId = commutatorId;
        this.devices = devices;
        if (devices === null || devices === undefined) throw new Error(`Field 'devices' cannot be null for type PendingPairing.`);
        
    }
    public getCommutatorId(): UUID  {
        return this.commutatorId;
        
    }
    public getDevices(): HardwareDevice[]  {
        return this.devices;
        
    }
    public devicesContains(el: HardwareDevice): boolean  {
        return (this.devices as HardwareDevice[]).includes(el as any);
        
    }
    /**
     * Calculates a hash code for a static instance of PendingPairing.
     * @param {PendingPairing | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: PendingPairing | null | undefined): number  {
        return PendingPairing.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of PendingPairing with another object.
     * @param {PendingPairing | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: PendingPairing | null | undefined, v2: any | null | undefined): boolean  {
        return PendingPairing.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return PendingPairing.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return PendingPairing.staticEquals(this, other);
        
    }
    public toString(result: AString): AString  {
        PendingPairing.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * A Logical Device that represents a Sensor.
 *
 * @aetherTypeId 1
 */
export class Sensor extends Device implements ToString  {
    public readonly unit: string | null;
    public override getDeviceType(): string  {
        return "SENSOR";
        
    }
    public override getAetherTypeId(): number  {
        return 1;
        
    }
    public static readonly META_BODY: FastMetaType<Sensor> = new Impl.SensorMetaBodyImpl();
    public static readonly META: FastMetaType<Sensor> = new Impl.SensorMetaImpl();
    /**
     * Creates an instance of Sensor.
     * @param id - number
     * @param name - string
     * @param commutatorId - UUID
     * @param localDeviceId - number
     * @param lastState - string
     * @param lastUpdated - Date
     * @param unit - string
     */
    constructor(id: number, name: string, commutatorId: UUID, localDeviceId: number, lastState: string, lastUpdated: Date, unit: string)  {
        super(id, name, commutatorId, localDeviceId, lastState, lastUpdated);
        this.unit = unit;
        
    }
    public getUnit(): string | null  {
        return this.unit;
        
    }
    /**
     * Calculates a hash code for a static instance of Sensor.
     * @param {Sensor | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: Sensor | null | undefined): number  {
        return Sensor.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of Sensor with another object.
     * @param {Sensor | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: Sensor | null | undefined, v2: any | null | undefined): boolean  {
        return Sensor.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return Sensor.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return Sensor.staticEquals(this, other);
        
    }
    public toString(result: AString): AString  {
        Sensor.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * A stream of commands destined FOR the Client (GUI).
 */
export class SmartHomeClientStream implements ToString  {
    public readonly data: Uint8Array;
    /**
     * Creates an instance of SmartHomeClientStream.
     * @param data - The raw byte data for this stream.
     */
    constructor(data: Uint8Array)  {
        this.data = data;
        
    }
    public static readonly META: FastMetaType<SmartHomeClientStream> = new Impl.SmartHomeClientStreamMetaImpl();
    public toString(result: AString): AString  {
        SmartHomeClientStream.META.metaToString(this, result);
        return result;
        
    }
    public accept(context: FastFutureContext, provider: BytesConverter, localApi: SmartHomeClientApi): void  {
        const decryptedData = provider(this.data);
        const dataInStatic = new DataInOutStatic(decryptedData);
        if (!(SmartHomeClientApi as any).META) throw new Error(`META not found for API type SmartHomeClientApi`);
        (SmartHomeClientApi as any).META.makeLocal_fromDataIn(context, dataInStatic, localApi);
        
    }
    public static fromRemote(context: FastFutureContext, provider: BytesConverter, remote: RemoteApiFuture<SmartHomeClientApiRemote>, sendFuture: AFuture): SmartHomeClientStream  {
        remote.executeAll(context, sendFuture);
        const encryptedData = provider(context.remoteDataToArrayAsArray());
        return new SmartHomeClientStream(encryptedData);
        
    }
    public static fromRemoteConsumer(context: FastFutureContext, provider: BytesConverter, remoteConsumer: AConsumer<SmartHomeClientApiRemote>): SmartHomeClientStream  {
        const api = (SmartHomeClientApi as any).META.makeRemote(context);
        remoteConsumer(api);
        const encryptedData = provider(context.remoteDataToArrayAsArray());
        return new SmartHomeClientStream(encryptedData);
        
    }
    public static fromRemoteBytes(provider: BytesConverter, remoteData: Uint8Array): SmartHomeClientStream  {
        const encryptedData = provider(remoteData);
        return new SmartHomeClientStream(encryptedData);
        
    }
    
}
/**
 * A stream of commands destined FOR the Commutator.
 */
export class SmartHomeCommutatorStream implements ToString  {
    public readonly data: Uint8Array;
    /**
     * Creates an instance of SmartHomeCommutatorStream.
     * @param data - The raw byte data for this stream.
     */
    constructor(data: Uint8Array)  {
        this.data = data;
        
    }
    public static readonly META: FastMetaType<SmartHomeCommutatorStream> = new Impl.SmartHomeCommutatorStreamMetaImpl();
    public toString(result: AString): AString  {
        SmartHomeCommutatorStream.META.metaToString(this, result);
        return result;
        
    }
    public accept(context: FastFutureContext, provider: BytesConverter, localApi: SmartHomeCommutatorApi): void  {
        const decryptedData = provider(this.data);
        const dataInStatic = new DataInOutStatic(decryptedData);
        if (!(SmartHomeCommutatorApi as any).META) throw new Error(`META not found for API type SmartHomeCommutatorApi`);
        (SmartHomeCommutatorApi as any).META.makeLocal_fromDataIn(context, dataInStatic, localApi);
        
    }
    public static fromRemote(context: FastFutureContext, provider: BytesConverter, remote: RemoteApiFuture<SmartHomeCommutatorApiRemote>, sendFuture: AFuture): SmartHomeCommutatorStream  {
        remote.executeAll(context, sendFuture);
        const encryptedData = provider(context.remoteDataToArrayAsArray());
        return new SmartHomeCommutatorStream(encryptedData);
        
    }
    public static fromRemoteConsumer(context: FastFutureContext, provider: BytesConverter, remoteConsumer: AConsumer<SmartHomeCommutatorApiRemote>): SmartHomeCommutatorStream  {
        const api = (SmartHomeCommutatorApi as any).META.makeRemote(context);
        remoteConsumer(api);
        const encryptedData = provider(context.remoteDataToArrayAsArray());
        return new SmartHomeCommutatorStream(encryptedData);
        
    }
    public static fromRemoteBytes(provider: BytesConverter, remoteData: Uint8Array): SmartHomeCommutatorStream  {
        const encryptedData = provider(remoteData);
        return new SmartHomeCommutatorStream(encryptedData);
        
    }
    
}
/**
 * A stream of commands destined FOR the Service.
 */
export class SmartHomeServiceStream implements ToString  {
    public readonly data: Uint8Array;
    /**
     * Creates an instance of SmartHomeServiceStream.
     * @param data - The raw byte data for this stream.
     */
    constructor(data: Uint8Array)  {
        this.data = data;
        
    }
    public static readonly META: FastMetaType<SmartHomeServiceStream> = new Impl.SmartHomeServiceStreamMetaImpl();
    public toString(result: AString): AString  {
        SmartHomeServiceStream.META.metaToString(this, result);
        return result;
        
    }
    public accept(context: FastFutureContext, provider: BytesConverter, localApi: SmartHomeServiceApi): void  {
        const decryptedData = provider(this.data);
        const dataInStatic = new DataInOutStatic(decryptedData);
        if (!(SmartHomeServiceApi as any).META) throw new Error(`META not found for API type SmartHomeServiceApi`);
        (SmartHomeServiceApi as any).META.makeLocal_fromDataIn(context, dataInStatic, localApi);
        
    }
    public static fromRemote(context: FastFutureContext, provider: BytesConverter, remote: RemoteApiFuture<SmartHomeServiceApiRemote>, sendFuture: AFuture): SmartHomeServiceStream  {
        remote.executeAll(context, sendFuture);
        const encryptedData = provider(context.remoteDataToArrayAsArray());
        return new SmartHomeServiceStream(encryptedData);
        
    }
    public static fromRemoteConsumer(context: FastFutureContext, provider: BytesConverter, remoteConsumer: AConsumer<SmartHomeServiceApiRemote>): SmartHomeServiceStream  {
        const api = (SmartHomeServiceApi as any).META.makeRemote(context);
        remoteConsumer(api);
        const encryptedData = provider(context.remoteDataToArrayAsArray());
        return new SmartHomeServiceStream(encryptedData);
        
    }
    public static fromRemoteBytes(provider: BytesConverter, remoteData: Uint8Array): SmartHomeServiceStream  {
        const encryptedData = provider(remoteData);
        return new SmartHomeServiceStream(encryptedData);
        
    }
    
}
/**
 * The main API implemented by the central Smart Home Service (Hub).
 */
export interface SmartHomeServiceApi  {
    /**
     * Registers the calling client with the service.
     *
     * @param type - ClientType
     * @param sensors - HardwareSensor[]
     * @param actors - HardwareActor[]
     *
     * @aetherMethodId 3
     */
    register(type: ClientType, sensors: HardwareSensor[], actors: HardwareActor[]): void;
    /**
     * Called by GUI to get a list of all *paired* logical devices.
     *
     * @returns ARFuture<Device[]>
     *
     * @aetherMethodId 4
     */
    getAllDevices(): ARFuture<Device[]>;
    /**
     * Called by GUI to send a command package to a specific actor on a specific commutator.
     *
     * @param commutatorId - UUID
     * @param localActorId - number
     * @param pkg - Uint8Array
     * @returns ARFuture<Actor>
     *
     * @aetherMethodId 5
     */
    executeActorCommand(commutatorId: UUID, localActorId: number, pkg: Uint8Array): ARFuture<Actor>;
    /**
     * Called by GUI to get a list of Commutators that are not yet approved.
     *
     * @returns ARFuture<PendingPairing[]>
     *
     * @aetherMethodId 6
     */
    getPendingPairings(): ARFuture<PendingPairing[]>;
    /**
     * Called by GUI when the user approves a Commutator.
     *
     * @param commutatorUuid - UUID
     *
     * @aetherMethodId 7
     */
    approvePairing(commutatorUuid: UUID): void;
    /**
     * Called by a paired Commutator to push new sensor data.
     *
     * @param localSensorId - number
     * @param data - DeviceStateData
     *
     * @aetherMethodId 8
     */
    pushSensorData(localSensorId: number, data: DeviceStateData): void;
    /**
     * Called by GUI to request a full sensor state refresh from all connected Commutators.
     *
     * @aetherMethodId 9
     */
    refreshAllSensorStates(): void;
    
}
export namespace SmartHomeServiceApi  {
    export const META: FastMetaApi<SmartHomeServiceApi, SmartHomeServiceApiRemote> = new Impl.SmartHomeServiceApiMetaImpl();
    
}
export interface SmartHomeServiceApiRemote extends SmartHomeServiceApi, RemoteApi  {
    
}
export abstract class SmartHomeServiceApiLocal<RT extends SmartHomeServiceApiRemote> implements SmartHomeServiceApi  {
    protected readonly remoteApi: RT;
    public getRemoteApi(): RT  {
        return this.remoteApi;
        
    }
    protected constructor(remoteApi: RT)  {
        this.remoteApi = remoteApi;
        
    }
    /**
     * Registers the calling client with the service.
     *
     * @param type - ClientType
     * @param sensors - HardwareSensor[]
     * @param actors - HardwareActor[]
     *
     * @aetherMethodId 3
     */
    public abstract register(type: ClientType, sensors: HardwareSensor[], actors: HardwareActor[]): void;
    /**
     * Called by GUI to get a list of all *paired* logical devices.
     *
     * @returns ARFuture<Device[]>
     *
     * @aetherMethodId 4
     */
    public abstract getAllDevices(): ARFuture<Device[]>;
    /**
     * Called by GUI to send a command package to a specific actor on a specific commutator.
     *
     * @param commutatorId - UUID
     * @param localActorId - number
     * @param pkg - Uint8Array
     * @returns ARFuture<Actor>
     *
     * @aetherMethodId 5
     */
    public abstract executeActorCommand(commutatorId: UUID, localActorId: number, pkg: Uint8Array): ARFuture<Actor>;
    /**
     * Called by GUI to get a list of Commutators that are not yet approved.
     *
     * @returns ARFuture<PendingPairing[]>
     *
     * @aetherMethodId 6
     */
    public abstract getPendingPairings(): ARFuture<PendingPairing[]>;
    /**
     * Called by GUI when the user approves a Commutator.
     *
     * @param commutatorUuid - UUID
     *
     * @aetherMethodId 7
     */
    public abstract approvePairing(commutatorUuid: UUID): void;
    /**
     * Called by a paired Commutator to push new sensor data.
     *
     * @param localSensorId - number
     * @param data - DeviceStateData
     *
     * @aetherMethodId 8
     */
    public abstract pushSensorData(localSensorId: number, data: DeviceStateData): void;
    /**
     * Called by GUI to request a full sensor state refresh from all connected Commutators.
     *
     * @aetherMethodId 9
     */
    public abstract refreshAllSensorStates(): void;
    
}
/**
 * The API implemented by each Commutator. It receives PUSH commands from the Service.
 */
export interface SmartHomeCommutatorApi  {
    /**
     * Called by the Service as the final step of pairing.
     *
     * @aetherMethodId 3
     */
    confirmPairing(): void;
    /**
     * Called by the Service to execute a command on a physical actor.
     *
     * @param localActorId - number
     * @param pkg - Uint8Array
     * @returns ARFuture<DeviceStateData>
     *
     * @aetherMethodId 4
     */
    executeActorCommand(localActorId: number, pkg: Uint8Array): ARFuture<DeviceStateData>;
    /**
     * Called by the Service to request the current state of a *single* device.
     *
     * @param localDeviceId - number
     * @returns ARFuture<DeviceStateData>
     *
     * @aetherMethodId 5
     */
    queryState(localDeviceId: number): ARFuture<DeviceStateData>;
    /**
     * Called by the Service to command this Commutator to read all its sensors.
     *
     * @aetherMethodId 6
     */
    queryAllSensorStates(): void;
    
}
export namespace SmartHomeCommutatorApi  {
    export const META: FastMetaApi<SmartHomeCommutatorApi, SmartHomeCommutatorApiRemote> = new Impl.SmartHomeCommutatorApiMetaImpl();
    
}
export interface SmartHomeCommutatorApiRemote extends SmartHomeCommutatorApi, RemoteApi  {
    
}
export abstract class SmartHomeCommutatorApiLocal<RT extends SmartHomeCommutatorApiRemote> implements SmartHomeCommutatorApi  {
    protected readonly remoteApi: RT;
    public getRemoteApi(): RT  {
        return this.remoteApi;
        
    }
    protected constructor(remoteApi: RT)  {
        this.remoteApi = remoteApi;
        
    }
    /**
     * Called by the Service as the final step of pairing.
     *
     * @aetherMethodId 3
     */
    public abstract confirmPairing(): void;
    /**
     * Called by the Service to execute a command on a physical actor.
     *
     * @param localActorId - number
     * @param pkg - Uint8Array
     * @returns ARFuture<DeviceStateData>
     *
     * @aetherMethodId 4
     */
    public abstract executeActorCommand(localActorId: number, pkg: Uint8Array): ARFuture<DeviceStateData>;
    /**
     * Called by the Service to request the current state of a *single* device.
     *
     * @param localDeviceId - number
     * @returns ARFuture<DeviceStateData>
     *
     * @aetherMethodId 5
     */
    public abstract queryState(localDeviceId: number): ARFuture<DeviceStateData>;
    /**
     * Called by the Service to command this Commutator to read all its sensors.
     *
     * @aetherMethodId 6
     */
    public abstract queryAllSensorStates(): void;
    
}
/**
 * The API implemented by the GUI Client. It receives PUSH notifications from the Service.
 */
export interface SmartHomeClientApi  {
    /**
     * PUSH notification from the Service when any device's state changes.
     *
     * @param device - Device
     *
     * @aetherMethodId 3
     */
    deviceStateUpdated(device: Device): void;
    /**
     * PUSH notification from the Service when a new Commutator connects.
     *
     * @param pairingInfo - PendingPairing
     *
     * @aetherMethodId 4
     */
    pairingRequested(pairingInfo: PendingPairing): void;
    
}
export namespace SmartHomeClientApi  {
    export const META: FastMetaApi<SmartHomeClientApi, SmartHomeClientApiRemote> = new Impl.SmartHomeClientApiMetaImpl();
    
}
export interface SmartHomeClientApiRemote extends SmartHomeClientApi, RemoteApi  {
    
}
export abstract class SmartHomeClientApiLocal<RT extends SmartHomeClientApiRemote> implements SmartHomeClientApi  {
    protected readonly remoteApi: RT;
    public getRemoteApi(): RT  {
        return this.remoteApi;
        
    }
    protected constructor(remoteApi: RT)  {
        this.remoteApi = remoteApi;
        
    }
    /**
     * PUSH notification from the Service when any device's state changes.
     *
     * @param device - Device
     *
     * @aetherMethodId 3
     */
    public abstract deviceStateUpdated(device: Device): void;
    /**
     * PUSH notification from the Service when a new Commutator connects.
     *
     * @param pairingInfo - PendingPairing
     *
     * @aetherMethodId 4
     */
    public abstract pairingRequested(pairingInfo: PendingPairing): void;
    
}