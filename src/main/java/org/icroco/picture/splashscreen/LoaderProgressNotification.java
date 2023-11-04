package org.icroco.picture.splashscreen;

import javafx.application.Preloader;
import lombok.Getter;
import org.springframework.lang.NonNull;

@Getter
public class LoaderProgressNotification extends Preloader.ProgressNotification {
    private final String details;

    /**
     * Constructs a progress notification.
     *
     * @param progress a value indicating the progress.
     *                 A negative value for progress indicates that the progress is
     *                 indeterminate. A value between 0 and 1 indicates the amount
     *                 of progress. Any value greater than 1 is interpreted as 1.
     */
    public LoaderProgressNotification(@NonNull double progress, @NonNull String details) {
        super(progress);
        this.details = details;
    }
}
