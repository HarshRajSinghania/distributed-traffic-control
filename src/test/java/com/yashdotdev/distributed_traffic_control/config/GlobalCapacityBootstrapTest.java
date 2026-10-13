package com.yashdotdev.distributed_traffic_control.config;

import com.yashdotdev.distributed_traffic_control.lease.LeaseCoordinator;
import com.yashdotdev.distributed_traffic_control.policy.TrafficPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class GlobalCapacityBootstrapTest {

    @Test
    void shouldRegisterCapacityOnFirstAttempt() {
        LeaseCoordinator coordinator = mock(LeaseCoordinator.class);
        TrafficPolicy policy = mock(TrafficPolicy.class);

        when(policy.getPolicyId()).thenReturn("default-policy");
        when(policy.getCapacity()).thenReturn(100L);

        GlobalCapacityBootstrap bootstrap =
                new GlobalCapacityBootstrap(coordinator, policy, 3, 0);

        assertDoesNotThrow(() -> bootstrap.run(null));

        verify(coordinator, times(1)).registerCapacity(any(), eq(100L));
    }

    @Test
    void shouldRetryWhenRegistrationInitiallyFails() {
        LeaseCoordinator coordinator = mock(LeaseCoordinator.class);
        TrafficPolicy policy = mock(TrafficPolicy.class);

        when(policy.getPolicyId()).thenReturn("default-policy");
        when(policy.getCapacity()).thenReturn(100L);

        doThrow(new IllegalStateException("Redis unavailable"))
                .doNothing()
                .when(coordinator)
                .registerCapacity(any(), eq(100L));

        GlobalCapacityBootstrap bootstrap =
                new GlobalCapacityBootstrap(coordinator, policy, 3, 0);

        assertDoesNotThrow(() -> bootstrap.run(null));

        verify(coordinator, times(2)).registerCapacity(any(), eq(100L));
    }

    @Test
    void shouldFailAfterExhaustingRetries() {
        LeaseCoordinator coordinator = mock(LeaseCoordinator.class);
        TrafficPolicy policy = mock(TrafficPolicy.class);

        when(policy.getPolicyId()).thenReturn("default-policy");
        when(policy.getCapacity()).thenReturn(100L);

        doThrow(new IllegalStateException("Redis unavailable"))
                .when(coordinator)
                .registerCapacity(any(), eq(100L));

        GlobalCapacityBootstrap bootstrap =
                new GlobalCapacityBootstrap(coordinator, policy, 3, 0);

        assertThrows(
                IllegalStateException.class,
                () -> bootstrap.run(null));

        verify(coordinator, times(3)).registerCapacity(any(), eq(100L));
    }

    @Test
    void shouldRejectInvalidRetryConfiguration() {
        LeaseCoordinator coordinator = mock(LeaseCoordinator.class);
        TrafficPolicy policy = mock(TrafficPolicy.class);

        assertThrows(
                IllegalArgumentException.class,
                () -> new GlobalCapacityBootstrap(coordinator, policy, 0, 0));

        assertThrows(
                IllegalArgumentException.class,
                () -> new GlobalCapacityBootstrap(coordinator, policy, 3, -1));
    }
}
