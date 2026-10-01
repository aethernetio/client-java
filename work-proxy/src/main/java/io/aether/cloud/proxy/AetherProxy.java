
package io.aether.cloud.proxy;


import io.aether.utils.Destroyer;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;
import io.aether.utils.interfaces.Destroyable;

import java.util.concurrent.TimeUnit;


/**
 * Combined proxy process: single registration listener plus single work
 * listener, both sharing one {@link ProxyService}.
 */
public final class AetherProxy implements Destroyable {


    private final ProxyService service;
    private final AetherRegistrationProxy registration;
    private final AetherWorkProxy work;
    private final Destroyer destroyer = new Destroyer("AetherProxy");


    public AetherProxy(AetherProxyConfig config) {
        this.service = config.createService();
        this.registration = new AetherRegistrationProxy(
                config.registrationListen(),
                service
        );
        this.work = new AetherWorkProxy(
                config.workListen(),
                service
        );
    }




    public AetherProxy start() {
        boolean registrationStarted = false;
        boolean workStarted = false;

        try {
            service.restorePersistentClients();

            registration.start();
            registrationStarted = true;

            work.start();
            workStarted = true;

            return this;
        } catch (RuntimeException error) {
            if (workStarted) {
                work.destroy(true);
            }
            if (registrationStarted) {
                registration.destroy(true);
            }
            service.destroy(true);
            throw error;
        }
    }




    public ProxyService service() {
        return service;
    }

    @Override


    public AFuture destroy(boolean force) {
        destroyer.destroy(force);
        return AFuture.all(
                registration.destroy(force),
                work.destroy(force),
                service.destroy(force)
        );

    }

}