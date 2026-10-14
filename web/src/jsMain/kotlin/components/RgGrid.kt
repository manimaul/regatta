package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLDivElement

// Bootstrap's containers, reproduced as .rg-container* in app.css
enum class RgContainerType(val cls: String) {
    container("rg-container"),
    container_sm("rg-container-sm"),
    container_md("rg-container-md"),
    container_lg("rg-container-lg"),
    container_xl("rg-container-xl"),
    container_xxl("rg-container-xxl"),
    container_fluid("rg-container-fluid"),
}

@Composable
fun RgGrid(
    type: RgContainerType = RgContainerType.container,
    position: RgPosition? = null,
    content: ContentBuilder<HTMLDivElement>? = null
) {
    Div(attrs = {
        listOfNotNull(type.cls, position?.cls, "min-h-screen", "flex", "flex-col").takeIf { it.isNotEmpty() }?.let {
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

// one class per entry (classList.add rejects names with spaces); defined in app.css
enum class RgPosition(val cls: String) {
    fixed_bottom("rg-fixed-bottom"),
    fixed_top("rg-fixed-top"),
    sticky_sm_top("rg-sticky-sm-top"),
    sticky_md_top("rg-sticky-md-top"),
    sticky_lg_top("rg-sticky-lg-top"),
    sticky_xl_top("rg-sticky-xl-top"),
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
        // e.g. m + b + s3 -> "mb-4". Every combination is safelisted with @source inline(...) in app.css.
        sss = space?.name?.let { sp ->
            side?.let { "$sp$it" } ?: sp
        }?.let { sps ->
            size?.let { "$sps-${it.tw}" } ?: sps
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

/**
 * @param size Bootstrap's spacer step
 * @param tw the Tailwind step with the same size (Bootstrap 3 = 1rem = Tailwind 4, 4 = 1.5rem = 6, 5 = 3rem = 12)
 */
enum class RgSz(val size: String, val tw: String) {
    s0("0", "0"),
    s1("1", "1"),
    s2("2", "2"),
    s3("3", "4"),
    s4("4", "6"),
    s5("5", "12"),
    auto("auto", "auto")
}

@Composable
fun RgDiv(
    id: String? = null,
    customizer: (@Composable Customizer.() -> Unit)? = null,
    content: ContentBuilder<HTMLDivElement>? = null
) {
    val c = remember { Customizer() }
    customizer?.invoke(c)
    Div(attrs = {
        id?.let { id(it) }
        c.getClasses().takeIf { it.isNotEmpty() }?.let { classes(it)
        }
                }, content)
}
