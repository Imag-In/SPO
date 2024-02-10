package org.icroco.picture.util;

import com.fasterxml.jackson.databind.util.StdConverter;

import java.util.Locale;

/**
 * Formats a Locale property when converting to String.
 */
public class LocaleCustomConverter extends StdConverter<Locale, String> {

    @Override
    public String convert(Locale locale) {
        //whatever custom format you dersire. Capitalize it, split it, whatever...
        if (locale == null) {
            return null;
        }
        return locale.toLanguageTag();
    }
}