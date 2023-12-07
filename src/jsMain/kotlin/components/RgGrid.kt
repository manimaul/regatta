package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    position: RgPosition? = null,
    content: ContentBuilder<HTMLDivElement>? = null
) {
    Div(attrs = {
        listOfNotNull(type.name, position?.cls).takeIf { it.isNotEmpty() }?.let {
            classes(it)
        }
    }, content)
}

//https://getbootstrap.com/docs/5.0/layout/breakpoints/
enum class RgBp {
    sm,
    md,
    lg,
    xl,
    xxl,
}


@Composable
fun RgRow(
    customizer: (@Composable RowCustomizer.() -> Unit)? = null,
    content: ContentBuilder<HTMLDivElement>? = null
) {
    val c = remember { RowCustomizer() }
    customizer?.invoke(c)
    Div(attrs = { c.getClasses().takeIf { it.isNotEmpty() }?.let { classes(it) } }, content)
}

enum class RgPosition(val cls: String) {
    fixed_bottom("fixed-bottom"),
    fixed_top("fixed-top"),
    sticky_sm_top("sticky-sm-top"),
    sticky_md_top("sticky-md-top"),
    sticky_lg_top("sticky-lg-top"),
    sticky_xl_top("sticky-xl-top"),
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

open class Customizer {
    protected var classes = mutableSetOf<String>()
    private var sss: String? = null

    open fun getClasses(): Collection<String> = classes
    fun addPosition(position: RgPosition) {
        classes.add(position.cls)
    }

    fun set(space: RgSpace? = null, side: RgSide? = null, size: RgSz? = null) {
        sss?.let { classes.remove(it) }
        sss = space?.name?.let { sp ->
            side?.let { "$sp$it" } ?: sp
        }?.let { sps ->
            size?.let { "$sps-${it.size}" } ?: sps
        }
        sss?.let { classes.add(it) }
    }

    fun addCustom(style: String) {
        classes.add(style)
    }

}

class RowCustomizer : Customizer() {
    var bp: RgBp? = null
    override fun getClasses(): List<String> {
        val base = bp?.let { "row-${it.name}" } ?: "row"
        return super.getClasses() + listOfNotNull(base)
    }
}

class ColCustomizer : Customizer() {
    var bucket: Int? = null
    var bp: RgBp? = null

    override fun getClasses(): List<String> {
        val first = bp?.let { "col-${it.name}" } ?: "col"
        val cls = bucket?.let { "$first-$it" } ?: first
        return super.getClasses() + listOfNotNull(cls)
    }
}

@Composable
fun RgCol(
    customizer: (@Composable ColCustomizer.() -> Unit)? = null,
    content: ContentBuilder<HTMLDivElement>? = null
) {
    val c = remember { ColCustomizer() }
    customizer?.invoke(c)
    Div(attrs = { c.getClasses().takeIf { it.isNotEmpty() }?.let { classes(it) } }, content)
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
    customizer: (@Composable Customizer.() -> Unit)? = null,
    content: ContentBuilder<HTMLDivElement>? = null
) {
    val c = remember { Customizer() }
    customizer?.invoke(c)
    Div(attrs = { c.getClasses().takeIf { it.isNotEmpty() }?.let { classes(it) } }, content)
}
