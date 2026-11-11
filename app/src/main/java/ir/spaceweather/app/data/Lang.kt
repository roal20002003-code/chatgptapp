package ir.spaceweather.app.data

import android.content.Context

/**
 * زبان برنامه: فارسی (پیش‌فرض گوشی‌های فارسی) یا انگلیسی.
 * هر متن با tr("فارسی", "English") نوشته می‌شود.
 */
object L {
    @Volatile var en: Boolean = false

    fun init(ctx: Context) { en = Prefs(ctx).lang == "en" }
}

fun tr(fa: String, en: String): String = if (L.en) en else fa
