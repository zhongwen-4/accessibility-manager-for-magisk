package com.accessibilitymanager

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.accessibilitymanager.data.AccessibilityServicesRepository
import com.accessibilitymanager.root.MagiskModuleInstaller
import com.accessibilitymanager.root.RootServiceManager
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val rootServiceManager = RootServiceManager()

    private lateinit var repository: AccessibilityServicesRepository
    private lateinit var moduleInstaller: MagiskModuleInstaller
    private var homeState by mutableStateOf(
        HomeState(
            moduleNotice = ModuleNotice(
                R.string.module_installing_title,
                R.string.module_installing_message,
                loading = true,
            ),
        ),
    )
    private var moduleOperationRunning = false
    private var moduleReady = false
    private var serviceLoadRunning = false
    private var destroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = AccessibilityServicesRepository(applicationContext)
        moduleInstaller = MagiskModuleInstaller(applicationContext)

        setContent {
            MiuixTheme(colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                AccessibilityManagerScreen(
                    state = homeState,
                    controlsEnabled = moduleReady,
                    onRefresh = ::refresh,
                    onToggle = ::toggleService,
                    onAction = ::performStateAction,
                )
            }
        }

        loadServices(showFullProgress = true)
        ensureModuleReady()
    }

    private fun refresh() {
        loadServices(showFullProgress = false)
        if (!moduleReady) ensureModuleReady()
    }

    private fun ensureModuleReady() {
        if (moduleOperationRunning) return
        moduleOperationRunning = true
        moduleReady = false
        homeState = homeState.copy(
            moduleNotice = ModuleNotice(
                R.string.module_installing_title,
                R.string.module_installing_message,
                loading = true,
            ),
        )

        executor.execute {
            val result = moduleInstaller.ensureInstalled(BuildConfig.BUNDLED_MODULE_VERSION_CODE)
            mainHandler.post {
                if (destroyed) return@post
                moduleOperationRunning = false
                when {
                    result.isSuccessful && result.state == MagiskModuleInstaller.State.READY -> {
                        moduleReady = true
                        homeState = homeState.copy(moduleNotice = null)
                        loadServices(showFullProgress = false)
                    }

                    result.isSuccessful &&
                        result.state == MagiskModuleInstaller.State.REBOOT_REQUIRED -> {
                        homeState = homeState.copy(
                            moduleNotice = ModuleNotice(
                                R.string.module_ready_title,
                                R.string.module_ready_message,
                                StateAction.REBOOT,
                                R.string.reboot_now,
                            ),
                        )
                    }

                    else -> showModuleFailure(result.state)
                }
            }
        }
    }

    private fun loadServices(showFullProgress: Boolean) {
        if (serviceLoadRunning) return
        serviceLoadRunning = true
        val current = homeState
        homeState = if (showFullProgress && current.services.isEmpty()) {
            current.copy(listState = ServiceListState.LOADING, refreshing = false)
        } else {
            current.copy(refreshing = true)
        }

        executor.execute {
            try {
                val services = repository.loadServices().map { service ->
                    ServiceUiModel(
                        label = service.label,
                        componentName = service.componentName,
                        icon = service.icon,
                        enabled = service.isEnabled,
                    )
                }
                mainHandler.post {
                    if (!destroyed) {
                        serviceLoadRunning = false
                        homeState = homeState.copy(
                            services = services,
                            listState = ServiceListState.READY,
                            refreshing = false,
                        )
                    }
                }
            } catch (_: RuntimeException) {
                mainHandler.post {
                    if (!destroyed) {
                        serviceLoadRunning = false
                        if (homeState.services.isEmpty()) {
                            homeState = homeState.copy(
                                listState = ServiceListState.ERROR,
                                refreshing = false,
                            )
                        } else {
                            homeState = homeState.copy(refreshing = false)
                            Toast.makeText(
                                this,
                                R.string.load_failed_message,
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun toggleService(item: ServiceUiModel, enabled: Boolean) {
        if (!moduleReady) {
            ensureModuleReady()
            return
        }
        updateService(item.componentName) { it.copy(pending = true) }
        executor.execute {
            val result = rootServiceManager.setEnabled(item.componentName, enabled)
            mainHandler.post {
                if (destroyed) return@post
                if (result.isSuccessful) {
                    updateService(item.componentName) { it.copy(enabled = enabled, pending = false) }
                    mainHandler.postDelayed({ loadServices(showFullProgress = false) }, 600)
                } else {
                    updateService(item.componentName) { it.copy(pending = false) }
                    Toast.makeText(this, errorMessage(result.error), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateService(
        componentName: String,
        transform: (ServiceUiModel) -> ServiceUiModel,
    ) {
        homeState = homeState.copy(
            services = homeState.services.map { service ->
                if (service.componentName == componentName) transform(service) else service
            },
        )
    }

    private fun performStateAction(action: StateAction) {
        when (action) {
            StateAction.RETRY_MODULE -> ensureModuleReady()
            StateAction.RETRY_LIST -> loadServices(showFullProgress = true)
            StateAction.REBOOT -> requestReboot()
        }
    }

    private fun requestReboot() {
        if (moduleOperationRunning) return
        moduleOperationRunning = true
        homeState = homeState.copy(
            moduleNotice = ModuleNotice(
                R.string.rebooting_title,
                R.string.rebooting_message,
                loading = true,
            ),
        )
        executor.execute {
            val result = moduleInstaller.reboot()
            mainHandler.post {
                if (destroyed) return@post
                moduleOperationRunning = false
                if (!result.isSuccessful) showModuleFailure(result.state)
            }
        }
    }

    private fun showModuleFailure(state: MagiskModuleInstaller.State) {
        val title: Int
        val message: Int
        if (state == MagiskModuleInstaller.State.MODULE_DISABLED) {
            title = R.string.module_disabled_title
            message = R.string.module_disabled_message
        } else {
            title = R.string.module_install_failed_title
            message = when (state) {
                MagiskModuleInstaller.State.ROOT_UNAVAILABLE -> R.string.module_root_unavailable
                MagiskModuleInstaller.State.MAGISK_MISSING -> R.string.module_magisk_missing
                MagiskModuleInstaller.State.ASSET_ERROR -> R.string.module_asset_error
                MagiskModuleInstaller.State.INSTALL_FAILED -> R.string.module_install_error
                MagiskModuleInstaller.State.TIMEOUT -> R.string.module_install_timeout
                else -> R.string.module_root_denied
            }
        }
        homeState = homeState.copy(
            moduleNotice = ModuleNotice(
                title,
                message,
                StateAction.RETRY_MODULE,
                R.string.retry,
            ),
        )
    }

    @StringRes
    private fun errorMessage(error: RootServiceManager.Error): Int = when (error) {
        RootServiceManager.Error.MODULE_MISSING -> R.string.error_module_missing
        RootServiceManager.Error.ROOT_UNAVAILABLE -> R.string.error_root_unavailable
        RootServiceManager.Error.TIMEOUT -> R.string.error_command_timeout
        RootServiceManager.Error.INVALID_COMPONENT -> R.string.error_invalid_component
        RootServiceManager.Error.INTERRUPTED -> R.string.error_operation_interrupted
        else -> R.string.error_root_denied
    }

    override fun onDestroy() {
        destroyed = true
        mainHandler.removeCallbacksAndMessages(null)
        executor.shutdownNow()
        super.onDestroy()
    }
}

private data class HomeState(
    val services: List<ServiceUiModel> = emptyList(),
    val listState: ServiceListState = ServiceListState.LOADING,
    val refreshing: Boolean = false,
    val moduleNotice: ModuleNotice? = null,
)

private enum class ServiceListState {
    LOADING,
    READY,
    ERROR,
}

private data class ModuleNotice(
    @param:StringRes val title: Int,
    @param:StringRes val message: Int,
    val action: StateAction? = null,
    @param:StringRes val actionText: Int? = null,
    val loading: Boolean = false,
)

private enum class StateAction {
    RETRY_MODULE,
    RETRY_LIST,
    REBOOT,
}

private data class ServiceUiModel(
    val label: String,
    val componentName: String,
    val icon: Drawable,
    val enabled: Boolean,
    val pending: Boolean = false,
)

@Composable
private fun AccessibilityManagerScreen(
    state: HomeState,
    controlsEnabled: Boolean,
    onRefresh: () -> Unit,
    onToggle: (ServiceUiModel, Boolean) -> Unit,
    onAction: (StateAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.app_name),
                actions = {
                    if (state.refreshing) {
                        CircularProgressIndicator(size = 24.dp)
                    } else {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.refresh),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        HomeContent(
            innerPadding = innerPadding,
            state = state,
            controlsEnabled = controlsEnabled,
            onToggle = onToggle,
            onAction = onAction,
        )
    }
}

@Composable
private fun HomeContent(
    innerPadding: PaddingValues,
    state: HomeState,
    controlsEnabled: Boolean,
    onToggle: (ServiceUiModel, Boolean) -> Unit,
    onAction: (StateAction) -> Unit,
) {
    val summary = pluralStringResource(
        R.plurals.services_summary,
        state.services.size,
        state.services.size,
        state.services.count { it.enabled },
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = innerPadding.calculateTopPadding() + 8.dp,
            end = 16.dp,
            bottom = innerPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.moduleNotice?.let { notice ->
            item(key = "module-notice") {
                ModuleNoticeCard(notice = notice, onAction = onAction)
            }
        }

        when (state.listState) {
            ServiceListState.LOADING -> item(key = "services-loading") {
                ListStatusContent(
                    title = stringResource(R.string.loading_services_title),
                    message = stringResource(R.string.loading_services_message),
                    loading = true,
                )
            }

            ServiceListState.ERROR -> item(key = "services-error") {
                ListStatusContent(
                    title = stringResource(R.string.load_failed_title),
                    message = stringResource(R.string.load_failed_message),
                    actionText = stringResource(R.string.retry),
                    onAction = { onAction(StateAction.RETRY_LIST) },
                )
            }

            ServiceListState.READY -> {
                if (state.services.isEmpty()) {
                    item(key = "services-empty") {
                        ListStatusContent(
                            title = stringResource(R.string.no_services_title),
                            message = stringResource(R.string.no_services_message),
                        )
                    }
                } else {
                    item(key = "summary") {
                        Text(
                            text = summary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.body1,
                        )
                    }
                    items(state.services, key = { it.componentName }) { service ->
                        ServiceRow(
                            service = service,
                            controlsEnabled = controlsEnabled,
                            onToggle = onToggle,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleNoticeCard(
    notice: ModuleNotice,
    onAction: (StateAction) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (notice.loading) {
                CircularProgressIndicator(size = 28.dp)
                Spacer(Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(notice.title),
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.headline1,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(notice.message),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                )
            }
            if (notice.action != null && notice.actionText != null) {
                Spacer(Modifier.width(12.dp))
                TextButton(
                    text = stringResource(notice.actionText),
                    onClick = { onAction(notice.action) },
                )
            }
        }
    }
}

@Composable
private fun ServiceRow(
    service: ServiceUiModel,
    controlsEnabled: Boolean,
    onToggle: (ServiceUiModel, Boolean) -> Unit,
) {
    val bitmap = remember(service.componentName, service.icon) { service.icon.toImageBitmap() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (service.pending || !controlsEnabled) {
            null
        } else {
            ({ onToggle(service, !service.enabled) })
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.label,
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.headline1,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = service.componentName,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.size(width = 58.dp, height = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (service.pending) {
                    CircularProgressIndicator(size = 26.dp)
                } else {
                    Switch(
                        checked = service.enabled,
                        onCheckedChange = { onToggle(service, it) },
                        enabled = controlsEnabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun ListStatusContent(
    title: String,
    message: String,
    loading: Boolean = false,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(size = 44.dp)
            Spacer(Modifier.height(24.dp))
        }
        Text(
            text = title,
            color = MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body1,
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(22.dp))
            TextButton(text = actionText, onClick = onAction)
        }
    }
}

private fun Drawable.toImageBitmap(): ImageBitmap {
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    val bitmap = createBitmap(width, height)
    val oldBounds = copyBounds()
    setBounds(0, 0, width, height)
    draw(Canvas(bitmap))
    bounds = oldBounds
    return bitmap.asImageBitmap()
}
