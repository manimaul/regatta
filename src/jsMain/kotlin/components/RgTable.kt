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

@Composable
fun RgTable(
    content: ContentBuilder<HTMLTableElement>? = null
) {
    Table(attrs = { classes("table", "table-secondary", "table-bordered", "table-striped") }, content)
}

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
fun RgTr(
    content: ContentBuilder<HTMLTableRowElement>? = null
) = Tr(
    attrs = {

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
fun RgTd(
    colSpan: Int? = null,
    scope: Scope = Scope.Row,
    classes: Collection<String>? = null,
    content: ContentBuilder<HTMLTableCellElement>? = null
) = Td(
    attrs = {
        classes?.let { classes(it) }
        scope(scope)
        colSpan?.let { colspan(it) }
    }, content = content
)

