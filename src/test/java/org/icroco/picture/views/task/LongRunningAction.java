package org.icroco.picture.views.task;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
class LongRunningAction implements Runnable {
    private String threadName;
//    private Phaser ph;

    LongRunningAction(String threadName/*, Phaser ph*/) {
        this.threadName = threadName;
//        this.ph = ph;
        this.randomWait();
//        ph.register();
    }

    @Override
    public void run() {
//        ph.arriveAndAwaitAdvance();
        randomWait();
        log.info("END: {}", threadName);

//        ph.arriveAndDeregister();
    }

    // Simulating real work
    private void randomWait() {
        try {
            Thread.sleep((long) (Math.random() * 100));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}