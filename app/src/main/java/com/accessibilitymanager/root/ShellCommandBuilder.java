package com.accessibilitymanager.root;

import java.util.regex.Pattern;

public final class ShellCommandBuilder {
    private static final String COMMAND_PREFIX = "command -v a11yctl >/dev/null 2>&1 || "
            + "{ echo __A11YCTL_MISSING__; exit 127; }; ";
    private static final Pattern COMPONENT_PATTERN =
            Pattern.compile("[A-Za-z0-9._$]+/[A-Za-z0-9._$]+");

    private ShellCommandBuilder() {
    }

    public static String forService(String componentName, boolean enabled) {
        if (componentName == null || !COMPONENT_PATTERN.matcher(componentName).matches()) {
            throw new IllegalArgumentException("Invalid accessibility service component");
        }

        String operation = enabled ? "enable" : "disable";
        return COMMAND_PREFIX + "a11yctl " + operation + " " + componentName;
    }

    public static String forLock(String componentName, boolean locked) {
        requireValidComponent(componentName);
        if (locked) {
            return COMMAND_PREFIX + "a11yctl add " + componentName
                    + " >/dev/null && a11yctl enable " + componentName;
        }
        return COMMAND_PREFIX + "a11yctl remove " + componentName;
    }

    public static String forLockedServices() {
        return COMMAND_PREFIX + "a11yctl configured";
    }

    static boolean isValidComponent(String componentName) {
        return componentName != null && COMPONENT_PATTERN.matcher(componentName).matches();
    }

    private static void requireValidComponent(String componentName) {
        if (!isValidComponent(componentName)) {
            throw new IllegalArgumentException("Invalid accessibility service component");
        }
    }
}
