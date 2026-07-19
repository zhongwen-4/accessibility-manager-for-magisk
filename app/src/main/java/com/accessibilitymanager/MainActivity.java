package com.accessibilitymanager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.accessibilitymanager.data.AccessibilityServicesRepository;
import com.accessibilitymanager.databinding.ActivityMainBinding;
import com.accessibilitymanager.model.AccessibilityServiceItem;
import com.accessibilitymanager.root.RootServiceManager;
import com.accessibilitymanager.ui.AccessibilityServicesAdapter;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final RootServiceManager rootServiceManager = new RootServiceManager();

    private ActivityMainBinding binding;
    private AccessibilityServicesRepository repository;
    private AccessibilityServicesAdapter adapter;
    private boolean loading;
    private boolean destroyed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        repository = new AccessibilityServicesRepository(getApplicationContext());
        adapter = new AccessibilityServicesAdapter(this::toggleService);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.servicesList.setLayoutManager(layoutManager);
        binding.servicesList.setAdapter(adapter);
        binding.servicesList.addItemDecoration(new DividerItemDecoration(
                this,
                layoutManager.getOrientation()
        ));

        loadServices(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            loadServices(false);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadServices(boolean showFullProgress) {
        if (loading) {
            return;
        }
        loading = true;
        if (showFullProgress && adapter.getItemCount() == 0) {
            showLoadingState();
        }

        executor.execute(() -> {
            try {
                List<AccessibilityServiceItem> services = repository.loadServices();
                mainHandler.post(() -> {
                    if (!destroyed) {
                        showServices(services);
                    }
                });
            } catch (RuntimeException exception) {
                mainHandler.post(() -> {
                    if (!destroyed) {
                        showLoadError();
                    }
                });
            }
        });
    }

    private void toggleService(AccessibilityServiceItem item, boolean enabled) {
        adapter.updateItemState(item.getComponentName(), item.isEnabled(), true);

        executor.execute(() -> {
            RootServiceManager.Result result = rootServiceManager.setEnabled(
                    item.getComponentName(),
                    enabled
            );
            mainHandler.post(() -> {
                if (destroyed) {
                    return;
                }
                if (result.isSuccessful()) {
                    adapter.updateItemState(item.getComponentName(), enabled, false);
                    mainHandler.postDelayed(() -> loadServices(false), 600);
                } else {
                    adapter.updateItemState(item.getComponentName(), item.isEnabled(), false);
                    Snackbar.make(
                            binding.getRoot(),
                            errorMessage(result.getError()),
                            Snackbar.LENGTH_LONG
                    ).show();
                }
            });
        });
    }

    private void showLoadingState() {
        binding.loadingIndicator.setVisibility(View.VISIBLE);
        binding.servicesList.setVisibility(View.GONE);
        binding.emptyState.setVisibility(View.GONE);
    }

    private void showServices(List<AccessibilityServiceItem> services) {
        loading = false;
        binding.loadingIndicator.setVisibility(View.GONE);
        adapter.submitList(new ArrayList<>(services));

        int enabledCount = 0;
        for (AccessibilityServiceItem item : services) {
            if (item.isEnabled()) {
                enabledCount++;
            }
        }
        binding.toolbar.setSubtitle(getResources().getQuantityString(
                R.plurals.services_summary,
                services.size(),
                services.size(),
                enabledCount
        ));

        boolean empty = services.isEmpty();
        binding.servicesList.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.emptyTitle.setText(R.string.no_services_title);
        binding.emptyMessage.setText(R.string.no_services_message);
    }

    private void showLoadError() {
        loading = false;
        binding.loadingIndicator.setVisibility(View.GONE);
        binding.servicesList.setVisibility(View.GONE);
        binding.emptyState.setVisibility(View.VISIBLE);
        binding.emptyTitle.setText(R.string.load_failed_title);
        binding.emptyMessage.setText(R.string.load_failed_message);
        binding.toolbar.setSubtitle(null);
    }

    private int errorMessage(RootServiceManager.Error error) {
        switch (error) {
            case MODULE_MISSING:
                return R.string.error_module_missing;
            case ROOT_UNAVAILABLE:
                return R.string.error_root_unavailable;
            case TIMEOUT:
                return R.string.error_command_timeout;
            case INVALID_COMPONENT:
                return R.string.error_invalid_component;
            case INTERRUPTED:
                return R.string.error_operation_interrupted;
            case ROOT_DENIED_OR_COMMAND_FAILED:
            default:
                return R.string.error_root_denied;
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        mainHandler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        super.onDestroy();
    }
}
