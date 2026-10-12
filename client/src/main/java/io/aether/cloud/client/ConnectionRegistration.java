package io.aether.cloud.client;

import io.aether.api.CryptoUtils;
import io.aether.api.clientserverapi.*;

import io.aether.api.common.Cloud;
import io.aether.api.common.KeySymmetric;
import io.aether.api.common.PowMethod;
import io.aether.api.common.ServerDescriptor;
import io.aether.api.common.WorkProofDTO;

import io.aether.crypto.AKey;
import io.aether.crypto.CryptoEngine;
import io.aether.crypto.CryptoProviderFactory;
import io.aether.logger.Log;
import io.aether.utils.WorkProofUtil;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;

import java.net.URI;
import java.util.Arrays;

public class ConnectionRegistration extends Connection<ClientApiUnsafe, RegistrationRootApiRemote> implements ClientApiUnsafe {
    final AFuture connectFuture = AFuture.make();
    private final AKey.Symmetric tempKey = CryptoProviderFactory.getProvider(client.getCryptLib().name()).createSymmetricKey();
    private final KeySymmetric tempKeyNative = CryptoUtils.of(tempKey);
    private final CryptoEngine tempKeyCp = tempKey.toCryptoEngine();


    private final io.aether.utils.slots.AMFuture<io.aether.api.common.SignedKey> asymmetricPublicKeyMFuture
            = new io.aether.utils.slots.AMFuture<>();
    private final io.aether.utils.slots.AMFuture<ServerDescriptor[]> resolvedServersMFuture
            = new io.aether.utils.slots.AMFuture<>();


    private final io.aether.utils.slots.AMFuture<WorkProofDTO> workProofMFuture
            = new io.aether.utils.slots.AMFuture<>();
    private final io.aether.utils.slots.AMFuture<RegistrationDirectResult> registrationDirectMFuture
            = new io.aether.utils.slots.AMFuture<>();


    volatile ServerRegistrationApiRemote safeApi;


    public ConnectionRegistration(AetherCloudClient client, URI uri) {
        super(client, uri, ClientApiUnsafe.META, RegistrationRootApi.META);
    }

    private ARFuture<CryptoEngine> getAsymmetricPublicKey() {
        Log.debug("RegConn: TCP connection successful, requesting asymmetric key.", "uri", uri);
        rootApi.getAsymmetricPublicKey(client.getCryptLib());
        return asymmetricPublicKeyMFuture.mapToARFuture().map(k -> {
            var kk = CryptoUtils.of(k);
            if (!client.verifySign(kk)) {
                Log.error("RegConn: Key verification failed.", "signedKey", kk);
                throw new IllegalStateException("Key verification exception");
            }
            return kk.key().asAsymmetric().toCryptoEngine();
        });
    }

    public AFuture registration() {
        Log.debug("RegConn: Starting async registration process.", "uri", uri);
        getAsymmetricPublicKey().to(this::regProcess);
        return connectFuture;
    }

    private void regProcess(CryptoEngine asymCE) {
        safeApi = rootApi.openEnter(
                client.getCryptLib(),
                r -> ClientApiRegSafe.EMPTY,
                asymCE::encrypt,
                "reg safe"
        );

        Log.info("RegConn: Asym public key was received.");

        safeApi.setReturnKey(tempKeyNative);
        safeApi.requestWorkProofData(client.getParent(), PowMethod.AE_BCRYPT_CRC32);

        workProofMFuture.mapToARFuture().to(wpd -> {
            Log.info("RegConn: WorkProofData received. Starting PoW calculation.");

            var passwords = WorkProofUtil.generateProofOfWorkPool(
                    wpd.getSalt(), wpd.getSuffix(),
                    wpd.getMaxHashVal(), wpd.getPoolSize(), 5300);

            safeApi.registrationDirect(
                    wpd.getSalt(), wpd.getSuffix(), passwords,
                    client.getParent(), CryptoUtils.of(client.getMasterKey()));

            registrationDirectMFuture.mapToARFuture().to(d -> {
                Log.trace("RegConn: registration step finish.");
                Log.info("RegConn: Registration confirmed.");
                resolveCloud(d.getCloud(), asymCE).to(() -> {
                    Log.info("RegConn: resolve cloud.");
                    client.confirmRegistration(d);
                });
            }, error -> Log.error("RegConn: registrationDirect failed", error));

            connectFuture.done();
        }, error -> Log.error("RegConn: requestWorkProofData failed", error));
    }





    public AFuture resolveCloud(Cloud cloud) {
        AFuture res = AFuture.make();
        getAsymmetricPublicKey().to(ce -> {
            resolveCloud(cloud, ce).to(res);
        }).onError(res);
        return res;
    }

    private AFuture resolveCloud(Cloud cloud, CryptoEngine asymCE) {
        if (!client.isRecoveryInProgress.compareAndSet(false, true)) {
            Log.debug("recovery procedure abort");
            return client.recoveryFuture;
        }
        AFuture result = client.recoveryFuture;
        Log.debug("Resolving cloud: " + cloud);
        Log.trace("RegConn: registration step resolve servers: $servers", "servers", cloud);

        safeApi.resolveServers(cloud);
        resolvedServersMFuture.mapToARFuture().to(
                ss -> {
                    Log.debug("Received server descriptors: " + Arrays.toString(ss));
                    for (var s : ss) {
                        Log.debug("Putting server descriptor: " + s);
                        client.putServerDescriptor(s);
                    }
                    result.tryDone();
                    Log.info("RegConn: Server descriptors resolved.");
                },
                e -> {
                    Log.error("Failed to resolve servers", e);
                    result.tryError(e);
                });

        return result;
    }



    @Override
    public void enterRegistration(ClientApiRegSafeStream stream) {
        stream.asIn()
                .ctx(safeApi.getFastMetaContext())
                .convert(tempKeyCp::decrypt)
                .accept();
    }


    @Override
    public void sendSafeApiDataMulti(short backId, LoginClientStream data) {
        throw new UnsupportedOperationException("ConnectionRegistration: no multi-backId support");
    }

    @Override
    public void sendSafeApiData(LoginClientStream data) {
        throw new UnsupportedOperationException("ConnectionRegistration: not logged in");
    }

    @Override
    public void sendCurrentTimeUtc(long time) {
        Log.warn("ConnectionRegistration: unexpected sendCurrentTimeUtc");
    }

    @Override
    public void sendMyIpInfo(io.aether.api.common.IpInfo info) {
        Log.warn("ConnectionRegistration: unexpected sendMyIpInfo");
    }

    @Override
    public void sendAsymmetricPublicKey(io.aether.api.common.SignedKey key) {
        asymmetricPublicKeyMFuture.set(key);
    }

    @Override
    public void sendWorkProofData(java.util.UUID parent, io.aether.api.common.WorkProofDTO dto) {
        workProofMFuture.set(dto);
    }

    @Override
    public void sendResolvedServers(ServerDescriptor[] servers) {
        resolvedServersMFuture.set(servers);
    }

    @Override
    public void sendRegistrationDirect(RegistrationDirectResult result) {
        registrationDirectMFuture.set(result);
    }

    @Override
    public void sendRecoveredClientState(RecoveryResult result) {
        Log.warn("ConnectionRegistration: unexpected sendRecoveredClientState");
    }

}