package ir.spaceweather.app.data

import org.json.JSONObject
import kotlin.math.pow

/** سطح فعلی سه مقیاس NOAA؛ null یعنی داده در دسترس نبود */
data class StormLevels(
    val r: Int?, val rText: String?,   // طوفان رادیویی ← کلاس اشعه ایکس
    val g: Int?, val gText: String?,   // طوفان ژئومغناطیسی ← Kp
    val s: Int?, val sText: String?,   // طوفان تابشی ← پروتون ≥10MeV
)

object Storms {
    /** کلاس اشعه ایکس → R:  M1=R1  M5=R2  X1=R3  X10=R4  X20=R5 */
    fun rFromClass(cls: String?): Int? {
        val m = Regex("""([ABCMX])\s*([\d.]+)""").find(cls?.uppercase() ?: return null) ?: return null
        val base = when (m.groupValues[1]) { "A" -> 1e-8; "B" -> 1e-7; "C" -> 1e-6; "M" -> 1e-5; else -> 1e-4 }
        val flux = base * (m.groupValues[2].toDoubleOrNull() ?: return null)
        return listOf(1e-5, 5e-5, 1e-4, 1e-3, 2e-3).count { flux >= it - 1e-12 }
    }

    /** Kp → G:  5=G1  6=G2  7=G3  8=G4  9=G5 */
    fun gFromKp(kp: Double?): Int? = kp?.let { (kotlin.math.floor(it + 1e-9).toInt() - 4).coerceIn(0, 5) }

    /** پروتون (pfu) → S:  10=S1  100=S2  1e3=S3  1e4=S4  1e5=S5 */
    fun sFromPfu(p: Double?): Int? = p?.let { v -> listOf(10.0, 1e2, 1e3, 1e4, 1e5).count { v >= it } }

    suspend fun fetch(): StormLevels {
        val h = runCatching { Api.hourly() }.getOrNull()
        val cls = h?.xrayClass ?: Status.classFromRadio(h?.radioBlackout)
        val kp = h?.kp

        // پروتون: اول از monitoring (ستون logg = log10 pfu)، وگرنه آخرین مقدار ChartData
        var pfu: Double? = runCatching {
            val o = JSONObject(Api.cleanJson(Api.get(Api.MONITOR)))
            val v = o.getJSONObject("params").getJSONObject("proton").opt("value")
            val d = (v as? Number)?.toDouble() ?: v.toString().toDouble()
            10.0.pow(d)
        }.getOrNull()
        if (pfu == null) pfu = runCatching {
            val s = Api.charts().series["proton"].orEmpty()
            val last = s.lastOrNull { it != null }!!
            if (s.any { it != null && it < 0 }) 10.0.pow(last) else last
        }.getOrNull()

        return StormLevels(
            r = rFromClass(cls), rText = cls,
            g = gFromKp(kp), gText = kp?.let { "Kp " + Fa.num(it, 0) },
            s = sFromPfu(pfu), sText = pfu?.let { Fa.ltr(Fa.num(it, if (it < 10) 2 else 0) + " pfu") },
        )
    }
}
