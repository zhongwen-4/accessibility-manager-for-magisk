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
import com.accessibilitymanager.root.MagiskModuleInstaller;
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
    private MagiskModuleInstaller moduleInstaller;
    private boolean loading;
    private boolean moduleOperationRunning;
    private boolean moduleReady;
    private boolean destroyed;
    private StateAction stateAction = StateAction.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        repository = new AccessibilityServicesRepository(getApplicationContext());
        moduleInstaller = new MagiskModuleInstaller(getApplicationContext());
        adapter = new AccessibilityServicesAdapter(this::toggleService);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.servicesList.setLayoutManager(layoutManager);
        binding.servicesList.setAdapter(adapter);
        binding.servicesList.addItemDecoration(new DividerItemDecoration(
                this,
                layoutManager.getOrientation()
        ));
        binding.stateAction.setOnClickListener(view -> performStateAction());

        ensureModuleReady();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            if (moduleReady) {
                loadServices(false);
            } else {
                ensureModuleReady();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void ensureModuleReady() {
        if (moduleOperationRunning) {
            return;
        }
        moduleOperationRunning = true;
        moduleReady = false;
        showModuleProgress();

        executor.execute(() -> {
            MagiskModuleInstaller.Result result = moduleInstaller.ensureInstalled(
                    BuildConfig.BUNDLED_MODULE_VERSION_CODE
            );
            mainHandler.post(() -> {
                if (destroyed) {
                    return;
                }
                moduleOperationRunning = false;
                if (result.isSuccessful()
                        && result.getState() == MagiskModuleInstaller.State.READY) {
                    moduleReady = true;
                    loadServices(true);
                } else if (result.isSuccessful()
                        && result.getState() == MagiskModuleInstaller.State.REBOOT_REQUIRED) {
                    showRebootRequired();
                } else {
                    showModuleFailure(result.getState());
                }
            });
        });
    }

    private void performStateAction() {
        switch (stateAction) {
            case RETRY_MODULE:
                ensureModuleReady();
                break;
            case RETRY_LIST:
                loadServices(true);
                break;
            case REBOOT:
                requestReboot();
                break;
            case NONE:
            default:
                break;
        }
    }

    private void requestReboot() {
        if (moduleOperationRunning) {
            return;
        }
        moduleOperationRunning = true;
        showState(
                true,
                R.string.rebooting_title,
                R.string.rebooting_message,
                StateAction.NONE,
                0
        );
        executor.execute(() -> {
            MagiskModuleInstaller.Result result = moduleInstaller.reboot();
            mainHandler.post(() -> {
                if (destroyed) {
                    return;
                }
                moduleOperationRunning = false;
                if (!result.isSuccessful()) {
                    showModuleFailure(result.getState());
                }
            });
        });
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

    private void showModuleProgress() {
        showState(
                true,
                R.string.module_installing_title,
                R.string.module_installing_message,
                StateAction.NONE,
                0
        );
        binding.toolbar.setSubtitle(null);
    }

    private void showRebootRequired() {
        showState(
                false,
                R.string.module_ready_title,
                R.string.module_ready_message,
                StateAction.REBOOT,
                R.string.reboot_now
        );
        binding.toolbar.setSubtitle(null);
    }

    private void showModuleFailure(MagiskModuleInstaller.State state) {
        int title = R.string.module_install_failed_title;
        int message;
        switch (state) {
            case MODULE_DISABLED:
                title = R.string.module_disabled_title;
                message = R.string.module_disabled_message;
                break;
            case ROOT_UNAVAILABLE:
                message = R.string.module_root_unavailable;
                break;
            case MAGISK_MISSING:
                message = R.string.module_magisk_missing;
                break;
            case ASSET_ERROR:
                message = R.string.module_asset_error;
                break;
            case INSTALL_FAILED:
                message = R.string.module_install_error;
                break;
            case TIMEOUT:
                message = R.string.module_install_timeout;
                break;
            case ROOT_DENIED_OR_COMMAND_FAILED:
            default:
                message = R.string.module_root_denied;
                break;
        }
        showState(false, title, message, StateAction.RETRY_MODULE, R.string.retry);
        binding.toolbar.setSubtitle(null);
    }

    private void showState(
            boolean progress,
            int title,
            int message,
            StateAction action,
            int actionText
    ) {
        loading = false;
        stateAction = action;
        binding.loadingIndicator.setVisibility(View.GONE);
        binding.servicesList.setVisibility(View.GONE);
        binding.emptyState.setVisibility(View.VISIBLE);
        binding.stateIcon.setVisibility(progress ? View.GONE : View.VISIBLE);
        binding.stateProgressIndicator.setVisibility(progress ? View.VISIBLE : View.GONE);
        binding.emptyTitle.setText(title);
        binding.emptyMessage.setText(message);
        binding.stateAction.setVisibility(action == StateAction.NONE ? View.GONE : View.VISIBLE);
        if (actionText != 0) {
            binding.stateAction.setText(actionText);
        }
    }

    private void showServices(List<AccessibilityServiceItem> services) {
        loading = false;
        binding.loadingIndicator.setVisibility(View.GONE);
        binding.stateProgressIndicator.setVisibility(View.GONE);
        binding.stateAction.setVisibility(View.GONE);
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
        binding.stateIcon.setVisibility(View.VISIBLE);
        binding.emptyTitle.setText(R.string.no_services_title);
        binding.emptyMessage.setText(R.string.no_services_message);
        stateAction = StateAction.NONE;
    }

    private void showLoadError() {
        loading = false;
        binding.loadingIndicator.setVisibility(View.GONE);
        binding.servicesList.setVisibility(View.GONE);
        binding.emptyState.setVisibility(View.VISIBLE);
        binding.stateIcon.setVisibility(View.VISIBLE);
        binding.stateProgressIndicator.setVisibility(View.GONE);
        binding.emptyTitle.setText(R.string.load_failed_title);
        binding.emptyMessage.setText(R.string.load_failed_message);
        binding.stateAction.setVisibility(View.VISIBLE);
        binding.stateAction.setText(R.string.retry);
        stateAction = StateAction.RETRY_LIST;
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

    private enum StateAction {
        NONE,
        RETRY_MODULE,
        RETRY_LIST,
        REBOOT
    }
}
