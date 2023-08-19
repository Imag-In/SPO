package org.icroco.picture.ui.util;

import javafx.application.Platform;
import org.springframework.lang.NonNull;

import java.util.concurrent.Executor;

public class FxPlatformExecutor implements Executor {
    @Override
    public void execute(@NonNull Runnable command) {
        Platform.runLater(command);
    }
}
