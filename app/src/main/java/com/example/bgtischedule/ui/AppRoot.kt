package com.example.bgtischedule.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.bgtischedule.api.UniversityApi
import com.example.bgtischedule.data.Credentials
import com.example.bgtischedule.data.SecureCredentialsStore
import com.example.bgtischedule.data.auth.AuthStateManager
import com.example.bgtischedule.data.model.SyncResult
import com.example.bgtischedule.datebase.AppDatabase
import com.example.bgtischedule.datebase.ScheduleRepository
import com.example.bgtischedule.service.Request
import com.example.bgtischedule.ui.components.DayHeader
import com.example.bgtischedule.ui.components.LessonCard
import androidx.compose.ui.text.style.TextOverflow
import com.example.bgtischedule.ui.components.SchedulePageHeader
import com.example.bgtischedule.model.ScheduleUiModel.*
import com.example.bgtischedule.parser.ScheduleParser
import com.example.bgtischedule.ui.mapper.LessonMapper
import com.example.bgtischedule.ui.update.UpdateAvailableDialog
import com.example.bgtischedule.update.DownloadState
import com.example.bgtischedule.update.UpdateCheckResult
import com.example.bgtischedule.update.UpdateInfo
import com.example.bgtischedule.update.UpdateManager
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.bgtischedule.model.StudentModel
import com.example.bgtischedule.ui.components.ContactLinksSection
import kotlinx.coroutines.delay
import java.io.File
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class AppSection(
    val title: String
) {
    Home("Рассписание"),
    Account("Аккаунт"),
    Behavior("Поведение"),
    Widget("Виджет"),
    Settings("Настройки")
}

private val TAG = "AppRoot"



