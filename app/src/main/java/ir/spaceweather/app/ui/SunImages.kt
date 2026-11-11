package ir.spaceweather.app.ui

import ir.spaceweather.app.data.tr
import ir.spaceweather.app.data.L

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage

/** همان فهرست طول‌موج‌های سایت (newsun.aspx) — تصاویر زندهٔ NASA SDO */
private data class Wave(val key: String, val label: String, val color: Color, val desc: String, val url: String)

private const val SDO = "https://sdo.gsfc.nasa.gov/assets/img/latest/latest_1024_"

private val WAVES get() = listOf(
    Wave("SPOT1", tr("لکه‌ها ۱", "Spots 1"), Color(0xFFFFB23F), tr("تصویر مرئی · لکه‌های خورشیدی", "Visible light · sunspots"), SDO + "HMIIF.jpg"),
    Wave("SPOT2", tr("لکه‌ها ۲", "Spots 2"), Color(0xFFFF9C3A), tr("تصویر مرئی · لکه‌های خورشیدی (نمای دوم)", "Visible light · sunspots (second view)"), SDO + "HMIIC.jpg"),
    Wave("0094", "94 Å", Color(0xFF2E7D5B), tr("رنگ سبز · گازهای بسیار داغ شراره‌ها", "Green · very hot flare plasma"), SDO + "0094.jpg"),
    Wave("0131", "131 Å", Color(0xFF2FA0B8), tr("رنگ فیروزه‌ای · شراره‌ها و پلاسمای داغ", "Teal · flares and hot plasma"), SDO + "0131.jpg"),
    Wave("0171", "171 Å", Color(0xFFE6B23A), tr("رنگ زرد · حلقه‌های مغناطیسی تاج خورشید", "Gold · magnetic loops in the corona"), SDO + "0171.jpg"),
    Wave("0193", "193 Å", Color(0xFFC98A3C), tr("رنگ کهربایی · تاج و چاله‌های تاجی", "Amber · corona and coronal holes"), SDO + "0193.jpg"),
    Wave("0211", "211 Å", Color(0xFFA55AC2), tr("رنگ بنفش · نواحی فعال مغناطیسی", "Purple · magnetically active regions"), SDO + "0211.jpg"),
    Wave("0304", "304 Å", Color(0xFFE0522D), tr("رنگ قرمز · کروموسفر و زبانه‌ها", "Red · chromosphere and prominences"), SDO + "0304.jpg"),
    Wave("1600", "1600 Å", Color(0xFF7AA7E8), tr("رنگ آبی · بالای فتوسفر", "Blue · upper photosphere"), SDO + "1600.jpg"),
    Wave("1700", "1700 Å", Color(0xFFD9C7A0), tr("رنگ کرم · فتوسفر", "Cream · photosphere"), SDO + "1700.jpg"),
)

@Composable
fun SunImages() {
    var sel by remember { mutableStateOf(WAVES.first()) }   // پیش‌فرض: لکه‌ها (SPOT)
    val stamp = remember { System.currentTimeMillis() / 600_000 } // هر ۱۰ دقیقه نسخهٔ تازه

    Card {
        SectionTitle(tr("تصاویر زندهٔ خورشید", "Live images of the Sun"), tr("طول‌موج‌های مختلف، لایه‌های متفاوتی از جو خورشید را نشان می‌دهند.", "Different wavelengths reveal different layers of the solar atmosphere."))
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp)).background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            SubcomposeAsyncImage(
                model = "${sel.url}?t=$stamp",
                contentDescription = tr("تصویر خورشید ", "The Sun at ") + sel.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = sel.color, strokeWidth = 2.dp) } },
                error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(tr("تصویر بارگذاری نشد", "Image could not be loaded"), color = C.muted, fontSize = 12.sp) } },
            )
            Text(
                sel.label, color = C.text, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                    .clip(RoundedCornerShape(8.dp)).background(C.night.copy(alpha = .75f)).padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(sel.color))
            Spacer(Modifier.width(8.dp))
            Text(sel.desc, color = C.text, fontSize = 13.sp)
        }
        Row(
            Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            WAVES.forEach { w ->
                val on = w.key == sel.key
                Row(
                    Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (on) w.color else C.deep)
                        .border(1.dp, if (on) w.color else C.line, RoundedCornerShape(20.dp))
                        .clickable { sel = w }.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!on) { Box(Modifier.size(8.dp).clip(CircleShape).background(w.color)); Spacer(Modifier.width(6.dp)) }
                    Text(w.label, color = if (on) C.night else C.text, fontSize = 12.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Text(tr("منبع: NASA SDO", "Source: NASA SDO"), color = C.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
    }
}
