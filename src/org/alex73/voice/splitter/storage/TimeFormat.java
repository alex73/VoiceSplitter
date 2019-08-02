package org.alex73.voice.splitter.storage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeFormat {
    static final Pattern RE = Pattern.compile("([0-9]{2})\\:([0-9]{2})\\:([0-9]{2})\\.([0-9]+)");

    public static String t2s(double time) {
        long sec = (long) time;
        long milli = Math.round(time * 1000) - sec * 1000;
        return n(sec / 60 / 60, 2) + ':' + n((sec / 60) % 60, 2) + ':' + n(sec % 60, 2) + '.' + n(milli, 3);
    }

    public static double s2t(String time) {
        if (time == null) {
            return 0;
        }
        Matcher ma = RE.matcher(time);
        if (!ma.matches()) {
            throw new NumberFormatException("Wrong time: " + time);
        }
        int h = Integer.parseInt(ma.group(1));
        int m = Integer.parseInt(ma.group(2));
        int s = Integer.parseInt(ma.group(3));
        int milli;
        switch (ma.group(4).length()) {
        case 3:
            milli = Integer.parseInt(ma.group(4));
            break;
        default:
            throw new NumberFormatException("Wrong time: " + time);
        }
        return h * 60 * 60 + m * 60 + s + milli / 1000.0;
    }

    private static String n(long v, int len) {
        String r = Long.toString(v);
        if (r.length() < len) {
            return "000000".substring(6 - len + r.length()) + r;
        } else if (r.length() == len) {
            return r;
        } else {
            throw new NumberFormatException("Wrong : " + r);
        }
    }

    public static void main(String[] a) {
        System.out.println(RE.matcher("00:00:00.000").matches());
    }
}
