package it.lu3g.tvlauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import it.lu3g.tvlauncher.R

// Palette LU3G: la stessa del brand wp-admin (zinc scuro + accento ambra)
val Ink = Color(0xFF09090B)
val Surface1 = Color(0xFF171719)
val Surface2 = Color(0xFF27272A)
val Surface3 = Color(0xFF3F3F46)
val Fog = Color(0xFFFAFAFA)
val Silver = Color(0xFFD4D4D8)
val Muted = Color(0xFFA1A1AA)
val Dim = Color(0xFF71717A)
val Amber = Color(0xFFFFAE4E)
val AmberDeep = Color(0xFFB45309)

/** Margine laterale della schermata (zona sicura TV). */
val GUTTER = 56.dp

// Istanze statiche dei font (generate dai font variabili con fonttools):
// funzionano su tutte le versioni Android, senza dipendere dall'asse wght
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

val DmSans = FontFamily(
    Font(R.font.dmsans_regular, FontWeight.Normal),
    Font(R.font.dmsans_medium, FontWeight.Medium),
    Font(R.font.dmsans_semibold, FontWeight.SemiBold),
    Font(R.font.dmsans_semibold, FontWeight.Bold),
)

val Lu3gTypography = Typography(
    displayLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-1.5).sp),
    displayMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
    headlineLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.2.sp),
)

val Lu3gColorScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Ink,
    primaryContainer = AmberDeep,
    onPrimaryContainer = Fog,
    secondary = Silver,
    onSecondary = Ink,
    secondaryContainer = Surface2,
    onSecondaryContainer = Fog,
    tertiary = Muted,
    onTertiary = Ink,
    background = Ink,
    onBackground = Fog,
    surface = Surface1,
    onSurface = Fog,
    surfaceVariant = Surface2,
    onSurfaceVariant = Silver,
    inverseSurface = Fog,
    inverseOnSurface = Ink,
    border = Surface3,
    borderVariant = Surface2,
    error = Color(0xFFF87171),
    onError = Ink,
)

@Composable
fun Lu3gTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Lu3gColorScheme, typography = Lu3gTypography) {
        // Colore di base del testo: bianco, esplicito, così non dipende dal dispositivo
        CompositionLocalProvider(LocalContentColor provides Fog, content = content)
    }
}
