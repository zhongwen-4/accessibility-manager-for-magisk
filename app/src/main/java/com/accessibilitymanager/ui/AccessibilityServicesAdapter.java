package com.accessibilitymanager.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.accessibilitymanager.R;
import com.accessibilitymanager.databinding.ItemAccessibilityServiceBinding;
import com.accessibilitymanager.model.AccessibilityServiceItem;

public final class AccessibilityServicesAdapter extends
        ListAdapter<AccessibilityServiceItem, AccessibilityServicesAdapter.ServiceViewHolder> {

    private final OnServiceToggleListener toggleListener;

    public AccessibilityServicesAdapter(OnServiceToggleListener toggleListener) {
        super(DIFF_CALLBACK);
        this.toggleListener = toggleListener;
    }

    public void updateItemState(String componentName, boolean enabled, boolean pending) {
        for (int index = 0; index < getCurrentList().size(); index++) {
            AccessibilityServiceItem item = getCurrentList().get(index);
            if (item.getComponentName().equals(componentName)) {
                item.setEnabled(enabled);
                item.setPending(pending);
                notifyItemChanged(index);
                return;
            }
        }
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAccessibilityServiceBinding binding = ItemAccessibilityServiceBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ServiceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public interface OnServiceToggleListener {
        void onServiceToggle(AccessibilityServiceItem item, boolean enabled);
    }

    final class ServiceViewHolder extends RecyclerView.ViewHolder {
        private final ItemAccessibilityServiceBinding binding;

        ServiceViewHolder(ItemAccessibilityServiceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AccessibilityServiceItem item) {
            binding.serviceName.setText(item.getLabel());
            binding.serviceComponent.setText(item.getComponentName());
            binding.serviceIcon.setImageDrawable(item.getIcon());

            binding.serviceSwitch.setOnCheckedChangeListener(null);
            binding.serviceSwitch.setChecked(item.isEnabled());
            binding.serviceSwitch.setEnabled(!item.isPending());
            binding.serviceSwitch.setVisibility(item.isPending() ? View.INVISIBLE : View.VISIBLE);
            binding.serviceSwitch.setContentDescription(
                    binding.getRoot().getContext().getString(
                            R.string.service_switch_description,
                            item.getLabel()
                    )
            );
            binding.pendingIndicator.setVisibility(item.isPending() ? View.VISIBLE : View.GONE);

            binding.serviceSwitch.setOnCheckedChangeListener((button, checked) ->
                    toggleListener.onServiceToggle(item, checked)
            );
            binding.getRoot().setOnClickListener(view -> {
                if (!item.isPending()) {
                    binding.serviceSwitch.toggle();
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<AccessibilityServiceItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AccessibilityServiceItem>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull AccessibilityServiceItem oldItem,
                        @NonNull AccessibilityServiceItem newItem
                ) {
                    return oldItem.getComponentName().equals(newItem.getComponentName());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull AccessibilityServiceItem oldItem,
                        @NonNull AccessibilityServiceItem newItem
                ) {
                    return oldItem.getLabel().equals(newItem.getLabel())
                            && oldItem.isEnabled() == newItem.isEnabled()
                            && oldItem.isPending() == newItem.isPending();
                }
            };
}
