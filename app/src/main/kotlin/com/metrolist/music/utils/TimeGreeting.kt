package com.metrolist.music.utils

import java.util.Calendar

enum class GreetingPeriod { MORNING, AFTERNOON, EVENING, NIGHT }

fun currentGreetingPeriod(): GreetingPeriod = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> GreetingPeriod.MORNING
    in 12..16 -> GreetingPeriod.AFTERNOON
    in 17..21 -> GreetingPeriod.EVENING
    else -> GreetingPeriod.NIGHT
}

fun greetingForCurrentTime(name: String): String = when (currentGreetingPeriod()) {
    GreetingPeriod.MORNING -> "Good morning, $name"
    GreetingPeriod.AFTERNOON -> "Good afternoon, $name"
    GreetingPeriod.EVENING -> "Good evening, $name"
    GreetingPeriod.NIGHT -> "Good night, $name"
}
