package ir.spaceweather.app.ui

import ir.spaceweather.app.data.tr
import ir.spaceweather.app.data.L

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.spaceweather.app.data.Charts
import ir.spaceweather.app.data.Fa
import ir.spaceweather.app.data.Status
import kotlin.math.log10
import kotlin.math.pow

/**
 * سه نمودار ۳ روزه بر اساس مقیاس‌های NOAA:
 *  R (Radio Blackout)        ← شار اشعه ایکس   R1=M1  R2=M5  R3=X1  R4=X10  R5=X20
 *  G (Geomagnetic Storm)     ← شاخص Kp          G1=5   G2=6   G3=7   G4=8    G5=9
 *  S (Solar Radiation Storm) ← پروتون ≥10MeV    S1=10  S2=1e2 S3=1e3 S4=1e4  S5=1e5 pfu
 */

/** رنگ سطح ۰ تا ۵ : سبز → قرمز */
val LEVEL_COLORS = listOf(
    Color(0xFF4FD1B0), Color(0xFFB5E26B), Color(0xFFFFD166),
    Color(0xFFFFB23F), Color(0xFFFF7A45), Color(0xFFFF3B4F),
)

/**
 * مقدار proton در ChartData لگاریتمی (log10 pfu) است یا خام؟
 * null = خودکار: اگر عدد منفی در داده باشد لگاریتمی فرض می‌شود.
 */
val PROTON_IS_LOG: Boolean? = null

private class StormSpec(
    val key: String,
    val letter: String,
    val title: String,
    val subtitle: String,
    val log: Boolean,
    val min: Double, val max: Double,
    val thresholds: List<Double>,              // آستانهٔ سطح ۱ تا ۵
    val leftTicks: List<Pair<Double, String>>, // برچسب‌های محور چپ
    val bars: Boolean = false,
    val valueText: (Double) -> String,
)

private val XRAY get() = StormSpec(
    key = "xray", letter = "R",
    title = tr("طوفان رادیویی (Radio Blackout)", "Radio Blackout"),
    subtitle = tr("شار اشعه ایکس خورشید · ۳ روز گذشته", "Solar X-ray flux · last 3 days"),
    log = true, min = 1e-8, max = 4e-3,
    thresholds = listOf(1e-5, 5e-5, 1e-4, 1e-3, 2e-3),
    leftTicks = listOf(1e-8 to "A", 1e-7 to "B", 1e-6 to "C", 1e-5 to "M", 1e-4 to "X"),
    valueText = { Status.xrayClass(it) ?: "—" },
)

private val KP get() = StormSpec(
    key = "kp", letter = "G",
    title = tr("طوفان ژئومغناطیسی (Geomagnetic)", "Geomagnetic Storm"),
    subtitle = tr("شاخص Kp · ۳ روز گذشته", "Kp index · last 3 days"),
    log = false, min = 0.0, max = 9.0,
    thresholds = listOf(5.0, 6.0, 7.0, 8.0, 9.0),
    leftTicks = (0..9 step 1).map { it.toDouble() to Fa.digits("$it") },
    bars = true,
    valueText = { "Kp " + Fa.num(it, 0) },
)

private val PROTON get() = StormSpec(
    key = "proton", letter = "S",
    title = tr("طوفان تابشی (Solar Radiation)", "Solar Radiation Storm"),
    subtitle = tr("شار پروتون ", "Proton flux ") + Fa.ltr("≥10 MeV") + tr(" · ۳ روز گذشته", " · last 3 days"),
    log = true, min = 1e-1, max = 3e5,
    thresholds = listOf(1e1, 1e2, 1e3, 1e4, 1e5),
    leftTicks = listOf(1e-1 to tr("۰٫۱", "0.1"), 1.0 to tr("۱", "1"), 1e1 to tr("۱۰", "10"), 1e2 to tr("۱۰²", "10²"), 1e3 to tr("۱۰³", "10³"), 1e4 to tr("۱۰⁴", "10⁴"), 1e5 to tr("۱۰⁵", "10⁵")),
    valueText = { Fa.num(it, if (it < 10) 2 else 0) + " pfu" },
)

private fun StormSpec.level(v: Double): Int = thresholds.count { v >= it }

@Composable
fun StormCharts(charts: Charts) {
    val proton = charts.series["proton"]?.let { s ->
        val isLog = PROTON_IS_LOG ?: s.any { it != null && it < 0 }
        if (isLog) s.map { it?.let { v -> 10.0.pow(v) } } else s
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StormCard(XRAY, Status.xrayFlux(charts.series["xray"].orEmpty()).takeLast(72))
        StormCard(KP, charts.series["kp"].orEmpty().takeLast(72))
        StormCard(PROTON, proton.orEmpty().takeLast(72))
    }
}

@Composable
private fun StormCard(spec: StormSpec, data: List<Double?>) {
    val last = data.lastOrNull { it != null }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(spec.title, color = C.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(spec.subtitle, color = C.muted, fontSize = 11.sp)
            }
            if (last != null) {
                val lv = spec.level(last)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        spec.letter + Fa.digits("$lv"), color = C.night, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(LEVEL_COLORS[lv])
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                    Text(Fa.ltr(spec.valueText(last)), color = C.muted, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (data.count { it != null } < 2) {
            Text(tr("داده‌ای برای نمایش نیست", "No data to display"), color = C.muted, fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 40.dp).fillMaxWidth())
        } else {
            val tm = rememberTextMeasurer()
            Canvas(Modifier.fillMaxWidth().height(190.dp)) { drawStorm(spec, data, tm) }
            Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
                // RTL: اولین مورد سمت راست = اکنون
                listOf(tr("اکنون", "now"), tr("۱ روز پیش", "1 day ago"), tr("۲ روز پیش", "2 days ago"), tr("۳ روز پیش", "3 days ago"))
                    .let { if (L.en) it.reversed() else it }.forEachIndexed { i, s ->
                    if (i > 0) Spacer(Modifier.weight(1f))
                    Text(s, color = C.muted, fontSize = 10.sp)
                }
            }
        }
    }
}

