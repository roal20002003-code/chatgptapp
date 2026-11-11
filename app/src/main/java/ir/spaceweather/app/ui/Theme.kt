package ir.spaceweather.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** همان رنگ‌های سایت */
object C {
    val night = Color(0xFF050E22)
    val deep = Color(0xFF091830)
    val panel = Color(0xFF0D2242)
    val line = Color(0xFF1B5082)
    val text = Color(0xFFE9ECF9)
    val muted = Color(0xFF9BB7D4)
    val sun = Color(0xFFFFC12E)
    val flare = Color(0xFFFF7A45)
    val ok = Color(0xFF20D98B)
    val storm = Color(0xFFFF5D6C)
    val sky = Color(0xFF13B6FF)
    val warn = Color(0xFFFFD166)
}

@Composable
fun SpaceTheme(rtl: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.sun, onPrimary = C.night,
            secondary = C.flare, background = C.night, onBackground = C.text,
            surface = C.panel, onSurface = C.text, surfaceVariant = C.deep, onSurfaceVariant = C.muted,
            outline = C.line,
        )
    ) {
        // فارسی راست‌به‌چپ، انگلیسی چپ‌به‌راست
        CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) { content() }
    }
}
