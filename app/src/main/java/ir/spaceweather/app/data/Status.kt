package ir.spaceweather.app.data

import java.util.Locale

/**
 * تشخیص آرام / فعال برای سه صحنه، بر اساس آستانه‌های NOAA SWPC:
 *  خورشید : شار اشعه ایکس ≥ M1 (1e-5 W/m²)
 *  باد    : سرعت ≥ 500 km/s یا Bt ≥ 10 nT یا چگالی ≥ 20 p/cm³
 *  جهت    : Bz < 0 جنوب‌سو (اتصال به مغناطیس‌کره زمین)، وگرنه شمال‌سو
 *  زمین   : Kp ≥ 5 (طوفان G1) یا Dst ≤ -50 nT
 */
enum class Level { CALM, ACTIVE, UNKNOWN }

data class SceneStatus(
    val sun: Level, val sunNote: String,
    val wind: Level, val windNote: String, val bzSouth: Boolean?,
    val earth: Level, val earthNote: String,
)

object Status {
    const val XRAY_M1 = 1e-5
    const val WIND_FAST = 500.0
    const val BT_STRONG = 10.0
    const val DENS_HIGH = 20.0
    const val KP_STORM = 5.0
    const val DST_STORM = -50.0

    /** شار اشعه ایکس (W/m²) → کلاس، مثل 1.1e-6 → "C1.1" */
    fun xrayClass(flux: Double?): String? {
        if (flux == null || flux <= 0) return null
        val (letter, base) = when {
            flux >= 1e-4 -> "X" to 1e-4
            flux >= 1e-5 -> "M" to 1e-5
            flux >= 1e-6 -> "C" to 1e-6
            flux >= 1e-7 -> "B" to 1e-7
            else -> "A" to 1e-8
        }
        return letter + String.format(Locale.US, "%.1f", flux / base)
    }

    /**
     * "Radio Blackouts Storm" سایت → کلاس اشعه ایکس
     *  ۱–۹ A ، ۹–۱۸ B ، ۱۸–۲۷ C ، ۲۷–۳۶ M ، بالای ۳۶ X
     *  عدد کلاس = مقدار منهای ابتدای بازه؛ مثلاً 19.1 → C1.1
     */
    fun classFromRadio(v: Double?): String? {
        if (v == null || v <= 0) return null
        val (letter, base) = when {
            v <= 9 -> "A" to 0.0
            v <= 18 -> "B" to 9.0
            v <= 27 -> "C" to 18.0
            v <= 36 -> "M" to 27.0
            else -> "X" to 36.0
        }
        return letter + String.format(Locale.US, "%.1f", v - base)
    }

    /** شاخص ۰–۶۰ (۱–۹ A، ۹–۱۸ B، …) → شار W/m² */
    fun radioIndexToFlux(v: Double?): Double? {
        if (v == null || v <= 0) return null
        return when {
            v <= 9 -> v * 1e-8
            v <= 18 -> maxOf(v - 9, .1) * 1e-7
            v <= 27 -> maxOf(v - 18, .1) * 1e-6
            v <= 36 -> maxOf(v - 27, .1) * 1e-5
            else -> maxOf(v - 36, .1) * 1e-4
        }
    }

    /** سری xray در ChartData اگر شاخص ۰–۶۰ باشد به شار تبدیل می‌شود (شار واقعی همیشه < 0.01 است) */
    fun xrayFlux(s: List<Double?>): List<Double?> =
        if ((s.filterNotNull().maxOrNull() ?: 0.0) > 0.01) s.map { radioIndexToFlux(it) } else s

    /** اولویت: فیلد xray_class ← Radio Blackouts Storm ← آخرین مقدار نمودار */
    fun xrayClassOf(h: Hourly?, c: Charts): String? =
        h?.xrayClass ?: classFromRadio(h?.radioBlackout)
            ?: xrayClass(xrayFlux(c.series["xray"].orEmpty()).lastOrNull { it != null })

