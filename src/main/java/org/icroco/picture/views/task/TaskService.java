package org.icroco.picture.views.task;

import javafx.application.Platform;
import javafx.concurrent.Task;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.config.ImagInConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
@AllArgsConstructor
@Slf4j
public class TaskService {
    private final ApplicationEventMulticaster eventBus;
    private final TaskView                    taskView;
    @Qualifier(ImagInConfiguration.IMAG_IN_EXECUTOR)
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


    public <T> CompletableFuture<T> supply(final Task<T> task, boolean visualFeedback) {
        log.debug("Start new task: {}, visualEffect: {}", task, visualFeedback);
        if (visualFeedback) {
            // TODO: Use event to decouple from controller.
            Platform.runLater(() -> taskView.addTask(task));
        }
        return CompletableFuture.supplyAsync(() -> {
            task.run();
            try {
                return task.get();
            }
            catch (InterruptedException | ExecutionException e) {
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
     *
     */
    public CompletableFuture<Void> sendEvent(final ApplicationEvent event) {
        return CompletableFuture.runAsync(() -> {
            log.info("Send Event from source: {}, event: {}", event.getSource().getClass().getSimpleName(), event);
            eventBus.multicastEvent(event);
        });
    }
}
