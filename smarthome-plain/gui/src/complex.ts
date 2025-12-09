import { SmartHomeController } from './SmartHomeController';
import { Chart, registerables } from 'chart.js';
import { Record } from './aether_api';

Chart.register(...registerables);

const CORE_URI = "wss://reg-dev.aethernet.io:9012";
// const CORE_URI = "wss://localhost:9012";
const WINDOW_SIZE = 50;
const STORAGE_KEY = 'aether_complex_v1';

interface DeviceStats {
    uuid: string;
    shortId: string;
    isActive: boolean;
    isPlotting: boolean;
    isPaused: boolean;
    isLoopRunning: boolean;
    totalRequests: number;
    totalLost: number;
    windowBuffer: boolean[];
    dataPoints: {x: number, y: number}[];
    rttPoints: {x: number, y: number}[];
    lossTotalPoints: {x: number, y: number}[];
    lossWindowPoints: {x: number, y: number}[];
    color: string;
}

interface SavedState {
    pollPeriod: number;
    pollTimeout: number;
    requestCount: number;
    showLossTotal: boolean;
    showLossWin: boolean;
    showRtt: boolean;
    devices: { uuid: string; isPlotting: boolean; isPaused: boolean; color: string; }[];
}

const devices = new Map<string, DeviceStats>();
const controller = new SmartHomeController();
let chart: Chart | null = null;
let isResetting = false;

const periodInput = document.getElementById('poll-period') as HTMLInputElement;
const timeoutInput = document.getElementById('poll-timeout') as HTMLInputElement;
const countInput = document.getElementById('req-count') as HTMLInputElement;
const uuidInput = document.getElementById('new-uuid') as HTMLInputElement;
const tableBody = document.getElementById('device-list-body')!;
const coreStatus = document.getElementById('core-status')!;
const globalStats = document.getElementById('global-stats')!;

const optLossTotal = document.getElementById('show-loss-total') as HTMLInputElement;
const optLossWin = document.getElementById('show-loss-win') as HTMLInputElement;
const optRtt = document.getElementById('show-rtt') as HTMLInputElement;

async function init() {
    initChart();
    loadState();

    controller.onConnectionState.add(state => {
        if (state === 'core_connected') {
            coreStatus.innerHTML = '<span class="status-dot dot-green"></span> Core Online';
            checkUrlAndConnect();
            devices.forEach(d => {
                if (d.isActive) controller.connectDevice(d.uuid);
            });
        } else if (state === 'disconnected') {
            coreStatus.innerHTML = '<span class="status-dot dot-red"></span> Disconnected';
        } else {
            coreStatus.innerHTML = '<span class="status-dot dot-gray"></span> Connecting...';
        }
    });

    controller.onDeviceConnected.add(uuid => {
        if (!devices.has(uuid)) {
            addDeviceToState(uuid);
            renderTable();
            saveState();
        }
        const dev = devices.get(uuid)!;
        if (!dev.isLoopRunning) startPollingLoop(dev);
    });

    document.getElementById('btn-add')!.onclick = () => {
        const uuid = uuidInput.value.trim();
        if (uuid) {
            connectAndAdd(uuid);
            uuidInput.value = '';
        }
    };

    document.getElementById('btn-reset')!.onclick = () => {
        if (confirm("Full Reset?")) {
            isResetting = true;
            localStorage.removeItem(STORAGE_KEY);
            window.location.reload();
        }
    };

    [optLossTotal, optLossWin, optRtt, periodInput, timeoutInput, countInput].forEach(el => {
        el.onchange = () => saveState();
    });

    window.addEventListener('beforeunload', () => saveState());
    setInterval(updateChartUI, 250);

    await controller.connectCore(CORE_URI);
}

function connectAndAdd(uuid: string) {
    if (!devices.has(uuid)) {
        addDeviceToState(uuid);
        renderTable();
        saveState();
    }
    controller.connectDevice(uuid);
}

