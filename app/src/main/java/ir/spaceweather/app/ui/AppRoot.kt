package ir.spaceweather.app.ui

import ir.spaceweather.app.data.tr
import ir.spaceweather.app.data.L

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.spaceweather.app.HomeViewModel
import ir.spaceweather.app.MonitorWorker
import ir.spaceweather.app.data.AdminConfig
import ir.spaceweather.app.data.Prefs
import ir.spaceweather.app.data.UpdateInfo
import ir.spaceweather.app.data.Updater

enum class Tab { HOME, MESSAGE, ABOUT, SETTINGS, ADMIN }

@Composable
fun AppRoot(vm: HomeViewModel, openAdmin: Boolean, onLanguage: (Boolean) -> Unit = {}) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    var admin by remember { mutableStateOf(prefs.adminUnlocked) }
    var tab by rememberSaveable { mutableStateOf(if (openAdmin && prefs.adminUnlocked) Tab.ADMIN else Tab.HOME) }
    var showLogin by remember { mutableStateOf(false) }
    val s by vm.state.collectAsStateWithLifecycle()

    // بررسی نسخه هنگام باز شدن اپ؛ اگر سرور در دسترس نبود اپ عادی باز می‌شود
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var later by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!Updater.ENABLED) return@LaunchedEffect
        update = runCatching { Updater.check() }.getOrNull()
            ?.takeIf { Updater.isNewer(it.version, installedVersion(ctx)) }
    }
    update?.let { u ->
        if (u.force || !later) {
            Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                UpdateScreen(u, onLater = if (u.force) null else ({ later = true }))
            }
            return
        }
    }

    Column(Modifier.fillMaxSize().background(C.night).statusBarsPadding().navigationBarsPadding()) {
        // نگه‌داشتن طولانی روی عنوان = ورود به بخش پایش
        TopBar(s.loading, vm::refresh, onLongPress = { if (admin) tab = Tab.ADMIN else showLogin = true })
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.HOME -> HomeScreen(vm)
                Tab.MESSAGE -> MessageScreen()
                Tab.ABOUT -> AboutScreen()
                Tab.SETTINGS -> SettingsScreen(onLanguage)
                // بخش پایش فقط برای مدیر است و فارسی/راست‌به‌چپ می‌ماند
                Tab.ADMIN -> CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { AdminScreen(onLogout = {
                    prefs.adminUnlocked = false; prefs.notifyEnabled = false
                    MonitorWorker.cancel(ctx)
                    admin = false; tab = Tab.HOME
                }) }
            }
        }
        NavigationBar(containerColor = C.deep) {
            val colors = NavigationBarItemDefaults.colors(
                selectedTextColor = C.sun, unselectedTextColor = C.muted, indicatorColor = C.panel,
            )
            NavigationBarItem(tab == Tab.HOME, { tab = Tab.HOME },
                icon = { Text("☀", fontSize = 20.sp) }, label = { Text(tr("خانه", "Home")) }, colors = colors)
            NavigationBarItem(tab == Tab.MESSAGE, { tab = Tab.MESSAGE },
                icon = { Text("✉", fontSize = 20.sp) }, label = { Text(tr("ارسال پیام", "Message")) }, colors = colors)
            NavigationBarItem(tab == Tab.ABOUT, { tab = Tab.ABOUT },
                icon = { Text("ℹ", fontSize = 20.sp) }, label = { Text(tr("درباره ما", "About")) }, colors = colors)
            NavigationBarItem(tab == Tab.SETTINGS, { tab = Tab.SETTINGS },
                icon = { Text("🔔", fontSize = 18.sp) }, label = { Text(tr("تنظیمات", "Settings")) }, colors = colors)
            // تب پایش فقط بعد از ورود با رمز، و فقط روی همین گوشی دیده می‌شود
            if (admin) NavigationBarItem(tab == Tab.ADMIN, { tab = Tab.ADMIN },
                icon = { Text("⚙", fontSize = 20.sp) }, label = { Text(tr("پایش", "Monitor")) }, colors = colors)
        }
    }

    if (showLogin) LoginDialog(
        onDismiss = { showLogin = false },
        onSubmit = { pass ->
            val ok = AdminConfig.check(pass)
            if (ok) { prefs.adminUnlocked = true; admin = true; tab = Tab.ADMIN; showLogin = false }
            ok
        }
    )
}

@Composable
private fun LoginDialog(onDismiss: () -> Unit, onSubmit: (String) -> Boolean) {
    var pass by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.panel,
        title = { Text(tr("ورود به بخش پایش", "Monitoring sign-in"), color = C.text) },
        text = {
            OutlinedTextField(
                value = pass, onValueChange = { pass = it; wrong = false },
                label = { Text(tr("رمز", "Password")) }, singleLine = true, isError = wrong,
                supportingText = { if (wrong) Text(tr("رمز اشتباه است", "Wrong password"), color = C.storm) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
        },
        confirmButton = { TextButton(onClick = { wrong = !onSubmit(pass) }) { Text(tr("ورود", "Sign in"), color = C.sun) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("انصراف", "Cancel"), color = C.muted) } },
    )
}
