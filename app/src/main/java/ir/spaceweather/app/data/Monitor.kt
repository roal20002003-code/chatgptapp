package ir.spaceweather.app.data

import org.json.JSONObject
import java.security.MessageDigest
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

/** تنظیمات بخش پایش — فقط برای مدیر */
object AdminConfig {
    /**
     * SHA-256 رمز ورود. رمز پیش‌فرض: Sw@1405  — حتماً عوضش کنید.
     * ساخت هش در PowerShell:
     * [BitConverter]::ToString([Security.Cryptography.SHA256]::Create().ComputeHash([Text.Encoding]::UTF8.GetBytes("رمز"))).Replace("-","").ToLower()
     */
    const val PASSWORD_SHA256 = "5ea9c02764d728a003dac4bb74d64c0180e57385ad0fde3cd3184872930bf4c2"

    /** اگر زمان یک پارامتر از این قدیمی‌تر باشد هشدار داده می‌شود (دقیقه) */
    const val STALE_MINUTES = 120L

    /**
     * زمان پارامترها در monitoring ("2026-11-08 18") به وقت UTC است یا تهران؟
     * داده‌های NOAA معمولاً UTC هستند. اگر در دیتابیس به وقت تهران ذخیره می‌کنید false کنید.
     */
    const val PARAM_TIME_IS_UTC = false
    /** اختلاف مجاز ساعت سرور با ساعت گوشی (دقیقه) */
    const val CLOCK_SKEW_MINUTES = 60L
    /** فاصلهٔ بررسی پس‌زمینه (حداقل ۱۵ دقیقه در اندروید) */
    const val CHECK_EVERY_MINUTES = 15L

    fun check(password: String): Boolean {
        val h = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return h == PASSWORD_SHA256
    }
}

object Tm {
    val TEHRAN: ZoneId = ZoneId.of("Asia/Tehran")
    private val rx = Regex("""(\d{4})[-/](\d{1,2})[-/](\d{1,2})[T ]+(\d{1,2})(?:[-:](\d{2}))?(?:[-:](\d{2}))?""")

    /** "2026-11-08T08-33-22" یا "2026-11-08T08:33:22.380" */
    fun parse(s: String?): LocalDateTime? {
        val m = rx.find(s ?: return null) ?: return null
        val g = m.groupValues
        return runCatching {
            LocalDateTime.of(g[1].toInt(), g[2].toInt(), g[3].toInt(), g[4].toInt(), g[5].ifEmpty { "0" }.toInt(), g[6].ifEmpty { "0" }.toInt())
        }.getOrNull()
    }

    fun now(): LocalDateTime = LocalDateTime.now(TEHRAN)
    fun utcToTehran(t: LocalDateTime): LocalDateTime =
        t.atZone(java.time.ZoneOffset.UTC).withZoneSameInstant(TEHRAN).toLocalDateTime()
    fun minutes(from: LocalDateTime, to: LocalDateTime): Long = Duration.between(from, to).toMinutes()
    fun show(t: LocalDateTime?): String =
        t?.let { Fa.ltr(Fa.digits("%04d/%02d/%02d %02d:%02d:%02d".format(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute, it.second))) } ?: "—"
    fun span(min: Long?): String = when {
        min == null -> "—"
        kotlin.math.abs(min) < 60 -> Fa.digits("$min") + " دقیقه"
        kotlin.math.abs(min) < 24 * 60 -> Fa.digits("${min / 60}") + " ساعت و " + Fa.digits("${kotlin.math.abs(min % 60)}") + " دقیقه"
        else -> Fa.digits("${min / 1440}") + " روز و " + Fa.digits("${kotlin.math.abs((min % 1440) / 60)}") + " ساعت"
    }
}

data class EndpointCheck(
    val name: String, val url: String, val ok: Boolean,
    val ms: Long, val bytes: Int, val error: String?, val raw: String?,
)

data class Problem(val code: String, val message: String)

/** وضعیت یک پارامتر در monitoring.aspx */
data class ParamCheck(
    val key: String, val label: String, val value: String,
    val rawTime: String, val time: LocalDateTime?,   // به وقت تهران
    val ageMin: Long?,
) {
    val stale get() = time == null || (ageMin ?: 0) > AdminConfig.STALE_MINUTES
}

val PARAM_LABELS = linkedMapOf(
    "solar_wind_speed_km_s" to "سرعت باد خورشیدی",
    "bz_nT" to "Bz",
    "bt_nT" to "Bt",
    "kp_index" to "شاخص Kp",
    "f10_7_index" to "F10.7",
    "Radio Blackouts Storm" to "اشعه ایکس (Radio Blackouts)",
    "temp" to "دمای باد خورشیدی",
    "proton" to "شار پروتون",
    "dst_nt" to "Dst",
    "sun_spot" to "لکه‌های خورشیدی",
)

