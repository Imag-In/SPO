package org.icroco.picture.views.task;

import javafx.application.Platform;
import javafx.concurrent.Task;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.config.SpoConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
@AllArgsConstructor
@Slf4j
public class TaskService {
    private final ApplicationEventMulticaster eventBus;
    private final TaskView                    taskView;
    @Qualifier(SpoConfiguration.IMAG_IN_EXECUTOR)
    private final TaskExecutor                executor;

    /**
     * Execute Task in background
     */

    public <T> CompletableFuture<T> supply(final Task<T> task) {
        return supply(task, true);
    }

    public <T> CompletableFuture<T> supply(final Task<T> task, Consumer<T> onSucceeded) {
        task.setOnSucceeded(event -> onSucceeded.accept(task.getValue()));
        return supply(task, true);
    }

    public <T> CompletableFuture<T> supply(final Task<T> task, Consumer<T> onSucceeded, Consumer<Throwable> onFailed) {
        task.setOnSucceeded(event -> onSucceeded.accept(task.getValue()));
        task.setOnFailed(event -> onFailed.accept(task.getException()));
        return supply(task, true);
    }


    public <T> Thread vSupply(boolean visualFeedback, final Task<T> task) {
        return vSupply("FxSupplyVThread", visualFeedback, task);
    }

    public <T> Thread vSupply(final String vThreadName, final Task<T> task) {
        return vSupply(vThreadName, true, task);
    }

    public <T> Thread vSupply(final String vThreadName, boolean visualFeedback, final Task<T> task) {
        if (visualFeedback) {
            Platform.runLater(() -> taskView.addTask(task));
        }
        return Thread.ofVirtual().name(vThreadName).start(task);
    }

    public <T> CompletableFuture<T> supply(final Task<T> task, boolean visualFeedback) {
        log.debug("Start new task: {}, visualEffect: {}", task, visualFeedback);
        if (visualFeedback) {
            Platform.runLater(() -> taskView.addTask(task));
        }
        return CompletableFuture.supplyAsync(() -> {
            task.run();
            try {
                return task.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Unexpected error while executing this task", e);
            }
        }, executor);
    }

    public <T> CompletableFuture<T> supply(final Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor);
    }

    public <T, R> CompletableFuture<R> supply(final Function<T, R> task, T input) {
        return CompletableFuture.supplyAsync(() -> task.apply(input), executor);
    }

    public CompletableFuture<Void> supply(final Runnable task) {
        return CompletableFuture.runAsync(task, executor);
    }

    /**
     * We do not use thread pool TaskExecutor because if this pool is busy we want to make sure to dispatch event.
     */
    public CompletableFuture<Void> sendEvent(final ApplicationEvent event) {
//        Thread.dumpStack();
        log.debug("Send Event from source: {}, event: {}", event.getSource().getClass().getSimpleName(), event);
        return CompletableFuture.runAsync(() -> {
            eventBus.multicastEvent(event);
        });
    }

    public void runAndWait(Runnable runnable) {
        try {/* ww  w . j a v a  2s .  c  o m*/
            if (Platform.isFxApplicationThread()) {
                runnable.run();
            } else {
                FutureTask<Void> futureTask = new FutureTask<>(runnable, null);
                Platform.runLater(futureTask);
                futureTask.get();
            }
        } catch (Exception e) {
            log.error("Cannot execute and wait task into Platform thread", e);
        }
    }

    public <R> Optional<R> runAndWait(Supplier<R> supplier) {
        return runAndWait(supplier, null);
    }

    public <R> Optional<R> runAndWait(Supplier<R> supplier, R defaultValue) {
        if (Platform.isFxApplicationThread()) {
            return Optional.ofNullable(supplier.get());
        } else {
            Callable<R>   callable   = supplier::get;
            FutureTask<R> futureTask = new FutureTask<>(callable);
            Platform.runLater(futureTask);
            try {
                return Optional.ofNullable(futureTask.get());
            } catch (InterruptedException | ExecutionException e) {
                futureTask.cancel(true);
                log.error("Cannot execute task into Platform thread: {}, Exception: {}",
                          e.getLocalizedMessage(),
                          Arrays.stream(e.getStackTrace()).findFirst().orElse(null));
            }
            return Optional.ofNullable(defaultValue);
        }
    }
}
