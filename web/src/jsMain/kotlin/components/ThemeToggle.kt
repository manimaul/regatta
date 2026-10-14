package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.svg.Circle
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Svg
import viewmodel.Theme
import viewmodel.ThemeViewModel
import viewmodel.themeViewModel

/**
 * Light/dark switch: a sun knob on the left in light mode, sliding right to a moon in dark mode (.rg-theme-toggle).
 */
@Composable
fun ThemeToggle(viewModel: ThemeViewModel = themeViewModel) {
    val state by viewModel.flow.collectAsState()
    val dark = state.theme == Theme.Dark
    Button(attrs = {
        classes("rg-theme-toggle")
        attr("type", "button")
        attr("role", "switch")
        attr("aria-checked", dark.toString())
        attr("aria-label", "Dark mode")
        attr("title", if (dark) "Switch to light mode" else "Switch to dark mode")
        onClick { viewModel.toggle() }
    }) {
        Span(attrs = { classes("rg-theme-toggle-knob") }) {
            if (dark) MoonIcon() else SunIcon()
        }
    }
}

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
private fun SunIcon() {
    Svg(viewBox = "0 0 24 24", attrs = {
        attr("width", "14")
        attr("height", "14")
        attr("aria-hidden", "true")
        attr("fill", "none")
        attr("stroke", "currentColor")
        attr("stroke-width", "2.5")
        attr("stroke-linecap", "round")
    }) {
        Circle(12, 12, 4, attrs = { attr("fill", "currentColor") })
        Path("M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4")
    }
}

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
private fun MoonIcon() {
    Svg(viewBox = "0 0 24 24", attrs = {
        attr("width", "14")
        attr("height", "14")
        attr("aria-hidden", "true")
    }) {
        Path("M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z", attrs = { attr("fill", "currentColor") })
    }
}
