import  {
    AFuture, ARFuture, DataIn, DataOut, DataInOut, DataInOutStatic, FastMetaType, FastFutureContext, RemoteApi, FastMeta, SerializerPackNumber, DeserializerPackNumber, FastApiContextLocal, FastMetaApi, BytesConverter, RemoteApiFuture, FastFutureContextStub, UUID, URI, AConsumer, ToString, AString
}
from 'aether-client';
import  {
    ClientType, Device, HardwareDevice, Actor, DeviceStateData, HardwareActor, HardwareSensor, PendingPairing, Sensor, SmartHomeClientStream, SmartHomeCommutatorStream, SmartHomeServiceStream, SmartHomeServiceApi, SmartHomeCommutatorApi, SmartHomeClientApi, SmartHomeServiceApiRemote, SmartHomeCommutatorApiRemote, SmartHomeClientApiRemote
}
from './aether_api';
// This is always relative
export class ClientTypeMetaImpl implements FastMetaType<ClientType>  {
    serialize(_sCtx: FastFutureContext, obj: ClientType, out: DataOut): void  {
        const values = Object.keys(ClientType).filter(k => isNaN(parseInt(k)));
        out.writeByte(values.indexOf(obj as string));
        
    }
    deserialize(_sCtx: FastFutureContext, in_: DataIn): ClientType  {
        const ordinal = in_.readUByte();
        const keys = Object.keys(ClientType).filter(k => isNaN(parseInt(k)));
        if (ordinal < 0 || ordinal >= keys.length) throw new Error(`Invalid ordinal $ {
            ordinal
        }
        for enum ClientType`);
        return ClientType[keys[ordinal] as keyof typeof ClientType] as ClientType;
        
    }
    metaHashCode(obj: ClientType | null | undefined): number  {
        return FastMeta.META_STRING.metaHashCode(obj as string);
        
    }
    metaEquals(v1: ClientType | null | undefined, v2: any | null | undefined): boolean  {
        return FastMeta.META_STRING.metaEquals(v1 as string, v2);
        
    }
    metaToString(obj: ClientType | null | undefined, res: AString): void  {
        res.add(obj as string);
        
    }
    public serializeToBytes(obj: ClientType): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): ClientType  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): ClientType  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class DeviceMetaImpl implements FastMetaType<Device>  {
    serialize(sCtx_0: FastFutureContext, obj_1: Device, _out_2: DataOut): void  {
        const typeId = typeof (obj_1 as any).getAetherTypeId === 'function' ? obj_1.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'Device' with invalid type id $ {
            typeId
        }
        `);
        _out_2.writeByte(typeId);
        switch(typeId)  {
            case 1: (Sensor as any).META_BODY.serialize(sCtx_0, obj_1 as any as Sensor, _out_2);
            break;
            case 2: (Actor as any).META_BODY.serialize(sCtx_0, obj_1 as any as Actor, _out_2);
            break;
            default: throw new Error(`Cannot serialize 'Device' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_0: FastFutureContext, in__3: DataIn): Device  {
        const typeId = in__3.readUByte();
        switch(typeId)  {
            case 1: return (Sensor as any).META_BODY.deserialize(sCtx_0, in__3) as any as Device;
            case 2: return (Actor as any).META_BODY.deserialize(sCtx_0, in__3) as any as Device;
            default: throw new Error(`Bad type id $ {
                typeId
            }
            for type 'Device'`);
            
        }
        
    }
    metaHashCode(obj: Device | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 1: return (Sensor as any).META.metaHashCode(obj as any as Sensor);
            case 2: return (Actor as any).META.metaHashCode(obj as any as Actor);
            default: throw new Error(`Cannot hashCode 'Device' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    metaEquals(v1: Device | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined) return false;
        const typeId1 = (v1 as any).getAetherTypeId ? (v1 as any).getAetherTypeId() : -1;
        const typeId2 = (v2 as any).getAetherTypeId ? (v2 as any).getAetherTypeId() : -1;
        if (typeId1 === -1 || typeId1 !== typeId2) return false;
        switch(typeId1)  {
            case 1: return (Sensor as any).META.metaEquals(v1 as any as Sensor, v2);
            case 2: return (Actor as any).META.metaEquals(v1 as any as Actor, v2);
            default: throw new Error(`Cannot equals 'Device' with unknown type id $ {
                typeId1
            }
            `);
            
        }
        
    }
    metaToString(obj: Device | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return ;
            
        }
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 1: (Sensor as any).META.metaToString(obj as any as Sensor, res);
            break;
            case 2: (Actor as any).META.metaToString(obj as any as Actor, res);
            break;
            default: throw new Error(`Cannot toString 'Device' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    public serializeToBytes(obj: Device): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): Device  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): Device  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class HardwareDeviceMetaImpl implements FastMetaType<HardwareDevice>  {
    serialize(sCtx_4: FastFutureContext, obj_5: HardwareDevice, _out_6: DataOut): void  {
        const typeId = typeof (obj_5 as any).getAetherTypeId === 'function' ? obj_5.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'HardwareDevice' with invalid type id $ {
            typeId
        }
        `);
        _out_6.writeByte(typeId);
        switch(typeId)  {
            case 1: (HardwareSensor as any).META_BODY.serialize(sCtx_4, obj_5 as any as HardwareSensor, _out_6);
            break;
            case 2: (HardwareActor as any).META_BODY.serialize(sCtx_4, obj_5 as any as HardwareActor, _out_6);
            break;
            default: throw new Error(`Cannot serialize 'HardwareDevice' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_4: FastFutureContext, in__7: DataIn): HardwareDevice  {
        const typeId = in__7.readUByte();
        switch(typeId)  {
            case 1: return (HardwareSensor as any).META_BODY.deserialize(sCtx_4, in__7) as any as HardwareDevice;
            case 2: return (HardwareActor as any).META_BODY.deserialize(sCtx_4, in__7) as any as HardwareDevice;
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
export class ActorMetaBodyImpl implements FastMetaType<Actor>  {
    serialize(sCtx_8: FastFutureContext, obj_9: Actor, _out_10: DataOut): void  {
        let _mask: number = 0;
        if (obj_9.lastState === null) _mask |= 1;
        if (obj_9.lastUpdated === null) _mask |= (1 << 1);
        _out_10.writeByte(_mask);
        _out_10.writeInt(obj_9.id);
        const stringBytes_12 = new TextEncoder().encode(obj_9.name);
        SerializerPackNumber.INSTANCE.put(_out_10, stringBytes_12.length);
        _out_10.write(stringBytes_12);
        FastMeta.META_UUID.serialize(sCtx_8, obj_9.commutatorId, _out_10);
        _out_10.writeInt(obj_9.localDeviceId);
        if (obj_9.lastState !== null)  {
            const stringBytes_14 = new TextEncoder().encode(obj_9.lastState);
            SerializerPackNumber.INSTANCE.put(_out_10, stringBytes_14.length);
            _out_10.write(stringBytes_14);
            
        }
        if (obj_9.lastUpdated !== null)  {
            _out_10.writeLong(obj_9.lastUpdated.getTime());
            
        }
        
    }
    deserialize(sCtx_8: FastFutureContext, in__11: DataIn): Actor  {
        let id_16: number;
        let name_17: string;
        let commutatorId_18: UUID;
        let localDeviceId_19: number;
        let lastState_20: string;
        let lastUpdated_21: Date;
        const _mask = in__11.readByte();
        id_16 = in__11.readInt();
        let stringBytes_22: Uint8Array;
        const len_24 = Number(DeserializerPackNumber.INSTANCE.put(in__11).valueOf());
        const bytes_25 = in__11.readBytes(len_24);
        stringBytes_22 = bytes_25;
        name_17 = new TextDecoder('utf-8').decode(stringBytes_22);
        commutatorId_18 = FastMeta.META_UUID.deserialize(sCtx_8, in__11);
        localDeviceId_19 = in__11.readInt();
        if (((_mask & 1) === 0))  {
            let stringBytes_26: Uint8Array;
            const len_28 = Number(DeserializerPackNumber.INSTANCE.put(in__11).valueOf());
            const bytes_29 = in__11.readBytes(len_28);
            stringBytes_26 = bytes_29;
            lastState_20 = new TextDecoder('utf-8').decode(stringBytes_26);
            
        }
        else  {
            lastState_20 = null;
            
        }
        if (((_mask & (1 << 1)) === 0))  {
            lastUpdated_21 = new Date(Number(in__11.readLong()));
            
        }
        else  {
            lastUpdated_21 = null;
            
        }
        return new Actor(id_16, name_17, commutatorId_18, localDeviceId_19, lastState_20, lastUpdated_21);
        
    }
    metaHashCode(obj: Actor | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_INT.metaHashCode(obj.id);
        hash = 37 * hash + FastMeta.META_STRING.metaHashCode(obj.name);
        hash = 37 * hash + FastMeta.META_UUID.metaHashCode(obj.commutatorId);
        hash = 37 * hash + FastMeta.META_INT.metaHashCode(obj.localDeviceId);
        hash = 37 * hash + FastMeta.META_STRING.metaHashCode(obj.lastState);
        hash = 37 * hash + FastMeta.META_DATE.metaHashCode(obj.lastUpdated);
        return hash | 0;
        
    }
    metaEquals(v1: Actor | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof Actor)) return false;
        if (!FastMeta.META_INT.metaEquals(v1.id, v2.id)) return false;
        if (!FastMeta.META_STRING.metaEquals(v1.name, v2.name)) return false;
        if (!FastMeta.META_UUID.metaEquals(v1.commutatorId, v2.commutatorId)) return false;
        if (!FastMeta.META_INT.metaEquals(v1.localDeviceId, v2.localDeviceId)) return false;
        if (!FastMeta.META_STRING.metaEquals(v1.lastState, v2.lastState)) return false;
        if (!FastMeta.META_DATE.metaEquals(v1.lastUpdated, v2.lastUpdated)) return false;
        return true;
        
    }
    metaToString(obj: Actor | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('Actor(');
        res.add('id:').add(obj.id);
        res.add(', ');
        res.add('name:').add(obj.name);
        res.add(', ');
        res.add('commutatorId:').add(obj.commutatorId);
        res.add(', ');
        res.add('localDeviceId:').add(obj.localDeviceId);
        res.add(', ');
        res.add('lastState:').add(obj.lastState);
        res.add(', ');
        res.add('lastUpdated:').add(obj.lastUpdated);
        res.add(', ');
        res.add('deviceType:').add(obj.getDeviceType());
        res.add(')');
        
    }
    public serializeToBytes(obj: Actor): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): Actor  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): Actor  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class ActorMetaImpl implements FastMetaType<Actor>  {
    serialize(sCtx_30: FastFutureContext, obj_31: Actor, _out_32: DataOut): void  {
        const typeId = typeof (obj_31 as any).getAetherTypeId === 'function' ? obj_31.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'Actor' with invalid type id $ {
            typeId
        }
        `);
        _out_32.writeByte(typeId);
        switch(typeId)  {
            case 2: (Actor as any).META_BODY.serialize(sCtx_30, obj_31 as any as Actor, _out_32);
            break;
            case 1: (Sensor as any).META_BODY.serialize(sCtx_30, obj_31 as any as Sensor, _out_32);
            break;
            default: throw new Error(`Cannot serialize 'Actor' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_30: FastFutureContext, in__33: DataIn): Actor  {
        const typeId = in__33.readUByte();
        switch(typeId)  {
            case 2: return (Actor as any).META_BODY.deserialize(sCtx_30, in__33) as any as Actor;
            case 1: return (Sensor as any).META_BODY.deserialize(sCtx_30, in__33) as any as Actor;
            default: throw new Error(`Bad type id $ {
                typeId
            }
            for type 'Actor'`);
            
        }
        
    }
    metaHashCode(obj: Actor | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 2: return (Actor as any).META_BODY.metaHashCode(obj as any as Actor);
            case 1: return (Sensor as any).META.metaHashCode(obj as any as Sensor);
            default: throw new Error(`Cannot hashCode 'Actor' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    metaEquals(v1: Actor | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined) return false;
        const typeId1 = (v1 as any).getAetherTypeId ? (v1 as any).getAetherTypeId() : -1;
        const typeId2 = (v2 as any).getAetherTypeId ? (v2 as any).getAetherTypeId() : -1;
        if (typeId1 === -1 || typeId1 !== typeId2) return false;
        switch(typeId1)  {
            case 2: return (Actor as any).META_BODY.metaEquals(v1 as any as Actor, v2);
            case 1: return (Sensor as any).META.metaEquals(v1 as any as Sensor, v2);
            default: throw new Error(`Cannot equals 'Actor' with unknown type id $ {
                typeId1
            }
            `);
            
        }
        
    }
    metaToString(obj: Actor | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return ;
            
        }
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 2: (Actor as any).META_BODY.metaToString(obj as any as Actor, res);
            break;
            case 1: (Sensor as any).META.metaToString(obj as any as Sensor, res);
            break;
            default: throw new Error(`Cannot toString 'Actor' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    public serializeToBytes(obj: Actor): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): Actor  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): Actor  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class DeviceStateDataMetaBodyImpl implements FastMetaType<DeviceStateData>  {
    serialize(sCtx_34: FastFutureContext, obj_35: DeviceStateData, _out_36: DataOut): void  {
        const stringBytes_38 = new TextEncoder().encode(obj_35.value);
        SerializerPackNumber.INSTANCE.put(_out_36, stringBytes_38.length);
        _out_36.write(stringBytes_38);
        _out_36.writeLong(obj_35.timestamp.getTime());
        
    }
    deserialize(sCtx_34: FastFutureContext, in__37: DataIn): DeviceStateData  {
        let value_40: string;
        let timestamp_41: Date;
        let stringBytes_42: Uint8Array;
        const len_44 = Number(DeserializerPackNumber.INSTANCE.put(in__37).valueOf());
        const bytes_45 = in__37.readBytes(len_44);
        stringBytes_42 = bytes_45;
        value_40 = new TextDecoder('utf-8').decode(stringBytes_42);
        timestamp_41 = new Date(Number(in__37.readLong()));
        return new DeviceStateData(value_40, timestamp_41);
        
    }
    metaHashCode(obj: DeviceStateData | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_STRING.metaHashCode(obj.value);
        hash = 37 * hash + FastMeta.META_DATE.metaHashCode(obj.timestamp);
        return hash | 0;
        
    }
    metaEquals(v1: DeviceStateData | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof DeviceStateData)) return false;
        if (!FastMeta.META_STRING.metaEquals(v1.value, v2.value)) return false;
        if (!FastMeta.META_DATE.metaEquals(v1.timestamp, v2.timestamp)) return false;
        return true;
        
    }
    metaToString(obj: DeviceStateData | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('DeviceStateData(');
        res.add('value:').add(obj.value);
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
    serialize(sCtx_46: FastFutureContext, obj_47: HardwareActor, _out_48: DataOut): void  {
        _out_48.writeInt(obj_47.localId);
        const stringBytes_50 = new TextEncoder().encode(obj_47.descriptor);
        SerializerPackNumber.INSTANCE.put(_out_48, stringBytes_50.length);
        _out_48.write(stringBytes_50);
        
    }
    deserialize(sCtx_46: FastFutureContext, in__49: DataIn): HardwareActor  {
        let localId_52: number;
        let descriptor_53: string;
        localId_52 = in__49.readInt();
        let stringBytes_54: Uint8Array;
        const len_56 = Number(DeserializerPackNumber.INSTANCE.put(in__49).valueOf());
        const bytes_57 = in__49.readBytes(len_56);
        stringBytes_54 = bytes_57;
        descriptor_53 = new TextDecoder('utf-8').decode(stringBytes_54);
        return new HardwareActor(localId_52, descriptor_53);
        
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
    serialize(sCtx_58: FastFutureContext, obj_59: HardwareActor, _out_60: DataOut): void  {
        const typeId = typeof (obj_59 as any).getAetherTypeId === 'function' ? obj_59.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'HardwareActor' with invalid type id $ {
            typeId
        }
        `);
        _out_60.writeByte(typeId);
        switch(typeId)  {
            case 2: (HardwareActor as any).META_BODY.serialize(sCtx_58, obj_59 as any as HardwareActor, _out_60);
            break;
            case 1: (HardwareSensor as any).META_BODY.serialize(sCtx_58, obj_59 as any as HardwareSensor, _out_60);
            break;
            default: throw new Error(`Cannot serialize 'HardwareActor' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_58: FastFutureContext, in__61: DataIn): HardwareActor  {
        const typeId = in__61.readUByte();
        switch(typeId)  {
            case 2: return (HardwareActor as any).META_BODY.deserialize(sCtx_58, in__61) as any as HardwareActor;
            case 1: return (HardwareSensor as any).META_BODY.deserialize(sCtx_58, in__61) as any as HardwareActor;
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
    serialize(sCtx_62: FastFutureContext, obj_63: HardwareSensor, _out_64: DataOut): void  {
        let _mask: number = 0;
        if (obj_63.unit === null) _mask |= 1;
        _out_64.writeByte(_mask);
        _out_64.writeInt(obj_63.localId);
        const stringBytes_66 = new TextEncoder().encode(obj_63.descriptor);
        SerializerPackNumber.INSTANCE.put(_out_64, stringBytes_66.length);
        _out_64.write(stringBytes_66);
        if (obj_63.unit !== null)  {
            const stringBytes_68 = new TextEncoder().encode(obj_63.unit);
            SerializerPackNumber.INSTANCE.put(_out_64, stringBytes_68.length);
            _out_64.write(stringBytes_68);
            
        }
        
    }
    deserialize(sCtx_62: FastFutureContext, in__65: DataIn): HardwareSensor  {
        let localId_70: number;
        let descriptor_71: string;
        let unit_72: string;
        const _mask = in__65.readByte();
        localId_70 = in__65.readInt();
        let stringBytes_73: Uint8Array;
        const len_75 = Number(DeserializerPackNumber.INSTANCE.put(in__65).valueOf());
        const bytes_76 = in__65.readBytes(len_75);
        stringBytes_73 = bytes_76;
        descriptor_71 = new TextDecoder('utf-8').decode(stringBytes_73);
        if (((_mask & 1) === 0))  {
            let stringBytes_77: Uint8Array;
            const len_79 = Number(DeserializerPackNumber.INSTANCE.put(in__65).valueOf());
            const bytes_80 = in__65.readBytes(len_79);
            stringBytes_77 = bytes_80;
            unit_72 = new TextDecoder('utf-8').decode(stringBytes_77);
            
        }
        else  {
            unit_72 = null;
            
        }
        return new HardwareSensor(localId_70, descriptor_71, unit_72);
        
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
    serialize(sCtx_81: FastFutureContext, obj_82: HardwareSensor, _out_83: DataOut): void  {
        const typeId = typeof (obj_82 as any).getAetherTypeId === 'function' ? obj_82.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'HardwareSensor' with invalid type id $ {
            typeId
        }
        `);
        _out_83.writeByte(typeId);
        switch(typeId)  {
            case 1: (HardwareSensor as any).META_BODY.serialize(sCtx_81, obj_82 as any as HardwareSensor, _out_83);
            break;
            case 2: (HardwareActor as any).META_BODY.serialize(sCtx_81, obj_82 as any as HardwareActor, _out_83);
            break;
            default: throw new Error(`Cannot serialize 'HardwareSensor' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_81: FastFutureContext, in__84: DataIn): HardwareSensor  {
        const typeId = in__84.readUByte();
        switch(typeId)  {
            case 1: return (HardwareSensor as any).META_BODY.deserialize(sCtx_81, in__84) as any as HardwareSensor;
            case 2: return (HardwareActor as any).META_BODY.deserialize(sCtx_81, in__84) as any as HardwareSensor;
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
export class PendingPairingMetaBodyImpl implements FastMetaType<PendingPairing>  {
    serialize(sCtx_85: FastFutureContext, obj_86: PendingPairing, _out_87: DataOut): void  {
        FastMeta.META_UUID.serialize(sCtx_85, obj_86.commutatorId, _out_87);
        SerializerPackNumber.INSTANCE.put(_out_87, obj_86.devices.length);
        for (const el_89 of obj_86.devices)  {
            HardwareDevice.META.serialize(sCtx_85, el_89, _out_87);
            
        }
        
    }
    deserialize(sCtx_85: FastFutureContext, in__88: DataIn): PendingPairing  {
        let commutatorId_90: UUID;
        let devices_91: HardwareDevice[];
        commutatorId_90 = FastMeta.META_UUID.deserialize(sCtx_85, in__88);
        const len_93 = Number(DeserializerPackNumber.INSTANCE.put(in__88).valueOf());
        devices_91 = new Array<HardwareDevice>(len_93);
        for (let idx_92 = 0;
        idx_92 < len_93;
        idx_92++)  {
            devices_91[idx_92] = HardwareDevice.META.deserialize(sCtx_85, in__88);
            
        }
        return new PendingPairing(commutatorId_90, devices_91);
        
    }
    metaHashCode(obj: PendingPairing | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_UUID.metaHashCode(obj.commutatorId);
        hash = 37 * hash + FastMeta.getMetaArray(HardwareDevice.META).metaHashCode(obj.devices);
        return hash | 0;
        
    }
    metaEquals(v1: PendingPairing | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof PendingPairing)) return false;
        if (!FastMeta.META_UUID.metaEquals(v1.commutatorId, v2.commutatorId)) return false;
        if (!FastMeta.getMetaArray(HardwareDevice.META).metaEquals(v1.devices, v2.devices)) return false;
        return true;
        
    }
    metaToString(obj: PendingPairing | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('PendingPairing(');
        res.add('commutatorId:').add(obj.commutatorId);
        res.add(', ');
        res.add('devices:').add(obj.devices);
        res.add(')');
        
    }
    public serializeToBytes(obj: PendingPairing): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): PendingPairing  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): PendingPairing  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class SensorMetaBodyImpl implements FastMetaType<Sensor>  {
    serialize(sCtx_94: FastFutureContext, obj_95: Sensor, _out_96: DataOut): void  {
        let _mask: number = 0;
        if (obj_95.lastState === null) _mask |= 1;
        if (obj_95.lastUpdated === null) _mask |= (1 << 1);
        if (obj_95.unit === null) _mask |= (1 << 2);
        _out_96.writeByte(_mask);
        _out_96.writeInt(obj_95.id);
        const stringBytes_98 = new TextEncoder().encode(obj_95.name);
        SerializerPackNumber.INSTANCE.put(_out_96, stringBytes_98.length);
        _out_96.write(stringBytes_98);
        FastMeta.META_UUID.serialize(sCtx_94, obj_95.commutatorId, _out_96);
        _out_96.writeInt(obj_95.localDeviceId);
        if (obj_95.lastState !== null)  {
            const stringBytes_100 = new TextEncoder().encode(obj_95.lastState);
            SerializerPackNumber.INSTANCE.put(_out_96, stringBytes_100.length);
            _out_96.write(stringBytes_100);
            
        }
        if (obj_95.lastUpdated !== null)  {
            _out_96.writeLong(obj_95.lastUpdated.getTime());
            
        }
        if (obj_95.unit !== null)  {
            const stringBytes_102 = new TextEncoder().encode(obj_95.unit);
            SerializerPackNumber.INSTANCE.put(_out_96, stringBytes_102.length);
            _out_96.write(stringBytes_102);
            
        }
        
    }
    deserialize(sCtx_94: FastFutureContext, in__97: DataIn): Sensor  {
        let id_104: number;
        let name_105: string;
        let commutatorId_106: UUID;
        let localDeviceId_107: number;
        let lastState_108: string;
        let lastUpdated_109: Date;
        let unit_110: string;
        const _mask = in__97.readByte();
        id_104 = in__97.readInt();
        let stringBytes_111: Uint8Array;
        const len_113 = Number(DeserializerPackNumber.INSTANCE.put(in__97).valueOf());
        const bytes_114 = in__97.readBytes(len_113);
        stringBytes_111 = bytes_114;
        name_105 = new TextDecoder('utf-8').decode(stringBytes_111);
        commutatorId_106 = FastMeta.META_UUID.deserialize(sCtx_94, in__97);
        localDeviceId_107 = in__97.readInt();
        if (((_mask & 1) === 0))  {
            let stringBytes_115: Uint8Array;
            const len_117 = Number(DeserializerPackNumber.INSTANCE.put(in__97).valueOf());
            const bytes_118 = in__97.readBytes(len_117);
            stringBytes_115 = bytes_118;
            lastState_108 = new TextDecoder('utf-8').decode(stringBytes_115);
            
        }
        else  {
            lastState_108 = null;
            
        }
        if (((_mask & (1 << 1)) === 0))  {
            lastUpdated_109 = new Date(Number(in__97.readLong()));
            
        }
        else  {
            lastUpdated_109 = null;
            
        }
        if (((_mask & (1 << 2)) === 0))  {
            let stringBytes_119: Uint8Array;
            const len_121 = Number(DeserializerPackNumber.INSTANCE.put(in__97).valueOf());
            const bytes_122 = in__97.readBytes(len_121);
            stringBytes_119 = bytes_122;
            unit_110 = new TextDecoder('utf-8').decode(stringBytes_119);
            
        }
        else  {
            unit_110 = null;
            
        }
        return new Sensor(id_104, name_105, commutatorId_106, localDeviceId_107, lastState_108, lastUpdated_109, unit_110);
        
    }
    metaHashCode(obj: Sensor | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_INT.metaHashCode(obj.id);
        hash = 37 * hash + FastMeta.META_STRING.metaHashCode(obj.name);
        hash = 37 * hash + FastMeta.META_UUID.metaHashCode(obj.commutatorId);
        hash = 37 * hash + FastMeta.META_INT.metaHashCode(obj.localDeviceId);
        hash = 37 * hash + FastMeta.META_STRING.metaHashCode(obj.lastState);
        hash = 37 * hash + FastMeta.META_DATE.metaHashCode(obj.lastUpdated);
        hash = 37 * hash + FastMeta.META_STRING.metaHashCode(obj.unit);
        return hash | 0;
        
    }
    metaEquals(v1: Sensor | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof Sensor)) return false;
        if (!FastMeta.META_INT.metaEquals(v1.id, v2.id)) return false;
        if (!FastMeta.META_STRING.metaEquals(v1.name, v2.name)) return false;
        if (!FastMeta.META_UUID.metaEquals(v1.commutatorId, v2.commutatorId)) return false;
        if (!FastMeta.META_INT.metaEquals(v1.localDeviceId, v2.localDeviceId)) return false;
        if (!FastMeta.META_STRING.metaEquals(v1.lastState, v2.lastState)) return false;
        if (!FastMeta.META_DATE.metaEquals(v1.lastUpdated, v2.lastUpdated)) return false;
        if (!FastMeta.META_STRING.metaEquals(v1.unit, v2.unit)) return false;
        return true;
        
    }
    metaToString(obj: Sensor | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('Sensor(');
        res.add('id:').add(obj.id);
        res.add(', ');
        res.add('name:').add(obj.name);
        res.add(', ');
        res.add('commutatorId:').add(obj.commutatorId);
        res.add(', ');
        res.add('localDeviceId:').add(obj.localDeviceId);
        res.add(', ');
        res.add('lastState:').add(obj.lastState);
        res.add(', ');
        res.add('lastUpdated:').add(obj.lastUpdated);
        res.add(', ');
        res.add('unit:').add(obj.unit);
        res.add(', ');
        res.add('deviceType:').add(obj.getDeviceType());
        res.add(')');
        
    }
    public serializeToBytes(obj: Sensor): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): Sensor  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): Sensor  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class SensorMetaImpl implements FastMetaType<Sensor>  {
    serialize(sCtx_123: FastFutureContext, obj_124: Sensor, _out_125: DataOut): void  {
        const typeId = typeof (obj_124 as any).getAetherTypeId === 'function' ? obj_124.getAetherTypeId() : -1;
        if (typeId === undefined || typeId < 0) throw new Error(`Cannot serialize 'Sensor' with invalid type id $ {
            typeId
        }
        `);
        _out_125.writeByte(typeId);
        switch(typeId)  {
            case 1: (Sensor as any).META_BODY.serialize(sCtx_123, obj_124 as any as Sensor, _out_125);
            break;
            case 2: (Actor as any).META_BODY.serialize(sCtx_123, obj_124 as any as Actor, _out_125);
            break;
            default: throw new Error(`Cannot serialize 'Sensor' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    deserialize(sCtx_123: FastFutureContext, in__126: DataIn): Sensor  {
        const typeId = in__126.readUByte();
        switch(typeId)  {
            case 1: return (Sensor as any).META_BODY.deserialize(sCtx_123, in__126) as any as Sensor;
            case 2: return (Actor as any).META_BODY.deserialize(sCtx_123, in__126) as any as Sensor;
            default: throw new Error(`Bad type id $ {
                typeId
            }
            for type 'Sensor'`);
            
        }
        
    }
    metaHashCode(obj: Sensor | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 1: return (Sensor as any).META_BODY.metaHashCode(obj as any as Sensor);
            case 2: return (Actor as any).META.metaHashCode(obj as any as Actor);
            default: throw new Error(`Cannot hashCode 'Sensor' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    metaEquals(v1: Sensor | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined) return false;
        const typeId1 = (v1 as any).getAetherTypeId ? (v1 as any).getAetherTypeId() : -1;
        const typeId2 = (v2 as any).getAetherTypeId ? (v2 as any).getAetherTypeId() : -1;
        if (typeId1 === -1 || typeId1 !== typeId2) return false;
        switch(typeId1)  {
            case 1: return (Sensor as any).META_BODY.metaEquals(v1 as any as Sensor, v2);
            case 2: return (Actor as any).META.metaEquals(v1 as any as Actor, v2);
            default: throw new Error(`Cannot equals 'Sensor' with unknown type id $ {
                typeId1
            }
            `);
            
        }
        
    }
    metaToString(obj: Sensor | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return ;
            
        }
        const typeId = typeof (obj as any).getAetherTypeId === 'function' ? (obj as any).getAetherTypeId() : -1;
        switch(typeId)  {
            case 1: (Sensor as any).META_BODY.metaToString(obj as any as Sensor, res);
            break;
            case 2: (Actor as any).META.metaToString(obj as any as Actor, res);
            break;
            default: throw new Error(`Cannot toString 'Sensor' with unknown type id $ {
                typeId
            }
            `);
            
        }
        
    }
    public serializeToBytes(obj: Sensor): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): Sensor  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): Sensor  {
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
export class SmartHomeServiceStreamMetaImpl implements FastMetaType<SmartHomeServiceStream>  {
    serialize(ctx: FastFutureContext, obj: SmartHomeServiceStream, out: DataOut): void  {
        FastMeta.META_ARRAY_BYTE.serialize(ctx, obj.data, out);
        
    }
    deserialize(ctx: FastFutureContext, in_: DataIn): SmartHomeServiceStream  {
        return new SmartHomeServiceStream(FastMeta.META_ARRAY_BYTE.deserialize(ctx, in_));
        
    }
    metaHashCode(obj: SmartHomeServiceStream | null | undefined): number  {
        return FastMeta.META_ARRAY_BYTE.metaHashCode(obj?.data);
        
    }
    metaEquals(v1: SmartHomeServiceStream | null | undefined, v2: any | null | undefined): boolean  {
        return FastMeta.META_ARRAY_BYTE.metaEquals(v1?.data, (v2 instanceof SmartHomeServiceStream) ? v2.data : v2);
        
    }
    metaToString(obj: SmartHomeServiceStream | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('SmartHomeServiceStream(').add('data:').add(obj.data).add(')');
        
    }
    public serializeToBytes(obj: SmartHomeServiceStream): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): SmartHomeServiceStream  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): SmartHomeServiceStream  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class SmartHomeServiceApiMetaImpl implements FastMetaApi<SmartHomeServiceApi, SmartHomeServiceApiRemote>  {
    makeLocal_fromDataIn(ctx: FastFutureContext, dataIn: DataIn, localApi: SmartHomeServiceApi): void  {
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
                    let type_128: ClientType;
                    let sensors_129: HardwareSensor[];
                    let actors_130: HardwareActor[];
                    type_128 = ClientType.META.deserialize(ctx, dataIn);
                    const len_132 = Number(DeserializerPackNumber.INSTANCE.put(dataIn).valueOf());
                    sensors_129 = new Array<HardwareSensor>(len_132);
                    for (let idx_131 = 0;
                    idx_131 < len_132;
                    idx_131++)  {
                        sensors_129[idx_131] = HardwareSensor.META.deserialize(ctx, dataIn);
                        
                    }
                    const len_134 = Number(DeserializerPackNumber.INSTANCE.put(dataIn).valueOf());
                    actors_130 = new Array<HardwareActor>(len_134);
                    for (let idx_133 = 0;
                    idx_133 < len_134;
                    idx_133++)  {
                        actors_130[idx_133] = HardwareActor.META.deserialize(ctx, dataIn);
                        
                    }
                    const argsNames_135: string[] = ["type", "sensors", "actors"];
                    const argsValues_136: any[] = [type_128, sensors_129, actors_130];
                    ctx.invokeLocalMethodBefore("register", argsNames_135, argsValues_136);
                    localApi.register(type_128, sensors_129, actors_130);
                    ctx.invokeLocalMethodAfter("register", null, argsNames_135, argsValues_136);
                    break;
                    
                }
                case 4:  {
                    const reqId_137 = dataIn.readInt();
                    const argsNames_138: string[] = [];
                    const argsValues_139: any[] = [];
                    ctx.invokeLocalMethodBefore("getAllDevices", argsNames_138, argsValues_139);
                    ctx.regLocalFuture();
                    const resultFuture = localApi.getAllDevices();
                    ctx.invokeLocalMethodAfter("getAllDevices", resultFuture, argsNames_138, argsValues_139);
                    resultFuture.to((v_141: Device[]) =>  {
                        const data_140 = new DataInOut();
                        SerializerPackNumber.INSTANCE.put(data_140, v_141.length);
                        for (const el_142 of v_141)  {
                            Device.META.serialize(ctx, el_142, data_140);
                            
                        }
                        ctx.sendResultToRemote(reqId_137, data_140.toArray());
                        
                    }
                    );
                    break;
                    
                }
                case 5:  {
                    const reqId_143 = dataIn.readInt();
                    let commutatorId_144: UUID;
                    let localActorId_145: number;
                    let pkg_146: Uint8Array;
                    commutatorId_144 = FastMeta.META_UUID.deserialize(ctx, dataIn);
                    localActorId_145 = dataIn.readInt();
                    const len_148 = Number(DeserializerPackNumber.INSTANCE.put(dataIn).valueOf());
                    const bytes_149 = dataIn.readBytes(len_148);
                    pkg_146 = bytes_149;
                    const argsNames_150: string[] = ["commutatorId", "localActorId", "pkg"];
                    const argsValues_151: any[] = [commutatorId_144, localActorId_145, pkg_146];
                    ctx.invokeLocalMethodBefore("executeActorCommand", argsNames_150, argsValues_151);
                    ctx.regLocalFuture();
                    const resultFuture = localApi.executeActorCommand(commutatorId_144, localActorId_145, pkg_146);
                    ctx.invokeLocalMethodAfter("executeActorCommand", resultFuture, argsNames_150, argsValues_151);
                    resultFuture.to((v_153: Actor) =>  {
                        const data_152 = new DataInOut();
                        Actor.META.serialize(ctx, v_153, data_152);
                        ctx.sendResultToRemote(reqId_143, data_152.toArray());
                        
                    }
                    );
                    break;
                    
                }
                case 6:  {
                    const reqId_154 = dataIn.readInt();
                    const argsNames_155: string[] = [];
                    const argsValues_156: any[] = [];
                    ctx.invokeLocalMethodBefore("getPendingPairings", argsNames_155, argsValues_156);
                    ctx.regLocalFuture();
                    const resultFuture = localApi.getPendingPairings();
                    ctx.invokeLocalMethodAfter("getPendingPairings", resultFuture, argsNames_155, argsValues_156);
                    resultFuture.to((v_158: PendingPairing[]) =>  {
                        const data_157 = new DataInOut();
                        SerializerPackNumber.INSTANCE.put(data_157, v_158.length);
                        for (const el_159 of v_158)  {
                            PendingPairing.META.serialize(ctx, el_159, data_157);
                            
                        }
                        ctx.sendResultToRemote(reqId_154, data_157.toArray());
                        
                    }
                    );
                    break;
                    
                }
                case 7:  {
                    let commutatorUuid_161: UUID;
                    commutatorUuid_161 = FastMeta.META_UUID.deserialize(ctx, dataIn);
                    const argsNames_162: string[] = ["commutatorUuid"];
                    const argsValues_163: any[] = [commutatorUuid_161];
                    ctx.invokeLocalMethodBefore("approvePairing", argsNames_162, argsValues_163);
                    localApi.approvePairing(commutatorUuid_161);
                    ctx.invokeLocalMethodAfter("approvePairing", null, argsNames_162, argsValues_163);
                    break;
                    
                }
                case 8:  {
                    let localSensorId_165: number;
                    let data_166: DeviceStateData;
                    localSensorId_165 = dataIn.readInt();
                    data_166 = DeviceStateData.META.deserialize(ctx, dataIn);
                    const argsNames_167: string[] = ["localSensorId", "data"];
                    const argsValues_168: any[] = [localSensorId_165, data_166];
                    ctx.invokeLocalMethodBefore("pushSensorData", argsNames_167, argsValues_168);
                    localApi.pushSensorData(localSensorId_165, data_166);
                    ctx.invokeLocalMethodAfter("pushSensorData", null, argsNames_167, argsValues_168);
                    break;
                    
                }
                case 9:  {
                    const argsNames_170: string[] = [];
                    const argsValues_171: any[] = [];
                    ctx.invokeLocalMethodBefore("refreshAllSensorStates", argsNames_170, argsValues_171);
                    localApi.refreshAllSensorStates();
                    ctx.invokeLocalMethodAfter("refreshAllSensorStates", null, argsNames_170, argsValues_171);
                    break;
                    
                }
                default: throw new Error(`Unknown command ID: $ {
                    commandId
                }
                `);
                
            }
        }
        
    }
    makeLocal_fromBytes_ctxLocal(ctx: FastApiContextLocal<SmartHomeServiceApi>, data: Uint8Array): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), ctx.localApi);
        
    }
    makeLocal_fromBytes_ctx(ctx: FastFutureContext, data: Uint8Array, localApi: SmartHomeServiceApi): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), localApi);
        
    }
    makeRemote(sCtx_172: FastFutureContext): SmartHomeServiceApiRemote  {
        const remoteApiImpl =  {
            flush: (sendFuture?: AFuture) =>  {
                sCtx_172.flush(sendFuture || AFuture.make());
                
            }
            , getFastMetaContext: () => sCtx_172, register: (type: ClientType, sensors: HardwareSensor[], actors: HardwareActor[]): void =>  {
                const dataOut_174 = new DataInOut();
                dataOut_174.writeByte(3);
                const argsNames_176: string[] = ["type", "sensors", "actors"];
                const argsValues_177: any[] = [type, sensors, actors];
                sCtx_172.invokeRemoteMethodAfter("register", null, argsNames_176, argsValues_177);
                ClientType.META.serialize(sCtx_172, type, dataOut_174);
                SerializerPackNumber.INSTANCE.put(dataOut_174, sensors.length);
                for (const el_178 of sensors)  {
                    HardwareSensor.META.serialize(sCtx_172, el_178, dataOut_174);
                    
                }
                SerializerPackNumber.INSTANCE.put(dataOut_174, actors.length);
                for (const el_179 of actors)  {
                    HardwareActor.META.serialize(sCtx_172, el_179, dataOut_174);
                    
                }
                sCtx_172.sendToRemote(dataOut_174.toArray());
                
            }
            , getAllDevices: (): ARFuture<Device[]> =>  {
                const dataOut_181 = new DataInOut();
                dataOut_181.writeByte(4);
                const argsNames_183: string[] = [];
                const argsValues_184: any[] = [];
                const result_182 = ARFuture.of<Device[]>();
                sCtx_172.invokeRemoteMethodAfter("getAllDevices", result_182, argsNames_183, argsValues_184);
                const reqId_180 = sCtx_172.regFuture( {
                    onDone: (in_: DataIn) =>  {
                        (result_182 as ARFuture<Device[]>).tryDone(FastMeta.getMetaArray(Device.META).deserialize(sCtx_172, in_));
                        
                    }
                    , onError: (_in_: DataIn) =>  {
                        result_182.error(new Error("Remote call failed without a typed exception"));
                        
                    }
                    
                }
                );
                dataOut_181.writeInt(reqId_180);
                sCtx_172.sendToRemote(dataOut_181.toArray());
                return result_182;
                
            }
            , executeActorCommand: (commutatorId: UUID, localActorId: number, pkg: Uint8Array): ARFuture<Actor> =>  {
                const dataOut_186 = new DataInOut();
                dataOut_186.writeByte(5);
                const argsNames_188: string[] = ["commutatorId", "localActorId", "pkg"];
                const argsValues_189: any[] = [commutatorId, localActorId, pkg];
                const result_187 = ARFuture.of<Actor>();
                sCtx_172.invokeRemoteMethodAfter("executeActorCommand", result_187, argsNames_188, argsValues_189);
                const reqId_185 = sCtx_172.regFuture( {
                    onDone: (in_: DataIn) =>  {
                        (result_187 as ARFuture<Actor>).tryDone(Actor.META.deserialize(sCtx_172, in_));
                        
                    }
                    , onError: (_in_: DataIn) =>  {
                        result_187.error(new Error("Remote call failed without a typed exception"));
                        
                    }
                    
                }
                );
                dataOut_186.writeInt(reqId_185);
                FastMeta.META_UUID.serialize(sCtx_172, commutatorId, dataOut_186);
                dataOut_186.writeInt(localActorId);
                SerializerPackNumber.INSTANCE.put(dataOut_186, pkg.length);
                dataOut_186.write(pkg);
                sCtx_172.sendToRemote(dataOut_186.toArray());
                return result_187;
                
            }
            , getPendingPairings: (): ARFuture<PendingPairing[]> =>  {
                const dataOut_192 = new DataInOut();
                dataOut_192.writeByte(6);
                const argsNames_194: string[] = [];
                const argsValues_195: any[] = [];
                const result_193 = ARFuture.of<PendingPairing[]>();
                sCtx_172.invokeRemoteMethodAfter("getPendingPairings", result_193, argsNames_194, argsValues_195);
                const reqId_191 = sCtx_172.regFuture( {
                    onDone: (in_: DataIn) =>  {
                        (result_193 as ARFuture<PendingPairing[]>).tryDone(FastMeta.getMetaArray(PendingPairing.META).deserialize(sCtx_172, in_));
                        
                    }
                    , onError: (_in_: DataIn) =>  {
                        result_193.error(new Error("Remote call failed without a typed exception"));
                        
                    }
                    
                }
                );
                dataOut_192.writeInt(reqId_191);
                sCtx_172.sendToRemote(dataOut_192.toArray());
                return result_193;
                
            }
            , approvePairing: (commutatorUuid: UUID): void =>  {
                const dataOut_197 = new DataInOut();
                dataOut_197.writeByte(7);
                const argsNames_199: string[] = ["commutatorUuid"];
                const argsValues_200: any[] = [commutatorUuid];
                sCtx_172.invokeRemoteMethodAfter("approvePairing", null, argsNames_199, argsValues_200);
                FastMeta.META_UUID.serialize(sCtx_172, commutatorUuid, dataOut_197);
                sCtx_172.sendToRemote(dataOut_197.toArray());
                
            }
            , pushSensorData: (localSensorId: number, data: DeviceStateData): void =>  {
                const dataOut_202 = new DataInOut();
                dataOut_202.writeByte(8);
                const argsNames_204: string[] = ["localSensorId", "data"];
                const argsValues_205: any[] = [localSensorId, data];
                sCtx_172.invokeRemoteMethodAfter("pushSensorData", null, argsNames_204, argsValues_205);
                dataOut_202.writeInt(localSensorId);
                DeviceStateData.META.serialize(sCtx_172, data, dataOut_202);
                sCtx_172.sendToRemote(dataOut_202.toArray());
                
            }
            , refreshAllSensorStates: (): void =>  {
                const dataOut_207 = new DataInOut();
                dataOut_207.writeByte(9);
                const argsNames_209: string[] = [];
                const argsValues_210: any[] = [];
                sCtx_172.invokeRemoteMethodAfter("refreshAllSensorStates", null, argsNames_209, argsValues_210);
                sCtx_172.sendToRemote(dataOut_207.toArray());
                
            }
            , 
        };
        return remoteApiImpl as SmartHomeServiceApiRemote;
        
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
                case 3:  {
                    const argsNames_212: string[] = [];
                    const argsValues_213: any[] = [];
                    ctx.invokeLocalMethodBefore("confirmPairing", argsNames_212, argsValues_213);
                    localApi.confirmPairing();
                    ctx.invokeLocalMethodAfter("confirmPairing", null, argsNames_212, argsValues_213);
                    break;
                    
                }
                case 4:  {
                    const reqId_214 = dataIn.readInt();
                    let localActorId_215: number;
                    let pkg_216: Uint8Array;
                    localActorId_215 = dataIn.readInt();
                    const len_218 = Number(DeserializerPackNumber.INSTANCE.put(dataIn).valueOf());
                    const bytes_219 = dataIn.readBytes(len_218);
                    pkg_216 = bytes_219;
                    const argsNames_220: string[] = ["localActorId", "pkg"];
                    const argsValues_221: any[] = [localActorId_215, pkg_216];
                    ctx.invokeLocalMethodBefore("executeActorCommand", argsNames_220, argsValues_221);
                    ctx.regLocalFuture();
                    const resultFuture = localApi.executeActorCommand(localActorId_215, pkg_216);
                    ctx.invokeLocalMethodAfter("executeActorCommand", resultFuture, argsNames_220, argsValues_221);
                    resultFuture.to((v_223: DeviceStateData) =>  {
                        const data_222 = new DataInOut();
                        DeviceStateData.META.serialize(ctx, v_223, data_222);
                        ctx.sendResultToRemote(reqId_214, data_222.toArray());
                        
                    }
                    );
                    break;
                    
                }
                case 5:  {
                    const reqId_224 = dataIn.readInt();
                    let localDeviceId_225: number;
                    localDeviceId_225 = dataIn.readInt();
                    const argsNames_226: string[] = ["localDeviceId"];
                    const argsValues_227: any[] = [localDeviceId_225];
                    ctx.invokeLocalMethodBefore("queryState", argsNames_226, argsValues_227);
                    ctx.regLocalFuture();
                    const resultFuture = localApi.queryState(localDeviceId_225);
                    ctx.invokeLocalMethodAfter("queryState", resultFuture, argsNames_226, argsValues_227);
                    resultFuture.to((v_229: DeviceStateData) =>  {
                        const data_228 = new DataInOut();
                        DeviceStateData.META.serialize(ctx, v_229, data_228);
                        ctx.sendResultToRemote(reqId_224, data_228.toArray());
                        
                    }
                    );
                    break;
                    
                }
                case 6:  {
                    const argsNames_231: string[] = [];
                    const argsValues_232: any[] = [];
                    ctx.invokeLocalMethodBefore("queryAllSensorStates", argsNames_231, argsValues_232);
                    localApi.queryAllSensorStates();
                    ctx.invokeLocalMethodAfter("queryAllSensorStates", null, argsNames_231, argsValues_232);
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
    makeRemote(sCtx_233: FastFutureContext): SmartHomeCommutatorApiRemote  {
        const remoteApiImpl =  {
            flush: (sendFuture?: AFuture) =>  {
                sCtx_233.flush(sendFuture || AFuture.make());
                
            }
            , getFastMetaContext: () => sCtx_233, confirmPairing: (): void =>  {
                const dataOut_235 = new DataInOut();
                dataOut_235.writeByte(3);
                const argsNames_237: string[] = [];
                const argsValues_238: any[] = [];
                sCtx_233.invokeRemoteMethodAfter("confirmPairing", null, argsNames_237, argsValues_238);
                sCtx_233.sendToRemote(dataOut_235.toArray());
                
            }
            , executeActorCommand: (localActorId: number, pkg: Uint8Array): ARFuture<DeviceStateData> =>  {
                const dataOut_240 = new DataInOut();
                dataOut_240.writeByte(4);
                const argsNames_242: string[] = ["localActorId", "pkg"];
                const argsValues_243: any[] = [localActorId, pkg];
                const result_241 = ARFuture.of<DeviceStateData>();
                sCtx_233.invokeRemoteMethodAfter("executeActorCommand", result_241, argsNames_242, argsValues_243);
                const reqId_239 = sCtx_233.regFuture( {
                    onDone: (in_: DataIn) =>  {
                        (result_241 as ARFuture<DeviceStateData>).tryDone(DeviceStateData.META.deserialize(sCtx_233, in_));
                        
                    }
                    , onError: (_in_: DataIn) =>  {
                        result_241.error(new Error("Remote call failed without a typed exception"));
                        
                    }
                    
                }
                );
                dataOut_240.writeInt(reqId_239);
                dataOut_240.writeInt(localActorId);
                SerializerPackNumber.INSTANCE.put(dataOut_240, pkg.length);
                dataOut_240.write(pkg);
                sCtx_233.sendToRemote(dataOut_240.toArray());
                return result_241;
                
            }
            , queryState: (localDeviceId: number): ARFuture<DeviceStateData> =>  {
                const dataOut_246 = new DataInOut();
                dataOut_246.writeByte(5);
                const argsNames_248: string[] = ["localDeviceId"];
                const argsValues_249: any[] = [localDeviceId];
                const result_247 = ARFuture.of<DeviceStateData>();
                sCtx_233.invokeRemoteMethodAfter("queryState", result_247, argsNames_248, argsValues_249);
                const reqId_245 = sCtx_233.regFuture( {
                    onDone: (in_: DataIn) =>  {
                        (result_247 as ARFuture<DeviceStateData>).tryDone(DeviceStateData.META.deserialize(sCtx_233, in_));
                        
                    }
                    , onError: (_in_: DataIn) =>  {
                        result_247.error(new Error("Remote call failed without a typed exception"));
                        
                    }
                    
                }
                );
                dataOut_246.writeInt(reqId_245);
                dataOut_246.writeInt(localDeviceId);
                sCtx_233.sendToRemote(dataOut_246.toArray());
                return result_247;
                
            }
            , queryAllSensorStates: (): void =>  {
                const dataOut_251 = new DataInOut();
                dataOut_251.writeByte(6);
                const argsNames_253: string[] = [];
                const argsValues_254: any[] = [];
                sCtx_233.invokeRemoteMethodAfter("queryAllSensorStates", null, argsNames_253, argsValues_254);
                sCtx_233.sendToRemote(dataOut_251.toArray());
                
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
                    let device_256: Device;
                    device_256 = Device.META.deserialize(ctx, dataIn);
                    const argsNames_257: string[] = ["device"];
                    const argsValues_258: any[] = [device_256];
                    ctx.invokeLocalMethodBefore("deviceStateUpdated", argsNames_257, argsValues_258);
                    localApi.deviceStateUpdated(device_256);
                    ctx.invokeLocalMethodAfter("deviceStateUpdated", null, argsNames_257, argsValues_258);
                    break;
                    
                }
                case 4:  {
                    let pairingInfo_260: PendingPairing;
                    pairingInfo_260 = PendingPairing.META.deserialize(ctx, dataIn);
                    const argsNames_261: string[] = ["pairingInfo"];
                    const argsValues_262: any[] = [pairingInfo_260];
                    ctx.invokeLocalMethodBefore("pairingRequested", argsNames_261, argsValues_262);
                    localApi.pairingRequested(pairingInfo_260);
                    ctx.invokeLocalMethodAfter("pairingRequested", null, argsNames_261, argsValues_262);
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
    makeRemote(sCtx_263: FastFutureContext): SmartHomeClientApiRemote  {
        const remoteApiImpl =  {
            flush: (sendFuture?: AFuture) =>  {
                sCtx_263.flush(sendFuture || AFuture.make());
                
            }
            , getFastMetaContext: () => sCtx_263, deviceStateUpdated: (device: Device): void =>  {
                const dataOut_265 = new DataInOut();
                dataOut_265.writeByte(3);
                const argsNames_267: string[] = ["device"];
                const argsValues_268: any[] = [device];
                sCtx_263.invokeRemoteMethodAfter("deviceStateUpdated", null, argsNames_267, argsValues_268);
                Device.META.serialize(sCtx_263, device, dataOut_265);
                sCtx_263.sendToRemote(dataOut_265.toArray());
                
            }
            , pairingRequested: (pairingInfo: PendingPairing): void =>  {
                const dataOut_270 = new DataInOut();
                dataOut_270.writeByte(4);
                const argsNames_272: string[] = ["pairingInfo"];
                const argsValues_273: any[] = [pairingInfo];
                sCtx_263.invokeRemoteMethodAfter("pairingRequested", null, argsNames_272, argsValues_273);
                PendingPairing.META.serialize(sCtx_263, pairingInfo, dataOut_270);
                sCtx_263.sendToRemote(dataOut_270.toArray());
                
            }
            , 
        };
        return remoteApiImpl as SmartHomeClientApiRemote;
        
    }
    
}