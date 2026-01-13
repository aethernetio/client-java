import  {
    AFuture, ARFuture, DataIn, DataOut, DataInOut, DataInOutStatic, FastMetaType, FastFutureContext, RemoteApi, FastMeta, SerializerPackNumber, DeserializerPackNumber, FastApiContextLocal, FastMetaApi, BytesConverter, RemoteApiFuture, FastFutureContextStub, UUID, URI, AConsumer, ToString, AString
}
from 'aether-client';
import  {
    Record, SimpleDeviceApi, SimpleClientApi, SimpleDeviceApiRemote, SimpleClientApiRemote
}
from './aether_api';
// This is always relative
export class RecordMetaBodyImpl implements FastMetaType<Record>  {
    serialize(sCtx_0: FastFutureContext, obj_1: Record, _out_2: DataOut): void  {
        _out_2.writeByte(obj_1.value);
        _out_2.writeByte(obj_1.time);
        
    }
    deserialize(sCtx_0: FastFutureContext, in__3: DataIn): Record  {
        let value_4: number;
        let time_5: number;
        value_4 = in__3.readByte();
        time_5 = in__3.readByte();
        return new Record(value_4, time_5);
        
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
                    let count_7: number;
                    count_7 = dataIn.readShort();
                    const argsNames_8: string[] = ["count"];
                    const argsValues_9: any[] = [count_7];
                    ctx.invokeLocalMethodBefore("requestRecords", argsNames_8, argsValues_9);
                    localApi.requestRecords(count_7);
                    ctx.invokeLocalMethodAfter("requestRecords", null, argsNames_8, argsValues_9);
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
    makeRemote(sCtx_10: FastFutureContext): SimpleDeviceApiRemote  {
        const remoteApiImpl =  {
            flush: (sendFuture?: AFuture): AFuture =>  {
                const futureToUse = sendFuture || AFuture.make();
                sCtx_10.flush(futureToUse);
                return futureToUse;
                
            }
            , getFastMetaContext: () => sCtx_10, requestRecords: (count: number): void =>  {
                const dataOut_12 = new DataInOut();
                dataOut_12.writeByte(3);
                const argsNames_14: string[] = ["count"];
                const argsValues_15: any[] = [count];
                sCtx_10.invokeRemoteMethodAfter("requestRecords", null, argsNames_14, argsValues_15);
                dataOut_12.writeShort(count);
                sCtx_10.sendToRemote(dataOut_12.toArray());
                
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
                    let value_17: Record[];
                    const len_19 = Number(DeserializerPackNumber.INSTANCE.put(dataIn));
                    value_17 = new Array<Record>(len_19);
                    for (let idx_18 = 0;
                    idx_18 < len_19;
                    idx_18++)  {
                        value_17[idx_18] = Record.META.deserialize(ctx, dataIn);
                        
                    }
                    const argsNames_20: string[] = ["value"];
                    const argsValues_21: any[] = [value_17];
                    ctx.invokeLocalMethodBefore("receiveStatus", argsNames_20, argsValues_21);
                    localApi.receiveStatus(value_17);
                    ctx.invokeLocalMethodAfter("receiveStatus", null, argsNames_20, argsValues_21);
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
    makeRemote(sCtx_22: FastFutureContext): SimpleClientApiRemote  {
        const remoteApiImpl =  {
            flush: (sendFuture?: AFuture): AFuture =>  {
                const futureToUse = sendFuture || AFuture.make();
                sCtx_22.flush(futureToUse);
                return futureToUse;
                
            }
            , getFastMetaContext: () => sCtx_22, receiveStatus: (value: Record[]): void =>  {
                const dataOut_24 = new DataInOut();
                dataOut_24.writeByte(3);
                const argsNames_26: string[] = ["value"];
                const argsValues_27: any[] = [value];
                sCtx_22.invokeRemoteMethodAfter("receiveStatus", null, argsNames_26, argsValues_27);
                SerializerPackNumber.INSTANCE.put(dataOut_24, value.length);
                for (const el_28 of value)  {
                    Record.META.serialize(sCtx_22, el_28, dataOut_24);
                    
                }
                sCtx_22.sendToRemote(dataOut_24.toArray());
                
            }
            , 
        };
        return remoteApiImpl as SimpleClientApiRemote;
        
    }
    
}