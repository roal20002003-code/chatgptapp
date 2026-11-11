package ir.spaceweather.app.data

/** آخرین داده‌های ساعتی از /data/hourly_solar_activity.aspx */
data class Hourly(
    val datetime: String,          // "2026-11-08T08-33-22"
    val windSpeed: Double?,        // km/s
    val f107: Double?,             // sfu
    val kp: Double?,
    val bz: Double?,               // nT
    val bt: Double?,               // nT
    val dst: Double?,              // nT
    val radioBlackout: Double?,    // "Radio Blackouts Storm"
    val sunspot: Double?,
    // فیلدهای اختیاری؛ اگر سرور اضافه کند در گزارش نمایش داده می‌شوند
    val xrayClass: String?,        // مثل "C1.1"
    val flareProbC: Double?,       // درصد
    val flareProbM: Double?,       // درصد
    val flareProbX: Double?,       // درصد
    val source: String?,
    val notes: String?,
)

/** یک اخطار از data1.aspx?id=alert */
data class Alert(
    val id: String,
    val date: String,
    val text: String,
    val enText: String = "",   // اگر دیتابیس متن انگلیسی بدهد (enalert)
) {
    /** متن مناسب زبان فعلی */
    val shown: String get() = if (L.en && enText.isNotBlank()) enText else text
}

/** سری‌های ۷۲۰ ساعته از ChartData.ashx (قدیمی ← جدید) */
data class Charts(val series: Map<String, List<Double?>>) {
    fun last(key: String): Double? = series[key]?.lastOrNull { it != null }
}

data class UiState(
    val loading: Boolean = true,
    val hourly: Hourly? = null,
    val alerts: List<Alert> = emptyList(),
    val charts: Charts = Charts(emptyMap()),
    val errors: List<String> = emptyList(),
)
