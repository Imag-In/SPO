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

    /**
     * Execute Task in background
     */
    public <T> CompletableFuture<T> supply(final Task<T> task) {
        log.debug("Start new task: {}", task);
        taskController.addTask(task);
        return CompletableFuture.supplyAsync(() -> {
            task.run();
            try {
                return task.get();
            }
            catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Unexpected error while executing this task", e);
            }
        });
    }


    public void notifyLater(ApplicationEvent event) {
        Platform.runLater(() -> eventBus.multicastEvent(event));
    }

}
