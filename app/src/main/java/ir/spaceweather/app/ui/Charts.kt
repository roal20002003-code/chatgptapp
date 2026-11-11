package ir.spaceweather.app.ui

import ir.spaceweather.app.data.tr
import ir.spaceweather.app.data.L

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import ir.spaceweather.app.data.Charts
import ir.spaceweather.app.data.Fa
import ir.spaceweather.app.data.Status
import kotlin.math.log10
import kotlin.math.pow

data class SeriesMeta(val key: String, val title: String, val unit: String, val color: Color, val log: Boolean = false, val bars: Boolean = false, val zero: Boolean = false)

val SERIES get() = listOf(
    SeriesMeta("wind", tr("سرعت باد خورشیدی", "Solar wind speed"), "km/s", C.sun),
    SeriesMeta("kp", tr("شاخص Kp", "Kp index"), "", C.ok, bars = true),
    SeriesMeta("xray", tr("شار اشعه ایکس", "X-ray flux"), "", C.flare, log = true),
    SeriesMeta("sfu", tr("شار رادیویی F10.7", "F10.7 radio flux"), "sfu", C.warn),
    SeriesMeta("bz", tr("میدان Bz", "IMF Bz"), "nT", C.sky, zero = true),
    SeriesMeta("bt", tr("میدان Bt", "IMF Bt"), "nT", Color(0xFF9F8CFF)),
    SeriesMeta("dens", tr("چگالی پلاسما", "Plasma density"), "p/cm³", Color(0xFF6EE7F9)),
    SeriesMeta("temp", tr("دمای باد خورشیدی", "Solar wind temperature"), "K", Color(0xFFFF9EC7)),
    SeriesMeta("dst", tr("شاخص Dst", "Dst index"), "nT", C.storm, zero = true),
    SeriesMeta("proton", tr("شار پروتون", "Proton flux"), "pfu", Color(0xFFFFC2A0), log = true),
    SeriesMeta("elec", tr("شار الکترون", "Electron flux"), "pfu", Color(0xFFB7F07A), log = true),
)

private val RANGES get() = listOf(1 to tr("۱ روز", "1 day"), 3 to tr("۳ روز", "3 days"), 7 to tr("۷ روز", "7 days"), 30 to tr("۳۰ روز", "30 days"))

