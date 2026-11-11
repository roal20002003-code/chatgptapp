package ir.spaceweather.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ir.spaceweather.app.MonitorWorker
import ir.spaceweather.app.Notifier
import ir.spaceweather.app.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdminScreen(onLogout: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<MonitorResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var notify by remember { mutableStateOf(prefs.notifyEnabled) }
    var lastBg by remember { mutableStateOf(prefs.lastCheck) }

    fun setNotify(on: Boolean) {
        notify = on; prefs.notifyEnabled = on
        if (on) MonitorWorker.schedule(ctx) else MonitorWorker.cancel(ctx)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) setNotify(true)
    }
    suspend fun check() {
        busy = true
        result = runCatching { Monitor.run() }.getOrNull()
        lastBg = prefs.lastCheck
        busy = false
    }
    // تا وقتی صفحه باز است هر ۶۰ ثانیه بررسی می‌شود
    LaunchedEffect(Unit) { while (true) { check(); delay(60_000) } }

    val r = result
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            val ok = r?.healthy == true
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when { r == null -> "در حال بررسی…"; ok -> "✓ همه چیز سالم است"; else -> "⚠ مشکل شناسایی شد" },
                        color = when { r == null -> C.muted; ok -> C.ok; else -> C.storm },
                        fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f)
                    )
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = C.sun, strokeWidth = 2.dp)
                    else TextButton(onClick = { scope.launch { check() } }) { Text("بررسی اکنون", color = C.sun) }
                }
                r?.problems?.forEach { Text("• " + it.message, color = C.storm, fontSize = 13.sp) }
            }
        }
        item {
            Card {
                SectionTitle("زمان‌ها")
                Kv("ساعت گوشی (تهران)", Tm.show(r?.checkedAt))
                Kv("ساعت سرور", Tm.show(r?.serverTime))
                Kv("جدیدترین ثبت داده", Tm.show(r?.lastData) + "  (" + (r?.lastDataSource ?: "—") + ")")
                Kv("سن داده", Tm.span(r?.dataAgeMin),
                    if ((r?.dataAgeMin ?: 0L) > AdminConfig.STALE_MINUTES) C.storm else C.ok)
                Kv("اختلاف ساعت گوشی و سرور", Tm.span(r?.clockSkewMin),
                    if (kotlin.math.abs(r?.clockSkewMin ?: 0L) > AdminConfig.CLOCK_SKEW_MINUTES) C.storm else C.text)
                Text(
                    "زمان پارامترها به وقت " + (if (AdminConfig.PARAM_TIME_IS_UTC) "UTC در نظر گرفته و به تهران تبدیل شده" else "تهران در نظر گرفته شده"),
                    color = C.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        item {
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("نوتیف هشدار", color = C.text, fontWeight = FontWeight.Bold)
                        Text("بررسی خودکار هر ${Fa.digits("${AdminConfig.CHECK_EVERY_MINUTES}")} دقیقه در پس‌زمینه", color = C.muted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = notify,
                        onCheckedChange = { on ->
                            if (on && Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else setNotify(on)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = C.night, checkedTrackColor = C.sun),
                    )
                }
                Kv("آخرین بررسی پس‌زمینه", lastBg.ifEmpty { "—" })
                TextButton(onClick = { Notifier.post(ctx, "آزمایش نوتیف", "نوتیف پایش SpaceWeather کار می‌کند ✓") }) {
                    Text("ارسال نوتیف آزمایشی", color = C.sun)
                }
                Text(
                    "اگر نوتیف‌ها دیر می‌رسند، در تنظیمات گوشی «بهینه‌سازی باتری» را برای این برنامه خاموش کنید.",
                    color = C.muted, fontSize = 11.sp
                )
            }
        }
        if (r != null && r.params.isNotEmpty()) item {
            Card {
                SectionTitle("پارامترها", "حد هشدار: بیش از ${Fa.digits("${AdminConfig.STALE_MINUTES / 60}")} ساعت بدون بروزرسانی")
                r.params.forEach { p -> ParamRow(p) }
            }
        }
        item {
            Card {
                SectionTitle("وضعیت سرویس‌ها", "برای دیدن پاسخ خام روی هر مورد بزنید")
                r?.endpoints?.forEach { EndpointRow(it) } ?: Text("—", color = C.muted)
            }
        }
        if (r != null && r.monitorFields.isNotEmpty()) item {
            Card {
                SectionTitle("سایر فیلدهای monitoring")
                r.monitorFields.forEach { (k, v) -> Kv(k, v) }
            }
        }
        item {
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("خروج از بخش پایش", color = C.storm)
            }
        }
    }
}

@Composable
private fun Kv(k: String, v: String, color: Color = C.text) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(k, color = C.muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(v, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ParamRow(p: ParamCheck) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(if (p.stale) C.storm else C.ok))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(p.label, color = C.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(p.value, color = C.muted, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(if (p.time == null) "زمان نامعتبر" else Tm.span(p.ageMin) + " پیش",
                color = if (p.stale) C.storm else C.ok, fontSize = 12.sp)
            Text(Tm.show(p.time), color = C.muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun EndpointRow(e: EndpointCheck) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(if (e.ok) C.ok else C.storm))
            Spacer(Modifier.width(8.dp))
            Text(e.name, color = C.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(
                if (e.ok) Fa.ltr(Fa.digits("%d ms · %.1f KB".format(e.ms, e.bytes / 1024.0))) else "خطا",
                color = if (e.ok) C.muted else C.storm, fontSize = 12.sp
            )
        }
        if (!e.ok) Text(e.error ?: "", color = C.storm, fontSize = 12.sp)
        if (open) {
            Text(e.url, color = C.sky, fontSize = 11.sp, style = TextStyle(textDirection = TextDirection.Ltr))
            Text(
                (e.raw ?: "").take(2500),
                color = C.text, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                style = TextStyle(textDirection = TextDirection.Ltr),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(8.dp))
                    .background(C.night).padding(8.dp)
            )
        }
    }
}
