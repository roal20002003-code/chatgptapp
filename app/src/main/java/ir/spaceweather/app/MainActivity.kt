package ir.spaceweather.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ir.spaceweather.app.data.L
import ir.spaceweather.app.data.Prefs
import ir.spaceweather.app.ui.AppRoot
import ir.spaceweather.app.ui.SpaceTheme

class MainActivity : ComponentActivity() {
    private val vm: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val openAdmin = intent?.getBooleanExtra("open_admin", false) == true  // از روی نوتیف
        L.init(this)
        setContent {
            // تغییر زبان از تب تنظیمات: کل رابط با زبان و جهت جدید دوباره ساخته می‌شود
            var en by remember { mutableStateOf(L.en) }
            SpaceTheme(rtl = !en) {
                key(en) {
                    AppRoot(vm, openAdmin, onLanguage = { toEn ->
                        L.en = toEn
                        Prefs(this@MainActivity).lang = if (toEn) "en" else "fa"
                        en = toEn
                    })
                }
            }
        }
    }
}
