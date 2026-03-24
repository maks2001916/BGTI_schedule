package com.example.bgtischedule.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bgtischedule.data.Credentials
import com.example.bgtischedule.data.SecureCredentialsStore
import com.example.bgtischedule.service.Request
import kotlinx.coroutines.launch

private enum class AppSection(
    val title: String
) {
    Home("Домой"),
    Account("Аккаунт"),
    Behavior("Поведение"),
    Widget("Виджет"),
    Settings("Настройки")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val context = LocalContext.current
    val settingsPrefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    val credentialsStore = remember { SecureCredentialsStore(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var section by rememberSaveable { mutableStateOf(AppSection.Home) }



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
            AppSection.Home -> HomeScreen(padding)
            AppSection.Account -> AccountScreen(
                padding = padding,
                credentialsStore = credentialsStore,
                onMessage = { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            )
            AppSection.Behavior -> BehaviorScreen(padding, settingsPrefs)
            AppSection.Widget -> WidgetScreen(padding, settingsPrefs)
            AppSection.Settings -> SettingsScreen(
                padding = padding,
                credentialsStore = credentialsStore,
                onMessage = { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            )
        }
    }
}

@Composable
private fun HomeScreen(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        val request = Request()



        Text("BGTI Schedule", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Главное окно. Здесь можно разместить текущую неделю, быстрый поиск и переход к расписанию.",
            style = MaterialTheme.typography.bodyMedium
        )
        InfoCard("Аккаунт", "Ввести и безопасно сохранить логин/пароль")
        InfoCard("Поведение", "Настроить автообновление, уведомления и т.п.")
        InfoCard("Виджет", "Параметры показа расписания в виджете")
        InfoCard("Настройки", "Общие параметры приложения")
    }
}

@Preview(showBackground = true, name = "Home - Light")
@Composable
fun HomeScreenPreviewLight() {
    Surface { HomeScreen(padding = androidx.compose.foundation.layout.PaddingValues(10.dp)) }}



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
    onMessage: (String) -> Unit
) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val data = credentialsStore.load()
        login = data.login
        password = data.password
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Вход с единым логином БГТИ", style = MaterialTheme.typography.titleLarge)
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
                onClick = {
                    credentialsStore.save(Credentials(login = login, password = password))
                    onMessage("Данные сохранены в защищенное хранилище")
                }
            ) { Text("Сохранить") }
            TextButton(
                onClick = {
                    val loaded = credentialsStore.load()
                    login = loaded.login
                    password = loaded.password
                    onMessage("Данные загружены")
                }
            ) { Text("Загрузить") }
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
    credentialsStore: SecureCredentialsStore,
    onMessage: (String) -> Unit
) {
    var dynamicColors by rememberSaveable { mutableStateOf(true) }
    var analyticsEnabled by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Общие настройки", style = MaterialTheme.typography.titleLarge)
        SettingsSwitch("Dynamic colors", dynamicColors) { dynamicColors = it }
        SettingsSwitch("Собирать анонимную аналитику", analyticsEnabled) { analyticsEnabled = it }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                credentialsStore.clear()
                onMessage("Логин и пароль удалены")
            }
        ) {
            Text("Очистить сохраненный аккаунт")
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
