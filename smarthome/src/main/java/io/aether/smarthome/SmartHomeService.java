package io.aether.smarthome;

import io.aether.StandardUUIDs;
import io.aether.api.common.CryptoLib;
import io.aether.api.smarthome.*;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.cloud.client.MessageNode;
import io.aether.logger.Log;
// [УДАЛЕНО] FastApiContext больше не нужен здесь
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SmartHomeService {

    private final AetherCloudClient client;

    // --- Состояние Сервиса ---

    // [ИЗМЕНЕНО] Главный реестр теперь хранит наши Контексты
    private final ConcurrentHashMap<UUID, ServiceApiContext> activeContexts = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, ClientType> clientRegistry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PendingPairing> pendingPairings = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Device> pairedDevices = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UUID> pairedCommutators = new ConcurrentHashMap<>();

    public SmartHomeService() {

        List<URI> registrationUris = List.of(URI.create("tcp://reg-dev.aethernet.io:9010"));
        UUID parentUuid = StandardUUIDs.ANONYMOUS_UID;

        ClientStateInMemory state = new ClientStateInMemory(
                parentUuid,
                registrationUris,
                null,
                CryptoLib.HYDROGEN
        );

        this.client = new AetherCloudClient(state, "SmartHomeService");

        client.onClientStream(m -> {

            UUID senderUuid = m.getConsumerUUID();
            Log.info("New API stream connection from: %s", senderUuid);

            ServiceApiContext context = activeContexts.computeIfAbsent(senderUuid, key -> new ServiceApiContext(m));

            context.setClientType(clientRegistry.get(senderUuid));

            m.toApi(context, SmartHomeServiceApi.META, new SmartHomeServiceApi() {

                @Override
                public void register(ClientType type, HardwareSensor[] sensors, HardwareActor[] actors) {
                    Log.info("API CALL from %s: register(type=%s)", senderUuid, type);

                    context.setClientType(type);
                    clientRegistry.put(senderUuid, type);

                    if (type == ClientType.COMMUTATOR) {
                        if (pairedCommutators.containsKey(senderUuid)) {
                            Log.info("Commutator %s re-registered.", senderUuid);
                            return;
                        }
                        Log.warn("register: STUB! DTO 'PendingPairing' must have setters.");
                        // TODO: ... (логика)
                    }
                }

                @Override
                public ARFuture<Device[]> getAllDevices() {
                    Log.info("API CALL from %s: getAllDevices", senderUuid);
                    if (context.getClientType() != ClientType.GUI_CLIENT) {
                        throw new RuntimeException("Permission denied");
                    }
                    return ARFuture.of(pairedDevices.values().toArray(new Device[0]));
                }

                @Override
                public ARFuture<Actor> executeActorCommand(UUID actorId, String command) {
                    Log.info("API CALL from %s: executeActorCommand(actorId=%s)", senderUuid, actorId);

                    if (context.getClientType() != ClientType.GUI_CLIENT) {
                        throw new RuntimeException("Permission denied");
                    }

                    Device device = pairedDevices.get(actorId);
                    if (device == null) throw new RuntimeException("Actor not found");
                    if (!(device instanceof Actor)) throw new RuntimeException("Device is not an Actor");

                    UUID commutatorUuid = device.getCommutatorId();
                    int localId = device.getLocalDeviceId();

                    ServiceApiContext commutatorContext = activeContexts.get(commutatorUuid);
                    if (commutatorContext == null) {
                        Log.error("Commutator %s is not connected", commutatorUuid);
                        throw new RuntimeException("Commutator is offline");
                    }

                    SmartHomeCommutatorApiRemote remote = commutatorContext.getCommutatorApiRemote();

                    ARFuture<DeviceStateData> responseFuture = remote.executeActorCommand(localId, command);

                    commutatorContext.flush(AFuture.make());

                    Log.info("Routed command '%s' to Commutator %s", command, commutatorUuid);

                    // TODO: Дождаться 'responseFuture', обновить 'device' и вернуть
                    return ARFuture.of((Actor) device);
                }

                @Override
                public ARFuture<PendingPairing[]> getPendingPairings() {
                    Log.info("API CALL from %s: getPendingPairings", senderUuid);
                    if (context.getClientType() != ClientType.GUI_CLIENT) {
                        throw new RuntimeException("Permission denied");
                    }
                    return ARFuture.of(pendingPairings.values().toArray(new PendingPairing[0]));
                }

                @Override
                public void approvePairing(UUID commutatorUuid) {
                    Log.info("API CALL from %s: approvePairing(commutator=%s)", senderUuid, commutatorUuid);

                    if (context.getClientType() != ClientType.GUI_CLIENT) {
                        Log.error("Permission denied (not a GUI)");
                        return;
                    }
                    if (clientRegistry.get(commutatorUuid) != ClientType.COMMUTATOR) {
                        Log.error("Target is not a Commutator");
                        return;
                    }

                    PendingPairing pending = pendingPairings.remove(commutatorUuid);
                    if (pending == null) {
                        Log.error("Pairing not found");
                        return;
                    }

                    pairedCommutators.put(commutatorUuid, senderUuid);
                    Log.info("Commutator %s approved by %s", commutatorUuid, senderUuid);
                    Log.warn("approvePairing: STUB! DTOs must have setters.");

                    ServiceApiContext commutatorContext = activeContexts.get(commutatorUuid);
                    if (commutatorContext != null) {
                        SmartHomeCommutatorApiRemote remote = commutatorContext.getCommutatorApiRemote();
                        remote.confirmPairing();
                        commutatorContext.flush(AFuture.make());
                    } else {
                        Log.warn("Commutator %s approved, but is not connected.", commutatorUuid);
                        // TODO: Установить флаг "ожидает confirm" в БД
                    }
                }

                @Override
                public void pushSensorData(int localSensorId, DeviceStateData data) {
                    Log.info("API CALL from %s: pushSensorData(sensor=%d, val=%s)", senderUuid, localSensorId, data.getValue());

                    if (context.getClientType() != ClientType.COMMUTATOR) {
                        Log.error("Permission denied (not a Commutator)");
                        return;
                    }
                    if (!pairedCommutators.containsKey(senderUuid)) {
                        Log.warn("Ignoring data from un-approved Commutator: " + senderUuid);
                        return;
                    }

                    Log.warn("pushSensorData: STUB! DTOs must have setters.");
                    // TODO: Найти Device по senderUuid + localSensorId
                    // TODO: Обновить 'device.lastState'

                    for (ServiceApiContext guiContext : activeContexts.values()) {
                        if (guiContext.getClientType() == ClientType.GUI_CLIENT) {
                            Log.debug("Pushing update to GUI: %s", guiContext.getSenderUuid());
                        }
                    }
                }
            });
        });
    }

    /**
     * Запускает сервис и слушает 'startFuture'.
     */
    public void start() {
        Log.info("Starting SmartHomeService...");

        client.connect().to(() -> {
            Log.info("### SmartHomeService IS ONLINE ###");
            Log.info("Service UUID: " + client.getUid());
        }).onError(error -> {
            Log.error("Failed to start SmartHomeService", error);
        });
    }

    /**
     * Точка входа.
     */
    public static void main(String[] args) {
        try {
            SmartHomeService service = new SmartHomeService();
            service.start();
            Thread.currentThread().join();
        } catch (Exception e) {
            Log.error("FATAL ERROR in SmartHomeService", e);
            System.exit(1);
        }
    }
}