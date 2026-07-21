package com.accessibilitymanager;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BottomBarFrostTest {
    @Test
    public void frostSpecProgressesContinuouslyFromOffToStrong() {
        BottomBarFrostSpec off = BottomBarFrostKt.bottomBarFrostSpec(0.0f);
        assertEquals(false, off.getEnabled());
        assertEquals(0.0f, off.getBlurRadiusDp(), 0.0f);
        assertEquals(1.0f, off.getTintAlpha(), 0.0f);
        assertEquals(0.0f, off.getNoiseFactor(), 0.0f);
        assertEquals(1.0f, off.getFallbackAlpha(), 0.0f);

        BottomBarFrostSpec minimal = BottomBarFrostKt.bottomBarFrostSpec(0.01f);
        assertEquals(true, minimal.getEnabled());
        assertEquals(0.52f, minimal.getBlurRadiusDp(), 0.001f);
        assertEquals(0.9942f, minimal.getTintAlpha(), 0.001f);
        assertEquals(0.001f, minimal.getNoiseFactor(), 0.001f);
        assertEquals(0.9968f, minimal.getFallbackAlpha(), 0.001f);

        BottomBarFrostSpec medium = BottomBarFrostKt.bottomBarFrostSpec(0.25f);
        assertEquals(true, medium.getEnabled());
        assertEquals(13.0f, medium.getBlurRadiusDp(), 0.001f);
        assertEquals(0.855f, medium.getTintAlpha(), 0.001f);
        assertEquals(0.025f, medium.getNoiseFactor(), 0.001f);
        assertEquals(0.92f, medium.getFallbackAlpha(), 0.001f);

        BottomBarFrostSpec strong = BottomBarFrostKt.bottomBarFrostSpec(1.0f);
        assertEquals(true, strong.getEnabled());
        assertEquals(52.0f, strong.getBlurRadiusDp(), 0.001f);
        assertEquals(0.42f, strong.getTintAlpha(), 0.001f);
        assertEquals(0.10f, strong.getNoiseFactor(), 0.001f);
        assertEquals(0.68f, strong.getFallbackAlpha(), 0.001f);
    }
}
