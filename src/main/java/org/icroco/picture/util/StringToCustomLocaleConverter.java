package org.icroco.picture.util;

import com.fasterxml.jackson.databind.util.StdConverter;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

@Slf4j
public class StringToCustomLocaleConverter extends StdConverter<String, Locale> {
    @Override
    public Locale convert(String value) {
        try {
            return Locale.forLanguageTag(value);
        } catch (Exception e) {
            log.warn("Cannot read Locale: '{}'", value);
        }
        return Locale.getDefault();
    }
}