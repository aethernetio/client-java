import { SmartHomeController } from './SmartHomeController';
import { DeviceStateData } from './aether_api';

export interface MetricPointRuntime {
    timestamp: number;
    rtt: number | null; // null indicates packet loss
}

interface PollTarget {
    uuid: string;
    id: number;
}

export class PollingManager {
    private controller: SmartHomeController;

    // Store targets as objects {uuid, id}
    private targets: PollTarget[] = [];

    private isRunning = false;
    private timeoutMs = 2000;

    // Buffer for sliding stats
    private bufferSize = 50; // Default buffer size
    private resultBuffer: boolean[] = [];

    // Public stats
    public totalRequests = 0;
    public totalResponses = 0;
    public stats: MetricPointRuntime[] = [];

    public onStatsUpdate: (stat: MetricPointRuntime, lossRate: number, tReq: number, tRes: number) => void = () => {};
    public onDataReceived: (uuid: string, deviceId: number, data: DeviceStateData) => void = () => {};

    constructor(controller: SmartHomeController) {
        this.controller = controller;
    }

    // --- Config Methods ---

    public setBufferSize(size: number): void {
        this.bufferSize = size;
        // Trim existing buffers if needed
        if (this.resultBuffer.length > size) {
            this.resultBuffer = this.resultBuffer.slice(-size);
        }
        if (this.stats.length > size) {
            this.stats = this.stats.slice(-size);
        }
    }

    // ADDED: Missing getter fixed here
    public getBufferSize(): number {
        return this.bufferSize;
    }

    public setTimeout(ms: number): void {
        this.timeoutMs = ms;
    }

    // --- Target Management ---

    public addTarget(uuid: string, id: number): void {
        const exists = this.targets.some(t => t.uuid === uuid && t.id === id);
        if (!exists) {
            this.targets.push({ uuid, id });
            if (!this.isRunning) this.start();
        }
    }

    public removeTarget(uuid: string, id: number): void {
        this.targets = this.targets.filter(t => !(t.uuid === uuid && t.id === id));
    }

    public clearTargets(): void {
        this.targets = [];
    }

    // --- State Management ---

    public start(): void {
        if (this.isRunning) return;
        this.isRunning = true;
        this.loop();
    }

    public stop(): void {
        this.isRunning = false;
    }

    public resetStats(): void {
        this.totalRequests = 0;
        this.totalResponses = 0;
        this.resultBuffer = [];
        this.stats = [];
    }

    public restoreState(stats: MetricPointRuntime[], tReq: number, tRes: number): void {
        this.stats = stats;
        this.totalRequests = tReq;
        this.totalResponses = tRes;
        // Infer buffer success from RTT presence (if rtt is not null, it was a success)
        this.resultBuffer = stats.map(s => s.rtt !== null);
        // Trim if restored state is larger than current default buffer
        this.setBufferSize(this.bufferSize);
    }

    // --- Loop ---

    private async loop(): Promise<void> {
        if (!this.isRunning) return;

        // Need targets AND at least one connection in controller
        if (this.targets.length === 0 || this.controller.connections.size === 0) {
            setTimeout(() => this.loop(), 1000);
            return;
        }

        // Round Robin selection
        const target = this.targets[this.totalRequests % this.targets.length];

        // Check if we are actually connected to this target's commutator
        if (!this.controller.connections.has(target.uuid)) {
             // Skip this target if disconnected.
             // Small delay to avoid tight loop if all targets are disconnected
             setTimeout(() => this.loop(), 100);
             return;
        }

        this.totalRequests++;
        const startTime = Date.now();
        let rtt: number | null = null;
        let isSuccess = false;

        try {
            const timeoutPromise = new Promise<never>((_, reject) =>
                setTimeout(() => reject(new Error("Timeout")), this.timeoutMs)
            );

            const requestFuture = this.controller.queryState(target.uuid, target.id);
            const response = await Promise.race([requestFuture, timeoutPromise]) as DeviceStateData;

            const endTime = Date.now();
            rtt = endTime - startTime;
            isSuccess = true;
            this.totalResponses++;

            if (response) {
                this.onDataReceived(target.uuid, target.id, response);
            }

        } catch (e) {
            rtt = null;
            isSuccess = false;
        }

        // Stats Buffer
        this.resultBuffer.push(isSuccess);
        if (this.resultBuffer.length > this.bufferSize) this.resultBuffer.shift();

        const metric: MetricPointRuntime = { timestamp: startTime, rtt: rtt };
        this.stats.push(metric);
        if (this.stats.length > this.bufferSize) this.stats.shift();

        // Window Loss Calculation
        const losses = this.resultBuffer.filter(x => !x).length;
        const totalWin = this.resultBuffer.length;
        const lossRate = totalWin > 0 ? (losses / totalWin) * 100 : 0;

        this.onStatsUpdate(metric, lossRate, this.totalRequests, this.totalResponses);

        if (this.isRunning) {
            setTimeout(() => this.loop(), 1000);
        }
    }
}