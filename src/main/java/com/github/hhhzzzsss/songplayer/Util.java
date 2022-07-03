package com.github.hhhzzzsss.songplayer;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    public static String formatTime(long milliseconds) {
        long temp = Math.abs(milliseconds);
        temp /= 1000;
        long seconds = temp % 60;
        temp /= 60;
        long minutes = temp % 60;
        temp /= 60;
        long hours = temp;
        StringBuilder sb = new StringBuilder();
        if (milliseconds < 0) {
            sb.append("-");
        }
        if (hours > 0) {
            sb.append(String.format("%d:", hours));
            sb.append(String.format("%02d:", minutes));
        } else {
            sb.append(String.format("%d:", minutes));
        }
        sb.append(String.format("%02d", seconds));
        return sb.toString();
    }

    public static Pattern timePattern = Pattern.compile("(?:(\\d+):)?(\\d+):(\\d+)");
    public static long parseTime(String timeStr) throws IOException {
        Matcher matcher = timePattern.matcher(timeStr);
        if (matcher.matches()) {
            long time = 0;
            String hourString = matcher.group(1);
            String minuteString = matcher.group(2);
            String secondString = matcher.group(3);
            if (hourString != null) {
                time += Integer.parseInt(hourString) * 60 * 60 * 1000;
            }
            time += Integer.parseInt(minuteString) * 60 * 1000;
            time += Double.parseDouble(secondString) * 1000.0;
            return time;
        } else {
            throw new IOException("Invalid time pattern");
        }
    }
}
