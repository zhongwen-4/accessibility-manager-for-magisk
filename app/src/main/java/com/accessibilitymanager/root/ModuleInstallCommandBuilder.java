package com.accessibilitymanager.root;

public final class ModuleInstallCommandBuilder {
    public static final String ROOT_MANAGER_MISSING_MARKER = "__ROOT_MANAGER_MISSING__";

    private ModuleInstallCommandBuilder() {
    }

    public static String build(String moduleZipPath) {
        if (moduleZipPath == null || moduleZipPath.isEmpty()) {
            throw new IllegalArgumentException("Module ZIP path is empty");
        }

        String quotedPath = shellQuote(moduleZipPath);
        return "ksud_bin=$(command -v ksud 2>/dev/null); "
                + "[ -n \"$ksud_bin\" ] || "
                + "[ ! -x /data/adb/ksud ] || ksud_bin=/data/adb/ksud; "
                + "[ -n \"$ksud_bin\" ] || "
                + "[ ! -x /data/adb/ksu/bin/ksud ] || ksud_bin=/data/adb/ksu/bin/ksud; "
                + "if [ -n \"$ksud_bin\" ]; then "
                + "\"$ksud_bin\" module install " + quotedPath + "; exit $?; fi; "
                + "magisk_bin=$(command -v magisk 2>/dev/null); "
                + "[ -n \"$magisk_bin\" ] || "
                + "[ ! -x /data/adb/magisk/magisk ] || magisk_bin=/data/adb/magisk/magisk; "
                + "[ -n \"$magisk_bin\" ] || "
                + "{ echo " + ROOT_MANAGER_MISSING_MARKER + "; exit 127; }; "
                + "\"$magisk_bin\" --install-module " + quotedPath;
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
