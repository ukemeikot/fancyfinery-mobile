package com.fancyfinery.mobile.core.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable

/**
 * Window insets for a screen that sits INSIDE the app shell's Scaffold.
 *
 * The shell already reserves the system navigation bar — its `NavigationBar`
 * consumes that inset, and `NavDisplay` is padded by the bar's full height. An
 * inner Scaffold left on its defaults applies the same bottom inset a second
 * time, which is how the bag ended up with a band of dead space above the tab
 * bar and the catalogue clipped a row of products in half.
 *
 * So a tabbed screen takes the top inset (it draws its own app bar, under the
 * status bar) and nothing at the bottom.
 *
 * Pushed screens — product, checkout, order, policy — are NOT inside the bar:
 * the shell gives them zero bottom padding precisely because there is no bar to
 * avoid. They keep the default insets, which is what keeps their content clear
 * of the system navigation bar.
 */
@Composable
fun tabScaffoldInsets(): WindowInsets =
    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

/** The default: used by screens pushed above the bottom bar. */
@Composable
fun pushedScaffoldInsets(): WindowInsets = ScaffoldDefaults.contentWindowInsets
