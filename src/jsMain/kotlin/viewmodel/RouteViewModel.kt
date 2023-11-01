package viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.mxmariner.regatta.data.AboutInfo
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

enum class Route(val path: String) {
    Home("/"),
    Series("/series"),
    People("/people"),
    Races("/races"),
    RaceResult("/races/results"),
    NotFound("/404");

    companion object {
        fun from(path: String): Route {
            return entries.firstOrNull { it.path == path } ?: NotFound
        }
    }
}

val mainScope = MainScope()
class RouteViewModel {
    private var routeState = mutableStateOf(Route.from(window.location.pathname))
    private val aboutState = mutableStateOf<AboutInfo?>(null)

    val aboutInfo: AboutInfo?
        get() = aboutState.value

    val route: Route
        get() = routeState.value
    init {
        window.addEventListener("popstate", {
            println("history location set to ${window.location.pathname}")
            println("event = ${it.type} $it")
            setRoute(Route.from(window.location.pathname), true)
        })
        mainScope.launch {
            aboutState.value = Network.fetch("about")
        }
    }

    fun setRoute(value: Route, replace: Boolean = false) {
        if (value.path != routeState.value.path) {
            routeState.value = value
            if (replace) {
                println("replacing route $route")
                window.history.replaceState(null, value.name, value.path)
            } else {
                println("pushing route $route")
                window.history.pushState(null, value.name, value.path)
            }
        } else {
            println("already at route $route")
        }
    }
}

private val routeViewModel = RouteViewModel()

@Composable
fun provideRouteViewModel(): RouteViewModel {
//    return remember { RouteViewModel() }
    return routeViewModel
}
