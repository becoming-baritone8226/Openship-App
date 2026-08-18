package com.kareemessam.openship

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform