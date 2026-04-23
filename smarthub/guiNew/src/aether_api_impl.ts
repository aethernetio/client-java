import  {
    AFuture, ARFuture, DataIn, DataOut, DataInOut, DataInOutStatic, FastMetaType, FastFutureContext, FastMeta, SerializerPackNumber, DeserializerPackNumber, FastApiContextLocal, FastMetaApi, FastFutureContextStub, UUID, URI, AString, FlushReport
}
from 'aether-client';
import  {
    SensorRecord, DeviceStream, GuiStream, SmartHomeHubRegistryApi, SmartHomeDeviceApi, SmartHomeGuiApi, SmartHomeClientGuiApi, SmartHomeHubRegistryApiRemote, SmartHomeDeviceApiRemote, SmartHomeGuiApiRemote, SmartHomeClientGuiApiRemote
}
from './aether_api';
// This is always relative
export class SensorRecordMetaBodyImpl implements FastMetaType<SensorRecord>  {
    serialize(sCtx_0: FastFutureContext, obj_1: SensorRecord, _out_2: DataOut): void  {
        _out_2.writeByte(obj_1.value);
        _out_2.writeByte(obj_1.time);
        
    }
    deserialize(sCtx_0: FastFutureContext, in__3: DataIn): SensorRecord  {
        let value_4: number;
        let time_5: number;
        value_4 = in__3.readByte();
        time_5 = in__3.readByte();
        return new SensorRecord(value_4, time_5);
        
    }
    metaHashCode(obj: SensorRecord | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_BYTE.metaHashCode(obj.value);
        hash = 37 * hash + FastMeta.META_BYTE.metaHashCode(obj.time);
        return hash | 0;
        
    }
    metaEquals(v1: SensorRecord | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof SensorRecord)) return false;
        if (!FastMeta.META_BYTE.metaEquals(v1.value, v2.value)) return false;
        if (!FastMeta.META_BYTE.metaEquals(v1.time, v2.time)) return false;
        return true;
        
    }
    metaToString(obj: SensorRecord | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('SensorRecord(');
        res.add('value:').add(obj.value);
        res.add(', ');
        res.add('time:').add(obj.time);
        res.add(')');
        
    }
    public serializeToBytes(obj: SensorRecord): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): SensorRecord  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): SensorRecord  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class DeviceStreamMetaImpl implements FastMetaType<DeviceStream>  {
    serialize(ctx: FastFutureContext, obj: DeviceStream, out: DataOut): void  {
        FastMeta.META_ARRAY_BYTE.serialize(ctx, obj.data, out);
        
    }
    deserialize(ctx: FastFutureContext, in_: DataIn): DeviceStream  {
        return new DeviceStream(FastMeta.META_ARRAY_BYTE.deserialize(ctx, in_));
        
    }
    metaHashCode(obj: DeviceStream | null | undefined): number  {
        return FastMeta.META_ARRAY_BYTE.metaHashCode(obj?.data);
        
    }
    metaEquals(v1: DeviceStream | null | undefined, v2: any | null | undefined): boolean  {
        return FastMeta.META_ARRAY_BYTE.metaEquals(v1?.data, (v2 instanceof DeviceStream) ? v2.data : v2);
        
    }
    metaToString(obj: DeviceStream | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('DeviceStream(').add('data:').add(obj.data).add(')');
        
    }
    public serializeToBytes(obj: DeviceStream): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): DeviceStream  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): DeviceStream  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class GuiStreamMetaImpl implements FastMetaType<GuiStream>  {
    serialize(ctx: FastFutureContext, obj: GuiStream, out: DataOut): void  {
        FastMeta.META_ARRAY_BYTE.serialize(ctx, obj.data, out);
        
    }
    deserialize(ctx: FastFutureContext, in_: DataIn): GuiStream  {
        return new GuiStream(FastMeta.META_ARRAY_BYTE.deserialize(ctx, in_));
        
    }
    metaHashCode(obj: GuiStream | null | undefined): number  {
        return FastMeta.META_ARRAY_BYTE.metaHashCode(obj?.data);
        
    }
    metaEquals(v1: GuiStream | null | undefined, v2: any | null | undefined): boolean  {
        return FastMeta.META_ARRAY_BYTE.metaEquals(v1?.data, (v2 instanceof GuiStream) ? v2.data : v2);
        
    }
    metaToString(obj: GuiStream | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('GuiStream(').add('data:').add(obj.data).add(')');
        
    }
    public serializeToBytes(obj: GuiStream): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): GuiStream  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): GuiStream  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class SmartHomeHubRegistryApiMetaImpl implements FastMetaApi<SmartHomeHubRegistryApi, SmartHomeHubRegistryApiRemote>  {
    makeLocal_fromDataIn(ctx: FastFutureContext, dataIn: DataIn, localApi: SmartHomeHubRegistryApi): void  {
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
                    let stream_7: DeviceStream;
                    stream_7 = DeviceStream.META.deserialize(ctx, dataIn);
                    const argsNames_8: string[] = ["stream"];
                    const argsValues_9: any[] = [stream_7];
                    ctx.invokeLocalMethodBefore("device", argsNames_8, argsValues_9);
                    localApi.device(stream_7);
                    ctx.invokeLocalMethodAfter("device", null, argsNames_8, argsValues_9);
                    break;
                    
                }
                case 4:  {
                    let stream_11: GuiStream;
                    stream_11 = GuiStream.META.deserialize(ctx, dataIn);
                    const argsNames_12: string[] = ["stream"];
                    const argsValues_13: any[] = [stream_11];
                    ctx.invokeLocalMethodBefore("gui", argsNames_12, argsValues_13);
                    localApi.gui(stream_11);
                    ctx.invokeLocalMethodAfter("gui", null, argsNames_12, argsValues_13);
                    break;
                    
                }
                default: throw new Error(`Unknown command ID: $ {
                    commandId
                }
                `);
                
            }
        }
        
    }
    makeLocal_fromBytes_ctxLocal(ctx: FastApiContextLocal<SmartHomeHubRegistryApi>, data: Uint8Array): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), ctx.localApi);
        
    }
    makeLocal_fromBytes_ctx(ctx: FastFutureContext, data: Uint8Array, localApi: SmartHomeHubRegistryApi): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), localApi);
        
    }
    makeRemote(sCtx_14: FastFutureContext): SmartHomeHubRegistryApiRemote  {
        const remoteApiImpl =  {
            flush: (sendFuture: FlushReport): void =>  {
                sCtx_14.flush(sendFuture);
                
            }
            , getFastMetaContext: () => sCtx_14, device: (stream: DeviceStream): void =>  {
                const dataOut_16 = new DataInOut();
                dataOut_16.writeByte(3);
                const argsNames_18: string[] = ["stream"];
                const argsValues_19: any[] = [stream];
                sCtx_14.invokeRemoteMethodAfter("device", null, argsNames_18, argsValues_19);
                DeviceStream.META.serialize(sCtx_14, stream, dataOut_16);
                sCtx_14.sendToRemote(dataOut_16.toArray());
                
            }
            , gui: (stream: GuiStream): void =>  {
                const dataOut_21 = new DataInOut();
                dataOut_21.writeByte(4);
                const argsNames_23: string[] = ["stream"];
                const argsValues_24: any[] = [stream];
                sCtx_14.invokeRemoteMethodAfter("gui", null, argsNames_23, argsValues_24);
                GuiStream.META.serialize(sCtx_14, stream, dataOut_21);
                sCtx_14.sendToRemote(dataOut_21.toArray());
                
            }
            , 
        };
        return remoteApiImpl as SmartHomeHubRegistryApiRemote;
        
    }
    
}
export class SmartHomeDeviceApiMetaImpl implements FastMetaApi<SmartHomeDeviceApi, SmartHomeDeviceApiRemote>  {
    makeLocal_fromDataIn(ctx: FastFutureContext, dataIn: DataIn, localApi: SmartHomeDeviceApi): void  {
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
                    let deviceUid_26: UUID;
                    let value_27: SensorRecord[];
                    deviceUid_26 = FastMeta.META_UUID.deserialize(ctx, dataIn);
                    const len_29 = Number(DeserializerPackNumber.INSTANCE.put(dataIn));
                    value_27 = new Array<SensorRecord>(len_29);
                    for (let idx_28 = 0;
                    idx_28 < len_29;
                    idx_28++)  {
                        value_27[idx_28] = SensorRecord.META.deserialize(ctx, dataIn);
                        
                    }
                    const argsNames_30: string[] = ["deviceUid", "value"];
                    const argsValues_31: any[] = [deviceUid_26, value_27];
                    ctx.invokeLocalMethodBefore("reportState", argsNames_30, argsValues_31);
                    localApi.reportState(deviceUid_26, value_27);
                    ctx.invokeLocalMethodAfter("reportState", null, argsNames_30, argsValues_31);
                    break;
                    
                }
                default: throw new Error(`Unknown command ID: $ {
                    commandId
                }
                `);
                
            }
        }
        
    }
    makeLocal_fromBytes_ctxLocal(ctx: FastApiContextLocal<SmartHomeDeviceApi>, data: Uint8Array): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), ctx.localApi);
        
    }
    makeLocal_fromBytes_ctx(ctx: FastFutureContext, data: Uint8Array, localApi: SmartHomeDeviceApi): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), localApi);
        
    }
    makeRemote(sCtx_32: FastFutureContext): SmartHomeDeviceApiRemote  {
        const remoteApiImpl =  {
            flush: (sendFuture: FlushReport): void =>  {
                sCtx_32.flush(sendFuture);
                
            }
            , getFastMetaContext: () => sCtx_32, reportState: (deviceUid: UUID, value: SensorRecord[]): void =>  {
                const dataOut_34 = new DataInOut();
                dataOut_34.writeByte(10);
                const argsNames_36: string[] = ["deviceUid", "value"];
                const argsValues_37: any[] = [deviceUid, value];
                sCtx_32.invokeRemoteMethodAfter("reportState", null, argsNames_36, argsValues_37);
                FastMeta.META_UUID.serialize(sCtx_32, deviceUid, dataOut_34);
                SerializerPackNumber.INSTANCE.put(dataOut_34, value.length);
                for (const el_38 of value)  {
                    SensorRecord.META.serialize(sCtx_32, el_38, dataOut_34);
                    
                }
                sCtx_32.sendToRemote(dataOut_34.toArray());
                
            }
            , 
        };
        return remoteApiImpl as SmartHomeDeviceApiRemote;
        
    }
    
}
export class SmartHomeGuiApiMetaImpl implements FastMetaApi<SmartHomeGuiApi, SmartHomeGuiApiRemote>  {
    makeLocal_fromDataIn(ctx: FastFutureContext, dataIn: DataIn, localApi: SmartHomeGuiApi): void  {
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
                case 12:  {
                    const argsNames_40: string[] = [];
                    const argsValues_41: any[] = [];
                    ctx.invokeLocalMethodBefore("getDevices", argsNames_40, argsValues_41);
                    localApi.getDevices();
                    ctx.invokeLocalMethodAfter("getDevices", null, argsNames_40, argsValues_41);
                    break;
                    
                }
                case 15:  {
                    let deviceUid_43: UUID;
                    let count_44: bigint;
                    deviceUid_43 = FastMeta.META_UUID.deserialize(ctx, dataIn);
                    count_44 = DeserializerPackNumber.INSTANCE.put(dataIn);
                    const argsNames_45: string[] = ["deviceUid", "count"];
                    const argsValues_46: any[] = [deviceUid_43, count_44];
                    ctx.invokeLocalMethodBefore("requestDeviceHistory", argsNames_45, argsValues_46);
                    localApi.requestDeviceHistory(deviceUid_43, count_44);
                    ctx.invokeLocalMethodAfter("requestDeviceHistory", null, argsNames_45, argsValues_46);
                    break;
                    
                }
                default: throw new Error(`Unknown command ID: $ {
                    commandId
                }
                `);
                
            }
        }
        
    }
    makeLocal_fromBytes_ctxLocal(ctx: FastApiContextLocal<SmartHomeGuiApi>, data: Uint8Array): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), ctx.localApi);
        
    }
    makeLocal_fromBytes_ctx(ctx: FastFutureContext, data: Uint8Array, localApi: SmartHomeGuiApi): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), localApi);
        
    }
    makeRemote(sCtx_47: FastFutureContext): SmartHomeGuiApiRemote  {
        const remoteApiImpl =  {
            flush: (sendFuture: FlushReport): void =>  {
                sCtx_47.flush(sendFuture);
                
            }
            , getFastMetaContext: () => sCtx_47, getDevices: (): void =>  {
                const dataOut_49 = new DataInOut();
                dataOut_49.writeByte(12);
                const argsNames_51: string[] = [];
                const argsValues_52: any[] = [];
                sCtx_47.invokeRemoteMethodAfter("getDevices", null, argsNames_51, argsValues_52);
                sCtx_47.sendToRemote(dataOut_49.toArray());
                
            }
            , requestDeviceHistory: (deviceUid: UUID, count: bigint): void =>  {
                const dataOut_54 = new DataInOut();
                dataOut_54.writeByte(15);
                const argsNames_56: string[] = ["deviceUid", "count"];
                const argsValues_57: any[] = [deviceUid, count];
                sCtx_47.invokeRemoteMethodAfter("requestDeviceHistory", null, argsNames_56, argsValues_57);
                FastMeta.META_UUID.serialize(sCtx_47, deviceUid, dataOut_54);
                SerializerPackNumber.INSTANCE.put(dataOut_54, count);
                sCtx_47.sendToRemote(dataOut_54.toArray());
                
            }
            , 
        };
        return remoteApiImpl as SmartHomeGuiApiRemote;
        
    }
    
}
export class SmartHomeClientGuiApiMetaImpl implements FastMetaApi<SmartHomeClientGuiApi, SmartHomeClientGuiApiRemote>  {
    makeLocal_fromDataIn(ctx: FastFutureContext, dataIn: DataIn, localApi: SmartHomeClientGuiApi): void  {
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
                case 20:  {
                    let deviceUid_59: UUID;
                    let records_60: SensorRecord[];
                    deviceUid_59 = FastMeta.META_UUID.deserialize(ctx, dataIn);
                    const len_62 = Number(DeserializerPackNumber.INSTANCE.put(dataIn));
                    records_60 = new Array<SensorRecord>(len_62);
                    for (let idx_61 = 0;
                    idx_61 < len_62;
                    idx_61++)  {
                        records_60[idx_61] = SensorRecord.META.deserialize(ctx, dataIn);
                        
                    }
                    const argsNames_63: string[] = ["deviceUid", "records"];
                    const argsValues_64: any[] = [deviceUid_59, records_60];
                    ctx.invokeLocalMethodBefore("deviceStateUpdated", argsNames_63, argsValues_64);
                    localApi.deviceStateUpdated(deviceUid_59, records_60);
                    ctx.invokeLocalMethodAfter("deviceStateUpdated", null, argsNames_63, argsValues_64);
                    break;
                    
                }
                case 21:  {
                    let devices_66: UUID[];
                    const len_68 = Number(DeserializerPackNumber.INSTANCE.put(dataIn));
                    devices_66 = new Array<UUID>(len_68);
                    for (let idx_67 = 0;
                    idx_67 < len_68;
                    idx_67++)  {
                        devices_66[idx_67] = FastMeta.META_UUID.deserialize(ctx, dataIn);
                        
                    }
                    const argsNames_69: string[] = ["devices"];
                    const argsValues_70: any[] = [devices_66];
                    ctx.invokeLocalMethodBefore("onGetDevicesResult", argsNames_69, argsValues_70);
                    localApi.onGetDevicesResult(devices_66);
                    ctx.invokeLocalMethodAfter("onGetDevicesResult", null, argsNames_69, argsValues_70);
                    break;
                    
                }
                case 24:  {
                    let deviceUid_72: UUID;
                    let records_73: SensorRecord[];
                    deviceUid_72 = FastMeta.META_UUID.deserialize(ctx, dataIn);
                    const len_75 = Number(DeserializerPackNumber.INSTANCE.put(dataIn));
                    records_73 = new Array<SensorRecord>(len_75);
                    for (let idx_74 = 0;
                    idx_74 < len_75;
                    idx_74++)  {
                        records_73[idx_74] = SensorRecord.META.deserialize(ctx, dataIn);
                        
                    }
                    const argsNames_76: string[] = ["deviceUid", "records"];
                    const argsValues_77: any[] = [deviceUid_72, records_73];
                    ctx.invokeLocalMethodBefore("onRequestHistoryResult", argsNames_76, argsValues_77);
                    localApi.onRequestHistoryResult(deviceUid_72, records_73);
                    ctx.invokeLocalMethodAfter("onRequestHistoryResult", null, argsNames_76, argsValues_77);
                    break;
                    
                }
                default: throw new Error(`Unknown command ID: $ {
                    commandId
                }
                `);
                
            }
        }
        
    }
    makeLocal_fromBytes_ctxLocal(ctx: FastApiContextLocal<SmartHomeClientGuiApi>, data: Uint8Array): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), ctx.localApi);
        
    }
    makeLocal_fromBytes_ctx(ctx: FastFutureContext, data: Uint8Array, localApi: SmartHomeClientGuiApi): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), localApi);
        
    }
    makeRemote(sCtx_78: FastFutureContext): SmartHomeClientGuiApiRemote  {
        const remoteApiImpl =  {
            flush: (sendFuture: FlushReport): void =>  {
                sCtx_78.flush(sendFuture);
                
            }
            , getFastMetaContext: () => sCtx_78, deviceStateUpdated: (deviceUid: UUID, records: SensorRecord[]): void =>  {
                const dataOut_80 = new DataInOut();
                dataOut_80.writeByte(20);
                const argsNames_82: string[] = ["deviceUid", "records"];
                const argsValues_83: any[] = [deviceUid, records];
                sCtx_78.invokeRemoteMethodAfter("deviceStateUpdated", null, argsNames_82, argsValues_83);
                FastMeta.META_UUID.serialize(sCtx_78, deviceUid, dataOut_80);
                SerializerPackNumber.INSTANCE.put(dataOut_80, records.length);
                for (const el_84 of records)  {
                    SensorRecord.META.serialize(sCtx_78, el_84, dataOut_80);
                    
                }
                sCtx_78.sendToRemote(dataOut_80.toArray());
                
            }
            , onGetDevicesResult: (devices: UUID[]): void =>  {
                const dataOut_86 = new DataInOut();
                dataOut_86.writeByte(21);
                const argsNames_88: string[] = ["devices"];
                const argsValues_89: any[] = [devices];
                sCtx_78.invokeRemoteMethodAfter("onGetDevicesResult", null, argsNames_88, argsValues_89);
                SerializerPackNumber.INSTANCE.put(dataOut_86, devices.length);
                for (const el_90 of devices)  {
                    FastMeta.META_UUID.serialize(sCtx_78, el_90, dataOut_86);
                    
                }
                sCtx_78.sendToRemote(dataOut_86.toArray());
                
            }
            , onRequestHistoryResult: (deviceUid: UUID, records: SensorRecord[]): void =>  {
                const dataOut_92 = new DataInOut();
                dataOut_92.writeByte(24);
                const argsNames_94: string[] = ["deviceUid", "records"];
                const argsValues_95: any[] = [deviceUid, records];
                sCtx_78.invokeRemoteMethodAfter("onRequestHistoryResult", null, argsNames_94, argsValues_95);
                FastMeta.META_UUID.serialize(sCtx_78, deviceUid, dataOut_92);
                SerializerPackNumber.INSTANCE.put(dataOut_92, records.length);
                for (const el_96 of records)  {
                    SensorRecord.META.serialize(sCtx_78, el_96, dataOut_92);
                    
                }
                sCtx_78.sendToRemote(dataOut_92.toArray());
                
            }
            , 
        };
        return remoteApiImpl as SmartHomeClientGuiApiRemote;
        
    }
    
}