@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    scheduleRepository: ScheduleRepository? = null,
    parser: ScheduleParser? = null
) {

    val context = LocalContext.current
    val credentialsStore = remember { SecureCredentialsStore(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val authManager = remember { AuthStateManager(credentialsStore) }
    val authState by authManager.authState.collectAsState()
    val api = remember { UniversityApi() }
    val scheduleParser = remember { parser ?: ScheduleParser() }

    val repository = remember {
        scheduleRepository ?: run {
            val db = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "schedule.db"
            ).build()
            ScheduleRepository(db.getScheduleDao(), authManager)
        }
    }

    val request = remember { Request(repository, scheduleParser, api) }
    val updateManager = remember { UpdateManager() }
    val downloadState by updateManager.downloadState.collectAsState()
    var pendingUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    @RequiresApi(Build.VERSION_CODES.P)
    suspend fun runUpdateCheck(showNoUpdateMessage: Boolean) {
        isCheckingUpdate = true
        try {
            when (val result = updateManager.checkForUpdate(context)) {
                is UpdateCheckResult.Available -> pendingUpdate = result.info
                is UpdateCheckResult.UpToDate -> {
                    if (showNoUpdateMessage) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Установлена последняя версия")
                        }
                    }
                }
                is UpdateCheckResult.Error -> {
                    if (showNoUpdateMessage) {
                        scope.launch { snackbarHostState.showSnackbar(result.message) }
                    } else {
                        Log.w(TAG, "Auto update check: ${result.message}")
                    }
                }
            }
        } finally {
            isCheckingUpdate = false
        }
    }

    // Автопроверка обновлений с GitHub (раз в 24 ч)
    LaunchedEffect(Unit) {
        if (UpdateManager.shouldRunAutoCheck(context)) {
            runUpdateCheck(showNoUpdateMessage = false)
            UpdateManager.markAutoCheckDone(context)
        }
    }

    pendingUpdate?.let { updateInfo ->
        UpdateAvailableDialog(
            info = updateInfo,
            downloadState = downloadState,
            onDismiss = {
                pendingUpdate = null
                updateManager.resetDownloadState()
            },
            onDownloadAndInstall = {
                scope.launch {
                    if (downloadState is DownloadState.Ready) {
                        val path = (downloadState as DownloadState.Ready).apkPath
                        updateManager.installDownloadedApk(context, File(path))
                            .onFailure {
                                snackbarHostState.showSnackbar(
                                    it.message ?: "Не удалось открыть установщик"
                                )
                            }
                        return@launch
                    }
                    val fileResult = updateManager.downloadAndPrepareInstall(context, updateInfo)
                    fileResult.onSuccess { file ->
                        updateManager.installDownloadedApk(context, file)
                            .onFailure {
                                snackbarHostState.showSnackbar(
                                    it.message ?: "Разрешите установку из неизвестных источников"
                                )
                            }
                    }.onFailure {
                        snackbarHostState.showSnackbar(it.message ?: "Ошибка загрузки")
                    }
                }
            },
            onOpenReleasePage = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.releasePageUrl))
                context.startActivity(intent)
            }
        )
    }

    var section by rememberSaveable { mutableStateOf(AppSection.Home) }
    var activeAccount by remember { mutableStateOf(credentialsStore.getActiveAccount()) }
    var scheduleRefreshTrigger by remember { mutableIntStateOf(0) }

    suspend fun fetchStudentInfo(): StudentModel? {
        val html = api.getSchedulePage() ?: return null
        return scheduleParser.parse(html)?.studentFIO
    }

    suspend fun authenticateActiveAccount(): Result<Unit> {
        val creds = credentialsStore.getActiveAccount()
            ?: run {
                authManager.setAuthError("Нет активного аккаунта")
                return Result.failure(Exception("Нет активного аккаунта"))
            }
        authManager.setAuthLoading()
        if (!api.login(creds.login, creds.password)) {
            authManager.setAuthError("Не удалось авторизоваться на сервере. Проверьте логин и пароль.")
            return Result.failure(Exception("Не удалось авторизоваться на сервере"))
        }
        return authManager.authenticateActiveAccount { fetchStudentInfo() }
    }

    // Восстановление сессии при старте
    LaunchedEffect(Unit) {
        if (authManager.checkSavedCredentials()) {
            authenticateActiveAccount().onFailure {
                Log.w(TAG, "Session restore failed: ${it.message}")
            }
        }
        activeAccount = credentialsStore.getActiveAccount()
    }

    LaunchedEffect(section) {
        activeAccount = credentialsStore.getActiveAccount()
    }

    Scaffold(
        topBar = {
            if (section != AppSection.Home) {
                CenterAlignedTopAppBar(title = { Text(section.title) })
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = section == AppSection.Home,
                    onClick = { section = AppSection.Home },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = "Расписание") },
                    label = {
                        Text(
                            text = "Расписание",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = section == AppSection.Account,
                    onClick = { section = AppSection.Account },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Аккаунт") },
                    label = {
                        Text(
                            text = "Аккаунт",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = section == AppSection.Behavior,
                    onClick = { section = AppSection.Behavior },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Поведение") },
                    label = {
                        Text(
                            text = "Повед.",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = section == AppSection.Widget,
                    onClick = { section = AppSection.Widget },
                    icon = { Icon(Icons.Default.Widgets, contentDescription = "Виджет") },
                    label = {
                        Text(
                            text = "Виджет",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = section == AppSection.Settings,
                    onClick = { section = AppSection.Settings },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Настройки") },
                    label = {
                        Text(
                            text = "Настр.",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    alwaysShowLabel = false
                )
            }
        }
    ) { padding ->
        when (section) {
            AppSection.Home -> HomeScreen(
                padding = padding,
                context = context,
                activeAccount = activeAccount,
                authState = authState,
                request = request,
                refreshTrigger = scheduleRefreshTrigger,
                onRetryAuth = { authenticateActiveAccount() },
                onMessage = { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                })
            AppSection.Account -> AccountScreen(
                padding = padding,
                credentialsStore = credentialsStore,
                authManager = authManager,
                authState = authState,
                onAuthenticate = { login, password ->
                    authManager.setAuthLoading()
                    if (!api.login(login, password)) {
                        authManager.setAuthError("Не удалось авторизоваться на сервере. Проверьте логин и пароль.")
                        Result.failure(Exception("Не удалось авторизоваться на сервере"))
                    } else {
                        authManager.loginNewAccount(login, password) { fetchStudentInfo() }
                    }
                },
                onSwitchAccount = { accountId ->
                    credentialsStore.switchAccount(accountId)
                    activeAccount = credentialsStore.getActiveAccount()
                    authenticateActiveAccount()
                },
                onAccountChanged = {
                    activeAccount = credentialsStore.getActiveAccount()
                    scope.launch {
                        if (activeAccount != null) {
                            authenticateActiveAccount()
                        } else {
                            authManager.clearSession()
                        }
                        scheduleRefreshTrigger++
                    }
                },
                onMessage = { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            )
            AppSection.Behavior -> BehaviorScreen(padding, context.getSharedPreferences("app_swttings", Context.MODE_PRIVATE))
            AppSection.Widget -> WidgetScreen(padding, context.getSharedPreferences("app_settings",
                Context.MODE_PRIVATE))
            AppSection.Settings -> SettingsScreen(
                padding = padding,
                context = context,
                updateManager = updateManager,
                isCheckingUpdate = isCheckingUpdate,
                onCheckUpdate = {
                    scope.launch { runUpdateCheck(showNoUpdateMessage = true) }
                },
                onLogout = {
                    scope.launch {
                        authManager.logout()
                        activeAccount = null
                        scheduleRefreshTrigger++
                    }
                },
                onMessage = { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            )
        }
    }
}

private fun loadLocalHtmlFile(context: Context, fileName: String): String? {
    return try {
        val inputStream = context.assets.open(fileName)
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val stringBuilder = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            stringBuilder.append(line).append("\n")
        }

        reader.close()
        inputStream.close()

        stringBuilder.toString()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun HomeScreen(
    padding: PaddingValues,
    context: Context,
    activeAccount: Credentials?,
    authState: AuthStateManager.AuthState,
    request: Request,
    refreshTrigger: Int,
    onRetryAuth: suspend () -> Result<Unit>,
    onMessage: (String) -> Unit
) {
    val prefs = remember {
        context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
    }

    var isScheduleLoading by remember { mutableStateOf(false) }
    var scheduleUi by remember { mutableStateOf<List<DayGroupUi>?>(null) }
    var weekRange by remember { mutableStateOf("") }
    var lastSyncTime by remember { mutableStateOf<Long?>(null) }
    var lastKnownGroup by remember(activeAccount?.id) {
        mutableStateOf(
            activeAccount?.let { prefs.getString(groupPrefsKey(it.id), null) }
        )
    }

    val scope = rememberCoroutineScope()
    var weekOffset by rememberSaveable { mutableStateOf(0) }  // 0 = текущая, 1 = следующая


    fun resolveGroup(): String? =
        authState.student?.group?.takeIf { it.isNotBlank() }
            ?: lastKnownGroup?.takeIf { it.isNotBlank() }

    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    fun applyScheduleResult(result: SyncResult) {
        when (result) {
            is SyncResult.Success -> {
                scheduleUi = LessonMapper.toDayGroups(result.schedule.lessons)
                weekRange = result.schedule.weekRange
                lastSyncTime = System.currentTimeMillis()
                val group = result.schedule.studentFIO.group.ifBlank { resolveGroup().orEmpty() }
                if (group.isNotBlank() && activeAccount != null) {
                    lastKnownGroup = group
                    prefs.edit().putString(groupPrefsKey(activeAccount.id), group).apply()
                }
            }
            is SyncResult.Cached -> {
                scheduleUi = LessonMapper.toDayGroups(result.schedule.lessons)
                weekRange = result.schedule.weekRange
                val group = result.schedule.studentFIO.group.ifBlank { resolveGroup().orEmpty() }
                if (group.isNotBlank() && activeAccount != null) {
                    lastKnownGroup = group
                    prefs.edit().putString(groupPrefsKey(activeAccount.id), group).apply()
                }
            }
            is SyncResult.Error -> {
                Log.e(TAG, "schedule: ${result.message}")
            }
        }
    }

    suspend fun loadCachedScheduleSilent(group: String) {
        when (val result = request.loadCachedSchedule(group)) {
            is SyncResult.Cached, is SyncResult.Success -> applyScheduleResult(result)
            else -> Unit
        }
    }

    suspend fun loadSchedule() {
        if (isScheduleLoading) return
        val account = activeAccount ?: return
        val group = resolveGroup() ?: return
        val weekStart = LocalDate.now()
            .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            .plusWeeks(weekOffset.toLong())

        val hadData = scheduleUi != null
        if (!hadData) isScheduleLoading = true

        // Зпгрузка расписания из БД
        val cached = request.loadCachedWeek(group, weekStart)
        if (cached is SyncResult.Cached) {
            applyScheduleResult(cached)
            isScheduleLoading = false   // данные уже на экране
        }

        // проверка сервера
        try {
            Log.d(TAG, "loadSchedule: group=$group")
            //val result = request.syncSchedule(group, activeAccount.login, activeAccount.password, weekOffset)
            val result = request.refreshWeek(group, account.login, account.password, weekOffset)
            Log.d(TAG, "loadSchedule: result=$result")
            when (result) {
                is SyncResult.Success -> {
                    applyScheduleResult(result)
                    if (result.changes.isNotEmpty()) {
                        val added = result.changes.count { it.type == SyncResult.ChangeType.ADDED }
                        val modified = result.changes.count { it.type == SyncResult.ChangeType.MODIFIED }
                        onMessage("Обновлено: +$added новых, $modified изменено")
                    }
                }
                else -> {
                    if (hadData) onMessage("Показано сохранённое расписание")
                    else onMessage("Нет данных")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadSchedule: Exception", e)
            if (!hadData) onMessage("Ошибка: ${e.message}")
        } finally {
            isScheduleLoading = false
        }
    }

// Единая точка запуска загрузки
    LaunchedEffect(activeAccount?.id, authState.student?.group, lastKnownGroup, refreshTrigger, weekOffset) {
        // 1. Восстановление группы из prefs при смене аккаунта
        lastKnownGroup = activeAccount?.let { prefs.getString(groupPrefsKey(it.id), null) }

        // 2. Мгновенно показать кэш (если есть)
        val groupToLoad = resolveGroup()
        if (groupToLoad != null) {
            loadCachedScheduleSilent(groupToLoad)
        }

        // 3. Фоном проверить сервер
        if (activeAccount != null && groupToLoad != null) {
            loadSchedule()
        }
    }
    val hasSchedule = scheduleUi != null && weekRange.isNotEmpty()
    val isBusy = authState.isLoading || isScheduleLoading

    val statusMessage = when {
        authState.isLoading -> "Авторизация..."
        isScheduleLoading -> "Обновление расписания..."
        !authState.isAuthenticated && authState.error != null && hasSchedule ->
            authState.error!!
        else -> ""
    }

    val showStatusBar = isBusy || (statusMessage.isNotEmpty() && hasSchedule)
    val showStatusProgress = isBusy
    val showAuthErrorCard = !authState.isAuthenticated && !isBusy && !hasSchedule && activeAccount != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            activeAccount == null -> AuthRequiredCard()
            else -> HomeScheduleLayout(
                screenTitle = AppSection.Home.title,
                showStatusBar = showStatusBar,
                statusMessage = statusMessage,
                showProgress = showStatusProgress,
                isStatusError = !isBusy && !authState.isAuthenticated && authState.error != null,
                hasSchedule = hasSchedule,
                weekRange = weekRange,
                scheduleUi = scheduleUi,
                lastSyncTime = lastSyncTime,
                showAuthErrorCard = showAuthErrorCard,
                authError = authState.error,
                showEmptyPlaceholder = authState.isAuthenticated && !isBusy && !hasSchedule,
                onRefresh = { scope.launch { loadSchedule() } },
                onRetryAuth = {
                    scope.launch {
                        onRetryAuth().onFailure {
                            onMessage(it.message ?: "Ошибка авторизации")
                        }
                    }
                },
                onRetryLoad = { scope.launch { loadSchedule() } },
                onShowPreviousWeek = { if (weekOffset > 0) weekOffset-- },
                onShowNextWeek = { if (weekOffset < 4) weekOffset++ }

            )
        }
    }
}

private fun groupPrefsKey(accountId: String) = "last_group_$accountId"

/**
 * Единая раскладка на всех этапах: строка статуса → расписание → (опционально) пустое/ошибка.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun HomeScheduleLayout(
    screenTitle: String,
    showStatusBar: Boolean,
    statusMessage: String,
    showProgress: Boolean,
    isStatusError: Boolean,
    hasSchedule: Boolean,
    weekRange: String,
    scheduleUi: List<DayGroupUi>?,
    lastSyncTime: Long?,
    showAuthErrorCard: Boolean,
    authError: String?,
    showEmptyPlaceholder: Boolean,
    onRefresh: () -> Unit,
    onRetryAuth: () -> Unit,
    onRetryLoad: () -> Unit,
    onShowPreviousWeek: () -> Unit,
    onShowNextWeek: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SchedulePageHeader(
            title = screenTitle,
            weekRange = weekRange,
            onRefresh = onRefresh,
            onShowNextWeek = onShowNextWeek,
            onShowPreviousWeek = onShowPreviousWeek
        )

        if (showStatusBar && statusMessage.isNotEmpty()) {
            ScheduleStatusBar(
                message = statusMessage,
                showProgress = showProgress,
                isError = isStatusError
            )
        }

        if (hasSchedule && scheduleUi != null) {
            ScheduleLessonsList(
                modifier = Modifier.weight(1f),
                dayGroups = scheduleUi
            )

            lastSyncTime?.let { time ->
                val formatted = Instant.ofEpochMilli(time)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("HH:mm dd.MM"))
                Text(
                    text = "Обновлено: $formatted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = TextAlign.End
                )
            }
        }

        if (showAuthErrorCard) {
            AuthErrorCard(error = authError, onRetry = onRetryAuth)
        }

        if (showEmptyPlaceholder) {
            EmptySchedulePlaceholder(
                modifier = Modifier.weight(1f),
                onRetry = onRetryLoad
            )
        }
    }
}

@Composable
private fun ScheduleStatusBar(
    message: String,
    showProgress: Boolean = true,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun AuthRequiredCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Требуется авторизация",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Перейдите в раздел «Аккаунт» для входа",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AuthErrorCard(
    error: String?,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Не удалось авторизоваться",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = error ?: "Проверьте логин и пароль в разделе «Аккаунт»",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text("Повторить вход")
            }
        }
    }
}

@Composable
private fun EmptySchedulePlaceholder(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Расписание не найдено",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRetry) {
                Text("Попробовать снова")
            }
        }
    }
}

@Preview(showBackground = true, name = "Home - Light")
@Composable
fun HomeScreenPreviewLight() {
    Surface {
     //   HomeScreen(padding = PaddingValues(10.dp))
    }
}



@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ScheduleLessonsList(
    dayGroups: List<DayGroupUi>,
    currentTimeColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            currentTime = LocalTime.now()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        dayGroups.forEach { dayGroup ->
            item(key = "day_${dayGroup.dayName}_${dayGroup.date}") {
                DayHeader(
                    dayName = dayGroup.dayName,
                    date = dayGroup.date
                )
            }

            items(
                items = dayGroup.lessons,
                key = { it.id }
            ) { lesson ->
                LessonCard(
                    lesson = lesson,
                    currentTime = currentTime,
                    currentTimeColor = currentTimeColor
                )
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    weekRange: String,
    dayGroups: List<DayGroupUi>,
    currentTimeColor: Color = MaterialTheme.colorScheme.primary,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier,
    onShowNextWeek: () -> Unit,
    onShowPreviousWeek: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        SchedulePageHeader(
            title = "Расписание",
            weekRange = weekRange,
            onRefresh = onRefresh,
            currentTimeColor = currentTimeColor,
            modifier = modifier,
            onShowNextWeek = onShowNextWeek,
            onShowPreviousWeek = onShowPreviousWeek
        )
        ScheduleLessonsList(
            dayGroups = dayGroups,
            currentTimeColor = currentTimeColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true, name = "Home - Light")
@Composable
private fun ScheduleCardPreview() {
    ScheduleCard(
        weekRange = "13–19 апреля",
        dayGroups = listOf(
            DayGroupUi(
                dayName = "Понедельник",
                date = "13 апреля",
                lessons = listOf(
                    LessonUi(
                        id = "lesson_1",
                        lessonNumber = 1,
                        startTime = "08:30",
                        endTime = "10:00",
                        subject = "Математический анализ",
                        type = "Лекция",
                        teacher = "Иванов И.И.",
                        classroom = "304",
                        building = "2",
                        topic = "Пределы и непрерывность",
                        color = LessonColors.getColorForLesson("lesson_1"),
                        floorPlan = FloorPlanUi(
                            building = "2 корпус",
                            floor = 3,
                            roomNumber = "304"
                        )
                    ),
                    LessonUi(
                        id = "lesson_2",
                        lessonNumber = 2,
                        startTime = "10:10",
                        endTime = "11:40",
                        subject = "Программирование",
                        type = "Практика",
                        teacher = "Петров П.П.",
                        classroom = "215",
                        building = "2",
                        topic = "Работа с коллекциями",
                        color = LessonColors.getColorForLesson("lesson_2"),
                        floorPlan = FloorPlanUi(
                            building = "2 корпус",
                            floor = 2,
                            roomNumber = "215"
                        )
                    )
                )
            )
        ),
        onShowNextWeek = {},
        onShowPreviousWeek = {}
    )
}

@Composable
private fun InfoCard(title: String, text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AccountScreen(
    padding: PaddingValues,
    credentialsStore: SecureCredentialsStore,
    authManager: AuthStateManager,
    authState: AuthStateManager.AuthState,
    onAuthenticate: suspend (String, String) -> Result<Unit>,
    onSwitchAccount: suspend (String) -> Result<Unit>,
    onAccountChanged: () -> Unit,
    onMessage: (String) -> Unit
) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    var accounts by remember { mutableStateOf(credentialsStore.getAllAccounts()) }
    val activeAccount = credentialsStore.getActiveAccount()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Вход с логином БГТИ", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Логин") },
            singleLine = true
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !isSubmitting && !authState.isLoading,
                onClick = {
                    if (login.isBlank() || password.isBlank()) {
                        onMessage("Введите логин и пароль")
                        return@Button
                    }
                    scope.launch {
                        isSubmitting = true
                        try {
                            val result = onAuthenticate(login, password)
                            result.onSuccess {
                                accounts = credentialsStore.getAllAccounts()
                                onAccountChanged()
                                onMessage("Аккаунт добавлен, расписание загружается")
                                login = ""
                                password = ""
                            }.onFailure {
                                onMessage("Ошибка: ${it.message ?: "Неизвестная ошибка"}")
                            }
                        } finally {
                            isSubmitting = false
                        }
                    }
                }
            ) { Text(if (isSubmitting) "Вход..." else "Войти") }
        }

        if (accounts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Аккаунты", style = MaterialTheme.typography.titleMedium)

            accounts.forEach { account ->
                AccountCard(
                    account = account,
                    isActive = account.id == activeAccount?.id,
                    group = if (account.id == activeAccount?.id) authState.student?.group else null,
                    onSwitch = {
                        scope.launch {
                            val result = onSwitchAccount(account.id)
                            result.onSuccess {
                                accounts = credentialsStore.getAllAccounts()
                                onAccountChanged()
                                onMessage("Переключено на ${account.login}")
                            }.onFailure {
                                onMessage("Ошибка: ${it.message}")
                            }
                        }
                    },
                    onRemove = {
                        credentialsStore.removeAccount(account.id)
                        accounts = credentialsStore.getAllAccounts()
                        onAccountChanged()
                        onMessage("Аккаунт удалён")
                    }
                )
            }
        }
    }
}


@Composable
private fun AccountCard(
    account: Credentials,
    isActive: Boolean,
    group: String?,
    onSwitch: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onSwitch
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isActive) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Активен",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text =  account.login ,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = group?.let { "Группа: $it" } ?: "Группа",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun BehaviorScreen(padding: PaddingValues, prefs: SharedPreferences) {
    var autoRefresh by rememberSaveable { mutableStateOf(prefs.getBoolean("behavior_auto_refresh", true)) }
    var notifyBeforeLesson by rememberSaveable { mutableStateOf(prefs.getBoolean("behavior_notify", false)) }
    var openLastWeekOnStart by rememberSaveable { mutableStateOf(prefs.getBoolean("behavior_last_week", true)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Настройка поведения", style = MaterialTheme.typography.titleLarge)
        /*
        SettingsSwitch("Автообновление расписания", autoRefresh) {
            autoRefresh = it
            prefs.edit().putBoolean("behavior_auto_refresh", it)
        }
        SettingsSwitch("Уведомлять перед парой", notifyBeforeLesson) {
            notifyBeforeLesson = it
            prefs.edit().putBoolean("behavior_notify", it)
        }
        SettingsSwitch("Открывать последнюю неделю", openLastWeekOnStart) {
            openLastWeekOnStart = it
            prefs.edit().putBoolean("behavior_last_week", it)
        }
        */
    }
}

@RequiresApi(Build.VERSION_CODES.GINGERBREAD)
@Composable
private fun WidgetScreen(padding: PaddingValues, prefs: SharedPreferences) {
    var showTeacher by rememberSaveable { mutableStateOf(prefs.getBoolean("widget_teacher", true)) }
    var showClassroom by rememberSaveable { mutableStateOf(prefs.getBoolean("widget_classroom", true)) }
    var compactMode by rememberSaveable { mutableStateOf(prefs.getBoolean("widget_compact", false)) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Настройка виджета", style = MaterialTheme.typography.titleLarge)
        /*
        SettingsSwitch("Показывать преподавателя", showTeacher) {
            showTeacher = it
            prefs.edit().putBoolean("widget_teacher", it).apply()
        }
        SettingsSwitch("Показывать аудиторию", showClassroom) {
            showClassroom = it
            prefs.edit().putBoolean("widget_classroom", it).apply()
        }
        SettingsSwitch("Компактный режим", compactMode) {
            compactMode = it
            prefs.edit().putBoolean("widget_compact", it).apply()
        }
        */
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
private fun SettingsScreen(
    padding: PaddingValues,
    context: Context,
    updateManager: UpdateManager,
    isCheckingUpdate: Boolean,
    onCheckUpdate: () -> Unit,
    onLogout: suspend () -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val prefs = remember {
        context.getSharedPreferences(UpdateManager.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var autoUpdateCheck by rememberSaveable {
        mutableStateOf(prefs.getBoolean(UpdateManager.KEY_AUTO_UPDATE_CHECK, true))
    }
    val versionName = remember { updateManager.currentVersionName(context) }
    val versionCode = remember { updateManager.currentVersionCode(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Общие настройки", style = MaterialTheme.typography.titleLarge)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Версия приложения",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$versionName (код $versionCode)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text("Обновления с GitHub", style = MaterialTheme.typography.titleMedium)

        SettingsSwitch(
            title = "Проверять обновления автоматически",
            checked = autoUpdateCheck
        ) {
            autoUpdateCheck = it
            prefs.edit().putBoolean(UpdateManager.KEY_AUTO_UPDATE_CHECK, it)
        }

        Button(
            onClick = onCheckUpdate,
            enabled = !isCheckingUpdate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCheckingUpdate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isCheckingUpdate) "Проверка…" else "Проверить обновления")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                scope.launch {
                    onLogout()
                    onMessage("Все аккаунты удалены")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Выйти из всех аккаунтов")
        }

        Spacer(modifier = Modifier.height(8.dp))

        ContactLinksSection(onMessage = onMessage)
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

