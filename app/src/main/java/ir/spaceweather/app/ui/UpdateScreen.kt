package ir.spaceweather.app.ui

import ir.spaceweather.app.data.tr
import ir.spaceweather.app.data.L

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.spaceweather.app.data.Fa
import ir.spaceweather.app.data.UpdateInfo
import ir.spaceweather.app.data.Updater

fun installedVersion(ctx: Context): String =
    runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "0"

/** دانلود APK با DownloadManager؛ بعد از پایان، با زدن نوتیف دانلود نصب می‌شود */
private fun download(ctx: Context, info: UpdateInfo): Boolean {
    val url = Updater.safeUrl(info.url) ?: return false   // بررسی دوباره دامنه
    return runCatching {
        val req = DownloadManager.Request(Uri.parse(url))
            .setTitle("SpaceWeather ${info.version}")
            .setDescription(tr("بروزرسانی برنامه", "App update"))
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "spaceweather-${info.version}.apk")
        ctx.getSystemService(DownloadManager::class.java).enqueue(req)
    }.isSuccess
}

/**
 * دوره تست: نسخه‌ها مهر امضای ثابت ندارند، پس نسخهٔ جدید روی قبلی نصب نمی‌شود.
 * برای همین به کاربر گفته می‌شود: ۱) نسخهٔ جدید را دانلود کن ۲) برنامهٔ فعلی را حذف کن ۳) فایل را نصب کن.
 */
@Composable
fun UpdateScreen(info: UpdateInfo, onLater: (() -> Unit)?) {
    val ctx = LocalContext.current
    val link = Updater.safeUrl(info.url) ?: ""
    var started by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(C.night).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(C.sun))
        Spacer(Modifier.height(16.dp))
        Text(tr("نسخهٔ جدید برنامه منتشر شده است", "A new version of the app is available"), color = C.text, fontWeight = FontWeight.Bold, fontSize = 19.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            tr("نسخهٔ نصب‌شده: ", "Installed: ") + Fa.ltr(installedVersion(ctx)) + tr("   ·   نسخهٔ جدید: ", "   ·   New: ") + Fa.ltr(info.version),
            color = C.muted, fontSize = 13.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Text(
            tr("لطفاً برنامهٔ فعلی را حذف کنید و نسخهٔ جدید را از لینک زیر دانلود و نصب کنید.", "Please uninstall the current app, then download and install the new version from the link below."),
            color = C.text, fontSize = 14.sp, lineHeight = 24.sp, textAlign = TextAlign.Center
        )
        if (info.notes.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                tr("تغییرات: ", "What’s new: ") + info.notes, color = C.muted, fontSize = 13.sp, lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.panel).padding(12.dp)
            )
        }

        // ---------- قدم ۱: دانلود ----------
        Step(tr("۱", "1"), tr("دانلود نسخهٔ جدید", "Download the new version")) {
            Button(
                onClick = { started = download(ctx, info) },
                colors = ButtonDefaults.buttonColors(containerColor = C.sun, contentColor = C.night),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (started) tr("در حال دانلود… (نوار اعلان را ببینید)", "Downloading… (see the notification bar)") else tr("دانلود فایل نصب", "Download the installer"), fontWeight = FontWeight.Bold) }
            TextButton(onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) } }) {
                Text(tr("یا دانلود با مرورگر", "or download with the browser"), color = C.sun, fontSize = 13.sp)
            }
            SelectionContainer {
                Text(Fa.ltr(link), color = C.muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }

        // ---------- قدم ۲: حذف برنامهٔ فعلی ----------
        Step(tr("۲", "2"), tr("حذف برنامهٔ فعلی", "Uninstall the current app")) {
            Text(tr("بعد از پایان دانلود، این دکمه را بزنید و در صفحهٔ باز شده «حذف» (Uninstall) را انتخاب کنید.", "When the download has finished, tap this button and choose “Uninstall” on the screen that opens."),
                color = C.muted, fontSize = 12.sp, lineHeight = 20.sp)
            OutlinedButton(
                onClick = {
                    runCatching {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            ) { Text(tr("باز کردن صفحهٔ حذف برنامه", "Open the uninstall screen"), color = C.storm) }
        }

        // ---------- قدم ۳: نصب ----------
        Step(tr("۳", "3"), tr("نصب نسخهٔ جدید", "Install the new version")) {
            Text(tr("فایل spaceweather-${info.version}.apk را از اعلان دانلود یا پوشهٔ Download گوشی باز کنید و «نصب» را بزنید.", "Open spaceweather-${info.version}.apk from the download notification or your phone’s Download folder and tap “Install”."),
                color = C.muted, fontSize = 12.sp, lineHeight = 20.sp)
        }

        if (onLater != null) TextButton(onClick = onLater, modifier = Modifier.padding(top = 8.dp)) { Text(tr("بعداً", "Later"), color = C.muted) }
    }
}

@Composable
private fun Step(num: String, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(14.dp)).background(C.panel).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(num, color = C.night, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.size(24.dp).clip(CircleShape).background(C.sun).padding(top = 2.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = C.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}
