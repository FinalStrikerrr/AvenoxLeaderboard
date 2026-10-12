package com.yourname.universalleaderboards.util;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public final class TextUtil {

    private TextUtil() {}

    public static String replace(String input, Map<String, String> replacements) {
        if (input == null) return "";
        String result = input;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    public static List<String> replace(List<String> input, Map<String, String> replacements) {
        List<String> out = new ArrayList<>(input.size());
        for (String line : input) out.add(replace(line, replacements));
        return out;
    }
}
