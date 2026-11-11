package ir.spaceweather.app.ui

import ir.spaceweather.app.data.tr
import ir.spaceweather.app.data.L

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ir.spaceweather.app.Notifier
import ir.spaceweather.app.StormWorker
import ir.spaceweather.app.data.Fa
import ir.spaceweather.app.data.Prefs
import ir.spaceweather.app.data.StormLevels
import ir.spaceweather.app.data.Storms

private data class Scale(val letter: String, val title: String, val desc: String)

private val SCALES get() = listOf(
    Scale("R", tr("طوفان رادیویی (R)", "Radio blackout (R)"), tr("شراره‌های اشعه ایکس از کلاس M1 به بالا — اختلال در ارتباطات رادیویی", "X-ray flares of class M1 or above — radio communication disruption")),
    Scale("G", tr("طوفان ژئومغناطیسی (G)", "Geomagnetic storm (G)"), tr("شاخص Kp از ۵ به بالا — اثر بر برق، GPS و شفق قطبی", "Kp index 5 or above — effects on power grids, GPS and aurora")),
    Scale("S", tr("طوفان تابشی (S)", "Solar radiation storm (S)"), tr("پروتون‌های پرانرژی از ۱۰ pfu به بالا — اثر بر ماهواره‌ها", "Energetic protons of 10 pfu or above — effects on satellites")),
)

@Composable
fun SettingsScreen(onLanguage: (Boolean) -> Unit = {}) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    var on by remember { mutableStateOf(prefs.stormNotify) }
    val scaleOn = remember { mutableStateMapOf<String, Boolean>().apply { SCALES.forEach { put(it.letter, prefs.scaleOn(it.letter)) } } }
    var now by remember { mutableStateOf<StormLevels?>(null) }
    var denied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { now = runCatching { Storms.fetch() }.getOrNull() }

    fun enable(v: Boolean) {
        on = v; prefs.stormNotify = v
        if (v) {
            // سطح فعلی را ثبت کن تا برای طوفانی که همین الان جریان دارد هم یک بار اعلان بیاید
            now?.let { StormWorker.notifyIfRaised(ctx, prefs, it) }
            StormWorker.schedule(ctx)
        } else StormWorker.cancel(ctx)
    }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        denied = !ok
        if (ok) enable(true)
    }
    val switchColors = SwitchDefaults.colors(checkedThumbColor = C.night, checkedTrackColor = C.sun)

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ---------- زبان / Language ----------
        item {
            Card {
                Text("زبان · Language", color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(false to "فارسی", true to "English").forEach { (en, label) ->
                        val on = L.en == en
                        Text(
                            label, color = if (on) C.night else C.text, fontSize = 14.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                                .background(if (on) C.sun else C.deep)
                                .clickable { if (!on) onLanguage(en) }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
        item {
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("اعلان طوفان‌های هواشناسی فضایی", "Space weather storm notifications"), color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(tr("وقتی هر یک از مقیاس‌های R، G یا S از صفر بیشتر شود خبرتان می‌کنیم.", "We notify you when any of the R, G or S scales rises above zero."), color = C.muted, fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = on, colors = switchColors,
                        onCheckedChange = { v ->
                            if (v && Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) perm.launch(Manifest.permission.POST_NOTIFICATIONS) else enable(v)
                        },
                    )
                }
                if (denied) Text(
                    tr("اجازهٔ اعلان داده نشد. از تنظیمات گوشی ← برنامه‌ها ← SpaceWeather ← اعلان‌ها آن را روشن کنید.", "Notification permission was denied. Enable it in phone Settings → Apps → SpaceWeather → Notifications."),
                    color = C.storm, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        item {
            Card {
                SectionTitle(tr("کدام طوفان‌ها؟", "Which storms?"))
                SCALES.forEachIndexed { i, s ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val lv = when (s.letter) { "R" -> now?.r; "G" -> now?.g; else -> now?.s }
                        Text(
                            s.letter + Fa.digits("${lv ?: 0}"), color = C.night, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(if (lv == null) C.line else LEVEL_COLORS[lv]).padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.title, color = if (on) C.text else C.muted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(s.desc, color = C.muted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = scaleOn[s.letter] == true, enabled = on, colors = switchColors,
                            onCheckedChange = { v -> scaleOn[s.letter] = v; prefs.setScaleOn(s.letter, v) },
                        )
                    }
                }
                Text(
                    if (now == null) tr("در حال دریافت وضعیت فعلی…", "Loading current conditions…") else tr("برچسب رنگی = وضعیت همین الان", "Coloured label = current level"),
                    color = C.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        item {
            Card {
                Text(tr("نحوهٔ کار", "How it works"), color = C.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    tr("• برنامه هر ساعت وضعیت را در پس‌زمینه بررسی می‌کند.\n" +
                        "• برای هر طوفان یک بار اعلان می‌آید؛ اگر شدیدتر شد (مثلاً از G1 به G2) دوباره خبر می‌دهیم.\n" +
                        "• اگر اعلان‌ها دیر می‌رسند، «بهینه‌سازی باتری» را برای این برنامه خاموش کنید.", "• The app checks conditions in the background every hour.\n• You get one notification per storm; if it intensifies (for example from G1 to G2) we notify you again.\n• If notifications arrive late, turn off “battery optimisation” for this app."),
                    color = C.muted, fontSize = 12.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 6.dp)
                )
                TextButton(
                    enabled = on,
                    onClick = { Notifier.postStorm(ctx, 2099, tr("آزمایش اعلان", "Test notification"), tr("اعلان طوفان‌های SpaceWeather برای شما فعال است ✓", "SpaceWeather storm notifications are on ✓")) },
                ) { Text(tr("ارسال اعلان آزمایشی", "Send a test notification"), color = if (on) C.sun else C.muted) }
            }
        }
    }
}
