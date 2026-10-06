package com.majidbahmani.cesto

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform