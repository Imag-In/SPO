package org.icroco.picture.views.task;

import javafx.concurrent.Task;

import java.util.Optional;

public abstract class AbstractTask<V> extends Task<V> {

    public final long start = System.currentTimeMillis();

    @Override
    protected void failed() {
        Optional.ofNullable(getException())
                .ifPresent(throwable -> {
                    throw new RuntimeException(throwable);
                });
    }
}
