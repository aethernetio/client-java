import  {
    AFuture, ARFuture, DataInOutStatic, DataInOut, FastMetaType, FastMeta, SerializerPackNumber, DeserializerPackNumber, FastFutureContextStub, SyncMapChannel, MetaContext, RemoteApi, FastMetaApi, BytesConverter, UUID, URI, AConsumer, ToString, FastMetaHierarchyType, AString
}
from 'aether-client';
import * as Impl from './aether_api_impl';
// This is always relative
/**
 * Represents the SensorRecord structure.
 */
export class SensorRecord implements ToString  {
    public readonly value: number;
    public readonly time: number;
    public static readonly META_BODY: FastMetaType<SensorRecord> = new Impl.SensorRecordMetaBodyImpl();
    public static readonly META: FastMetaType<SensorRecord> = SensorRecord.META_BODY;
    /**
     * Creates an instance of SensorRecord.
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
     * Calculates a hash code for a static instance of SensorRecord.
     * @param {SensorRecord | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: SensorRecord | null | undefined): number  {
        return SensorRecord.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of SensorRecord with another object.
     * @param {SensorRecord | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: SensorRecord | null | undefined, v2: any | null | undefined): boolean  {
        return SensorRecord.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return SensorRecord.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return SensorRecord.staticEquals(this, other);
        
    }
    public toAString(result: AString): AString  {
        SensorRecord.META.metaToString(this, result);
        return result;
        
    }
    
}
export class DeviceStream implements ToString  {
    public data: Uint8Array;
    constructor(data: Uint8Array)  {
        this.data = data;
        
    }
    public asIn(): any  {
        return this as any;
        
    }
    public static readonly In = class In extends DeviceStream  {
        public parentContext: MetaContext | null = null;
        public activeContext: MetaContext | null = null;
        public factory: ((ctx: MetaContext) => SmartHomeDeviceApi) | null = null;
        public _streamKeys: any[] | null = null;
        public onFlushC: ((cc: MetaContext) => void) | null = null;
        constructor(data: Uint8Array, parentContext: MetaContext)  {
            super(data);
            this.parentContext = parentContext;
            
        }
        onFlush(c: (cc: MetaContext, data: Uint8Array) => void): this  {
            this.onFlushC = (cc) =>  {
                const d = cc.remoteDataToArrayAsArray();
                if (d.length > 0) c(cc, d);
                
            };
            return this;
            
        }
        onFlushWithLocal<LT extends SmartHomeDeviceApi>(c: (cc: MetaContext, data: Uint8Array, localApi: LT) => void): this  {
            this.onFlushC = (cc) =>  {
                const d = cc.remoteDataToArrayAsArray();
                if (d.length > 0) c(cc, d, cc.getLocalApi() as LT);
                
            };
            return this;
            
        }
        onFlushCtx(c: (cc: MetaContext) => void): this  {
            this.onFlushC = c;
            return this;
            
        }
        onFlushData(c: (data: Uint8Array) => void): this  {
            this.onFlushC = (cc) =>  {
                const d = cc.remoteDataToArrayAsArray();
                if (d.length > 0) c(d);
                
            };
            return this;
            
        }
        onFlushToRemote<RT extends RemoteApi>(meta: FastMetaApi<any, RT>, c: (data: Uint8Array, remote: RT) => void): this  {
            this.onFlushC = (cc) =>  {
                const d = cc.remoteDataToArrayAsArray();
                if (d.length > 0) c(d, (this.parentContext as any).makeRemote(meta));
                
            };
            return this;
            
        }
        keys(factory: (ctx: MetaContext) => SmartHomeDeviceApi, ...keys: any[]): this  {
            this.factory = factory;
            this._streamKeys = keys;
            return this;
            
        }
        remoteApi(): SmartHomeClientDeviceApiRemote  {
            if (!this.factory) throw new Error("factory is not set");
            const activeCtx = this.parentContext!.findContext(this.factory!, ...(this._streamKeys || []));
            return activeCtx.makeRemote((SmartHomeClientDeviceApi as any).META) as SmartHomeClientDeviceApiRemote;
            
        }
        remoteParentApi<RT extends RemoteApi>(meta: FastMetaApi<any, RT>): RT  {
            return this.parentContext!.makeRemote(meta) as RT;
            
        }
        ctx(c: MetaContext): this  {
            this.activeContext = c;
            return this;
            
        }
        accept(): void  {
            let targetData = this.data;
            if (!this.activeContext)  {
                if (!this.factory) throw new Error("factory is null");
                let effectiveFactory = this.factory;
                if (this.onFlushC)  {
                    const flushCallback = this.onFlushC;
                    effectiveFactory = (ctx: MetaContext) =>  {
                        ctx.onFlush(() => flushCallback(ctx));
                        return this.factory!(ctx);
                        
                    };
                    
                }
                this.activeContext = this.parentContext!.findContext(effectiveFactory, ...(this._streamKeys || []));
                
            }
            (SmartHomeDeviceApi as any).META.makeLocal(this.activeContext, new DataInOutStatic(targetData));
            
        }
        
    };
    public static readonly Out = class Out extends DeviceStream  {
        public deferredRemoteGenerator: ((api: any) => void) | null = null;
        public deferredFactory: ((ctx: MetaContext) => any) | null = null;
        public deferredKeys: any[] | null = null;
        constructor()  {
            super(new Uint8Array(0));
            
        }
        static send(rawData: Uint8Array): Out  {
            const out = new Out();
            (out as any).data = rawData;
            return out;
            
        }
        static sendWithApi(remoteGenerator: (api: SmartHomeDeviceApiRemote) => void, factory: (ctx: MetaContext) => SmartHomeClientDeviceApi, ...keys: any[]): Out  {
            const out = new Out();
            out.deferredRemoteGenerator = remoteGenerator as any;
            out.deferredFactory = factory as any;
            out.deferredKeys = keys;
            return out;
            
        }
        
    };
    public static readonly META: FastMetaType<DeviceStream> = new Impl.DeviceStreamMetaImpl();
    public toAString(result: AString): AString  {
        DeviceStream.META.metaToString(this, result);
        return result;
        
    }
    
}
export class GuiStream implements ToString  {
    public data: Uint8Array;
    constructor(data: Uint8Array)  {
        this.data = data;
        
    }
    public asIn(): any  {
        return this as any;
        
    }
    public static readonly In = class In extends GuiStream  {
        public parentContext: MetaContext | null = null;
        public activeContext: MetaContext | null = null;
        public factory: ((ctx: MetaContext) => SmartHomeGuiApi) | null = null;
        public _streamKeys: any[] | null = null;
        public onFlushC: ((cc: MetaContext) => void) | null = null;
        constructor(data: Uint8Array, parentContext: MetaContext)  {
            super(data);
            this.parentContext = parentContext;
            
        }
        onFlush(c: (cc: MetaContext, data: Uint8Array) => void): this  {
            this.onFlushC = (cc) =>  {
                const d = cc.remoteDataToArrayAsArray();
                if (d.length > 0) c(cc, d);
                
            };
            return this;
            
        }
        onFlushWithLocal<LT extends SmartHomeGuiApi>(c: (cc: MetaContext, data: Uint8Array, localApi: LT) => void): this  {
            this.onFlushC = (cc) =>  {
                const d = cc.remoteDataToArrayAsArray();
                if (d.length > 0) c(cc, d, cc.getLocalApi() as LT);
                
            };
            return this;
            
        }
        onFlushCtx(c: (cc: MetaContext) => void): this  {
            this.onFlushC = c;
            return this;
            
        }
        onFlushData(c: (data: Uint8Array) => void): this  {
            this.onFlushC = (cc) =>  {
                const d = cc.remoteDataToArrayAsArray();
                if (d.length > 0) c(d);
                
            };
            return this;
            
        }
        onFlushToRemote<RT extends RemoteApi>(meta: FastMetaApi<any, RT>, c: (data: Uint8Array, remote: RT) => void): this  {
            this.onFlushC = (cc) =>  {
                const d = cc.remoteDataToArrayAsArray();
                if (d.length > 0) c(d, (this.parentContext as any).makeRemote(meta));
                
            };
            return this;
            
        }
        keys(factory: (ctx: MetaContext) => SmartHomeGuiApi, ...keys: any[]): this  {
            this.factory = factory;
            this._streamKeys = keys;
            return this;
            
        }
        remoteApi(): SmartHomeClientGuiApiRemote  {
            if (!this.factory) throw new Error("factory is not set");
            const activeCtx = this.parentContext!.findContext(this.factory!, ...(this._streamKeys || []));
            return activeCtx.makeRemote((SmartHomeClientGuiApi as any).META) as SmartHomeClientGuiApiRemote;
            
        }
        remoteParentApi<RT extends RemoteApi>(meta: FastMetaApi<any, RT>): RT  {
            return this.parentContext!.makeRemote(meta) as RT;
            
        }
        ctx(c: MetaContext): this  {
            this.activeContext = c;
            return this;
            
        }
        accept(): void  {
            let targetData = this.data;
            if (!this.activeContext)  {
                if (!this.factory) throw new Error("factory is null");
                let effectiveFactory = this.factory;
                if (this.onFlushC)  {
                    const flushCallback = this.onFlushC;
                    effectiveFactory = (ctx: MetaContext) =>  {
                        ctx.onFlush(() => flushCallback(ctx));
                        return this.factory!(ctx);
                        
                    };
                    
                }
                this.activeContext = this.parentContext!.findContext(effectiveFactory, ...(this._streamKeys || []));
                
            }
            (SmartHomeGuiApi as any).META.makeLocal(this.activeContext, new DataInOutStatic(targetData));
            
        }
        
    };
    public static readonly Out = class Out extends GuiStream  {
        public deferredRemoteGenerator: ((api: any) => void) | null = null;
        public deferredFactory: ((ctx: MetaContext) => any) | null = null;
        public deferredKeys: any[] | null = null;
        constructor()  {
            super(new Uint8Array(0));
            
        }
        static send(rawData: Uint8Array): Out  {
            const out = new Out();
            (out as any).data = rawData;
            return out;
            
        }
        static sendWithApi(remoteGenerator: (api: SmartHomeGuiApiRemote) => void, factory: (ctx: MetaContext) => SmartHomeClientGuiApi, ...keys: any[]): Out  {
            const out = new Out();
            out.deferredRemoteGenerator = remoteGenerator as any;
            out.deferredFactory = factory as any;
            out.deferredKeys = keys;
            return out;
            
        }
        
    };
    public static readonly META: FastMetaType<GuiStream> = new Impl.GuiStreamMetaImpl();
    public toAString(result: AString): AString  {
        GuiStream.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * Represents the SmartHomeHubRegistryApiDeviceArguments structure.
 */
export class SmartHomeHubRegistryApiDeviceArguments implements ToString  {
    public readonly stream: DeviceStream;
    public static readonly META_BODY: FastMetaType<SmartHomeHubRegistryApiDeviceArguments> = new Impl.SmartHomeHubRegistryApiDeviceArgumentsMetaBodyImpl();
    public static readonly META: FastMetaType<SmartHomeHubRegistryApiDeviceArguments> = SmartHomeHubRegistryApiDeviceArguments.META_BODY;
    /**
     * Creates an instance of SmartHomeHubRegistryApiDeviceArguments.
     * @param stream - DeviceStream
     */
    constructor(stream: DeviceStream)  {
        this.stream = stream;
        if (stream === null || stream === undefined) throw new Error(`Field 'stream' cannot be null for type SmartHomeHubRegistryApiDeviceArguments.`);
        
    }
    public getStream(): DeviceStream  {
        return this.stream;
        
    }
    /**
     * Calculates a hash code for a static instance of SmartHomeHubRegistryApiDeviceArguments.
     * @param {SmartHomeHubRegistryApiDeviceArguments | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: SmartHomeHubRegistryApiDeviceArguments | null | undefined): number  {
        return SmartHomeHubRegistryApiDeviceArguments.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of SmartHomeHubRegistryApiDeviceArguments with another object.
     * @param {SmartHomeHubRegistryApiDeviceArguments | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: SmartHomeHubRegistryApiDeviceArguments | null | undefined, v2: any | null | undefined): boolean  {
        return SmartHomeHubRegistryApiDeviceArguments.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return SmartHomeHubRegistryApiDeviceArguments.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return SmartHomeHubRegistryApiDeviceArguments.staticEquals(this, other);
        
    }
    public toAString(result: AString): AString  {
        SmartHomeHubRegistryApiDeviceArguments.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * Represents the SmartHomeHubRegistryApiGuiArguments structure.
 */
export class SmartHomeHubRegistryApiGuiArguments implements ToString  {
    public readonly stream: GuiStream;
    public static readonly META_BODY: FastMetaType<SmartHomeHubRegistryApiGuiArguments> = new Impl.SmartHomeHubRegistryApiGuiArgumentsMetaBodyImpl();
    public static readonly META: FastMetaType<SmartHomeHubRegistryApiGuiArguments> = SmartHomeHubRegistryApiGuiArguments.META_BODY;
    /**
     * Creates an instance of SmartHomeHubRegistryApiGuiArguments.
     * @param stream - GuiStream
     */
    constructor(stream: GuiStream)  {
        this.stream = stream;
        if (stream === null || stream === undefined) throw new Error(`Field 'stream' cannot be null for type SmartHomeHubRegistryApiGuiArguments.`);
        
    }
    public getStream(): GuiStream  {
        return this.stream;
        
    }
    /**
     * Calculates a hash code for a static instance of SmartHomeHubRegistryApiGuiArguments.
     * @param {SmartHomeHubRegistryApiGuiArguments | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: SmartHomeHubRegistryApiGuiArguments | null | undefined): number  {
        return SmartHomeHubRegistryApiGuiArguments.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of SmartHomeHubRegistryApiGuiArguments with another object.
     * @param {SmartHomeHubRegistryApiGuiArguments | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: SmartHomeHubRegistryApiGuiArguments | null | undefined, v2: any | null | undefined): boolean  {
        return SmartHomeHubRegistryApiGuiArguments.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return SmartHomeHubRegistryApiGuiArguments.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return SmartHomeHubRegistryApiGuiArguments.staticEquals(this, other);
        
    }
    public toAString(result: AString): AString  {
        SmartHomeHubRegistryApiGuiArguments.META.metaToString(this, result);
        return result;
        
    }
    
}
export interface SmartHomeHubRegistryApi  {
    /**
     * @param stream - DeviceStream
     *
     * @aetherMethodId 3
     */
    device(stream: DeviceStream): void;
    deviceArguments?(args: SmartHomeHubRegistryApiDeviceArguments): void;
    /**
     * @param stream - GuiStream
     *
     * @aetherMethodId 4
     */
    gui(stream: GuiStream): void;
    guiArguments?(args: SmartHomeHubRegistryApiGuiArguments): void;
    
}
export namespace SmartHomeHubRegistryApi  {
    export const META: FastMetaApi<SmartHomeHubRegistryApi, SmartHomeHubRegistryApiRemote> = new Impl.SmartHomeHubRegistryApiMetaImpl();
    
}
export interface SmartHomeHubRegistryApiRemote extends SmartHomeHubRegistryApi, RemoteApi  {
    openDevice(factory: (api: SmartHomeDeviceApiRemote) => SmartHomeClientDeviceApi, converter: BytesConverter, ...keys: any[]): SmartHomeDeviceApiRemote;
    openGui(factory: (api: SmartHomeGuiApiRemote) => SmartHomeClientGuiApi, converter: BytesConverter, ...keys: any[]): SmartHomeGuiApiRemote;
    
}
export abstract class SmartHomeHubRegistryApiLocal<RT extends RemoteApi> implements SmartHomeHubRegistryApi  {
    protected readonly remoteApi: RT;
    public getRemoteApi(): RT  {
        return this.remoteApi;
        
    }
    protected constructor(remoteApi: RT)  {
        this.remoteApi = remoteApi;
        
    }
    /**
     * @param stream - DeviceStream
     *
     * @aetherMethodId 3
     */
    public abstract device(stream: DeviceStream): void;
    public deviceArguments(args: SmartHomeHubRegistryApiDeviceArguments): void  {
        this.device(args.stream);
        
    }
    /**
     * @param stream - GuiStream
     *
     * @aetherMethodId 4
     */
    public abstract gui(stream: GuiStream): void;
    public guiArguments(args: SmartHomeHubRegistryApiGuiArguments): void  {
        this.gui(args.stream);
        
    }
    
}
/**
 * Represents the SmartHomeDeviceApiReportStateArguments structure.
 */
export class SmartHomeDeviceApiReportStateArguments implements ToString  {
    public readonly value: number;
    public static readonly META_BODY: FastMetaType<SmartHomeDeviceApiReportStateArguments> = new Impl.SmartHomeDeviceApiReportStateArgumentsMetaBodyImpl();
    public static readonly META: FastMetaType<SmartHomeDeviceApiReportStateArguments> = SmartHomeDeviceApiReportStateArguments.META_BODY;
    /**
     * Creates an instance of SmartHomeDeviceApiReportStateArguments.
     * @param value - number
     */
    constructor(value: number)  {
        this.value = value;
        
    }
    public getValue(): number  {
        return this.value;
        
    }
    /**
     * Calculates a hash code for a static instance of SmartHomeDeviceApiReportStateArguments.
     * @param {SmartHomeDeviceApiReportStateArguments | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: SmartHomeDeviceApiReportStateArguments | null | undefined): number  {
        return SmartHomeDeviceApiReportStateArguments.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of SmartHomeDeviceApiReportStateArguments with another object.
     * @param {SmartHomeDeviceApiReportStateArguments | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: SmartHomeDeviceApiReportStateArguments | null | undefined, v2: any | null | undefined): boolean  {
        return SmartHomeDeviceApiReportStateArguments.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return SmartHomeDeviceApiReportStateArguments.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return SmartHomeDeviceApiReportStateArguments.staticEquals(this, other);
        
    }
    public toAString(result: AString): AString  {
        SmartHomeDeviceApiReportStateArguments.META.metaToString(this, result);
        return result;
        
    }
    
}
export interface SmartHomeDeviceApi  {
    /**
     * @param value - number
     *
     * @aetherMethodId 10
     */
    reportState(value: number): void;
    reportStateArguments?(args: SmartHomeDeviceApiReportStateArguments): void;
    
}
export namespace SmartHomeDeviceApi  {
    export const META: FastMetaApi<SmartHomeDeviceApi, SmartHomeDeviceApiRemote> = new Impl.SmartHomeDeviceApiMetaImpl();
    
}
export interface SmartHomeDeviceApiRemote extends SmartHomeDeviceApi, RemoteApi  {
    
}
export abstract class SmartHomeDeviceApiLocal<RT extends RemoteApi> implements SmartHomeDeviceApi  {
    protected readonly remoteApi: RT;
    public getRemoteApi(): RT  {
        return this.remoteApi;
        
    }
    protected constructor(remoteApi: RT)  {
        this.remoteApi = remoteApi;
        
    }
    /**
     * @param value - number
     *
     * @aetherMethodId 10
     */
    public abstract reportState(value: number): void;
    public reportStateArguments(args: SmartHomeDeviceApiReportStateArguments): void  {
        this.reportState(args.value);
        
    }
    
}
/**
 * Represents the SmartHomeGuiApiGetDevicesArguments structure.
 */
export class SmartHomeGuiApiGetDevicesArguments implements ToString  {
    public static readonly META_BODY: FastMetaType<SmartHomeGuiApiGetDevicesArguments> = new Impl.SmartHomeGuiApiGetDevicesArgumentsMetaBodyImpl();
    public static readonly META: FastMetaType<SmartHomeGuiApiGetDevicesArguments> = SmartHomeGuiApiGetDevicesArguments.META_BODY;
    /**
     * Creates an instance of SmartHomeGuiApiGetDevicesArguments.
     */
    constructor()  {
        
    }
    /**
     * Calculates a hash code for a static instance of SmartHomeGuiApiGetDevicesArguments.
     * @param {SmartHomeGuiApiGetDevicesArguments | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: SmartHomeGuiApiGetDevicesArguments | null | undefined): number  {
        return SmartHomeGuiApiGetDevicesArguments.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of SmartHomeGuiApiGetDevicesArguments with another object.
     * @param {SmartHomeGuiApiGetDevicesArguments | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: SmartHomeGuiApiGetDevicesArguments | null | undefined, v2: any | null | undefined): boolean  {
        return SmartHomeGuiApiGetDevicesArguments.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return SmartHomeGuiApiGetDevicesArguments.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return SmartHomeGuiApiGetDevicesArguments.staticEquals(this, other);
        
    }
    public toAString(result: AString): AString  {
        SmartHomeGuiApiGetDevicesArguments.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * Represents the SmartHomeGuiApiRequestDeviceHistoryArguments structure.
 */
export class SmartHomeGuiApiRequestDeviceHistoryArguments implements ToString  {
    public readonly deviceUid: UUID;
    public readonly count: bigint;
    public static readonly META_BODY: FastMetaType<SmartHomeGuiApiRequestDeviceHistoryArguments> = new Impl.SmartHomeGuiApiRequestDeviceHistoryArgumentsMetaBodyImpl();
    public static readonly META: FastMetaType<SmartHomeGuiApiRequestDeviceHistoryArguments> = SmartHomeGuiApiRequestDeviceHistoryArguments.META_BODY;
    /**
     * Creates an instance of SmartHomeGuiApiRequestDeviceHistoryArguments.
     * @param deviceUid - UUID
     * @param count - bigint
     */
    constructor(deviceUid: UUID, count: bigint)  {
        this.deviceUid = deviceUid;
        this.count = count;
        
    }
    public getDeviceUid(): UUID  {
        return this.deviceUid;
        
    }
    public getCount(): bigint  {
        return this.count;
        
    }
    /**
     * Calculates a hash code for a static instance of SmartHomeGuiApiRequestDeviceHistoryArguments.
     * @param {SmartHomeGuiApiRequestDeviceHistoryArguments | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: SmartHomeGuiApiRequestDeviceHistoryArguments | null | undefined): number  {
        return SmartHomeGuiApiRequestDeviceHistoryArguments.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of SmartHomeGuiApiRequestDeviceHistoryArguments with another object.
     * @param {SmartHomeGuiApiRequestDeviceHistoryArguments | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: SmartHomeGuiApiRequestDeviceHistoryArguments | null | undefined, v2: any | null | undefined): boolean  {
        return SmartHomeGuiApiRequestDeviceHistoryArguments.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return SmartHomeGuiApiRequestDeviceHistoryArguments.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return SmartHomeGuiApiRequestDeviceHistoryArguments.staticEquals(this, other);
        
    }
    public toAString(result: AString): AString  {
        SmartHomeGuiApiRequestDeviceHistoryArguments.META.metaToString(this, result);
        return result;
        
    }
    
}
export interface SmartHomeGuiApi  {
    /**
     * @aetherMethodId 12
     */
    getDevices(): void;
    getDevicesArguments?(args: SmartHomeGuiApiGetDevicesArguments): void;
    /**
     * @param deviceUid - UUID
     * @param count - bigint
     *
     * @aetherMethodId 15
     */
    requestDeviceHistory(deviceUid: UUID, count: bigint): void;
    requestDeviceHistoryArguments?(args: SmartHomeGuiApiRequestDeviceHistoryArguments): void;
    
}
export namespace SmartHomeGuiApi  {
    export const META: FastMetaApi<SmartHomeGuiApi, SmartHomeGuiApiRemote> = new Impl.SmartHomeGuiApiMetaImpl();
    
}
export interface SmartHomeGuiApiRemote extends SmartHomeGuiApi, RemoteApi  {
    
}
export abstract class SmartHomeGuiApiLocal<RT extends RemoteApi> implements SmartHomeGuiApi  {
    protected readonly remoteApi: RT;
    public getRemoteApi(): RT  {
        return this.remoteApi;
        
    }
    protected constructor(remoteApi: RT)  {
        this.remoteApi = remoteApi;
        
    }
    /**
     * @aetherMethodId 12
     */
    public abstract getDevices(): void;
    public getDevicesArguments(args: SmartHomeGuiApiGetDevicesArguments): void  {
        this.getDevices();
        
    }
    /**
     * @param deviceUid - UUID
     * @param count - bigint
     *
     * @aetherMethodId 15
     */
    public abstract requestDeviceHistory(deviceUid: UUID, count: bigint): void;
    public requestDeviceHistoryArguments(args: SmartHomeGuiApiRequestDeviceHistoryArguments): void  {
        this.requestDeviceHistory(args.deviceUid, args.count);
        
    }
    
}
/**
 * Represents the SmartHomeClientGuiApiDeviceStateUpdatedArguments structure.
 */
export class SmartHomeClientGuiApiDeviceStateUpdatedArguments implements ToString  {
    public readonly deviceUid: UUID;
    public readonly records: SensorRecord[];
    public static readonly META_BODY: FastMetaType<SmartHomeClientGuiApiDeviceStateUpdatedArguments> = new Impl.SmartHomeClientGuiApiDeviceStateUpdatedArgumentsMetaBodyImpl();
    public static readonly META: FastMetaType<SmartHomeClientGuiApiDeviceStateUpdatedArguments> = SmartHomeClientGuiApiDeviceStateUpdatedArguments.META_BODY;
    /**
     * Creates an instance of SmartHomeClientGuiApiDeviceStateUpdatedArguments.
     * @param deviceUid - UUID
     * @param records - SensorRecord[]
     */
    constructor(deviceUid: UUID, records: SensorRecord[])  {
        this.deviceUid = deviceUid;
        this.records = records;
        if (records === null || records === undefined) throw new Error(`Field 'records' cannot be null for type SmartHomeClientGuiApiDeviceStateUpdatedArguments.`);
        
    }
    public getDeviceUid(): UUID  {
        return this.deviceUid;
        
    }
    public getRecords(): SensorRecord[]  {
        return this.records;
        
    }
    public recordsContains(el: SensorRecord): boolean  {
        return (this.records as SensorRecord[]).includes(el as any);
        
    }
    /**
     * Calculates a hash code for a static instance of SmartHomeClientGuiApiDeviceStateUpdatedArguments.
     * @param {SmartHomeClientGuiApiDeviceStateUpdatedArguments | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: SmartHomeClientGuiApiDeviceStateUpdatedArguments | null | undefined): number  {
        return SmartHomeClientGuiApiDeviceStateUpdatedArguments.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of SmartHomeClientGuiApiDeviceStateUpdatedArguments with another object.
     * @param {SmartHomeClientGuiApiDeviceStateUpdatedArguments | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: SmartHomeClientGuiApiDeviceStateUpdatedArguments | null | undefined, v2: any | null | undefined): boolean  {
        return SmartHomeClientGuiApiDeviceStateUpdatedArguments.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return SmartHomeClientGuiApiDeviceStateUpdatedArguments.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return SmartHomeClientGuiApiDeviceStateUpdatedArguments.staticEquals(this, other);
        
    }
    public toAString(result: AString): AString  {
        SmartHomeClientGuiApiDeviceStateUpdatedArguments.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * Represents the SmartHomeClientGuiApiOnGetDevicesResultArguments structure.
 */
export class SmartHomeClientGuiApiOnGetDevicesResultArguments implements ToString  {
    public readonly devices: UUID[];
    public static readonly META_BODY: FastMetaType<SmartHomeClientGuiApiOnGetDevicesResultArguments> = new Impl.SmartHomeClientGuiApiOnGetDevicesResultArgumentsMetaBodyImpl();
    public static readonly META: FastMetaType<SmartHomeClientGuiApiOnGetDevicesResultArguments> = SmartHomeClientGuiApiOnGetDevicesResultArguments.META_BODY;
    /**
     * Creates an instance of SmartHomeClientGuiApiOnGetDevicesResultArguments.
     * @param devices - UUID[]
     */
    constructor(devices: UUID[])  {
        this.devices = devices;
        if (devices === null || devices === undefined) throw new Error(`Field 'devices' cannot be null for type SmartHomeClientGuiApiOnGetDevicesResultArguments.`);
        
    }
    public getDevices(): UUID[]  {
        return this.devices;
        
    }
    public devicesContains(el: UUID): boolean  {
        return (this.devices as UUID[]).includes(el as any);
        
    }
    /**
     * Calculates a hash code for a static instance of SmartHomeClientGuiApiOnGetDevicesResultArguments.
     * @param {SmartHomeClientGuiApiOnGetDevicesResultArguments | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: SmartHomeClientGuiApiOnGetDevicesResultArguments | null | undefined): number  {
        return SmartHomeClientGuiApiOnGetDevicesResultArguments.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of SmartHomeClientGuiApiOnGetDevicesResultArguments with another object.
     * @param {SmartHomeClientGuiApiOnGetDevicesResultArguments | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: SmartHomeClientGuiApiOnGetDevicesResultArguments | null | undefined, v2: any | null | undefined): boolean  {
        return SmartHomeClientGuiApiOnGetDevicesResultArguments.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return SmartHomeClientGuiApiOnGetDevicesResultArguments.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return SmartHomeClientGuiApiOnGetDevicesResultArguments.staticEquals(this, other);
        
    }
    public toAString(result: AString): AString  {
        SmartHomeClientGuiApiOnGetDevicesResultArguments.META.metaToString(this, result);
        return result;
        
    }
    
}
/**
 * Represents the SmartHomeClientGuiApiOnRequestHistoryResultArguments structure.
 */
export class SmartHomeClientGuiApiOnRequestHistoryResultArguments implements ToString  {
    public readonly deviceUid: UUID;
    public readonly records: SensorRecord[];
    public static readonly META_BODY: FastMetaType<SmartHomeClientGuiApiOnRequestHistoryResultArguments> = new Impl.SmartHomeClientGuiApiOnRequestHistoryResultArgumentsMetaBodyImpl();
    public static readonly META: FastMetaType<SmartHomeClientGuiApiOnRequestHistoryResultArguments> = SmartHomeClientGuiApiOnRequestHistoryResultArguments.META_BODY;
    /**
     * Creates an instance of SmartHomeClientGuiApiOnRequestHistoryResultArguments.
     * @param deviceUid - UUID
     * @param records - SensorRecord[]
     */
    constructor(deviceUid: UUID, records: SensorRecord[])  {
        this.deviceUid = deviceUid;
        this.records = records;
        if (records === null || records === undefined) throw new Error(`Field 'records' cannot be null for type SmartHomeClientGuiApiOnRequestHistoryResultArguments.`);
        
    }
    public getDeviceUid(): UUID  {
        return this.deviceUid;
        
    }
    public getRecords(): SensorRecord[]  {
        return this.records;
        
    }
    public recordsContains(el: SensorRecord): boolean  {
        return (this.records as SensorRecord[]).includes(el as any);
        
    }
    /**
     * Calculates a hash code for a static instance of SmartHomeClientGuiApiOnRequestHistoryResultArguments.
     * @param {SmartHomeClientGuiApiOnRequestHistoryResultArguments | null | undefined} obj - The object to hash.
     * @returns {number} The hash code.
     */
    public static staticHashCode(obj: SmartHomeClientGuiApiOnRequestHistoryResultArguments | null | undefined): number  {
        return SmartHomeClientGuiApiOnRequestHistoryResultArguments.META.metaHashCode(obj);
        
    }
    /**
     * Compares a static instance of SmartHomeClientGuiApiOnRequestHistoryResultArguments with another object.
     * @param {SmartHomeClientGuiApiOnRequestHistoryResultArguments | null | undefined} v1 - The first object.
     * @param {any | null | undefined} v2 - The second object.
     * @returns {boolean} True if the objects are equal.
     */
    public static staticEquals(v1: SmartHomeClientGuiApiOnRequestHistoryResultArguments | null | undefined, v2: any | null | undefined): boolean  {
        return SmartHomeClientGuiApiOnRequestHistoryResultArguments.META.metaEquals(v1, v2);
        
    }
    /**
     * Calculates a hash code for this object.
     * @returns {number} The hash code.
     */
    public hashCode(): number  {
        return SmartHomeClientGuiApiOnRequestHistoryResultArguments.staticHashCode(this);
        
    }
    /**
     * Checks if this object is equal to another.
     * @param {any} other - The object to compare with.
     * @returns {boolean} True if the objects are equal, false otherwise.
     */
    public equals(other: any): boolean  {
        return SmartHomeClientGuiApiOnRequestHistoryResultArguments.staticEquals(this, other);
        
    }
    public toAString(result: AString): AString  {
        SmartHomeClientGuiApiOnRequestHistoryResultArguments.META.metaToString(this, result);
        return result;
        
    }
    
}
export interface SmartHomeClientGuiApi  {
    /**
     * @param deviceUid - UUID
     * @param records - SensorRecord[]
     *
     * @aetherMethodId 20
     */
    deviceStateUpdated(deviceUid: UUID, records: SensorRecord[]): void;
    deviceStateUpdatedArguments?(args: SmartHomeClientGuiApiDeviceStateUpdatedArguments): void;
    /**
     * @param devices - UUID[]
     *
     * @aetherMethodId 21
     */
    onGetDevicesResult(devices: UUID[]): void;
    onGetDevicesResultArguments?(args: SmartHomeClientGuiApiOnGetDevicesResultArguments): void;
    /**
     * @param deviceUid - UUID
     * @param records - SensorRecord[]
     *
     * @aetherMethodId 24
     */
    onRequestHistoryResult(deviceUid: UUID, records: SensorRecord[]): void;
    onRequestHistoryResultArguments?(args: SmartHomeClientGuiApiOnRequestHistoryResultArguments): void;
    
}
export namespace SmartHomeClientGuiApi  {
    export const META: FastMetaApi<SmartHomeClientGuiApi, SmartHomeClientGuiApiRemote> = new Impl.SmartHomeClientGuiApiMetaImpl();
    
}
export interface SmartHomeClientGuiApiRemote extends SmartHomeClientGuiApi, RemoteApi  {
    
}
export abstract class SmartHomeClientGuiApiLocal<RT extends RemoteApi> implements SmartHomeClientGuiApi  {
    protected readonly remoteApi: RT;
    public getRemoteApi(): RT  {
        return this.remoteApi;
        
    }
    protected constructor(remoteApi: RT)  {
        this.remoteApi = remoteApi;
        
    }
    /**
     * @param deviceUid - UUID
     * @param records - SensorRecord[]
     *
     * @aetherMethodId 20
     */
    public abstract deviceStateUpdated(deviceUid: UUID, records: SensorRecord[]): void;
    public deviceStateUpdatedArguments(args: SmartHomeClientGuiApiDeviceStateUpdatedArguments): void  {
        this.deviceStateUpdated(args.deviceUid, args.records);
        
    }
    /**
     * @param devices - UUID[]
     *
     * @aetherMethodId 21
     */
    public abstract onGetDevicesResult(devices: UUID[]): void;
    public onGetDevicesResultArguments(args: SmartHomeClientGuiApiOnGetDevicesResultArguments): void  {
        this.onGetDevicesResult(args.devices);
        
    }
    /**
     * @param deviceUid - UUID
     * @param records - SensorRecord[]
     *
     * @aetherMethodId 24
     */
    public abstract onRequestHistoryResult(deviceUid: UUID, records: SensorRecord[]): void;
    public onRequestHistoryResultArguments(args: SmartHomeClientGuiApiOnRequestHistoryResultArguments): void  {
        this.onRequestHistoryResult(args.deviceUid, args.records);
        
    }
    
}
export interface SmartHomeClientDeviceApi  {
    
}
export namespace SmartHomeClientDeviceApi  {
    export const EMPTY: SmartHomeClientDeviceApi =  {
        
    };
    export const META: FastMetaApi<SmartHomeClientDeviceApi, SmartHomeClientDeviceApiRemote> = new Impl.SmartHomeClientDeviceApiMetaImpl();
    
}
export interface SmartHomeClientDeviceApiRemote extends SmartHomeClientDeviceApi, RemoteApi  {
    
}
export abstract class SmartHomeClientDeviceApiLocal<RT extends RemoteApi> implements SmartHomeClientDeviceApi  {
    protected readonly remoteApi: RT;
    public getRemoteApi(): RT  {
        return this.remoteApi;
        
    }
    protected constructor(remoteApi: RT)  {
        this.remoteApi = remoteApi;
        
    }
    
}