function addDeviceToState(uuid: string) {
    const stats: DeviceStats = {
        uuid,
        shortId: uuid.substring(0, 6),
        isActive: true,
        isPlotting: true,
        isPaused: false,
        isLoopRunning: false,
        totalRequests: 0,
        totalLost: 0,
        windowBuffer: [],
        dataPoints: [],
        rttPoints: [],
        lossTotalPoints: [],
        lossWindowPoints: [],
        color: `hsl(${Math.random() * 360}, 70%, 50%)`
    };
    devices.set(uuid, stats);
}

function checkUrlAndConnect() {
    const params = new URLSearchParams(window.location.search);
    const targetUuid = params.get('uuid');
    if (targetUuid) {
        connectAndAdd(targetUuid);
    }
}

async function startPollingLoop(device: DeviceStats) {
    if (device.isLoopRunning) return;
    device.isLoopRunning = true;

    while (device.isActive) {
        const periodMs = parseInt(periodInput.value) || 500;
        if (device.isPaused) {
            await new Promise(r => setTimeout(r, 500));
            continue;
        }

        const timeoutMs = parseInt(timeoutInput.value) || 2000;
        const reqCount = parseInt(countInput.value) || 50;
        device.totalRequests++;
        const startTime = Date.now();
        let success = false;

        try {
            const timeoutPromise = new Promise<never>((_, reject) =>
                setTimeout(() => reject(new Error("TIMEOUT")), timeoutMs)
            );
            const requestPromise = controller.requestRecords(device.uuid, reqCount);
            const records = await Promise.race([requestPromise, timeoutPromise]);
            success = true;
            const rtt = Date.now() - startTime;

            if (records && records.length > 0) {
                device.dataPoints = [];
                let cursorTime = Date.now();
                for (const rec of records) {
                    const rawVal = rec.value & 0xFF;
                    const temperature = (rawVal / 3.0) - 30.0;
                    device.dataPoints.push({ x: cursorTime, y: temperature });
                    cursorTime -= (rec.time & 0xFF) * 1000;
                }
            }

            device.rttPoints.push({x: Date.now(), y: rtt});
            if (device.rttPoints.length > 50) device.rttPoints.shift();

        } catch (e) {
            success = false;
            device.totalLost++;
        }

        if (!device.isActive) break;

        const now = Date.now();
        device.windowBuffer.push(success);
        if (device.windowBuffer.length > WINDOW_SIZE) device.windowBuffer.shift();

        const lossPct = (device.windowBuffer.filter(x => !x).length / device.windowBuffer.length) * 100;
        device.lossTotalPoints.push({x: now, y: device.totalLost});
        if (device.lossTotalPoints.length > 50) device.lossTotalPoints.shift();
        device.lossWindowPoints.push({x: now, y: lossPct});
        if (device.lossWindowPoints.length > 50) device.lossWindowPoints.shift();

        await new Promise(r => setTimeout(r, periodMs));
    }
    device.isLoopRunning = false;
}

function saveState() {
    if (isResetting) return;
    const state: SavedState = {
        pollPeriod: parseInt(periodInput.value) || 500,
        pollTimeout: parseInt(timeoutInput.value) || 2000,
        requestCount: parseInt(countInput.value) || 50,
        showLossTotal: optLossTotal.checked,
        showLossWin: optLossWin.checked,
        showRtt: optRtt.checked,
        devices: []
    };
    devices.forEach(d => {
        state.devices.push({ uuid: d.uuid, isPlotting: d.isPlotting, isPaused: d.isPaused, color: d.color });
    });
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function loadState() {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return;
    try {
        const state: SavedState = JSON.parse(raw);
        if (state.pollPeriod) periodInput.value = state.pollPeriod.toString();
        if (state.pollTimeout) timeoutInput.value = state.pollTimeout.toString();
        if (state.requestCount) countInput.value = state.requestCount.toString();
        optLossTotal.checked = !!state.showLossTotal;
        optLossWin.checked = !!state.showLossWin;
        optRtt.checked = !!state.showRtt;
        if (state.devices) {
            state.devices.forEach(savedDev => {
                const stats: DeviceStats = {
                    uuid: savedDev.uuid,
                    shortId: savedDev.uuid.substring(0, 6),
                    isActive: true,
                    isPlotting: savedDev.isPlotting,
                    isPaused: !!savedDev.isPaused,
                    isLoopRunning: false,
                    totalRequests: 0,
                    totalLost: 0,
                    windowBuffer: [],
                    dataPoints: [],
                    rttPoints: [],
                    lossTotalPoints: [],
                    lossWindowPoints: [],
                    color: savedDev.color
                };
                devices.set(savedDev.uuid, stats);
            });
            renderTable();
        }
    } catch (e) {}
}

function initChart() {
    const ctx = document.getElementById('main-chart') as HTMLCanvasElement;
    chart = new Chart(ctx, {
        type: 'line',
        data: { datasets: [] },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            animation: false,
            parsing: false,
            interaction: { mode: 'nearest', axis: 'x', intersect: false },
            scales: {
                x: { type: 'linear', position: 'bottom', ticks: { callback: (val) => new Date(Number(val)).toLocaleTimeString() } },
                y: { beginAtZero: false }
            },
            plugins: { legend: { position: 'top', labels: { boxWidth: 10, padding: 10, font: { size: 11 } } } }
        }
    });
}

