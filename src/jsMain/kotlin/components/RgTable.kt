package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.Scope
import org.jetbrains.compose.web.attributes.colspan
import org.jetbrains.compose.web.attributes.scope
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLTableCellElement
import org.w3c.dom.HTMLTableElement
import org.w3c.dom.HTMLTableRowElement
import org.w3c.dom.HTMLTableSectionElement


enum class TableColor {
    primary,
    secondary,
    success,
    danger,
    warning,
    info,
    light,
    dark;
}


@Composable
fun RgTable(
    caption: String? = null,
    color: TableColor = TableColor.secondary,
    stripeColumn: Boolean = false,
    content: ContentBuilder<HTMLTableElement>? = null
) {
    Div(attrs = { classes("table-responsive") }) {
        Table(attrs = {
            classes(
                "table",
                "table-hover",
                "table-${color.name}",
                "table-sm",
                if (stripeColumn) "table-striped-columns" else "table-striped",
                "caption-top",
                "table-bordered"
            )
        }) {
            caption?.let { Caption { Text(it) } }
            content?.invoke(this)
        }
    }
}

@Composable
fun RgTfoot(
    content: ContentBuilder<HTMLTableSectionElement>? = null
) = Tfoot(
    attrs = { }, content = content
)

@Composable
fun RgThead(
    content: ContentBuilder<HTMLTableSectionElement>? = null
) = Thead(
    attrs = { }, content = content
)

@Composable
fun RgTbody(
    content: ContentBuilder<HTMLTableSectionElement>? = null
) = Tbody(
    attrs = { }, content = content
)

@Composable
fun RgTrColor(
    color: TableColor? = null,
    content: ContentBuilder<HTMLTableRowElement>? = null
) = RgTr(color?.let { listOf("table-${it.name}") }, content)

@Composable
fun RgTr(
    classes: Collection<String>? = null,
    content: ContentBuilder<HTMLTableRowElement>? = null
) = Tr(
    attrs = {
        classes?.let { classes(it) }
    }, content = content
)

@Composable
fun RgTh(
    scope: Scope = Scope.Col,
    content: ContentBuilder<HTMLTableCellElement>? = null
) = Th(
    attrs = { scope(scope) }, content = content
)

@Composable
fun RgTdColor(
    colSpan: Int? = null,
    color: TableColor? = null,
    content: ContentBuilder<HTMLTableCellElement>? = null
) = RgTd(colSpan, color?.let { listOf("table-${it.name}") }, content)

@Composable
fun RgTd(
    colSpan: Int? = null,
    classes: Collection<String>? = null,
    content: ContentBuilder<HTMLTableCellElement>? = null
) = Td(
    attrs = {
        classes?.let { classes(it) }
        scope(Scope.Row)
        colSpan?.let { colspan(it) }
    }, content = content
)

