package ir.spaceweather.app.ui

import ir.spaceweather.app.data.tr
import ir.spaceweather.app.data.L

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.spaceweather.app.data.Api
import kotlinx.coroutines.launch

@Composable
fun MessageScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = C.sun, unfocusedBorderColor = C.line, focusedLabelColor = C.sun,
        unfocusedLabelColor = C.muted, cursorColor = C.sun, focusedTextColor = C.text, unfocusedTextColor = C.text,
    )
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "" }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        Card {
            SectionTitle(tr("ارسال پیام", "Send a message"), tr("پیشنهاد، گزارش خطا یا هر پیامی برای تیم SpaceWeather", "Suggestions, bug reports or any message for the SpaceWeather team"))
            OutlinedTextField(name, { name = it.take(100) }, label = { Text(tr("نام (اختیاری)", "Name (optional)")) },
                singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(contact, { contact = it.take(150) }, label = { Text(tr("ایمیل یا شماره تماس (اختیاری)", "Email or phone (optional)")) },
                singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(body, { body = it.take(2000) }, label = { Text(tr("متن پیام", "Message")) },
                minLines = 5, colors = fieldColors, modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("${body.length}/2000", color = C.muted, fontSize = 11.sp) })
            Spacer(Modifier.height(10.dp))
            Button(
                enabled = body.trim().length >= 3 && !sending,
                onClick = {
                    scope.launch {
                        sending = true
                        result = runCatching {
                            Api.sendMessage(
                                name.trim(), contact.trim(), body.trim(),
                                "${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE}", version
                            )
                        }.getOrElse { false to tr("ارسال نشد؛ اتصال اینترنت را بررسی کنید", "Not sent; please check your internet connection") }
                        if (result?.first == true) body = ""
                        sending = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = C.sun, contentColor = C.night),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (sending) tr("در حال ارسال…", "Sending…") else tr("ارسال پیام", "Send a message"), fontWeight = FontWeight.Bold) }

            result?.let { (ok, msg) ->
                Text(msg, color = if (ok) C.ok else C.storm, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Card {
            Text(tr("راه‌های دیگر ارتباط", "Other ways to reach us"), color = C.text, fontWeight = FontWeight.Bold)
            TextButton(onClick = {
                val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:info@spaceweather.ir"))
                    .putExtra(Intent.EXTRA_SUBJECT, tr("پیام از اپ SpaceWeather", "Message from the SpaceWeather app"))
                    .putExtra(Intent.EXTRA_TEXT, body)
                runCatching { ctx.startActivity(i) }
            }) { Text("✉ info@spaceweather.ir", color = C.sun) }
        }
    }
}