@Composable
private fun Chip(label: String, selected: Boolean, color: Color = C.sun, onClick: () -> Unit) {
    Text(
        label, fontSize = 12.sp,
        color = if (selected) C.night else C.text,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (selected) color else C.deep)
            .border(1.dp, if (selected) color else C.line, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** xray: شاخص ۰–۶۰ → شار؛ proton: log10(pfu) → pfu (مثل نمودارهای طوفان) */
private fun normalized(key: String, s: List<Double?>): List<Double?> = when (key) {
    "xray" -> Status.xrayFlux(s)
    "proton" -> if (s.any { it != null && it < 0 }) s.map { it?.let { v -> 10.0.pow(v) } } else s
    else -> s
}

/** همهٔ نمودارها زیر هم، با یک انتخاب بازهٔ زمانی مشترک */
@Composable
fun ChartsCard(charts: Charts) {
    var days by remember { mutableIntStateOf(3) }
    val list = SERIES.filter { charts.series.isEmpty() || charts.series.containsKey(it.key) }

    Card {
        SectionTitle(tr("نمودارها", "Charts"), tr("داده‌های ساعتی · بازهٔ نمایش را انتخاب کنید", "Hourly data · choose a time range"))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RANGES.forEach { (d, l) -> Chip(l, d == days) { days = d } }
        }
        list.forEach { meta ->
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
            Spacer(Modifier.height(12.dp))
            SeriesBlock(meta, normalized(meta.key, charts.series[meta.key].orEmpty()).takeLast(days * 24), days)
        }
    }
}

@Composable
private fun SeriesBlock(meta: SeriesMeta, data: List<Double?>, days: Int) {
    val vals = data.filterNotNull()
    fun fmt(v: Double): String = when {
        meta.key == "xray" -> Status.xrayClass(v) ?: "—"
        meta.key == "kp" -> Fa.num(v, 0)
        meta.key == "proton" -> Fa.num(v, if (v < 10) 2 else 0)
        meta.log -> String.format(java.util.Locale.US, "%.1e", v)
        kotlin.math.abs(v) < 20 -> Fa.num(v, 1)
        else -> Fa.num(v, 0)
    }
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(meta.color))
        Spacer(Modifier.width(8.dp))
        Text(meta.title, color = C.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
        vals.lastOrNull()?.let {
            Text(Fa.ltr(fmt(it) + (if (meta.unit.isNotEmpty()) " " + meta.unit else "")), color = meta.color,
                fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
    if (vals.isNotEmpty()) Text(
        tr("کمینه ${fmt(vals.min())} · بیشینه ${fmt(vals.max())}", "min ${fmt(vals.min())} · max ${fmt(vals.max())}"), color = C.muted, fontSize = 11.sp
    )
    Spacer(Modifier.height(6.dp))
    if (vals.size < 2) {
        Text(tr("داده‌ای برای نمایش نیست", "No data to display"), color = C.muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 30.dp))
    } else {
        LineChart(data, meta, Modifier.fillMaxWidth().height(170.dp), days)
    }
}

private val SUP = mapOf('-' to '⁻', '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴', '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹')
private fun pow10Label(e: Int) = "10" + e.toString().map { SUP[it] ?: it }.joinToString("")

/** گام «گرد» برای محور: ۱، ۲، ۵ × ۱۰ⁿ */
private fun niceStep(range: Double): Double {
    val raw = range / 4
    val mag = 10.0.pow(kotlin.math.floor(log10(raw)))
    val f = raw / mag
    return mag * when { f <= 1 -> 1.0; f <= 2 -> 2.0; f <= 5 -> 5.0; else -> 10.0 }
}

private fun axisNum(v: Double, step: Double): String = when {
    kotlin.math.abs(v) >= 10000 -> Fa.num(v / 1000, 0) + "k"
    step < 1 -> Fa.num(v, 1)
    else -> Fa.num(v, 0)
}

/** نمودار با محور عمودی (مقدار) و محور افقی (زمان) */
@Composable
fun LineChart(data: List<Double?>, meta: SeriesMeta, modifier: Modifier, days: Int) {
    val tm = rememberTextMeasurer()
    Canvas(modifier) {
        val style = TextStyle(fontSize = 9.sp, color = C.muted)
        val x0 = 42.dp.toPx(); val x1 = size.width - 6.dp.toPx()
        val y0 = 6.dp.toPx(); val y1 = size.height - 18.dp.toPx()

        val t: (Double) -> Double = { if (meta.log) log10(maxOf(it, 1e-12)) else it }
        val v = data.filterNotNull().map(t)
        var lo = v.min(); var hi = v.max()
        if (meta.bars) { lo = 0.0; hi = maxOf(hi, 5.0) }
        if (meta.zero) { lo = minOf(lo, 0.0); hi = maxOf(hi, 0.0) }
        if (hi - lo < 1e-9) { hi += 1; lo -= 1 }
        if (meta.log) { lo = kotlin.math.floor(lo); hi = kotlin.math.ceil(hi); if (hi - lo < 1) hi = lo + 1 }
        else if (!meta.bars) { val pad = (hi - lo) * 0.08; hi += pad; lo -= pad }
        val n = data.size
        fun x(i: Int) = if (n <= 1) x0 else x0 + (x1 - x0) * i / (n - 1)
        fun yT(tv: Double) = (y1 - (tv - lo) / (hi - lo) * (y1 - y0)).toFloat()
        fun y(value: Double) = yT(t(value))

        // ---- محور عمودی: خطوط و اعداد ----
        val ticks = ArrayList<Pair<Double, String>>()
        if (meta.log) {
            for (e in lo.toInt()..hi.toInt()) {
                val label = if (meta.key == "xray") when (e) {
                    -8 -> "A"; -7 -> "B"; -6 -> "C"; -5 -> "M"; -4 -> "X"; else -> pow10Label(e)
                } else pow10Label(e)
                ticks += e.toDouble() to label
            }
        } else {
            val step = if (meta.bars) 1.0 * maxOf(1, ((hi - lo) / 5).toInt()) else niceStep(hi - lo)
            var tv = kotlin.math.ceil(lo / step) * step
            while (tv <= hi + 1e-9) { ticks += tv to axisNum(tv, step); tv += step }
        }
        ticks.forEach { (tv, label) ->
            val yy = yT(tv)
            drawLine(C.line, Offset(x0, yy), Offset(x1, yy), 1f)
            val lay = tm.measure(label, style, layoutDirection = LayoutDirection.Ltr)
            // برای A..X حرف وسط دهه می‌نشیند
            val ly = if (meta.key == "xray" && tv < hi) (yy + yT(tv + 1)) / 2f else yy
            drawText(lay, topLeft = Offset(x0 - lay.size.width - 5.dp.toPx(), (ly - lay.size.height / 2f).coerceIn(0f, y1 - lay.size.height / 2f)))
        }
        drawLine(C.muted.copy(alpha = .5f), Offset(x0, y0), Offset(x0, y1), 1f)
        drawLine(C.muted.copy(alpha = .5f), Offset(x0, y1), Offset(x1, y1), 1f)

        // ---- محور افقی: زمان (راست = اکنون) ----
        val hours = days * 24
        val stepH = when { days <= 1 -> 6; days <= 7 -> 24; else -> 120 }
        var hAgo = 0
        while (hAgo <= hours) {
            val xx = x1 - (x1 - x0) * hAgo / hours
            if (hAgo in 1 until hours) drawLine(C.line, Offset(xx, y0), Offset(xx, y1), 1f)
            val label = when {
                hAgo == 0 -> tr("اکنون", "now")
                days <= 1 -> Fa.digits("$hAgo") + tr(" ساعت", " h")
                else -> Fa.digits("${hAgo / 24}") + tr(" روز", " d")
            }
            val lay = tm.measure(label, style)
            val lx = (xx - lay.size.width / 2f).coerceIn(x0 - 4.dp.toPx(), size.width - lay.size.width)
            drawText(lay, topLeft = Offset(lx, y1 + 3.dp.toPx()))
            hAgo += stepH
        }

        if (meta.zero) {
            val yz = yT(0.0)
            drawLine(C.muted, Offset(x0, yz), Offset(x1, yz), 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        }
        if (meta.key == "xray") {
            val yM = y(1e-5)   // آستانه M1
            if (yM in y0..y1) drawLine(C.storm, Offset(x0, yM), Offset(x1, yM), 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        }

        if (meta.bars) {
            val bw = maxOf(1f, (x1 - x0) / n * 0.8f)
            data.forEachIndexed { i, d ->
                if (d == null) return@forEachIndexed
                val col = when { d >= 5 -> C.storm; d >= 4 -> C.warn; else -> C.ok }
                val top = y(d)
                drawRect(col, Offset(x(i) - bw / 2, top), Size(bw, y1 - top))
            }
            return@Canvas
        }

        val path = Path(); var pen = false
        data.forEachIndexed { i, d ->
            if (d == null) { pen = false; return@forEachIndexed }
            if (!pen) { path.moveTo(x(i), y(d)); pen = true } else path.lineTo(x(i), y(d))
        }
        drawPath(path, meta.color, style = Stroke(width = 2.2f))
        data.indexOfLast { it != null }.takeIf { it >= 0 }?.let { i ->
            drawCircle(meta.color, 4.5f, Offset(x(i), y(data[i]!!)))
        }
    }
}

@Suppress("unused")
private fun unlog(v: Double) = 10.0.pow(v)
