package org.icroco.picture.ui.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;
import org.threeten.extra.AmountFormats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.stream.Stream;

@UtilityClass
@Slf4j
public class FileUtil {
    public static boolean isEmpty(Path path) {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return entries.findFirst().isEmpty();
            }
            catch (IOException ex) {
                log.debug("Un-expected error", ex);
                return true;
            }
        }

        return false;
    }

    public static boolean isLastDir(Path path) {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path).filter(Files::isDirectory)) {
                return entries.findFirst().isEmpty();
            }
            catch (IOException ex) {
                log.debug("Un-expected error", ex);
                return true;
            }
        }

        return false;
    }

    public static boolean isNotEmpty(Path path) {
        return !isEmpty(path);
    }

    public static void print(StopWatch watch) {
        if (watch.isRunning()) {
            watch.stop();
        }
        log.info(watch.getId() + " Time: " + AmountFormats.wordBased(Duration.ofMillis(watch.getTotalTimeMillis()), Locale.getDefault()));
        for (StopWatch.TaskInfo t : watch.getTaskInfo()) {
            log.info("   " +
                     t.getTaskName() + " Time: " + AmountFormats.wordBased(Duration.ofMillis(t.getTimeMillis()), Locale.getDefault()));
        }
    }

}
