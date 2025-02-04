package pl.lejdi.alc.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import pl.lejdi.alc.R

val HelpIcon = R.drawable.ic_baseline_help_outline_24

val settingsTextSize = 24.sp

fun getTextColor(darkMode: Boolean): Color {
    return if (darkMode) Color.White else Color.Black
}

fun getAlternativeTextColor(darkMode: Boolean): Color {
    return if (darkMode) Color.White else Color.DarkGray
}

@Composable
fun getSwitchColors(darkMode: Boolean): SwitchColors {
    return if (darkMode) {
        SwitchDefaults.colors(
            checkedTrackColor = Color(0xFF404040),
            checkedThumbColor = Color(0xFF454545),
            uncheckedThumbColor = Color(0xFF333333),
            uncheckedTrackColor = Color(0xFF404040)
        )
    } else {
        SwitchDefaults.colors(
            checkedTrackColor = Color(0xFF47d147),
            checkedThumbColor = Color(0xFF29a329),
            checkedTrackAlpha = 0.3f,
        )
    }
}

fun getIconColor(darkMode: Boolean) = getTextColor(darkMode)

fun getReportIssueTextColor(darkMode: Boolean): Color {
    return if (darkMode) Color.White else Color(41, 163, 41)
}

@Composable
fun ALCTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColors()
    } else {
        lightColors()
    }

    MaterialTheme(
        colors = colors,
        content = content
    )
}