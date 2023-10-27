import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.*

enum class Route(val path: String) {
    Home("/"),
    Counter("/counter"),
    NotFound("/404");

    companion object {
        fun from(path: String): Route {
            return entries.firstOrNull { it.path == path } ?: NotFound
        }
    }
}

class RouteViewModel {
    private var routeState = mutableStateOf(Route.from(window.location.pathname))

    val route: Route
        get() = routeState.value
    init {
        window.addEventListener("popstate", {
            println("history location set to ${window.location.pathname}")
            println("event = ${it.type} $it")
            setRoute(Route.from(window.location.pathname), true)
        })
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

@Composable
fun provideRouteViewModel(): RouteViewModel {
    return remember { RouteViewModel() }
}

@Composable
fun Router(
    viewModel: RouteViewModel = provideRouteViewModel()
) {
    Div {
        H1 {
            Text("Regatta")
        }
        Hr()
        P {
            Text("route = ${viewModel.route}")
        }
        P {
            Text("path = ${viewModel.route.path}")
        }
        P {

            Button(attrs = {
                onClick {
                    viewModel.setRoute(Route.Home)
                }
            }) {
                Text("Go Home")
            }
            Button(attrs = {
                onClick {
                    viewModel.setRoute(Route.Counter)
                }
            }) {
                Text("Go to Counter")
            }
        }
        when (viewModel.route) {
            Route.Home -> Text("home")
            Route.Counter -> Counter(0) { println("count = $it")}
            Route.NotFound -> Text("womp womp")
        }
    }
}