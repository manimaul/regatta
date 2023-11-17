package viewmodel

import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.w3c.dom.url.URL
import utils.RouteMatcher


data class Routing(
    val route: Route,
    val path: String,
    val args: Map<String, String>? = null,
    val replace: Boolean = false,
) : VmState {

    companion object {

        private val matchers by lazy {
            Route.entries.map { RouteMatcher.build(it) }
        }

        fun from(path: String, replace: Boolean = false): Routing {
            return matchers.firstOrNull {
                it.matches(path)
            }?.let {
                Routing(it.route, path, it.groups(path), replace)
            } ?: Routing(Route.NotFound, path, null, replace)
        }
    }
}

enum class Route(val pathPattern: String) {
    Home("/"),
    Series("/series"),
    People("/people"),
    PeopleEdit("/people/:id"),
    Admin("/admin"),
    Races("/races"),
    Boats("/boats"),
    BoatEdit("/boat/:id"),
    Classes("/class"),
    RaceResult("/races/results"),
    NotFound("/404")
}

class RouteViewModel : BaseViewModel<Routing>(Routing.from(window.location.pathname)) {

    fun getQueryParam(key: String): List<String> {
        return URL(window.location.href).searchParams.getAll(key).toList()
    }

    init {
        window.addEventListener("popstate", {
            println("history location set to ${window.location.pathname}")
            println("event = ${it.type} $it")
            replaceRoute(window.location.pathname)
        })

        launch {
            flow.collect {
                if (it.replace) {
                    window.history.replaceState(null, it.route.name, it.path)
                } else {
                    window.history.pushState(null, it.route.name, it.path)
                }
            }
        }
    }

    fun replaceRoute(path: String) {
        val routing = Routing.from(path, true)
        setState { routing }
    }

    fun setRoute(value: Route, replace: Boolean = false) {
        if (value.pathPattern != flow.value.route.pathPattern) {
            setState { Routing(value, value.pathPattern, null, replace) }
        } else {
            println("already at route $value")
        }
    }
}

val routeViewModel = RouteViewModel()
