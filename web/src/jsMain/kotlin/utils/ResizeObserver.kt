package utils

import org.w3c.dom.DOMRectReadOnly
import org.w3c.dom.Element

external interface ResizeObserverEntry {
    val target: Element
    val contentRect: DOMRectReadOnly
    val contentBoxSize: Array<ResizeObserverSize>
    val borderBoxSize: Array<ResizeObserverSize>
    val devicePixelContentBoxSize: Array<ResizeObserverSize>
}

external interface ResizeObserverSize {
    val inlineSize: Double
    val blockSize: Double
}

external interface ResizeObserverObserveOptions {
    val box: String
}

external class ResizeObserver(callback: (Array<ResizeObserverEntry>, ResizeObserver) -> Unit) {
    fun observe(target: Element, options: ResizeObserverObserveOptions = definedExternally)
    fun unobserve(target: Element)
    fun disconnect()
}
