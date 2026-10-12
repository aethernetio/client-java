
package io.aether.cloud.proxy;

import io.aether.api.clientserverapi.ClientApiUnsafe;
import io.aether.api.clientserverapi.ClientApiUnsafeRemote;
import io.aether.api.clientserverapi.ClientApiSafeRemote;
import io.aether.api.clientserverapi.LoginApi;
import io.aether.api.clientserverapi.LoginClientStream;
import io.aether.api.clientserverapi.LoginStream;
import io.aether.api.common.IpInfo;
import io.aether.crypto.CryptoEngine;
import io.aether.crypto.DecryptException;
import io.aether.logger.Log;
import io.aether.net.fastMeta.MetaContext;
import io.aether.net.fastMeta.SecurityConnectionDropException;
import io.aether.utils.RU;
import io.aether.utils.futures.ARFuture;

import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Downstream work/login endpoint. Modeled on
 * {@code io.aether.cloud.server.api.LoginApiImpl} but backed by
 * {@link ProxyService} and {@link ProxyClientDescriptor} instead of
 * {@code LService}/{@code ClientDescriptorWork}.
 */
public final class ProxyLoginApiImpl implements LoginApi {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private final ProxyService service;
    private final MetaContext ctx;
    private final ClientApiUnsafeRemote remoteRootApi;

    private final LoginStreamSession loginStreamSession =
            new LoginStreamSession();

    public ProxyLoginApiImpl(MetaContext ctx, ProxyService service) {
        this.ctx = ctx;
        this.remoteRootApi = ctx.makeRemote(ClientApiUnsafeRemote.META);
        this.service = service;
    }

    private ProxyAuthorizedApiImpl createAuthorizedApi(
            ClientApiSafeRemote safeRemoteApi,
            ProxyClientDescriptor client,
            LoginStreamSession session,
            LoginStreamTransport transport
    ) {
        MetaContext context = safeRemoteApi.getFastMetaContext();

        context.setActivePredicate(client::isReceiveWindowOpen);

        context.setLocalMethodBeforeListener(methodName -> {
            if ("ping".equals(methodName)
                    || "openReceiveWindow".equals(methodName)
                    || "setReceiveWindow".equals(methodName)
                    || "setNextReadDelay".equals(methodName)) {
                return;
            }
            client.recordActivity(RU.time());
        });

        client.setSluice(safeRemoteApi);


        if (session.version == 0) {
            return new ProxyAuthorizedApiV0Impl(
                    service,
                    client,
                    safeRemoteApi,
                    session,
                    transport
            );
        }

        throw new SecurityConnectionDropException(
                "Unsupported login stream API version: "
                        + session.version
        );
    }


    private LoginStreamTransport createLoginStreamTransport(
            ProxyClientDescriptor client,
            CryptoEngine engine,
            short backId
    ) {
        return new LoginStreamTransport() {
            @Override
            public void send(byte[] data) {
                byte[] encrypted = engine.encrypt(data);
                if (backId < 0) {
                    remoteRootApi.sendSafeApiData(
                            LoginClientStream.Out.send(encrypted)
                    );
                } else {
                    remoteRootApi.sendSafeApiDataMulti(
                            backId,
                            LoginClientStream.Out.send(encrypted)
                    );
                }
            }

            @Override
            public void sendMulti(short routeId, byte[] data) {
                byte[] encrypted = engine.encrypt(data);
                remoteRootApi.sendSafeApiDataMulti(
                        routeId,
                        LoginClientStream.Out.send(encrypted)
                );
            }
        };
    }



    private void acceptLoginStream(
            ProxyClientDescriptor client,
            LoginStream data,
            CryptoEngine engine,
            short backId
    ) {
        Objects.requireNonNull(engine);
        try {
            LoginStreamTransport transport =
                    createLoginStreamTransport(client, engine, backId);

            LoginStream.In in = data
                    .asIn()
                    .onFlush((context, bytes) -> {
                        Object localApi = context.getLocalApi();
                        if (!(localApi instanceof LoginStreamRouting routing)) {
                            throw new SecurityConnectionDropException(
                                    "Login stream local API has no routing policy"
                            );
                        }
                        routing.flushLoginStream(bytes);
                    })
                    .convert(engine::decrypt);

            if (loginStreamSession.version == 0) {
                LoginStream.V0.in(
                        in,
                        safeRemoteApi -> createAuthorizedApi(
                                safeRemoteApi,
                                client,
                                loginStreamSession,
                                transport
                        ),
                        "proxy client work",
                        client.uid()
                ).accept();
            } else {
                throw new SecurityConnectionDropException(
                        "Unsupported login stream API version: "
                                + loginStreamSession.version
                );
            }
        } catch (DecryptException error) {
            throw new SecurityConnectionDropException(
                    "login decrypt exception"
            );
        }
    }


    @Override
    public void getTimeUTC() {
        remoteRootApi.sendCurrentTimeUtc(System.currentTimeMillis());
    }


    private void login(ProxyClientDescriptor client, LoginStream data, short backId) {
        Objects.requireNonNull(client.symmetricEngine());
        acceptLoginStream(client, data, client.symmetricEngine(), backId);
    }


    @Override
    public void getMyIp() {
        String ip = (String) ctx.getProperty(1);
        Integer port = (Integer) ctx.getProperty(2);
        if (ip == null) ip = "0.0.0.0";
        if (port == null) port = 0;
        var ipAddr = io.aether.api.common.IPAddressUtil.of(
                java.net.URI.create("tcp://" + ip))[0];
        remoteRootApi.sendMyIpInfo(new IpInfo(ipAddr, port.shortValue(), 0.0, 0.0));
    }

    @Override
    public void loginByUID(UUID uid, LoginStream data) {
        ProxyClientDescriptor client = service.clients().getByUid(uid);
        if (client == null) {
            Log.warn(
                    "ProxyLoginApiImpl: loginByUID unknown uid",
                    "uid",
                    uid
            );
            ctx.close();
            return;
        }
        login(client, data, (short) -1);
    }

    @Override
    public void loginByAlias(UUID alias, LoginStream data) {
        ProxyClientDescriptor client =
                service.clients().getByAlias(alias);
        if (client == null) {
            Log.warn(
                    "ProxyLoginApiImpl: loginByAlias unknown alias",
                    "alias",
                    alias
            );
            ctx.close();
            return;
        }
        login(client, data, (short) -1);
    }


    @Override
    public void loginByUIDMulti(UUID uid, short backId, LoginStream data) {
        ProxyClientDescriptor client = service.clients().getByUid(uid);
        if (client == null) {
            Log.warn(
                    "ProxyLoginApiImpl: loginByUIDMulti unknown uid",
                    "uid",
                    uid
            );
            ctx.close();
            return;
        }
        login(client, data, backId);
    }

    @Override
    public void loginByAliasMulti(UUID alias, short backId, LoginStream data) {
        ProxyClientDescriptor client =
                service.clients().getByAlias(alias);
        if (client == null) {
            Log.warn(
                    "ProxyLoginApiImpl: loginByAliasMulti unknown alias",
                    "alias",
                    alias
            );
            ctx.close();
            return;
        }
        login(client, data, backId);
    }


    interface LoginStreamTransport {
        void send(byte[] data);

        void sendMulti(short routeId, byte[] data);
    }

    interface LoginStreamRouting {
        void flushLoginStream(byte[] data);
    }

    static final class LoginStreamSession {
        volatile int version;
    }
}