import { SmartHomeController } from './SmartHomeController';
import { HardwareDevice, HardwareSensor, DeviceStateData, VariantDouble, VariantBool, VariantString } from './aether_api';
import { PollingManager, MetricPointRuntime } from './PollingManager';
import { AppStateManager, SensorDataSeries, CommutatorMeta } from './AppStateManager';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

// --- FIX: MAKE CHART FONTS READABLE ON MOBILE ---
Chart.defaults.font.size = 14;
Chart.defaults.font.family = '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif';
// ------------------------------------------------

declare global {
    interface Window {
        connectToUuid: (uuid: string) => Promise<void>;
        removeCommutator: (uuid: string) => void;
        sendCmd: (uuid: string, id: number, val: string) => void;
        toggleGraph: (key: string, show: boolean) => void;
    }
}

// DOM
const statusText = document.getElementById('status-text')!;
const resetButton = document.getElementById('reset-session-button')!;
const commutatorsTbody = document.getElementById('commutators-table-body')!;
const devicesGrid = document.getElementById('devices-grid')!;

const coreConnectionSection = document.getElementById('core-connection-section')!;
const uriInput = document.getElementById('reg-uri-input') as HTMLInputElement;
const connectCoreButton = document.getElementById('connect-core-button')!;

const appConfigSection = document.getElementById('app-config-section')!;
const newUuidInput = document.getElementById('new-uuid-input') as HTMLInputElement;
const addButton = document.getElementById('add-commutator-button')!;

const pollingInput = document.getElementById('polling-timeout-input') as HTMLInputElement;
const bufferInput = document.getElementById('buffer-size-input') as HTMLInputElement;
const windowLossDisp = document.getElementById('window-loss-disp')!;
const totalReqDisp = document.getElementById('total-req-disp')!;

const rttCanvas = document.getElementById('rtt-chart') as HTMLCanvasElement;
const sensorCanvas = document.getElementById('sensor-chart') as HTMLCanvasElement;

// STATE
const controller = new SmartHomeController();
const poller = new PollingManager(controller);

let rttChart: Chart | null = null;
let sensorChart: Chart | null = null;

const globalSensorHistory = new Map<string, SensorDataSeries>();
const knownCommutators = new Map<string, CommutatorMeta>();
const visibleSeriesKeys = new Set<string>();

let activeCommutatorUuid: string | null = null;
let isAetherCoreConnected = false;
let isResetting = false;

// HELPERS
const makeKey = (uuid: string, id: number) => `${uuid}:${id}`;
const parseKey = (key: string) => {
    const p = key.split(':');
    return { uuid: p[0], id: parseInt(p[1]) };
};
const timeStr = (ts: number | string) => new Date(Number(ts)).toLocaleTimeString();

// CHART FUNCTIONS
function initCharts() {
    const commonOpts = {
        animation: false as const,
        maintainAspectRatio: false,
        responsive: true,
        scales: {
            x: { type: 'linear' as const, position: 'bottom' as const, ticks: { callback: (val: any) => timeStr(val) } }
        },
        plugins: {
            legend: {
                labels: {
                    font: { size: 14 }
                }
            }
        }
    };

    if (rttCanvas) {
        rttChart = new Chart(rttCanvas, {
            type: 'line',
            data: { datasets: [{ label: 'RTT (ms)', data: [], borderColor: '#17a2b8', tension: 0.1 }] },
            options: { ...commonOpts, scales: { ...commonOpts.scales, y: { beginAtZero: true } } }
        });
    }

    if (sensorCanvas) {
        sensorChart = new Chart(sensorCanvas, {
            type: 'line',
            data: { datasets: [] },
            options: commonOpts
        });
    }
}

function updateRttGraph(pt: MetricPointRuntime) {
    if (!rttChart) return;
    rttChart.data.datasets[0].data.push({x: pt.timestamp, y: pt.rtt === null ? null : pt.rtt});
    const lim = poller.getBufferSize();
    while (rttChart.data.datasets[0].data.length > lim) {
        rttChart.data.datasets[0].data.shift();
    }
    rttChart.update();
}

function updateStatsUI() {
    if (totalReqDisp) totalReqDisp.textContent = poller.totalRequests.toString();
    if (windowLossDisp) {
         // Updated via poller callback usually
    }
}

