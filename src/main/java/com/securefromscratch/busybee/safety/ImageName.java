package com.securefromscratch.busybee.safety;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageName {
    private static final int LENGTH_MIN = 4;
    private static final int LENGTH_MAX = 200;

    private static final String ALLOWED_NON_LETTERS = " -()_,";

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageName.class);

    private final String m_name;

    public ImageName(String imgName) {
        if (imgName == null) {
            throw new IllegalArgumentException("Image name must not be null");
        }
        if (imgName.contains("/") || imgName.contains("\\")) {
                throw new IllegalArgumentException("Image name must not contain path separators");
            }

        if (imgName.contains("..")) {
            throw new IllegalArgumentException("Image name must not contain path traversal characters.");
        }

        if (imgName.length() < LENGTH_MIN || imgName.length() > LENGTH_MAX) {
            throw new IllegalArgumentException("Invalid length");
        }


        boolean alreadySawADot = false;

        for (char c : imgName.toCharArray()) {
            if (Character.isAlphabetic(c)) {
                continue; 
            }
            if (Character.isDigit(c)) {
                continue; 
            }
            
            if (c == '.' && !alreadySawADot) {
                alreadySawADot = true;
                continue;
            }

            if (ALLOWED_NON_LETTERS.indexOf(c) != -1) {
                continue; 
            }

            LOGGER.error("{} is not a valid character", c);
            throw new IllegalArgumentException("Invalid characters");
        }

        m_name = imgName;
    }

    public String getName() {
        return m_name;
    }

    @Override
    public String toString() {
        return m_name;
    }
}

