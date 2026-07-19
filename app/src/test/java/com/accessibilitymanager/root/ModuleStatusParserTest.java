package com.accessibilitymanager.root;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ModuleStatusParserTest {
    @Test
    public void parsesStatusMarkerAmongOtherRootOutput() {
        ModuleStatusParser.Status status = ModuleStatusParser.parse(
                "root granted\n__A11Y_STATUS__:10001:10002:1:0\n"
        );

        assertEquals(10001, status.getInstalledVersionCode());
        assertEquals(10002, status.getPendingVersionCode());
        assertTrue(status.isMounted());
        assertFalse(status.isDisabled());
    }

    @Test
    public void rejectsMissingMarker() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ModuleStatusParser.parse("permission denied")
        );
    }
}
