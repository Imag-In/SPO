package org.icroco.picture.views.ext_import;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ERenameStrategy {

    COUNTER(new CounterStrategy()),
    DATE(new DateStrategy()),
    DATE_TIME(new DateTimeStrategy());

    @Getter
    private final IRenameFilesStrategy strategy;
}
