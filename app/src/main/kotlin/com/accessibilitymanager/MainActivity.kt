package com.accessibilitymanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import com.accessibilitymanager.data.AccessibilityServicesRepository
import com.accessibilitymanager.data.ManagerLogEntry
import com.accessibilitymanager.data.ManagerLogLevel
import com.accessibilitymanager.data.ManagerLogStore
import com.accessibilitymanager.root.RootModuleInstaller
import com.accessibilitymanager.root.RootServiceManager
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val rootServiceManager = RootServiceManager()

    private lateinit var repository: AccessibilityServicesRepository
    private lateinit var moduleInstaller: RootModuleInstaller
    private lateinit var logStore: ManagerLogStore
    private var logEntries by mutableStateOf<List<ManagerLogEntry>>(emptyList())
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
        moduleInstaller = RootModuleInstaller(applicationContext)
        logStore = ManagerLogStore(applicationContext)
        logEntries = logStore.load()
        addLog(ManagerLogLevel.INFO, R.string.log_app_started)

        setContent {
            MiuixTheme(colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                AccessibilityManagerScreen(
                    state = homeState,
                    logEntries = logEntries,
                    controlsEnabled = moduleReady,
                    onRefresh = ::refresh,
                    onToggle = ::toggleService,
                    onSetLocked = ::setServiceLocked,
                    onAction = ::performStateAction,
                    onCopyLogs = ::copyLogs,
                    onClearLogs = ::clearLogs,
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
        addLog(ManagerLogLevel.INFO, R.string.log_module_check_started)
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
                    result.isSuccessful && result.state == RootModuleInstaller.State.READY -> {
                        moduleReady = true
                        homeState = homeState.copy(moduleNotice = null)
                        addLog(ManagerLogLevel.SUCCESS, R.string.log_module_ready)
                        loadServices(showFullProgress = false)
                    }

                    result.isSuccessful &&
                        result.state == RootModuleInstaller.State.REBOOT_REQUIRED -> {
                        homeState = homeState.copy(
                            moduleNotice = ModuleNotice(
                                R.string.module_ready_title,
                                R.string.module_ready_message,
                                StateAction.REBOOT,
                                R.string.reboot_now,
                            ),
                        )
                        addLog(ManagerLogLevel.SUCCESS, R.string.log_module_reboot_required)
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
        val knownLockedServices = current.services
            .filter { it.locked }
            .mapTo(mutableSetOf()) { it.componentName }
        val loadLockedServices = moduleReady
        homeState = if (showFullProgress && current.services.isEmpty()) {
            current.copy(listState = ServiceListState.LOADING, refreshing = false)
        } else {
            current.copy(refreshing = true)
        }

        executor.execute {
            try {
                val lockedResult = if (loadLockedServices) {
                    rootServiceManager.getLockedServices()
                } else {
                    null
                }
                val lockedServices = if (lockedResult?.isSuccessful == true) {
                    lockedResult.components
                } else {
                    knownLockedServices
                }
                val services = repository.loadServices().map { service ->
                    ServiceUiModel(
                        label = service.label,
                        description = service.description,
                        componentName = service.componentName,
                        icon = service.icon,
                        systemApp = service.isSystemApp,
                        enabled = service.isEnabled,
                        locked = service.componentName in lockedServices,
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
                        addLog(
                            ManagerLogLevel.INFO,
                            R.string.log_services_loaded,
                            services.size,
                        )
                        if (lockedResult != null && !lockedResult.isSuccessful) {
                            addLog(
                                ManagerLogLevel.ERROR,
                                R.string.log_locks_load_failed,
                                getString(errorMessage(lockedResult.error)),
                            )
                        }
                    }
                }
            } catch (_: RuntimeException) {
                mainHandler.post {
                    if (!destroyed) {
                        serviceLoadRunning = false
                        addLog(ManagerLogLevel.ERROR, R.string.log_services_load_failed)
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
                    addLog(
                        ManagerLogLevel.SUCCESS,
                        if (enabled) R.string.log_service_enabled else R.string.log_service_disabled,
                        item.label,
                    )
                    mainHandler.postDelayed({ loadServices(showFullProgress = false) }, 600)
                } else {
                    updateService(item.componentName) { it.copy(pending = false) }
                    addLog(
                        ManagerLogLevel.ERROR,
                        R.string.log_service_toggle_failed,
                        item.label,
                        getString(errorMessage(result.error)),
                    )
                    Toast.makeText(this, errorMessage(result.error), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setServiceLocked(item: ServiceUiModel, locked: Boolean) {
        if (!moduleReady) {
            ensureModuleReady()
            return
        }
        updateService(item.componentName) { it.copy(pending = true) }
        executor.execute {
            val result = rootServiceManager.setLocked(item.componentName, locked)
            mainHandler.post {
                if (destroyed) return@post
                if (result.isSuccessful) {
                    updateService(item.componentName) {
                        it.copy(
                            enabled = if (locked) true else it.enabled,
                            locked = locked,
                            pending = false,
                        )
                    }
                    addLog(
                        ManagerLogLevel.SUCCESS,
                        if (locked) R.string.log_service_locked else R.string.log_service_unlocked,
                        item.label,
                    )
                    mainHandler.postDelayed({ loadServices(showFullProgress = false) }, 600)
                } else {
                    updateService(item.componentName) { it.copy(pending = false) }
                    addLog(
                        ManagerLogLevel.ERROR,
                        R.string.log_service_lock_failed,
                        item.label,
                        getString(errorMessage(result.error)),
                    )
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
        addLog(ManagerLogLevel.INFO, R.string.log_reboot_requested)
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
                if (!result.isSuccessful) {
                    addLog(
                        ManagerLogLevel.ERROR,
                        R.string.log_reboot_failed,
                        result.state.name,
                    )
                    showModuleFailure(result.state)
                }
            }
        }
    }

    private fun showModuleFailure(state: RootModuleInstaller.State) {
        val title: Int
        val message: Int
        if (state == RootModuleInstaller.State.MODULE_DISABLED) {
            title = R.string.module_disabled_title
            message = R.string.module_disabled_message
        } else {
            title = R.string.module_install_failed_title
            message = when (state) {
                RootModuleInstaller.State.ROOT_UNAVAILABLE -> R.string.module_root_unavailable
                RootModuleInstaller.State.ROOT_MANAGER_MISSING -> R.string.module_root_manager_missing
                RootModuleInstaller.State.ASSET_ERROR -> R.string.module_asset_error
                RootModuleInstaller.State.INSTALL_FAILED -> R.string.module_install_error
                RootModuleInstaller.State.TIMEOUT -> R.string.module_install_timeout
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
        addLog(
            ManagerLogLevel.ERROR,
            R.string.log_module_failed,
            getString(message),
        )
    }

    private fun addLog(
        level: ManagerLogLevel,
        @StringRes message: Int,
        vararg formatArgs: Any,
    ) {
        logEntries = logStore.append(level, getString(message, *formatArgs))
    }

    private fun copyLogs() {
        if (logEntries.isEmpty()) return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = logEntries.joinToString(separator = "\n") { formatLogEntry(it) }
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.logs), text))
        Toast.makeText(this, R.string.logs_copied, Toast.LENGTH_SHORT).show()
    }

    private fun clearLogs() {
        logStore.clear()
        logEntries = emptyList()
        Toast.makeText(this, R.string.logs_cleared, Toast.LENGTH_SHORT).show()
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

private enum class ManagerPage {
    HOME,
    SERVICES,
    LOGS,
}

private const val FILTER_SYSTEM_APPS = 1
private const val FILTER_USER_APPS = 1 shl 1
private const val FILTER_DISABLED = 1 shl 2
private const val FILTER_ENABLED = 1 shl 3
private const val FILTER_APP_TYPE_MASK = FILTER_SYSTEM_APPS or FILTER_USER_APPS
private const val FILTER_SERVICE_STATE_MASK = FILTER_DISABLED or FILTER_ENABLED

private data class ServiceUiModel(
    val label: String,
    val description: String,
    val componentName: String,
    val icon: Drawable,
    val systemApp: Boolean,
    val enabled: Boolean,
    val locked: Boolean,
    val pending: Boolean = false,
)

@Composable
private fun AccessibilityManagerScreen(
    state: HomeState,
    logEntries: List<ManagerLogEntry>,
    controlsEnabled: Boolean,
    onRefresh: () -> Unit,
    onToggle: (ServiceUiModel, Boolean) -> Unit,
    onSetLocked: (ServiceUiModel, Boolean) -> Unit,
    onAction: (StateAction) -> Unit,
    onCopyLogs: () -> Unit,
    onClearLogs: () -> Unit,
) {
    var selectedPage by remember { mutableStateOf(ManagerPage.HOME) }
    var serviceSearchVisible by rememberSaveable { mutableStateOf(false) }
    var serviceSearchQuery by rememberSaveable { mutableStateOf("") }
    var serviceFiltersVisible by rememberSaveable { mutableStateOf(false) }
    var serviceFilterMask by rememberSaveable { mutableIntStateOf(0) }
    val homeScrollBehavior = MiuixScrollBehavior()
    val servicesScrollBehavior = MiuixScrollBehavior()
    val logsScrollBehavior = MiuixScrollBehavior()
    val currentScrollBehavior = when (selectedPage) {
        ManagerPage.HOME -> homeScrollBehavior
        ManagerPage.SERVICES -> servicesScrollBehavior
        ManagerPage.LOGS -> logsScrollBehavior
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = when (selectedPage) {
                    ManagerPage.HOME -> stringResource(R.string.app_name)
                    ManagerPage.SERVICES -> stringResource(R.string.services)
                    ManagerPage.LOGS -> stringResource(R.string.logs)
                },
                actions = {
                    if (selectedPage == ManagerPage.LOGS) {
                        if (logEntries.isNotEmpty()) {
                            IconButton(onClick = onCopyLogs) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = stringResource(R.string.copy_logs),
                                )
                            }
                            IconButton(onClick = onClearLogs) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = stringResource(R.string.clear_logs),
                                )
                            }
                        }
                    } else {
                        if (selectedPage == ManagerPage.SERVICES) {
                            IconButton(
                                onClick = {
                                    serviceSearchVisible = !serviceSearchVisible
                                    if (!serviceSearchVisible) {
                                        serviceSearchQuery = ""
                                        serviceFiltersVisible = false
                                        serviceFilterMask = 0
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = if (serviceSearchVisible) {
                                        Icons.Rounded.Close
                                    } else {
                                        Icons.Rounded.Search
                                    },
                                    contentDescription = stringResource(
                                        if (serviceSearchVisible) {
                                            R.string.close_search
                                        } else {
                                            R.string.search_services
                                        },
                                    ),
                                )
                            }
                        }
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
                    }
                },
                scrollBehavior = currentScrollBehavior,
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    modifier = Modifier.weight(1f),
                    selected = selectedPage == ManagerPage.HOME,
                    onClick = { selectedPage = ManagerPage.HOME },
                    icon = Icons.Rounded.Cottage,
                    label = stringResource(R.string.home),
                )
                NavigationBarItem(
                    modifier = Modifier.weight(1f),
                    selected = selectedPage == ManagerPage.SERVICES,
                    onClick = { selectedPage = ManagerPage.SERVICES },
                    icon = Icons.Rounded.AccessibilityNew,
                    label = stringResource(R.string.services),
                )
                NavigationBarItem(
                    modifier = Modifier.weight(1f),
                    selected = selectedPage == ManagerPage.LOGS,
                    onClick = { selectedPage = ManagerPage.LOGS },
                    icon = Icons.AutoMirrored.Rounded.Article,
                    label = stringResource(R.string.logs),
                )
            }
        },
    ) { innerPadding ->
        when (selectedPage) {
            ManagerPage.HOME -> DashboardPage(
                innerPadding = innerPadding,
                state = state,
                controlsEnabled = controlsEnabled,
                onOpenServices = { selectedPage = ManagerPage.SERVICES },
                onAction = onAction,
                scrollBehavior = homeScrollBehavior,
            )

            ManagerPage.SERVICES -> ServicesPage(
                innerPadding = innerPadding,
                state = state,
                controlsEnabled = controlsEnabled,
                searchVisible = serviceSearchVisible,
                searchQuery = serviceSearchQuery,
                onSearchQueryChange = { serviceSearchQuery = it },
                filtersVisible = serviceFiltersVisible,
                filterMask = serviceFilterMask,
                onFiltersVisibleChange = { serviceFiltersVisible = it },
                onFilterMaskChange = { serviceFilterMask = it },
                onToggle = onToggle,
                onSetLocked = onSetLocked,
                onAction = onAction,
                scrollBehavior = servicesScrollBehavior,
            )

            ManagerPage.LOGS -> LogPage(
                innerPadding = innerPadding,
                entries = logEntries,
                scrollBehavior = logsScrollBehavior,
            )
        }
    }
}

@Composable
// Dashboard structure adapted from SukiSU Ultra's GPL-3.0 HomeMiuix at main@278d822.
private fun DashboardPage(
    innerPadding: PaddingValues,
    state: HomeState,
    controlsEnabled: Boolean,
    onOpenServices: () -> Unit,
    onAction: (StateAction) -> Unit,
    scrollBehavior: ScrollBehavior,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(horizontal = 12.dp),
        contentPadding = innerPadding,
    ) {
        item {
            Column(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.moduleNotice?.let { notice ->
                    ModuleNoticeCard(notice = notice, onAction = onAction)
                }
                StatusOverview(
                    state = state,
                    controlsEnabled = controlsEnabled,
                    onOpenServices = onOpenServices,
                    onAction = onAction,
                )
                ManagerInformationCard()
            }
        }
    }
}

@Composable
private fun ServicesPage(
    innerPadding: PaddingValues,
    state: HomeState,
    controlsEnabled: Boolean,
    searchVisible: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filtersVisible: Boolean,
    filterMask: Int,
    onFiltersVisibleChange: (Boolean) -> Unit,
    onFilterMaskChange: (Int) -> Unit,
    onToggle: (ServiceUiModel, Boolean) -> Unit,
    onSetLocked: (ServiceUiModel, Boolean) -> Unit,
    onAction: (StateAction) -> Unit,
    scrollBehavior: ScrollBehavior,
) {
    val normalizedQuery = searchQuery.trim()
    val filteredServices = remember(state.services, normalizedQuery, filterMask) {
        val appTypeFilter = filterMask and FILTER_APP_TYPE_MASK
        val stateFilter = filterMask and FILTER_SERVICE_STATE_MASK
        state.services.filter { service ->
            val matchesQuery = normalizedQuery.isEmpty() ||
                service.label.contains(normalizedQuery, ignoreCase = true) ||
                service.description.contains(normalizedQuery, ignoreCase = true) ||
                service.componentName.contains(normalizedQuery, ignoreCase = true)
            val matchesAppType = appTypeFilter == 0 ||
                appTypeFilter == FILTER_APP_TYPE_MASK ||
                (service.systemApp && appTypeFilter == FILTER_SYSTEM_APPS) ||
                (!service.systemApp && appTypeFilter == FILTER_USER_APPS)
            val matchesState = stateFilter == 0 ||
                stateFilter == FILTER_SERVICE_STATE_MASK ||
                (service.enabled && stateFilter == FILTER_ENABLED) ||
                (!service.enabled && stateFilter == FILTER_DISABLED)
            matchesQuery && matchesAppType && matchesState
        }
    }
    val summary = if (normalizedQuery.isEmpty() && filterMask == 0) {
        pluralStringResource(
            R.plurals.services_summary,
            state.services.size,
            state.services.size,
            state.services.count { it.enabled },
        )
    } else {
        pluralStringResource(
            R.plurals.search_results_summary,
            filteredServices.size,
            filteredServices.size,
            state.services.size,
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(horizontal = 12.dp),
        contentPadding = innerPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.moduleNotice?.let { notice ->
            item(key = "module-notice") {
                ModuleNoticeCard(notice = notice, onAction = onAction)
            }
        }

        if (searchVisible) {
            item(key = "service-search") {
                Box(modifier = Modifier.fillMaxWidth()) {
                    InputField(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onSearch = { },
                        expanded = true,
                        onExpandedChange = { },
                        label = stringResource(R.string.search_services_hint),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                modifier = Modifier.offset(x = 10.dp),
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = stringResource(R.string.clear_search),
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onFiltersVisibleChange(!filtersVisible) },
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FilterList,
                                        contentDescription = stringResource(R.string.filter_services),
                                        tint = if (filterMask != 0) {
                                            MiuixTheme.colorScheme.primary
                                        } else {
                                            MiuixTheme.colorScheme.onSurface
                                        },
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_services_hint),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 60.dp, end = 108.dp),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.body1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (filtersVisible) {
                item(key = "service-filters") {
                    ServiceFilterPanel(
                        filterMask = filterMask,
                        onFilterMaskChange = onFilterMaskChange,
                        onDone = { onFiltersVisibleChange(false) },
                    )
                }
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
                } else if (filteredServices.isEmpty()) {
                    item(key = "services-search-empty") {
                        ListStatusContent(
                            title = stringResource(R.string.no_search_results_title),
                            message = stringResource(R.string.no_search_results_message),
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
                    items(filteredServices, key = { it.componentName }) { service ->
                        ServiceRow(
                            service = service,
                            controlsEnabled = controlsEnabled,
                            onToggle = onToggle,
                            onSetLocked = onSetLocked,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceFilterPanel(
    filterMask: Int,
    onFilterMaskChange: (Int) -> Unit,
    onDone: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            FilterSectionTitle(stringResource(R.string.filter_application_type))
            ServiceFilterOption(
                title = stringResource(R.string.filter_system_apps),
                checked = filterMask and FILTER_SYSTEM_APPS != 0,
                onCheckedChange = {
                    onFilterMaskChange(filterMask xor FILTER_SYSTEM_APPS)
                },
            )
            ServiceFilterOption(
                title = stringResource(R.string.filter_user_apps),
                checked = filterMask and FILTER_USER_APPS != 0,
                onCheckedChange = {
                    onFilterMaskChange(filterMask xor FILTER_USER_APPS)
                },
            )
            Spacer(Modifier.height(6.dp))
            FilterSectionTitle(stringResource(R.string.filter_service_state))
            ServiceFilterOption(
                title = stringResource(R.string.filter_disabled),
                checked = filterMask and FILTER_DISABLED != 0,
                onCheckedChange = {
                    onFilterMaskChange(filterMask xor FILTER_DISABLED)
                },
            )
            ServiceFilterOption(
                title = stringResource(R.string.filter_enabled),
                checked = filterMask and FILTER_ENABLED != 0,
                onCheckedChange = {
                    onFilterMaskChange(filterMask xor FILTER_ENABLED)
                },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (filterMask != 0) {
                    TextButton(
                        text = stringResource(R.string.reset_filters),
                        onClick = { onFilterMaskChange(0) },
                    )
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(
                    text = stringResource(R.string.done),
                    onClick = onDone,
                )
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        style = MiuixTheme.textStyles.body2,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun ServiceFilterOption(
    title: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.body1,
        )
        Checkbox(
            state = ToggleableState(checked),
            onClick = onCheckedChange,
        )
    }
}

@Composable
private fun LogPage(
    innerPadding: PaddingValues,
    entries: List<ManagerLogEntry>,
    scrollBehavior: ScrollBehavior,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(horizontal = 12.dp),
        contentPadding = innerPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (entries.isEmpty()) {
            item(key = "logs-empty") {
                ListStatusContent(
                    title = stringResource(R.string.no_logs_title),
                    message = stringResource(R.string.no_logs_message),
                )
            }
        } else {
            items(entries.asReversed()) { entry ->
                LogEntryCard(entry)
            }
        }
    }
}

@Composable
private fun LogEntryCard(entry: ManagerLogEntry) {
    val timestamp = remember(entry.timestamp) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
            .format(Date(entry.timestamp))
    }
    val icon = when (entry.level) {
        ManagerLogLevel.INFO -> Icons.Rounded.Info
        ManagerLogLevel.SUCCESS -> Icons.Rounded.CheckCircleOutline
        ManagerLogLevel.ERROR -> Icons.Rounded.ErrorOutline
    }
    val iconColor = when (entry.level) {
        ManagerLogLevel.INFO -> MiuixTheme.colorScheme.primary
        ManagerLogLevel.SUCCESS -> if (isSystemInDarkTheme()) {
            Color(0xFF75D48A)
        } else {
            Color(0xFF36A852)
        }
        ManagerLogLevel.ERROR -> MiuixTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = iconColor,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.message,
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = timestamp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                )
            }
        }
    }
}

@Composable
private fun StatusOverview(
    state: HomeState,
    controlsEnabled: Boolean,
    onOpenServices: () -> Unit,
    onAction: (StateAction) -> Unit,
) {
    val darkTheme = isSystemInDarkTheme()
    val readyBackground = if (darkTheme) Color(0xFF1A3825) else Color(0xFFDFFAE4)
    val readyForeground = Color(0xFF36D167)
    val statusTitle = if (controlsEnabled) {
        stringResource(R.string.manager_running)
    } else {
        state.moduleNotice?.let { stringResource(it.title) }
            ?: stringResource(R.string.manager_unavailable)
    }
    val statusColors = CardDefaults.defaultColors(
        color = if (controlsEnabled) readyBackground else MiuixTheme.colorScheme.surface,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = statusColors,
            insideMargin = PaddingValues(0.dp),
            onClick = {
                if (controlsEnabled) {
                    onOpenServices()
                } else {
                    onAction(state.moduleNotice?.action ?: StateAction.RETRY_MODULE)
                }
            },
            showIndication = true,
            pressFeedbackType = PressFeedbackType.Tilt,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(38.dp, 45.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    if (!controlsEnabled && state.moduleNotice?.loading == true) {
                        CircularProgressIndicator(size = 72.dp)
                    } else {
                        Icon(
                            modifier = Modifier.size(170.dp),
                            imageVector = if (controlsEnabled) {
                                Icons.Rounded.CheckCircleOutline
                            } else {
                                Icons.Rounded.AccessibilityNew
                            },
                            tint = if (controlsEnabled) {
                                readyForeground
                            } else {
                                MiuixTheme.colorScheme.primary
                            },
                            contentDescription = null,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = statusTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(
                            R.string.module_version_short,
                            BuildConfig.BUNDLED_MODULE_VERSION_NAME,
                        ),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.services_total),
                value = state.services.size,
                onClick = onOpenServices,
            )
            Spacer(Modifier.height(12.dp))
            MetricCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.services_enabled),
                value = state.services.count { it.enabled },
                onClick = onOpenServices,
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    label: String,
    value: Int,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
        onClick = onClick,
        showIndication = true,
        pressFeedbackType = PressFeedbackType.Tilt,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = label,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = value.toString(),
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ManagerInformationCard() {
    val device = remember { "${Build.MANUFACTURER} ${Build.MODEL}".trim() }
    val androidVersion = remember { "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            InformationRow(
                title = stringResource(R.string.manager_version),
                value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            )
            InformationRow(
                title = stringResource(R.string.module_version),
                value = BuildConfig.BUNDLED_MODULE_VERSION_NAME,
            )
            InformationRow(
                title = stringResource(R.string.device_model),
                value = device,
            )
            InformationRow(
                title = stringResource(R.string.android_version),
                value = androidVersion,
                bottomPadding = 0.dp,
            )
        }
    }
}

@Composable
private fun InformationRow(
    title: String,
    value: String,
    bottomPadding: Dp = 24.dp,
) {
    Text(
        text = title,
        fontSize = MiuixTheme.textStyles.headline1.fontSize,
        fontWeight = FontWeight.Medium,
        color = MiuixTheme.colorScheme.onSurface,
    )
    Text(
        text = value,
        fontSize = MiuixTheme.textStyles.body2.fontSize,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(top = 2.dp, bottom = bottomPadding),
    )
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
    onSetLocked: (ServiceUiModel, Boolean) -> Unit,
) {
    val bitmap = remember(service.componentName, service.icon) { service.icon.toImageBitmap() }
    var expanded by rememberSaveable(service.componentName) { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        onClick = { expanded = !expanded },
        showIndication = true,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .offset(x = 7.dp)
                        .size(44.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.label,
                        modifier = Modifier.offset(x = 2.dp),
                        color = MiuixTheme.colorScheme.onSurface,
                        style = MiuixTheme.textStyles.headline1,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            if (expanded) R.string.tap_to_collapse else R.string.tap_for_description,
                        ),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                        maxLines = 1,
                    )
                }
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (service.pending) {
                        CircularProgressIndicator(size = 24.dp)
                    } else {
                        IconButton(
                            onClick = {
                                if (controlsEnabled) onSetLocked(service, !service.locked)
                            },
                        ) {
                            Icon(
                                imageVector = if (service.locked) {
                                    Icons.Rounded.Lock
                                } else {
                                    Icons.Rounded.LockOpen
                                },
                                contentDescription = stringResource(
                                    if (service.locked) R.string.unlock_service else R.string.lock_service,
                                ),
                                tint = if (service.locked) {
                                    MiuixTheme.colorScheme.primary
                                } else {
                                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                                },
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier.size(width = 58.dp, height = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Switch(
                        checked = service.enabled,
                        onCheckedChange = { onToggle(service, it) },
                        enabled = controlsEnabled && !service.pending && !service.locked,
                    )
                }
            }

            if (expanded) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = stringResource(R.string.service_description),
                        color = MiuixTheme.colorScheme.onSurface,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = service.description.ifBlank {
                            stringResource(R.string.no_service_description)
                        },
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.service_component, service.componentName),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                    )
                    if (service.locked) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.locked_service_description),
                            color = MiuixTheme.colorScheme.primary,
                            style = MiuixTheme.textStyles.body2,
                        )
                    }
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

private fun formatLogEntry(entry: ManagerLogEntry): String {
    val timestamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
        .format(Date(entry.timestamp))
    return "$timestamp [${entry.level.name}] ${entry.message}"
}
