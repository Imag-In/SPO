package org.icroco.picture.views.task;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public interface IFxCallable<S> extends Callable<TaskResult<S>>, Supplier<TaskResult<S>> {
    default String getTitle() {
        return "";
    }

    record FxCallable<FX>(String title, Supplier<FX> supplier) implements IFxCallable<FX> {
        @Override
        public TaskResult<FX> get() {
            return new TaskResult<>(title, supplier.get());
        }

        @Override
        public TaskResult<FX> call() throws Exception {
            return get();
        }
    }

    static <P> IFxCallable<P> wrap(String title, Supplier<P> supplier) {
        return new FxCallable<>(title, supplier);
    }
}
