package com.yourname.universalleaderboards.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates legacy '&' color codes plus &#RRGGBB hex codes into a display-ready string.
 * Kept intentionally simple/dependency-light; if the server has Adventure/MiniMessage
 * available it is preferred at the call sites that build Components directly.
 */
public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private ColorUtil() {}

    public static String colorize(String input) {
        if (input == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
        }
        matcher.appendTail(buffer);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static String strip(String input) {
        if (input == null) return "";
        return org.bukkit.ChatColor.stripColor(colorize(input));
    }
}
