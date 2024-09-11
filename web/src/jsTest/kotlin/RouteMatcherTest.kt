import viewmodel.Route
import viewmodel.Routing
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteMatcherTest {

    @Test
    fun routeArgMatch() {
        val path = "/boat/1"
        val route = Routing.from(path)
        assertEquals(Route.BoatEdit, route.route)
        assertEquals("1", route.args?.get("id"))
    }

    @Test
    fun routeMatch() {
        val path = "/people"
        val route = Routing.from(path)
        assertEquals(Route.People, route.route)
    }
}