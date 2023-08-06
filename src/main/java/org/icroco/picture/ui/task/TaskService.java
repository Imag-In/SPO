package org.icroco.picture.ui.task;

import javafx.application.Platform;
import javafx.concurrent.Task;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
@AllArgsConstructor
@Slf4j
public class TaskService {
    private final ApplicationEventMulticaster eventBus;
    private final TaskController              taskController;
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
            Platform.runLater(() -> taskController.addTask(task));
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

    public CompletableFuture<Void> supply(final Runnable task) {
        return CompletableFuture.runAsync(task, executor);
    }


    /**
     * post an event into the bus through Fx Thread.
     */
    public void sendEventIntoFx(final ApplicationEvent event) {
        Platform.runLater(() -> {
            log.info("Send Event: {}", event.getClass().getSimpleName());
            eventBus.multicastEvent(event);
        });
//        fxRun(() -> eventBus.multicastEvent(event));
    }

    public void sendEvent(final ApplicationEvent event) {
        supply(() -> eventBus.multicastEvent(event));
    }

    public static void fxRun(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

}
