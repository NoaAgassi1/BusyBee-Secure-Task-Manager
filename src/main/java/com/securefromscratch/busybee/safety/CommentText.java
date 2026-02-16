package com.securefromscratch.busybee.safety;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.regex.Pattern;

@Schema(type = "String", description = "Comment")
public class CommentText {
    public static final int MIN_LENGTH = 1;
    public static final int MAX_LENGTH = 200;

    private static final Pattern VALID_COMMENT_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\s.,'!?()-]*$");

    private final String value;

    @ConstructorBinding
    public CommentText(String value) {
        if (value == null || value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Invalid length for comment");
        }
        if (!VALID_COMMENT_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid characters in comment");
        }
        this.value = value;
    }

    @JsonValue
    public String get() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentText that = (CommentText) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}