data class MonitorResult(
    val checkedAt: LocalDateTime,
    val serverTime: LocalDateTime?,      // از monitoring
    val lastData: LocalDateTime?,        // آخرین ثبت داده
    val lastDataSource: String,
    val dataAgeMin: Long?,               // سن داده
    val clockSkewMin: Long?,             // ساعت گوشی − ساعت سرور
    val monitorFields: List<Pair<String, String>>,
    val endpoints: List<EndpointCheck>,
    val params: List<ParamCheck> = emptyList(),
) {
    val problems: List<Problem> get() = buildList {
        // monitoring.aspx اگر هنوز روی سایت نیست (404) هشدار نده؛ از JSON ساعتی استفاده می‌شود
        endpoints.filter { !it.ok && !(it.name == "monitoring" && it.error?.contains("404") == true) }.forEach {
            add(Problem("down:${it.name}", "${it.name} پاسخ نمی‌دهد: ${it.error}"))
        }
        if (params.isNotEmpty()) {
            val stale = params.filter { it.stale }
            if (stale.size == params.size)
                add(Problem("stale:all", "هیچ‌کدام از پارامترها بروز نشده‌اند؛ آخرین ثبت ${Tm.span(params.mapNotNull { it.ageMin }.minOrNull())} پیش"))
            else stale.forEach {
                add(Problem("stale:${it.key}",
                    if (it.time == null) "${it.label}: زمان ثبت خوانده نشد (${it.rawTime})"
                    else "${it.label} از ${Tm.span(it.ageMin)} پیش بروز نشده"))
            }
        } else if (dataAgeMin != null && dataAgeMin > AdminConfig.STALE_MINUTES)
            add(Problem("stale", "داده‌ها بروز نشده‌اند؛ آخرین ثبت ${Tm.span(dataAgeMin)} پیش"))
        if (lastData == null && endpoints.any { it.name == "hourly" && it.ok })
            add(Problem("nodate", "تاریخ آخرین داده خوانده نشد"))
        if (clockSkewMin != null && kotlin.math.abs(clockSkewMin) > AdminConfig.CLOCK_SKEW_MINUTES)
            add(Problem("clock", "اختلاف ساعت سرور و گوشی ${Tm.span(clockSkewMin)}"))
    }
    val healthy get() = problems.isEmpty()
}

object Monitor {
    private suspend fun probe(name: String, url: String): EndpointCheck {
        val t0 = System.currentTimeMillis()
        return try {
            val raw = Api.get(url)
            EndpointCheck(name, url, true, System.currentTimeMillis() - t0, raw.length, null, raw)
        } catch (e: Exception) {
            EndpointCheck(name, url, false, System.currentTimeMillis() - t0, 0, e.message ?: e.javaClass.simpleName, null)
        }
    }

    suspend fun run(): MonitorResult {
        val eps = listOf(
            probe("hourly", Api.HOURLY),
            probe("monitoring", Api.MONITOR),
            probe("alerts", Api.ALERTS),
            probe("charts", Api.CHARTS),
        )
        return build(Tm.now(), eps)
    }

    /** بدون شبکه؛ برای تست هم استفاده می‌شود */
    fun build(now: LocalDateTime, eps: List<EndpointCheck>): MonitorResult {

        // monitoring.aspx (اختیاری)
        var serverTime: LocalDateTime? = null
        var monLast: LocalDateTime? = null
        val fields = ArrayList<Pair<String, String>>()
        val params = ArrayList<ParamCheck>()
        eps.first { it.name == "monitoring" }.raw?.let { raw ->
            runCatching {
                val o = JSONObject(Api.cleanJson(raw))
                serverTime = Tm.parse(o.optString("server_time").ifEmpty { o.optString("now") })
                o.keys().forEach { k -> if (k != "params" && k != "server_time") fields += k to o.opt(k).toString() }
                val ref = serverTime ?: now
                o.optJSONObject("params")?.let { p ->
                    val keys = p.keys().asSequence().toList()
                        .sortedBy { k -> PARAM_LABELS.keys.indexOf(k).let { if (it < 0) 99 else it } }
                    for (k in keys) {
                        val item = p.optJSONObject(k) ?: continue
                        val rawT = item.optString("time")
                        val parsed = Tm.parse(rawT)?.let { if (AdminConfig.PARAM_TIME_IS_UTC) Tm.utcToTehran(it) else it }
                        val v = item.opt("value")
                        val vs = (v as? Number)?.toDouble() ?: v?.toString()?.toDoubleOrNull()
                        params += ParamCheck(
                            key = k, label = PARAM_LABELS[k] ?: k,
                            value = vs?.let { d -> if (kotlin.math.abs(d) >= 100 || d == Math.floor(d)) Fa.num(d, 0) else Fa.num(d, 2) }
                                ?.let { s -> if (k == "Radio Blackouts Storm") "$s  (${Status.classFromRadio(vs)})" else s }
                                ?: (v?.toString() ?: "—"),
                            rawTime = rawT, time = parsed,
                            ageMin = parsed?.let { Tm.minutes(it, ref) },
                        )
                    }
                }
                monLast = params.mapNotNull { it.time }.maxOrNull()
                    ?: Tm.parse(o.optString("last_update").ifEmpty { o.optString("last_data") })
            }
        }

        // datetime داخل JSON ساعتی
        val hourlyTime = eps.first { it.name == "hourly" }.raw?.let { raw ->
            runCatching { Tm.parse(Api.parseHourly(Api.cleanJson(raw)).datetime) }.getOrNull()
        }

        val lastData = monLast ?: hourlyTime
        val source = if (monLast != null) "monitoring" else "hourly"
        // اگر ساعت سرور داریم سن داده با ساعت سرور، وگرنه با ساعت گوشی (به وقت تهران)
        val age = lastData?.let { Tm.minutes(it, serverTime ?: now) }
        val skew = serverTime?.let { Tm.minutes(it, now) }

        return MonitorResult(now, serverTime, lastData, source, age, skew, fields, eps, params)
    }
}
