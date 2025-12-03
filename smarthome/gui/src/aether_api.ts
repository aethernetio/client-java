import  {
    AFuture, ARFuture, DataIn, DataOut, DataInOut, DataInOutStatic, FastMetaType, FastFutureContext, RemoteApi, FastMeta, SerializerPackNumber, DeserializerPackNumber, FastApiContextLocal, FastMetaApi, BytesConverter, RemoteApiFuture, UUID, URI, AConsumer, ToString, AString
}
from 'aether-client';
import * as Impl from './aether_api_impl';
// This is always relative
/**
 * Базовый тип физического устройства на коммутаторе.
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
 * Абстрактный базовый класс для передачи типизированных данных.
 */
export abstract class VariantData implements ToString  {
    public abstract getAetherTypeId(): number;
    public static readonly META: FastMetaType<VariantData> = new Impl.VariantDataMetaImpl();
    /**
     * Creates an instance of VariantData.
     */
    constructor()  {
        
    }
    /**
     * Calculates a hash code for a static instance of VariantData.
     * @param {VariantData | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: VariantData | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        return (obj.constructor as any).META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of VariantData with another object.
     * @param {VariantData | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: VariantData | null | undefined, v2: any | null | undefined): boolean  {
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
 * Структура состояния (значение + метка времени).
 */
export class DeviceStateData implements ToString  {
    public readonly payload: VariantData;
    public readonly timestamp: Date;
    public static readonly META_BODY: FastMetaType<DeviceStateData> = new Impl.DeviceStateDataMetaBodyImpl();
    public static readonly META: FastMetaType<DeviceStateData> = DeviceStateData.META_BODY;
    /**
     * Creates an instance of DeviceStateData.
     * @param payload - VariantData
     * @param timestamp - Date
     */
    constructor(payload: VariantData, timestamp: Date)  {
        this.payload = payload;
        this.timestamp = timestamp;
        if (payload === null || payload === undefined) throw new Error(`Field 'payload' cannot be null for type DeviceStateData.`);
        
    }
    public getPayload(): VariantData  {
        return this.payload;
        
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
 * Represents the HardwareActor structure.
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
 * Represents the HardwareSensor structure.
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
/**
 * Represents the VariantBool structure.
 *
 * @aetherTypeId 1
 */
export class VariantBool extends VariantData implements ToString  {
    public readonly value: boolean;
    public override getAetherTypeId(): number  {
        return 1;
        
    }
    public static readonly META_BODY: FastMetaType<VariantBool> = new Impl.VariantBoolMetaBodyImpl();
    public static readonly META: FastMetaType<VariantBool> = new Impl.VariantBoolMetaImpl();
    /**
     * Creates an instance of VariantBool.
     * @param value - boolean
     */
    constructor(value: boolean)  {
        super();
        this.value = value;
        
    }
    public isValue(): boolean  {
        return this.value;
        
    }
    /**
     * Calculates a hash code for a static instance of VariantBool.
     * @param {VariantBool | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: VariantBool | null | undefined): number  {
        return VariantBool.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of VariantBool with another object.
     * @param {VariantBool | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: VariantBool | null | undefined, v2: any | null | undefined): boolean  {
        return VariantBool.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return VariantBool.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return VariantBool.staticEquals(this, other);
        
    }
    public toString(result: AString): AString  {
        VariantBool.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * Для передачи сложных бинарных данных.
 *
 * @aetherTypeId 5
 */
export class VariantBytes extends VariantData implements ToString  {
    public readonly value: Uint8Array;
    public override getAetherTypeId(): number  {
        return 5;
        
    }
    public static readonly META_BODY: FastMetaType<VariantBytes> = new Impl.VariantBytesMetaBodyImpl();
    public static readonly META: FastMetaType<VariantBytes> = new Impl.VariantBytesMetaImpl();
    /**
     * Creates an instance of VariantBytes.
     * @param value - Uint8Array
     */
    constructor(value: Uint8Array)  {
        super();
        this.value = value;
        if (value === null || value === undefined) throw new Error(`Field 'value' cannot be null for type VariantBytes.`);
        
    }
    public getValue(): Uint8Array  {
        return this.value;
        
    }
    public valueContains(el: number): boolean  {
        return (this.value as Uint8Array).includes(el as any);
        
    }
    /**
     * Calculates a hash code for a static instance of VariantBytes.
     * @param {VariantBytes | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: VariantBytes | null | undefined): number  {
        return VariantBytes.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of VariantBytes with another object.
     * @param {VariantBytes | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: VariantBytes | null | undefined, v2: any | null | undefined): boolean  {
        return VariantBytes.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return VariantBytes.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return VariantBytes.staticEquals(this, other);
        
    }
    public toString(result: AString): AString  {
        VariantBytes.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * Используется для всех чисел с плавающей запятой (float, double).
 *
 * @aetherTypeId 3
 */
export class VariantDouble extends VariantData implements ToString  {
    public readonly value: number;
    public override getAetherTypeId(): number  {
        return 3;
        
    }
    public static readonly META_BODY: FastMetaType<VariantDouble> = new Impl.VariantDoubleMetaBodyImpl();
    public static readonly META: FastMetaType<VariantDouble> = new Impl.VariantDoubleMetaImpl();
    /**
     * Creates an instance of VariantDouble.
     * @param value - number
     */
    constructor(value: number)  {
        super();
        this.value = value;
        
    }
    public getValue(): number  {
        return this.value;
        
    }
    /**
     * Calculates a hash code for a static instance of VariantDouble.
     * @param {VariantDouble | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: VariantDouble | null | undefined): number  {
        return VariantDouble.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of VariantDouble with another object.
     * @param {VariantDouble | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: VariantDouble | null | undefined, v2: any | null | undefined): boolean  {
        return VariantDouble.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return VariantDouble.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return VariantDouble.staticEquals(this, other);
        
    }
    public toString(result: AString): AString  {
        VariantDouble.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * Используется для всех целых чисел (byte, short, int, long).
 *
 * @aetherTypeId 2
 */
export class VariantLong extends VariantData implements ToString  {
    public readonly value: bigint;
    public override getAetherTypeId(): number  {
        return 2;
        
    }
    public static readonly META_BODY: FastMetaType<VariantLong> = new Impl.VariantLongMetaBodyImpl();
    public static readonly META: FastMetaType<VariantLong> = new Impl.VariantLongMetaImpl();
    /**
     * Creates an instance of VariantLong.
     * @param value - bigint
     */
    constructor(value: bigint)  {
        super();
        this.value = value;
        
    }
    public getValue(): bigint  {
        return this.value;
        
    }
    /**
     * Calculates a hash code for a static instance of VariantLong.
     * @param {VariantLong | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: VariantLong | null | undefined): number  {
        return VariantLong.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of VariantLong with another object.
     * @param {VariantLong | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: VariantLong | null | undefined, v2: any | null | undefined): boolean  {
        return VariantLong.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return VariantLong.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return VariantLong.staticEquals(this, other);
        
    }
    public toString(result: AString): AString  {
        VariantLong.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * Represents the VariantString structure.
 *
 * @aetherTypeId 4
 */
export class VariantString extends VariantData implements ToString  {
    public readonly value: string;
    public override getAetherTypeId(): number  {
        return 4;
        
    }
    public static readonly META_BODY: FastMetaType<VariantString> = new Impl.VariantStringMetaBodyImpl();
    public static readonly META: FastMetaType<VariantString> = new Impl.VariantStringMetaImpl();
    /**
     * Creates an instance of VariantString.
     * @param value - string
     */
    constructor(value: string)  {
        super();
        this.value = value;
        
    }
    public getValue(): string  {
        return this.value;
        
    }
    /**
     * Calculates a hash code for a static instance of VariantString.
     * @param {VariantString | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: VariantString | null | undefined): number  {
        return VariantString.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of VariantString with another object.
     * @param {VariantString | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: VariantString | null | undefined, v2: any | null | undefined): boolean  {
        return VariantString.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return VariantString.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return VariantString.staticEquals(this, other);
        
    }
    public toString(result: AString): AString  {
        VariantString.META.metaToString(this, result);
        return result;
        
    }
    
}
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
 * The API implemented by each Commutator.
 */
export interface SmartHomeCommutatorApi  {
    /**
     * Запрашивает список всех физических устройств на этом коммутаторе.
     *
     * @returns ARFuture<HardwareDevice[]>
     *
     * @aetherMethodId 10
     */
    getSystemStructure(): ARFuture<HardwareDevice[]>;
    /**
     * Отправить команду на конкретный актуатор.
     *
     * @param localActorId - number
     * @param command - VariantData
     * @returns ARFuture<DeviceStateData>
     *
     * @aetherMethodId 4
     */
    executeActorCommand(localActorId: number, command: VariantData): ARFuture<DeviceStateData>;
    /**
     * Запросить текущее состояние конкретного устройства.
     *
     * @param localDeviceId - number
     * @returns ARFuture<DeviceStateData>
     *
     * @aetherMethodId 5
     */
    queryState(localDeviceId: number): ARFuture<DeviceStateData>;
    /**
     * Попросить коммутатор прислать PUSH-уведомления о состоянии всех датчиков.
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
     * Запрашивает список всех физических устройств на этом коммутаторе.
     *
     * @returns ARFuture<HardwareDevice[]>
     *
     * @aetherMethodId 10
     */
    public abstract getSystemStructure(): ARFuture<HardwareDevice[]>;
    /**
     * Отправить команду на конкретный актуатор.
     *
     * @param localActorId - number
     * @param command - VariantData
     * @returns ARFuture<DeviceStateData>
     *
     * @aetherMethodId 4
     */
    public abstract executeActorCommand(localActorId: number, command: VariantData): ARFuture<DeviceStateData>;
    /**
     * Запросить текущее состояние конкретного устройства.
     *
     * @param localDeviceId - number
     * @returns ARFuture<DeviceStateData>
     *
     * @aetherMethodId 5
     */
    public abstract queryState(localDeviceId: number): ARFuture<DeviceStateData>;
    /**
     * Попросить коммутатор прислать PUSH-уведомления о состоянии всех датчиков.
     *
     * @aetherMethodId 6
     */
    public abstract queryAllSensorStates(): void;
    
}
/**
 * The API implemented by the GUI Client.
 */
export interface SmartHomeClientApi  {
    /**
     * Уведомление от коммутатора об изменении состояния.
     *
     * @param localDeviceId - number
     * @param state - DeviceStateData
     *
     * @aetherMethodId 3
     */
    deviceStateUpdated(localDeviceId: number, state: DeviceStateData): void;
    
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
     * Уведомление от коммутатора об изменении состояния.
     *
     * @param localDeviceId - number
     * @param state - DeviceStateData
     *
     * @aetherMethodId 3
     */
    public abstract deviceStateUpdated(localDeviceId: number, state: DeviceStateData): void;
    
}