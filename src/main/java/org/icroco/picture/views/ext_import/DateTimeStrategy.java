package org.icroco.picture.views.ext_import;

import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public final class DateTimeStrategy extends AbstractDateStrategy {
    @Override
    public String displayName() {
        return "DATE_TIME";
    }

    DateTimeStrategy() {
        super(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }


}
