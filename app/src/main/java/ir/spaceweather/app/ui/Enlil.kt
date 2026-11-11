package ir.spaceweather.app.ui

import ir.spaceweather.app.data.tr
import ir.spaceweather.app.data.L

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import ir.spaceweather.app.data.Fa
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** مثل سایت: ۴۰ فریم ساعتی، پخش خودکار از فریم ۲۰ تا ۴۰ */
private const val FRAMES = 40
private const val AUTO_FROM = 20
private const val AUTO_TO = 40
private const val DELAY = 450L
private const val END_PAUSE = 1500L

private data class Frame(val url: String, val time: ZonedDateTime)

private fun buildFrames(): List<Frame> {
    val end = ZonedDateTime.now(ZoneId.of("Asia/Tehran")).truncatedTo(ChronoUnit.HOURS)
    val f = DateTimeFormatter.ofPattern("yyMMddHH", Locale.US)
    return (FRAMES - 1 downTo 0).map { k ->
        val t = end.minusHours(k.toLong())
        Frame("http://spaceweather.ir/dailyimage/imf/${t.format(f)}.jpg", t)
    }
}

@Composable
fun EnlilPlayer() {
    val ctx = LocalContext.current
    val frames = remember { buildFrames() }
    var index by remember { mutableIntStateOf(AUTO_FROM - 1) }
    var playing by remember { mutableStateOf(true) }

    // پیش‌بارگذاری همه فریم‌ها
    LaunchedEffect(Unit) {
        frames.forEach { ctx.imageLoader.enqueue(ImageRequest.Builder(ctx).data(it.url).build()) }
    }
    LaunchedEffect(playing) {
        while (playing) {
            delay(if (index == AUTO_TO - 1) END_PAUSE else DELAY)
            index = if (index < AUTO_FROM - 1 || index >= AUTO_TO - 1) AUTO_FROM - 1 else index + 1
        }
    }

    Card {
        SectionTitle(tr("مدل ENLIL — باد خورشیدی", "WSA-Enlil model — solar wind"), tr("پیش‌بینی انتشار باد خورشیدی و CME در منظومه داخلی", "Forecast of the solar wind and CMEs in the inner solar system"))
        Box(Modifier.fillMaxWidth().aspectRatio(1.25f).clip(RoundedCornerShape(12.dp)).background(C.night)) {
            AsyncImage(
                model = ImageRequest.Builder(ctx).data(frames[index].url).placeholderMemoryCacheKey(frames[(index - 1).coerceAtLeast(0)].url).build(),
                contentDescription = "Enlil",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            val t = frames[index].time
            Text(
                Fa.dateHour(t.year, t.monthValue, t.dayOfMonth, t.hour) + "  ·  " + Fa.digits("${index + 1}/$FRAMES"),
                color = C.text, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp)
                    .clip(RoundedCornerShape(8.dp)).background(C.night.copy(alpha = .75f)).padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Slider(
            value = index.toFloat(), onValueChange = { playing = false; index = it.toInt() },
            valueRange = 0f..(FRAMES - 1).toFloat(), steps = FRAMES - 2,
            colors = SliderDefaults.colors(thumbColor = C.sun, activeTrackColor = C.sun, inactiveTrackColor = C.line,
                activeTickColor = C.sun.copy(alpha = 0f), inactiveTickColor = C.line.copy(alpha = 0f)),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { playing = false; index = (index - 1).coerceAtLeast(0) }) { Text(tr("قبلی", "Previous"), color = C.text) }
            TextButton(onClick = { playing = !playing }) {
                Text(if (playing) tr("⏸ توقف", "⏸ Stop") else tr("▶ پخش", "▶ Play"), color = C.sun, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            TextButton(onClick = { playing = false; index = (index + 1).coerceAtMost(FRAMES - 1) }) { Text(tr("بعدی", "Next"), color = C.text) }
        }
    }
}
