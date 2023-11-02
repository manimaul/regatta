import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.svg.*
import kotlin.math.PI

@Composable
fun Boats() {
    H4 {
        Text("Boats")
    }
    Spinner(50f)
}

@OptIn(ExperimentalComposeWebSvgApi::class)
@Composable
fun Spinner(
    size: Float,
    color: String = "black"
) {
    val center = size / 2f
    val stroke = size / 10f
    val radius = center - stroke
    val amt = (2f * PI * radius) * .8
    Svg(attrs = {
        width(size)
        height(size)
    }) {
        Defs {
            LinearGradient("grad") {
                Stop(attrs = {
                    attr("offset", "0%")
                    attr("stop-color", color)
                })
                Stop(attrs = {
                    attr("offset", "100%")
                    attr("stop-color", color)
                    attr("stop-opacity", "0")
                })
            }
        }
        Circle(center, center, radius, attrs = {
            fill("none")
            attr("stroke", "url(#grad)")
            attr("stroke-width", "$stroke")
            attr("stroke-dasharray", "$amt")
            attr("stroke-linecap", "round")
        }) {
            AnimateTransform(attrs = {
                attributeName("transform")
                attr("attributeType", "XML")
                attr("type", "rotate")
                attr("repeatCount", "indefinite")
                attr("from", "0, $center, $center")
                attr("to", "360, $center, $center")
                attr("dur", "2s")
            })
        }
    }
}
