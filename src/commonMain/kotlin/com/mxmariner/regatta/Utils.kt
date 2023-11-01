
package com.mxmariner.regatta
fun String.versionedApi() : String {
    return "/v1/api/$this"
}
fun String.versionedApi(params: Map<String, String>) : String {
    val builder = StringBuilder().apply {
        params.map {
            if (length == 0) {
                append('?')
            } else {
                append('&')
            }
            append(it.key)
            append('=')
            append(it.value)
        }
    }
    return "/v1/api/$this$builder"
}
