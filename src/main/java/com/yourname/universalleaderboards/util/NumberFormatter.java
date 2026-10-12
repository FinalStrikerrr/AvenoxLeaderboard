package com.yourname.universalleaderboards.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Parses raw PlaceholderAPI output ("$1,250,000", "12345", "1.25M", "N/A", "")
 * into a sortable double, and formats sortable values back into display strings.
 */
public final class NumberFormatter {

    public enum Mode { RAW, COMMA, SHORT, COMPACT }

    private static final Pattern STRIP_PATTERN = Pattern.compile("[^0-9.\\-]");
    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Q"};

    private NumberFormatter() {}

    /**
     * Attempts to parse a raw placeholder string into a numeric value.
     * Returns null if the value could not be parsed (e.g. "N/A", empty, non-numeric).
     */
    public static Double parse(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.equalsIgnoreCase("N/A") || trimmed.equalsIgnoreCase("NaN")) return null;

        // Handle shorthand suffixes like "1.25M" produced by other plugins.
        char last = Character.toUpperCase(trimmed.charAt(trimmed.length() - 1));
        double multiplier = 1;
        String numericPart = trimmed;
        for (int i = 1; i < SUFFIXES.length; i++) {
            if (SUFFIXES[i].charAt(0) == last) {
                multiplier = Math.pow(1000, i);
                numericPart = trimmed.substring(0, trimmed.length() - 1);
                break;
            }
        }

        String cleaned = STRIP_PATTERN.matcher(numericPart).replaceAll("");
        if (cleaned.isEmpty() || cleaned.equals("-")) return null;

        try {
            return Double.parseDouble(cleaned) * multiplier;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static String format(double value, Mode mode, int decimals) {
        return switch (mode) {
            case RAW -> stripTrailingZero(value, decimals);
            case COMMA -> comma(value, decimals);
            case SHORT, COMPACT -> shorten(value, decimals);
        };
    }

    private static String stripTrailingZero(double value, int decimals) {
        BigDecimal bd = BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP);
        return bd.stripTrailingZeros().toPlainString();
    }

    private static String comma(double value, int decimals) {
        DecimalFormat format = new DecimalFormat();
        format.setGroupingUsed(true);
        format.setMaximumFractionDigits(decimals);
        format.setMinimumFractionDigits(0);
        format.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
        return format.format(value);
    }

    private static String shorten(double value, int decimals) {
        double absValue = Math.abs(value);
        int magnitude = 0;
        double shortValue = value;
        while (Math.abs(shortValue) >= 1000 && magnitude < SUFFIXES.length - 1) {
            shortValue /= 1000.0;
            magnitude++;
        }
        if (magnitude == 0) {
            return comma(value, decimals);
        }
        BigDecimal bd = BigDecimal.valueOf(shortValue).setScale(decimals, RoundingMode.HALF_UP);
        return bd.stripTrailingZeros().toPlainString() + SUFFIXES[magnitude];
    }
}
