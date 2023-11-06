package viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import kotlinx.browser.window
import org.w3c.dom.url.URL


sealed interface RoutingArgs

data class Routing(
    val route: Route,
    val args: RoutingArgs? = null
) {
    companion object {
        fun from(path: String): Routing {
            val route = Route.entries.firstOrNull { it.path == path } ?: Route.NotFound
            return Routing(route)
        }
    }
}

enum class Route(val path: String) {
    Home("/"),
    Series("/series"),
    People("/people"),
    Admin("/admin"),
    Races("/races"),
    Boats("/boats"),
    BoatEdit("/boat/{id}"),
    Classes("/class"),
    RaceResult("/races/results"),
    NotFound("/404");

    companion object {
        fun from(path: String): Route {
            return entries.firstOrNull { it.path == path } ?: NotFound
        }
    }
}

class RouteViewModel {
    private var routeState = mutableStateOf(Routing.from(window.location.pathname))

    val route: Route
        get() = routeState.value.route


    fun getQueryParam(key: String) : List<String> {
        return URL(window.location.href).searchParams.let{
            it.getAll(key)
        }.toList()
    }

    init {
        window.addEventListener("popstate", {
            println("history location set to ${window.location.pathname}")
            println("event = ${it.type} $it")
            setRoute(Route.from(window.location.pathname), true)
        })
    }

    fun setRoute(value: Route, replace: Boolean = false, args: RoutingArgs? = null) {
        if (value.path != routeState.value.route.path) {
            routeState.value = Routing(value, args)
            if (replace) {
                window.history.replaceState(null, value.name, value.path)
            } else {
                window.history.pushState(null, value.name, value.path)
            }
        } else {
            println("already at route $route")
        }
    }
}

val routeViewModel = RouteViewModel()
