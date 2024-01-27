package viewmodel

import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.w3c.dom.url.URL
import utils.RouteMatcher


data class Routing(
    val route: Route,
    val path: String,
    val args: Map<String, String>? = null,
) {

    companion object {

        private val matchers by lazy {
            Route.entries.map { RouteMatcher.build(it) }
        }

        fun from(path: String): Routing {
            return matchers.firstOrNull {
                it.matches(path)
            }?.let {
                Routing(it.route, path, it.groups(path))
            } ?: Routing(Route.NotFound, path, null)
        }

        fun from(route: Route): Routing {
            return Routing(route, route.pathPattern)
        }
    }
}

enum class Route(val pathPattern: String) {
    Home("/"),
    Series("/series"),
    SeriesEdit("/series/:id"),
    People("/people"),
    PeopleEdit("/people/:id"),
    Admin("/admin"),
    Races("/races"),
    RaceCreate("/race/create"),
    RaceEdit("/race/:id"),
    Boats("/boats"),
    BoatEdit("/boat/:id"),
    Classes("/class"),
    ClassesEdit("/class/:id"),
    CategoryEdit("/category/:id"),
    RaceResult("/races/results"),
    RaceResultView("/races/results/view/:id"),
    RaceResultEdit("/races/results/:id"),
    NotFound("/404")
}

data class RouteState(
    val current: Routing = Routing.from(window.location.pathname),
    val canGoback: Boolean = false,
    val replace: Boolean = true,
) : VmState

class RouteViewModel : BaseViewModel<RouteState>(RouteState()) {

    fun getQueryParam(key: String): List<String> {
        return URL(window.location.href).searchParams.getAll(key).toList()
    }

    init {
        window.addEventListener("popstate", {
            replaceRoute(window.location.pathname)
        })

        launch {
            flow.collect {
                println("state = $it")
                if (it.replace) {
                    println("replacing history state ${it.current.path}")
                    window.history.replaceState(null, it.current.route.name, it.current.path)
                } else {
                    println("pushing history state ${it.current.path}")
                    window.history.pushState(null, it.current.route.name, it.current.path)
                }
            }
        }
    }

    override fun reload() {
    }

    fun goBackOrHome() {
        withState {
           if (it.canGoback)  {
               window.history.back()
           } else {
               pushRoute(Route.Home)
           }
        }
    }

    private fun replaceRoute(path: String) {
        setState {
            copy(
                current = Routing.from(path),
                replace = true,
            )
        }
    }
    fun pushRoute(path: String) {
        if (path != flow.value.current.path) {
            setState {
                copy(
                    current = Routing.from(path),
                    canGoback = true,
                    replace = false,
                )
            }
        } else {
            println("already at route path $path")
        }
    }

    fun pushRoute(route: Route) {
        if (route.pathPattern != flow.value.current.route.pathPattern) {
            setState {
                copy(
                    current = Routing(route, route.pathPattern, null),
                    canGoback = true,
                    replace = false,
                )
            }
        } else {
            println("already at route $route")
        }
    }
}

val routeViewModel = RouteViewModel()
