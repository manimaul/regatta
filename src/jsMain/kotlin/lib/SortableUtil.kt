package lib


external interface SortEvent {
    val oldIndex: Int
    val newIndex: Int
}

fun sortableArgs(
    ghostClass: String? = null,
    onSort: ((SortEvent) -> Unit)? = null,
): dynamic {
    val obj = js("{}")
    ghostClass?.let { obj["ghostClass"] = it }
    onSort?.let { obj["onSort"] = it }
    return obj
}

//fun keys(json: dynamic) = js("Object").keys(json).unsafeCast<Array<String>>()
//fun showKeys(item: dynamic) {
//    keys(item).forEach {
//        println("$it : ${item[it]}")
//    }
//}
