package com.paradoxo.amadeus.util

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object Cronos {
    fun getData(): String = "Hoje é dia ${DateTime.now().toString(DateTimeFormat.longDate())}"

    fun getHora(): String = "São ${DateTime.now().toString("HH:mm")}"
}
