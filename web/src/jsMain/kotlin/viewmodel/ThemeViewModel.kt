package viewmodel

import kotlinx.browser.document
import utils.localStoreGet
import utils.localStoreSet

enum class Theme(val attr: String) {
    Light("light"),
    Dark("dark");

    companion object {
        // the site defaults to light; only an explicit choice from the toggle switches it
        fun from(value: String?): Theme = entries.firstOrNull { it.attr == value } ?: Light
    }
}

data class ThemeState(
    val theme: Theme = Theme.Light,
) : VmState

// index.html sets data-theme from the same key before the bundle loads, so the page doesn't flash light first
const val themeStorageKey = "theme"

val themeViewModel = ThemeViewModel()

class ThemeViewModel : BaseViewModel<ThemeState>(ThemeState(Theme.from(localStoreGet(themeStorageKey)))) {
    init {
        apply(flow.value.theme)
    }

    override fun reload() {
    }

    fun toggle() {
        setTheme(if (flow.value.theme == Theme.Dark) Theme.Light else Theme.Dark)
    }

    fun setTheme(theme: Theme) {
        localStoreSet(themeStorageKey, theme.attr)
        apply(theme)
        setState { copy(theme = theme) }
    }

    private fun apply(theme: Theme) {
        document.documentElement?.setAttribute("data-theme", theme.attr)
    }
}
