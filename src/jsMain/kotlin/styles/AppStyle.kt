package styles

import org.jetbrains.compose.web.css.*

object AppStyle : StyleSheet() {
    private const val marginSize = 8

    val marginAll by style {
        margin(marginSize.px)
    }

    val marginStart by style {
        marginLeft(marginSize.px)
    }

    val marginEnd by style {
        marginRight(marginSize.px)
    }

    val marginHrz by style {
        marginRight(marginSize.px)
        marginLeft(marginSize.px)
    }

    val marginVert by style {
        marginTop(marginSize.px)
        marginBottom(marginSize.px)
    }
}