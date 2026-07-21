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

    @Test
    public void frostEffectHasDistinctOffMediumAndStrongStates() {
        BottomBarFrostEffect off = ManagerPreferencesStoreKt.bottomBarFrostEffect(0.0f);
        assertEquals(false, off.getEnabled());
        assertEquals(0.0f, off.getBlurRadiusDp(), 0.0f);
        assertEquals(1.0f, off.getTintAlpha(), 0.0f);
        assertEquals(true, ManagerPreferencesStoreKt.bottomBarFrostEffect(0.01f).getEnabled());

        BottomBarFrostEffect medium = ManagerPreferencesStoreKt.bottomBarFrostEffect(0.25f);
        assertEquals(true, medium.getEnabled());
        assertEquals(22.0f, medium.getBlurRadiusDp(), 0.001f);
        assertEquals(0.795f, medium.getTintAlpha(), 0.001f);

        BottomBarFrostEffect strong = ManagerPreferencesStoreKt.bottomBarFrostEffect(1.0f);
        assertEquals(true, strong.getEnabled());
        assertEquals(64.0f, strong.getBlurRadiusDp(), 0.001f);
        assertEquals(0.30f, strong.getTintAlpha(), 0.001f);
        assertEquals(0.18f, strong.getNoiseFactor(), 0.001f);
        assertEquals(0.65f, strong.getFallbackAlpha(), 0.001f);
    }
}
