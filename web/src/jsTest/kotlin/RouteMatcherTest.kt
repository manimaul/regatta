import viewmodel.Route
import viewmodel.Routing
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteMatcherTest {

    @Test
    fun routeArgMatch() {
        val path = "/races/results/view/1"
        val route = Routing.from(path)
        assertEquals(Route.RaceResultView, route.route)
        assertEquals("1", route.args?.get("id"))
    }

    @Test
    fun routeMatch() {
        val path = "/people"
        val route = Routing.from(path)
        assertEquals(Route.People, route.route)
    }
}