package io.aether.cloud.client;

import io.aether.api.CryptoUtils;
import io.aether.api.clientserverregapi.*;
import io.aether.api.common.*;
import io.aether.crypto.AKey;
import io.aether.crypto.CryptoEngine;
import io.aether.crypto.CryptoProviderFactory;
import io.aether.crypto.SignedKey;
import io.aether.logger.Log;
import io.aether.net.fastMeta.FastApiContext;
import io.aether.utils.WorkProofUtil;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;

import java.net.URI;
import java.util.Arrays;

public class ConnectionRegistration extends Connection<ClientApiRegUnsafe, RegistrationRootApiRemote> implements ClientApiRegUnsafe {
    private final AKey.Symmetric tempKey = CryptoProviderFactory.getProvider(client.getCryptLib().name()).createSymmetricKey();
    private final KeySymmetric tempKeyNative = CryptoUtils.of(tempKey);
    private final CryptoEngine tempKeyCp = tempKey.toCryptoEngine();
    private final FastApiContext ctxSafe = new FastApiContext() {
        @Override
        public void flush(AFuture sendFuture) {
            Log.debug("test");
        }
    };
    private final FastApiContext globalCtx = new FastApiContext();
    private CryptoEngine gcp;

    public ConnectionRegistration(AetherCloudClient client, URI uri) {
        super(client, uri, ClientApiRegUnsafe.META, RegistrationRootApi.META);
    }
    private ARFuture<CryptoEngine> getAsymmetricPublicKey(){
        ARFuture<CryptoEngine> result=ARFuture.make();
        getRootApiFuture()
                .to(api -> {
                    Log.debug("RegConn: TCP connection successful, requesting asymmetric key.", "uri", uri);
                    api.getAsymmetricPublicKey(client.getCryptLib()).to(k->{
                        var kk=CryptoUtils.of(k);
                        if (!client.verifySign(kk)) {
                            Log.error("RegConn: Key verification failed.", "signedKey", kk);
                            throw new IllegalStateException("Key verification exception");
                        }
                        result.done(kk.key().asAsymmetric().toCryptoEngine());
                    });
                    api.flush();
                })
                .onError(e -> {
                    Log.error("RegConn: Initial connection failed.", e, "uri", uri);
                    result.error(e);
                });
        return result;
    }
    public AFuture registration() {
        Log.debug("RegConn: Starting async registration process.", "uri", uri);
        getAsymmetricPublicKey().to(this::regProcess);
        return connectFuture.toFuture();
    }

    private void regProcess(CryptoEngine asymCE) {
        Log.info("RegConn: Asym public key was received.");
        getRootApiFuture().to(api->{

            if (api == null) {
                Log.error("RegConn: Root API is null after successful connection.");
                return;
            }

            api.enter(client.getCryptLib(), new ServerRegistrationApiStream(ctxSafe, asymCE::encrypt, apiInner -> {
                apiInner.requestWorkProofData(client.getParent(), PowMethod.AE_BCRYPT_CRC32, tempKeyNative)
                        .to(wpd -> {
                            Log.info("RegConn: WorkProofData has been received. Starting PoW calculation.");
                            var passwords = WorkProofUtil.generateProofOfWorkPool(
                                    wpd.getSalt(),
                                    wpd.getSuffix(),
                                    wpd.getMaxHashVal(),
                                    wpd.getPoolSize(),
                                    5300);
                            if (!client.verifySign(CryptoUtils.of(wpd.getGlobalKey()))) {
                                Log.error("RegConn: Global key verification failed.");
                                throw new RuntimeException();
                            }
                            gcp = CryptoEngine.of(CryptoUtils.of(wpd.getGlobalKey().getKey()).asAsymmetric().toCryptoEngine(), client.getMasterKey().toCryptoEngine());

                            api.enter(client.getCryptLib(), new ServerRegistrationApiStream(ctxSafe, asymCE::encrypt,
                                    a2 -> a2.registration(wpd.getSalt(), wpd.getSuffix(), passwords, client.getParent(), tempKeyNative,
                                            new GlobalApiStream(globalCtx, gcp::encrypt, gapi -> {
                                                gapi.setMasterKey(CryptoUtils.of(client.getMasterKey()));
                                                gapi.finish()
                                                        .to(d -> {
                                                            Log.trace("RegConn: registration step finish.");
                                                            client.confirmRegistration(d);
                                                            Log.info("RegConn: Registration confirmed.");
                                                            resolveCloud(d.getCloud(), asymCE).to(()->{
                                                                Log.info("RegConn: resolve cloud.");
                                                            });
                                                        }).addListener((f)->{
                                                            if(!f.isDone()){
                                                                Log.error("flush task canceled 1! $f","f",f);
                                                            }
                                                        });
                                            }))));
                            api.flush().addListener((f)->{
                                if(!f.isDone()){
                                    Log.error("flush task canceled 2! $f","f",f);
                                }
                            });
                        }, 6, () -> Log.warn("RegConn: timeout requestWorkProofData"));

            }));
            api.flush().addListener((f)->{
                if(!f.isDone()){
                    Log.error("flush task canceled 3! $f","f",f);
                }
            });
        });
    }

    public AFuture resolveCloud(Cloud cloud) {
        AFuture res=AFuture.make();
        getAsymmetricPublicKey().to(ce->{
            resolveCloud(cloud,ce).to(res);
        }).onError(res);
        return res;
    }
    private AFuture resolveCloud(Cloud cloud, CryptoEngine asymCE) {
        if(!client.isRecoveryInProgress.compareAndSet(false,true)){
            Log.debug("recovery procedure abort");
            return client.recoveryFuture;
        }
        AFuture result = client.recoveryFuture;
        Log.debug("Resolving cloud: " + cloud);

        rootApi.enter(client.getCryptLib(), new ServerRegistrationApiStream(ctxSafe, asymCE::encrypt, a3 -> {
            Log.trace("RegConn: registration step resolve servers: $servers", "servers", cloud);
            a3.resolveServers(cloud)
                    .to(ss -> {
                        Log.debug("Received server descriptors: " + Arrays.toString(ss));
                        for (var s : ss) {
                            Log.debug("Putting server descriptor: " + s);
                            client.putServerDescriptor(s);
                        }
                        result.done();
                        Log.info("RegConn: Server descriptors resolved.");
                    })
                    .onError(e -> {
                        Log.error("Failed to resolve servers", e);
                        result.error(e);
                    });
        }));
        rootApi.flush();
        return result;
    }
    @Override
    public void enterGlobal(GlobalRegClientApiStream stream) {
        stream.accept(globalCtx, gcp::decrypt, GlobalRegClientApi.EMPTY);
    }

    @Override
    public void enter(ClientApiRegSafeStream stream) {
        stream.accept(ctxSafe, tempKeyCp::decrypt, ClientApiRegSafe.EMPTY);
    }
}