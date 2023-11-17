package com.mxmariner.regatta

fun String.versionedApi(): String {
    return "/v1/api/$this"
}

fun String.versionedApi(version: Int = 1, params: Map<String, String>? = null): String {
    val paramString = params?.let {
        StringBuilder().apply {
            it.onEachIndexed { index, entry ->
                if (index == 0) {
                    append('?')
                } else {
                    append('&')
                }
                append(entry.key)
                append('=')
                append(entry.value)
            }
        }.toString()
    } ?: ""
    return "/v$version/api/$this$paramString"
}
