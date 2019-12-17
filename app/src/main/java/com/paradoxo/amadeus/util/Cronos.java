package com.paradoxo.amadeus.util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

public class Cronos {

    public static String getData(){
        return "Hoje é dia "+DateTime.now().toString(DateTimeFormat.longDate());
    }

    public static String getHora(){
        return "São "+DateTime.now().toString("HH:mm");
    }
}
