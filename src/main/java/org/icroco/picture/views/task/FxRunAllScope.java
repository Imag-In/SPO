package org.icroco.picture.views.task;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Phaser;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class FxRunAllScope<T> extends StructuredTaskScope<TaskResult<T>> {
    private final AtomicInteger                  nbOfTasks   = new AtomicInteger(0);
    private final Phaser                         phaser      = new Phaser(0);
    private final AtomicReference<TaskResult<T>> latestValue = new AtomicReference<>(null);
    private final Thread                         monitor;

    @SneakyThrows
    public FxRunAllScope(TaskService taskService, final String title) {
        super("FxRunAllScope", Thread.ofVirtual().name("FxScope-", 0L).factory());
        log.debug("Starting Virtuale Thread: {}", title);
        monitor = taskService.vSupply("FxRunAllScope", true, new AbstractTask<Void>() {
            @Override
            protected Void call() {
                updateTitle(title);
                long progress = 0;
                updateProgress(progress, nbOfTasks.get());
                while (!phaser.isTerminated()) {
                    try {
                        Thread.sleep(Duration.ofMillis(50));
                        long newProgress = (nbOfTasks.get() - phaser.getUnarrivedParties());
                        if (progress != newProgress) {
                            updateProgress(newProgress, nbOfTasks.get());
                            Optional.ofNullable(latestValue.get())
                                    .ifPresent(r -> updateMessage(r.title()));
                            progress = newProgress;
                        }
                    } catch (InterruptedException e) {
                        log.trace("Thread interrupted", e);
                    }
                }
                log.debug("End of monitoring scope: {}", phaser.getUnarrivedParties());
                return null;
            }
        });
    }

    @Override
    public <U extends TaskResult<T>> Subtask<U> fork(Callable<? extends U> task) {
        nbOfTasks.incrementAndGet();
        phaser.register();
        return super.fork(task);
    }

    @SneakyThrows
    @Override
    public StructuredTaskScope<TaskResult<T>> join() throws InterruptedException {
        var result = super.join();
        try {
            phaser.forceTermination();
            try {
                monitor.interrupt();
                monitor.join();
            } catch (InterruptedException e) {
                log.warn("Thread interrupted: {}", monitor.getName());
            }
            latestValue.set(null);
        } finally {
            log.debug("Phase terminated: {}, monitor alive: {}", phaser.isTerminated(), monitor.isAlive());
        }
        return result;
    }

    @Override
    protected void handleComplete(Subtask<? extends TaskResult<T>> subtask) {
//        super.handleComplete(subtask);
        phaser.arriveAndDeregister();
        try {
            latestValue.set(subtask.get());
        } catch (Throwable t) {
            log.error("Error in task", subtask.exception());
        }
    }
}
