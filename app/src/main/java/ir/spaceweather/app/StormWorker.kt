package ir.spaceweather.app

import ir.spaceweather.app.data.tr
import ir.spaceweather.app.data.L

import android.content.Context
import androidx.work.*
import ir.spaceweather.app.data.Prefs
import ir.spaceweather.app.data.StormLevels
import ir.spaceweather.app.data.Storms
import java.util.concurrent.TimeUnit

/**
 * هر ۱ ساعت (همان آهنگ بروزرسانی سرور) سطح R / G / S را بررسی می‌کند.
 * اعلان وقتی می‌آید که سطح از ۰ بیشتر شود یا از آخرین سطح اعلان‌شده بالاتر برود
 * (مثلاً G1 → G2). وقتی به ۰ برگشت، دفعهٔ بعد دوباره اعلان داده می‌شود.
 */
class StormWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val p = Prefs(applicationContext)
        if (!p.stormNotify) return Result.success()
        L.init(applicationContext)
        val lv = runCatching { Storms.fetch() }.getOrNull() ?: return Result.retry()
        notifyIfRaised(applicationContext, p, lv)
        return Result.success()
    }

    companion object {
        private const val NAME = "sw_storms"

        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<StormWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
        }

        fun cancel(ctx: Context) = WorkManager.getInstance(ctx).cancelUniqueWork(NAME)

        fun notifyIfRaised(ctx: Context, p: Prefs, lv: StormLevels) {
            check(ctx, p, "R", lv.r, 2001,
                { n -> tr("⚠ طوفان رادیویی R$n", "⚠ Radio blackout R$n") },
                { n -> tr("اشعه ایکس خورشید به کلاس ${lv.rText} رسیده است (R$n). ارتباطات رادیویی HF در نیمکرهٔ روز ممکن است دچار اختلال شود.", "Solar X-rays have reached class ${lv.rText} (R$n). HF radio communication on the sunlit side of Earth may be disrupted.") })
            check(ctx, p, "G", lv.g, 2002,
                { n -> tr("⚠ طوفان ژئومغناطیسی G$n", "⚠ Geomagnetic storm G$n") },
                { n -> tr("${lv.gText} — طوفان ژئومغناطیسی G$n. احتمال نوسان در شبکه‌های برق، اختلال در GPS و دیده شدن شفق قطبی در عرض‌های بالا.", "${lv.gText} — geomagnetic storm G$n. Possible power-grid fluctuations, GPS disturbances and aurora at high latitudes.") })
            check(ctx, p, "S", lv.s, 2003,
                { n -> tr("⚠ طوفان تابشی S$n", "⚠ Solar radiation storm S$n") },
                { n -> tr("شار پروتون‌های پرانرژی به ${lv.sText} رسیده است (S$n). ماهواره‌ها و پروازهای قطبی ممکن است تحت تأثیر قرار بگیرند.", "Energetic proton flux has reached ${lv.sText} (S$n). Satellites and polar flights may be affected.") })
        }

        private fun check(
            ctx: Context, p: Prefs, letter: String, level: Int?, id: Int,
            title: (Int) -> String, body: (Int) -> String,
        ) {
            if (level == null) return
            val last = p.lastLevel(letter)
            if (level > 0 && level > last && p.scaleOn(letter)) {
                Notifier.postStorm(ctx, id, title(level), body(level))
            }
            if (level != last) p.setLastLevel(letter, level)
        }
    }
}
