package ir.spaceweather.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object Api {
    const val BASE = "http://spaceweather.ir"   // تا اصلاح گواهی SSL سرور، همه درخواست‌ها با http
    const val HOURLY = "$BASE/hourly_solar_activity.aspx"
    const val CHARTS = "$BASE/ChartData.ashx"
    const val ALERTS = "$BASE/data1.aspx?id=alert"
    /** JSON پایش که روی سرور می‌سازید (آدرس را در صورت نیاز عوض کنید) */
    const val MONITOR = "$BASE/monitoring.aspx"
    /** دریافت پیام کاربران (AppMessage.ashx) */
    const val MESSAGE = "$BASE/AppMessage.ashx"

    val CHART_KEYS = listOf("sfu", "xray", "wind", "bt", "bz", "dens", "kp", "dst", "elec", "proton", "temp")

    // ---------- HTTP ----------
    /** ارسال پیام کاربر؛ خروجی: (موفق؟، پیام سرور) */
    suspend fun sendMessage(name: String, contact: String, body: String, device: String, version: String): Pair<Boolean, String> =
        try { postMessage(MESSAGE, name, contact, body, device, version) } catch (e: javax.net.ssl.SSLException) {
            postMessage("http://" + MESSAGE.removePrefix("https://"), name, contact, body, device, version)
        }

    private suspend fun postMessage(target: String, name: String, contact: String, body: String, device: String, version: String): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
            val form = "name=${enc(name)}&contact=${enc(contact)}&body=${enc(body)}&device=${enc(device)}&version=${enc(version)}"
            val c = URL(target).openConnection() as HttpURLConnection
            try {
                c.requestMethod = "POST"; c.doOutput = true
                c.connectTimeout = 15_000; c.readTimeout = 20_000
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                c.outputStream.use { it.write(form.toByteArray(Charsets.UTF_8)) }
                val code = c.responseCode
                val txt = (if (code in 200..299) c.inputStream else c.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                val o = runCatching { JSONObject(cleanJson(txt)) }.getOrNull()
                val ok = code in 200..299 && (o?.optBoolean("ok", true) ?: true)
                ok to (o?.optString("message")?.takeIf { it.isNotBlank() } ?: if (ok) tr("پیام شما ارسال شد", "Your message was sent") else tr("خطا ($code)", "Error ($code)"))
            } finally { c.disconnect() }
        }

    /**
     * اگر گواهی SSL سایت روی اندروید پذیرفته نشد (زنجیرهٔ ناقص گواهی روی سرور)،
     * همان آدرس با http خوانده می‌شود. بعد از اصلاح گواهی روی IIS دیگر لازم نیست.
     */
    internal suspend fun get(url: String): String =
        try { getOnce(url) } catch (e: javax.net.ssl.SSLException) {
            if (url.startsWith("https://")) getOnce("http://" + url.removePrefix("https://")) else throw e
        }

    private suspend fun getOnce(url: String): String = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 25_000
        c.setRequestProperty("Accept", "application/json, */*")
        c.setRequestProperty("User-Agent", "SpaceWeatherApp/1.0 (Android)")
        try {
            val code = c.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    /** اگر https جواب نداد، http را امتحان می‌کند (برای data1.aspx) */
    private suspend fun getWithFallback(url: String): String =
        try { get(url) } catch (e: Exception) {
            if (url.startsWith("https://")) get("http://" + url.removePrefix("https://")) else throw e
        }

    /** BOM و هر HTML اطراف JSON را حذف می‌کند */
    internal fun cleanJson(raw: String): String {
        val s = raw.trimStart('﻿', ' ', '\n', '\r', '\t')
        val iObj = s.indexOf('{'); val iArr = s.indexOf('[')
        val start = listOf(iObj, iArr).filter { it >= 0 }.minOrNull() ?: return s
        val end = if (s[start] == '{') s.lastIndexOf('}') else s.lastIndexOf(']')
        return if (end > start) s.substring(start, end + 1) else s
    }

    // ---------- helpers ----------
    private fun JSONObject.num(vararg keys: String): Double? {
        for (k in keys) {
            if (!has(k) || isNull(k)) continue
            when (val v = opt(k)) {
                is Number -> return v.toDouble()
                is String -> v.replace(",", ".").filter { it.isDigit() || it == '.' || it == '-' }
                    .toDoubleOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun JSONObject.str(vararg keys: String): String? {
        for (k in keys) {
            if (has(k) && !isNull(k)) {
                val v = opt(k)?.toString()?.trim()
                if (!v.isNullOrEmpty()) return v
            }
        }
        return null
    }

    // ---------- hourly ----------
    suspend fun hourly(): Hourly = parseHourly(cleanJson(get(HOURLY)))

    fun parseHourly(json: String): Hourly {
        val root = JSONObject(json)
        val p = root.optJSONObject("parameters") ?: root
        return Hourly(
            datetime = root.str("datetime", "updated", "time") ?: "",
            windSpeed = p.num("solar_wind_speed_km_s", "wind", "solar_wind_speed"),
            f107 = p.num("f10_7_index", "f107", "sfu"),
            kp = p.num("kp_index", "kp"),
            bz = p.num("bz_nT", "bz_nt", "bz"),
            bt = p.num("bt_nT", "bt_nt", "bt"),
            dst = p.num("dst_nt", "dst_nT", "dst"),
            radioBlackout = p.num("Radio Blackouts Storm", "radio_blackouts"),
            sunspot = p.num("sun_spot", "sunspot"),
            xrayClass = p.str("xray_class", "x_ray_class", "xray"),
            flareProbC = p.num("flare_prob_c", "c_flare_probability"),
            flareProbM = p.num("flare_prob_m", "m_flare_probability"),
            flareProbX = p.num("flare_prob_x", "x_flare_probability"),
            source = root.str("source"),
            notes = root.str("notes"),
        )
    }

    // ---------- charts ----------
    suspend fun charts(): Charts = parseCharts(cleanJson(get(CHARTS)))

    fun parseCharts(json: String): Charts {
        var root = JSONObject(json)
        root.optJSONObject("data")?.let { root = it }
        root.optJSONObject("series")?.let { root = it }
        val map = LinkedHashMap<String, List<Double?>>()
        for (k in CHART_KEYS) {
            val arr = root.optJSONArray(k) ?: continue
            map[k] = List(arr.length()) { i -> value(arr.opt(i)) }
        }
        return Charts(map)
    }

    private fun value(v: Any?): Double? = when (v) {
        null, JSONObject.NULL -> null
        is Number -> v.toDouble().takeIf { !it.isNaN() }
        is String -> v.toDoubleOrNull()
        is JSONObject -> v.num("v", "value", "y")
        is JSONArray -> value(v.opt(v.length() - 1))
        else -> null
    }

    // ---------- alerts ----------
    suspend fun alerts(): List<Alert> = parseAlerts(cleanJson(getWithFallback(ALERTS)))

    fun parseAlerts(json: String): List<Alert> {
        val t = json.trim()
        val arr: JSONArray = if (t.startsWith("[")) JSONArray(t) else {
            val o = JSONObject(t)
            listOf("alerts", "alert", "data", "items", "Table", "rows")
                .firstNotNullOfOrNull { o.optJSONArray(it) }
                ?: o.keys().asSequence().firstNotNullOfOrNull { o.optJSONArray(it) }
                ?: JSONArray().put(o)
        }
        val out = ArrayList<Alert>()
        for (i in 0 until arr.length()) {
            when (val e = arr.opt(i)) {
                is JSONObject -> {
                    val text = e.str("peralert", "alert", "text", "message", "متن", "اخطار", "متن اخطار")
                        ?: e.keys().asSequence().map { e.optString(it) }.maxByOrNull { it.length } ?: ""
                    out += Alert(
                        id = e.str("alertid", "id", "code", "شماره", "کد") ?: "",
                        // تاریخ شمسی در اولویت است
                        date = e.keys().asSequence().firstOrNull { it.contains("شمسی") }?.let { e.optString(it) }
                            ?: e.str("shamsi", "alertdate", "date", "تاریخ") ?: "",
                        text = text,
                        enText = e.str("enalert", "en_alert", "alert_en", "englishalert") ?: "",
                    )
                }
                is JSONArray -> out += Alert(
                    id = e.optString(0), date = e.optString(1), text = e.optString(e.length() - 1)
                )
                is String -> out += Alert("", "", e)
            }
        }
        return out.map { it.copy(text = cleanText(it.text), enText = cleanText(it.enText), id = it.id.trim()) }
            .filter { it.text.isNotBlank() }.take(4)
    }

    /** "شاخص Kp_index_ژئومغناطیسی 5" → "شاخص Kp ژئومغناطیسی 5" */
    fun cleanText(s: String): String = s
        .replace(Regex("""Kp\s*[_)(]\s*index\s*_?"""), "Kp ")
        .replace('_', ' ')
        .replace(Regex("""\s+"""), " ")
        .trim()
}
