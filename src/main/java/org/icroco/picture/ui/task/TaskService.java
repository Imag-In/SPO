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

    public <T> CompletableFuture<T> supply(final Task<T> task, boolean visualFeedback) {
        log.debug("Start new task: {}", task);
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


    /**
     * post an event into the bus through Fx Thread.
     */
    public void fxNotifyLater(final ApplicationEvent event) {
        fxRun(() -> eventBus.multicastEvent(event));
    }

    public static void fxRun(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

}