function updateSensorGraph() {
    if (!sensorChart) return;
    const datasets: any[] = [];
    visibleSeriesKeys.forEach(key => {
        const series = globalSensorHistory.get(key);
        if (!series) return;
        let hash = 0;
        for (let i = 0; i < key.length; i++) hash = key.charCodeAt(i) + ((hash << 5) - hash);
        const color = `hsl(${Math.abs(hash % 360)}, 70%, 50%)`;
        datasets.push({
            label: `${series.name} [${series.deviceId}]`,
            data: series.data,
            borderColor: color,
            backgroundColor: color,
            tension: 0.1,
            pointRadius: 3,
            fill: false
        });
    });
    sensorChart.data.datasets = datasets;
    sensorChart.update();
}

function syncPollerTargets() {
    if (!isAetherCoreConnected) return;
    poller.clearTargets();
    visibleSeriesKeys.forEach(key => {
        const { uuid, id } = parseKey(key);
        if (uuid && !isNaN(id)) {
            poller.addTarget(uuid, id);
        }
    });
}

// INIT
async function init() {
    console.log("App Init...");

    if (resetButton) resetButton.onclick = () => {
        if(confirm("Reset?")) {
            isResetting = true; controller.client = null;
            localStorage.clear(); window.location.reload();
        }
    };

    if (connectCoreButton) connectCoreButton.onclick = connectAetherCoreHandler;
    if (addButton) addButton.onclick = addCommutatorHandler;

    const refreshBtn = document.getElementById('refresh-button');
    // FIX: Changed queryEverything -> queryAllSensorStates
    if (refreshBtn) refreshBtn.onclick = () => controller.queryAllSensorStates();

    if (pollingInput) pollingInput.onchange = () => poller.setTimeout(parseInt(pollingInput.value));
    if (bufferInput) bufferInput.onchange = () => poller.setBufferSize(parseInt(bufferInput.value));

    initCharts();

    // RESTORE
    try {
        const saved = AppStateManager.load();
        if (saved) {
            if (pollingInput) pollingInput.value = saved.pollingTimeoutMs.toString();
            if (bufferInput) bufferInput.value = saved.pollingBufferSize.toString();
            poller.setTimeout(saved.pollingTimeoutMs);
            poller.setBufferSize(saved.pollingBufferSize);

            saved.knownCommutators.forEach(c => knownCommutators.set(c.uuid.toString(), { lastSeen: Number(c.lastSeen), req: c.totalRequests, res: c.totalResponses }));

            saved.sensorHistory.forEach(h => {
                const u = h.commutatorUuid.toString();
                const key = makeKey(u, h.deviceId);
                const data = h.points.map(p => ({ x: Number(p.timestamp), y: p.value }));
                globalSensorHistory.set(key, { commutatorUuid: u, deviceId: h.deviceId, name: h.deviceName, unit: h.unit || "", data: data });
            });

            saved.visibleSensorKeys.forEach(k => visibleSeriesKeys.add(k));

            const metricsSource = saved.rttMetrics || [];
            const runtimeRtt = metricsSource.map(m => ({ timestamp: Number(m.timestamp), rtt: m.rtt === -1 ? null : m.rtt }));

            const restoredLoss = saved.packetLossCount;
            const restoredReq = saved.totalRequests;
            const restoredRes = restoredReq - restoredLoss;

            poller.restoreState(runtimeRtt, restoredReq, restoredRes);

            if (saved.targetCommutatorUuid) activeCommutatorUuid = saved.targetCommutatorUuid.toString();

            runtimeRtt.forEach(p => updateRttGraph(p));
            updateSensorGraph();
            updateStatsUI();
        }
    } catch (e) { console.error("Restore fail", e); }

    renderCommutatorTable();
    renderDeviceCards();

    // EVENTS
    controller.onDeviceListUpdate.add(evt => {
        const { uuid, devices } = evt;
        devices.forEach(d => {
            const key = makeKey(uuid, d.localId);
            if (!globalSensorHistory.has(key)) {
                globalSensorHistory.set(key, { commutatorUuid: uuid, deviceId: d.localId, name: d.descriptor, unit: (d instanceof HardwareSensor) ? (d.unit || '') : '', data: [] });
            } else {
                globalSensorHistory.get(key)!.name = d.descriptor;
            }
        });
        if (activeCommutatorUuid === uuid) renderDeviceCards();
    });

    const onData = (uuid: string, id: number, state: DeviceStateData) => {
        if (activeCommutatorUuid === uuid) {
            const card = document.getElementById(`card-${uuid}-${id}`);
            if (card) (card as any).updateVal(state);
        }
        if (state.payload instanceof VariantDouble) {
            const key = makeKey(uuid, id);
            if (!globalSensorHistory.has(key)) {
                 globalSensorHistory.set(key, { commutatorUuid: uuid, deviceId: id, name: `Device ${id}`, unit: '', data: [] });
            }
            const series = globalSensorHistory.get(key)!;
            series.data.push({ x: state.timestamp.getTime(), y: state.payload.value });
            if (series.data.length > 200) series.data.shift();

            if (visibleSeriesKeys.has(key)) updateSensorGraph();
        }
        const comm = knownCommutators.get(uuid);
        if (comm) { comm.lastSeen = Date.now(); renderCommutatorTable(); }
    };

    controller.onDeviceStateChanged.add(e => onData(e.uuid, e.id, e.state));
    poller.onDataReceived = (uuid, id, data) => onData(uuid, id, data);

    poller.onStatsUpdate = (pt, lossRate, tReq, tRes) => {
        if (windowLossDisp) windowLossDisp.textContent = lossRate.toFixed(1) + "%";
        if (totalReqDisp) totalReqDisp.textContent = tReq.toString();
        updateRttGraph(pt);

        if (activeCommutatorUuid && knownCommutators.has(activeCommutatorUuid)) {
             const c = knownCommutators.get(activeCommutatorUuid)!;
             c.req = tReq;
             c.res = tRes;
             renderCommutatorTable();
        }
    };

    window.addEventListener('beforeunload', () => {
        if (isResetting) return;
        if (controller.client) {
            controller.saveSession();
            AppStateManager.save(
                parseInt(pollingInput.value),
                parseInt(bufferInput.value),
                activeCommutatorUuid,
                knownCommutators,
                globalSensorHistory,
                visibleSeriesKeys,
                poller.stats,
                poller.totalRequests,
                poller.totalResponses
            );
        }
    });

    const restored = await controller.restoreSession();
    if (restored) {
        const uid = controller.client?.getUid()?.toString().toString();
        statusText.textContent = `Session Ready. UUID: ${uid?.substring(0,6)}...`;
        statusText.style.color = 'blue';
        if (connectCoreButton) {
            connectCoreButton.textContent = "Resume";
            connectCoreButton.className = "btn-green";
        }
    } else {
        statusText.textContent = "No Session. Connect to Core to start.";
    }
}

