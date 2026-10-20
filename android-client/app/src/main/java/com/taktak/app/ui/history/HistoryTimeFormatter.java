package com.taktak.app.ui.history;

import android.content.Context;
import com.taktak.app.R;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class HistoryTimeFormatter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.systemDefault());

    private HistoryTimeFormatter() { }

    public static String format(Context context, String isoString) {
        if (isoString == null || context == null) { return ""; }
        try {
            Instant instant = Instant.parse(isoString);
            Instant now = Instant.now();
            Duration duration = Duration.between(instant, now);
            long seconds = duration.getSeconds();
            if (seconds < 60) {
                return context.getString(R.string.history_time_just_now);
            }
            long minutes = seconds / 60;
            if (minutes < 60) {
                return context.getString(R.string.history_time_minutes_ago, (int) minutes);
            }
            long hours = minutes / 60;
            if (hours < 24) {
                return context.getString(R.string.history_time_hours_ago, (int) hours);
            }
            long days = hours / 24;
            if (days < 7) {
                return context.getString(R.string.history_time_days_ago, (int) days);
            }
            return DATE_FORMATTER.format(instant);
        } catch (Exception e) {
            return "";
        }
    }

    public static String getExpiryRemaining(String isoString) {
        if (isoString == null) { return null; }
        try {
            Instant instant = Instant.parse(isoString);
            Duration duration = Duration.between(instant, Instant.now());
            long hours = 24 - duration.toHours();
            if (hours > 0) {
                return "⏳ " + hours + "h";
            } else if (duration.toHours() < 24) {
                return "⏳ <1h";
            }
        } catch (Exception ignored) { }
        return null;
    }
}
