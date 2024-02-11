package org.icroco.picture.views.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.jooq.lambda.Unchecked;

import java.awt.*;
import java.nio.file.Path;

@Slf4j
@UtilityClass
public class SystemUtil {

    public static boolean isWindoww() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }

    public static boolean isMac() {
        return SystemUtils.IS_OS_MAC;
    }

    public static void browseFile(Path path) {
        if (Desktop.isDesktopSupported()) {
            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
                Unchecked.runnable(() -> Desktop.getDesktop().browseFileDirectory(path.toFile())).run();
            } else if (SystemUtil.isWindoww()) {
                log.warn("Windows is not supporting: {}", "Desktop.Action.BROWSE_FILE_DIR");
                final var EXPLORER_EXE = "explorer.exe";
//                final String command      = STR."\{EXPLORER_EXE} /SELECT,\"\{path.toAbsolutePath().toString()}\"";

                Unchecked.runnable(() -> {
                                       ProcessBuilder processBuilder = new ProcessBuilder();
                                       processBuilder.command(EXPLORER_EXE, STR."/SELECT,\"\{path.toAbsolutePath().toString()}\"");
                                       processBuilder.start();
                                   },
                                   t -> log.warn("Cannot open explorer: {}", t.getLocalizedMessage()))
                         .run();
            }
        } else {
            log.warn("Desktop feature is not supported for this os: {}", System.getProperty("os.name").toLowerCase());
        }
    }

    public static void moveToTrash(Path path) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
            Unchecked.runnable(() -> {
                log.info("File moved to trash: '{}'", path);
                Desktop.getDesktop().moveToTrash(path.toFile());
            }).run();
        } else {
            log.warn("Desktop feature / MOVE_TO_TRASH is not supported for this os: {}", System.getProperty("os.name").toLowerCase());
        }
    }
}
