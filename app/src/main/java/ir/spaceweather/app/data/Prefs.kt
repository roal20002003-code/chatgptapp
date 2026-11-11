package ir.spaceweather.app.data

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("sw_prefs", Context.MODE_PRIVATE)

    var adminUnlocked: Boolean
        get() = sp.getBoolean("admin", false)
        set(v) = sp.edit().putBoolean("admin", v).apply()

    var notifyEnabled: Boolean
        get() = sp.getBoolean("notify", false)
        set(v) = sp.edit().putBoolean("notify", v).apply()

    /** کد مشکلاتی که برایشان نوتیف داده شده (برای جلوگیری از تکرار) */
    var lastAlertKey: String
        get() = sp.getString("alertKey", "") ?: ""
        set(v) = sp.edit().putString("alertKey", v).apply()

    var lastAlertAt: Long
        get() = sp.getLong("alertAt", 0)
        set(v) = sp.edit().putLong("alertAt", v).apply()

    var failStreak: Int
        get() = sp.getInt("fails", 0)
        set(v) = sp.edit().putInt("fails", v).apply()

    var lastCheck: String
        get() = sp.getString("lastCheck", "") ?: ""
        set(v) = sp.edit().putString("lastCheck", v).apply()

    /** "fa" یا "en"؛ بار اول از زبان گوشی */
    var lang: String
        get() = sp.getString("lang", null)
            ?: if (java.util.Locale.getDefault().language == "fa") "fa" else "en"
        set(v) = sp.edit().putString("lang", v).apply()

    // ---------- اعلان طوفان‌ها (برای همهٔ کاربران) ----------
    var stormNotify: Boolean
        get() = sp.getBoolean("stormNotify", false)
        set(v) = sp.edit().putBoolean("stormNotify", v).apply()

    /** letter = "R" | "G" | "S" */
    fun scaleOn(letter: String): Boolean = sp.getBoolean("storm_on_$letter", true)
    fun setScaleOn(letter: String, v: Boolean) = sp.edit().putBoolean("storm_on_$letter", v).apply()

    /** آخرین سطحی که برایش اعلان داده شده */
    fun lastLevel(letter: String): Int = sp.getInt("storm_last_$letter", 0)
    fun setLastLevel(letter: String, v: Int) = sp.edit().putInt("storm_last_$letter", v).apply()
}
