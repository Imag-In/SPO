package org.icroco.picture.views.task;

import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FxRunAllScope<T> extends StructuredTaskScope<T> {
    private final CountDownLatch latch;
    private final Queue<T>       results;

    public FxRunAllScope(TaskService taskService, final String title, final int nbOfTasks) {
        latch = new CountDownLatch(nbOfTasks);
        results = new ConcurrentLinkedQueue<>();
        Task<Void> task = taskService.runAndWait(() -> new AbstractTask<Void>() {
            @Override
            protected Void call() throws Exception {
                boolean allTaskDone = false;
                long    lastCount   = 0;

                updateTitle(title);
                updateProgress(lastCount, nbOfTasks);
                while (!allTaskDone) {
                    lastCount = latch.getCount();
                    allTaskDone = latch.await(50, TimeUnit.MILLISECONDS);
                    if (lastCount != latch.getCount()) {
                        long progress = (nbOfTasks - lastCount) + 1;
                        updateProgress(progress, nbOfTasks);
                    }
                }
                return null;
            }
        }).orElseThrow();

        Thread.ofVirtual().name("FxRunAllScope").start(task);
    }

    @Override
    protected void handleComplete(Subtask<? extends T> subtask) {
        super.handleComplete(subtask);
        latch.countDown();
        results.add(subtask.get());
//        log.info("Subtask complete: {}", subtask.state());
    }

    public List<T> getValues() {
        return results.stream().toList();
    }
}
