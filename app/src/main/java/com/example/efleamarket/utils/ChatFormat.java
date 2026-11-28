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

    public static String dateTime(Date d) {
        if (d == null) return "";
        return new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(d);
    }

    public static String countdown(Date d) {
        if (d == null) return "";
        long diff = d.getTime() - System.currentTimeMillis();
        if (diff <= 0) return diff > -2 * 3_600_000L ? "Đã đến giờ hẹn" : "Đã qua giờ hẹn";
        long totalMin = diff / 60_000L;
        long days = totalMin / 1440;
        long hours = (totalMin % 1440) / 60;
        long mins = totalMin % 60;
        if (days > 0) return "Còn " + days + " ngày " + hours + " giờ";
        if (hours > 0) return "Còn " + hours + " giờ " + mins + " phút";
        return "Còn " + Math.max(mins, 1) + " phút";
    }
}