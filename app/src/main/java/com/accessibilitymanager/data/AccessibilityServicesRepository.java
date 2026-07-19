package com.accessibilitymanager.data;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.accessibility.AccessibilityManager;

import com.accessibilitymanager.model.AccessibilityServiceItem;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AccessibilityServicesRepository {
    private final AccessibilityManager accessibilityManager;
    private final PackageManager packageManager;

    public AccessibilityServicesRepository(Context context) {
        accessibilityManager = (AccessibilityManager) context.getSystemService(
                Context.ACCESSIBILITY_SERVICE
        );
        packageManager = context.getPackageManager();
    }

    public List<AccessibilityServiceItem> loadServices() {
        List<AccessibilityServiceInfo> installed =
                accessibilityManager.getInstalledAccessibilityServiceList();
        List<AccessibilityServiceInfo> enabled =
                accessibilityManager.getEnabledAccessibilityServiceList(
                        AccessibilityServiceInfo.FEEDBACK_ALL_MASK
                );

        Set<ComponentName> enabledComponents = new HashSet<>();
        for (AccessibilityServiceInfo service : enabled) {
            ComponentName component = componentOf(service);
            if (component != null) {
                enabledComponents.add(component);
            }
        }

        List<AccessibilityServiceItem> result = new ArrayList<>(installed.size());
        for (AccessibilityServiceInfo service : installed) {
            ResolveInfo resolveInfo = service.getResolveInfo();
            ComponentName component = componentOf(service);
            if (resolveInfo == null || component == null) {
                continue;
            }

            CharSequence loadedLabel = resolveInfo.loadLabel(packageManager);
            String label = loadedLabel == null
                    ? component.getPackageName()
                    : loadedLabel.toString().trim();
            if (label.isEmpty()) {
                label = component.getPackageName();
            }
            Drawable icon = resolveInfo.loadIcon(packageManager);
            String description = loadDescription(service);
            result.add(new AccessibilityServiceItem(
                    label,
                    description,
                    component.flattenToString(),
                    icon,
                    enabledComponents.contains(component)
            ));
        }

        Collator collator = Collator.getInstance(Locale.getDefault());
        result.sort((left, right) -> {
            int enabledOrder = Boolean.compare(right.isEnabled(), left.isEnabled());
            if (enabledOrder != 0) {
                return enabledOrder;
            }
            return collator.compare(left.getLabel(), right.getLabel());
        });
        return result;
    }

    private String loadDescription(AccessibilityServiceInfo service) {
        try {
            String description = service.loadDescription(packageManager);
            return description == null ? "" : description.trim();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static ComponentName componentOf(AccessibilityServiceInfo service) {
        ResolveInfo resolveInfo = service.getResolveInfo();
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            return null;
        }
        return new ComponentName(
                resolveInfo.serviceInfo.packageName,
                resolveInfo.serviceInfo.name
        );
    }
}
