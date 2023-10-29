package org.icroco.picture.views.ext_import;

import java.time.format.DateTimeFormatter;

class DateTimeStrategy extends AbstractDateStrategy {

    DateTimeStrategy() {
        super(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

}
