@file:JsModule("sortablejs")
@file:JsNonModule
package lib

import org.w3c.dom.Element

external class Sortable {
    companion object {
        fun create(element: Element?, args: dynamic) : Sortable
    }
}

