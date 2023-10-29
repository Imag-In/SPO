package org.icroco.picture.views.ext_import;

import java.time.format.DateTimeFormatter;

class DateStrategy extends AbstractDateStrategy {

    DateStrategy() {
        super(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

}
