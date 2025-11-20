import  {
    AFuture, ARFuture, DataIn, DataOut, DataInOut, DataInOutStatic, FastMetaType, FastFutureContext, RemoteApi, FastMeta, SerializerPackNumber, DeserializerPackNumber, FastApiContextLocal, FastMetaApi, BytesConverter, RemoteApiFuture, FastFutureContextStub, UUID, URI, AConsumer, ToString, AString
}
from 'aether-client';
import  {
    HardwareDevice, VariantData, DeviceStateData, HardwareActor, HardwareSensor, VariantBool, VariantBytes, VariantDouble, VariantLong, VariantString, SmartHomeClientStream, SmartHomeCommutatorStream, SmartHomeCommutatorApi, SmartHomeClientApi, SmartHomeCommutatorApiRemote, SmartHomeClientApiRemote
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
export class DeviceStateDataMetaBodyImpl implements FastMetaType<DeviceStateData>  {
    serialize(sCtx_8: FastFutureContext, obj_9: DeviceStateData, _out_10: DataOut): void  {
        VariantData.META.serialize(sCtx_8, obj_9.payload, _out_10);
        _out_10.writeLong(obj_9.timestamp.getTime());
        
    }
    deserialize(sCtx_8: FastFutureContext, in__11: DataIn): DeviceStateData  {
        let payload_12: VariantData;
        let timestamp_13: Date;
        payload_12 = VariantData.META.deserialize(sCtx_8, in__11);
        timestamp_13 = new Date(Number(in__11.readLong()));
        return new DeviceStateData(payload_12, timestamp_13);
        
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
export class HardwareActorMetaBodyImpl implements FastMetaType<HardwareActor>  {
    serialize(sCtx_14: FastFutureContext, obj_15: HardwareActor, _out_16: DataOut): void  {
        _out_16.writeInt(obj_15.localId);
        const stringBytes_18 = new TextEncoder().encode(obj_15.descriptor);
        SerializerPackNumber.INSTANCE.put(_out_16, stringBytes_18.length);
        _out_16.write(stringBytes_18);
        
    }
    deserialize(sCtx_14: FastFutureContext, in__17: DataIn): HardwareActor  {
        let localId_20: number;
        let descriptor_21: string;
        localId_20 = in__17.readInt();
        let stringBytes_22: Uint8Array;
        const len_24 = Number(DeserializerPackNumber.INSTANCE.put(in__17));
        const bytes_25 = in__17.readBytes(len_24);
        stringBytes_22 = bytes_25;
        descriptor_21 = new TextDecoder('utf-8').decode(stringBytes_22);
        return new HardwareActor(localId_20, descriptor_21);
        
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
    serialize(sCtx_26: FastFutureContext, obj_27: HardwareActor, _out_28: DataOut): void  {
        const typeId = typeof (obj_27 as any).getAetherTypeId === 'function' ? obj_27.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'HardwareActor' with invalid type id $ {
            typeId
        }
        `);
        _out_28.writeByte(typeId);
        switch(typeId)  {
            case 2: (HardwareActor as any).META_BODY.serialize(sCtx_26, obj_27 as any as HardwareActor, _out_28);
            break;
            case 1: (HardwareSensor as any).META_BODY.serialize(sCtx_26, obj_27 as any as HardwareSensor, _out_28);
            break;
            default: throw new Error(`Cannot serialize 'HardwareActor' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_26: FastFutureContext, in__29: DataIn): HardwareActor  {
        const typeId = in__29.readUByte();
        switch(typeId)  {
            case 2: return (HardwareActor as any).META_BODY.deserialize(sCtx_26, in__29) as any as HardwareActor;
            case 1: return (HardwareSensor as any).META_BODY.deserialize(sCtx_26, in__29) as any as HardwareActor;
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
    serialize(sCtx_30: FastFutureContext, obj_31: HardwareSensor, _out_32: DataOut): void  {
        let _mask: number = 0;
        if (obj_31.unit === null) _mask |= 1;
        _out_32.writeByte(_mask);
        _out_32.writeInt(obj_31.localId);
        const stringBytes_34 = new TextEncoder().encode(obj_31.descriptor);
        SerializerPackNumber.INSTANCE.put(_out_32, stringBytes_34.length);
        _out_32.write(stringBytes_34);
        if (obj_31.unit !== null)  {
            const stringBytes_36 = new TextEncoder().encode(obj_31.unit);
            SerializerPackNumber.INSTANCE.put(_out_32, stringBytes_36.length);
            _out_32.write(stringBytes_36);
            
        }
        
    }
    deserialize(sCtx_30: FastFutureContext, in__33: DataIn): HardwareSensor  {
        let localId_38: number;
        let descriptor_39: string;
        let unit_40: string;
        const _mask = in__33.readByte();
        localId_38 = in__33.readInt();
        let stringBytes_41: Uint8Array;
        const len_43 = Number(DeserializerPackNumber.INSTANCE.put(in__33));
        const bytes_44 = in__33.readBytes(len_43);
        stringBytes_41 = bytes_44;
        descriptor_39 = new TextDecoder('utf-8').decode(stringBytes_41);
        if (((_mask & 1) === 0))  {
            let stringBytes_45: Uint8Array;
            const len_47 = Number(DeserializerPackNumber.INSTANCE.put(in__33));
            const bytes_48 = in__33.readBytes(len_47);
            stringBytes_45 = bytes_48;
            unit_40 = new TextDecoder('utf-8').decode(stringBytes_45);
            
        }
        else  {
            unit_40 = null;
            
        }
        return new HardwareSensor(localId_38, descriptor_39, unit_40);
        
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
    serialize(sCtx_49: FastFutureContext, obj_50: HardwareSensor, _out_51: DataOut): void  {
        const typeId = typeof (obj_50 as any).getAetherTypeId === 'function' ? obj_50.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'HardwareSensor' with invalid type id $ {
            typeId
        }
        `);
        _out_51.writeByte(typeId);
        switch(typeId)  {
            case 1: (HardwareSensor as any).META_BODY.serialize(sCtx_49, obj_50 as any as HardwareSensor, _out_51);
            break;
            case 2: (HardwareActor as any).META_BODY.serialize(sCtx_49, obj_50 as any as HardwareActor, _out_51);
            break;
            default: throw new Error(`Cannot serialize 'HardwareSensor' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_49: FastFutureContext, in__52: DataIn): HardwareSensor  {
        const typeId = in__52.readUByte();
        switch(typeId)  {
            case 1: return (HardwareSensor as any).META_BODY.deserialize(sCtx_49, in__52) as any as HardwareSensor;
            case 2: return (HardwareActor as any).META_BODY.deserialize(sCtx_49, in__52) as any as HardwareSensor;
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
export class VariantBoolMetaBodyImpl implements FastMetaType<VariantBool>  {
    serialize(sCtx_53: FastFutureContext, obj_54: VariantBool, _out_55: DataOut): void  {
        _out_55.writeBoolean(obj_54.value);
        
    }
    deserialize(sCtx_53: FastFutureContext, in__56: DataIn): VariantBool  {
        let value_57: boolean;
        value_57 = in__56.readBoolean();
        return new VariantBool(value_57);
        
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
    serialize(sCtx_58: FastFutureContext, obj_59: VariantBool, _out_60: DataOut): void  {
        const typeId = typeof (obj_59 as any).getAetherTypeId === 'function' ? obj_59.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'VariantBool' with invalid type id $ {
            typeId
        }
        `);
        _out_60.writeByte(typeId);
        switch(typeId)  {
            case 1: (VariantBool as any).META_BODY.serialize(sCtx_58, obj_59 as any as VariantBool, _out_60);
            break;
            case 2: (VariantLong as any).META_BODY.serialize(sCtx_58, obj_59 as any as VariantLong, _out_60);
            break;
            case 3: (VariantDouble as any).META_BODY.serialize(sCtx_58, obj_59 as any as VariantDouble, _out_60);
            break;
            case 4: (VariantString as any).META_BODY.serialize(sCtx_58, obj_59 as any as VariantString, _out_60);
            break;
            case 5: (VariantBytes as any).META_BODY.serialize(sCtx_58, obj_59 as any as VariantBytes, _out_60);
            break;
            default: throw new Error(`Cannot serialize 'VariantBool' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_58: FastFutureContext, in__61: DataIn): VariantBool  {
        const typeId = in__61.readUByte();
        switch(typeId)  {
            case 1: return (VariantBool as any).META_BODY.deserialize(sCtx_58, in__61) as any as VariantBool;
            case 2: return (VariantLong as any).META_BODY.deserialize(sCtx_58, in__61) as any as VariantBool;
            case 3: return (VariantDouble as any).META_BODY.deserialize(sCtx_58, in__61) as any as VariantBool;
            case 4: return (VariantString as any).META_BODY.deserialize(sCtx_58, in__61) as any as VariantBool;
            case 5: return (VariantBytes as any).META_BODY.deserialize(sCtx_58, in__61) as any as VariantBool;
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
    serialize(sCtx_62: FastFutureContext, obj_63: VariantBytes, _out_64: DataOut): void  {
        SerializerPackNumber.INSTANCE.put(_out_64, obj_63.value.length);
        _out_64.write(obj_63.value);
        
    }
    deserialize(sCtx_62: FastFutureContext, in__65: DataIn): VariantBytes  {
        let value_67: Uint8Array;
        const len_69 = Number(DeserializerPackNumber.INSTANCE.put(in__65));
        const bytes_70 = in__65.readBytes(len_69);
        value_67 = bytes_70;
        return new VariantBytes(value_67);
        
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
    serialize(sCtx_71: FastFutureContext, obj_72: VariantBytes, _out_73: DataOut): void  {
        const typeId = typeof (obj_72 as any).getAetherTypeId === 'function' ? obj_72.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'VariantBytes' with invalid type id $ {
            typeId
        }
        `);
        _out_73.writeByte(typeId);
        switch(typeId)  {
            case 5: (VariantBytes as any).META_BODY.serialize(sCtx_71, obj_72 as any as VariantBytes, _out_73);
            break;
            case 1: (VariantBool as any).META_BODY.serialize(sCtx_71, obj_72 as any as VariantBool, _out_73);
            break;
            case 2: (VariantLong as any).META_BODY.serialize(sCtx_71, obj_72 as any as VariantLong, _out_73);
            break;
            case 3: (VariantDouble as any).META_BODY.serialize(sCtx_71, obj_72 as any as VariantDouble, _out_73);
            break;
            case 4: (VariantString as any).META_BODY.serialize(sCtx_71, obj_72 as any as VariantString, _out_73);
            break;
            default: throw new Error(`Cannot serialize 'VariantBytes' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_71: FastFutureContext, in__74: DataIn): VariantBytes  {
        const typeId = in__74.readUByte();
        switch(typeId)  {
            case 5: return (VariantBytes as any).META_BODY.deserialize(sCtx_71, in__74) as any as VariantBytes;
            case 1: return (VariantBool as any).META_BODY.deserialize(sCtx_71, in__74) as any as VariantBytes;
            case 2: return (VariantLong as any).META_BODY.deserialize(sCtx_71, in__74) as any as VariantBytes;
            case 3: return (VariantDouble as any).META_BODY.deserialize(sCtx_71, in__74) as any as VariantBytes;
            case 4: return (VariantString as any).META_BODY.deserialize(sCtx_71, in__74) as any as VariantBytes;
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
    serialize(sCtx_75: FastFutureContext, obj_76: VariantDouble, _out_77: DataOut): void  {
        _out_77.writeDouble(obj_76.value);
        
    }
    deserialize(sCtx_75: FastFutureContext, in__78: DataIn): VariantDouble  {
        let value_79: number;
        value_79 = in__78.readDouble();
        return new VariantDouble(value_79);
        
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
    serialize(sCtx_80: FastFutureContext, obj_81: VariantDouble, _out_82: DataOut): void  {
        const typeId = typeof (obj_81 as any).getAetherTypeId === 'function' ? obj_81.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'VariantDouble' with invalid type id $ {
            typeId
        }
        `);
        _out_82.writeByte(typeId);
        switch(typeId)  {
            case 3: (VariantDouble as any).META_BODY.serialize(sCtx_80, obj_81 as any as VariantDouble, _out_82);
            break;
            case 1: (VariantBool as any).META_BODY.serialize(sCtx_80, obj_81 as any as VariantBool, _out_82);
            break;
            case 2: (VariantLong as any).META_BODY.serialize(sCtx_80, obj_81 as any as VariantLong, _out_82);
            break;
            case 4: (VariantString as any).META_BODY.serialize(sCtx_80, obj_81 as any as VariantString, _out_82);
            break;
            case 5: (VariantBytes as any).META_BODY.serialize(sCtx_80, obj_81 as any as VariantBytes, _out_82);
            break;
            default: throw new Error(`Cannot serialize 'VariantDouble' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_80: FastFutureContext, in__83: DataIn): VariantDouble  {
        const typeId = in__83.readUByte();
        switch(typeId)  {
            case 3: return (VariantDouble as any).META_BODY.deserialize(sCtx_80, in__83) as any as VariantDouble;
            case 1: return (VariantBool as any).META_BODY.deserialize(sCtx_80, in__83) as any as VariantDouble;
            case 2: return (VariantLong as any).META_BODY.deserialize(sCtx_80, in__83) as any as VariantDouble;
            case 4: return (VariantString as any).META_BODY.deserialize(sCtx_80, in__83) as any as VariantDouble;
            case 5: return (VariantBytes as any).META_BODY.deserialize(sCtx_80, in__83) as any as VariantDouble;
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
    serialize(sCtx_84: FastFutureContext, obj_85: VariantLong, _out_86: DataOut): void  {
        _out_86.writeLong(obj_85.value);
        
    }
    deserialize(sCtx_84: FastFutureContext, in__87: DataIn): VariantLong  {
        let value_88: bigint;
        value_88 = in__87.readLong();
        return new VariantLong(value_88);
        
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
    serialize(sCtx_89: FastFutureContext, obj_90: VariantLong, _out_91: DataOut): void  {
        const typeId = typeof (obj_90 as any).getAetherTypeId === 'function' ? obj_90.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'VariantLong' with invalid type id $ {
            typeId
        }
        `);
        _out_91.writeByte(typeId);
        switch(typeId)  {
            case 2: (VariantLong as any).META_BODY.serialize(sCtx_89, obj_90 as any as VariantLong, _out_91);
            break;
            case 1: (VariantBool as any).META_BODY.serialize(sCtx_89, obj_90 as any as VariantBool, _out_91);
            break;
            case 3: (VariantDouble as any).META_BODY.serialize(sCtx_89, obj_90 as any as VariantDouble, _out_91);
            break;
            case 4: (VariantString as any).META_BODY.serialize(sCtx_89, obj_90 as any as VariantString, _out_91);
            break;
            case 5: (VariantBytes as any).META_BODY.serialize(sCtx_89, obj_90 as any as VariantBytes, _out_91);
            break;
            default: throw new Error(`Cannot serialize 'VariantLong' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_89: FastFutureContext, in__92: DataIn): VariantLong  {
        const typeId = in__92.readUByte();
        switch(typeId)  {
            case 2: return (VariantLong as any).META_BODY.deserialize(sCtx_89, in__92) as any as VariantLong;
            case 1: return (VariantBool as any).META_BODY.deserialize(sCtx_89, in__92) as any as VariantLong;
            case 3: return (VariantDouble as any).META_BODY.deserialize(sCtx_89, in__92) as any as VariantLong;
            case 4: return (VariantString as any).META_BODY.deserialize(sCtx_89, in__92) as any as VariantLong;
            case 5: return (VariantBytes as any).META_BODY.deserialize(sCtx_89, in__92) as any as VariantLong;
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
    serialize(sCtx_93: FastFutureContext, obj_94: VariantString, _out_95: DataOut): void  {
        const stringBytes_97 = new TextEncoder().encode(obj_94.value);
        SerializerPackNumber.INSTANCE.put(_out_95, stringBytes_97.length);
        _out_95.write(stringBytes_97);
        
    }
    deserialize(sCtx_93: FastFutureContext, in__96: DataIn): VariantString  {
        let value_99: string;
        let stringBytes_100: Uint8Array;
        const len_102 = Number(DeserializerPackNumber.INSTANCE.put(in__96));
        const bytes_103 = in__96.readBytes(len_102);
        stringBytes_100 = bytes_103;
        value_99 = new TextDecoder('utf-8').decode(stringBytes_100);
        return new VariantString(value_99);
        
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
    serialize(sCtx_104: FastFutureContext, obj_105: VariantString, _out_106: DataOut): void  {
        const typeId = typeof (obj_105 as any).getAetherTypeId === 'function' ? obj_105.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'VariantString' with invalid type id $ {
            typeId
        }
        `);
        _out_106.writeByte(typeId);
        switch(typeId)  {
            case 4: (VariantString as any).META_BODY.serialize(sCtx_104, obj_105 as any as VariantString, _out_106);
            break;
            case 1: (VariantBool as any).META_BODY.serialize(sCtx_104, obj_105 as any as VariantBool, _out_106);
            break;
            case 2: (VariantLong as any).META_BODY.serialize(sCtx_104, obj_105 as any as VariantLong, _out_106);
            break;
            case 3: (VariantDouble as any).META_BODY.serialize(sCtx_104, obj_105 as any as VariantDouble, _out_106);
            break;
            case 5: (VariantBytes as any).META_BODY.serialize(sCtx_104, obj_105 as any as VariantBytes, _out_106);
            break;
            default: throw new Error(`Cannot serialize 'VariantString' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_104: FastFutureContext, in__107: DataIn): VariantString  {
        const typeId = in__107.readUByte();
        switch(typeId)  {
            case 4: return (VariantString as any).META_BODY.deserialize(sCtx_104, in__107) as any as VariantString;
            case 1: return (VariantBool as any).META_BODY.deserialize(sCtx_104, in__107) as any as VariantString;
            case 2: return (VariantLong as any).META_BODY.deserialize(sCtx_104, in__107) as any as VariantString;
            case 3: return (VariantDouble as any).META_BODY.deserialize(sCtx_104, in__107) as any as VariantString;
            case 5: return (VariantBytes as any).META_BODY.deserialize(sCtx_104, in__107) as any as VariantString;
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
                    const reqId_108 = dataIn.readInt();
                    const argsNames_109: string[] = [];
                    const argsValues_110: any[] = [];
                    ctx.invokeLocalMethodBefore("getSystemStructure", argsNames_109, argsValues_110);
                    ctx.regLocalFuture();
                    const resultFuture = localApi.getSystemStructure();
                    ctx.invokeLocalMethodAfter("getSystemStructure", resultFuture, argsNames_109, argsValues_110);
                    resultFuture.to((v_112: HardwareDevice[]) =>  {
                        const data_111 = new DataInOut();
                        SerializerPackNumber.INSTANCE.put(data_111, v_112.length);
                        for (const el_113 of v_112)  {
                            HardwareDevice.META.serialize(ctx, el_113, data_111);
                            
                        }
                        ctx.sendResultToRemote(reqId_108, data_111.toArray());
                        
                    }
                    );
                    break;
                    
                }
                case 4:  {
                    const reqId_114 = dataIn.readInt();
                    let localActorId_115: number;
                    let command_116: VariantData;
                    localActorId_115 = dataIn.readInt();
                    command_116 = VariantData.META.deserialize(ctx, dataIn);
                    const argsNames_117: string[] = ["localActorId", "command"];
                    const argsValues_118: any[] = [localActorId_115, command_116];
                    ctx.invokeLocalMethodBefore("executeActorCommand", argsNames_117, argsValues_118);
                    ctx.regLocalFuture();
                    const resultFuture = localApi.executeActorCommand(localActorId_115, command_116);
                    ctx.invokeLocalMethodAfter("executeActorCommand", resultFuture, argsNames_117, argsValues_118);
                    resultFuture.to((v_120: DeviceStateData) =>  {
                        const data_119 = new DataInOut();
                        DeviceStateData.META.serialize(ctx, v_120, data_119);
                        ctx.sendResultToRemote(reqId_114, data_119.toArray());
                        
                    }
                    );
                    break;
                    
                }
                case 5:  {
                    const reqId_121 = dataIn.readInt();
                    let localDeviceId_122: number;
                    localDeviceId_122 = dataIn.readInt();
                    const argsNames_123: string[] = ["localDeviceId"];
                    const argsValues_124: any[] = [localDeviceId_122];
                    ctx.invokeLocalMethodBefore("queryState", argsNames_123, argsValues_124);
                    ctx.regLocalFuture();
                    const resultFuture = localApi.queryState(localDeviceId_122);
                    ctx.invokeLocalMethodAfter("queryState", resultFuture, argsNames_123, argsValues_124);
                    resultFuture.to((v_126: DeviceStateData) =>  {
                        const data_125 = new DataInOut();
                        DeviceStateData.META.serialize(ctx, v_126, data_125);
                        ctx.sendResultToRemote(reqId_121, data_125.toArray());
                        
                    }
                    );
                    break;
                    
                }
                case 6:  {
                    const argsNames_128: string[] = [];
                    const argsValues_129: any[] = [];
                    ctx.invokeLocalMethodBefore("queryAllSensorStates", argsNames_128, argsValues_129);
                    localApi.queryAllSensorStates();
                    ctx.invokeLocalMethodAfter("queryAllSensorStates", null, argsNames_128, argsValues_129);
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
    makeRemote(sCtx_130: FastFutureContext): SmartHomeCommutatorApiRemote  {
        const remoteApiImpl =  {
            flush: (sendFuture?: AFuture): AFuture =>  {
                const futureToUse = sendFuture || AFuture.make();
                sCtx_130.flush(futureToUse);
                return futureToUse;
                
            }
            , getFastMetaContext: () => sCtx_130, getSystemStructure: (): ARFuture<HardwareDevice[]> =>  {
                const dataOut_132 = new DataInOut();
                dataOut_132.writeByte(10);
                const argsNames_134: string[] = [];
                const argsValues_135: any[] = [];
                const result_133 = ARFuture.of<HardwareDevice[]>();
                sCtx_130.invokeRemoteMethodAfter("getSystemStructure", result_133, argsNames_134, argsValues_135);
                const reqId_131 = sCtx_130.regFuture( {
                    onDone: (in_: DataIn) =>  {
                        (result_133 as ARFuture<HardwareDevice[]>).tryDone(FastMeta.getMetaArray(HardwareDevice.META).deserialize(sCtx_130, in_));
                        
                    }
                    , onError: (_in_: DataIn) =>  {
                        result_133.error(new Error("Remote call failed without a typed exception"));
                        
                    }
                    
                }
                );
                dataOut_132.writeInt(reqId_131);
                sCtx_130.sendToRemote(dataOut_132.toArray());
                return result_133;
                
            }
            , executeActorCommand: (localActorId: number, command: VariantData): ARFuture<DeviceStateData> =>  {
                const dataOut_137 = new DataInOut();
                dataOut_137.writeByte(4);
                const argsNames_139: string[] = ["localActorId", "command"];
                const argsValues_140: any[] = [localActorId, command];
                const result_138 = ARFuture.of<DeviceStateData>();
                sCtx_130.invokeRemoteMethodAfter("executeActorCommand", result_138, argsNames_139, argsValues_140);
                const reqId_136 = sCtx_130.regFuture( {
                    onDone: (in_: DataIn) =>  {
                        (result_138 as ARFuture<DeviceStateData>).tryDone(DeviceStateData.META.deserialize(sCtx_130, in_));
                        
                    }
                    , onError: (_in_: DataIn) =>  {
                        result_138.error(new Error("Remote call failed without a typed exception"));
                        
                    }
                    
                }
                );
                dataOut_137.writeInt(reqId_136);
                dataOut_137.writeInt(localActorId);
                VariantData.META.serialize(sCtx_130, command, dataOut_137);
                sCtx_130.sendToRemote(dataOut_137.toArray());
                return result_138;
                
            }
            , queryState: (localDeviceId: number): ARFuture<DeviceStateData> =>  {
                const dataOut_142 = new DataInOut();
                dataOut_142.writeByte(5);
                const argsNames_144: string[] = ["localDeviceId"];
                const argsValues_145: any[] = [localDeviceId];
                const result_143 = ARFuture.of<DeviceStateData>();
                sCtx_130.invokeRemoteMethodAfter("queryState", result_143, argsNames_144, argsValues_145);
                const reqId_141 = sCtx_130.regFuture( {
                    onDone: (in_: DataIn) =>  {
                        (result_143 as ARFuture<DeviceStateData>).tryDone(DeviceStateData.META.deserialize(sCtx_130, in_));
                        
                    }
                    , onError: (_in_: DataIn) =>  {
                        result_143.error(new Error("Remote call failed without a typed exception"));
                        
                    }
                    
                }
                );
                dataOut_142.writeInt(reqId_141);
                dataOut_142.writeInt(localDeviceId);
                sCtx_130.sendToRemote(dataOut_142.toArray());
                return result_143;
                
            }
            , queryAllSensorStates: (): void =>  {
                const dataOut_147 = new DataInOut();
                dataOut_147.writeByte(6);
                const argsNames_149: string[] = [];
                const argsValues_150: any[] = [];
                sCtx_130.invokeRemoteMethodAfter("queryAllSensorStates", null, argsNames_149, argsValues_150);
                sCtx_130.sendToRemote(dataOut_147.toArray());
                
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
                    let localDeviceId_152: number;
                    let state_153: DeviceStateData;
                    localDeviceId_152 = dataIn.readInt();
                    state_153 = DeviceStateData.META.deserialize(ctx, dataIn);
                    const argsNames_154: string[] = ["localDeviceId", "state"];
                    const argsValues_155: any[] = [localDeviceId_152, state_153];
                    ctx.invokeLocalMethodBefore("deviceStateUpdated", argsNames_154, argsValues_155);
                    localApi.deviceStateUpdated(localDeviceId_152, state_153);
                    ctx.invokeLocalMethodAfter("deviceStateUpdated", null, argsNames_154, argsValues_155);
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
    makeRemote(sCtx_156: FastFutureContext): SmartHomeClientApiRemote  {
        const remoteApiImpl =  {
            flush: (sendFuture?: AFuture): AFuture =>  {
                const futureToUse = sendFuture || AFuture.make();
                sCtx_156.flush(futureToUse);
                return futureToUse;
                
            }
            , getFastMetaContext: () => sCtx_156, deviceStateUpdated: (localDeviceId: number, state: DeviceStateData): void =>  {
                const dataOut_158 = new DataInOut();
                dataOut_158.writeByte(3);
                const argsNames_160: string[] = ["localDeviceId", "state"];
                const argsValues_161: any[] = [localDeviceId, state];
                sCtx_156.invokeRemoteMethodAfter("deviceStateUpdated", null, argsNames_160, argsValues_161);
                dataOut_158.writeInt(localDeviceId);
                DeviceStateData.META.serialize(sCtx_156, state, dataOut_158);
                sCtx_156.sendToRemote(dataOut_158.toArray());
                
            }
            , 
        };
        return remoteApiImpl as SmartHomeClientApiRemote;
        
    }
    
}