private fun DrawScope.drawStorm(spec: StormSpec, data: List<Double?>, tm: TextMeasurer) {
    val padL = 30.dp.toPx(); val padR = 30.dp.toPx(); val padT = 6.dp.toPx(); val padB = 4.dp.toPx()
    val x0 = padL; val x1 = size.width - padR; val y0 = padT; val y1 = size.height - padB
    val t: (Double) -> Double = { if (spec.log) log10(it.coerceAtLeast(spec.min * 0.5)) else it }
    val lo = t(spec.min); val hi = t(spec.max)
    fun y(v: Double) = (y1 - ((t(v) - lo) / (hi - lo)).coerceIn(0.0, 1.0) * (y1 - y0)).toFloat()
    val n = data.size
    fun x(i: Int) = x0 + (x1 - x0) * i / (n - 1).coerceAtLeast(1)
    val small = TextStyle(fontSize = 10.sp, color = C.muted)

    // نوارهای رنگی سطح‌ها (سبز → قرمز)
    val edges = listOf(spec.min) + spec.thresholds + listOf(spec.max)
    for (lv in 0..5) {
        val top = y(edges[lv + 1].coerceAtMost(spec.max)); val bot = y(edges[lv])
        if (bot > top) drawRect(LEVEL_COLORS[lv].copy(alpha = 0.07f), Offset(x0, top), Size(x1 - x0, bot - top))
    }
    // خطوط روزها
    for (d in 1..2) {
        val xx = x1 - (x1 - x0) * d / 3f
        drawLine(C.line, Offset(xx, y0), Offset(xx, y1), 1f)
    }
    // محور چپ: مقدار واقعی
    spec.leftTicks.forEach { (v, s) ->
        val yy = y(v)
        drawLine(C.line.copy(alpha = .6f), Offset(x0, yy), Offset(x1, yy), 1f)
        val lay = tm.measure(s, small.copy(fontWeight = if (spec.key == "xray") FontWeight.Bold else FontWeight.Normal), layoutDirection = LayoutDirection.Ltr)
        val ty = if (spec.key == "xray") yy - lay.size.height - 1 else yy - lay.size.height / 2f  // A..X بالای خط دهه
        drawText(lay, topLeft = Offset(x0 - lay.size.width - 6.dp.toPx(), ty.coerceIn(0f, size.height - lay.size.height)))
    }
    // خطوط آستانه + محور راست: R1..R5
    val labels = spec.thresholds.mapIndexed { i, _ ->
        tm.measure(spec.letter + Fa.digits("${i + 1}"), small.copy(color = LEVEL_COLORS[i + 1], fontWeight = FontWeight.Bold), layoutDirection = LayoutDirection.Ltr)
    }
    // جای برچسب‌ها طوری که روی هم نیفتند (مثلاً R4 و R5 خیلی نزدیک‌اند)
    val gap = labels.first().size.height * 0.95f
    val ly = spec.thresholds.map { y(it) }.toMutableList()
    for (i in 1 until ly.size) if (ly[i] > ly[i - 1] - gap) ly[i] = ly[i - 1] - gap
    val minTop = labels.first().size.height / 2f
    if (ly.last() < minTop) { ly[ly.lastIndex] = minTop; for (i in ly.lastIndex - 1 downTo 0) if (ly[i] < ly[i + 1] + gap) ly[i] = ly[i + 1] + gap }
    spec.thresholds.forEachIndexed { i, th ->
        val lv = i + 1; val yy = y(th)
        drawLine(LEVEL_COLORS[lv], Offset(x0, yy), Offset(x1, yy), 1.6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 5f)))
        drawText(labels[i], topLeft = Offset(x1 + 5.dp.toPx(), ly[i] - labels[i].size.height / 2f))
    }
    // سطح ۰
    run {
        val lay = tm.measure(spec.letter + tr("۰", "0"), small.copy(color = LEVEL_COLORS[0], fontWeight = FontWeight.Bold), layoutDirection = LayoutDirection.Ltr)
        val yy = (y(spec.thresholds[0]) + y1) / 2f
        drawText(lay, topLeft = Offset(x1 + 5.dp.toPx(), yy - lay.size.height / 2f))
    }

    // داده
    if (spec.bars) {
        val bw = (x1 - x0) / n
        data.forEachIndexed { i, v ->
            if (v == null) return@forEachIndexed
            val top = y(v)
            drawRect(LEVEL_COLORS[spec.level(v)], Offset(x0 + i * bw, top), Size(bw * 0.9f, y1 - top))
        }
    } else {
        var prev: Pair<Offset, Double>? = null
        data.forEachIndexed { i, v ->
            if (v == null) { prev = null; return@forEachIndexed }
            val p = Offset(x(i), y(v))
            prev?.let { (pp, pv) ->
                drawLine(LEVEL_COLORS[spec.level(maxOf(pv, v))], pp, p, 2.4f)
            }
            prev = p to v
        }
        data.indexOfLast { it != null }.takeIf { it >= 0 }?.let { i ->
            drawCircle(LEVEL_COLORS[spec.level(data[i]!!)], 4f, Offset(x(i), y(data[i]!!)))
        }
    }
    drawRect(C.line, Offset(x0, y0), Size(x1 - x0, y1 - y0), style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
}