    fun compute(h: Hourly?, c: Charts): SceneStatus {
        // خورشید: فعال اگر کلاس M یا X باشد
        val cls = xrayClassOf(h, c)
        val sun = if (cls == null) Level.UNKNOWN
            else if (cls.first().uppercaseChar() in "MX") Level.ACTIVE else Level.CALM
        val sunNote = cls?.let { tr("اشعه ایکس $it", "X-ray $it") } ?: "—"

        // باد خورشیدی
        val v = h?.windSpeed ?: c.last("wind")
        val bt = h?.bt ?: c.last("bt")
        val bz = h?.bz ?: c.last("bz")
        val dens = c.last("dens")
        val wind = if (v == null && bt == null) Level.UNKNOWN else if (
            (v ?: 0.0) >= WIND_FAST || (bt ?: 0.0) >= BT_STRONG || (dens ?: 0.0) >= DENS_HIGH
        ) Level.ACTIVE else Level.CALM
        val windNote = v?.let { Fa.ltr("${Fa.num(it, 0)} km/s") } ?: "—"

        // زمین
        val kp = h?.kp ?: c.last("kp")
        val dst = h?.dst ?: c.last("dst")
        val earth = if (kp == null && dst == null) Level.UNKNOWN else if (
            (kp ?: 0.0) >= KP_STORM || (dst ?: 0.0) <= DST_STORM
        ) Level.ACTIVE else Level.CALM
        val earthNote = kp?.let { "Kp ${Fa.num(it, 0)}" } ?: "—"

        return SceneStatus(sun, sunNote, wind, windNote, bz?.let { it < 0 }, earth, earthNote)
    }

    /** متن گزارش کوتاه، مثل نمونه سایت */
    fun report(h: Hourly?, c: Charts): String {
        if (h == null) return tr("در حال دریافت آخرین داده‌ها…", "Loading the latest data…")
        if (L.en) return reportEn(h, c)
        val parts = ArrayList<String>()
        h.windSpeed?.let { parts += "سرعت باد خورشیدی ${Fa.num(it, 0)} کیلومتر بر ثانیه" }
        h.kp?.let { parts += "شاخص Kp برابر ${Fa.num(it, 0)}" }
        xrayClassOf(h, c)?.let { parts += "کلاس اشعه ایکس در حدود $it" }
        h.f107?.let { parts += "شار رادیویی خورشید در طول موج ۱۰٫۷ سانتی‌متر ${Fa.num(it, 0)} sfu" }
        var s = if (parts.isEmpty()) "" else parts.dropLast(1).joinToString("، ") +
                (if (parts.size > 1) " و " else "") + parts.last() + " است."
        val probs = listOfNotNull(
            h.flareProbC?.let { "کلاس C حدود ${Fa.num(it, 0)}٪" },
            h.flareProbM?.let { "کلاس M حدود ${Fa.num(it, 0)}٪" },
            h.flareProbX?.let { "کلاس X ${if (it < 50) "تنها" else "حدود"} ${Fa.num(it, 0)}٪" },
        )
        if (probs.isNotEmpty()) {
            s += " احتمال شراره " + (if (probs.size == 1) probs[0]
                else probs.dropLast(1).joinToString("، ") + " و " + probs.last()) + " برآورد شده است."
        }
        return s
    }
}

/** English short report (same template as the English website) */
private fun reportEn(h: Hourly, c: Charts): String {
    val parts = ArrayList<String>()
    h.windSpeed?.let { parts += "solar wind speed is ${Fa.num(it, 0)} km/s" }
    h.kp?.let { parts += "the Kp index is ${Fa.num(it, 0)}" }
    Status.xrayClassOf(h, c)?.let { parts += "the X-ray flux is around class $it" }
    h.f107?.let { parts += "the 10.7 cm solar radio flux is ${Fa.num(it, 0)} sfu" }
    var s = if (parts.isEmpty()) "" else (parts.dropLast(1).joinToString(", ") +
            (if (parts.size > 1) " and " else "") + parts.last() + ".").replaceFirstChar { it.uppercase() }
    val probs = listOfNotNull(
        h.flareProbC?.let { "C-class flare is about ${Fa.num(it, 0)}%" },
        h.flareProbM?.let { "M-class ${Fa.num(it, 0)}%" },
        h.flareProbX?.let { "X-class ${if (it < 50) "only " else ""}${Fa.num(it, 0)}%" },
    )
    if (probs.isNotEmpty()) s += " The probability of a " + (if (probs.size == 1) probs[0]
        else probs.dropLast(1).joinToString(", ") + " and " + probs.last()) + "."
    return s
}

