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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.example.bgtischedule.model.Schedule
import com.example.bgtischedule.model.ScheduleUiModel
import com.example.bgtischedule.service.Request
import com.example.bgtischedule.ui.components.DayHeader
import com.example.bgtischedule.ui.components.LessonCard
import com.example.bgtischedule.ui.components.ScheduleHeader
import com.example.bgtischedule.model.ScheduleUiModel.*
import com.example.bgtischedule.parser.ScheduleParser
import com.example.bgtischedule.ui.mapper.LessonMapper
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class AppSection(
    val title: String
) {
    Home("Домой"),
    Account("Аккаунт"),
    Behavior("Поведение"),
    Widget("Виджет"),
    Settings("Настройки")
}

private val TAG = "AppRoot"

@RequiresApi(Build.VERSION_CODES.O)
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

    var section by rememberSaveable { mutableStateOf(AppSection.Home) }
    var activeAccount by remember { mutableStateOf(credentialsStore.getActiveAccount()) }
    var scheduleRefreshTrigger by remember { mutableIntStateOf(0) }

    suspend fun fetchStudentInfo(): com.example.bgtischedule.model.StudentModel? {
        val html = api.getSchedulePage() ?: return null
        return scheduleParser.parse(html)?.studentFIO
    }

    suspend fun authenticateActiveAccount(): Result<Unit> {
        val creds = credentialsStore.getActiveAccount()
        authManager.setAuthError("Нет активного аккаунта")

        authManager.setAuthLoading()
        if (!api.login(creds?.login ?: "" , creds?.password ?: "")) {
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
        topBar = { CenterAlignedTopAppBar(title = { Text(section.title) }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = section == AppSection.Home,
                    onClick = { section = AppSection.Home },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Домой") },
                    label = { Text("Домой") }
                )
                NavigationBarItem(
                    selected = section == AppSection.Account,
                    onClick = { section = AppSection.Account },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Аккаунт") },
                    label = { Text("Аккаунт") }
                )
                NavigationBarItem(
                    selected = section == AppSection.Behavior,
                    onClick = { section = AppSection.Behavior },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Поведение") },
                    label = { Text("Поведение") }
                )
                NavigationBarItem(
                    selected = section == AppSection.Widget,
                    onClick = { section = AppSection.Widget },
                    icon = { Icon(Icons.Default.Widgets, contentDescription = "Виджет") },
                    label = { Text("Виджет") }
                )
                NavigationBarItem(
                    selected = section == AppSection.Settings,
                    onClick = { section = AppSection.Settings },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Настройки") },
                    label = { Text("Настройки") }
                )
            }
        }
    ) { padding ->
        when (section) {
            AppSection.Home -> HomeScreen(
                padding,
                activeAccount,
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

@Composable
private fun HomeScreen(
    padding: PaddingValues,
    activeAccount: Credentials?,
    authState: AuthStateManager.AuthState,
    request: Request,
    refreshTrigger: Int,
    onRetryAuth: suspend () -> Result<Unit>,
    onMessage: (String) -> Unit
) {

    var isLoading by remember { mutableStateOf(false) }
    var scheduleUi by remember { mutableStateOf<List<DayGroupUi>?>(null) }
    var weekRange by remember { mutableStateOf("") }
    var lastSyncTime by remember { mutableStateOf<Long?>(null) }

    val scope = rememberCoroutineScope()
    val group = authState.student?.group

    suspend fun loadSchedule() {
        val account = activeAccount
        if (account == null || !authState.isAuthenticated || group.isNullOrBlank()) {
            Log.w(TAG, "loadSchedule: account=$account, auth=${authState.isAuthenticated}, group=$group")
            return
        }

        isLoading = true
        try {
            Log.d(TAG, "loadSchedule: starting for group=$group")

            val result = request.syncSchedule(
                group = group,
                login = account.login,
                password = account.password
            )

            Log.d(TAG, "loadSchedule: result=$result")

            when (result) {
                is SyncResult.Success -> {
                    val dayGroups = LessonMapper.toDayGroups(result.schedule.lessons)
                    scheduleUi = dayGroups
                    weekRange = result.schedule.weekRange
                    lastSyncTime = System.currentTimeMillis()

                    if (result.changes.isNotEmpty()) {
                        val added = result.changes.count { it.type == SyncResult.ChangeType.ADDED }
                        val modified = result.changes.count { it.type == SyncResult.ChangeType.MODIFIED }
                        onMessage("Обновлено: +$added новых, $modified изменено")
                    }
                }
                is SyncResult.Cached -> {
                    scheduleUi = LessonMapper.toDayGroups(result.schedule.lessons)
                    weekRange = result.schedule.weekRange
                    onMessage("Показано сохранённое расписание")
                }
                is SyncResult.Error -> {
                    Log.e(TAG, "loadSchedule: Error: ${result.message}")
                    onMessage(result.message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadSchedule: Exception", e)
            onMessage("Ошибка: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // Загрузка при открытии домашней страницы, после авторизации или по триггеру
    LaunchedEffect(activeAccount?.id, authState.isAuthenticated, group, refreshTrigger) {
        Log.d(TAG, "LaunchedEffect: account=${activeAccount?.login}, auth=${authState.isAuthenticated}, group=$group")
        if (activeAccount != null && authState.isAuthenticated && !group.isNullOrBlank()) {
            loadSchedule()
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text("BGTI Schedule", style = MaterialTheme.typography.headlineSmall)



        if (activeAccount == null) {
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
        } else if (authState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Авторизация...",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else if (!authState.isAuthenticated) {
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
                        text = authState.error ?: "Проверьте логин и пароль в разделе «Аккаунт»",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        scope.launch {
                            onRetryAuth().onFailure {
                                onMessage(it.message ?: "Ошибка авторизации")
                            }
                        }
                    }) {
                        Text("Повторить вход")
                    }
                }
            }
        } else if(isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Загрузка расписания...",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else if (scheduleUi != null && weekRange.isNotEmpty()) {
            ScheduleCard(
                weekRange = weekRange,
                dayGroups = scheduleUi!!,
                onRefresh = {
                    scope.launch { loadSchedule() }
                }
            )

            // Инфо о последней синхронизации
            lastSyncTime?.let { time ->
                val formatted = Instant
                    .ofEpochMilli(time)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("HH:mm dd.MM"))

                Text(
                    text = "Обновлено: $formatted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .align(Alignment.End)
                )
            }
        } else {
            // Нет данных

            Log.i(TAG, "scheduleUi: $scheduleUi, weekRange: $weekRange")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
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
                    Text("Расписание не найдено",
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        scope.launch { loadSchedule() }
                        // Повторная попытка загрузки
                        // (логика дублируется с LaunchedEffect, можно вынести)
                    }) {
                        Text("Попробовать снова")
                    }
                }
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



@Composable
private fun ScheduleCard(
    weekRange: String,
    dayGroups: List<DayGroupUi>,
    currentTimeColor: Color = MaterialTheme.colorScheme.primary,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(LocalTime.now()) }

    // Обновляем время каждую минуту
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(60_000)
        while (true) {
            kotlinx.coroutines.delay(60_000)
            currentTime = LocalTime.now()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Заголовок с диапазоном и временем
        ScheduleHeader(
            weekRange = weekRange,
            currentTimeColor = currentTimeColor,
            onRefresh = onRefresh
        )

        // Список дней и занятий
        LazyColumn(
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            dayGroups.forEach { dayGroup ->
                // Заголовок дня
                item {
                    DayHeader(
                        dayName = dayGroup.dayName,
                        date = dayGroup.date
                    )
                }

                // Занятия дня
                items(dayGroup.lessons, key = { it.id }) { lesson ->
                    LessonCard(
                        lesson = lesson,
                        currentTime = currentTime,
                        currentTimeColor = currentTimeColor
                    )
                }
            }
        }
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
        )
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
        SettingsSwitch("Автообновление расписания", autoRefresh) {
            autoRefresh = it
            prefs.edit().putBoolean("behavior_auto_refresh", it).apply()
        }
        SettingsSwitch("Уведомлять перед парой", notifyBeforeLesson) {
            notifyBeforeLesson = it
            prefs.edit().putBoolean("behavior_notify", it).apply()
        }
        SettingsSwitch("Открывать последнюю неделю", openLastWeekOnStart) {
            openLastWeekOnStart = it
            prefs.edit().putBoolean("behavior_last_week", it).apply()
        }
    }
}

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
    }
}

@Composable
private fun SettingsScreen(
    padding: PaddingValues,
    onLogout: suspend () -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Общие настройки", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                scope.launch {
                    onLogout()
                    onMessage("Все аккаунты удалены")
                }
            }
        ) {
            Text("Выйти из всех аккаунтов")
        }
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
