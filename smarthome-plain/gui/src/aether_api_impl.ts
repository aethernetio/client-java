import  {
    AFuture, ARFuture, DataIn, DataOut, DataInOut, DataInOutStatic, FastMetaType, FastFutureContext, RemoteApi, FastMeta, SerializerPackNumber, DeserializerPackNumber, FastApiContextLocal, FastMetaApi, BytesConverter, RemoteApiFuture, FastFutureContextStub, UUID, URI, AConsumer, ToString, AString
}
from 'aether-client';
import  {
    AppStateData, GraphPoint, KnownCommutator, MetricPoint, Record, SensorHistorySeries, SimpleDeviceApi, SimpleClientApi, SimpleDeviceApiRemote, SimpleClientApiRemote
}
from './aether_api';
// This is always relative
export class AppStateDataMetaBodyImpl implements FastMetaType<AppStateData>  {
    serialize(sCtx_0: FastFutureContext, obj_1: AppStateData, _out_2: DataOut): void  {
        let _mask: number = 0;
        if (obj_1.targetCommutatorUuid === null) _mask |= 1;
        _out_2.writeByte(_mask);
        _out_2.writeInt(obj_1.pollingTimeoutMs);
        _out_2.writeInt(obj_1.pollingBufferSize);
        _out_2.writeInt(obj_1.targetDeviceId);
        if (obj_1.targetCommutatorUuid !== null)  {
            const stringBytes_4 = new TextEncoder().encode(obj_1.targetCommutatorUuid);
            SerializerPackNumber.INSTANCE.put(_out_2, stringBytes_4.length);
            _out_2.write(stringBytes_4);
            
        }
        SerializerPackNumber.INSTANCE.put(_out_2, obj_1.knownCommutators.length);
        for (const el_6 of obj_1.knownCommutators)  {
            KnownCommutator.META.serialize(sCtx_0, el_6, _out_2);
            
        }
        SerializerPackNumber.INSTANCE.put(_out_2, obj_1.sensorHistory.length);
        for (const el_7 of obj_1.sensorHistory)  {
            SensorHistorySeries.META.serialize(sCtx_0, el_7, _out_2);
            
        }
        SerializerPackNumber.INSTANCE.put(_out_2, obj_1.rttMetrics.length);
        for (const el_8 of obj_1.rttMetrics)  {
            MetricPoint.META.serialize(sCtx_0, el_8, _out_2);
            
        }
        _out_2.writeInt(obj_1.totalRequests);
        _out_2.writeInt(obj_1.packetLossCount);
        SerializerPackNumber.INSTANCE.put(_out_2, obj_1.visibleSensorKeys.length);
        for (const el_9 of obj_1.visibleSensorKeys)  {
            const stringBytes_10 = new TextEncoder().encode(el_9);
            SerializerPackNumber.INSTANCE.put(_out_2, stringBytes_10.length);
            _out_2.write(stringBytes_10);
            
        }
        
    }
    deserialize(sCtx_0: FastFutureContext, in__3: DataIn): AppStateData  {
        let pollingTimeoutMs_12: number;
        let pollingBufferSize_13: number;
        let targetDeviceId_14: number;
        let targetCommutatorUuid_15: string;
        let knownCommutators_16: KnownCommutator[];
        let sensorHistory_17: SensorHistorySeries[];
        let rttMetrics_18: MetricPoint[];
        let totalRequests_19: number;
        let packetLossCount_20: number;
        let visibleSensorKeys_21: string[];
        const _mask = in__3.readByte();
        pollingTimeoutMs_12 = in__3.readInt();
        pollingBufferSize_13 = in__3.readInt();
        targetDeviceId_14 = in__3.readInt();
        if (((_mask & 1) === 0))  {
            let stringBytes_22: Uint8Array;
            const len_24 = Number(DeserializerPackNumber.INSTANCE.put(in__3));
            const bytes_25 = in__3.readBytes(len_24);
            stringBytes_22 = bytes_25;
            targetCommutatorUuid_15 = new TextDecoder('utf-8').decode(stringBytes_22);
            
        }
        else  {
            targetCommutatorUuid_15 = null;
            
        }
        const len_27 = Number(DeserializerPackNumber.INSTANCE.put(in__3));
        knownCommutators_16 = new Array<KnownCommutator>(len_27);
        for (let idx_26 = 0;
        idx_26 < len_27;
        idx_26++)  {
            knownCommutators_16[idx_26] = KnownCommutator.META.deserialize(sCtx_0, in__3);
            
        }
        const len_29 = Number(DeserializerPackNumber.INSTANCE.put(in__3));
        sensorHistory_17 = new Array<SensorHistorySeries>(len_29);
        for (let idx_28 = 0;
        idx_28 < len_29;
        idx_28++)  {
            sensorHistory_17[idx_28] = SensorHistorySeries.META.deserialize(sCtx_0, in__3);
            
        }
        const len_31 = Number(DeserializerPackNumber.INSTANCE.put(in__3));
        rttMetrics_18 = new Array<MetricPoint>(len_31);
        for (let idx_30 = 0;
        idx_30 < len_31;
        idx_30++)  {
            rttMetrics_18[idx_30] = MetricPoint.META.deserialize(sCtx_0, in__3);
            
        }
        totalRequests_19 = in__3.readInt();
        packetLossCount_20 = in__3.readInt();
        const len_33 = Number(DeserializerPackNumber.INSTANCE.put(in__3));
        visibleSensorKeys_21 = new Array<string>(len_33);
        for (let idx_32 = 0;
        idx_32 < len_33;
        idx_32++)  {
            let stringBytes_34: Uint8Array;
            const len_36 = Number(DeserializerPackNumber.INSTANCE.put(in__3));
            const bytes_37 = in__3.readBytes(len_36);
            stringBytes_34 = bytes_37;
            visibleSensorKeys_21[idx_32] = new TextDecoder('utf-8').decode(stringBytes_34);
            
        }
        return new AppStateData(pollingTimeoutMs_12, pollingBufferSize_13, targetDeviceId_14, targetCommutatorUuid_15, knownCommutators_16, sensorHistory_17, rttMetrics_18, totalRequests_19, packetLossCount_20, visibleSensorKeys_21);
        
    }
    metaHashCode(obj: AppStateData | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_INT.metaHashCode(obj.pollingTimeoutMs);
        hash = 37 * hash + FastMeta.META_INT.metaHashCode(obj.pollingBufferSize);
        hash = 37 * hash + FastMeta.META_INT.metaHashCode(obj.targetDeviceId);
        hash = 37 * hash + FastMeta.META_STRING.metaHashCode(obj.targetCommutatorUuid);
        hash = 37 * hash + FastMeta.getMetaArray(KnownCommutator.META).metaHashCode(obj.knownCommutators);
        hash = 37 * hash + FastMeta.getMetaArray(SensorHistorySeries.META).metaHashCode(obj.sensorHistory);
        hash = 37 * hash + FastMeta.getMetaArray(MetricPoint.META).metaHashCode(obj.rttMetrics);
        hash = 37 * hash + FastMeta.META_INT.metaHashCode(obj.totalRequests);
        hash = 37 * hash + FastMeta.META_INT.metaHashCode(obj.packetLossCount);
        hash = 37 * hash + FastMeta.getMetaArray(FastMeta.META_STRING).metaHashCode(obj.visibleSensorKeys);
        return hash | 0;
        
    }
    metaEquals(v1: AppStateData | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof AppStateData)) return false;
        if (!FastMeta.META_INT.metaEquals(v1.pollingTimeoutMs, v2.pollingTimeoutMs)) return false;
        if (!FastMeta.META_INT.metaEquals(v1.pollingBufferSize, v2.pollingBufferSize)) return false;
        if (!FastMeta.META_INT.metaEquals(v1.targetDeviceId, v2.targetDeviceId)) return false;
        if (!FastMeta.META_STRING.metaEquals(v1.targetCommutatorUuid, v2.targetCommutatorUuid)) return false;
        if (!FastMeta.getMetaArray(KnownCommutator.META).metaEquals(v1.knownCommutators, v2.knownCommutators)) return false;
        if (!FastMeta.getMetaArray(SensorHistorySeries.META).metaEquals(v1.sensorHistory, v2.sensorHistory)) return false;
        if (!FastMeta.getMetaArray(MetricPoint.META).metaEquals(v1.rttMetrics, v2.rttMetrics)) return false;
        if (!FastMeta.META_INT.metaEquals(v1.totalRequests, v2.totalRequests)) return false;
        if (!FastMeta.META_INT.metaEquals(v1.packetLossCount, v2.packetLossCount)) return false;
        if (!FastMeta.getMetaArray(FastMeta.META_STRING).metaEquals(v1.visibleSensorKeys, v2.visibleSensorKeys)) return false;
        return true;
        
    }
    metaToString(obj: AppStateData | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('AppStateData(');
        res.add('pollingTimeoutMs:').add(obj.pollingTimeoutMs);
        res.add(', ');
        res.add('pollingBufferSize:').add(obj.pollingBufferSize);
        res.add(', ');
        res.add('targetDeviceId:').add(obj.targetDeviceId);
        res.add(', ');
        res.add('targetCommutatorUuid:').add(obj.targetCommutatorUuid);
        res.add(', ');
        res.add('knownCommutators:').add(obj.knownCommutators);
        res.add(', ');
        res.add('sensorHistory:').add(obj.sensorHistory);
        res.add(', ');
        res.add('rttMetrics:').add(obj.rttMetrics);
        res.add(', ');
        res.add('totalRequests:').add(obj.totalRequests);
        res.add(', ');
        res.add('packetLossCount:').add(obj.packetLossCount);
        res.add(', ');
        res.add('visibleSensorKeys:').add(obj.visibleSensorKeys);
        res.add(')');
        
    }
    public serializeToBytes(obj: AppStateData): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): AppStateData  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): AppStateData  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class GraphPointMetaBodyImpl implements FastMetaType<GraphPoint>  {
    serialize(sCtx_38: FastFutureContext, obj_39: GraphPoint, _out_40: DataOut): void  {
        _out_40.writeLong(obj_39.timestamp);
        _out_40.writeDouble(obj_39.value);
        
    }
    deserialize(sCtx_38: FastFutureContext, in__41: DataIn): GraphPoint  {
        let timestamp_42: bigint;
        let value_43: number;
        timestamp_42 = in__41.readLong();
        value_43 = in__41.readDouble();
        return new GraphPoint(timestamp_42, value_43);
        
    }
    metaHashCode(obj: GraphPoint | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_LONG.metaHashCode(obj.timestamp);
        hash = 37 * hash + FastMeta.META_DOUBLE.metaHashCode(obj.value);
        return hash | 0;
        
    }
    metaEquals(v1: GraphPoint | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof GraphPoint)) return false;
        if (!FastMeta.META_LONG.metaEquals(v1.timestamp, v2.timestamp)) return false;
        if (!FastMeta.META_DOUBLE.metaEquals(v1.value, v2.value)) return false;
        return true;
        
    }
    metaToString(obj: GraphPoint | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('GraphPoint(');
        res.add('timestamp:').add(obj.timestamp);
        res.add(', ');
        res.add('value:').add(obj.value);
        res.add(')');
        
    }
    public serializeToBytes(obj: GraphPoint): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): GraphPoint  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): GraphPoint  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class KnownCommutatorMetaBodyImpl implements FastMetaType<KnownCommutator>  {
    serialize(sCtx_44: FastFutureContext, obj_45: KnownCommutator, _out_46: DataOut): void  {
        let _mask: number = 0;
        if (obj_45.label === null) _mask |= 1;
        _out_46.writeByte(_mask);
        const stringBytes_48 = new TextEncoder().encode(obj_45.uuid);
        SerializerPackNumber.INSTANCE.put(_out_46, stringBytes_48.length);
        _out_46.write(stringBytes_48);
        if (obj_45.label !== null)  {
            const stringBytes_50 = new TextEncoder().encode(obj_45.label);
            SerializerPackNumber.INSTANCE.put(_out_46, stringBytes_50.length);
            _out_46.write(stringBytes_50);
            
        }
        _out_46.writeLong(obj_45.lastSeen);
        _out_46.writeInt(obj_45.totalRequests);
        _out_46.writeInt(obj_45.totalResponses);
        
    }
    deserialize(sCtx_44: FastFutureContext, in__47: DataIn): KnownCommutator  {
        let uuid_52: string;
        let label_53: string;
        let lastSeen_54: bigint;
        let totalRequests_55: number;
        let totalResponses_56: number;
        const _mask = in__47.readByte();
        let stringBytes_57: Uint8Array;
        const len_59 = Number(DeserializerPackNumber.INSTANCE.put(in__47));
        const bytes_60 = in__47.readBytes(len_59);
        stringBytes_57 = bytes_60;
        uuid_52 = new TextDecoder('utf-8').decode(stringBytes_57);
        if (((_mask & 1) === 0))  {
            let stringBytes_61: Uint8Array;
            const len_63 = Number(DeserializerPackNumber.INSTANCE.put(in__47));
            const bytes_64 = in__47.readBytes(len_63);
            stringBytes_61 = bytes_64;
            label_53 = new TextDecoder('utf-8').decode(stringBytes_61);
            
        }
        else  {
            label_53 = null;
            
        }
        lastSeen_54 = in__47.readLong();
        totalRequests_55 = in__47.readInt();
        totalResponses_56 = in__47.readInt();
        return new KnownCommutator(uuid_52, label_53, lastSeen_54, totalRequests_55, totalResponses_56);
        
    }
    metaHashCode(obj: KnownCommutator | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_STRING.metaHashCode(obj.uuid);
        hash = 37 * hash + FastMeta.META_STRING.metaHashCode(obj.label);
        hash = 37 * hash + FastMeta.META_LONG.metaHashCode(obj.lastSeen);
        hash = 37 * hash + FastMeta.META_INT.metaHashCode(obj.totalRequests);
        hash = 37 * hash + FastMeta.META_INT.metaHashCode(obj.totalResponses);
        return hash | 0;
        
    }
    metaEquals(v1: KnownCommutator | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof KnownCommutator)) return false;
        if (!FastMeta.META_STRING.metaEquals(v1.uuid, v2.uuid)) return false;
        if (!FastMeta.META_STRING.metaEquals(v1.label, v2.label)) return false;
        if (!FastMeta.META_LONG.metaEquals(v1.lastSeen, v2.lastSeen)) return false;
        if (!FastMeta.META_INT.metaEquals(v1.totalRequests, v2.totalRequests)) return false;
        if (!FastMeta.META_INT.metaEquals(v1.totalResponses, v2.totalResponses)) return false;
        return true;
        
    }
    metaToString(obj: KnownCommutator | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('KnownCommutator(');
        res.add('uuid:').add(obj.uuid);
        res.add(', ');
        res.add('label:').add(obj.label);
        res.add(', ');
        res.add('lastSeen:').add(obj.lastSeen);
        res.add(', ');
        res.add('totalRequests:').add(obj.totalRequests);
        res.add(', ');
        res.add('totalResponses:').add(obj.totalResponses);
        res.add(')');
        
    }
    public serializeToBytes(obj: KnownCommutator): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): KnownCommutator  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): KnownCommutator  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class MetricPointMetaBodyImpl implements FastMetaType<MetricPoint>  {
    serialize(sCtx_65: FastFutureContext, obj_66: MetricPoint, _out_67: DataOut): void  {
        _out_67.writeLong(obj_66.timestamp);
        _out_67.writeInt(obj_66.rtt);
        
    }
    deserialize(sCtx_65: FastFutureContext, in__68: DataIn): MetricPoint  {
        let timestamp_69: bigint;
        let rtt_70: number;
        timestamp_69 = in__68.readLong();
        rtt_70 = in__68.readInt();
        return new MetricPoint(timestamp_69, rtt_70);
        
    }
    metaHashCode(obj: MetricPoint | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_LONG.metaHashCode(obj.timestamp);
        hash = 37 * hash + FastMeta.META_INT.metaHashCode(obj.rtt);
        return hash | 0;
        
    }
    metaEquals(v1: MetricPoint | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof MetricPoint)) return false;
        if (!FastMeta.META_LONG.metaEquals(v1.timestamp, v2.timestamp)) return false;
        if (!FastMeta.META_INT.metaEquals(v1.rtt, v2.rtt)) return false;
        return true;
        
    }
    metaToString(obj: MetricPoint | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('MetricPoint(');
        res.add('timestamp:').add(obj.timestamp);
        res.add(', ');
        res.add('rtt:').add(obj.rtt);
        res.add(')');
        
    }
    public serializeToBytes(obj: MetricPoint): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): MetricPoint  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): MetricPoint  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class RecordMetaBodyImpl implements FastMetaType<Record>  {
    serialize(sCtx_71: FastFutureContext, obj_72: Record, _out_73: DataOut): void  {
        _out_73.writeByte(obj_72.value);
        _out_73.writeByte(obj_72.time);
        
    }
    deserialize(sCtx_71: FastFutureContext, in__74: DataIn): Record  {
        let value_75: number;
        let time_76: number;
        value_75 = in__74.readByte();
        time_76 = in__74.readByte();
        return new Record(value_75, time_76);
        
    }
    metaHashCode(obj: Record | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_BYTE.metaHashCode(obj.value);
        hash = 37 * hash + FastMeta.META_BYTE.metaHashCode(obj.time);
        return hash | 0;
        
    }
    metaEquals(v1: Record | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof Record)) return false;
        if (!FastMeta.META_BYTE.metaEquals(v1.value, v2.value)) return false;
        if (!FastMeta.META_BYTE.metaEquals(v1.time, v2.time)) return false;
        return true;
        
    }
    metaToString(obj: Record | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('Record(');
        res.add('value:').add(obj.value);
        res.add(', ');
        res.add('time:').add(obj.time);
        res.add(')');
        
    }
    public serializeToBytes(obj: Record): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): Record  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): Record  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class SensorHistorySeriesMetaBodyImpl implements FastMetaType<SensorHistorySeries>  {
    serialize(sCtx_77: FastFutureContext, obj_78: SensorHistorySeries, _out_79: DataOut): void  {
        let _mask: number = 0;
        if (obj_78.unit === null) _mask |= 1;
        _out_79.writeByte(_mask);
        const stringBytes_81 = new TextEncoder().encode(obj_78.commutatorUuid);
        SerializerPackNumber.INSTANCE.put(_out_79, stringBytes_81.length);
        _out_79.write(stringBytes_81);
        _out_79.writeInt(obj_78.deviceId);
        const stringBytes_83 = new TextEncoder().encode(obj_78.deviceName);
        SerializerPackNumber.INSTANCE.put(_out_79, stringBytes_83.length);
        _out_79.write(stringBytes_83);
        if (obj_78.unit !== null)  {
            const stringBytes_85 = new TextEncoder().encode(obj_78.unit);
            SerializerPackNumber.INSTANCE.put(_out_79, stringBytes_85.length);
            _out_79.write(stringBytes_85);
            
        }
        SerializerPackNumber.INSTANCE.put(_out_79, obj_78.points.length);
        for (const el_87 of obj_78.points)  {
            GraphPoint.META.serialize(sCtx_77, el_87, _out_79);
            
        }
        
    }
    deserialize(sCtx_77: FastFutureContext, in__80: DataIn): SensorHistorySeries  {
        let commutatorUuid_88: string;
        let deviceId_89: number;
        let deviceName_90: string;
        let unit_91: string;
        let points_92: GraphPoint[];
        const _mask = in__80.readByte();
        let stringBytes_93: Uint8Array;
        const len_95 = Number(DeserializerPackNumber.INSTANCE.put(in__80));
        const bytes_96 = in__80.readBytes(len_95);
        stringBytes_93 = bytes_96;
        commutatorUuid_88 = new TextDecoder('utf-8').decode(stringBytes_93);
        deviceId_89 = in__80.readInt();
        let stringBytes_97: Uint8Array;
        const len_99 = Number(DeserializerPackNumber.INSTANCE.put(in__80));
        const bytes_100 = in__80.readBytes(len_99);
        stringBytes_97 = bytes_100;
        deviceName_90 = new TextDecoder('utf-8').decode(stringBytes_97);
        if (((_mask & 1) === 0))  {
            let stringBytes_101: Uint8Array;
            const len_103 = Number(DeserializerPackNumber.INSTANCE.put(in__80));
            const bytes_104 = in__80.readBytes(len_103);
            stringBytes_101 = bytes_104;
            unit_91 = new TextDecoder('utf-8').decode(stringBytes_101);
            
        }
        else  {
            unit_91 = null;
            
        }
        const len_106 = Number(DeserializerPackNumber.INSTANCE.put(in__80));
        points_92 = new Array<GraphPoint>(len_106);
        for (let idx_105 = 0;
        idx_105 < len_106;
        idx_105++)  {
            points_92[idx_105] = GraphPoint.META.deserialize(sCtx_77, in__80);
            
        }
        return new SensorHistorySeries(commutatorUuid_88, deviceId_89, deviceName_90, unit_91, points_92);
        
    }
    metaHashCode(obj: SensorHistorySeries | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_STRING.metaHashCode(obj.commutatorUuid);
        hash = 37 * hash + FastMeta.META_INT.metaHashCode(obj.deviceId);
        hash = 37 * hash + FastMeta.META_STRING.metaHashCode(obj.deviceName);
        hash = 37 * hash + FastMeta.META_STRING.metaHashCode(obj.unit);
        hash = 37 * hash + FastMeta.getMetaArray(GraphPoint.META).metaHashCode(obj.points);
        return hash | 0;
        
    }
    metaEquals(v1: SensorHistorySeries | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof SensorHistorySeries)) return false;
        if (!FastMeta.META_STRING.metaEquals(v1.commutatorUuid, v2.commutatorUuid)) return false;
        if (!FastMeta.META_INT.metaEquals(v1.deviceId, v2.deviceId)) return false;
        if (!FastMeta.META_STRING.metaEquals(v1.deviceName, v2.deviceName)) return false;
        if (!FastMeta.META_STRING.metaEquals(v1.unit, v2.unit)) return false;
        if (!FastMeta.getMetaArray(GraphPoint.META).metaEquals(v1.points, v2.points)) return false;
        return true;
        
    }
    metaToString(obj: SensorHistorySeries | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('SensorHistorySeries(');
        res.add('commutatorUuid:').add(obj.commutatorUuid);
        res.add(', ');
        res.add('deviceId:').add(obj.deviceId);
        res.add(', ');
        res.add('deviceName:').add(obj.deviceName);
        res.add(', ');
        res.add('unit:').add(obj.unit);
        res.add(', ');
        res.add('points:').add(obj.points);
        res.add(')');
        
    }
    public serializeToBytes(obj: SensorHistorySeries): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): SensorHistorySeries  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): SensorHistorySeries  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class SimpleDeviceApiMetaImpl implements FastMetaApi<SimpleDeviceApi, SimpleDeviceApiRemote>  {
    makeLocal_fromDataIn(ctx: FastFutureContext, dataIn: DataIn, localApi: SimpleDeviceApi): void  {
        while(dataIn.isReadable())  {
            const commandId = dataIn.readUByte();
            switch(commandId)  {
                case 0:  {
                    const reqId = FastMeta.META_REQUEST_ID.deserialize(ctx, dataIn);
                    const futureRec = ctx.getFuture(reqId);
                    if (futureRec) futureRec.onDone(dataIn);
                    break;
                    
                }
                case 1:  {
                    const reqId = FastMeta.META_REQUEST_ID.deserialize(ctx, dataIn);
                    const futureRec = ctx.getFuture(reqId);
                    if (futureRec) futureRec.onError(dataIn);
                    break;
                    
                }
                case 3:  {
                    let count_108: number;
                    count_108 = dataIn.readShort();
                    const argsNames_109: string[] = ["count"];
                    const argsValues_110: any[] = [count_108];
                    ctx.invokeLocalMethodBefore("requestRecords", argsNames_109, argsValues_110);
                    localApi.requestRecords(count_108);
                    ctx.invokeLocalMethodAfter("requestRecords", null, argsNames_109, argsValues_110);
                    break;
                    
                }
                default: throw new Error(`Unknown command ID: $ {
                    commandId
                }
                `);
                
            }
        }
        
    }
    makeLocal_fromBytes_ctxLocal(ctx: FastApiContextLocal<SimpleDeviceApi>, data: Uint8Array): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), ctx.localApi);
        
    }
    makeLocal_fromBytes_ctx(ctx: FastFutureContext, data: Uint8Array, localApi: SimpleDeviceApi): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), localApi);
        
    }
    makeRemote(sCtx_111: FastFutureContext): SimpleDeviceApiRemote  {
        const remoteApiImpl =  {
            flush: (sendFuture?: AFuture): AFuture =>  {
                const futureToUse = sendFuture || AFuture.make();
                sCtx_111.flush(futureToUse);
                return futureToUse;
                
            }
            , getFastMetaContext: () => sCtx_111, requestRecords: (count: number): void =>  {
                const dataOut_113 = new DataInOut();
                dataOut_113.writeByte(3);
                const argsNames_115: string[] = ["count"];
                const argsValues_116: any[] = [count];
                sCtx_111.invokeRemoteMethodAfter("requestRecords", null, argsNames_115, argsValues_116);
                dataOut_113.writeShort(count);
                sCtx_111.sendToRemote(dataOut_113.toArray());
                
            }
            , 
        };
        return remoteApiImpl as SimpleDeviceApiRemote;
        
    }
    
}
export class SimpleClientApiMetaImpl implements FastMetaApi<SimpleClientApi, SimpleClientApiRemote>  {
    makeLocal_fromDataIn(ctx: FastFutureContext, dataIn: DataIn, localApi: SimpleClientApi): void  {
        while(dataIn.isReadable())  {
            const commandId = dataIn.readUByte();
            switch(commandId)  {
                case 0:  {
                    const reqId = FastMeta.META_REQUEST_ID.deserialize(ctx, dataIn);
                    const futureRec = ctx.getFuture(reqId);
                    if (futureRec) futureRec.onDone(dataIn);
                    break;
                    
                }
                case 1:  {
                    const reqId = FastMeta.META_REQUEST_ID.deserialize(ctx, dataIn);
                    const futureRec = ctx.getFuture(reqId);
                    if (futureRec) futureRec.onError(dataIn);
                    break;
                    
                }
                case 3:  {
                    let value_118: Record[];
                    const len_120 = Number(DeserializerPackNumber.INSTANCE.put(dataIn));
                    value_118 = new Array<Record>(len_120);
                    for (let idx_119 = 0;
                    idx_119 < len_120;
                    idx_119++)  {
                        value_118[idx_119] = Record.META.deserialize(ctx, dataIn);
                        
                    }
                    const argsNames_121: string[] = ["value"];
                    const argsValues_122: any[] = [value_118];
                    ctx.invokeLocalMethodBefore("receiveStatus", argsNames_121, argsValues_122);
                    localApi.receiveStatus(value_118);
                    ctx.invokeLocalMethodAfter("receiveStatus", null, argsNames_121, argsValues_122);
                    break;
                    
                }
                default: throw new Error(`Unknown command ID: $ {
                    commandId
                }
                `);
                
            }
        }
        
    }
    makeLocal_fromBytes_ctxLocal(ctx: FastApiContextLocal<SimpleClientApi>, data: Uint8Array): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), ctx.localApi);
        
    }
    makeLocal_fromBytes_ctx(ctx: FastFutureContext, data: Uint8Array, localApi: SimpleClientApi): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), localApi);
        
    }
    makeRemote(sCtx_123: FastFutureContext): SimpleClientApiRemote  {
        const remoteApiImpl =  {
            flush: (sendFuture?: AFuture): AFuture =>  {
                const futureToUse = sendFuture || AFuture.make();
                sCtx_123.flush(futureToUse);
                return futureToUse;
                
            }
            , getFastMetaContext: () => sCtx_123, receiveStatus: (value: Record[]): void =>  {
                const dataOut_125 = new DataInOut();
                dataOut_125.writeByte(3);
                const argsNames_127: string[] = ["value"];
                const argsValues_128: any[] = [value];
                sCtx_123.invokeRemoteMethodAfter("receiveStatus", null, argsNames_127, argsValues_128);
                SerializerPackNumber.INSTANCE.put(dataOut_125, value.length);
                for (const el_129 of value)  {
                    Record.META.serialize(sCtx_123, el_129, dataOut_125);
                    
                }
                sCtx_123.sendToRemote(dataOut_125.toArray());
                
            }
            , 
        };
        return remoteApiImpl as SimpleClientApiRemote;
        
    }
    
}