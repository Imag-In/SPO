package org.icroco.picture.views.ext_import;

import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public final class DateStrategy extends AbstractDateStrategy {

    @Override
    public String displayName() {
        return "DATE";
    }

    DateStrategy() {
        super(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}
