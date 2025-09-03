package viewmodel

import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.w3c.dom.url.URL
import utils.RouteMatcher

data class QueryParams(
    val queryString: String? = queryString()
) {
   val values: Map<String, String?>? by lazy {
       println("url = $queryString")
       queryString?.let {
           val retVal = mutableMapOf<String, String?>()
           queryString.split('&').forEach { qp ->
               val pair = qp.split('=')
               if (pair.size == 1) {
                   retVal[pair[0]] = null
               } else if (pair.size == 2) {
                   retVal[pair[0]] = pair[1]
               }
           }
           retVal
       }
   }
}
fun queryString(): String? {
    val url = window.location.href
    return url.lastIndexOf('?').takeIf { it > 0 }?.let { qs ->
        url.substring(qs + 1)
    }
}

data class Routing(
    val route: Route,
    val path: String,
    val args: Map<String, String>? = null,
    val params: QueryParams? = null,
) {

    fun pathAndParams() : String {
        return params?.queryString?.let {
          "$path?$it"
        } ?: path
    }

    companion object {

        private val matchers by lazy {
            Route.entries.map { RouteMatcher.build(it) }
        }

        fun from(path: String, params: QueryParams? = null): Routing {
            val queryParams = params ?: QueryParams()
            return matchers.firstOrNull {
                it.matches(path)
            }?.let {
                Routing(it.route, path, it.groups(path), queryParams)
            } ?: Routing(Route.NotFound, path, null, queryParams)
        }

        fun from(route: Route, params: QueryParams? = null): Routing {
            val queryParams = params ?: QueryParams()
            return Routing(route, route.pathPattern, null, queryParams)
        }
    }
}

enum class Route(val pathPattern: String, val label: String) {
    Home("/", "Home"),
    Series("/series", "Series"),
    Course("/course", "Course"),
    People("/people", "Skippers"),
    Admin("/admin", "Admin"),
    AdminCreate("/admin/create", "Create Admin"),
    Races("/races", "Races"),
    RaceCreate("/race/create", "Create Race"),
    RaceEdit("/race/:id", "Edit Race"),
    Boats("/boats", "Boats"),
    Classes("/class", "Classes"),
    RaceResult("/races/results", "Race Results"),
    RaceResultView("/races/results/view/:id", "Race Result"),
    RaceResultEdit("/races/results/:id", "Edit Race Results"),
    SeriesStandingsView("/series/standings/view/:id/:year", "Series Standings"),
    Rc("/rc", "RC"),
    NotFound("/404", "NotFound")
}

data class RouteState(
    val current: Routing = Routing.from(window.location.pathname),
    val href: String = window.location.href,
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
                    window.history.replaceState(null, it.current.route.name, it.current.pathAndParams())
                } else {
                    println("pushing history state ${it.current.path}")
                    window.history.pushState(null, it.current.route.name, it.current.pathAndParams())
                }
            }
        }
    }

    override fun reload() {
    }

    fun goBackOrHome() {
        withState {
            if (it.canGoback) {
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
                href = window.location.href,
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
                    current = Routing(route, route.pathPattern, null, QueryParams()),
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
