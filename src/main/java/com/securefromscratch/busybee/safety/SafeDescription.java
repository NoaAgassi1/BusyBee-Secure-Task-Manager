package com.securefromscratch.busybee.safety;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public class SafeDescription {

    private final String value;

    @JsonCreator
    public SafeDescription(String value) {
        if (value == null) {
            this.value = "";
            return;
        }
        
        if (!isSafe(value)) {
            throw new IllegalArgumentException("Description contains unsafe HTML");
        }
        
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    private static boolean isSafe(String input) {
        String lower = input.toLowerCase(Locale.ROOT);

        if (lower.contains("<script")) return false;
        if (lower.contains("javascript:")) return false;
        if (lower.matches("(?s).*\\son\\w+\\s*=.*")) return false;
        if (lower.contains("src=\"data:") || lower.contains("src='data:")) return false;

        if (input.contains("<")) {
            String stripped = input
            .replaceAll("(?is)</?(a|img|b|i|u|strong|em)(\\s+[^>]*)?>", "")
            .replaceAll("(?is)<!--.*?-->", "");

            
            if (stripped.contains("<") || stripped.contains(">")) return false;
        }
        return true;
    }
}