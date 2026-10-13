
package com.yashdotdev.distributed_traffic_control.config;

import com.yashdotdev.distributed_traffic_control.lease.GlobalCapacityKey;
import com.yashdotdev.distributed_traffic_control.lease.LeaseCoordinator;
import com.yashdotdev.distributed_traffic_control.policy.TrafficPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class GlobalCapacityBootstrap implements ApplicationRunner {

    private static final Logger logger =
            LoggerFactory.getLogger(GlobalCapacityBootstrap.class);

    private static final String DEFAULT_RESOURCE = "/api/orders";

    private final LeaseCoordinator leaseCoordinator;
    private final TrafficPolicy defaultTrafficPolicy;
    private final int maxAttempts;
    private final long retryDelayMs;

    public GlobalCapacityBootstrap(
            LeaseCoordinator leaseCoordinator,
            TrafficPolicy defaultTrafficPolicy,
            @Value("${allocation.bootstrap.max-attempts:5}") int maxAttempts,
            @Value("${allocation.bootstrap.retry-delay-ms:1000}") long retryDelayMs) {

        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "allocation.bootstrap.max-attempts must be at least 1");
        }
        if (retryDelayMs < 0) {
            throw new IllegalArgumentException(
                    "allocation.bootstrap.retry-delay-ms must not be negative");
        }

        this.leaseCoordinator = leaseCoordinator;
        this.defaultTrafficPolicy = defaultTrafficPolicy;
        this.maxAttempts = maxAttempts;
        this.retryDelayMs = retryDelayMs;
    }

    @Override
    public void run(ApplicationArguments args) {
        GlobalCapacityKey capacityKey = new GlobalCapacityKey(
                defaultTrafficPolicy.getPolicyId(), DEFAULT_RESOURCE);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                leaseCoordinator.registerCapacity(
                        capacityKey, defaultTrafficPolicy.getCapacity());

                logger.info(
                        "Registered global capacity for {} on attempt {}",
                        DEFAULT_RESOURCE, attempt);
                return;
            }
            catch (RuntimeException exception) {
                if (attempt == maxAttempts) {
                    throw new IllegalStateException(
                            "Failed to register global capacity after "
                                    + maxAttempts + " attempts",
                            exception);
                }

                logger.warn(
                        "Failed to register global capacity (attempt {}/{}). "
                                + "Retrying in {} ms",
                        attempt, maxAttempts, retryDelayMs, exception);

                try {
                    Thread.sleep(retryDelayMs);
                }
                catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                            "Interrupted while retrying global capacity registration",
                            interruptedException);
                }
            }
        }
    }
}
