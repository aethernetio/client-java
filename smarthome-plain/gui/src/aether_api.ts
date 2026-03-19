import  {
    AFuture, ARFuture, DataIn, DataOut, DataInOut, DataInOutStatic, FastMetaType, FastFutureContext, RemoteApi, FastMeta, SerializerPackNumber, DeserializerPackNumber, FastApiContextLocal, FastMetaApi, BytesConverter, UUID, URI, AConsumer, ToString, AString, FlushReport
}
from 'aether-client';
import * as Impl from './aether_api_impl';
// This is always relative
/**
 * Represents the Record structure.
 */
export class Record implements ToString  {
    public readonly value: number;
    public readonly time: number;
    public static readonly META_BODY: FastMetaType<Record> = new Impl.RecordMetaBodyImpl();
    public static readonly META: FastMetaType<Record> = Record.META_BODY;
    /**
     * Creates an instance of Record.
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
     * Calculates a hash code for a static instance of Record.
     * @param {Record | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: Record | null | undefined): number  {
        return Record.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of Record with another object.
     * @param {Record | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: Record | null | undefined, v2: any | null | undefined): boolean  {
        return Record.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return Record.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return Record.staticEquals(this, other);
        
    }
    public toAString(result: AString): AString  {
        Record.META.metaToString(this, result);
        return result;
        
    }
    
}
export interface SimpleDeviceApi  {
    /**
     * @param count - number
     *
     * @aetherMethodId 3
     */
    requestRecords(count: number): void;
    
}
export namespace SimpleDeviceApi  {
    export const META: FastMetaApi<SimpleDeviceApi, SimpleDeviceApiRemote> = new Impl.SimpleDeviceApiMetaImpl();
    
}
export interface SimpleDeviceApiRemote extends SimpleDeviceApi, RemoteApi  {
    
}
export abstract class SimpleDeviceApiLocal<RT extends SimpleDeviceApiRemote> implements SimpleDeviceApi  {
    protected readonly remoteApi: RT;
    public getRemoteApi(): RT  {
        return this.remoteApi;
        
    }
    protected constructor(remoteApi: RT)  {
        this.remoteApi = remoteApi;
        
    }
    /**
     * @param count - number
     *
     * @aetherMethodId 3
     */
    public abstract requestRecords(count: number): void;
    
}
export interface SimpleClientApi  {
    /**
     * @param value - Record[]
     *
     * @aetherMethodId 3
     */
    receiveStatus(value: Record[]): void;
    
}
export namespace SimpleClientApi  {
    export const META: FastMetaApi<SimpleClientApi, SimpleClientApiRemote> = new Impl.SimpleClientApiMetaImpl();
    
}
export interface SimpleClientApiRemote extends SimpleClientApi, RemoteApi  {
    
}
export abstract class SimpleClientApiLocal<RT extends SimpleClientApiRemote> implements SimpleClientApi  {
    protected readonly remoteApi: RT;
    public getRemoteApi(): RT  {
        return this.remoteApi;
        
    }
    protected constructor(remoteApi: RT)  {
        this.remoteApi = remoteApi;
        
    }
    /**
     * @param value - Record[]
     *
     * @aetherMethodId 3
     */
    public abstract receiveStatus(value: Record[]): void;
    
}