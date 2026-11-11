package ir.spaceweather.app

import ir.spaceweather.app.data.tr
import ir.spaceweather.app.data.L

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import ir.spaceweather.app.data.AdminConfig
import ir.spaceweather.app.data.Monitor
import ir.spaceweather.app.data.Prefs
import ir.spaceweather.app.data.Tm
import java.util.concurrent.TimeUnit

/** بررسی دوره‌ای سرور در پس‌زمینه؛ فقط وقتی مدیر نوتیف را روشن کرده باشد */
class MonitorWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val p = Prefs(applicationContext)
        L.init(applicationContext)
        if (!p.adminUnlocked || !p.notifyEnabled) return Result.success()

        val r = runCatching { Monitor.run() }.getOrNull()
        p.lastCheck = Tm.show(Tm.now()) + if (r == null || !r.healthy) " — مشکل" else " — سالم"

        var problems = r?.problems ?: emptyList()
        // قطعی شبکه: فقط بعد از ۲ بار پیاپی خبر بده (خطای لحظه‌ای اینترنت گوشی را نادیده بگیر)
        val down = problems.filter { it.code.startsWith("down:") }
        if (r == null || down.isNotEmpty()) {
            p.failStreak = p.failStreak + 1
            if (p.failStreak < 2) problems = problems - down.toSet()
        } else p.failStreak = 0

        val key = problems.map { it.code }.sorted().joinToString("|")
        val nowMs = System.currentTimeMillis()
        if (problems.isNotEmpty()) {
            // مشکل جدید → فوراً؛ همان مشکل → هر ۲ ساعت یادآوری
            if (key != p.lastAlertKey || nowMs - p.lastAlertAt > 2 * 3600_000L) {
                Notifier.post(applicationContext, "⚠ مشکل در بروزرسانی SpaceWeather", problems.joinToString("\n") { it.message })
                p.lastAlertKey = key; p.lastAlertAt = nowMs
            }
        } else if (p.lastAlertKey.isNotEmpty() && r != null) {
            Notifier.post(applicationContext, "✓ بروزرسانی برقرار شد", "آخرین داده: ${Tm.show(r.lastData)}")
            p.lastAlertKey = ""
        }
        return Result.success()
    }

    companion object {
        private const val NAME = "sw_monitor"

        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<MonitorWorker>(AdminConfig.CHECK_EVERY_MINUTES, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
        }

        fun cancel(ctx: Context) = WorkManager.getInstance(ctx).cancelUniqueWork(NAME)
    }
}

object Notifier {
    private const val CHANNEL = "monitor"
    const val STORM_CHANNEL = "storms"

    fun post(ctx: Context, title: String, text: String) =
        show(ctx, CHANNEL, "پایش سرور", "هشدار بروز نشدن داده‌های سایت", 1001, title, text, openAdmin = true)

    fun postStorm(ctx: Context, id: Int, title: String, text: String) =
        show(ctx, STORM_CHANNEL, tr("طوفان‌های هواشناسی فضایی", "Space weather storms"), tr("اعلان طوفان رادیویی، ژئومغناطیسی و تابشی", "Radio blackout, geomagnetic and radiation storm notifications"), id, title, text, openAdmin = false)

    private fun show(
        ctx: Context, channel: String, channelName: String, channelDesc: String,
        id: Int, title: String, text: String, openAdmin: Boolean,
    ) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(channel) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channel, channelName, NotificationManager.IMPORTANCE_HIGH).apply { description = channelDesc }
            )
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val open = PendingIntent.getActivity(
            ctx, id,
            Intent(ctx, MainActivity::class.java).putExtra("open_admin", openAdmin).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_monitor)
            .setContentTitle(title)
            .setContentText(text.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}
