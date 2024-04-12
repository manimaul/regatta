package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.browser.window
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.alt
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.*
import viewmodel.*

@Composable
fun NavBar(
    viewModel: RouteViewModel = routeViewModel,
    loginVm: LoginViewModel = loginViewModel,
) {
    val state by viewModel.flow.collectAsState()
    val loginFlowState by loginVm.flow.collectAsState()
    if (state.current.params?.values?.containsKey("nonav") == true) {
        RgDiv(customizer = { set(space = RgSpace.m, size = RgSz.s2) }) {
            A(href = state.href.replace("nonav", "nav"), attrs = {
                target(ATarget.Blank)
                classes("nav-link")
            }) { Text("Full Regatta Results") }
        }
    } else
    Nav(attrs = {
        classes(
            "navbar",
            "navbar-expand-lg",
            "bg-body-tertiary",
            "sticky-top",
            "border-body",
            "border-bottom"
        )
        attr("data-bs-theme", "light")
    }) {
        Div(attrs = { classes("container-fluid") }) {
            A(attrs = { classes("navbar-brand") }, href = "#") {
                Img(src = "/cyct_burgee.png", attrs = {
                    classes("d-inline-block")
                    attr("height", "60")
                    alt("logo")
                })
            }
            Button(attrs = {
                classes("navbar-toggler")
                attr("data-bs-toggle", "collapse")
                attr("data-bs-target", "#navbarNavDropdown")
                attr("aria-controls", "navbarNavDropdown")
                attr("aria-expanded", "false")
                attr("aria-label", "Toggle navigation")
            }) {
                Span(attrs = { classes("navbar-toggler-icon") }) { }
            }
            Div(attrs = {
                id("navbarNavDropdown")
                classes("collapse", "navbar-collapse")
            }) {
                Ul(attrs = { classes("navbar-nav") }) {
                    (loginFlowState.login?.let {
                        arrayOf(
                            Route.Home,
                            Route.People,
                            Route.Boats,
                            Route.Classes,
                            Route.Series,
                            Route.Races,
                            Route.Rc,
                            Route.RaceResult,
                        )
                    } ?: arrayOf(
                        Route.RaceResult,
                    )).forEach { route ->
                        Li(attrs = { classes("nav-item") }) {
                            Button(attrs = {
                                attr("data-bs-toggle", "collapse")
                                attr("data-bs-target", "#navbarNavDropdown")
                                if (route == state.current.route) {
                                    classes("nav-link", "active")
                                } else {
                                    classes("nav-link")
                                }
                                onClick {
                                    viewModel.pushRoute(route)
                                }
                            }) {
                                Text(route.name)
                            }
                        }
                    }

                    if (loginFlowState.loginStatus == LoginStatus.LoggedIn) {
                        Li(attrs = { classes("nav-item") }) {
                            Button(attrs = {
                                classes("nav-link")
                                onClick {
                                    loginVm.logout()
                                }
                            }) {
                                Text("Logout")
                            }
                        }
                    } else {
                        Li(attrs = { classes("nav-item") }) {
                            Button(attrs = {
                                classes("nav-link")
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
}
