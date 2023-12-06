package components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement

//https://getbootstrap.com/docs/5.0/layout/containers/
enum class RgContainerType {
    container,
    container_sm,
    container_md,
    container_lg,
    container_xl,
    container_xxl,
    container_fluid,
}

@Composable
fun RgGrid(
    type: RgContainerType = RgContainerType.container,
    content: ContentBuilder<HTMLDivElement>? = null
) {
    Div(attrs = { classes(type.name) }, content)
}

//https://getbootstrap.com/docs/5.0/layout/breakpoints/
enum class RgBp {
    sm,
    md,
    lg,
    xl,
    xxl,
}

data class RgRowCol(
    val bp: RgBp? = null
)

@Composable
fun RgRow(
    bp: RgBp? = null,
    content: ContentBuilder<HTMLDivElement>? = null
) {
    val base = bp?.let { "row-${it.name}" } ?: "row"
    Div(attrs = { classes(base) }, content)
}

//https://getbootstrap.com/docs/5.0/layout/grid/
enum class RgColType {
    col,
    col_sm,
    col_md,
    col_lg,
    col_xl,
    col_xxl,
}

@Composable
fun RgCol(
    bp: RgBp? = null,
    bucket: Int? = null,
    content: ContentBuilder<HTMLDivElement>? = null
) {
    val base = bp?.let { "col-${it.name}" } ?: "col"
    val cls = bucket?.let { "$base-$it" } ?: base
    Div(attrs = { classes(cls) }, content)
}

enum class RgSpace {
    m, //margin
    p, //padding
}

enum class RgSide {
    t, //top
    b, //bottom
    s, //start
    e, //end
    x, //sides
    y, //top, bottom
}

enum class RgSz(val size: String) {
    s0("0"),
    s1("1"),
    s2("2"),
    s3("3"),
    s4("4"),
    s5("5"),
    auto("auto")
}

@Composable
fun RgDiv(
    space: RgSpace? = null,
    side: RgSide? = null,
    size: RgSz? = null,
    content: ContentBuilder<HTMLDivElement>? = null
) {
    val cls = space?.name?.let { sp ->
        side?.let { "$sp$it" } ?: sp
    }?.let { sps ->
        size?.let { "$sps-${it.size}" } ?: sps
    }
    Div(attrs = { cls?.let { classes(it) } }, content)
}
