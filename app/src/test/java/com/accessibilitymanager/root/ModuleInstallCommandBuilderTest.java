package com.accessibilitymanager.root;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ModuleInstallCommandBuilderTest {
    private static final String ZIP_PATH = "/data/user/0/com.accessibilitymanager/cache/module.zip";

    @Test
    public void prefersSukiSuBeforeMagisk() {
        String command = ModuleInstallCommandBuilder.build(ZIP_PATH);

        int sukiSuInstall = command.indexOf("module install '" + ZIP_PATH + "'");
        int magiskInstall = command.indexOf("--install-module '" + ZIP_PATH + "'");
        assertTrue(sukiSuInstall >= 0);
        assertTrue(magiskInstall > sukiSuInstall);
    }

    @Test
    public void checksStableSukiSuDaemonLocations() {
        String command = ModuleInstallCommandBuilder.build(ZIP_PATH);

        assertTrue(command.contains("command -v ksud"));
        assertTrue(command.contains("/data/adb/ksud"));
        assertTrue(command.contains("/data/adb/ksu/bin/ksud"));
    }

    @Test
    public void reportsWhenNeitherSupportedManagerExists() {
        String command = ModuleInstallCommandBuilder.build(ZIP_PATH);

        assertTrue(command.contains(ModuleInstallCommandBuilder.ROOT_MANAGER_MISSING_MARKER));
    }

    @Test
    public void shellQuotesModulePath() {
        String command = ModuleInstallCommandBuilder.build("/data/local/a'b/module.zip");

        assertTrue(command.contains("'/data/local/a'\\''b/module.zip'"));
    }

    @Test
    public void rejectsEmptyPath() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ModuleInstallCommandBuilder.build("")
        );
    }
}
