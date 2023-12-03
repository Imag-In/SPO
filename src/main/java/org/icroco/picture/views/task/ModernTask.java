package org.icroco.picture.views.task;

import javafx.concurrent.Task;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.jooq.lambda.fi.util.function.CheckedFunction;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Builder
public final class ModernTask<V> extends Task<V> {

    private final long                              start       = System.currentTimeMillis();
    @Getter
    private       Duration                          duration;
    @NonNull
    private final CheckedFunction<ModernTask<V>, V> execute;
    @Builder.Default
    private final Runnable                          onFinished  = () -> {
    };
    @Builder.Default
    private final BiConsumer<ModernTask<V>, V>      onSuccess   = (t, v) -> {
    };
    @Builder.Default
    private final Consumer<Throwable>               onFailed    = (t) -> {
    };
    @Builder.Default
    private final Runnable                          onCancelled = () -> {
    };

    @Override
    protected V call() throws Exception {
        try {
            return execute.apply(this);
        } catch (Throwable e) {
            if (e instanceof Exception exception) {
                throw exception;
            }
            throw new RuntimeException(e);
        }
    }


    @Override
    protected void failed() {
        onFinished.run();
        Optional.ofNullable(getException())
                .ifPresent(onFailed);
    }

    @Override
    protected void succeeded() {
        duration = Duration.ofMillis(System.currentTimeMillis() - start);
        onFinished.run();
        onSuccess.accept(this, getValue());
    }

    @Override
    protected void cancelled() {
        onFinished.run();
        onCancelled.run();
    }

    @Override
    public void updateMessage(String message) {
        super.updateMessage(message);
    }

    @Override
    public void updateTitle(String title) {
        super.updateTitle(title);
    }

    @Override
    public void updateProgress(long workDone, long max) {
        super.updateProgress(workDone, max);
    }

    @Override
    public void updateProgress(double workDone, double max) {
        super.updateProgress(workDone, max);
    }

    @Override
    public void updateValue(V value) {
        super.updateValue(value);
    }
}
