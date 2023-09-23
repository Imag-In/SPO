package org.icroco.picture.views.util;

import javafx.application.Platform;
import org.springframework.lang.NonNull;

import java.util.concurrent.Executor;

public class FxPlatformExecutor implements Executor {
    @Override
    public void execute(@NonNull Runnable command) {
        Platform.runLater(command);
    }

    public static void fxRun(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}
