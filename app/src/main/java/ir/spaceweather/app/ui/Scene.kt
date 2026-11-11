package ir.spaceweather.app.ui

import ir.spaceweather.app.data.tr
import ir.spaceweather.app.data.L

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.spaceweather.app.data.Level
import ir.spaceweather.app.data.SceneStatus
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * سه صحنهٔ متحرک: خورشید ← فضای میان‌سیاره‌ای ← زمین.
 * در چیدمان راست‌به‌چپ، خورشید سمت راست و زمین سمت چپ قرار می‌گیرد،
 * پس ذرات باد از راست به چپ حرکت می‌کنند.
 */
@Composable
fun Scenes(st: SceneStatus) {
    Card {
        SectionTitle(tr("وضعیت لحظه‌ای", "Current conditions"), tr("خورشید · فضای میان‌سیاره‌ای · زمین", "Sun · interplanetary space · Earth"))
        val inf = rememberInfiniteTransition(label = "scene")
        val phase by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "p")
        val pulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "q")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SceneBox(tr("خورشید", "Sun"), st.sun, st.sunNote, Modifier.weight(1f)) { drawSun(st.sun == Level.ACTIVE, pulse, phase) }
            SceneBox(
                tr("باد خورشیدی", "Solar wind"), st.wind,
                st.windNote + when (st.bzSouth) { true -> tr("\nBz جنوب‌سو ↓", "\nBz southward ↓"); false -> tr("\nBz شمال‌سو ↑", "\nBz northward ↑"); null -> "" },
                Modifier.weight(1f)
            ) { drawWind(st.wind == Level.ACTIVE, st.bzSouth, phase) }
            SceneBox(tr("زمین", "Earth"), st.earth, st.earthNote, Modifier.weight(1f)) { drawEarth(st.earth == Level.ACTIVE, pulse, phase) }
        }
    }
}

@Composable
private fun SceneBox(title: String, level: Level, note: String, modifier: Modifier, draw: DrawScope.() -> Unit) {
    val (label, col) = when (level) {
        Level.ACTIVE -> tr("فعال", "Active") to C.storm
        Level.CALM -> tr("آرام", "Calm") to C.ok
        Level.UNKNOWN -> tr("نامعلوم", "Unknown") to C.muted
    }
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(C.night)
            .border(1.dp, if (level == Level.ACTIVE) C.storm.copy(alpha = .6f) else C.line, RoundedCornerShape(14.dp))
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.fillMaxWidth().height(104.dp).graphicsLayer { scaleX = if (L.en) -1f else 1f }, onDraw = draw)
        Text(title, color = C.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(
            label, color = C.night, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 3.dp).clip(RoundedCornerShape(10.dp)).background(col)
                .padding(horizontal = 10.dp, vertical = 1.dp)
        )
        Text(note, color = C.muted, fontSize = 10.sp, minLines = 2, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 3.dp, start = 4.dp, end = 4.dp))
    }
}

private fun DrawScope.drawSun(active: Boolean, pulse: Float, phase: Float) {
    val c = center; val r = size.minDimension * 0.26f
    val glow = if (active) C.flare else C.sun
    drawCircle(
        Brush.radialGradient(listOf(glow.copy(alpha = if (active) .55f else .3f), Color.Transparent), c, r * (2.0f + pulse * if (active) .6f else .2f)),
        r * (2.0f + pulse * if (active) .6f else .2f), c
    )
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF3C4), C.sun, Color(0xFFE8731A)), c, r), r, c)
    if (active) {
        // شراره: حلقه‌های تاج که بیرون می‌زنند
        for (k in 0 until 3) {
            val a = (phase * 2 * PI + k * 2.1).toFloat()
            val p = Path()
            val s = Offset(c.x + r * cos(a), c.y + r * sin(a))
            val e = Offset(c.x + r * cos(a + .7f), c.y + r * sin(a + .7f))
            val m = Offset(c.x + r * (1.9f + pulse * .3f) * cos(a + .35f), c.y + r * (1.9f + pulse * .3f) * sin(a + .35f))
            p.moveTo(s.x, s.y); p.quadraticBezierTo(m.x, m.y, e.x, e.y)
            drawPath(p, C.flare.copy(alpha = .85f), style = Stroke(3f))
        }
        drawCircle(Color.White.copy(alpha = .9f * pulse), r * .18f, Offset(c.x + r * .45f, c.y - r * .3f))
    }
}

private fun DrawScope.drawWind(active: Boolean, bzSouth: Boolean?, phase: Float) {
    val w = size.width; val h = size.height
    val rows = 7
    val col = if (active) C.flare else C.sun
    val speed = if (active) 3f else 1f
    for (i in 0 until rows) {
        val y = h * (i + 1) / (rows + 1)
        for (j in 0 until 3) {
            val base = ((phase * speed + j / 3f + i * 0.137f) % 1f)
            val x = w - base * (w + 40f) // از راست (خورشید) به چپ (زمین)
            val len = if (active) 30f else 16f
            drawLine(col.copy(alpha = .25f + .6f * (1 - base)), Offset(x, y), Offset(x + len, y), strokeWidth = 2.5f)
        }
    }
    // جهت Bz
    if (bzSouth != null) {
        val cx = w / 2; val cy = h / 2
        val arrowCol = if (bzSouth) C.storm else C.ok
        val dir = if (bzSouth) 1 else -1
        drawLine(arrowCol, Offset(cx, cy - 22f * dir), Offset(cx, cy + 22f * dir), 4f)
        val tip = Path().apply {
            moveTo(cx, cy + 30f * dir); lineTo(cx - 10f, cy + 16f * dir); lineTo(cx + 10f, cy + 16f * dir); close()
        }
        drawPath(tip, arrowCol)
    }
}

private fun DrawScope.drawEarth(active: Boolean, pulse: Float, phase: Float) {
    val c = center; val r = size.minDimension * 0.24f
    // مغناطیس‌کره، فشرده‌تر در حالت فعال (سمت رو به خورشید = راست)
    val squeeze = if (active) 1.35f else 1.8f
    for (k in 1..3) {
        val rr = r * (1f + k * .28f)
        drawArc(
            C.sky.copy(alpha = .35f - k * .07f), -60f, 120f, false,
            Offset(c.x - rr * squeeze * 0.3f, c.y - rr), Size(rr * squeeze, rr * 2), style = Stroke(2f)
        )
    }
    drawCircle(Brush.radialGradient(listOf(Color(0xFF6FC3FF), Color(0xFF1F5FBF), Color(0xFF0B2A66)), Offset(c.x + r * .3f, c.y - r * .3f), r * 1.6f), r, c)
    // خشکی‌ها
    drawCircle(Color(0xFF3FB27F).copy(alpha = .8f), r * .32f, Offset(c.x - r * .25f + phase * 4, c.y - r * .1f))
    drawCircle(Color(0xFF3FB27F).copy(alpha = .7f), r * .2f, Offset(c.x + r * .35f, c.y + r * .35f))
    if (active) {
        // شفق قطبی
        val a = .45f + .45f * pulse
        drawArc(Color(0xFF4FFFA0).copy(alpha = a), 200f, 140f, false, Offset(c.x - r * .8f, c.y - r * 1.25f), Size(r * 1.6f, r * .7f), style = Stroke(5f))
        drawArc(Color(0xFFFF5DA8).copy(alpha = a * .8f), 20f, 140f, false, Offset(c.x - r * .8f, c.y + r * .55f), Size(r * 1.6f, r * .7f), style = Stroke(5f))
    }
}
