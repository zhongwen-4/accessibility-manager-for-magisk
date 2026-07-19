package com.accessibilitymanager.root;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class ShellCommandBuilderTest {
    private static final String COMPONENT = "com.example.app/.ExampleService";
    private static final String PREFIX = "command -v a11yctl >/dev/null 2>&1 || "
            + "{ echo __A11YCTL_MISSING__; exit 127; }; ";

    @Test
    public void enableOnlyChangesCurrentServiceState() {
        assertEquals(
                PREFIX + "a11yctl enable " + COMPONENT,
                ShellCommandBuilder.forService(COMPONENT, true)
        );
    }

    @Test
    public void disableOnlyChangesCurrentServiceState() {
        assertEquals(
                PREFIX + "a11yctl disable " + COMPONENT,
                ShellCommandBuilder.forService(COMPONENT, false)
        );
    }

    @Test
    public void lockPersistsAndImmediatelyEnablesService() {
        assertEquals(
                PREFIX + "a11yctl add " + COMPONENT
                        + " >/dev/null && a11yctl enable " + COMPONENT,
                ShellCommandBuilder.forLock(COMPONENT, true)
        );
    }

    @Test
    public void unlockOnlyRemovesPersistentConfiguration() {
        assertEquals(
                PREFIX + "a11yctl remove " + COMPONENT,
                ShellCommandBuilder.forLock(COMPONENT, false)
        );
    }

    @Test
    public void configuredServicesCommandIsReadOnly() {
        assertEquals(PREFIX + "a11yctl configured", ShellCommandBuilder.forLockedServices());
    }

    @Test
    public void rejectsShellMetacharacters() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ShellCommandBuilder.forService("com.example/.Service;reboot", true)
        );
    }
}
