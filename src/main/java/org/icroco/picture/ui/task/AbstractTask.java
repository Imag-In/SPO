package org.icroco.picture.ui.task;

import javafx.concurrent.Task;

import java.util.Optional;

public abstract class AbstractTask<V> extends Task<V> {
    @Override
    protected void failed() {
        Optional.ofNullable(getException())
                .ifPresent(throwable -> {
                    throw new RuntimeException(throwable);
                });
    }
}