// HANDLERS

async function restoreConnections() {
    console.log("Restoring P2P connections...");
    knownCommutators.forEach((_, uuid) => {
        controller.connectCommutatorP2P(uuid)
            .then(() => console.log(`Connected to ${uuid}`))
            .catch(e => console.warn(`Bg connect fail ${uuid}`, e));
    });
}

window.connectToUuid = async (uuid: string) => {
    if (uuid === controller.client?.getUid()?.toString().toString()) {
        alert("Self-connect error"); return;
    }
    activeCommutatorUuid = uuid;
    renderCommutatorTable();
    renderDeviceCards();
    syncPollerTargets();

    try {
        await controller.connectCommutatorP2P(uuid);
        statusText.textContent = "P2P Connected";
    } catch (e) {
        statusText.textContent = "P2P Error";
    }
};

window.removeCommutator = (uuid: string) => {
    if(confirm("Delete?")) {
        knownCommutators.delete(uuid);
        controller.disconnectP2P(uuid);
        for (const key of globalSensorHistory.keys()) {
            if (key.startsWith(uuid + ":")) {
                globalSensorHistory.delete(key);
                visibleSeriesKeys.delete(key);
            }
        }
        if (activeCommutatorUuid === uuid) {
            activeCommutatorUuid = null;
            renderDeviceCards();
            poller.clearTargets();
        }
        renderCommutatorTable();
        updateSensorGraph();
    }
};

