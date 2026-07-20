package com.accessibilitymanager.model;

import android.graphics.drawable.Drawable;

import java.util.Objects;

public final class AccessibilityServiceItem {
    private final String label;
    private final String description;
    private final String componentName;
    private final Drawable icon;
    private final boolean systemApp;
    private boolean enabled;
    private boolean pending;

    public AccessibilityServiceItem(
            String label,
            String description,
            String componentName,
            Drawable icon,
            boolean systemApp,
            boolean enabled
    ) {
        this.label = label;
        this.description = description;
        this.componentName = componentName;
        this.icon = icon;
        this.systemApp = systemApp;
        this.enabled = enabled;
    }

    public String getLabel() {
        return label;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getDescription() {
        return description;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean isSystemApp() {
        return systemApp;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AccessibilityServiceItem)) {
            return false;
        }
        AccessibilityServiceItem that = (AccessibilityServiceItem) other;
        return componentName.equals(that.componentName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentName);
    }
}
