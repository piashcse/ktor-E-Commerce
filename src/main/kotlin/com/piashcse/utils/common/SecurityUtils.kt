package com.piashcse.utils.common

import kotlin.math.pow
import kotlin.random.Random

fun generateOTP(length: Int = 6): String {
    val min = 10.0.pow(length - 1).toInt()
    val max = (10.0.pow(length) - 1).toInt()
    return Random.nextInt(min, max).toString()
}
