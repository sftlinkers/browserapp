package com.softwarelinkers.browser.model

data class Bookmarknew(
    val id: String,
    val name: String,
    val url: String,
    var image: ByteArray? = null)
