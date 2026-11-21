package com.example.efleamarket.utils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class ChatFormat {
    private ChatFormat() {}

    public static String time(Date d) {
        if (d == null) return "";
        Calendar now = Calendar.getInstance(), c = Calendar.getInstance();
        c.setTime(d);
        boolean today = now.get(Calendar.YEAR) == c.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR);
        return new SimpleDateFormat(today ? "HH:mm" : "dd/MM", Locale.getDefault()).format(d);
    }

    public static String price(double vnd) {
        return new DecimalFormat("#,### đ").format(vnd);
    }
}