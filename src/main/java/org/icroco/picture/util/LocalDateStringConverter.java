package org.icroco.picture.util;

import javafx.util.StringConverter;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateStringConverter extends StringConverter<LocalDate> {
    @Override
    public String toString(LocalDate date) {
        return date == null ? "" : DateTimeFormatter.ISO_DATE.format(date);
    }

    @Override
    public LocalDate fromString(String textDate) {
        return StringUtils.hasText(textDate) ? LocalDate.parse(textDate, DateTimeFormatter.ISO_DATE) : null;
    }
}
