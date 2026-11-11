package ir.spaceweather.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

/** اطلاعات آخرین نسخه از جدول version سایت */
data class UpdateInfo(
    val version: String,
    val url: String,       // آدرس کامل و بررسی‌شده روی spaceweather.ir
    val force: Boolean,    // true = تا بروزرسانی نشود اپ باز نمی‌شود
    val notes: String,
)

object Updater {
    /**
     * بررسی نسخه هنگام باز شدن اپ.
     * true = روشن. برای خاموش کردن false کنید.
     */
    const val ENABLED = true

    /** JSON نسخه؛ خروجی select جدول app_version */
    const val VERSION_URL = "${Api.BASE}/appversion.aspx"

    /** فقط این دامنه برای دانلود پذیرفته می‌شود */
    private const val HOST = "spaceweather.ir"

    suspend fun check(): UpdateInfo? = parse(Api.get(VERSION_URL))

    fun parse(raw: String): UpdateInfo? {
        val j = Api.cleanJson(raw).trim()
        val o = if (j.startsWith("[")) JSONArray(j).optJSONObject(0) ?: return null else JSONObject(j)
        val version = o.optString("version").trim()
        if (!Regex("""\d+(\.\d+){0,3}""").matches(version)) return null
        val url = safeUrl(o.optString("url").ifEmpty { o.optString("link").ifEmpty { o.optString("path") } }) ?: return null
        val force = when (val f = o.opt("force")) {
            is Boolean -> f
            is Number -> f.toInt() != 0
            is String -> f == "1" || f.equals("true", true)
            else -> true
        }
        return UpdateInfo(version, url, force, o.optString("notes").trim())
    }

    /**
     * پروتکل دانلود APK: همان پروتکل Api.BASE (فعلاً http به‌خاطر گواهی SSL سرور).
     * امن است چون اندروید آپدیت را فقط با همان مهر امضای نسخهٔ نصب‌شده قبول می‌کند.
     * بعد از اصلاح گواهی، BASE را https کنید تا این هم خودکار https شود.
     */
    private val SCHEME = if (Api.BASE.startsWith("https")) "https" else "http"

    /**
     * فقط مسیر روی spaceweather.ir قبول می‌شود، مثل "/app/abc.apk".
     * اگر آدرس کامل داده شود فقط http(s)://spaceweather.ir/... (یا www.) پذیرفته است.
     * هر چیز دیگری (دامنهٔ دیگر، پورت، user@، ..، غیر apk) رد می‌شود.
     */
    fun safeUrl(input: String?): String? {
        var s = (input ?: return null).trim().trimEnd('/')
        if (s.isEmpty() || s.contains('\\') || s.contains("..") || s.startsWith("//")) return null
        if (s.startsWith("/")) s = "$SCHEME://$HOST$s"
        val u = runCatching { URI(s) }.getOrNull() ?: return null
        if (u.scheme != "https" && u.scheme != "http") return null
        val h = u.host?.lowercase() ?: return null
        if (h != HOST && h != "www.$HOST") return null
        if (u.userInfo != null || u.port != -1) return null
        val path = u.rawPath ?: return null
        if (!path.lowercase().endsWith(".apk") || path.startsWith("//")) return null
        return "$SCHEME://$h$path"
    }

    /** آیا نسخهٔ سرور از نسخهٔ نصب‌شده جدیدتر است؟ (1.0.10 > 1.0.9) */
    fun isNewer(server: String, installed: String): Boolean {
        val a = server.split('.').map { it.toIntOrNull() ?: 0 }
        val b = installed.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