/** اعداد و تاریخ فارسی */
object Fa {
    private const val DIGITS = "۰۱۲۳۴۵۶۷۸۹"
    fun digits(s: String): String = if (L.en) s.replace('−', '-') else buildString {
        for (ch in s) append(
            when (ch) {
                in '0'..'9' -> DIGITS[ch - '0']
                '.' -> '٫'
                '-' -> '−'
                else -> ch
            }
        )
    }

    /** متن چپ‌به‌راست داخل متن فارسی (برای اعداد منفی و واحدها) */
    fun ltr(s: String): String = "\u2066$s\u2069"

    fun num(v: Double, decimals: Int = 0): String {
        val s = digits(String.format(Locale.US, "%.${decimals}f", v))
        return if (s.startsWith("−")) ltr(s) else s   // تا علامت منفی سمت چپ عدد بماند
    }

    private val MONTHS = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    fun toJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 + gd + gdm[gm - 1]
        var jy = -1595 + 33 * (days / 12053); days %= 12053
        jy += 4 * (days / 1461); days %= 1461
        if (days > 365) { jy += (days - 1) / 365; days = (days - 1) % 365 }
        val jm = if (days < 186) 1 + days / 31 else 7 + (days - 186) / 30
        val jd = 1 + if (days < 186) days % 31 else (days - 186) % 30
        return Triple(jy, jm, jd)
    }

    private val EN_MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    /** "2026-11-08T08-33-22" → "۱۷ آبان ۱۴۰۵ — ساعت ۰۸:۳۳"  |  English: "8 Nov 2026 · 08:33 (Tehran)" */
    fun updated(dt: String): String {
        val m = Regex("""(\d{4})-(\d{1,2})-(\d{1,2})[T ](\d{1,2})[-:](\d{2})""").find(dt) ?: return digits(dt)
        val (y, mo, d, hh, mm) = m.destructured
        if (L.en) return "${d.toInt()} ${EN_MONTHS[mo.toInt() - 1]} $y · ${hh.padStart(2, '0')}:$mm (Tehran)"
        val (jy, jm, jd) = toJalali(y.toInt(), mo.toInt(), d.toInt())
        return digits("$jd ") + MONTHS[jm - 1] + digits(" $jy") + " — ساعت " + digits("${hh.padStart(2, '0')}:$mm")
    }

    /**
     * "2026-11-08T02:03:49.380" → "۲ روز پیش" / "۵ ساعت پیش" / "۱۰ دقیقه پیش"
     * زمان ثبت اخطار به وقت تهران فرض شده است.
     */
    fun ago(dt: String, now: java.time.ZonedDateTime = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Tehran"))): String? {
        val m = Regex("""(\d{4})-(\d{1,2})-(\d{1,2})[T ](\d{1,2}):(\d{2})""").find(dt) ?: return null
        val (y, mo, d, hh, mm) = m.destructured
        val t = java.time.ZonedDateTime.of(y.toInt(), mo.toInt(), d.toInt(), hh.toInt(), mm.toInt(), 0, 0, now.zone)
        val min = java.time.Duration.between(t, now).toMinutes()
        return when {
            min < 1 -> tr("همین حالا", "just now")
            min < 60 -> digits("$min") + tr(" دقیقه پیش", " min ago")
            min < 24 * 60 -> digits("${min / 60}") + tr(" ساعت پیش", " h ago")
            else -> digits("${min / (24 * 60)}") + tr(" روز پیش", if (min / 1440 == 1L) " day ago" else " days ago")
        }
    }

    fun dateHour(y: Int, mo: Int, d: Int, h: Int): String {
        if (L.en) return "$d ${EN_MONTHS[mo - 1]} · " + String.format(Locale.US, "%02d:00", h)
        val (jy, jm, jd) = toJalali(y, mo, d)
        return digits("$jd ") + MONTHS[jm - 1] + " — ساعت " + digits(String.format(Locale.US, "%02d:00", h))
    }
}
