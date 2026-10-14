package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.alt
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.svg.Path
import org.jetbrains.compose.web.svg.Svg
import viewmodel.*

@Composable
fun NavBar(
    viewModel: RouteViewModel = routeViewModel,
    loginVm: LoginViewModel = loginViewModel,
) {
    val state by viewModel.flow.collectAsState()
    val loginFlowState by loginVm.flow.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    if (state.current.params?.values?.containsKey("nonav") == true) {
        RgDiv(customizer = { set(space = RgSpace.m, size = RgSz.s2) }) {
            A(href = state.href.replace("nonav", "nav"), attrs = {
                target(ATarget.Blank)
                classes("nav-link")
            }) { Text("Full Regatta Results") }
        }
    } else
    Nav(attrs = {
        classes("rg-navbar")
    }) {
        Div(attrs = { classes("rg-navbar-inner") }) {
            A(href = "#") {
                Img(src = "/cyct_burgee.svg", attrs = {
                    // a height class, not attr("height"): Preflight's img { height: auto } overrides the attribute
                    classes("inline-block", "h-[60px]", "w-auto")
                    alt("logo")
                })
            }
            Button(attrs = {
                classes("rg-navbar-toggler", "lg:hidden")
                attr("aria-controls", "navbarNavDropdown")
                attr("aria-expanded", menuOpen.toString())
                attr("aria-label", "Toggle navigation")
                onClick { menuOpen = !menuOpen }
            }) {
                HamburgerIcon()
            }
            // Below lg the list is a vertical menu shown only while open; at lg and up it's always a row.
            // Both class strings are literals so Tailwind's scanner sees every class.
            Ul(attrs = {
                id("navbarNavDropdown")
                if (menuOpen) {
                    classes("m-0", "p-0", "list-none", "flex", "basis-full", "flex-col", "lg:basis-auto", "lg:grow", "lg:flex-row", "lg:ms-4")
                } else {
                    classes("m-0", "p-0", "list-none", "hidden", "lg:flex", "lg:grow", "lg:flex-row", "lg:ms-4")
                }
            }) {
                (loginFlowState.login?.let {
                    arrayOf(
                        Route.Home,
                        Route.People,
                        Route.Boats,
                        Route.Series,
                        Route.Classes,
                        Route.Races,
                        Route.Rc,
                        Route.RaceResult,
                        Route.Course,
                    )
                } ?: arrayOf(
                    Route.RaceResult,
                    Route.Course,
                )).forEach { route ->
                    Li {
                        Button(attrs = {
                            classes("rg-nav-link")
                            if (route == state.current.route) {
                                attr("aria-current", "page")
                            }
                            onClick {
                                viewModel.pushRoute(route)
                            }
                        }) {
                            Text(route.label)
                        }
                    }
                }

                    if (loginFlowState.loginStatus == LoginStatus.LoggedIn) {
                        Li {
                            Button(attrs = {
                                classes("rg-nav-link")
                                onClick {
                                    loginVm.logout()
                                }
                            }) {
                                Text("Logout")
                            }
                        }
                    } else {
                        Li {
                            Button(attrs = {
                                classes("rg-nav-link")
                                onClick {
                                    viewModel.pushRoute(Route.Admin)
                                }
                            }) {
                                Text("Admin")
                            }
                        }
                    }
                }
            }
        }
}

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
private fun HamburgerIcon() {
    Svg(viewBox = "0 0 30 30", attrs = {
        attr("width", "30")
        attr("height", "30")
        attr("aria-hidden", "true")
    }) {
        Path("M4 7h22M4 15h22M4 23h22", attrs = {
            attr("stroke", "currentColor")
            attr("stroke-width", "2")
            attr("stroke-linecap", "round")
        })
    }
}
