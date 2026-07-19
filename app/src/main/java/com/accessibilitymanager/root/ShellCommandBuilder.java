package com.accessibilitymanager.root;

import java.util.regex.Pattern;

public final class ShellCommandBuilder {
    private static final Pattern COMPONENT_PATTERN =
            Pattern.compile("[A-Za-z0-9._$]+/[A-Za-z0-9._$]+");

    private ShellCommandBuilder() {
    }

    public static String forService(String componentName, boolean enabled) {
        if (componentName == null || !COMPONENT_PATTERN.matcher(componentName).matches()) {
            throw new IllegalArgumentException("Invalid accessibility service component");
        }

        String operation = enabled
                ? "a11yctl add " + componentName + " >/dev/null && a11yctl enable " + componentName
                : "a11yctl remove " + componentName + " >/dev/null && a11yctl disable " + componentName;
        return "command -v a11yctl >/dev/null 2>&1 || "
                + "{ echo __A11YCTL_MISSING__; exit 127; }; "
                + operation;
    }
}
