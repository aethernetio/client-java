
package io.aether.cloud.proxy;

import io.aether.api.CryptoUtils;
import io.aether.api.clientserverapi.ClientApiRegUnsafeRemote;
import io.aether.api.clientserverapi.GlobalApiStream;
import io.aether.api.clientserverapi.GlobalRegClientApiStream;
import io.aether.api.clientserverapi.ServerRegistrationApi;

import io.aether.api.clientserverapi.RecoveryResult;

import io.aether.api.clientserverapi.RegistrationDirectResult;
import io.aether.api.common.Cloud;
import io.aether.api.common.CryptoLib;
import io.aether.api.common.IPAddress;
import io.aether.api.common.Key;
import io.aether.api.common.PowMethod;
import io.aether.api.common.ServerDescriptor;
import io.aether.api.common.SignedKey;
import io.aether.api.common.WorkProofDTO;
import io.aether.crypto.CryptoEngine;
import io.aether.crypto.AKey;
import io.aether.logger.Log;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.slots.AMFuture;

import java.util.UUID;

/**
 * Downstream-facing side of the registration flow: the proxy pretends to be
 * an ordinary Aether registration work server for one client.
 *
 * <p>PoW challenge is fetched from the original server via
 * {@link ProxyUpstreamRegistrationSession}; the same salt/suffix/passwords
 * are reused downstream. The only substitution is the global key: downstream
 * receives the PROXY's signed global key so that the next {@code GlobalApi}
 * stream is encrypted for the proxy, not the original server.
 */
