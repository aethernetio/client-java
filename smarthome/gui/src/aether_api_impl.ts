import  {
    AFuture, ARFuture, DataIn, DataOut, DataInOut, DataInOutStatic, FastMetaType, FastFutureContext, RemoteApi, FastMeta, SerializerPackNumber, DeserializerPackNumber, FastApiContextLocal, FastMetaApi, BytesConverter, RemoteApiFuture, FastFutureContextStub, UUID, URI, AConsumer, ToString, AString
}
from 'aether-client';
import  {
    HardwareDevice, VariantData, AppStateData, DeviceStateData, GraphPoint, HardwareActor, HardwareSensor, KnownCommutator, MetricPoint, SensorHistorySeries, VariantBool, VariantBytes, VariantDouble, VariantLong, VariantString, SmartHomeClientStream, SmartHomeCommutatorStream, SmartHomeCommutatorApi, SmartHomeClientApi, SmartHomeCommutatorApiRemote, SmartHomeClientApiRemote
}
from './aether_api';
// This is always relative
export class HardwareDeviceMetaImpl implements FastMetaType<HardwareDevice>  {
    serialize(sCtx_0: FastFutureContext, obj_1: HardwareDevice, _out_2: DataOut): void  {
        const typeId = typeof (obj_1 as any).getAetherTypeId === 'function' ? obj_1.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'HardwareDevice' with invalid type id $ {
            typeId
        }
        `);
        _out_2.writeByte(typeId);
        switch(typeId)  {
            case 1: (HardwareSensor as any).META_BODY.serialize(sCtx_0, obj_1 as any as HardwareSensor, _out_2);
            break;
            case 2: (HardwareActor as any).META_BODY.serialize(sCtx_0, obj_1 as any as HardwareActor, _out_2);
            break;
            default: throw new Error(`Cannot serialize 'HardwareDevice' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_0: FastFutureContext, in__3: DataIn): HardwareDevice  {
        const typeId = in__3.readUByte();
        switch(typeId)  {
            case 1: return (HardwareSensor as any).META_BODY.deserialize(sCtx_0, in__3) as any as HardwareDevice;
            case 2: return (HardwareActor as any).META_BODY.deserialize(sCtx_0, in__3) as any as HardwareDevice;
            default: throw new Error(`Bad type id $ {
                typeId
            }
            for type 'HardwareDevice'`);
            
        }
        
    }
    metaHashCode(obj: HardwareDevice | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 1: return (HardwareSensor as any).META.metaHashCode(obj as any as HardwareSensor);
            case 2: return (HardwareActor as any).META.metaHashCode(obj as any as HardwareActor);
            default: throw new Error(`Cannot hashCode 'HardwareDevice' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    metaEquals(v1: HardwareDevice | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined) return false;
        const typeId1 = (v1 as any).getAetherTypeId ? (v1 as any).getAetherTypeId() : -1;
        const typeId2 = (v2 as any).getAetherTypeId ? (v2 as any).getAetherTypeId() : -1;
        if (typeId1 === -1 || typeId1 !== typeId2) return false;
        switch(typeId1)  {
            case 1: return (HardwareSensor as any).META.metaEquals(v1 as any as HardwareSensor, v2);
            case 2: return (HardwareActor as any).META.metaEquals(v1 as any as HardwareActor, v2);
            default: throw new Error(`Cannot equals 'HardwareDevice' with unknown type id $ {
                typeId1
            }
            `);
            
        }
        
    }
    metaToString(obj: HardwareDevice | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return ;
            
        }
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 1: (HardwareSensor as any).META.metaToString(obj as any as HardwareSensor, res);
            break;
            case 2: (HardwareActor as any).META.metaToString(obj as any as HardwareActor, res);
            break;
            default: throw new Error(`Cannot toString 'HardwareDevice' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    public serializeToBytes(obj: HardwareDevice): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): HardwareDevice  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): HardwareDevice  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class VariantDataMetaImpl implements FastMetaType<VariantData>  {
    serialize(sCtx_4: FastFutureContext, obj_5: VariantData, _out_6: DataOut): void  {
        const typeId = typeof (obj_5 as any).getAetherTypeId === 'function' ? obj_5.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'VariantData' with invalid type id $ {
            typeId
        }
        `);
        _out_6.writeByte(typeId);
        switch(typeId)  {
            case 1: (VariantBool as any).META_BODY.serialize(sCtx_4, obj_5 as any as VariantBool, _out_6);
            break;
            case 2: (VariantLong as any).META_BODY.serialize(sCtx_4, obj_5 as any as VariantLong, _out_6);
            break;
            case 3: (VariantDouble as any).META_BODY.serialize(sCtx_4, obj_5 as any as VariantDouble, _out_6);
            break;
            case 4: (VariantString as any).META_BODY.serialize(sCtx_4, obj_5 as any as VariantString, _out_6);
            break;
            case 5: (VariantBytes as any).META_BODY.serialize(sCtx_4, obj_5 as any as VariantBytes, _out_6);
            break;
            default: throw new Error(`Cannot serialize 'VariantData' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_4: FastFutureContext, in__7: DataIn): VariantData  {
        const typeId = in__7.readUByte();
        switch(typeId)  {
            case 1: return (VariantBool as any).META_BODY.deserialize(sCtx_4, in__7) as any as VariantData;
            case 2: return (VariantLong as any).META_BODY.deserialize(sCtx_4, in__7) as any as VariantData;
            case 3: return (VariantDouble as any).META_BODY.deserialize(sCtx_4, in__7) as any as VariantData;
            case 4: return (VariantString as any).META_BODY.deserialize(sCtx_4, in__7) as any as VariantData;
            case 5: return (VariantBytes as any).META_BODY.deserialize(sCtx_4, in__7) as any as VariantData;
            default: throw new Error(`Bad type id $ {
                typeId
            }
            for type 'VariantData'`);
            
        }
        
    }
    metaHashCode(obj: VariantData | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 1: return (VariantBool as any).META.metaHashCode(obj as any as VariantBool);
            case 2: return (VariantLong as any).META.metaHashCode(obj as any as VariantLong);
            case 3: return (VariantDouble as any).META.metaHashCode(obj as any as VariantDouble);
            case 4: return (VariantString as any).META.metaHashCode(obj as any as VariantString);
            case 5: return (VariantBytes as any).META.metaHashCode(obj as any as VariantBytes);
            default: throw new Error(`Cannot hashCode 'VariantData' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    metaEquals(v1: VariantData | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined) return false;
        const typeId1 = (v1 as any).getAetherTypeId ? (v1 as any).getAetherTypeId() : -1;
        const typeId2 = (v2 as any).getAetherTypeId ? (v2 as any).getAetherTypeId() : -1;
        if (typeId1 === -1 || typeId1 !== typeId2) return false;
        switch(typeId1)  {
            case 1: return (VariantBool as any).META.metaEquals(v1 as any as VariantBool, v2);
            case 2: return (VariantLong as any).META.metaEquals(v1 as any as VariantLong, v2);
            case 3: return (VariantDouble as any).META.metaEquals(v1 as any as VariantDouble, v2);
            case 4: return (VariantString as any).META.metaEquals(v1 as any as VariantString, v2);
            case 5: return (VariantBytes as any).META.metaEquals(v1 as any as VariantBytes, v2);
            default: throw new Error(`Cannot equals 'VariantData' with unknown type id $ {
                typeId1
            }
            `);
            
        }
        
    }
    metaToString(obj: VariantData | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return ;
            
        }
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 1: (VariantBool as any).META.metaToString(obj as any as VariantBool, res);
            break;
            case 2: (VariantLong as any).META.metaToString(obj as any as VariantLong, res);
            break;
            case 3: (VariantDouble as any).META.metaToString(obj as any as VariantDouble, res);
            break;
            case 4: (VariantString as any).META.metaToString(obj as any as VariantString, res);
            break;
            case 5: (VariantBytes as any).META.metaToString(obj as any as VariantBytes, res);
            break;
            default: throw new Error(`Cannot toString 'VariantData' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    public serializeToBytes(obj: VariantData): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): VariantData  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): VariantData  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class AppStateDataMetaBodyImpl implements FastMetaType<AppStateData>  {
    serialize(sCtx_8: FastFutureContext, obj_9: AppStateData, _out_10: DataOut): void  {
        let _mask: number = 0;
        if (obj_9.targetCommutatorUuid === null) _mask |= 1;
        _out_10.writeByte(_mask);
        _out_10.writeInt(obj_9.pollingTimeoutMs);
        _out_10.writeInt(obj_9.pollingBufferSize);
        _out_10.writeInt(obj_9.targetDeviceId);
        if (obj_9.targetCommutatorUuid !== null)  {
            const stringBytes_12 = new TextEncoder().encode(obj_9.targetCommutatorUuid);
            SerializerPackNumber.INSTANCE.put(_out_10, stringBytes_12.length);
            _out_10.write(stringBytes_12);
            
        }
        SerializerPackNumber.INSTANCE.put(_out_10, obj_9.knownCommutators.length);
        for (const el_14 of obj_9.knownCommutators)  {
            KnownCommutator.META.serialize(sCtx_8, el_14, _out_10);
            
        }
        SerializerPackNumber.INSTANCE.put(_out_10, obj_9.sensorHistory.length);
        for (const el_15 of obj_9.sensorHistory)  {
            SensorHistorySeries.META.serialize(sCtx_8, el_15, _out_10);
            
        }
        SerializerPackNumber.INSTANCE.put(_out_10, obj_9.rttMetrics.length);
        for (const el_16 of obj_9.rttMetrics)  {
            MetricPoint.META.serialize(sCtx_8, el_16, _out_10);
            
        }
        _out_10.writeInt(obj_9.totalRequests);
        _out_10.writeInt(obj_9.packetLossCount);
        SerializerPackNumber.INSTANCE.put(_out_10, obj_9.visibleSensorKeys.length);
        for (const el_17 of obj_9.visibleSensorKeys)  {
            const stringBytes_18 = new TextEncoder().encode(el_17);
            SerializerPackNumber.INSTANCE.put(_out_10, stringBytes_18.length);
            _out_10.write(stringBytes_18);
            
        }
        
    }
    deserialize(sCtx_8: FastFutureContext, in__11: DataIn): AppStateData  {
        let pollingTimeoutMs_20: number;
        let pollingBufferSize_21: number;
        let targetDeviceId_22: number;
        let targetCommutatorUuid_23: string;
        let knownCommutators_24: KnownCommutator[];
        let sensorHistory_25: SensorHistorySeries[];
        let rttMetrics_26: MetricPoint[];
        let totalRequests_27: number;
        let packetLossCount_28: number;
        let visibleSensorKeys_29: string[];
        const _mask = in__11.readByte();
        pollingTimeoutMs_20 = in__11.readInt();
        pollingBufferSize_21 = in__11.readInt();
        targetDeviceId_22 = in__11.readInt();
        if (((_mask & 1) === 0))  {
            let stringBytes_30: Uint8Array;
            const len_32 = Number(DeserializerPackNumber.INSTANCE.put(in__11));
            const bytes_33 = in__11.readBytes(len_32);
            stringBytes_30 = bytes_33;
            targetCommutatorUuid_23 = new TextDecoder('utf-8').decode(stringBytes_30);
            
        }
        else  {
            targetCommutatorUuid_23 = null;
            
        }
        const len_35 = Number(DeserializerPackNumber.INSTANCE.put(in__11));
        knownCommutators_24 = new Array<KnownCommutator>(len_35);
        for (let idx_34 = 0;
        idx_34 < len_35;
        idx_34++)  {
            knownCommutators_24[idx_34] = KnownCommutator.META.deserialize(sCtx_8, in__11);
            
        }
        const len_37 = Number(DeserializerPackNumber.INSTANCE.put(in__11));
        sensorHistory_25 = new Array<SensorHistorySeries>(len_37);
        for (let idx_36 = 0;
        idx_36 < len_37;
        idx_36++)  {
            sensorHistory_25[idx_36] = SensorHistorySeries.META.deserialize(sCtx_8, in__11);
            
        }
        const len_39 = Number(DeserializerPackNumber.INSTANCE.put(in__11));
        rttMetrics_26 = new Array<MetricPoint>(len_39);
        for (let idx_38 = 0;
        idx_38 < len_39;
        idx_38++)  {
            rttMetrics_26[idx_38] = MetricPoint.META.deserialize(sCtx_8, in__11);
            
        }
        totalRequests_27 = in__11.readInt();
        packetLossCount_28 = in__11.readInt();
        const len_41 = Number(DeserializerPackNumber.INSTANCE.put(in__11));
        visibleSensorKeys_29 = new Array<string>(len_41);
        for (let idx_40 = 0;
        idx_40 < len_41;
        idx_40++)  {
            let stringBytes_42: Uint8Array;
            const len_44 = Number(DeserializerPackNumber.INSTANCE.put(in__11));
            const bytes_45 = in__11.readBytes(len_44);
            stringBytes_42 = bytes_45;
            visibleSensorKeys_29[idx_40] = new TextDecoder('utf-8').decode(stringBytes_42);
            
        }
        return new AppStateData(pollingTimeoutMs_20, pollingBufferSize_21, targetDeviceId_22, targetCommutatorUuid_23, knownCommutators_24, sensorHistory_25, rttMetrics_26, totalRequests_27, packetLossCount_28, visibleSensorKeys_29);
        
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
export class DeviceStateDataMetaBodyImpl implements FastMetaType<DeviceStateData>  {
    serialize(sCtx_46: FastFutureContext, obj_47: DeviceStateData, _out_48: DataOut): void  {
        VariantData.META.serialize(sCtx_46, obj_47.payload, _out_48);
        _out_48.writeLong(obj_47.timestamp.getTime());
        
    }
    deserialize(sCtx_46: FastFutureContext, in__49: DataIn): DeviceStateData  {
        let payload_50: VariantData;
        let timestamp_51: Date;
        payload_50 = VariantData.META.deserialize(sCtx_46, in__49);
        timestamp_51 = new Date(Number(in__49.readLong()));
        return new DeviceStateData(payload_50, timestamp_51);
        
    }
    metaHashCode(obj: DeviceStateData | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + VariantData.META.metaHashCode(obj.payload);
        hash = 37 * hash + FastMeta.META_DATE.metaHashCode(obj.timestamp);
        return hash | 0;
        
    }
    metaEquals(v1: DeviceStateData | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof DeviceStateData)) return false;
        if (!VariantData.META.metaEquals(v1.payload, v2.payload)) return false;
        if (!FastMeta.META_DATE.metaEquals(v1.timestamp, v2.timestamp)) return false;
        return true;
        
    }
    metaToString(obj: DeviceStateData | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('DeviceStateData(');
        res.add('payload:').add(obj.payload);
        res.add(', ');
        res.add('timestamp:').add(obj.timestamp);
        res.add(')');
        
    }
    public serializeToBytes(obj: DeviceStateData): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): DeviceStateData  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): DeviceStateData  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class GraphPointMetaBodyImpl implements FastMetaType<GraphPoint>  {
    serialize(sCtx_52: FastFutureContext, obj_53: GraphPoint, _out_54: DataOut): void  {
        _out_54.writeLong(obj_53.timestamp);
        _out_54.writeDouble(obj_53.value);
        
    }
    deserialize(sCtx_52: FastFutureContext, in__55: DataIn): GraphPoint  {
        let timestamp_56: bigint;
        let value_57: number;
        timestamp_56 = in__55.readLong();
        value_57 = in__55.readDouble();
        return new GraphPoint(timestamp_56, value_57);
        
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
export class HardwareActorMetaBodyImpl implements FastMetaType<HardwareActor>  {
    serialize(sCtx_58: FastFutureContext, obj_59: HardwareActor, _out_60: DataOut): void  {
        _out_60.writeInt(obj_59.localId);
        const stringBytes_62 = new TextEncoder().encode(obj_59.descriptor);
        SerializerPackNumber.INSTANCE.put(_out_60, stringBytes_62.length);
        _out_60.write(stringBytes_62);
        
    }
    deserialize(sCtx_58: FastFutureContext, in__61: DataIn): HardwareActor  {
        let localId_64: number;
        let descriptor_65: string;
        localId_64 = in__61.readInt();
        let stringBytes_66: Uint8Array;
        const len_68 = Number(DeserializerPackNumber.INSTANCE.put(in__61));
        const bytes_69 = in__61.readBytes(len_68);
        stringBytes_66 = bytes_69;
        descriptor_65 = new TextDecoder('utf-8').decode(stringBytes_66);
        return new HardwareActor(localId_64, descriptor_65);
        
    }
    metaHashCode(obj: HardwareActor | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_INT.metaHashCode(obj.localId);
        hash = 37 * hash + FastMeta.META_STRING.metaHashCode(obj.descriptor);
        return hash | 0;
        
    }
    metaEquals(v1: HardwareActor | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof HardwareActor)) return false;
        if (!FastMeta.META_INT.metaEquals(v1.localId, v2.localId)) return false;
        if (!FastMeta.META_STRING.metaEquals(v1.descriptor, v2.descriptor)) return false;
        return true;
        
    }
    metaToString(obj: HardwareActor | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('HardwareActor(');
        res.add('localId:').add(obj.localId);
        res.add(', ');
        res.add('descriptor:').add(obj.descriptor);
        res.add(', ');
        res.add('hardwareType:').add(obj.getHardwareType());
        res.add(')');
        
    }
    public serializeToBytes(obj: HardwareActor): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): HardwareActor  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): HardwareActor  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class HardwareActorMetaImpl implements FastMetaType<HardwareActor>  {
    serialize(sCtx_70: FastFutureContext, obj_71: HardwareActor, _out_72: DataOut): void  {
        const typeId = typeof (obj_71 as any).getAetherTypeId === 'function' ? obj_71.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'HardwareActor' with invalid type id $ {
            typeId
        }
        `);
        _out_72.writeByte(typeId);
        switch(typeId)  {
            case 2: (HardwareActor as any).META_BODY.serialize(sCtx_70, obj_71 as any as HardwareActor, _out_72);
            break;
            case 1: (HardwareSensor as any).META_BODY.serialize(sCtx_70, obj_71 as any as HardwareSensor, _out_72);
            break;
            default: throw new Error(`Cannot serialize 'HardwareActor' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_70: FastFutureContext, in__73: DataIn): HardwareActor  {
        const typeId = in__73.readUByte();
        switch(typeId)  {
            case 2: return (HardwareActor as any).META_BODY.deserialize(sCtx_70, in__73) as any as HardwareActor;
            case 1: return (HardwareSensor as any).META_BODY.deserialize(sCtx_70, in__73) as any as HardwareActor;
            default: throw new Error(`Bad type id $ {
                typeId
            }
            for type 'HardwareActor'`);
            
        }
        
    }
    metaHashCode(obj: HardwareActor | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 2: return (HardwareActor as any).META_BODY.metaHashCode(obj as any as HardwareActor);
            case 1: return (HardwareSensor as any).META.metaHashCode(obj as any as HardwareSensor);
            default: throw new Error(`Cannot hashCode 'HardwareActor' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    metaEquals(v1: HardwareActor | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined) return false;
        const typeId1 = (v1 as any).getAetherTypeId ? (v1 as any).getAetherTypeId() : -1;
        const typeId2 = (v2 as any).getAetherTypeId ? (v2 as any).getAetherTypeId() : -1;
        if (typeId1 === -1 || typeId1 !== typeId2) return false;
        switch(typeId1)  {
            case 2: return (HardwareActor as any).META_BODY.metaEquals(v1 as any as HardwareActor, v2);
            case 1: return (HardwareSensor as any).META.metaEquals(v1 as any as HardwareSensor, v2);
            default: throw new Error(`Cannot equals 'HardwareActor' with unknown type id $ {
                typeId1
            }
            `);
            
        }
        
    }
    metaToString(obj: HardwareActor | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return ;
            
        }
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 2: (HardwareActor as any).META_BODY.metaToString(obj as any as HardwareActor, res);
            break;
            case 1: (HardwareSensor as any).META.metaToString(obj as any as HardwareSensor, res);
            break;
            default: throw new Error(`Cannot toString 'HardwareActor' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    public serializeToBytes(obj: HardwareActor): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): HardwareActor  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): HardwareActor  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class HardwareSensorMetaBodyImpl implements FastMetaType<HardwareSensor>  {
    serialize(sCtx_74: FastFutureContext, obj_75: HardwareSensor, _out_76: DataOut): void  {
        let _mask: number = 0;
        if (obj_75.unit === null) _mask |= 1;
        _out_76.writeByte(_mask);
        _out_76.writeInt(obj_75.localId);
        const stringBytes_78 = new TextEncoder().encode(obj_75.descriptor);
        SerializerPackNumber.INSTANCE.put(_out_76, stringBytes_78.length);
        _out_76.write(stringBytes_78);
        if (obj_75.unit !== null)  {
            const stringBytes_80 = new TextEncoder().encode(obj_75.unit);
            SerializerPackNumber.INSTANCE.put(_out_76, stringBytes_80.length);
            _out_76.write(stringBytes_80);
            
        }
        
    }
    deserialize(sCtx_74: FastFutureContext, in__77: DataIn): HardwareSensor  {
        let localId_82: number;
        let descriptor_83: string;
        let unit_84: string;
        const _mask = in__77.readByte();
        localId_82 = in__77.readInt();
        let stringBytes_85: Uint8Array;
        const len_87 = Number(DeserializerPackNumber.INSTANCE.put(in__77));
        const bytes_88 = in__77.readBytes(len_87);
        stringBytes_85 = bytes_88;
        descriptor_83 = new TextDecoder('utf-8').decode(stringBytes_85);
        if (((_mask & 1) === 0))  {
            let stringBytes_89: Uint8Array;
            const len_91 = Number(DeserializerPackNumber.INSTANCE.put(in__77));
            const bytes_92 = in__77.readBytes(len_91);
            stringBytes_89 = bytes_92;
            unit_84 = new TextDecoder('utf-8').decode(stringBytes_89);
            
        }
        else  {
            unit_84 = null;
            
        }
        return new HardwareSensor(localId_82, descriptor_83, unit_84);
        
    }
    metaHashCode(obj: HardwareSensor | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_INT.metaHashCode(obj.localId);
        hash = 37 * hash + FastMeta.META_STRING.metaHashCode(obj.descriptor);
        hash = 37 * hash + FastMeta.META_STRING.metaHashCode(obj.unit);
        return hash | 0;
        
    }
    metaEquals(v1: HardwareSensor | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof HardwareSensor)) return false;
        if (!FastMeta.META_INT.metaEquals(v1.localId, v2.localId)) return false;
        if (!FastMeta.META_STRING.metaEquals(v1.descriptor, v2.descriptor)) return false;
        if (!FastMeta.META_STRING.metaEquals(v1.unit, v2.unit)) return false;
        return true;
        
    }
    metaToString(obj: HardwareSensor | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('HardwareSensor(');
        res.add('localId:').add(obj.localId);
        res.add(', ');
        res.add('descriptor:').add(obj.descriptor);
        res.add(', ');
        res.add('unit:').add(obj.unit);
        res.add(', ');
        res.add('hardwareType:').add(obj.getHardwareType());
        res.add(')');
        
    }
    public serializeToBytes(obj: HardwareSensor): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): HardwareSensor  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): HardwareSensor  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class HardwareSensorMetaImpl implements FastMetaType<HardwareSensor>  {
    serialize(sCtx_93: FastFutureContext, obj_94: HardwareSensor, _out_95: DataOut): void  {
        const typeId = typeof (obj_94 as any).getAetherTypeId === 'function' ? obj_94.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'HardwareSensor' with invalid type id $ {
            typeId
        }
        `);
        _out_95.writeByte(typeId);
        switch(typeId)  {
            case 1: (HardwareSensor as any).META_BODY.serialize(sCtx_93, obj_94 as any as HardwareSensor, _out_95);
            break;
            case 2: (HardwareActor as any).META_BODY.serialize(sCtx_93, obj_94 as any as HardwareActor, _out_95);
            break;
            default: throw new Error(`Cannot serialize 'HardwareSensor' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_93: FastFutureContext, in__96: DataIn): HardwareSensor  {
        const typeId = in__96.readUByte();
        switch(typeId)  {
            case 1: return (HardwareSensor as any).META_BODY.deserialize(sCtx_93, in__96) as any as HardwareSensor;
            case 2: return (HardwareActor as any).META_BODY.deserialize(sCtx_93, in__96) as any as HardwareSensor;
            default: throw new Error(`Bad type id $ {
                typeId
            }
            for type 'HardwareSensor'`);
            
        }
        
    }
    metaHashCode(obj: HardwareSensor | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 1: return (HardwareSensor as any).META_BODY.metaHashCode(obj as any as HardwareSensor);
            case 2: return (HardwareActor as any).META.metaHashCode(obj as any as HardwareActor);
            default: throw new Error(`Cannot hashCode 'HardwareSensor' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    metaEquals(v1: HardwareSensor | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined) return false;
        const typeId1 = (v1 as any).getAetherTypeId ? (v1 as any).getAetherTypeId() : -1;
        const typeId2 = (v2 as any).getAetherTypeId ? (v2 as any).getAetherTypeId() : -1;
        if (typeId1 === -1 || typeId1 !== typeId2) return false;
        switch(typeId1)  {
            case 1: return (HardwareSensor as any).META_BODY.metaEquals(v1 as any as HardwareSensor, v2);
            case 2: return (HardwareActor as any).META.metaEquals(v1 as any as HardwareActor, v2);
            default: throw new Error(`Cannot equals 'HardwareSensor' with unknown type id $ {
                typeId1
            }
            `);
            
        }
        
    }
    metaToString(obj: HardwareSensor | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return ;
            
        }
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 1: (HardwareSensor as any).META_BODY.metaToString(obj as any as HardwareSensor, res);
            break;
            case 2: (HardwareActor as any).META.metaToString(obj as any as HardwareActor, res);
            break;
            default: throw new Error(`Cannot toString 'HardwareSensor' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    public serializeToBytes(obj: HardwareSensor): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): HardwareSensor  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): HardwareSensor  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class KnownCommutatorMetaBodyImpl implements FastMetaType<KnownCommutator>  {
    serialize(sCtx_97: FastFutureContext, obj_98: KnownCommutator, _out_99: DataOut): void  {
        let _mask: number = 0;
        if (obj_98.label === null) _mask |= 1;
        _out_99.writeByte(_mask);
        const stringBytes_101 = new TextEncoder().encode(obj_98.uuid);
        SerializerPackNumber.INSTANCE.put(_out_99, stringBytes_101.length);
        _out_99.write(stringBytes_101);
        if (obj_98.label !== null)  {
            const stringBytes_103 = new TextEncoder().encode(obj_98.label);
            SerializerPackNumber.INSTANCE.put(_out_99, stringBytes_103.length);
            _out_99.write(stringBytes_103);
            
        }
        _out_99.writeLong(obj_98.lastSeen);
        _out_99.writeInt(obj_98.totalRequests);
        _out_99.writeInt(obj_98.totalResponses);
        
    }
    deserialize(sCtx_97: FastFutureContext, in__100: DataIn): KnownCommutator  {
        let uuid_105: string;
        let label_106: string;
        let lastSeen_107: bigint;
        let totalRequests_108: number;
        let totalResponses_109: number;
        const _mask = in__100.readByte();
        let stringBytes_110: Uint8Array;
        const len_112 = Number(DeserializerPackNumber.INSTANCE.put(in__100));
        const bytes_113 = in__100.readBytes(len_112);
        stringBytes_110 = bytes_113;
        uuid_105 = new TextDecoder('utf-8').decode(stringBytes_110);
        if (((_mask & 1) === 0))  {
            let stringBytes_114: Uint8Array;
            const len_116 = Number(DeserializerPackNumber.INSTANCE.put(in__100));
            const bytes_117 = in__100.readBytes(len_116);
            stringBytes_114 = bytes_117;
            label_106 = new TextDecoder('utf-8').decode(stringBytes_114);
            
        }
        else  {
            label_106 = null;
            
        }
        lastSeen_107 = in__100.readLong();
        totalRequests_108 = in__100.readInt();
        totalResponses_109 = in__100.readInt();
        return new KnownCommutator(uuid_105, label_106, lastSeen_107, totalRequests_108, totalResponses_109);
        
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
    serialize(sCtx_118: FastFutureContext, obj_119: MetricPoint, _out_120: DataOut): void  {
        _out_120.writeLong(obj_119.timestamp);
        _out_120.writeInt(obj_119.rtt);
        
    }
    deserialize(sCtx_118: FastFutureContext, in__121: DataIn): MetricPoint  {
        let timestamp_122: bigint;
        let rtt_123: number;
        timestamp_122 = in__121.readLong();
        rtt_123 = in__121.readInt();
        return new MetricPoint(timestamp_122, rtt_123);
        
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
export class SensorHistorySeriesMetaBodyImpl implements FastMetaType<SensorHistorySeries>  {
    serialize(sCtx_124: FastFutureContext, obj_125: SensorHistorySeries, _out_126: DataOut): void  {
        let _mask: number = 0;
        if (obj_125.unit === null) _mask |= 1;
        _out_126.writeByte(_mask);
        const stringBytes_128 = new TextEncoder().encode(obj_125.commutatorUuid);
        SerializerPackNumber.INSTANCE.put(_out_126, stringBytes_128.length);
        _out_126.write(stringBytes_128);
        _out_126.writeInt(obj_125.deviceId);
        const stringBytes_130 = new TextEncoder().encode(obj_125.deviceName);
        SerializerPackNumber.INSTANCE.put(_out_126, stringBytes_130.length);
        _out_126.write(stringBytes_130);
        if (obj_125.unit !== null)  {
            const stringBytes_132 = new TextEncoder().encode(obj_125.unit);
            SerializerPackNumber.INSTANCE.put(_out_126, stringBytes_132.length);
            _out_126.write(stringBytes_132);
            
        }
        SerializerPackNumber.INSTANCE.put(_out_126, obj_125.points.length);
        for (const el_134 of obj_125.points)  {
            GraphPoint.META.serialize(sCtx_124, el_134, _out_126);
            
        }
        
    }
    deserialize(sCtx_124: FastFutureContext, in__127: DataIn): SensorHistorySeries  {
        let commutatorUuid_135: string;
        let deviceId_136: number;
        let deviceName_137: string;
        let unit_138: string;
        let points_139: GraphPoint[];
        const _mask = in__127.readByte();
        let stringBytes_140: Uint8Array;
        const len_142 = Number(DeserializerPackNumber.INSTANCE.put(in__127));
        const bytes_143 = in__127.readBytes(len_142);
        stringBytes_140 = bytes_143;
        commutatorUuid_135 = new TextDecoder('utf-8').decode(stringBytes_140);
        deviceId_136 = in__127.readInt();
        let stringBytes_144: Uint8Array;
        const len_146 = Number(DeserializerPackNumber.INSTANCE.put(in__127));
        const bytes_147 = in__127.readBytes(len_146);
        stringBytes_144 = bytes_147;
        deviceName_137 = new TextDecoder('utf-8').decode(stringBytes_144);
        if (((_mask & 1) === 0))  {
            let stringBytes_148: Uint8Array;
            const len_150 = Number(DeserializerPackNumber.INSTANCE.put(in__127));
            const bytes_151 = in__127.readBytes(len_150);
            stringBytes_148 = bytes_151;
            unit_138 = new TextDecoder('utf-8').decode(stringBytes_148);
            
        }
        else  {
            unit_138 = null;
            
        }
        const len_153 = Number(DeserializerPackNumber.INSTANCE.put(in__127));
        points_139 = new Array<GraphPoint>(len_153);
        for (let idx_152 = 0;
        idx_152 < len_153;
        idx_152++)  {
            points_139[idx_152] = GraphPoint.META.deserialize(sCtx_124, in__127);
            
        }
        return new SensorHistorySeries(commutatorUuid_135, deviceId_136, deviceName_137, unit_138, points_139);
        
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
export class VariantBoolMetaBodyImpl implements FastMetaType<VariantBool>  {
    serialize(sCtx_154: FastFutureContext, obj_155: VariantBool, _out_156: DataOut): void  {
        _out_156.writeBoolean(obj_155.value);
        
    }
    deserialize(sCtx_154: FastFutureContext, in__157: DataIn): VariantBool  {
        let value_158: boolean;
        value_158 = in__157.readBoolean();
        return new VariantBool(value_158);
        
    }
    metaHashCode(obj: VariantBool | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_BOOLEAN.metaHashCode(obj.value);
        return hash | 0;
        
    }
    metaEquals(v1: VariantBool | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof VariantBool)) return false;
        if (!FastMeta.META_BOOLEAN.metaEquals(v1.value, v2.value)) return false;
        return true;
        
    }
    metaToString(obj: VariantBool | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('VariantBool(');
        res.add('value:').add(obj.value);
        res.add(')');
        
    }
    public serializeToBytes(obj: VariantBool): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): VariantBool  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): VariantBool  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class VariantBoolMetaImpl implements FastMetaType<VariantBool>  {
    serialize(sCtx_159: FastFutureContext, obj_160: VariantBool, _out_161: DataOut): void  {
        const typeId = typeof (obj_160 as any).getAetherTypeId === 'function' ? obj_160.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'VariantBool' with invalid type id $ {
            typeId
        }
        `);
        _out_161.writeByte(typeId);
        switch(typeId)  {
            case 1: (VariantBool as any).META_BODY.serialize(sCtx_159, obj_160 as any as VariantBool, _out_161);
            break;
            case 2: (VariantLong as any).META_BODY.serialize(sCtx_159, obj_160 as any as VariantLong, _out_161);
            break;
            case 3: (VariantDouble as any).META_BODY.serialize(sCtx_159, obj_160 as any as VariantDouble, _out_161);
            break;
            case 4: (VariantString as any).META_BODY.serialize(sCtx_159, obj_160 as any as VariantString, _out_161);
            break;
            case 5: (VariantBytes as any).META_BODY.serialize(sCtx_159, obj_160 as any as VariantBytes, _out_161);
            break;
            default: throw new Error(`Cannot serialize 'VariantBool' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_159: FastFutureContext, in__162: DataIn): VariantBool  {
        const typeId = in__162.readUByte();
        switch(typeId)  {
            case 1: return (VariantBool as any).META_BODY.deserialize(sCtx_159, in__162) as any as VariantBool;
            case 2: return (VariantLong as any).META_BODY.deserialize(sCtx_159, in__162) as any as VariantBool;
            case 3: return (VariantDouble as any).META_BODY.deserialize(sCtx_159, in__162) as any as VariantBool;
            case 4: return (VariantString as any).META_BODY.deserialize(sCtx_159, in__162) as any as VariantBool;
            case 5: return (VariantBytes as any).META_BODY.deserialize(sCtx_159, in__162) as any as VariantBool;
            default: throw new Error(`Bad type id $ {
                typeId
            }
            for type 'VariantBool'`);
            
        }
        
    }
    metaHashCode(obj: VariantBool | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 1: return (VariantBool as any).META_BODY.metaHashCode(obj as any as VariantBool);
            case 2: return (VariantLong as any).META.metaHashCode(obj as any as VariantLong);
            case 3: return (VariantDouble as any).META.metaHashCode(obj as any as VariantDouble);
            case 4: return (VariantString as any).META.metaHashCode(obj as any as VariantString);
            case 5: return (VariantBytes as any).META.metaHashCode(obj as any as VariantBytes);
            default: throw new Error(`Cannot hashCode 'VariantBool' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    metaEquals(v1: VariantBool | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined) return false;
        const typeId1 = (v1 as any).getAetherTypeId ? (v1 as any).getAetherTypeId() : -1;
        const typeId2 = (v2 as any).getAetherTypeId ? (v2 as any).getAetherTypeId() : -1;
        if (typeId1 === -1 || typeId1 !== typeId2) return false;
        switch(typeId1)  {
            case 1: return (VariantBool as any).META_BODY.metaEquals(v1 as any as VariantBool, v2);
            case 2: return (VariantLong as any).META.metaEquals(v1 as any as VariantLong, v2);
            case 3: return (VariantDouble as any).META.metaEquals(v1 as any as VariantDouble, v2);
            case 4: return (VariantString as any).META.metaEquals(v1 as any as VariantString, v2);
            case 5: return (VariantBytes as any).META.metaEquals(v1 as any as VariantBytes, v2);
            default: throw new Error(`Cannot equals 'VariantBool' with unknown type id $ {
                typeId1
            }
            `);
            
        }
        
    }
    metaToString(obj: VariantBool | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return ;
            
        }
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 1: (VariantBool as any).META_BODY.metaToString(obj as any as VariantBool, res);
            break;
            case 2: (VariantLong as any).META.metaToString(obj as any as VariantLong, res);
            break;
            case 3: (VariantDouble as any).META.metaToString(obj as any as VariantDouble, res);
            break;
            case 4: (VariantString as any).META.metaToString(obj as any as VariantString, res);
            break;
            case 5: (VariantBytes as any).META.metaToString(obj as any as VariantBytes, res);
            break;
            default: throw new Error(`Cannot toString 'VariantBool' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    public serializeToBytes(obj: VariantBool): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): VariantBool  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): VariantBool  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class VariantBytesMetaBodyImpl implements FastMetaType<VariantBytes>  {
    serialize(sCtx_163: FastFutureContext, obj_164: VariantBytes, _out_165: DataOut): void  {
        SerializerPackNumber.INSTANCE.put(_out_165, obj_164.value.length);
        _out_165.write(obj_164.value);
        
    }
    deserialize(sCtx_163: FastFutureContext, in__166: DataIn): VariantBytes  {
        let value_168: Uint8Array;
        const len_170 = Number(DeserializerPackNumber.INSTANCE.put(in__166));
        const bytes_171 = in__166.readBytes(len_170);
        value_168 = bytes_171;
        return new VariantBytes(value_168);
        
    }
    metaHashCode(obj: VariantBytes | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_ARRAY_BYTE.metaHashCode(obj.value);
        return hash | 0;
        
    }
    metaEquals(v1: VariantBytes | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof VariantBytes)) return false;
        if (!FastMeta.META_ARRAY_BYTE.metaEquals(v1.value, v2.value)) return false;
        return true;
        
    }
    metaToString(obj: VariantBytes | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('VariantBytes(');
        res.add('value:').add(obj.value);
        res.add(')');
        
    }
    public serializeToBytes(obj: VariantBytes): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): VariantBytes  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): VariantBytes  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class VariantBytesMetaImpl implements FastMetaType<VariantBytes>  {
    serialize(sCtx_172: FastFutureContext, obj_173: VariantBytes, _out_174: DataOut): void  {
        const typeId = typeof (obj_173 as any).getAetherTypeId === 'function' ? obj_173.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'VariantBytes' with invalid type id $ {
            typeId
        }
        `);
        _out_174.writeByte(typeId);
        switch(typeId)  {
            case 5: (VariantBytes as any).META_BODY.serialize(sCtx_172, obj_173 as any as VariantBytes, _out_174);
            break;
            case 1: (VariantBool as any).META_BODY.serialize(sCtx_172, obj_173 as any as VariantBool, _out_174);
            break;
            case 2: (VariantLong as any).META_BODY.serialize(sCtx_172, obj_173 as any as VariantLong, _out_174);
            break;
            case 3: (VariantDouble as any).META_BODY.serialize(sCtx_172, obj_173 as any as VariantDouble, _out_174);
            break;
            case 4: (VariantString as any).META_BODY.serialize(sCtx_172, obj_173 as any as VariantString, _out_174);
            break;
            default: throw new Error(`Cannot serialize 'VariantBytes' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_172: FastFutureContext, in__175: DataIn): VariantBytes  {
        const typeId = in__175.readUByte();
        switch(typeId)  {
            case 5: return (VariantBytes as any).META_BODY.deserialize(sCtx_172, in__175) as any as VariantBytes;
            case 1: return (VariantBool as any).META_BODY.deserialize(sCtx_172, in__175) as any as VariantBytes;
            case 2: return (VariantLong as any).META_BODY.deserialize(sCtx_172, in__175) as any as VariantBytes;
            case 3: return (VariantDouble as any).META_BODY.deserialize(sCtx_172, in__175) as any as VariantBytes;
            case 4: return (VariantString as any).META_BODY.deserialize(sCtx_172, in__175) as any as VariantBytes;
            default: throw new Error(`Bad type id $ {
                typeId
            }
            for type 'VariantBytes'`);
            
        }
        
    }
    metaHashCode(obj: VariantBytes | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 5: return (VariantBytes as any).META_BODY.metaHashCode(obj as any as VariantBytes);
            case 1: return (VariantBool as any).META.metaHashCode(obj as any as VariantBool);
            case 2: return (VariantLong as any).META.metaHashCode(obj as any as VariantLong);
            case 3: return (VariantDouble as any).META.metaHashCode(obj as any as VariantDouble);
            case 4: return (VariantString as any).META.metaHashCode(obj as any as VariantString);
            default: throw new Error(`Cannot hashCode 'VariantBytes' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    metaEquals(v1: VariantBytes | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined) return false;
        const typeId1 = (v1 as any).getAetherTypeId ? (v1 as any).getAetherTypeId() : -1;
        const typeId2 = (v2 as any).getAetherTypeId ? (v2 as any).getAetherTypeId() : -1;
        if (typeId1 === -1 || typeId1 !== typeId2) return false;
        switch(typeId1)  {
            case 5: return (VariantBytes as any).META_BODY.metaEquals(v1 as any as VariantBytes, v2);
            case 1: return (VariantBool as any).META.metaEquals(v1 as any as VariantBool, v2);
            case 2: return (VariantLong as any).META.metaEquals(v1 as any as VariantLong, v2);
            case 3: return (VariantDouble as any).META.metaEquals(v1 as any as VariantDouble, v2);
            case 4: return (VariantString as any).META.metaEquals(v1 as any as VariantString, v2);
            default: throw new Error(`Cannot equals 'VariantBytes' with unknown type id $ {
                typeId1
            }
            `);
            
        }
        
    }
    metaToString(obj: VariantBytes | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return ;
            
        }
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 5: (VariantBytes as any).META_BODY.metaToString(obj as any as VariantBytes, res);
            break;
            case 1: (VariantBool as any).META.metaToString(obj as any as VariantBool, res);
            break;
            case 2: (VariantLong as any).META.metaToString(obj as any as VariantLong, res);
            break;
            case 3: (VariantDouble as any).META.metaToString(obj as any as VariantDouble, res);
            break;
            case 4: (VariantString as any).META.metaToString(obj as any as VariantString, res);
            break;
            default: throw new Error(`Cannot toString 'VariantBytes' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    public serializeToBytes(obj: VariantBytes): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): VariantBytes  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): VariantBytes  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class VariantDoubleMetaBodyImpl implements FastMetaType<VariantDouble>  {
    serialize(sCtx_176: FastFutureContext, obj_177: VariantDouble, _out_178: DataOut): void  {
        _out_178.writeDouble(obj_177.value);
        
    }
    deserialize(sCtx_176: FastFutureContext, in__179: DataIn): VariantDouble  {
        let value_180: number;
        value_180 = in__179.readDouble();
        return new VariantDouble(value_180);
        
    }
    metaHashCode(obj: VariantDouble | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_DOUBLE.metaHashCode(obj.value);
        return hash | 0;
        
    }
    metaEquals(v1: VariantDouble | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof VariantDouble)) return false;
        if (!FastMeta.META_DOUBLE.metaEquals(v1.value, v2.value)) return false;
        return true;
        
    }
    metaToString(obj: VariantDouble | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('VariantDouble(');
        res.add('value:').add(obj.value);
        res.add(')');
        
    }
    public serializeToBytes(obj: VariantDouble): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): VariantDouble  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): VariantDouble  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class VariantDoubleMetaImpl implements FastMetaType<VariantDouble>  {
    serialize(sCtx_181: FastFutureContext, obj_182: VariantDouble, _out_183: DataOut): void  {
        const typeId = typeof (obj_182 as any).getAetherTypeId === 'function' ? obj_182.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'VariantDouble' with invalid type id $ {
            typeId
        }
        `);
        _out_183.writeByte(typeId);
        switch(typeId)  {
            case 3: (VariantDouble as any).META_BODY.serialize(sCtx_181, obj_182 as any as VariantDouble, _out_183);
            break;
            case 1: (VariantBool as any).META_BODY.serialize(sCtx_181, obj_182 as any as VariantBool, _out_183);
            break;
            case 2: (VariantLong as any).META_BODY.serialize(sCtx_181, obj_182 as any as VariantLong, _out_183);
            break;
            case 4: (VariantString as any).META_BODY.serialize(sCtx_181, obj_182 as any as VariantString, _out_183);
            break;
            case 5: (VariantBytes as any).META_BODY.serialize(sCtx_181, obj_182 as any as VariantBytes, _out_183);
            break;
            default: throw new Error(`Cannot serialize 'VariantDouble' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_181: FastFutureContext, in__184: DataIn): VariantDouble  {
        const typeId = in__184.readUByte();
        switch(typeId)  {
            case 3: return (VariantDouble as any).META_BODY.deserialize(sCtx_181, in__184) as any as VariantDouble;
            case 1: return (VariantBool as any).META_BODY.deserialize(sCtx_181, in__184) as any as VariantDouble;
            case 2: return (VariantLong as any).META_BODY.deserialize(sCtx_181, in__184) as any as VariantDouble;
            case 4: return (VariantString as any).META_BODY.deserialize(sCtx_181, in__184) as any as VariantDouble;
            case 5: return (VariantBytes as any).META_BODY.deserialize(sCtx_181, in__184) as any as VariantDouble;
            default: throw new Error(`Bad type id $ {
                typeId
            }
            for type 'VariantDouble'`);
            
        }
        
    }
    metaHashCode(obj: VariantDouble | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 3: return (VariantDouble as any).META_BODY.metaHashCode(obj as any as VariantDouble);
            case 1: return (VariantBool as any).META.metaHashCode(obj as any as VariantBool);
            case 2: return (VariantLong as any).META.metaHashCode(obj as any as VariantLong);
            case 4: return (VariantString as any).META.metaHashCode(obj as any as VariantString);
            case 5: return (VariantBytes as any).META.metaHashCode(obj as any as VariantBytes);
            default: throw new Error(`Cannot hashCode 'VariantDouble' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    metaEquals(v1: VariantDouble | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined) return false;
        const typeId1 = (v1 as any).getAetherTypeId ? (v1 as any).getAetherTypeId() : -1;
        const typeId2 = (v2 as any).getAetherTypeId ? (v2 as any).getAetherTypeId() : -1;
        if (typeId1 === -1 || typeId1 !== typeId2) return false;
        switch(typeId1)  {
            case 3: return (VariantDouble as any).META_BODY.metaEquals(v1 as any as VariantDouble, v2);
            case 1: return (VariantBool as any).META.metaEquals(v1 as any as VariantBool, v2);
            case 2: return (VariantLong as any).META.metaEquals(v1 as any as VariantLong, v2);
            case 4: return (VariantString as any).META.metaEquals(v1 as any as VariantString, v2);
            case 5: return (VariantBytes as any).META.metaEquals(v1 as any as VariantBytes, v2);
            default: throw new Error(`Cannot equals 'VariantDouble' with unknown type id $ {
                typeId1
            }
            `);
            
        }
        
    }
    metaToString(obj: VariantDouble | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return ;
            
        }
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 3: (VariantDouble as any).META_BODY.metaToString(obj as any as VariantDouble, res);
            break;
            case 1: (VariantBool as any).META.metaToString(obj as any as VariantBool, res);
            break;
            case 2: (VariantLong as any).META.metaToString(obj as any as VariantLong, res);
            break;
            case 4: (VariantString as any).META.metaToString(obj as any as VariantString, res);
            break;
            case 5: (VariantBytes as any).META.metaToString(obj as any as VariantBytes, res);
            break;
            default: throw new Error(`Cannot toString 'VariantDouble' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    public serializeToBytes(obj: VariantDouble): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): VariantDouble  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): VariantDouble  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class VariantLongMetaBodyImpl implements FastMetaType<VariantLong>  {
    serialize(sCtx_185: FastFutureContext, obj_186: VariantLong, _out_187: DataOut): void  {
        _out_187.writeLong(obj_186.value);
        
    }
    deserialize(sCtx_185: FastFutureContext, in__188: DataIn): VariantLong  {
        let value_189: bigint;
        value_189 = in__188.readLong();
        return new VariantLong(value_189);
        
    }
    metaHashCode(obj: VariantLong | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_LONG.metaHashCode(obj.value);
        return hash | 0;
        
    }
    metaEquals(v1: VariantLong | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof VariantLong)) return false;
        if (!FastMeta.META_LONG.metaEquals(v1.value, v2.value)) return false;
        return true;
        
    }
    metaToString(obj: VariantLong | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('VariantLong(');
        res.add('value:').add(obj.value);
        res.add(')');
        
    }
    public serializeToBytes(obj: VariantLong): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): VariantLong  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): VariantLong  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class VariantLongMetaImpl implements FastMetaType<VariantLong>  {
    serialize(sCtx_190: FastFutureContext, obj_191: VariantLong, _out_192: DataOut): void  {
        const typeId = typeof (obj_191 as any).getAetherTypeId === 'function' ? obj_191.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'VariantLong' with invalid type id $ {
            typeId
        }
        `);
        _out_192.writeByte(typeId);
        switch(typeId)  {
            case 2: (VariantLong as any).META_BODY.serialize(sCtx_190, obj_191 as any as VariantLong, _out_192);
            break;
            case 1: (VariantBool as any).META_BODY.serialize(sCtx_190, obj_191 as any as VariantBool, _out_192);
            break;
            case 3: (VariantDouble as any).META_BODY.serialize(sCtx_190, obj_191 as any as VariantDouble, _out_192);
            break;
            case 4: (VariantString as any).META_BODY.serialize(sCtx_190, obj_191 as any as VariantString, _out_192);
            break;
            case 5: (VariantBytes as any).META_BODY.serialize(sCtx_190, obj_191 as any as VariantBytes, _out_192);
            break;
            default: throw new Error(`Cannot serialize 'VariantLong' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_190: FastFutureContext, in__193: DataIn): VariantLong  {
        const typeId = in__193.readUByte();
        switch(typeId)  {
            case 2: return (VariantLong as any).META_BODY.deserialize(sCtx_190, in__193) as any as VariantLong;
            case 1: return (VariantBool as any).META_BODY.deserialize(sCtx_190, in__193) as any as VariantLong;
            case 3: return (VariantDouble as any).META_BODY.deserialize(sCtx_190, in__193) as any as VariantLong;
            case 4: return (VariantString as any).META_BODY.deserialize(sCtx_190, in__193) as any as VariantLong;
            case 5: return (VariantBytes as any).META_BODY.deserialize(sCtx_190, in__193) as any as VariantLong;
            default: throw new Error(`Bad type id $ {
                typeId
            }
            for type 'VariantLong'`);
            
        }
        
    }
    metaHashCode(obj: VariantLong | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 2: return (VariantLong as any).META_BODY.metaHashCode(obj as any as VariantLong);
            case 1: return (VariantBool as any).META.metaHashCode(obj as any as VariantBool);
            case 3: return (VariantDouble as any).META.metaHashCode(obj as any as VariantDouble);
            case 4: return (VariantString as any).META.metaHashCode(obj as any as VariantString);
            case 5: return (VariantBytes as any).META.metaHashCode(obj as any as VariantBytes);
            default: throw new Error(`Cannot hashCode 'VariantLong' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    metaEquals(v1: VariantLong | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined) return false;
        const typeId1 = (v1 as any).getAetherTypeId ? (v1 as any).getAetherTypeId() : -1;
        const typeId2 = (v2 as any).getAetherTypeId ? (v2 as any).getAetherTypeId() : -1;
        if (typeId1 === -1 || typeId1 !== typeId2) return false;
        switch(typeId1)  {
            case 2: return (VariantLong as any).META_BODY.metaEquals(v1 as any as VariantLong, v2);
            case 1: return (VariantBool as any).META.metaEquals(v1 as any as VariantBool, v2);
            case 3: return (VariantDouble as any).META.metaEquals(v1 as any as VariantDouble, v2);
            case 4: return (VariantString as any).META.metaEquals(v1 as any as VariantString, v2);
            case 5: return (VariantBytes as any).META.metaEquals(v1 as any as VariantBytes, v2);
            default: throw new Error(`Cannot equals 'VariantLong' with unknown type id $ {
                typeId1
            }
            `);
            
        }
        
    }
    metaToString(obj: VariantLong | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return ;
            
        }
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 2: (VariantLong as any).META_BODY.metaToString(obj as any as VariantLong, res);
            break;
            case 1: (VariantBool as any).META.metaToString(obj as any as VariantBool, res);
            break;
            case 3: (VariantDouble as any).META.metaToString(obj as any as VariantDouble, res);
            break;
            case 4: (VariantString as any).META.metaToString(obj as any as VariantString, res);
            break;
            case 5: (VariantBytes as any).META.metaToString(obj as any as VariantBytes, res);
            break;
            default: throw new Error(`Cannot toString 'VariantLong' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    public serializeToBytes(obj: VariantLong): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): VariantLong  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): VariantLong  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class VariantStringMetaBodyImpl implements FastMetaType<VariantString>  {
    serialize(sCtx_194: FastFutureContext, obj_195: VariantString, _out_196: DataOut): void  {
        const stringBytes_198 = new TextEncoder().encode(obj_195.value);
        SerializerPackNumber.INSTANCE.put(_out_196, stringBytes_198.length);
        _out_196.write(stringBytes_198);
        
    }
    deserialize(sCtx_194: FastFutureContext, in__197: DataIn): VariantString  {
        let value_200: string;
        let stringBytes_201: Uint8Array;
        const len_203 = Number(DeserializerPackNumber.INSTANCE.put(in__197));
        const bytes_204 = in__197.readBytes(len_203);
        stringBytes_201 = bytes_204;
        value_200 = new TextDecoder('utf-8').decode(stringBytes_201);
        return new VariantString(value_200);
        
    }
    metaHashCode(obj: VariantString | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_STRING.metaHashCode(obj.value);
        return hash | 0;
        
    }
    metaEquals(v1: VariantString | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof VariantString)) return false;
        if (!FastMeta.META_STRING.metaEquals(v1.value, v2.value)) return false;
        return true;
        
    }
    metaToString(obj: VariantString | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('VariantString(');
        res.add('value:').add(obj.value);
        res.add(')');
        
    }
    public serializeToBytes(obj: VariantString): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): VariantString  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): VariantString  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class VariantStringMetaImpl implements FastMetaType<VariantString>  {
    serialize(sCtx_205: FastFutureContext, obj_206: VariantString, _out_207: DataOut): void  {
        const typeId = typeof (obj_206 as any).getAetherTypeId === 'function' ? obj_206.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'VariantString' with invalid type id $ {
            typeId
        }
        `);
        _out_207.writeByte(typeId);
        switch(typeId)  {
            case 4: (VariantString as any).META_BODY.serialize(sCtx_205, obj_206 as any as VariantString, _out_207);
            break;
            case 1: (VariantBool as any).META_BODY.serialize(sCtx_205, obj_206 as any as VariantBool, _out_207);
            break;
            case 2: (VariantLong as any).META_BODY.serialize(sCtx_205, obj_206 as any as VariantLong, _out_207);
            break;
            case 3: (VariantDouble as any).META_BODY.serialize(sCtx_205, obj_206 as any as VariantDouble, _out_207);
            break;
            case 5: (VariantBytes as any).META_BODY.serialize(sCtx_205, obj_206 as any as VariantBytes, _out_207);
            break;
            default: throw new Error(`Cannot serialize 'VariantString' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_205: FastFutureContext, in__208: DataIn): VariantString  {
        const typeId = in__208.readUByte();
        switch(typeId)  {
            case 4: return (VariantString as any).META_BODY.deserialize(sCtx_205, in__208) as any as VariantString;
            case 1: return (VariantBool as any).META_BODY.deserialize(sCtx_205, in__208) as any as VariantString;
            case 2: return (VariantLong as any).META_BODY.deserialize(sCtx_205, in__208) as any as VariantString;
            case 3: return (VariantDouble as any).META_BODY.deserialize(sCtx_205, in__208) as any as VariantString;
            case 5: return (VariantBytes as any).META_BODY.deserialize(sCtx_205, in__208) as any as VariantString;
            default: throw new Error(`Bad type id $ {
                typeId
            }
            for type 'VariantString'`);
            
        }
        
    }
    metaHashCode(obj: VariantString | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 4: return (VariantString as any).META_BODY.metaHashCode(obj as any as VariantString);
            case 1: return (VariantBool as any).META.metaHashCode(obj as any as VariantBool);
            case 2: return (VariantLong as any).META.metaHashCode(obj as any as VariantLong);
            case 3: return (VariantDouble as any).META.metaHashCode(obj as any as VariantDouble);
            case 5: return (VariantBytes as any).META.metaHashCode(obj as any as VariantBytes);
            default: throw new Error(`Cannot hashCode 'VariantString' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    metaEquals(v1: VariantString | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined) return false;
        const typeId1 = (v1 as any).getAetherTypeId ? (v1 as any).getAetherTypeId() : -1;
        const typeId2 = (v2 as any).getAetherTypeId ? (v2 as any).getAetherTypeId() : -1;
        if (typeId1 === -1 || typeId1 !== typeId2) return false;
        switch(typeId1)  {
            case 4: return (VariantString as any).META_BODY.metaEquals(v1 as any as VariantString, v2);
            case 1: return (VariantBool as any).META.metaEquals(v1 as any as VariantBool, v2);
            case 2: return (VariantLong as any).META.metaEquals(v1 as any as VariantLong, v2);
            case 3: return (VariantDouble as any).META.metaEquals(v1 as any as VariantDouble, v2);
            case 5: return (VariantBytes as any).META.metaEquals(v1 as any as VariantBytes, v2);
            default: throw new Error(`Cannot equals 'VariantString' with unknown type id $ {
                typeId1
            }
            `);
            
        }
        
    }
    metaToString(obj: VariantString | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return ;
            
        }
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 4: (VariantString as any).META_BODY.metaToString(obj as any as VariantString, res);
            break;
            case 1: (VariantBool as any).META.metaToString(obj as any as VariantBool, res);
            break;
            case 2: (VariantLong as any).META.metaToString(obj as any as VariantLong, res);
            break;
            case 3: (VariantDouble as any).META.metaToString(obj as any as VariantDouble, res);
            break;
            case 5: (VariantBytes as any).META.metaToString(obj as any as VariantBytes, res);
            break;
            default: throw new Error(`Cannot toString 'VariantString' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    public serializeToBytes(obj: VariantString): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): VariantString  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): VariantString  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class SmartHomeClientStreamMetaImpl implements FastMetaType<SmartHomeClientStream>  {
    serialize(ctx: FastFutureContext, obj: SmartHomeClientStream, out: DataOut): void  {
        FastMeta.META_ARRAY_BYTE.serialize(ctx, obj.data, out);
        
    }
    deserialize(ctx: FastFutureContext, in_: DataIn): SmartHomeClientStream  {
        return new SmartHomeClientStream(FastMeta.META_ARRAY_BYTE.deserialize(ctx, in_));
        
    }
    metaHashCode(obj: SmartHomeClientStream | null | undefined): number  {
        return FastMeta.META_ARRAY_BYTE.metaHashCode(obj?.data);
        
    }
    metaEquals(v1: SmartHomeClientStream | null | undefined, v2: any | null | undefined): boolean  {
        return FastMeta.META_ARRAY_BYTE.metaEquals(v1?.data, (v2 instanceof SmartHomeClientStream) ? v2.data : v2);
        
    }
    metaToString(obj: SmartHomeClientStream | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('SmartHomeClientStream(').add('data:').add(obj.data).add(')');
        
    }
    public serializeToBytes(obj: SmartHomeClientStream): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): SmartHomeClientStream  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): SmartHomeClientStream  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class SmartHomeCommutatorStreamMetaImpl implements FastMetaType<SmartHomeCommutatorStream>  {
    serialize(ctx: FastFutureContext, obj: SmartHomeCommutatorStream, out: DataOut): void  {
        FastMeta.META_ARRAY_BYTE.serialize(ctx, obj.data, out);
        
    }
    deserialize(ctx: FastFutureContext, in_: DataIn): SmartHomeCommutatorStream  {
        return new SmartHomeCommutatorStream(FastMeta.META_ARRAY_BYTE.deserialize(ctx, in_));
        
    }
    metaHashCode(obj: SmartHomeCommutatorStream | null | undefined): number  {
        return FastMeta.META_ARRAY_BYTE.metaHashCode(obj?.data);
        
    }
    metaEquals(v1: SmartHomeCommutatorStream | null | undefined, v2: any | null | undefined): boolean  {
        return FastMeta.META_ARRAY_BYTE.metaEquals(v1?.data, (v2 instanceof SmartHomeCommutatorStream) ? v2.data : v2);
        
    }
    metaToString(obj: SmartHomeCommutatorStream | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('SmartHomeCommutatorStream(').add('data:').add(obj.data).add(')');
        
    }
    public serializeToBytes(obj: SmartHomeCommutatorStream): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): SmartHomeCommutatorStream  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): SmartHomeCommutatorStream  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class SmartHomeCommutatorApiMetaImpl implements FastMetaApi<SmartHomeCommutatorApi, SmartHomeCommutatorApiRemote>  {
    makeLocal_fromDataIn(ctx: FastFutureContext, dataIn: DataIn, localApi: SmartHomeCommutatorApi): void  {
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
                case 10:  {
                    const reqId_209 = dataIn.readInt();
                    const argsNames_210: string[] = [];
                    const argsValues_211: any[] = [];
                    ctx.invokeLocalMethodBefore("getSystemStructure", argsNames_210, argsValues_211);
                    ctx.regLocalFuture();
                    const resultFuture = localApi.getSystemStructure();
                    ctx.invokeLocalMethodAfter("getSystemStructure", resultFuture, argsNames_210, argsValues_211);
                    resultFuture.to((v_213: HardwareDevice[]) =>  {
                        const data_212 = new DataInOut();
                        SerializerPackNumber.INSTANCE.put(data_212, v_213.length);
                        for (const el_214 of v_213)  {
                            HardwareDevice.META.serialize(ctx, el_214, data_212);
                            
                        }
                        ctx.sendResultToRemote(reqId_209, data_212.toArray());
                        
                    }
                    );
                    break;
                    
                }
                case 4:  {
                    const reqId_215 = dataIn.readInt();
                    let localActorId_216: number;
                    let command_217: VariantData;
                    localActorId_216 = dataIn.readInt();
                    command_217 = VariantData.META.deserialize(ctx, dataIn);
                    const argsNames_218: string[] = ["localActorId", "command"];
                    const argsValues_219: any[] = [localActorId_216, command_217];
                    ctx.invokeLocalMethodBefore("executeActorCommand", argsNames_218, argsValues_219);
                    ctx.regLocalFuture();
                    const resultFuture = localApi.executeActorCommand(localActorId_216, command_217);
                    ctx.invokeLocalMethodAfter("executeActorCommand", resultFuture, argsNames_218, argsValues_219);
                    resultFuture.to((v_221: DeviceStateData) =>  {
                        const data_220 = new DataInOut();
                        DeviceStateData.META.serialize(ctx, v_221, data_220);
                        ctx.sendResultToRemote(reqId_215, data_220.toArray());
                        
                    }
                    );
                    break;
                    
                }
                case 5:  {
                    const reqId_222 = dataIn.readInt();
                    let localDeviceId_223: number;
                    localDeviceId_223 = dataIn.readInt();
                    const argsNames_224: string[] = ["localDeviceId"];
                    const argsValues_225: any[] = [localDeviceId_223];
                    ctx.invokeLocalMethodBefore("queryState", argsNames_224, argsValues_225);
                    ctx.regLocalFuture();
                    const resultFuture = localApi.queryState(localDeviceId_223);
                    ctx.invokeLocalMethodAfter("queryState", resultFuture, argsNames_224, argsValues_225);
                    resultFuture.to((v_227: DeviceStateData) =>  {
                        const data_226 = new DataInOut();
                        DeviceStateData.META.serialize(ctx, v_227, data_226);
                        ctx.sendResultToRemote(reqId_222, data_226.toArray());
                        
                    }
                    );
                    break;
                    
                }
                case 6:  {
                    const argsNames_229: string[] = [];
                    const argsValues_230: any[] = [];
                    ctx.invokeLocalMethodBefore("queryAllSensorStates", argsNames_229, argsValues_230);
                    localApi.queryAllSensorStates();
                    ctx.invokeLocalMethodAfter("queryAllSensorStates", null, argsNames_229, argsValues_230);
                    break;
                    
                }
                default: throw new Error(`Unknown command ID: $ {
                    commandId
                }
                `);
                
            }
        }
        
    }
    makeLocal_fromBytes_ctxLocal(ctx: FastApiContextLocal<SmartHomeCommutatorApi>, data: Uint8Array): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), ctx.localApi);
        
    }
    makeLocal_fromBytes_ctx(ctx: FastFutureContext, data: Uint8Array, localApi: SmartHomeCommutatorApi): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), localApi);
        
    }
    makeRemote(sCtx_231: FastFutureContext): SmartHomeCommutatorApiRemote  {
        const remoteApiImpl =  {
            flush: (sendFuture?: AFuture): AFuture =>  {
                const futureToUse = sendFuture || AFuture.make();
                sCtx_231.flush(futureToUse);
                return futureToUse;
                
            }
            , getFastMetaContext: () => sCtx_231, getSystemStructure: (): ARFuture<HardwareDevice[]> =>  {
                const dataOut_233 = new DataInOut();
                dataOut_233.writeByte(10);
                const argsNames_235: string[] = [];
                const argsValues_236: any[] = [];
                const result_234 = ARFuture.of<HardwareDevice[]>();
                sCtx_231.invokeRemoteMethodAfter("getSystemStructure", result_234, argsNames_235, argsValues_236);
                const reqId_232 = sCtx_231.regFuture( {
                    onDone: (in_: DataIn) =>  {
                        (result_234 as ARFuture<HardwareDevice[]>).tryDone(FastMeta.getMetaArray(HardwareDevice.META).deserialize(sCtx_231, in_));
                        
                    }
                    , onError: (_in_: DataIn) =>  {
                        result_234.error(new Error("Remote call failed without a typed exception"));
                        
                    }
                    
                }
                );
                dataOut_233.writeInt(reqId_232);
                sCtx_231.sendToRemote(dataOut_233.toArray());
                return result_234;
                
            }
            , executeActorCommand: (localActorId: number, command: VariantData): ARFuture<DeviceStateData> =>  {
                const dataOut_238 = new DataInOut();
                dataOut_238.writeByte(4);
                const argsNames_240: string[] = ["localActorId", "command"];
                const argsValues_241: any[] = [localActorId, command];
                const result_239 = ARFuture.of<DeviceStateData>();
                sCtx_231.invokeRemoteMethodAfter("executeActorCommand", result_239, argsNames_240, argsValues_241);
                const reqId_237 = sCtx_231.regFuture( {
                    onDone: (in_: DataIn) =>  {
                        (result_239 as ARFuture<DeviceStateData>).tryDone(DeviceStateData.META.deserialize(sCtx_231, in_));
                        
                    }
                    , onError: (_in_: DataIn) =>  {
                        result_239.error(new Error("Remote call failed without a typed exception"));
                        
                    }
                    
                }
                );
                dataOut_238.writeInt(reqId_237);
                dataOut_238.writeInt(localActorId);
                VariantData.META.serialize(sCtx_231, command, dataOut_238);
                sCtx_231.sendToRemote(dataOut_238.toArray());
                return result_239;
                
            }
            , queryState: (localDeviceId: number): ARFuture<DeviceStateData> =>  {
                const dataOut_243 = new DataInOut();
                dataOut_243.writeByte(5);
                const argsNames_245: string[] = ["localDeviceId"];
                const argsValues_246: any[] = [localDeviceId];
                const result_244 = ARFuture.of<DeviceStateData>();
                sCtx_231.invokeRemoteMethodAfter("queryState", result_244, argsNames_245, argsValues_246);
                const reqId_242 = sCtx_231.regFuture( {
                    onDone: (in_: DataIn) =>  {
                        (result_244 as ARFuture<DeviceStateData>).tryDone(DeviceStateData.META.deserialize(sCtx_231, in_));
                        
                    }
                    , onError: (_in_: DataIn) =>  {
                        result_244.error(new Error("Remote call failed without a typed exception"));
                        
                    }
                    
                }
                );
                dataOut_243.writeInt(reqId_242);
                dataOut_243.writeInt(localDeviceId);
                sCtx_231.sendToRemote(dataOut_243.toArray());
                return result_244;
                
            }
            , queryAllSensorStates: (): void =>  {
                const dataOut_248 = new DataInOut();
                dataOut_248.writeByte(6);
                const argsNames_250: string[] = [];
                const argsValues_251: any[] = [];
                sCtx_231.invokeRemoteMethodAfter("queryAllSensorStates", null, argsNames_250, argsValues_251);
                sCtx_231.sendToRemote(dataOut_248.toArray());
                
            }
            , 
        };
        return remoteApiImpl as SmartHomeCommutatorApiRemote;
        
    }
    
}
export class SmartHomeClientApiMetaImpl implements FastMetaApi<SmartHomeClientApi, SmartHomeClientApiRemote>  {
    makeLocal_fromDataIn(ctx: FastFutureContext, dataIn: DataIn, localApi: SmartHomeClientApi): void  {
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
                    let localDeviceId_253: number;
                    let state_254: DeviceStateData;
                    localDeviceId_253 = dataIn.readInt();
                    state_254 = DeviceStateData.META.deserialize(ctx, dataIn);
                    const argsNames_255: string[] = ["localDeviceId", "state"];
                    const argsValues_256: any[] = [localDeviceId_253, state_254];
                    ctx.invokeLocalMethodBefore("deviceStateUpdated", argsNames_255, argsValues_256);
                    localApi.deviceStateUpdated(localDeviceId_253, state_254);
                    ctx.invokeLocalMethodAfter("deviceStateUpdated", null, argsNames_255, argsValues_256);
                    break;
                    
                }
                default: throw new Error(`Unknown command ID: $ {
                    commandId
                }
                `);
                
            }
        }
        
    }
    makeLocal_fromBytes_ctxLocal(ctx: FastApiContextLocal<SmartHomeClientApi>, data: Uint8Array): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), ctx.localApi);
        
    }
    makeLocal_fromBytes_ctx(ctx: FastFutureContext, data: Uint8Array, localApi: SmartHomeClientApi): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), localApi);
        
    }
    makeRemote(sCtx_257: FastFutureContext): SmartHomeClientApiRemote  {
        const remoteApiImpl =  {
            flush: (sendFuture?: AFuture): AFuture =>  {
                const futureToUse = sendFuture || AFuture.make();
                sCtx_257.flush(futureToUse);
                return futureToUse;
                
            }
            , getFastMetaContext: () => sCtx_257, deviceStateUpdated: (localDeviceId: number, state: DeviceStateData): void =>  {
                const dataOut_259 = new DataInOut();
                dataOut_259.writeByte(3);
                const argsNames_261: string[] = ["localDeviceId", "state"];
                const argsValues_262: any[] = [localDeviceId, state];
                sCtx_257.invokeRemoteMethodAfter("deviceStateUpdated", null, argsNames_261, argsValues_262);
                dataOut_259.writeInt(localDeviceId);
                DeviceStateData.META.serialize(sCtx_257, state, dataOut_259);
                sCtx_257.sendToRemote(dataOut_259.toArray());
                
            }
            , 
        };
        return remoteApiImpl as SmartHomeClientApiRemote;
        
    }
    
}