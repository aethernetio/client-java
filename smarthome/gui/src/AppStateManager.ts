import {
    AppStateData,
    MetricPoint,
    KnownCommutator,
    SensorHistorySeries,
    GraphPoint
} from './aether_api';
import { DataInOut, DataInOutStatic } from 'aether-client/build/aether_client';
import { Base64 } from 'js-base64';
import { MetricPointRuntime } from './PollingManager';

export interface SensorDataSeries {
    commutatorUuid: string;
    deviceId: number;
    name: string;
    unit: string;
    data: {x: number, y: number}[];
}

export interface CommutatorMeta {
    lastSeen: number;
    req: number;
    res: number;
}

export class AppStateManager {
    private static readonly STORAGE_KEY = 'smarthome_app_state_v3_compact';

    static save(
        timeout: number,
        bufferSize: number,
        targetUuid: string | null,
        knownCommutators: Map<string, CommutatorMeta>,
        sensorHistory: Map<string, SensorDataSeries>,
        visibleKeys: Set<string>,
        rttStats: MetricPointRuntime[],
        totalReq: number,
        totalRes: number
    ): void {
        try {
            const commsDto: KnownCommutator[] = [];
            knownCommutators.forEach((meta, uuid) => {
                commsDto.push(new KnownCommutator(
                    uuid, null, BigInt(meta.lastSeen),
                    meta.req, meta.res
                ));
            });

            const historyDto: SensorHistorySeries[] = [];
            sensorHistory.forEach(series => {
                const points = series.data.map(p => new GraphPoint(BigInt(p.x), p.y));
                const recentPoints = points.slice(-100);
                historyDto.push(new SensorHistorySeries(
                    series.commutatorUuid, series.deviceId, series.name, series.unit, recentPoints
                ));
            });

            const metricsDto = rttStats.map(s =>
                new MetricPoint(BigInt(s.timestamp), s.rtt === null ? -1 : s.rtt)
            );

            // FIX: Calculate loss count for persistence, as 'totalResponses' is not in AppStateData ADSL
            const lossCount = totalReq - totalRes;

            const dto = new AppStateData(
                timeout,
                bufferSize,
                0, // targetDeviceId unused
                targetUuid,
                commsDto,
                historyDto,
                metricsDto,
                totalReq,
                lossCount, // Saved as packetLossCount
                Array.from(visibleKeys)
            );

            const stream = new DataInOut();
            AppStateData.META.serialize(null, dto, stream);
            localStorage.setItem(this.STORAGE_KEY, Base64.fromUint8Array(stream.toArray()));

        } catch (e) {
            console.error("Failed to save app state:", e);
        }
    }

    static load(): AppStateData | null {
        const stored = localStorage.getItem(this.STORAGE_KEY);
        if (!stored) return null;
        try {
            const bytes = Base64.toUint8Array(stored);
            const stream = new DataInOutStatic(bytes);
            return AppStateData.META.deserialize(null, stream);
        } catch (e) {
            console.error("Failed to load app state:", e);
            return null;
        }
    }
}