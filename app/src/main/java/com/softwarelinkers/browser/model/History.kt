package com.softwarelinkers.browser.model

data class History(
    val id: String,
    val name: String,
    val url: String,
    var image: ByteArray? = null)
