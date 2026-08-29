import  {
    AFuture, ARFuture, DataIn, DataOut, DataInOut, DataInOutStatic, FastMetaType, MetaContext, FastMeta, SerializerPackNumber, DeserializerPackNumber, RemoteApi, FastMetaApi, FastFutureContextStub, SecurityConnectionDropException, AetherException, UUID, URI, AString, BytesConverter,
}
from 'aether-client';
import  {
    SensorRecord, DeviceStream, GuiStream, SmartHomeHubRegistryApiDeviceArguments, SmartHomeHubRegistryApiGuiArguments, SmartHomeDeviceApiReportStateArguments, SmartHomeGuiApiGetDevicesArguments, SmartHomeGuiApiRequestDeviceHistoryArguments, SmartHomeClientGuiApiDeviceStateUpdatedArguments, SmartHomeClientGuiApiOnGetDevicesResultArguments, SmartHomeClientGuiApiOnRequestHistoryResultArguments, SmartHomeHubRegistryApi, SmartHomeDeviceApi, SmartHomeGuiApi, SmartHomeClientGuiApi, SmartHomeClientDeviceApi, SmartHomeHubRegistryApiRemote, SmartHomeDeviceApiRemote, SmartHomeGuiApiRemote, SmartHomeClientGuiApiRemote, SmartHomeClientDeviceApiRemote
}
from './aether_api';
// This is always relative
export class SensorRecordMetaBodyImpl implements FastMetaType<SensorRecord>  {
    serialize(sCtx_0: MetaContext, obj_1: SensorRecord, _out_2: DataOut): void  {
        _out_2.writeByte(obj_1.value);
        _out_2.writeByte(obj_1.time);
        
    }
    deserialize(sCtx_0: MetaContext, in__3: DataIn): SensorRecord  {
        try  {
            let value_4: number;
            let time_5: number;
            value_4 = in__3.readByte();
            time_5 = in__3.readByte();
            return new SensorRecord(value_4, time_5);
            
        }
        catch (e)  {
            throw new SecurityConnectionDropException("Body error: " + (e instanceof Error ? e.message : String(e)));
            
        }
        
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
    serialize(ctx: MetaContext, obj: DeviceStream, out: DataOut): void  {
        if (obj instanceof DeviceStream.Out)  {
            const outObj = obj as any;
            if (outObj.deferredFactory)  {
                const childCtx = ctx.findContext(outObj.deferredFactory, ...(outObj.deferredKeys || []));
                const childLock = childCtx.lock();
                try  {
                    const remoteApi = childCtx.makeRemote((SmartHomeDeviceApi as any).META);
                    outObj.deferredRemoteGenerator(remoteApi);
                    outObj.data = childCtx.remoteDataToArrayAsArray();
                    
                }
                finally  {
                    childLock?.close();
                    
                }
                
            }
            
        }
        FastMeta.META_ARRAY_BYTE.serialize(ctx, obj.data, out);
        
    }
    deserialize(ctx: MetaContext, in_: DataIn): DeviceStream  {
        try  {
            const data = FastMeta.META_ARRAY_BYTE.deserialize(ctx, in_);
            return new DeviceStream.In(data, ctx) as any as DeviceStream;
            
        }
        catch (e)  {
            throw new SecurityConnectionDropException("Stream error: " + (e instanceof Error ? e.message : String(e)));
            
        }
        
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
    serialize(ctx: MetaContext, obj: GuiStream, out: DataOut): void  {
        if (obj instanceof GuiStream.Out)  {
            const outObj = obj as any;
            if (outObj.deferredFactory)  {
                const childCtx = ctx.findContext(outObj.deferredFactory, ...(outObj.deferredKeys || []));
                const childLock = childCtx.lock();
                try  {
                    const remoteApi = childCtx.makeRemote((SmartHomeGuiApi as any).META);
                    outObj.deferredRemoteGenerator(remoteApi);
                    outObj.data = childCtx.remoteDataToArrayAsArray();
                    
                }
                finally  {
                    childLock?.close();
                    
                }
                
            }
            
        }
        FastMeta.META_ARRAY_BYTE.serialize(ctx, obj.data, out);
        
    }
    deserialize(ctx: MetaContext, in_: DataIn): GuiStream  {
        try  {
            const data = FastMeta.META_ARRAY_BYTE.deserialize(ctx, in_);
            return new GuiStream.In(data, ctx) as any as GuiStream;
            
        }
        catch (e)  {
            throw new SecurityConnectionDropException("Stream error: " + (e instanceof Error ? e.message : String(e)));
            
        }
        
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
export class SmartHomeHubRegistryApiDeviceArgumentsMetaBodyImpl implements FastMetaType<SmartHomeHubRegistryApiDeviceArguments>  {
    serialize(sCtx_6: MetaContext, obj_7: SmartHomeHubRegistryApiDeviceArguments, _out_8: DataOut): void  {
        DeviceStream.META.serialize(sCtx_6, obj_7.stream, _out_8);
        
    }
    deserialize(sCtx_6: MetaContext, in__9: DataIn): SmartHomeHubRegistryApiDeviceArguments  {
        try  {
            let stream_10: DeviceStream;
            stream_10 = DeviceStream.META.deserialize(sCtx_6, in__9);
            return new SmartHomeHubRegistryApiDeviceArguments(stream_10);
            
        }
        catch (e)  {
            throw new SecurityConnectionDropException("Body error: " + (e instanceof Error ? e.message : String(e)));
            
        }
        
    }
    metaHashCode(obj: SmartHomeHubRegistryApiDeviceArguments | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + DeviceStream.META.metaHashCode(obj.stream);
        return hash | 0;
        
    }
    metaEquals(v1: SmartHomeHubRegistryApiDeviceArguments | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof SmartHomeHubRegistryApiDeviceArguments)) return false;
        if (!DeviceStream.META.metaEquals(v1.stream, v2.stream)) return false;
        return true;
        
    }
    metaToString(obj: SmartHomeHubRegistryApiDeviceArguments | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('SmartHomeHubRegistryApiDeviceArguments(');
        res.add('stream:').add(obj.stream);
        res.add(')');
        
    }
    public serializeToBytes(obj: SmartHomeHubRegistryApiDeviceArguments): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): SmartHomeHubRegistryApiDeviceArguments  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): SmartHomeHubRegistryApiDeviceArguments  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class SmartHomeHubRegistryApiGuiArgumentsMetaBodyImpl implements FastMetaType<SmartHomeHubRegistryApiGuiArguments>  {
    serialize(sCtx_11: MetaContext, obj_12: SmartHomeHubRegistryApiGuiArguments, _out_13: DataOut): void  {
        GuiStream.META.serialize(sCtx_11, obj_12.stream, _out_13);
        
    }
    deserialize(sCtx_11: MetaContext, in__14: DataIn): SmartHomeHubRegistryApiGuiArguments  {
        try  {
            let stream_15: GuiStream;
            stream_15 = GuiStream.META.deserialize(sCtx_11, in__14);
            return new SmartHomeHubRegistryApiGuiArguments(stream_15);
            
        }
        catch (e)  {
            throw new SecurityConnectionDropException("Body error: " + (e instanceof Error ? e.message : String(e)));
            
        }
        
    }
    metaHashCode(obj: SmartHomeHubRegistryApiGuiArguments | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + GuiStream.META.metaHashCode(obj.stream);
        return hash | 0;
        
    }
    metaEquals(v1: SmartHomeHubRegistryApiGuiArguments | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof SmartHomeHubRegistryApiGuiArguments)) return false;
        if (!GuiStream.META.metaEquals(v1.stream, v2.stream)) return false;
        return true;
        
    }
    metaToString(obj: SmartHomeHubRegistryApiGuiArguments | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('SmartHomeHubRegistryApiGuiArguments(');
        res.add('stream:').add(obj.stream);
        res.add(')');
        
    }
    public serializeToBytes(obj: SmartHomeHubRegistryApiGuiArguments): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): SmartHomeHubRegistryApiGuiArguments  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): SmartHomeHubRegistryApiGuiArguments  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class SmartHomeHubRegistryApiMetaImpl implements FastMetaApi<SmartHomeHubRegistryApi, SmartHomeHubRegistryApiRemote>  {
    makeLocal(ctx: MetaContext, dataIn: DataIn): void  {
        this.makeLocal_fromDataIn(ctx, dataIn, ctx.getLocalApi() as SmartHomeHubRegistryApi);
        
    }
    makeLocal_fromDataIn(ctx: MetaContext, dataIn: DataIn, localApi: SmartHomeHubRegistryApi): void  {
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
                    let stream_17: DeviceStream;
                    stream_17 = DeviceStream.META.deserialize(ctx, dataIn);
                    const argsObject_18 = new SmartHomeHubRegistryApiDeviceArguments(stream_17);
                    const argsNames_19: string[] = ["stream"];
                    const argsValues_20: any[] = [stream_17];
                    ctx.invokeLocalMethodBefore("device", argsNames_19, argsValues_20);
                    (typeof (localApi as any).deviceArguments === "function" ? (localApi as any).deviceArguments(argsObject_18) : localApi.device(stream_17));
                    ctx.invokeLocalMethodAfter("device", null, argsNames_19, argsValues_20);
                    break;
                    
                }
                case 4:  {
                    let stream_22: GuiStream;
                    stream_22 = GuiStream.META.deserialize(ctx, dataIn);
                    const argsObject_23 = new SmartHomeHubRegistryApiGuiArguments(stream_22);
                    const argsNames_24: string[] = ["stream"];
                    const argsValues_25: any[] = [stream_22];
                    ctx.invokeLocalMethodBefore("gui", argsNames_24, argsValues_25);
                    (typeof (localApi as any).guiArguments === "function" ? (localApi as any).guiArguments(argsObject_23) : localApi.gui(stream_22));
                    ctx.invokeLocalMethodAfter("gui", null, argsNames_24, argsValues_25);
                    break;
                    
                }
                default: throw new SecurityConnectionDropException(`Unknown command ID: ${commandId}`);
                
            }
        }
        
    }
    makeLocal_fromBytes_ctxLocal(ctx: MetaContext, data: Uint8Array): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), ctx.getLocalApi());
        
    }
    makeLocal_fromBytes_ctx(ctx: MetaContext, data: Uint8Array, localApi: SmartHomeHubRegistryApi): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), localApi);
        
    }
    makeRemote(sCtx_26: MetaContext): SmartHomeHubRegistryApiRemote  {
        const remoteApiImpl =  {
            destroy: (_force: boolean): AFuture =>  {
                sCtx_26.close();
                return AFuture.completed();
                
            }
            , flush: (): void =>  {
                sCtx_26.flush();
                
            }
            , getFastMetaContext: () => sCtx_26, device: (stream: DeviceStream): void =>  {
                const dataOut_28 = new DataInOut();
                dataOut_28.writeByte(3);
                const argsNames_30: string[] = ["stream"];
                const argsValues_31: any[] = [stream];
                sCtx_26.invokeRemoteMethodAfter("device", null, argsNames_30, argsValues_31);
                DeviceStream.META.serialize(sCtx_26, stream, dataOut_28);
                sCtx_26.sendToRemote(dataOut_28.toArray());
                
            }
            , gui: (stream: GuiStream): void =>  {
                const dataOut_33 = new DataInOut();
                dataOut_33.writeByte(4);
                const argsNames_35: string[] = ["stream"];
                const argsValues_36: any[] = [stream];
                sCtx_26.invokeRemoteMethodAfter("gui", null, argsNames_35, argsValues_36);
                GuiStream.META.serialize(sCtx_26, stream, dataOut_33);
                sCtx_26.sendToRemote(dataOut_33.toArray());
                
            }
            , openDevice(factory: (api: SmartHomeDeviceApiRemote) => SmartHomeClientDeviceApi, converter: BytesConverter, ...keys: any[]): SmartHomeDeviceApiRemote  {
                return sCtx_26.findContext(ctx =>  {
                    ctx.onFlushData(data => this.device(DeviceStream.Out.send(converter(data))));
                    return factory(ctx.makeRemote((SmartHomeDeviceApi as any).META));
                    
                }
                , ...keys).makeRemote((SmartHomeDeviceApi as any).META) as SmartHomeDeviceApiRemote;
                
            }
            , openGui(factory: (api: SmartHomeGuiApiRemote) => SmartHomeClientGuiApi, converter: BytesConverter, ...keys: any[]): SmartHomeGuiApiRemote  {
                return sCtx_26.findContext(ctx =>  {
                    ctx.onFlushData(data => this.gui(GuiStream.Out.send(converter(data))));
                    return factory(ctx.makeRemote((SmartHomeGuiApi as any).META));
                    
                }
                , ...keys).makeRemote((SmartHomeGuiApi as any).META) as SmartHomeGuiApiRemote;
                
            }
            , 
        };
        return remoteApiImpl as SmartHomeHubRegistryApiRemote;
        
    }
    isValidCommand(commandId: number): boolean  {
        switch(commandId)  {
            case 0: // META_RESULT
            case 1: // META_ERROR
            case 3: // device
            case 4: // gui
            return true;
            default: return false;
            
        }
        
    }
    
}
export class SmartHomeDeviceApiReportStateArgumentsMetaBodyImpl implements FastMetaType<SmartHomeDeviceApiReportStateArguments>  {
    serialize(sCtx_37: MetaContext, obj_38: SmartHomeDeviceApiReportStateArguments, _out_39: DataOut): void  {
        _out_39.writeShort(obj_38.value);
        
    }
    deserialize(sCtx_37: MetaContext, in__40: DataIn): SmartHomeDeviceApiReportStateArguments  {
        try  {
            let value_41: number;
            value_41 = in__40.readShort();
            return new SmartHomeDeviceApiReportStateArguments(value_41);
            
        }
        catch (e)  {
            throw new SecurityConnectionDropException("Body error: " + (e instanceof Error ? e.message : String(e)));
            
        }
        
    }
    metaHashCode(obj: SmartHomeDeviceApiReportStateArguments | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_SHORT.metaHashCode(obj.value);
        return hash | 0;
        
    }
    metaEquals(v1: SmartHomeDeviceApiReportStateArguments | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof SmartHomeDeviceApiReportStateArguments)) return false;
        if (!FastMeta.META_SHORT.metaEquals(v1.value, v2.value)) return false;
        return true;
        
    }
    metaToString(obj: SmartHomeDeviceApiReportStateArguments | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('SmartHomeDeviceApiReportStateArguments(');
        res.add('value:').add(obj.value);
        res.add(')');
        
    }
    public serializeToBytes(obj: SmartHomeDeviceApiReportStateArguments): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): SmartHomeDeviceApiReportStateArguments  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): SmartHomeDeviceApiReportStateArguments  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class SmartHomeDeviceApiMetaImpl implements FastMetaApi<SmartHomeDeviceApi, SmartHomeDeviceApiRemote>  {
    makeLocal(ctx: MetaContext, dataIn: DataIn): void  {
        this.makeLocal_fromDataIn(ctx, dataIn, ctx.getLocalApi() as SmartHomeDeviceApi);
        
    }
    makeLocal_fromDataIn(ctx: MetaContext, dataIn: DataIn, localApi: SmartHomeDeviceApi): void  {
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
                    let value_43: number;
                    value_43 = dataIn.readShort();
                    const argsObject_44 = new SmartHomeDeviceApiReportStateArguments(value_43);
                    const argsNames_45: string[] = ["value"];
                    const argsValues_46: any[] = [value_43];
                    ctx.invokeLocalMethodBefore("reportState", argsNames_45, argsValues_46);
                    (typeof (localApi as any).reportStateArguments === "function" ? (localApi as any).reportStateArguments(argsObject_44) : localApi.reportState(value_43));
                    ctx.invokeLocalMethodAfter("reportState", null, argsNames_45, argsValues_46);
                    break;
                    
                }
                default: throw new SecurityConnectionDropException(`Unknown command ID: ${commandId}`);
                
            }
        }
        
    }
    makeLocal_fromBytes_ctxLocal(ctx: MetaContext, data: Uint8Array): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), ctx.getLocalApi());
        
    }
    makeLocal_fromBytes_ctx(ctx: MetaContext, data: Uint8Array, localApi: SmartHomeDeviceApi): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), localApi);
        
    }
    makeRemote(sCtx_47: MetaContext): SmartHomeDeviceApiRemote  {
        const remoteApiImpl =  {
            destroy: (_force: boolean): AFuture =>  {
                sCtx_47.close();
                return AFuture.completed();
                
            }
            , flush: (): void =>  {
                sCtx_47.flush();
                
            }
            , getFastMetaContext: () => sCtx_47, reportState: (value: number): void =>  {
                const dataOut_49 = new DataInOut();
                dataOut_49.writeByte(10);
                const argsNames_51: string[] = ["value"];
                const argsValues_52: any[] = [value];
                sCtx_47.invokeRemoteMethodAfter("reportState", null, argsNames_51, argsValues_52);
                dataOut_49.writeShort(value);
                sCtx_47.sendToRemote(dataOut_49.toArray());
                
            }
            , 
        };
        return remoteApiImpl as SmartHomeDeviceApiRemote;
        
    }
    isValidCommand(commandId: number): boolean  {
        switch(commandId)  {
            case 0: // META_RESULT
            case 1: // META_ERROR
            case 10: // reportState
            return true;
            default: return false;
            
        }
        
    }
    
}
export class SmartHomeGuiApiGetDevicesArgumentsMetaBodyImpl implements FastMetaType<SmartHomeGuiApiGetDevicesArguments>  {
    serialize(sCtx_53: MetaContext, obj_55: SmartHomeGuiApiGetDevicesArguments, _out_56: DataOut): void  {
        
    }
    deserialize(sCtx_54: MetaContext, in__57: DataIn): SmartHomeGuiApiGetDevicesArguments  {
        try  {
            return new SmartHomeGuiApiGetDevicesArguments();
            
        }
        catch (e)  {
            throw new SecurityConnectionDropException("Body error: " + (e instanceof Error ? e.message : String(e)));
            
        }
        
    }
    metaHashCode(obj: SmartHomeGuiApiGetDevicesArguments | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        return hash | 0;
        
    }
    metaEquals(v1: SmartHomeGuiApiGetDevicesArguments | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof SmartHomeGuiApiGetDevicesArguments)) return false;
        return true;
        
    }
    metaToString(obj: SmartHomeGuiApiGetDevicesArguments | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('SmartHomeGuiApiGetDevicesArguments(');
        res.add(')');
        
    }
    public serializeToBytes(obj: SmartHomeGuiApiGetDevicesArguments): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): SmartHomeGuiApiGetDevicesArguments  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): SmartHomeGuiApiGetDevicesArguments  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class SmartHomeGuiApiRequestDeviceHistoryArgumentsMetaBodyImpl implements FastMetaType<SmartHomeGuiApiRequestDeviceHistoryArguments>  {
    serialize(sCtx_58: MetaContext, obj_59: SmartHomeGuiApiRequestDeviceHistoryArguments, _out_60: DataOut): void  {
        FastMeta.META_UUID.serialize(sCtx_58, obj_59.deviceUid, _out_60);
        SerializerPackNumber.INSTANCE.put(_out_60, obj_59.count);
        
    }
    deserialize(sCtx_58: MetaContext, in__61: DataIn): SmartHomeGuiApiRequestDeviceHistoryArguments  {
        try  {
            let deviceUid_62: UUID;
            let count_63: bigint;
            deviceUid_62 = FastMeta.META_UUID.deserialize(sCtx_58, in__61);
            count_63 = DeserializerPackNumber.INSTANCE.put(in__61);
            return new SmartHomeGuiApiRequestDeviceHistoryArguments(deviceUid_62, count_63);
            
        }
        catch (e)  {
            throw new SecurityConnectionDropException("Body error: " + (e instanceof Error ? e.message : String(e)));
            
        }
        
    }
    metaHashCode(obj: SmartHomeGuiApiRequestDeviceHistoryArguments | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_UUID.metaHashCode(obj.deviceUid);
        hash = 37 * hash + FastMeta.META_PACK.metaHashCode(obj.count);
        return hash | 0;
        
    }
    metaEquals(v1: SmartHomeGuiApiRequestDeviceHistoryArguments | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof SmartHomeGuiApiRequestDeviceHistoryArguments)) return false;
        if (!FastMeta.META_UUID.metaEquals(v1.deviceUid, v2.deviceUid)) return false;
        if (!FastMeta.META_PACK.metaEquals(v1.count, v2.count)) return false;
        return true;
        
    }
    metaToString(obj: SmartHomeGuiApiRequestDeviceHistoryArguments | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('SmartHomeGuiApiRequestDeviceHistoryArguments(');
        res.add('deviceUid:').add(obj.deviceUid);
        res.add(', ');
        res.add('count:').add(obj.count);
        res.add(')');
        
    }
    public serializeToBytes(obj: SmartHomeGuiApiRequestDeviceHistoryArguments): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): SmartHomeGuiApiRequestDeviceHistoryArguments  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): SmartHomeGuiApiRequestDeviceHistoryArguments  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class SmartHomeGuiApiMetaImpl implements FastMetaApi<SmartHomeGuiApi, SmartHomeGuiApiRemote>  {
    makeLocal(ctx: MetaContext, dataIn: DataIn): void  {
        this.makeLocal_fromDataIn(ctx, dataIn, ctx.getLocalApi() as SmartHomeGuiApi);
        
    }
    makeLocal_fromDataIn(ctx: MetaContext, dataIn: DataIn, localApi: SmartHomeGuiApi): void  {
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
                    const argsObject_65 = new SmartHomeGuiApiGetDevicesArguments();
                    const argsNames_66: string[] = [];
                    const argsValues_67: any[] = [];
                    ctx.invokeLocalMethodBefore("getDevices", argsNames_66, argsValues_67);
                    (typeof (localApi as any).getDevicesArguments === "function" ? (localApi as any).getDevicesArguments(argsObject_65) : localApi.getDevices());
                    ctx.invokeLocalMethodAfter("getDevices", null, argsNames_66, argsValues_67);
                    break;
                    
                }
                case 15:  {
                    let deviceUid_69: UUID;
                    let count_70: bigint;
                    deviceUid_69 = FastMeta.META_UUID.deserialize(ctx, dataIn);
                    count_70 = DeserializerPackNumber.INSTANCE.put(dataIn);
                    const argsObject_71 = new SmartHomeGuiApiRequestDeviceHistoryArguments(deviceUid_69, count_70);
                    const argsNames_72: string[] = ["deviceUid", "count"];
                    const argsValues_73: any[] = [deviceUid_69, count_70];
                    ctx.invokeLocalMethodBefore("requestDeviceHistory", argsNames_72, argsValues_73);
                    (typeof (localApi as any).requestDeviceHistoryArguments === "function" ? (localApi as any).requestDeviceHistoryArguments(argsObject_71) : localApi.requestDeviceHistory(deviceUid_69, count_70));
                    ctx.invokeLocalMethodAfter("requestDeviceHistory", null, argsNames_72, argsValues_73);
                    break;
                    
                }
                default: throw new SecurityConnectionDropException(`Unknown command ID: ${commandId}`);
                
            }
        }
        
    }
    makeLocal_fromBytes_ctxLocal(ctx: MetaContext, data: Uint8Array): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), ctx.getLocalApi());
        
    }
    makeLocal_fromBytes_ctx(ctx: MetaContext, data: Uint8Array, localApi: SmartHomeGuiApi): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), localApi);
        
    }
    makeRemote(sCtx_74: MetaContext): SmartHomeGuiApiRemote  {
        const remoteApiImpl =  {
            destroy: (_force: boolean): AFuture =>  {
                sCtx_74.close();
                return AFuture.completed();
                
            }
            , flush: (): void =>  {
                sCtx_74.flush();
                
            }
            , getFastMetaContext: () => sCtx_74, getDevices: (): void =>  {
                const dataOut_76 = new DataInOut();
                dataOut_76.writeByte(12);
                const argsNames_78: string[] = [];
                const argsValues_79: any[] = [];
                sCtx_74.invokeRemoteMethodAfter("getDevices", null, argsNames_78, argsValues_79);
                sCtx_74.sendToRemote(dataOut_76.toArray());
                
            }
            , requestDeviceHistory: (deviceUid: UUID, count: bigint): void =>  {
                const dataOut_81 = new DataInOut();
                dataOut_81.writeByte(15);
                const argsNames_83: string[] = ["deviceUid", "count"];
                const argsValues_84: any[] = [deviceUid, count];
                sCtx_74.invokeRemoteMethodAfter("requestDeviceHistory", null, argsNames_83, argsValues_84);
                FastMeta.META_UUID.serialize(sCtx_74, deviceUid, dataOut_81);
                SerializerPackNumber.INSTANCE.put(dataOut_81, count);
                sCtx_74.sendToRemote(dataOut_81.toArray());
                
            }
            , 
        };
        return remoteApiImpl as SmartHomeGuiApiRemote;
        
    }
    isValidCommand(commandId: number): boolean  {
        switch(commandId)  {
            case 0: // META_RESULT
            case 1: // META_ERROR
            case 12: // getDevices
            case 15: // requestDeviceHistory
            return true;
            default: return false;
            
        }
        
    }
    
}
export class SmartHomeClientGuiApiDeviceStateUpdatedArgumentsMetaBodyImpl implements FastMetaType<SmartHomeClientGuiApiDeviceStateUpdatedArguments>  {
    serialize(sCtx_85: MetaContext, obj_86: SmartHomeClientGuiApiDeviceStateUpdatedArguments, _out_87: DataOut): void  {
        FastMeta.META_UUID.serialize(sCtx_85, obj_86.deviceUid, _out_87);
        SerializerPackNumber.INSTANCE.put(_out_87, obj_86.records.length);
        for (const el_89 of obj_86.records)  {
            SensorRecord.META.serialize(sCtx_85, el_89, _out_87);
            
        }
        
    }
    deserialize(sCtx_85: MetaContext, in__88: DataIn): SmartHomeClientGuiApiDeviceStateUpdatedArguments  {
        try  {
            let deviceUid_90: UUID;
            let records_91: SensorRecord[];
            deviceUid_90 = FastMeta.META_UUID.deserialize(sCtx_85, in__88);
            const len_93 = Number(DeserializerPackNumber.INSTANCE.put(in__88));
            records_91 = new Array<SensorRecord>(len_93);
            for (let idx_92 = 0;
            idx_92 < len_93;
            idx_92++)  {
                records_91[idx_92] = SensorRecord.META.deserialize(sCtx_85, in__88);
                
            }
            return new SmartHomeClientGuiApiDeviceStateUpdatedArguments(deviceUid_90, records_91);
            
        }
        catch (e)  {
            throw new SecurityConnectionDropException("Body error: " + (e instanceof Error ? e.message : String(e)));
            
        }
        
    }
    metaHashCode(obj: SmartHomeClientGuiApiDeviceStateUpdatedArguments | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_UUID.metaHashCode(obj.deviceUid);
        hash = 37 * hash + FastMeta.getMetaArray(SensorRecord.META).metaHashCode(obj.records);
        return hash | 0;
        
    }
    metaEquals(v1: SmartHomeClientGuiApiDeviceStateUpdatedArguments | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof SmartHomeClientGuiApiDeviceStateUpdatedArguments)) return false;
        if (!FastMeta.META_UUID.metaEquals(v1.deviceUid, v2.deviceUid)) return false;
        if (!FastMeta.getMetaArray(SensorRecord.META).metaEquals(v1.records, v2.records)) return false;
        return true;
        
    }
    metaToString(obj: SmartHomeClientGuiApiDeviceStateUpdatedArguments | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('SmartHomeClientGuiApiDeviceStateUpdatedArguments(');
        res.add('deviceUid:').add(obj.deviceUid);
        res.add(', ');
        res.add('records:').add(obj.records);
        res.add(')');
        
    }
    public serializeToBytes(obj: SmartHomeClientGuiApiDeviceStateUpdatedArguments): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): SmartHomeClientGuiApiDeviceStateUpdatedArguments  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): SmartHomeClientGuiApiDeviceStateUpdatedArguments  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class SmartHomeClientGuiApiOnGetDevicesResultArgumentsMetaBodyImpl implements FastMetaType<SmartHomeClientGuiApiOnGetDevicesResultArguments>  {
    serialize(sCtx_94: MetaContext, obj_95: SmartHomeClientGuiApiOnGetDevicesResultArguments, _out_96: DataOut): void  {
        SerializerPackNumber.INSTANCE.put(_out_96, obj_95.devices.length);
        for (const el_98 of obj_95.devices)  {
            FastMeta.META_UUID.serialize(sCtx_94, el_98, _out_96);
            
        }
        
    }
    deserialize(sCtx_94: MetaContext, in__97: DataIn): SmartHomeClientGuiApiOnGetDevicesResultArguments  {
        try  {
            let devices_99: UUID[];
            const len_101 = Number(DeserializerPackNumber.INSTANCE.put(in__97));
            devices_99 = new Array<UUID>(len_101);
            for (let idx_100 = 0;
            idx_100 < len_101;
            idx_100++)  {
                devices_99[idx_100] = FastMeta.META_UUID.deserialize(sCtx_94, in__97);
                
            }
            return new SmartHomeClientGuiApiOnGetDevicesResultArguments(devices_99);
            
        }
        catch (e)  {
            throw new SecurityConnectionDropException("Body error: " + (e instanceof Error ? e.message : String(e)));
            
        }
        
    }
    metaHashCode(obj: SmartHomeClientGuiApiOnGetDevicesResultArguments | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.getMetaArray(FastMeta.META_UUID).metaHashCode(obj.devices);
        return hash | 0;
        
    }
    metaEquals(v1: SmartHomeClientGuiApiOnGetDevicesResultArguments | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof SmartHomeClientGuiApiOnGetDevicesResultArguments)) return false;
        if (!FastMeta.getMetaArray(FastMeta.META_UUID).metaEquals(v1.devices, v2.devices)) return false;
        return true;
        
    }
    metaToString(obj: SmartHomeClientGuiApiOnGetDevicesResultArguments | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('SmartHomeClientGuiApiOnGetDevicesResultArguments(');
        res.add('devices:').add(obj.devices);
        res.add(')');
        
    }
    public serializeToBytes(obj: SmartHomeClientGuiApiOnGetDevicesResultArguments): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): SmartHomeClientGuiApiOnGetDevicesResultArguments  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): SmartHomeClientGuiApiOnGetDevicesResultArguments  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class SmartHomeClientGuiApiOnRequestHistoryResultArgumentsMetaBodyImpl implements FastMetaType<SmartHomeClientGuiApiOnRequestHistoryResultArguments>  {
    serialize(sCtx_102: MetaContext, obj_103: SmartHomeClientGuiApiOnRequestHistoryResultArguments, _out_104: DataOut): void  {
        FastMeta.META_UUID.serialize(sCtx_102, obj_103.deviceUid, _out_104);
        SerializerPackNumber.INSTANCE.put(_out_104, obj_103.records.length);
        for (const el_106 of obj_103.records)  {
            SensorRecord.META.serialize(sCtx_102, el_106, _out_104);
            
        }
        
    }
    deserialize(sCtx_102: MetaContext, in__105: DataIn): SmartHomeClientGuiApiOnRequestHistoryResultArguments  {
        try  {
            let deviceUid_107: UUID;
            let records_108: SensorRecord[];
            deviceUid_107 = FastMeta.META_UUID.deserialize(sCtx_102, in__105);
            const len_110 = Number(DeserializerPackNumber.INSTANCE.put(in__105));
            records_108 = new Array<SensorRecord>(len_110);
            for (let idx_109 = 0;
            idx_109 < len_110;
            idx_109++)  {
                records_108[idx_109] = SensorRecord.META.deserialize(sCtx_102, in__105);
                
            }
            return new SmartHomeClientGuiApiOnRequestHistoryResultArguments(deviceUid_107, records_108);
            
        }
        catch (e)  {
            throw new SecurityConnectionDropException("Body error: " + (e instanceof Error ? e.message : String(e)));
            
        }
        
    }
    metaHashCode(obj: SmartHomeClientGuiApiOnRequestHistoryResultArguments | null | undefined): number  {
        if (obj === null || obj === undefined) return 0;
        let hash = 17;
        hash = 37 * hash + FastMeta.META_UUID.metaHashCode(obj.deviceUid);
        hash = 37 * hash + FastMeta.getMetaArray(SensorRecord.META).metaHashCode(obj.records);
        return hash | 0;
        
    }
    metaEquals(v1: SmartHomeClientGuiApiOnRequestHistoryResultArguments | null | undefined, v2: any | null | undefined): boolean  {
        if (v1 === v2) return true;
        if (v1 === null || v1 === undefined) return (v2 === null || v2 === undefined);
        if (v2 === null || v2 === undefined || !(v2 instanceof SmartHomeClientGuiApiOnRequestHistoryResultArguments)) return false;
        if (!FastMeta.META_UUID.metaEquals(v1.deviceUid, v2.deviceUid)) return false;
        if (!FastMeta.getMetaArray(SensorRecord.META).metaEquals(v1.records, v2.records)) return false;
        return true;
        
    }
    metaToString(obj: SmartHomeClientGuiApiOnRequestHistoryResultArguments | null | undefined, res: AString): void  {
        if (obj === null || obj === undefined)  {
            res.add('null');
            return;
            
        }
        res.add('SmartHomeClientGuiApiOnRequestHistoryResultArguments(');
        res.add('deviceUid:').add(obj.deviceUid);
        res.add(', ');
        res.add('records:').add(obj.records);
        res.add(')');
        
    }
    public serializeToBytes(obj: SmartHomeClientGuiApiOnRequestHistoryResultArguments): Uint8Array  {
        const d = new DataInOut();
        // FastFutureContextStub is imported in aether_api_impl.ts
        this.serialize(FastFutureContextStub, obj, d);
        return d.toArray();
        
    }
    public deserializeFromBytes(data: Uint8Array): SmartHomeClientGuiApiOnRequestHistoryResultArguments  {
        const d = new DataInOutStatic(data);
        // FastFutureContextStub is imported in aether_api_impl.ts
        return this.deserialize(FastFutureContextStub, d);
        
    }
    public loadFromFile(file: string): SmartHomeClientGuiApiOnRequestHistoryResultArguments  {
        throw new Error("UnsupportedOperationException: loadFromFile requires Node.js/Filesystem access.");
        
    }
    
}
export class SmartHomeClientGuiApiMetaImpl implements FastMetaApi<SmartHomeClientGuiApi, SmartHomeClientGuiApiRemote>  {
    makeLocal(ctx: MetaContext, dataIn: DataIn): void  {
        this.makeLocal_fromDataIn(ctx, dataIn, ctx.getLocalApi() as SmartHomeClientGuiApi);
        
    }
    makeLocal_fromDataIn(ctx: MetaContext, dataIn: DataIn, localApi: SmartHomeClientGuiApi): void  {
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
                    let deviceUid_112: UUID;
                    let records_113: SensorRecord[];
                    deviceUid_112 = FastMeta.META_UUID.deserialize(ctx, dataIn);
                    const len_115 = Number(DeserializerPackNumber.INSTANCE.put(dataIn));
                    records_113 = new Array<SensorRecord>(len_115);
                    for (let idx_114 = 0;
                    idx_114 < len_115;
                    idx_114++)  {
                        records_113[idx_114] = SensorRecord.META.deserialize(ctx, dataIn);
                        
                    }
                    const argsObject_116 = new SmartHomeClientGuiApiDeviceStateUpdatedArguments(deviceUid_112, records_113);
                    const argsNames_117: string[] = ["deviceUid", "records"];
                    const argsValues_118: any[] = [deviceUid_112, records_113];
                    ctx.invokeLocalMethodBefore("deviceStateUpdated", argsNames_117, argsValues_118);
                    (typeof (localApi as any).deviceStateUpdatedArguments === "function" ? (localApi as any).deviceStateUpdatedArguments(argsObject_116) : localApi.deviceStateUpdated(deviceUid_112, records_113));
                    ctx.invokeLocalMethodAfter("deviceStateUpdated", null, argsNames_117, argsValues_118);
                    break;
                    
                }
                case 21:  {
                    let devices_120: UUID[];
                    const len_122 = Number(DeserializerPackNumber.INSTANCE.put(dataIn));
                    devices_120 = new Array<UUID>(len_122);
                    for (let idx_121 = 0;
                    idx_121 < len_122;
                    idx_121++)  {
                        devices_120[idx_121] = FastMeta.META_UUID.deserialize(ctx, dataIn);
                        
                    }
                    const argsObject_123 = new SmartHomeClientGuiApiOnGetDevicesResultArguments(devices_120);
                    const argsNames_124: string[] = ["devices"];
                    const argsValues_125: any[] = [devices_120];
                    ctx.invokeLocalMethodBefore("onGetDevicesResult", argsNames_124, argsValues_125);
                    (typeof (localApi as any).onGetDevicesResultArguments === "function" ? (localApi as any).onGetDevicesResultArguments(argsObject_123) : localApi.onGetDevicesResult(devices_120));
                    ctx.invokeLocalMethodAfter("onGetDevicesResult", null, argsNames_124, argsValues_125);
                    break;
                    
                }
                case 24:  {
                    let deviceUid_127: UUID;
                    let records_128: SensorRecord[];
                    deviceUid_127 = FastMeta.META_UUID.deserialize(ctx, dataIn);
                    const len_130 = Number(DeserializerPackNumber.INSTANCE.put(dataIn));
                    records_128 = new Array<SensorRecord>(len_130);
                    for (let idx_129 = 0;
                    idx_129 < len_130;
                    idx_129++)  {
                        records_128[idx_129] = SensorRecord.META.deserialize(ctx, dataIn);
                        
                    }
                    const argsObject_131 = new SmartHomeClientGuiApiOnRequestHistoryResultArguments(deviceUid_127, records_128);
                    const argsNames_132: string[] = ["deviceUid", "records"];
                    const argsValues_133: any[] = [deviceUid_127, records_128];
                    ctx.invokeLocalMethodBefore("onRequestHistoryResult", argsNames_132, argsValues_133);
                    (typeof (localApi as any).onRequestHistoryResultArguments === "function" ? (localApi as any).onRequestHistoryResultArguments(argsObject_131) : localApi.onRequestHistoryResult(deviceUid_127, records_128));
                    ctx.invokeLocalMethodAfter("onRequestHistoryResult", null, argsNames_132, argsValues_133);
                    break;
                    
                }
                default: throw new SecurityConnectionDropException(`Unknown command ID: ${commandId}`);
                
            }
        }
        
    }
    makeLocal_fromBytes_ctxLocal(ctx: MetaContext, data: Uint8Array): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), ctx.getLocalApi());
        
    }
    makeLocal_fromBytes_ctx(ctx: MetaContext, data: Uint8Array, localApi: SmartHomeClientGuiApi): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), localApi);
        
    }
    makeRemote(sCtx_134: MetaContext): SmartHomeClientGuiApiRemote  {
        const remoteApiImpl =  {
            destroy: (_force: boolean): AFuture =>  {
                sCtx_134.close();
                return AFuture.completed();
                
            }
            , flush: (): void =>  {
                sCtx_134.flush();
                
            }
            , getFastMetaContext: () => sCtx_134, deviceStateUpdated: (deviceUid: UUID, records: SensorRecord[]): void =>  {
                const dataOut_136 = new DataInOut();
                dataOut_136.writeByte(20);
                const argsNames_138: string[] = ["deviceUid", "records"];
                const argsValues_139: any[] = [deviceUid, records];
                sCtx_134.invokeRemoteMethodAfter("deviceStateUpdated", null, argsNames_138, argsValues_139);
                FastMeta.META_UUID.serialize(sCtx_134, deviceUid, dataOut_136);
                SerializerPackNumber.INSTANCE.put(dataOut_136, records.length);
                for (const el_140 of records)  {
                    SensorRecord.META.serialize(sCtx_134, el_140, dataOut_136);
                    
                }
                sCtx_134.sendToRemote(dataOut_136.toArray());
                
            }
            , onGetDevicesResult: (devices: UUID[]): void =>  {
                const dataOut_142 = new DataInOut();
                dataOut_142.writeByte(21);
                const argsNames_144: string[] = ["devices"];
                const argsValues_145: any[] = [devices];
                sCtx_134.invokeRemoteMethodAfter("onGetDevicesResult", null, argsNames_144, argsValues_145);
                SerializerPackNumber.INSTANCE.put(dataOut_142, devices.length);
                for (const el_146 of devices)  {
                    FastMeta.META_UUID.serialize(sCtx_134, el_146, dataOut_142);
                    
                }
                sCtx_134.sendToRemote(dataOut_142.toArray());
                
            }
            , onRequestHistoryResult: (deviceUid: UUID, records: SensorRecord[]): void =>  {
                const dataOut_148 = new DataInOut();
                dataOut_148.writeByte(24);
                const argsNames_150: string[] = ["deviceUid", "records"];
                const argsValues_151: any[] = [deviceUid, records];
                sCtx_134.invokeRemoteMethodAfter("onRequestHistoryResult", null, argsNames_150, argsValues_151);
                FastMeta.META_UUID.serialize(sCtx_134, deviceUid, dataOut_148);
                SerializerPackNumber.INSTANCE.put(dataOut_148, records.length);
                for (const el_152 of records)  {
                    SensorRecord.META.serialize(sCtx_134, el_152, dataOut_148);
                    
                }
                sCtx_134.sendToRemote(dataOut_148.toArray());
                
            }
            , 
        };
        return remoteApiImpl as SmartHomeClientGuiApiRemote;
        
    }
    isValidCommand(commandId: number): boolean  {
        switch(commandId)  {
            case 0: // META_RESULT
            case 1: // META_ERROR
            case 20: // deviceStateUpdated
            case 21: // onGetDevicesResult
            case 24: // onRequestHistoryResult
            return true;
            default: return false;
            
        }
        
    }
    
}
export class SmartHomeClientDeviceApiMetaImpl implements FastMetaApi<SmartHomeClientDeviceApi, SmartHomeClientDeviceApiRemote>  {
    makeLocal(ctx: MetaContext, dataIn: DataIn): void  {
        this.makeLocal_fromDataIn(ctx, dataIn, ctx.getLocalApi() as SmartHomeClientDeviceApi);
        
    }
    makeLocal_fromDataIn(ctx: MetaContext, dataIn: DataIn, _localApi: SmartHomeClientDeviceApi): void  {
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
                default: throw new SecurityConnectionDropException(`Unknown command ID: ${commandId}`);
                
            }
        }
        
    }
    makeLocal_fromBytes_ctxLocal(ctx: MetaContext, data: Uint8Array): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), ctx.getLocalApi());
        
    }
    makeLocal_fromBytes_ctx(ctx: MetaContext, data: Uint8Array, localApi: SmartHomeClientDeviceApi): void  {
        this.makeLocal_fromDataIn(ctx, new DataInOutStatic(data), localApi);
        
    }
    makeRemote(sCtx_153: MetaContext): SmartHomeClientDeviceApiRemote  {
        const remoteApiImpl =  {
            destroy: (_force: boolean): AFuture =>  {
                sCtx_153.close();
                return AFuture.completed();
                
            }
            , flush: (): void =>  {
                sCtx_153.flush();
                
            }
            , getFastMetaContext: () => sCtx_153, 
        };
        return remoteApiImpl as SmartHomeClientDeviceApiRemote;
        
    }
    isValidCommand(commandId: number): boolean  {
        switch(commandId)  {
            case 0: // META_RESULT
            case 1: // META_ERROR
            return true;
            default: return false;
            
        }
        
    }
    
}