function updateChartUI() {
    if (!chart) return;
    let totalReq = 0;
    devices.forEach(d => totalReq += d.totalRequests);
    globalStats.textContent = `Total Reqs: ${totalReq}`;

    const datasets: any[] = [];
    devices.forEach(d => {
        if (!d.isPlotting || !d.isActive) return;
        datasets.push({ label: `${d.shortId} (°C)`, data: d.dataPoints, borderColor: d.color, borderWidth: 2, pointRadius: 1, tension: 0.1 });
        if (optRtt.checked) datasets.push({ label: `${d.shortId} (RTT)`, data: d.rttPoints, borderColor: d.color, borderDash: [5, 5], borderWidth: 1, pointRadius: 0 });
        if (optLossTotal.checked) datasets.push({ label: `${d.shortId} (Loss)`, data: d.lossTotalPoints, borderColor: '#e74c3c', borderWidth: 1, pointRadius: 0 });
        if (optLossWin.checked) datasets.push({ label: `${d.shortId} (Loss %)`, data: d.lossWindowPoints, borderColor: '#f39c12', borderDash: [2, 2], borderWidth: 1, pointRadius: 0 });
    });
    chart.data.datasets = datasets;
    chart.update();
}

function renderTable() {
    tableBody.innerHTML = '';
    devices.forEach(d => {
        if (!d.isActive) return;
        const tr = document.createElement('tr');

        const tdCheck = document.createElement('td');
        const cb = document.createElement('input');
        cb.type = 'checkbox';
        cb.checked = d.isPlotting;
        cb.onchange = () => { d.isPlotting = cb.checked; saveState(); };
        tdCheck.appendChild(cb);

        const tdInfo = document.createElement('td');
        const divId = document.createElement('div');
        divId.style.fontWeight = 'bold';
        divId.style.fontFamily = 'monospace';
        divId.style.color = d.color;
        divId.style.marginBottom = '4px';
        divId.textContent = d.shortId;
        divId.title = d.uuid;

        const divBtns = document.createElement('div');
        divBtns.style.display = 'flex';
        divBtns.style.gap = '5px';
        const btnPause = document.createElement('button');
        btnPause.textContent = d.isPaused ? '▶' : '⏸';
        btnPause.className = d.isPaused ? 'btn-icon btn-run' : 'btn-icon btn-pause';
        btnPause.onclick = () => { d.isPaused = !d.isPaused; saveState(); renderTable(); };
        divBtns.appendChild(btnPause);
        tdInfo.appendChild(divId);
        tdInfo.appendChild(divBtns);

        const tdDel = document.createElement('td');
        const btnDel = document.createElement('button');
        btnDel.textContent = 'X';
        btnDel.className = 'btn-icon btn-del';
        btnDel.onclick = () => {
            d.isActive = false;
            d.isLoopRunning = false;
            controller.disconnectDevice(d.uuid);
            devices.delete(d.uuid);
            saveState();
            renderTable();
        };
        tdDel.appendChild(btnDel);

        tr.appendChild(tdCheck);
        tr.appendChild(tdInfo);
        tr.appendChild(tdDel);
        tableBody.appendChild(tr);
    });
}

document.addEventListener('DOMContentLoaded', init);