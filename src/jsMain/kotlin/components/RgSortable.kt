package components

import androidx.compose.runtime.*
import lib.Sortable
import lib.sortableArgs
import org.jetbrains.compose.web.dom.H6
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul


object NoopDER : DisposableEffectResult {
    override fun dispose() {}
}



@Composable
fun <T> RgSortable(
    items: List<T>,
    name: (T) -> String,
    onSort: (List<T>) -> Unit,
) {
    H6 { Text("sortable") }
    var arranged by remember { mutableStateOf(items) }
    Ul(attrs = {
        classes("list-group")
        ref {
            Sortable.create(
                it, sortableArgs(
                    ghostClass = "bg-info",
                    onSort = {
                        val a = arranged.toMutableList().apply {
                            val e = removeAt(it.oldIndex)
                            add(it.newIndex, e)
                        }
                        arranged = a
                        onSort(arranged)
                    }
                )
            )
            NoopDER
        }
    }) {
        items.forEach { item ->
            Li(attrs = { classes("list-group-item") }) { Text(name(item)) }
        }
    }
}