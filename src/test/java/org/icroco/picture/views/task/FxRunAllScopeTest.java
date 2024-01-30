package org.icroco.picture.views.task;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.Phaser;
import java.util.stream.IntStream;

@Slf4j
class FxRunAllScopeTest {

    @Test
    @Disabled
    void phaser_demo() throws InterruptedException {
        Phaser phaser = new Phaser(1);

        Assertions.assertThat(phaser.isTerminated()).isFalse();

        System.out.println(phaser.getUnarrivedParties());
        System.out.println(phaser.getRegisteredParties());
        System.out.println(phaser.getPhase());

        Thread thread = Thread.startVirtualThread(() -> {
            phaser.register();
            System.out.println("---");
            System.out.println(phaser.getUnarrivedParties());
            System.out.println(phaser.getRegisteredParties());
            System.out.println(phaser.getPhase());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                phaser.arriveAndDeregister();
                System.out.println("***");
                System.out.println(phaser.getUnarrivedParties());
                System.out.println(phaser.getRegisteredParties());
                System.out.println(phaser.getPhase());
            }

        });
        thread.join();
        System.out.println("###");
        phaser.arriveAndAwaitAdvance();
//        phaser.forceTermination();
        System.out.println(phaser.getUnarrivedParties());
        System.out.println(phaser.getRegisteredParties());
        System.out.println(phaser.getPhase());

        Assertions.assertThat(phaser.isTerminated()).isTrue();


    }

    @Test
    @Disabled
    void scope_test() {
        var taskService = Mockito.mock(TaskService.class);
//        Mockito.doReturn(null).when(taskService).runAndWait()
        try (var scope = new FxRunAllScope<>(taskService, "FOO Task")) {
            IntStream.range(1, 11)
                     .mapToObj(String::valueOf)
                     .map(s -> IFxCallable.wrap(s, () -> {
                         new LongRunningAction(s).run();
                         return null;
                     }))
                     .map(scope::fork)
                     .toList();
            scope.join();
        } catch (InterruptedException e) {
            log.error("Unexpected error", e);
        }
    }

}