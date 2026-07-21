package com.accessibilitymanager;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ManagerPreferencesTest {
    @Test
    public void themeModeFallsBackToSystemForUnknownValue() {
        assertEquals(ManagerThemeMode.SYSTEM, ManagerThemeMode.Companion.fromPreference("UNKNOWN"));
        assertEquals(ManagerThemeMode.DARK, ManagerThemeMode.Companion.fromPreference("DARK"));
    }

    @Test
    public void frostValueIsClampedAndNonFiniteFallsBackToDefault() {
        assertEquals(0.0f, ManagerPreferencesStoreKt.sanitizeBottomBarFrost(-1.0f), 0.0f);
        assertEquals(1.0f, ManagerPreferencesStoreKt.sanitizeBottomBarFrost(2.0f), 0.0f);
        assertEquals(
                ManagerPreferencesStoreKt.DEFAULT_BOTTOM_BAR_FROST,
                ManagerPreferencesStoreKt.sanitizeBottomBarFrost(Float.NaN),
                0.0f
        );
    }
}