window.sendCmd = (uuid: string, id: number, val: string) => {
    controller.executeCommand(uuid, id, val);
};

window.toggleGraph = (key: string, show: boolean) => {
    if (show) visibleSeriesKeys.add(key);
    else visibleSeriesKeys.delete(key);

    if (isAetherCoreConnected) {
        syncPollerTargets();
    }
    updateSensorGraph();
};

async function connectAetherCoreHandler() {
    const regUri = uriInput.value.trim();
    if (!regUri) return;
    statusText.textContent = "Connecting...";
    try {
        if (controller.client) await controller.connect();
        else await controller.connectAetherCore(regUri);

        isAetherCoreConnected = true;
        statusText.textContent = "Online";
        statusText.style.color = "green";
        coreConnectionSection.style.display = 'none';

        restoreConnections();
        syncPollerTargets();
    } catch (e) {
        statusText.textContent = "Connect Failed";
    }
}

function addCommutatorHandler() {
    const uuid = newUuidInput.value.trim();
    if (uuid) {
        if(!knownCommutators.has(uuid)) {
            knownCommutators.set(uuid, { lastSeen: Date.now(), req: 0, res: 0 });
        }
        renderCommutatorTable();
        window.connectToUuid(uuid);
    }
}

function renderCommutatorTable() {
    if (!commutatorsTbody) return;
    commutatorsTbody.innerHTML = '';
    knownCommutators.forEach((meta, uuid) => {
        const tr = document.createElement('tr');
        if (uuid === activeCommutatorUuid) tr.className = 'active-row';
        // Полный UUID, моноширинный, 13px
        tr.innerHTML = `
            <td title="${uuid}" style="font-family: monospace; font-size: 0.85rem;">${uuid}</td>
            <td>${new Date(meta.lastSeen).toLocaleTimeString().split(' ')[0]}</td>
            <td>${meta.req}</td>
            <td>${meta.res}</td>
            <td>
                <button onclick="window.connectToUuid('${uuid}')" class="btn-green">Go</button>
                <button onclick="window.removeCommutator('${uuid}')" class="btn-red">X</button>
            </td>
        `;
        commutatorsTbody.appendChild(tr);
    });
}

function renderDeviceCards() {
    if (!devicesGrid) return;
    devicesGrid.innerHTML = '';
    if (!activeCommutatorUuid) {
        devicesGrid.innerHTML = '<div style="padding:10px; color:#777">Select a commutator...</div>';
        return;
    }

    const keys = Array.from(globalSensorHistory.keys()).sort();
    keys.forEach(key => {
        const series = globalSensorHistory.get(key)!;
        if (series.commutatorUuid !== activeCommutatorUuid) return;

        const card = document.createElement('div');
        card.className = 'device-card';
        card.id = `card-${series.commutatorUuid}-${series.deviceId}`;

        let controls = '';
        const isChecked = visibleSeriesKeys.has(key) ? 'checked' : '';
        controls = `<label><input type="checkbox" onchange="window.toggleGraph('${key}', this.checked)" ${isChecked}> Graph</label>`;

        card.innerHTML = `
            <h4>${series.name}</h4>
            <div class="val">--</div>
            <div style="margin-top:2px">${controls}</div>
        `;

        (card as any).updateVal = (data: DeviceStateData) => {
             const el = card.querySelector('.val');
             if(el) {
                 if (data.payload instanceof VariantDouble) el.textContent = data.payload.value.toFixed(1);
                 else if (data.payload instanceof VariantString) el.textContent = data.payload.value;
                 else if (data.payload instanceof VariantBool) {
                     el.textContent = data.payload.value ? "ON" : "OFF";
                     const ctrlDiv = card.querySelector('div:last-child');
                     if (ctrlDiv && !ctrlDiv.innerHTML.includes('button')) {
                         ctrlDiv.innerHTML = `
                            <button onclick="window.sendCmd('${series.commutatorUuid}', ${series.deviceId}, 'ON')">ON</button>
                            <button onclick="window.sendCmd('${series.commutatorUuid}', ${series.deviceId}, 'OFF')">OFF</button>
                         `;
                     }
                 }
             }
        };
        devicesGrid.appendChild(card);
    });
}

init();