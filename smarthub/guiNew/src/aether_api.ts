import  {
    AFuture, ARFuture, DataInOutStatic, FastMetaType, FastFutureContext, RemoteApi, FastMetaApi, BytesConverter, UUID, URI, AConsumer, ToString, AString
}
from 'aether-client';
import * as Impl from './aether_api_impl';
// This is always relative
/**
 * Represents the SensorRecord structure.
 */
export class SensorRecord implements ToString  {
    public readonly value: number;
    public readonly time: number;
    public static readonly META_BODY: FastMetaType<SensorRecord> = new Impl.SensorRecordMetaBodyImpl();
    public static readonly META: FastMetaType<SensorRecord> = SensorRecord.META_BODY;
    /**
     * Creates an instance of SensorRecord.
     * @param value - number
     * @param time - number
     */
    constructor(value: number, time: number)  {
        this.value = value;
        this.time = time;
        
    }
    public getValue(): number  {
        return this.value;
        
    }
    public getTime(): number  {
        return this.time;
        
    }
    /**
     * Calculates a hash code for a static instance of SensorRecord.
     * @param {SensorRecord | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: SensorRecord | null | undefined): number  {
        return SensorRecord.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of SensorRecord with another object.
     * @param {SensorRecord | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: SensorRecord | null | undefined, v2: any | null | undefined): boolean  {
        return SensorRecord.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return SensorRecord.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return SensorRecord.staticEquals(this, other);
        
    }
    public toAString(result: AString): AString  {
        SensorRecord.META.metaToString(this, result);
        return result;
        
    }
    
}
export class DeviceStream implements ToString  {
    public readonly data: Uint8Array;
    /**
     * Creates an instance of DeviceStream.
     * @param data - The raw byte data for this stream.
     */
    constructor(data: Uint8Array)  {
        this.data = data;
        
    }
    public static readonly META: FastMetaType<DeviceStream> = new Impl.DeviceStreamMetaImpl();
    public toAString(result: AString): AString  {
        DeviceStream.META.metaToString(this, result);
        return result;
        
    }
    public accept(context: FastFutureContext, localApi: SmartHomeDeviceApi): void  {
        const dataInStatic = new DataInOutStatic(this.data);
        if (!(SmartHomeDeviceApi as any).META) throw new Error(`META not found for API type SmartHomeDeviceApi`);
        (SmartHomeDeviceApi as any).META.makeLocal_fromDataIn(context, dataInStatic, localApi);
        
    }
    public static remoteApi(context: FastFutureContext, apiConsumer: AConsumer<SmartHomeDeviceApiRemote>): DeviceStream  {
        const api = (SmartHomeDeviceApi as any).META.makeRemote(context);
        apiConsumer(api);
        return new DeviceStream(context.remoteDataToArrayAsArray());
        
    }
    
}
export class GuiStream implements ToString  {
    public readonly data: Uint8Array;
    /**
     * Creates an instance of GuiStream.
     * @param data - The raw byte data for this stream.
     */
    constructor(data: Uint8Array)  {
        this.data = data;
        
    }
    public static readonly META: FastMetaType<GuiStream> = new Impl.GuiStreamMetaImpl();
    public toAString(result: AString): AString  {
        GuiStream.META.metaToString(this, result);
        return result;
        
    }
    public accept(context: FastFutureContext, localApi: SmartHomeGuiApi): void  {
        const dataInStatic = new DataInOutStatic(this.data);
        if (!(SmartHomeGuiApi as any).META) throw new Error(`META not found for API type SmartHomeGuiApi`);
        (SmartHomeGuiApi as any).META.makeLocal_fromDataIn(context, dataInStatic, localApi);
        
    }
    public static remoteApi(context: FastFutureContext, apiConsumer: AConsumer<SmartHomeGuiApiRemote>): GuiStream  {
        const api = (SmartHomeGuiApi as any).META.makeRemote(context);
        apiConsumer(api);
        return new GuiStream(context.remoteDataToArrayAsArray());
        
    }
    
}
export interface SmartHomeHubRegistryApi  {
    /**
     * @param stream - DeviceStream
     *
     * @aetherMethodId 3
     */
    device(stream: DeviceStream): void;
    /**
     * @param stream - GuiStream
     *
     * @aetherMethodId 4
     */
    gui(stream: GuiStream): void;
    
}
export namespace SmartHomeHubRegistryApi  {
    export const META: FastMetaApi<SmartHomeHubRegistryApi, SmartHomeHubRegistryApiRemote> = new Impl.SmartHomeHubRegistryApiMetaImpl();
    
}
export interface SmartHomeHubRegistryApiRemote extends SmartHomeHubRegistryApi, RemoteApi  {
    
}
export abstract class SmartHomeHubRegistryApiLocal<RT extends RemoteApi> implements SmartHomeHubRegistryApi  {
    protected readonly remoteApi: RT;
    public getRemoteApi(): RT  {
        return this.remoteApi;
        
    }
    protected constructor(remoteApi: RT)  {
        this.remoteApi = remoteApi;
        
    }
    /**
     * @param stream - DeviceStream
     *
     * @aetherMethodId 3
     */
    public abstract device(stream: DeviceStream): void;
    /**
     * @param stream - GuiStream
     *
     * @aetherMethodId 4
     */
    public abstract gui(stream: GuiStream): void;
    
}
export interface SmartHomeDeviceApi  {
    /**
     * @param deviceUid - UUID
     * @param value - SensorRecord[]
     * @returns ARFuture<boolean>
     *
     * @aetherMethodId 10
     */
    reportState(deviceUid: UUID, value: SensorRecord[]): ARFuture<boolean>;
    
}
export namespace SmartHomeDeviceApi  {
    export const META: FastMetaApi<SmartHomeDeviceApi, SmartHomeDeviceApiRemote> = new Impl.SmartHomeDeviceApiMetaImpl();
    
}
export interface SmartHomeDeviceApiRemote extends SmartHomeDeviceApi, RemoteApi  {
    
}
export abstract class SmartHomeDeviceApiLocal<RT extends RemoteApi> implements SmartHomeDeviceApi  {
    protected readonly remoteApi: RT;
    public getRemoteApi(): RT  {
        return this.remoteApi;
        
    }
    protected constructor(remoteApi: RT)  {
        this.remoteApi = remoteApi;
        
    }
    /**
     * @param deviceUid - UUID
     * @param value - SensorRecord[]
     * @returns ARFuture<boolean>
     *
     * @aetherMethodId 10
     */
    public abstract reportState(deviceUid: UUID, value: SensorRecord[]): ARFuture<boolean>;
    
}
export interface SmartHomeGuiApi  {
    /**
     * @returns ARFuture<UUID[]>
     *
     * @aetherMethodId 12
     */
    getDevices(): ARFuture<UUID[]>;
    /**
     * @param deviceUid - UUID
     * @returns ARFuture<boolean>
     *
     * @aetherMethodId 13
     */
    subscribeToDevice(deviceUid: UUID): ARFuture<boolean>;
    /**
     * @param deviceUid - UUID
     * @returns ARFuture<boolean>
     *
     * @aetherMethodId 14
     */
    unsubscribeFromDevice(deviceUid: UUID): ARFuture<boolean>;
    /**
     * @param deviceUid - UUID
     * @param count - bigint
     * @returns ARFuture<SensorRecord[]>
     *
     * @aetherMethodId 15
     */
    requestDeviceHistory(deviceUid: UUID, count: bigint): ARFuture<SensorRecord[]>;
    
}
export namespace SmartHomeGuiApi  {
    export const META: FastMetaApi<SmartHomeGuiApi, SmartHomeGuiApiRemote> = new Impl.SmartHomeGuiApiMetaImpl();
    
}
export interface SmartHomeGuiApiRemote extends SmartHomeGuiApi, RemoteApi  {
    
}
export abstract class SmartHomeGuiApiLocal<RT extends RemoteApi> implements SmartHomeGuiApi  {
    protected readonly remoteApi: RT;
    public getRemoteApi(): RT  {
        return this.remoteApi;
        
    }
    protected constructor(remoteApi: RT)  {
        this.remoteApi = remoteApi;
        
    }
    /**
     * @returns ARFuture<UUID[]>
     *
     * @aetherMethodId 12
     */
    public abstract getDevices(): ARFuture<UUID[]>;
    /**
     * @param deviceUid - UUID
     * @returns ARFuture<boolean>
     *
     * @aetherMethodId 13
     */
    public abstract subscribeToDevice(deviceUid: UUID): ARFuture<boolean>;
    /**
     * @param deviceUid - UUID
     * @returns ARFuture<boolean>
     *
     * @aetherMethodId 14
     */
    public abstract unsubscribeFromDevice(deviceUid: UUID): ARFuture<boolean>;
    /**
     * @param deviceUid - UUID
     * @param count - bigint
     * @returns ARFuture<SensorRecord[]>
     *
     * @aetherMethodId 15
     */
    public abstract requestDeviceHistory(deviceUid: UUID, count: bigint): ARFuture<SensorRecord[]>;
    
}
export interface SmartHomeClientGuiApi  {
    /**
     * @param deviceUid - UUID
     * @param records - SensorRecord[]
     *
     * @aetherMethodId 20
     */
    deviceStateUpdated(deviceUid: UUID, records: SensorRecord[]): void;
    
}
export namespace SmartHomeClientGuiApi  {
    export const META: FastMetaApi<SmartHomeClientGuiApi, SmartHomeClientGuiApiRemote> = new Impl.SmartHomeClientGuiApiMetaImpl();
    
}
export interface SmartHomeClientGuiApiRemote extends SmartHomeClientGuiApi, RemoteApi  {
    
}
export abstract class SmartHomeClientGuiApiLocal<RT extends RemoteApi> implements SmartHomeClientGuiApi  {
    protected readonly remoteApi: RT;
    public getRemoteApi(): RT  {
        return this.remoteApi;
        
    }
    protected constructor(remoteApi: RT)  {
        this.remoteApi = remoteApi;
        
    }
    /**
     * @param deviceUid - UUID
     * @param records - SensorRecord[]
     *
     * @aetherMethodId 20
     */
    public abstract deviceStateUpdated(deviceUid: UUID, records: SensorRecord[]): void;
    
}
export interface SmartHomeClientDeviceApi  {
    
}
export namespace SmartHomeClientDeviceApi  {
    export const EMPTY: SmartHomeClientDeviceApi =  {
        
    };
    export const META: FastMetaApi<SmartHomeClientDeviceApi, SmartHomeClientDeviceApiRemote> = new Impl.SmartHomeClientDeviceApiMetaImpl();
    
}
export interface SmartHomeClientDeviceApiRemote extends SmartHomeClientDeviceApi, RemoteApi  {
    
}
export abstract class SmartHomeClientDeviceApiLocal<RT extends RemoteApi> implements SmartHomeClientDeviceApi  {
    protected readonly remoteApi: RT;
    public getRemoteApi(): RT  {
        return this.remoteApi;
        
    }
    protected constructor(remoteApi: RT)  {
        this.remoteApi = remoteApi;
        
    }
    
}