public final class ProxyServerRegistrationApiImpl
        implements ServerRegistrationApi {

    final CryptoLib cryptoLib;
    final AMFuture<CryptoEngine> backCryptoEngine;

    private final ProxyService service;
    private final ClientApiRegUnsafeRemote remoteRootApi;
    private final IPAddress clientIp;

    private volatile ProxyUpstreamRegistrationSession upstream;
    private volatile WorkProofDTO originalWorkProof;
    private volatile WorkProofDTO proxyWorkProof;

    private UUID parent;
    private String cachedSalt;
    private String cachedSuffix;
    private int[] cachedPasswords;

    public ProxyServerRegistrationApiImpl(
            ClientApiRegUnsafeRemote remoteRootApi,
            ProxyService service,
            CryptoLib cryptoLib,
            AMFuture<CryptoEngine> backCryptoEngine,
            IPAddress clientIp
    ) {
        this.remoteRootApi = remoteRootApi;
        this.service = service;
        this.cryptoLib = cryptoLib;
        this.backCryptoEngine = backCryptoEngine;
        this.clientIp = clientIp;
    }

    @Override
    public void setReturnKey(Key key) {
        backCryptoEngine.set(
                CryptoUtils.of(key)
                        .asSymmetric()
                        .toCryptoEngine()
        );
    }


    @Override
    public ARFuture<WorkProofDTO> requestWorkProofData(
            UUID parent,
            PowMethod powMethods
    ) {
        if (this.parent != null && !this.parent.equals(parent)) {
            return ARFuture.ofThrow(new IllegalStateException(
                    "Registration parent changed from "
                            + this.parent + " to " + parent
            ));
        }
        this.parent = parent;


        ARFuture<WorkProofDTO> result = ARFuture.make();
        try {

            ProxyUpstreamRegistrationSession session =
                    new ProxyUpstreamRegistrationSession(
                            service.upstreamRegistrationUris().get(0),
                            cryptoLib,
                            parent,
                            service.rootSigners()
                    );

            session.connect();
            this.upstream = session;

            session.requestWorkProofData().to(
                    wpd -> {
                        originalWorkProof = wpd;
                        SignedKey proxyGlobalKey = CryptoUtils.of(
                                service.registrationKey(cryptoLib)
                                        .getPublicKeySigned()
                        );
                        proxyWorkProof = new WorkProofDTO(
                                wpd.getSalt(),
                                wpd.getSuffix(),
                                wpd.getPoolSize(),
                                wpd.getMaxHashVal(),
                                proxyGlobalKey
                        );
                        result.done(proxyWorkProof);
                    },
                    error -> result.error(error)
            );
        } catch (RuntimeException error) {
            result.error(error);
        }
        return result;
    }

    @Override
    public ARFuture<ServerDescriptor[]> resolveServers(Cloud serverIds) {
        short[] data = serverIds == null
                ? new short[0]
                : serverIds.getData();
        for (short sid : data) {
            if (sid == service.proxySid()) {
                return ARFuture.of(new ServerDescriptor[]{
                        service.proxyDescriptor()
                });
            }
        }
        if (upstream == null) {
            return ARFuture.of(new ServerDescriptor[0]);
        }
        return upstream.resolveServers(serverIds);
    }

    @Override
    public void registration(
            String salt,
            String suffix,
            int[] passwords,
            UUID parent,
            GlobalApiStream globalApi
    ) {
        if (originalWorkProof == null) {
            Log.error("ProxyServerRegistrationApiImpl: registration before WPD");
            return;
        }
        this.cachedSalt = salt;
        this.cachedSuffix = suffix;
        this.cachedPasswords = passwords;
        this.parent = parent;



        if (upstream == null) {
            throw new IllegalStateException(
                    "Upstream registration session is missing"
            );
        }

        service.globalClientApi(
                parent,
                cryptoLib,
                globalApi.asIn(),
                clientIp,
                upstream,
                salt,
                suffix,
                passwords
        ).to(
                data ->
                        remoteRootApi.enterGlobal(
                                GlobalRegClientApiStream
                                        .Out
                                        .send(data)
                        ),
                error ->
                        Log.error(
                                "Proxy global registration failed",
                                error,
                                "parent",
                                parent
                        )
        );


    }

    @Override
    public ARFuture<RecoveryResult> recovery(
            UUID uid,
            Key oldMasterKey,
            Key newMasterKey
    ) {
        ARFuture<RecoveryResult> result = ARFuture.make();

        ProxyUpstreamRegistrationSession session = null;
        try {
            session = new ProxyUpstreamRegistrationSession(
                    service.upstreamRegistrationUris().get(0),
                    cryptoLib,
                    null,
                    service.rootSigners()
            );
            session.connect();

            ProxyUpstreamRegistrationSession recoverySession = session;
            session.recovery(
                    uid,
                    oldMasterKey,
                    newMasterKey
            ).to(
                    recovered -> {
                        recoverySession.close();
                        result.done(recovered);
                    },
                    error -> {
                        recoverySession.close();
                        result.error(error);
                    }
            );
        } catch (RuntimeException error) {
            if (session != null) {
                session.close();
            }
            result.error(error);
        }

        return result;
    }



            @Override
            public ARFuture<RegistrationDirectResult> registrationDirect(
                    String salt,
                    String suffix,
                    int[] passwords,
                    UUID parent,
                    Key masterKey
            ) {
                if (originalWorkProof == null) {
                    return ARFuture.ofThrow(
                            new IllegalStateException(
                                    "registrationDirect before WorkProofData"
                            )
                    );
                }

                if (
                        this.parent != null
                                && !this.parent.equals(parent)
                ) {
                    return ARFuture.ofThrow(
                            new IllegalStateException(
                                    "Registration parent changed from "
                                            + this.parent
                                            + " to "
                                            + parent
                            )
                    );
                }

                ProxyUpstreamRegistrationSession session = upstream;
                if (session == null) {
                    return ARFuture.ofThrow(
                            new IllegalStateException(
                                    "Upstream registration session is missing"
                            )
                    );
                }

                this.parent = parent;

                ProxyGlobalRegServerApiImpl registration =
                        new ProxyGlobalRegServerApiImpl(
                                service,
                                session,
                                parent,
                                cryptoLib,
                                salt,
                                suffix,
                                passwords
                        );

                registration.setMasterKey(masterKey);

                return registration.finish().map(result ->
                        new RegistrationDirectResult(
                                result.getAlias(),
                                result.getUid(),
                                result.getCloud()
                        )
                );
            }
}