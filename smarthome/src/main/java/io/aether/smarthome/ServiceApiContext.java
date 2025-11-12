package io.aether.smarthome;

import io.aether.api.smarthome.ClientType;
import io.aether.api.smarthome.SmartHomeClientApiRemote;
import io.aether.api.smarthome.SmartHomeCommutatorApiRemote;
import io.aether.cloud.client.MessageNode;
import io.aether.net.fastMeta.FastApiContext;
import io.aether.utils.futures.AFuture;

import java.util.UUID;

/**
 * Наш собственный FastApiContext, который хранит состояние
 * для каждого уникального соединения (MessageNode).
 * Он хранит UUID, тип клиента и готовые Remote API
 * для PUSH-сообщений *обратно* этому клиенту.
 */
public class ServiceApiContext extends FastApiContext {

    private final UUID senderUuid;
    private final MessageNode messageNode;

    // Remote API для PUSH-сообщений *этому* клиенту
    private final SmartHomeClientApiRemote clientApiRemote;
    private final SmartHomeCommutatorApiRemote commutatorApiRemote;

    private ClientType clientType;

    public ServiceApiContext(MessageNode m) {
        this.messageNode = m;
        this.senderUuid = m.getConsumerUUID(); //

        // Создаем "дешевые" Remote API, привязанные к ЭТОМУ контексту
        //
        this.clientApiRemote = SmartHomeClientApiRemote.META.makeRemote(this);
        this.commutatorApiRemote = SmartHomeCommutatorApiRemote.META.makeRemote(this);
    }

    /**
     * Отправляет (flush) данные обратно клиенту,
     * привязанному к этому контексту.
     */
    @Override
    public void flush(AFuture sendFuture) {
        //
        messageNode.send(remoteDataToArray()).to(sendFuture);
    }

    // --- Геттеры и Сеттеры для состояния сессии ---

    public UUID getSenderUuid() {
        return senderUuid;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    public SmartHomeClientApiRemote getClientApiRemote() {
        return clientApiRemote;
    }

    public SmartHomeCommutatorApiRemote getCommutatorApiRemote() {
        return commutatorApiRemote;
    }
}