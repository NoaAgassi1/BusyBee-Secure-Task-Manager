package com.securefromscratch.busybee.controllers;

import com.securefromscratch.busybee.safety.ImageName;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ImageNameConverter implements Converter<String, ImageName> {
    @Override
    public ImageName convert(String source) {
        return new ImageName(source);
    }
}
