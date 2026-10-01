
package io.aether.cloud.proxy;

import io.aether.api.CryptoUtils;
import io.aether.api.clientserverapi.ClientApiRegSafeStream;
import io.aether.api.clientserverapi.ClientApiRegUnsafeRemote;
import io.aether.api.clientserverapi.RegistrationRootApi;
import io.aether.api.clientserverapi.ServerRegistrationApiStream;
import io.aether.api.common.CryptoLib;
import io.aether.api.common.IPAddress;
import io.aether.api.common.IpInfo;
import io.aether.crypto.CryptoEngine;
import io.aether.logger.Log;
import io.aether.net.fastMeta.MetaContext;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.slots.AMFuture;

/**
 * Downstream registration endpoint of the proxy.
 *
 * <p>Unlike the old opaque forwarding proxy, this class terminates crypto.
 * The client encrypts registration data with the proxy's registration public
 * key; the proxy decrypts it using the matching private key, so it can see
 * and forward the same plaintext to the original registration server.
 */
public final class ProxyRegistrationRootApiImpl
        implements RegistrationRootApi {


    final ProxyService service;
    final MetaContext ctx;
    final ClientApiRegUnsafeRemote remoteApi;


    public ProxyRegistrationRootApiImpl(
            MetaContext ctx,
            ProxyService service
    ) {
        this.ctx = ctx;
        this.service = service;
        this.remoteApi = ctx.makeRemote(ClientApiRegUnsafeRemote.META);
    }

    @Override
    public void enter(
            CryptoLib cryptoLib,
            ServerRegistrationApiStream stream
    ) {
        Log.trace(
                "ProxyRegistrationRootApiImpl: enter",
                "cryptoLib",
                cryptoLib
        );

        AMFuture<CryptoEngine> backCryptoEngine = new AMFuture<>();
        IPAddress clientIp = service.clientIpFromContext(ctx);

        stream
                .asIn()
                .convert(data ->
                        service
                                .registrationPrivateEngine(cryptoLib)
                                .decrypt(data)
                )
                .keys(
                        context -> new ProxyServerRegistrationApiImpl(
                                remoteApi,
                                service,
                                cryptoLib,
                                backCryptoEngine,
                                clientIp
                        ),
                        "proxy client-reg enter"
                )
                .onFlushData(data ->
                        backCryptoEngine.once(engine ->
                                remoteApi.enter(
                                        ClientApiRegSafeStream
                                                .Out
                                                .send(
                                                        engine.encrypt(data)
                                                )
                                )
                        )
                )
                .accept();
    }

    @Override
    public ARFuture<io.aether.api.common.SignedKey>
    getAsymmetricPublicKey(CryptoLib cryptoLib) {
        var key = service.registrationKey(cryptoLib);
        return ARFuture.of(CryptoUtils.of(key.getPublicKeySigned()));
    }

    @Override
    public ARFuture<IpInfo> getMyIp() {
        IPAddress ipAddress = service.clientIpFromContext(ctx);

        Integer port = (Integer) ctx.getProperty(2);
        if (port == null) {
            port = 0;
        }
        int finalPort = port;

        return ARFuture.of(new IpInfo(
                ipAddress,
                (short) finalPort,
                0.0,
                0.0
        ));
    }
}