package org.icroco.picture.views.task;

import javafx.concurrent.Task;
import lombok.Builder;
import lombok.NonNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Builder
public final class ModernTask<V> extends Task<V> {

    public final  long                start       = System.currentTimeMillis();
    @NonNull
    private final Supplier<V>         execute;
    @Builder.Default
    private final Runnable            onFinished  = () -> {
    };
    @Builder.Default
    private final Consumer<V>         onSuccess   = (t) -> {
    };
    @Builder.Default
    private final Consumer<Throwable> onFailed    = (t) -> {
    };
    @Builder.Default
    private final Runnable            onCancelled = () -> {
    };

    @Override
    protected V call() throws Exception {
        return execute.get();
    }


    @Override
    protected void failed() {
        onFinished.run();
        Optional.ofNullable(getException())
                .ifPresent(onFailed);
    }

    @Override
    protected void succeeded() {
        onFinished.run();
        onSuccess.accept(getValue());
    }

    @Override
    protected void cancelled() {
        onFinished.run();
        onCancelled.run();
    }
}
