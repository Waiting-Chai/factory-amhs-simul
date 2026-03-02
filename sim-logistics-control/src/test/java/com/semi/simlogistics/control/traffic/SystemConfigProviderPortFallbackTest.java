package com.semi.simlogistics.control.traffic;

import com.semi.simlogistics.control.port.config.SystemConfigPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies SystemConfigProvider DB-port priority and local fallback behavior.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
class SystemConfigProviderPortFallbackTest {

    private final SystemConfigProvider provider = SystemConfigProvider.getInstance();

    @AfterEach
    void tearDown() {
        provider.clearPrimaryPort();
        provider.resetToDefaults();
    }

    @Test
    void testDbPortPriority_UsesPortValueWhenPresent() {
        provider.setConfig("traffic.replan.timeoutSeconds", "30");
        provider.setPrimaryPort(new StubSystemConfigPort(Map.of("traffic.replan.timeoutSeconds", "45")));

        assertThat(provider.getReplanTimeoutSeconds()).isEqualTo(45.0);
    }

    @Test
    void testFallback_UsesInMemoryWhenPortHasNoValue() {
        provider.setConfig("traffic.replan.maxAttempts", "7");
        provider.setPrimaryPort(new StubSystemConfigPort(Map.of()));

        assertThat(provider.getReplanMaxAttempts()).isEqualTo(7);
    }

    private record StubSystemConfigPort(Map<String, String> store) implements SystemConfigPort {
        @Override
        public Optional<String> findConfigValue(String tenantId, String key) {
            return Optional.ofNullable(store.get(key));
        }
    }
}
