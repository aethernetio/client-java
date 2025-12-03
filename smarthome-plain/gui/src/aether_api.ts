import  {
    AFuture, ARFuture, DataIn, DataOut, DataInOut, DataInOutStatic, FastMetaType, FastFutureContext, RemoteApi, FastMeta, SerializerPackNumber, DeserializerPackNumber, FastApiContextLocal, FastMetaApi, BytesConverter, RemoteApiFuture, UUID, URI, AConsumer, ToString, AString
}
from 'aether-client';
import * as Impl from './aether_api_impl';
// This is always relative
/**
 * Represents the AppStateData structure.
 */
export class AppStateData implements ToString  {
    public readonly pollingTimeoutMs: number;
    public readonly pollingBufferSize: number;
    public readonly targetDeviceId: number;
    public readonly targetCommutatorUuid: string | null;
    public readonly knownCommutators: KnownCommutator[];
    public readonly sensorHistory: SensorHistorySeries[];
    public readonly rttMetrics: MetricPoint[];
    public readonly totalRequests: number;
    public readonly packetLossCount: number;
    public readonly visibleSensorKeys: string[];
    public static readonly META_BODY: FastMetaType<AppStateData> = new Impl.AppStateDataMetaBodyImpl();
    public static readonly META: FastMetaType<AppStateData> = AppStateData.META_BODY;
    /**
     * Creates an instance of AppStateData.
     * @param pollingTimeoutMs - number
     * @param pollingBufferSize - number
     * @param targetDeviceId - number
     * @param targetCommutatorUuid - string
     * @param knownCommutators - KnownCommutator[]
     * @param sensorHistory - SensorHistorySeries[]
     * @param rttMetrics - MetricPoint[]
     * @param totalRequests - number
     * @param packetLossCount - number
     * @param visibleSensorKeys - string[]
     */
    constructor(pollingTimeoutMs: number, pollingBufferSize: number, targetDeviceId: number, targetCommutatorUuid: string, knownCommutators: KnownCommutator[], sensorHistory: SensorHistorySeries[], rttMetrics: MetricPoint[], totalRequests: number, packetLossCount: number, visibleSensorKeys: string[])  {
        this.pollingTimeoutMs = pollingTimeoutMs;
        this.pollingBufferSize = pollingBufferSize;
        this.targetDeviceId = targetDeviceId;
        this.targetCommutatorUuid = targetCommutatorUuid;
        this.knownCommutators = knownCommutators;
        this.sensorHistory = sensorHistory;
        this.rttMetrics = rttMetrics;
        this.totalRequests = totalRequests;
        this.packetLossCount = packetLossCount;
        this.visibleSensorKeys = visibleSensorKeys;
        if (knownCommutators === null || knownCommutators === undefined) throw new Error(`Field 'knownCommutators' cannot be null for type AppStateData.`);
        if (sensorHistory === null || sensorHistory === undefined) throw new Error(`Field 'sensorHistory' cannot be null for type AppStateData.`);
        if (rttMetrics === null || rttMetrics === undefined) throw new Error(`Field 'rttMetrics' cannot be null for type AppStateData.`);
        if (visibleSensorKeys === null || visibleSensorKeys === undefined) throw new Error(`Field 'visibleSensorKeys' cannot be null for type AppStateData.`);
        
    }
    public getPollingTimeoutMs(): number  {
        return this.pollingTimeoutMs;
        
    }
    public getPollingBufferSize(): number  {
        return this.pollingBufferSize;
        
    }
    public getTargetDeviceId(): number  {
        return this.targetDeviceId;
        
    }
    public getTargetCommutatorUuid(): string | null  {
        return this.targetCommutatorUuid;
        
    }
    public getKnownCommutators(): KnownCommutator[]  {
        return this.knownCommutators;
        
    }
    public knownCommutatorsContains(el: KnownCommutator): boolean  {
        return (this.knownCommutators as KnownCommutator[]).includes(el as any);
        
    }
    public getSensorHistory(): SensorHistorySeries[]  {
        return this.sensorHistory;
        
    }
    public sensorHistoryContains(el: SensorHistorySeries): boolean  {
        return (this.sensorHistory as SensorHistorySeries[]).includes(el as any);
        
    }
    public getRttMetrics(): MetricPoint[]  {
        return this.rttMetrics;
        
    }
    public rttMetricsContains(el: MetricPoint): boolean  {
        return (this.rttMetrics as MetricPoint[]).includes(el as any);
        
    }
    public getTotalRequests(): number  {
        return this.totalRequests;
        
    }
    public getPacketLossCount(): number  {
        return this.packetLossCount;
        
    }
    public getVisibleSensorKeys(): string[]  {
        return this.visibleSensorKeys;
        
    }
    public visibleSensorKeysContains(el: string): boolean  {
        return (this.visibleSensorKeys as string[]).includes(el as any);
        
    }
    /**
     * Calculates a hash code for a static instance of AppStateData.
     * @param {AppStateData | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: AppStateData | null | undefined): number  {
        return AppStateData.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of AppStateData with another object.
     * @param {AppStateData | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: AppStateData | null | undefined, v2: any | null | undefined): boolean  {
        return AppStateData.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return AppStateData.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return AppStateData.staticEquals(this, other);
        
    }
    public toString(result: AString): AString  {
        AppStateData.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * Represents the GraphPoint structure.
 */
export class GraphPoint implements ToString  {
    public readonly timestamp: bigint;
    public readonly value: number;
    public static readonly META_BODY: FastMetaType<GraphPoint> = new Impl.GraphPointMetaBodyImpl();
    public static readonly META: FastMetaType<GraphPoint> = GraphPoint.META_BODY;
    /**
     * Creates an instance of GraphPoint.
     * @param timestamp - bigint
     * @param value - number
     */
    constructor(timestamp: bigint, value: number)  {
        this.timestamp = timestamp;
        this.value = value;
        
    }
    public getTimestamp(): bigint  {
        return this.timestamp;
        
    }
    public getValue(): number  {
        return this.value;
        
    }
    /**
     * Calculates a hash code for a static instance of GraphPoint.
     * @param {GraphPoint | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: GraphPoint | null | undefined): number  {
        return GraphPoint.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of GraphPoint with another object.
     * @param {GraphPoint | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: GraphPoint | null | undefined, v2: any | null | undefined): boolean  {
        return GraphPoint.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return GraphPoint.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return GraphPoint.staticEquals(this, other);
        
    }
    public toString(result: AString): AString  {
        GraphPoint.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * Represents the KnownCommutator structure.
 */
export class KnownCommutator implements ToString  {
    public readonly uuid: string;
    public readonly label: string | null;
    public readonly lastSeen: bigint;
    public readonly totalRequests: number;
    public readonly totalResponses: number;
    public static readonly META_BODY: FastMetaType<KnownCommutator> = new Impl.KnownCommutatorMetaBodyImpl();
    public static readonly META: FastMetaType<KnownCommutator> = KnownCommutator.META_BODY;
    /**
     * Creates an instance of KnownCommutator.
     * @param uuid - string
     * @param label - string
     * @param lastSeen - bigint
     * @param totalRequests - number
     * @param totalResponses - number
     */
    constructor(uuid: string, label: string, lastSeen: bigint, totalRequests: number, totalResponses: number)  {
        this.uuid = uuid;
        this.label = label;
        this.lastSeen = lastSeen;
        this.totalRequests = totalRequests;
        this.totalResponses = totalResponses;
        
    }
    public getUuid(): string  {
        return this.uuid;
        
    }
    public getLabel(): string | null  {
        return this.label;
        
    }
    public getLastSeen(): bigint  {
        return this.lastSeen;
        
    }
    public getTotalRequests(): number  {
        return this.totalRequests;
        
    }
    public getTotalResponses(): number  {
        return this.totalResponses;
        
    }
    /**
     * Calculates a hash code for a static instance of KnownCommutator.
     * @param {KnownCommutator | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: KnownCommutator | null | undefined): number  {
        return KnownCommutator.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of KnownCommutator with another object.
     * @param {KnownCommutator | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: KnownCommutator | null | undefined, v2: any | null | undefined): boolean  {
        return KnownCommutator.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return KnownCommutator.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return KnownCommutator.staticEquals(this, other);
        
    }
    public toString(result: AString): AString  {
        KnownCommutator.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * Represents the MetricPoint structure.
 */
export class MetricPoint implements ToString  {
    public readonly timestamp: bigint;
    public readonly rtt: number;
    public static readonly META_BODY: FastMetaType<MetricPoint> = new Impl.MetricPointMetaBodyImpl();
    public static readonly META: FastMetaType<MetricPoint> = MetricPoint.META_BODY;
    /**
     * Creates an instance of MetricPoint.
     * @param timestamp - bigint
     * @param rtt - number
     */
    constructor(timestamp: bigint, rtt: number)  {
        this.timestamp = timestamp;
        this.rtt = rtt;
        
    }
    public getTimestamp(): bigint  {
        return this.timestamp;
        
    }
    public getRtt(): number  {
        return this.rtt;
        
    }
    /**
     * Calculates a hash code for a static instance of MetricPoint.
     * @param {MetricPoint | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: MetricPoint | null | undefined): number  {
        return MetricPoint.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of MetricPoint with another object.
     * @param {MetricPoint | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: MetricPoint | null | undefined, v2: any | null | undefined): boolean  {
        return MetricPoint.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return MetricPoint.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return MetricPoint.staticEquals(this, other);
        
    }
    public toString(result: AString): AString  {
        MetricPoint.META.metaToString(this, result);
        return result;
        
    }
    
}
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
    public toString(result: AString): AString  {
        Record.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * Represents the SensorHistorySeries structure.
 */
export class SensorHistorySeries implements ToString  {
    public readonly commutatorUuid: string;
    public readonly deviceId: number;
    public readonly deviceName: string;
    public readonly unit: string | null;
    public readonly points: GraphPoint[];
    public static readonly META_BODY: FastMetaType<SensorHistorySeries> = new Impl.SensorHistorySeriesMetaBodyImpl();
    public static readonly META: FastMetaType<SensorHistorySeries> = SensorHistorySeries.META_BODY;
    /**
     * Creates an instance of SensorHistorySeries.
     * @param commutatorUuid - string
     * @param deviceId - number
     * @param deviceName - string
     * @param unit - string
     * @param points - GraphPoint[]
     */
    constructor(commutatorUuid: string, deviceId: number, deviceName: string, unit: string, points: GraphPoint[])  {
        this.commutatorUuid = commutatorUuid;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.unit = unit;
        this.points = points;
        if (points === null || points === undefined) throw new Error(`Field 'points' cannot be null for type SensorHistorySeries.`);
        
    }
    public getCommutatorUuid(): string  {
        return this.commutatorUuid;
        
    }
    public getDeviceId(): number  {
        return this.deviceId;
        
    }
    public getDeviceName(): string  {
        return this.deviceName;
        
    }
    public getUnit(): string | null  {
        return this.unit;
        
    }
    public getPoints(): GraphPoint[]  {
        return this.points;
        
    }
    public pointsContains(el: GraphPoint): boolean  {
        return (this.points as GraphPoint[]).includes(el as any);
        
    }
    /**
     * Calculates a hash code for a static instance of SensorHistorySeries.
     * @param {SensorHistorySeries | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: SensorHistorySeries | null | undefined): number  {
        return SensorHistorySeries.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of SensorHistorySeries with another object.
     * @param {SensorHistorySeries | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: SensorHistorySeries | null | undefined, v2: any | null | undefined): boolean  {
        return SensorHistorySeries.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return SensorHistorySeries.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return SensorHistorySeries.staticEquals(this, other);
        
    }
    public toString(result: AString): AString  {
        SensorHistorySeries.META.metaToString(this, result);
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