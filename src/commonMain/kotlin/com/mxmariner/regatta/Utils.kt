
package com.mxmariner.regatta
fun String.versionedApi() : String {
    return "/v1/api/$this"
}
