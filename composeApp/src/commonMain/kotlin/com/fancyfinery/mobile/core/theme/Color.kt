package com.fancyfinery.mobile.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The Fancy Finery palette, taken from the storefront rather than invented.
 *
 * The house reads as near-black and gold: `--foreground: #171717` on white, and
 * a wordmark that runs a four-stop gradient from pale champagne through to dark
 * bronze. Those exact stops are kept below, because the app sits beside the
 * website in a customer's memory and a different gold reads as a different
 * brand — or worse, as a counterfeit.
 *
 * Material 3 wants a `primary` that carries interactive weight. Gold is the
 * brand colour but it is a poor primary: `#eab308` on white is roughly 2:1
 * contrast, which fails WCAG AA for text and for button labels. So the
 * near-black is primary (buttons, links, anything that must be read) and gold
 * is `tertiary` — used for the wordmark, accents, rating stars and price
 * emphasis, where it decorates rather than carries meaning.
 */

// --- Brand constants -------------------------------------------------------

/** The wordmark gradient, light to dark. Use for text brushes, not fills. */
val GoldChampagne = Color(0xFFFDE68A)
val GoldLight = Color(0xFFF0C245)
val Gold = Color(0xFFEAB308)
val GoldDeep = Color(0xFFA9791B)

/** `--foreground` on the website. Near-black, not pure black: softer on OLED. */
val Ink = Color(0xFF171717)
val InkSoft = Color(0xFF4A4A4A)
val Paper = Color(0xFFFFFFFF)
val PaperWarm = Color(0xFFFAF8F5)

/** The dark ground the website's header and footer sit on. */
val Obsidian = Color(0xFF0A0A0A)
val ObsidianRaised = Color(0xFF161616)

private val Danger = Color(0xFFB3261E)
private val DangerDark = Color(0xFFF2B8B5)

val LightColorScheme = lightColorScheme(
    // Near-black carries every action, so labels stay legible.
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = Color(0xFFE8E4DE),
    onPrimaryContainer = Ink,

    secondary = InkSoft,
    onSecondary = Paper,
    secondaryContainer = Color(0xFFEFEBE5),
    onSecondaryContainer = Ink,

    // Gold lives here: accents and emphasis, never load-bearing text.
    tertiary = GoldDeep,
    onTertiary = Paper,
    tertiaryContainer = Color(0xFFFBF0D0),
    onTertiaryContainer = Color(0xFF3D2B06),

    error = Danger,
    onError = Paper,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF370606),

    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    // Warm rather than grey — a boutique ground, not a dashboard one.
    surfaceVariant = PaperWarm,
    onSurfaceVariant = Color(0xFF55514B),
    outline = Color(0xFFB9B3AA),
    outlineVariant = Color(0xFFE4DFD7),
    scrim = Color(0xFF000000),
)

val DarkColorScheme = darkColorScheme(
    // Inverted: on a dark ground the light tone is what carries actions.
    primary = Color(0xFFF2EFEA),
    onPrimary = Obsidian,
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Color(0xFFF2EFEA),

    secondary = Color(0xFFCFC9C1),
    onSecondary = Color(0xFF2A2A2A),
    secondaryContainer = Color(0xFF353535),
    onSecondaryContainer = Color(0xFFEDE9E3),

    // Gold gains contrast on dark, so the brighter stop is usable here.
    tertiary = Gold,
    onTertiary = Color(0xFF2B1F02),
    tertiaryContainer = Color(0xFF4A3708),
    onTertiaryContainer = GoldChampagne,

    error = DangerDark,
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),

    background = Obsidian,
    onBackground = Color(0xFFEDE9E3),
    surface = Obsidian,
    onSurface = Color(0xFFEDE9E3),
    surfaceVariant = ObsidianRaised,
    onSurfaceVariant = Color(0xFFB5AFA6),
    outline = Color(0xFF6E6862),
    outlineVariant = Color(0xFF2E2B27),
    scrim = Color(0xFF000000),
)
