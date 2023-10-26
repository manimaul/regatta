import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import androidx.compose.runtime.setValue

@Composable
fun Counter(count: Int, onCountChange: (Int) -> Unit) {
    var currentCount by remember { mutableStateOf(count) }
    Div({ style { padding(25.px) } }) {
        Button(attrs = {
            onClick {
                currentCount = currentCount - 1
                onCountChange(currentCount)
            }
        }) {
            Text("-")
        }

        Span({ style { padding(15.px) } }) {
            Text("$currentCount")
        }

        Button(attrs = {
            onClick {
                currentCount =  currentCount + 1
                onCountChange(currentCount)
            }
        }) {
            Text("+")
        }
    }
}
