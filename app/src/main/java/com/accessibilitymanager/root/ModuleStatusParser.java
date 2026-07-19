package com.accessibilitymanager.root;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ModuleStatusParser {
    private static final Pattern STATUS_PATTERN = Pattern.compile(
            "__A11Y_STATUS__:(\\d+):(\\d+):([01]):([01])"
    );

    private ModuleStatusParser() {
    }

    public static Status parse(String output) {
        Matcher matcher = STATUS_PATTERN.matcher(output == null ? "" : output);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Module status marker is missing");
        }
        return new Status(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                "1".equals(matcher.group(3)),
                "1".equals(matcher.group(4))
        );
    }

    public static final class Status {
        private final int installedVersionCode;
        private final int pendingVersionCode;
        private final boolean mounted;
        private final boolean disabled;

        private Status(
                int installedVersionCode,
                int pendingVersionCode,
                boolean mounted,
                boolean disabled
        ) {
            this.installedVersionCode = installedVersionCode;
            this.pendingVersionCode = pendingVersionCode;
            this.mounted = mounted;
            this.disabled = disabled;
        }

        public int getInstalledVersionCode() {
            return installedVersionCode;
        }

        public int getPendingVersionCode() {
            return pendingVersionCode;
        }

        public boolean isMounted() {
            return mounted;
        }

        public boolean isDisabled() {
            return disabled;
        }
    }
}
