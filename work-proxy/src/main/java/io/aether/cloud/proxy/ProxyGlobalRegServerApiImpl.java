
package io.aether.cloud.proxy;

import io.aether.api.CryptoUtils;
import io.aether.api.clientserverapi.FinishResult;
import io.aether.api.clientserverapi.GlobalRegServerApi;
import io.aether.api.common.Cloud;
import io.aether.api.common.CryptoLib;
import io.aether.api.common.Key;
import io.aether.api.common.ServerDescriptor;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientCloud;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.crypto.AKey;
import io.aether.crypto.CryptoEngine;
import io.aether.logger.Log;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.slots.AMFuture;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Downstream global registration facade. All operations are non-blocking;
 * nothing uses {@code synchronized} or {@code .get()}.
 */
public final class ProxyGlobalRegServerApiImpl
        implements GlobalRegServerApi {

    private final ProxyService service;
    private final ProxyUpstreamRegistrationSession upstreamSession;
    private final UUID parent;
    private final CryptoLib cryptoLib;
    private final String salt;
    private final String suffix;
    private final int[] passwords;

    private final AtomicReference<AKey.Symmetric> masterKey =
            new AtomicReference<>();
    private final AtomicReference<ARFuture<FinishResult>> finishFuture =
            new AtomicReference<>();

    private final AMFuture<CryptoEngine> responseEncryptEngine =
            new AMFuture<>();

    public ProxyGlobalRegServerApiImpl(
            ProxyService service,
            ProxyUpstreamRegistrationSession upstreamSession,
            UUID parent,
            CryptoLib cryptoLib,
            String salt,
            String suffix,
            int[] passwords
    ) {
        this.service = service;
        this.upstreamSession = upstreamSession;
        this.parent = parent;
        this.cryptoLib = cryptoLib;
        this.salt = salt;
        this.suffix = suffix;
        this.passwords = passwords;
    }

    public AMFuture<CryptoEngine> responseEncryptEngine() {
        return responseEncryptEngine;
    }

    public AKey.Symmetric masterKeyNow() {
        return masterKey.get();
    }

    @Override
    public void setMasterKey(Key key) {
        if (finishFuture.get() != null) {
            throw new IllegalStateException(
                    "Registration is already finishing"
            );
        }
        AKey.Symmetric mk = CryptoUtils.of(key);
        masterKey.set(mk);
        responseEncryptEngine.set(
                mk.asSymmetric().toCryptoEngine()
        );
    }

    @Override
    public ARFuture<FinishResult> finish() {
        ARFuture<FinishResult> existing = finishFuture.get();
        if (existing != null) {
            return existing;
        }
        AKey.Symmetric mk = masterKey.get();
        if (mk == null) {
            return ARFuture.ofThrow(new IllegalStateException(
                    "Master key is not set"
            ));
        }

        ARFuture<FinishResult> result = ARFuture.make();
        if (!finishFuture.compareAndSet(null, result)) {
            return finishFuture.get();
        }

        upstreamSession.finish(mk, salt, suffix, passwords)
                .mapRFuture(realResult ->
                        upstreamSession
                                .resolveServers(realResult.getCloud())
                                .map(descriptors ->
                                        new RegDone(realResult, descriptors))
                )
                .to(
                        done -> completeRegistration(done, mk, result),
                        error -> result.error(error)
                );

        return result;
    }

    private void completeRegistration(
            RegDone done,
            AKey.Symmetric mk,
            ARFuture<FinishResult> result
    ) {
        try {
            ClientStateInMemory state = buildUpstreamState(
                    done.realResult,
                    done.descriptors,
                    mk
            );
            service.registerNewClient(state);

            FinishResult downstream = new FinishResult(
                    done.realResult.getAlias(),
                    done.realResult.getUid(),
                    new Cloud(new short[]{service.proxySid()})
            );
            result.done(downstream);
        } catch (Throwable error) {
            Log.error(
                    "ProxyGlobalRegServerApiImpl: post-registration failure",
                    error
            );
            result.error(error);
        }
    }

    private ClientStateInMemory buildUpstreamState(
            FinishResult realResult,
            ServerDescriptor[] descriptors,
            AKey.Symmetric mk
    ) {
        ClientStateInMemory state = new ClientStateInMemory(
                parent,
                service.upstreamRegistrationUris(),
                null,
                cryptoLib
        );
        state.setMasterKey(CryptoUtils.of(mk));
        state.setUid(realResult.getUid());
        state.setAlias(realResult.getAlias());
        state.saveCloud(new ClientCloud(
                realResult.getUid(),
                realResult.getCloud()
        ));
        for (ServerDescriptor descriptor : descriptors) {
            state.getServerInfo(descriptor.getId())
                    .setDescriptor(descriptor);
        }
        return state;
    }

    private record RegDone(
            FinishResult realResult,
            ServerDescriptor[] descriptors
    ) {
    }
}