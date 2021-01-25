package com.bw.vrtnumm.androidApp.utils

fun String.sanitizedUrl(width: Int = 320): String {
    var url = this
        if (!url.startsWith("https:"))  {
            url = "https:$url"
        }
        if (url.contains("orig")) {
            url = url.replace("orig", "w${width}hx")
        }
    return url
}

fun String.escHtml(): String {
    var esc = this.replace("&nbsp;", " ")
        esc  = esc.replace("<p>", "")
        esc = esc.replace("</p>", "")
        esc = esc.replace("<br>", "").trim()
    return esc
}