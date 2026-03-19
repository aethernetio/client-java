import  {
    AFuture, ARFuture, DataIn, DataOut, DataInOut, DataInOutStatic, FastMetaType, FastFutureContext, FastMeta, SerializerPackNumber, DeserializerPackNumber, FastApiContextLocal, FastMetaApi, FastFutureContextStub, UUID, URI, AString, FlushReport
}
from 'aether-client';
import  {
    DeviceRecord, DeviceStream, GuiStream, SmartHomeHubRegistryApi, SmartHomeDeviceApi, SmartHomeGuiApi, SmartHomeClientGuiApi, SmartHomeHubRegistryApiRemote, SmartHomeDeviceApiRemote, SmartHomeGuiApiRemote, SmartHomeClientGuiApiRemote
}
from './aether_api';
// This is always relative
export class DeviceRecordMetaBodyImpl implements FastMetaType<DeviceRecord>  {
    serialize(sCtx_0: FastFutureContext, obj_1: DeviceRecord, _out_2: DataOut): void  {
        _out_2.writeShort(obj_1.value);
        _out_2.writeShort(obj_1.time);
        
    }
    deserialize(sCtx_0: FastFutureContext, in__3: DataIn): DeviceRecord  {
        let value_4: number;
        let time_5: number;
        value_4 = in__3.readShort();
        time_5 = in__3.readShort();
        return new DeviceRecord(value_4, time_5);
        
    }
    metaHashCode(obj: DeviceRecord | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_SHORT.metaHashCode(obj.value);
        hash = 37 * hash + FastMeta.META_SHORT.metaHashCode(obj.time);
        return hash | 0;
        
    }
    metaEquals(v1: DeviceRecord | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof DeviceRecord)) return false;
        if (!FastMeta.META_SHORT.metaEquals(v1.value, v2.value)) return false;
        if (!FastMeta.META_SHORT.metaEquals(v1.time, v2.time)) return false;
        return true;
        
    }
    metaToString(obj: DeviceRecord | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('DeviceRecord(');
        res.add('value:').add(obj.value);
        res.add(', ');
        res.add('time:').add(obj.time);
        res.add(')');
        
    }
    public serializeToBytes(obj: DeviceRecord): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): DeviceRecord  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): DeviceRecord  {
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
                    const reqId_25 = dataIn.readInt();
                    let deviceUid_26: UUID;
                    let value_27: DeviceRecord[];
                    deviceUid_26 = FastMeta.META_UUID.deserialize(ctx, dataIn);
                    const len_29 = Number(DeserializerPackNumber.INSTANCE.put(dataIn));
                    value_27 = new Array<DeviceRecord>(len_29);
                    for (let idx_28 = 0;
                    idx_28 < len_29;
                    idx_28++)  {
                        value_27[idx_28] = DeviceRecord.META.deserialize(ctx, dataIn);
                        
                    }
                    const argsNames_30: string[] = ["deviceUid", "value"];
                    const argsValues_31: any[] = [deviceUid_26, value_27];
                    ctx.invokeLocalMethodBefore("reportState", argsNames_30, argsValues_31);
                    ctx.regLocalFuture();
                    const resultFuture = localApi.reportState(deviceUid_26, value_27);
                    ctx.invokeLocalMethodAfter("reportState", resultFuture, argsNames_30, argsValues_31);
                    resultFuture.to((v_33: boolean) =>  {
                        const data_32 = new DataInOut();
                        data_32.writeBoolean(v_33);
                        ctx.sendResultToRemote(reqId_25, data_32.toArray());
                        
                    }
                    );
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
    makeRemote(sCtx_34: FastFutureContext): SmartHomeDeviceApiRemote  {
        const remoteApiImpl =  {
            flush: (sendFuture: FlushReport): void =>  {
                sCtx_34.flush(sendFuture);
                
            }
            , getFastMetaContext: () => sCtx_34, reportState: (deviceUid: UUID, value: DeviceRecord[]): ARFuture<boolean> =>  {
                const dataOut_36 = new DataInOut();
                dataOut_36.writeByte(10);
                const argsNames_38: string[] = ["deviceUid", "value"];
                const argsValues_39: any[] = [deviceUid, value];
                const result_37 = ARFuture.of<boolean>();
                sCtx_34.invokeRemoteMethodAfter("reportState", result_37, argsNames_38, argsValues_39);
                const reqId_35 = sCtx_34.regFuture( {
                    onDone: (in_: DataIn) =>  {
                        (result_37 as ARFuture<boolean>).tryDone(FastMeta.META_BOOLEAN.deserialize(sCtx_34, in_));
                        
                    }
                    , onError: (_in_: DataIn) =>  {
                        result_37.error(new Error("Remote call failed without a typed exception"));
                        
                    }
                    
                }
                );
                dataOut_36.writeInt(reqId_35);
                FastMeta.META_UUID.serialize(sCtx_34, deviceUid, dataOut_36);
                SerializerPackNumber.INSTANCE.put(dataOut_36, value.length);
                for (const el_40 of value)  {
                    DeviceRecord.META.serialize(sCtx_34, el_40, dataOut_36);
                    
                }
                sCtx_34.sendToRemote(dataOut_36.toArray());
                return result_37;
                
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
                case 34:  {
                    const reqId_41 = dataIn.readInt();
                    const argsNames_42: string[] = [];
                    const argsValues_43: any[] = [];
                    ctx.invokeLocalMethodBefore("getDevices", argsNames_42, argsValues_43);
                    ctx.regLocalFuture();
                    const resultFuture = localApi.getDevices();
                    ctx.invokeLocalMethodAfter("getDevices", resultFuture, argsNames_42, argsValues_43);
                    resultFuture.to((v_45: UUID[]) =>  {
                        const data_44 = new DataInOut();
                        SerializerPackNumber.INSTANCE.put(data_44, v_45.length);
                        for (const el_46 of v_45)  {
                            FastMeta.META_UUID.serialize(ctx, el_46, data_44);
                            
                        }
                        ctx.sendResultToRemote(reqId_41, data_44.toArray());
                        
                    }
                    );
                    break;
                    
                }
                case 32:  {
                    const reqId_47 = dataIn.readInt();
                    let deviceUid_48: UUID;
                    deviceUid_48 = FastMeta.META_UUID.deserialize(ctx, dataIn);
                    const argsNames_49: string[] = ["deviceUid"];
                    const argsValues_50: any[] = [deviceUid_48];
                    ctx.invokeLocalMethodBefore("subscribeToDevice", argsNames_49, argsValues_50);
                    ctx.regLocalFuture();
                    const resultFuture = localApi.subscribeToDevice(deviceUid_48);
                    ctx.invokeLocalMethodAfter("subscribeToDevice", resultFuture, argsNames_49, argsValues_50);
                    resultFuture.to((v_52: boolean) =>  {
                        const data_51 = new DataInOut();
                        data_51.writeBoolean(v_52);
                        ctx.sendResultToRemote(reqId_47, data_51.toArray());
                        
                    }
                    );
                    break;
                    
                }
                case 33:  {
                    const reqId_53 = dataIn.readInt();
                    let deviceUid_54: UUID;
                    deviceUid_54 = FastMeta.META_UUID.deserialize(ctx, dataIn);
                    const argsNames_55: string[] = ["deviceUid"];
                    const argsValues_56: any[] = [deviceUid_54];
                    ctx.invokeLocalMethodBefore("unsubscribeFromDevice", argsNames_55, argsValues_56);
                    ctx.regLocalFuture();
                    const resultFuture = localApi.unsubscribeFromDevice(deviceUid_54);
                    ctx.invokeLocalMethodAfter("unsubscribeFromDevice", resultFuture, argsNames_55, argsValues_56);
                    resultFuture.to((v_58: boolean) =>  {
                        const data_57 = new DataInOut();
                        data_57.writeBoolean(v_58);
                        ctx.sendResultToRemote(reqId_53, data_57.toArray());
                        
                    }
                    );
                    break;
                    
                }
                case 31:  {
                    const reqId_59 = dataIn.readInt();
                    let deviceUid_60: UUID;
                    let count_61: bigint;
                    deviceUid_60 = FastMeta.META_UUID.deserialize(ctx, dataIn);
                    count_61 = DeserializerPackNumber.INSTANCE.put(dataIn);
                    const argsNames_62: string[] = ["deviceUid", "count"];
                    const argsValues_63: any[] = [deviceUid_60, count_61];
                    ctx.invokeLocalMethodBefore("requestDeviceHistory", argsNames_62, argsValues_63);
                    ctx.regLocalFuture();
                    const resultFuture = localApi.requestDeviceHistory(deviceUid_60, count_61);
                    ctx.invokeLocalMethodAfter("requestDeviceHistory", resultFuture, argsNames_62, argsValues_63);
                    resultFuture.to((v_65: DeviceRecord[]) =>  {
                        const data_64 = new DataInOut();
                        SerializerPackNumber.INSTANCE.put(data_64, v_65.length);
                        for (const el_66 of v_65)  {
                            DeviceRecord.META.serialize(ctx, el_66, data_64);
                            
                        }
                        ctx.sendResultToRemote(reqId_59, data_64.toArray());
                        
                    }
                    );
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
    makeRemote(sCtx_67: FastFutureContext): SmartHomeGuiApiRemote  {
        const remoteApiImpl =  {
            flush: (sendFuture: FlushReport): void =>  {
                sCtx_67.flush(sendFuture);
                
            }
            , getFastMetaContext: () => sCtx_67, getDevices: (): ARFuture<UUID[]> =>  {
                const dataOut_69 = new DataInOut();
                dataOut_69.writeByte(34);
                const argsNames_71: string[] = [];
                const argsValues_72: any[] = [];
                const result_70 = ARFuture.of<UUID[]>();
                sCtx_67.invokeRemoteMethodAfter("getDevices", result_70, argsNames_71, argsValues_72);
                const reqId_68 = sCtx_67.regFuture( {
                    onDone: (in_: DataIn) =>  {
                        (result_70 as ARFuture<UUID[]>).tryDone(FastMeta.getMetaArray(FastMeta.META_UUID).deserialize(sCtx_67, in_));
                        
                    }
                    , onError: (_in_: DataIn) =>  {
                        result_70.error(new Error("Remote call failed without a typed exception"));
                        
                    }
                    
                }
                );
                dataOut_69.writeInt(reqId_68);
                sCtx_67.sendToRemote(dataOut_69.toArray());
                return result_70;
                
            }
            , subscribeToDevice: (deviceUid: UUID): ARFuture<boolean> =>  {
                const dataOut_74 = new DataInOut();
                dataOut_74.writeByte(32);
                const argsNames_76: string[] = ["deviceUid"];
                const argsValues_77: any[] = [deviceUid];
                const result_75 = ARFuture.of<boolean>();
                sCtx_67.invokeRemoteMethodAfter("subscribeToDevice", result_75, argsNames_76, argsValues_77);
                const reqId_73 = sCtx_67.regFuture( {
                    onDone: (in_: DataIn) =>  {
                        (result_75 as ARFuture<boolean>).tryDone(FastMeta.META_BOOLEAN.deserialize(sCtx_67, in_));
                        
                    }
                    , onError: (_in_: DataIn) =>  {
                        result_75.error(new Error("Remote call failed without a typed exception"));
                        
                    }
                    
                }
                );
                dataOut_74.writeInt(reqId_73);
                FastMeta.META_UUID.serialize(sCtx_67, deviceUid, dataOut_74);
                sCtx_67.sendToRemote(dataOut_74.toArray());
                return result_75;
                
            }
            , unsubscribeFromDevice: (deviceUid: UUID): ARFuture<boolean> =>  {
                const dataOut_79 = new DataInOut();
                dataOut_79.writeByte(33);
                const argsNames_81: string[] = ["deviceUid"];
                const argsValues_82: any[] = [deviceUid];
                const result_80 = ARFuture.of<boolean>();
                sCtx_67.invokeRemoteMethodAfter("unsubscribeFromDevice", result_80, argsNames_81, argsValues_82);
                const reqId_78 = sCtx_67.regFuture( {
                    onDone: (in_: DataIn) =>  {
                        (result_80 as ARFuture<boolean>).tryDone(FastMeta.META_BOOLEAN.deserialize(sCtx_67, in_));
                        
                    }
                    , onError: (_in_: DataIn) =>  {
                        result_80.error(new Error("Remote call failed without a typed exception"));
                        
                    }
                    
                }
                );
                dataOut_79.writeInt(reqId_78);
                FastMeta.META_UUID.serialize(sCtx_67, deviceUid, dataOut_79);
                sCtx_67.sendToRemote(dataOut_79.toArray());
                return result_80;
                
            }
            , requestDeviceHistory: (deviceUid: UUID, count: bigint): ARFuture<DeviceRecord[]> =>  {
                const dataOut_84 = new DataInOut();
                dataOut_84.writeByte(31);
                const argsNames_86: string[] = ["deviceUid", "count"];
                const argsValues_87: any[] = [deviceUid, count];
                const result_85 = ARFuture.of<DeviceRecord[]>();
                sCtx_67.invokeRemoteMethodAfter("requestDeviceHistory", result_85, argsNames_86, argsValues_87);
                const reqId_83 = sCtx_67.regFuture( {
                    onDone: (in_: DataIn) =>  {
                        (result_85 as ARFuture<DeviceRecord[]>).tryDone(FastMeta.getMetaArray(DeviceRecord.META).deserialize(sCtx_67, in_));
                        
                    }
                    , onError: (_in_: DataIn) =>  {
                        result_85.error(new Error("Remote call failed without a typed exception"));
                        
                    }
                    
                }
                );
                dataOut_84.writeInt(reqId_83);
                FastMeta.META_UUID.serialize(sCtx_67, deviceUid, dataOut_84);
                SerializerPackNumber.INSTANCE.put(dataOut_84, count);
                sCtx_67.sendToRemote(dataOut_84.toArray());
                return result_85;
                
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
                    let deviceUid_89: UUID;
                    let records_90: DeviceRecord[];
                    deviceUid_89 = FastMeta.META_UUID.deserialize(ctx, dataIn);
                    const len_92 = Number(DeserializerPackNumber.INSTANCE.put(dataIn));
                    records_90 = new Array<DeviceRecord>(len_92);
                    for (let idx_91 = 0;
                    idx_91 < len_92;
                    idx_91++)  {
                        records_90[idx_91] = DeviceRecord.META.deserialize(ctx, dataIn);
                        
                    }
                    const argsNames_93: string[] = ["deviceUid", "records"];
                    const argsValues_94: any[] = [deviceUid_89, records_90];
                    ctx.invokeLocalMethodBefore("deviceStateUpdated", argsNames_93, argsValues_94);
                    localApi.deviceStateUpdated(deviceUid_89, records_90);
                    ctx.invokeLocalMethodAfter("deviceStateUpdated", null, argsNames_93, argsValues_94);
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
    makeRemote(sCtx_95: FastFutureContext): SmartHomeClientGuiApiRemote  {
        const remoteApiImpl =  {
            flush: (sendFuture: FlushReport): void =>  {
                sCtx_95.flush(sendFuture);
                
            }
            , getFastMetaContext: () => sCtx_95, deviceStateUpdated: (deviceUid: UUID, records: DeviceRecord[]): void =>  {
                const dataOut_97 = new DataInOut();
                dataOut_97.writeByte(20);
                const argsNames_99: string[] = ["deviceUid", "records"];
                const argsValues_100: any[] = [deviceUid, records];
                sCtx_95.invokeRemoteMethodAfter("deviceStateUpdated", null, argsNames_99, argsValues_100);
                FastMeta.META_UUID.serialize(sCtx_95, deviceUid, dataOut_97);
                SerializerPackNumber.INSTANCE.put(dataOut_97, records.length);
                for (const el_101 of records)  {
                    DeviceRecord.META.serialize(sCtx_95, el_101, dataOut_97);
                    
                }
                sCtx_95.sendToRemote(dataOut_97.toArray());
                
            }
            , 
        };
        return remoteApiImpl as SmartHomeClientGuiApiRemote;
        
    }
    
}