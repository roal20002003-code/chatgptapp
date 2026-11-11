package ir.spaceweather.app.ui

import ir.spaceweather.app.data.tr
import ir.spaceweather.app.data.L

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.spaceweather.app.HomeViewModel
import ir.spaceweather.app.data.*

@Composable
fun HomeScreen(vm: HomeViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val status = remember(s.hourly, s.charts) { Status.compute(s.hourly, s.charts) }

    Column(Modifier.fillMaxSize()) {
        AlertTicker(s.alerts)
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { DashboardHero(s.hourly, s.charts) }
            item { StatTiles(s.hourly, s.charts) }
            item { Scenes(status) }
            item { ReportCard(s.hourly, s.charts) }
            item { StormCharts(s.charts) }
            item { ChartsCard(s.charts) }
            item { SunImages() }
            item { EnlilPlayer() }
            item { TeamSection() }
            if (s.errors.isNotEmpty()) item {
                Text(
                    s.errors.joinToString("\n"), color = C.storm, fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text(
                    tr("spaceweather.ir — مرکز رصد فعالیت‌های خورشیدی\nنسخهٔ ", "spaceweather.ir — Solar Activity Observation Center\nVersion ") + Fa.ltr(installedVersion(ctx)),
                    color = C.muted, fontSize = 12.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }
        }
    }
}

/** نگه‌داشتن طولانی روی عنوان → ورود به بخش پایش */
@Composable
fun TopBar(loading: Boolean, onRefresh: () -> Unit, onLongPress: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(C.deep)
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(C.panel)
            .border(1.dp, C.line, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text("☀", color = C.sun, fontSize = 23.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("SpaceWeather.ir", color = C.text, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Text(tr("مرکز رصد فعالیت‌های خورشیدی", "Solar Activity Observation Center"), color = C.muted, fontSize = 10.sp)
        }
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = C.sun, strokeWidth = 2.dp)
        else TextButton(onClick = onRefresh) { Text(tr("⟳ بروزرسانی", "⟳ Refresh"), color = C.sun) }
    }
}

// داشبورد جدید: فقط نمایش اطلاعات واقعی دریافتی، بدون داده ساختگی
@Composable
private fun DashboardHero(h: Hourly?, c: Charts) {
    val available = h != null
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("☀  پایش زنده خورشید", "☀  Solar monitoring"), color = C.sun,
                    fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Spacer(Modifier.height(4.dp))
                Text(tr("وضعیت فعلی هواشناسی فضایی", "Current space weather conditions"),
                    color = C.muted, fontSize = 12.sp)
            }
            Box(Modifier.clip(RoundedCornerShape(12.dp))
                .background(if (available) C.ok.copy(alpha = 0.14f) else C.line.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp, vertical = 7.dp)) {
                Text(if (available) tr("● آنلاین", "● Live") else tr("● بدون داده", "● No data"),
                    color = if (available) C.ok else C.muted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(Status.report(h, c), color = C.text, fontSize = 14.sp, lineHeight = 24.sp)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
        Spacer(Modifier.height(10.dp))
        Text(tr("آخرین بروزرسانی: ", "Last updated: ") +
            (h?.let { Fa.updated(it.datetime) } ?: "—"), color = C.muted, fontSize = 11.sp)
    }
}

// ---------------- اخطارها ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlertTicker(alerts: List<Alert>) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(C.panel).clickable { open = !open }) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr("اخطارها:", "Alerts:"), color = C.sun, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            if (alerts.isEmpty()) {
                Text(tr("اخطاری ثبت نشده است", "No alerts recorded"), color = C.muted, fontSize = 13.sp)
            } else {
                Row(
                    Modifier.weight(1f).basicMarquee(iterations = Int.MAX_VALUE, velocity = 45.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    alerts.forEachIndexed { i, a ->
                        if (i > 0) Text("   |   ", color = C.muted, fontSize = 13.sp)
                        Text(a.shown, color = C.text, fontSize = 13.sp, maxLines = 1, softWrap = false, style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Content))
                        Fa.ago(a.date)?.let {
                            Text(" ($it)", color = C.muted, fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
        if (open) alerts.forEach { a ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
                Text(a.shown, color = C.text, fontSize = 13.sp, style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Content))
                Fa.ago(a.date)?.let { Text(it, color = C.muted, fontSize = 11.sp) }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
    }
}

// ---------------- گزارش کوتاه ----------------

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.panel)
            .border(1.dp, C.line, RoundedCornerShape(20.dp)).padding(14.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(t: String, sub: String? = null) {
    Text(t, color = C.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    if (sub != null) Text(sub, color = C.muted, fontSize = 12.sp)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ReportCard(h: Hourly?, c: Charts) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("گزارش کوتاه", "Short report"), color = C.sun, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            if (h != null) Text(
                tr("آخرین بروزرسانی: ", "Last update: ") + Fa.updated(h.datetime), color = C.muted, fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(Status.report(h, c), color = C.text, fontSize = 15.sp, lineHeight = 26.sp)
        h?.notes?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = C.muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun StatTiles(h: Hourly?, c: Charts) {
    val tiles = listOf(
        Triple(tr("باد خورشیدی", "Solar wind"), h?.windSpeed?.let { Fa.num(it) }, "km/s"),
        Triple(tr("شاخص Kp", "Kp index"), h?.kp?.let { Fa.num(it) }, ""),
        Triple("F10.7", h?.f107?.let { Fa.num(it) }, "sfu"),
        Triple(tr("اشعه ایکس", "X-ray"), Status.xrayClassOf(h, c), ""),
        Triple("Bz", h?.bz?.let { Fa.num(it, 1) }, "nT"),
        Triple("Bt", h?.bt?.let { Fa.num(it, 1) }, "nT"),
        Triple("Dst", h?.dst?.let { Fa.num(it, 0) }, "nT"),
        Triple(tr("لکه‌های خورشیدی", "Sunspots"), h?.sunspot?.let { Fa.num(it) }, ""),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value, unit) ->
                    // همهٔ کاشی‌ها یک اندازه: ارتفاع ثابت، خط واحد همیشه جا دارد
                    Column(
                        Modifier.weight(1f).height(94.dp).clip(RoundedCornerShape(16.dp)).background(C.deep)
                            .border(1.dp, C.line, RoundedCornerShape(16.dp)).padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(label, color = C.muted, fontSize = 12.sp, maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(value ?: "—", color = C.text, fontWeight = FontWeight.Bold, fontSize = 23.sp, maxLines = 1)
                        Text(unit.ifEmpty { " " }, color = C.muted, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

// ---------------- معرفی گروه ----------------

data class Member(val name: String, val role: String, val photo: String)

/** برای تغییر اعضا فقط همین فهرست را ویرایش کنید */
val TEAM get() = listOf(
    Member(tr("علی سلطانی", "Ali Soltani"), tr("گزارشگر · منجم آماتور", "Reporter · amateur astronomer"), "http://spaceweather.ir/img/team-ali.webp"),
    Member(tr("عقیل ساری", "Aghil Sari"), tr("طراح وب و توسعه‌دهنده", "Web designer & developer"), "http://spaceweather.ir/img/team-aghil.webp"),
    Member(tr("مهدی گلدی", "Mahdi Galdi"), tr("اخترفیزیک‌دان و پژوهشگر هواشناسی فضایی · برنامه‌نویس هوش مصنوعی", "Astrophysicist & space weather researcher · AI programmer"), "http://spaceweather.ir/img/team-mahdi.webp"),
)

@Composable
private fun TeamSection() {
    Card {
        SectionTitle(tr("معرفی گروه", "Our team"), tr("تیم رصد و پایش هواشناسی فضایی", "The space weather monitoring team"))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TEAM.forEach { m ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = m.photo, contentDescription = m.name, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(84.dp).clip(CircleShape).background(C.deep)
                            .border(2.dp, C.sun, CircleShape)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(m.name, color = C.text, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Text(m.role, color = C.muted, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
