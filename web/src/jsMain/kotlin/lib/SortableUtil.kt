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
