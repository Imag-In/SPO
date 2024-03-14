package org.icroco.picture.util;

import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.jooq.lambda.Unchecked;

import java.awt.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

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

    public static Predicate<MouseEvent> mouseNonContiguousSelection() {
        return isMac() ? MouseEvent::isMetaDown : MouseEvent::isControlDown;
    }

    public static Predicate<MouseEvent> mouseContiguousSelection() {
        return MouseEvent::isShiftDown;
    }


    public static Predicate<KeyEvent> keyNonContiguousSelection() {
        return isMac() ? KeyEvent::isMetaDown : KeyEvent::isControlDown;
    }

    public static void browseFile(Path path) {
        if (Desktop.isDesktopSupported()) {
            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
                Unchecked.runnable(() -> Desktop.getDesktop().browseFileDirectory(path.toFile())).run();
            } else if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Unchecked.runnable(() -> Desktop.getDesktop().open(path.toFile())).run();
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
        moveToTrash(List.of(path));
    }

    public static void moveToTrash(Collection<Path> paths) {
        // TODO: Use Task Service (parameter)
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
            Unchecked.runnable(() -> {
                paths.stream()
                     .peek(path -> log.info("File moved to trash: '{}'", path))
                     .forEach(path -> Desktop.getDesktop().moveToTrash(path.toFile()));
            }).run();
        } else {
            log.warn("Desktop feature / MOVE_TO_TRASH is not supported for this os: {}", System.getProperty("os.name").toLowerCase());
        }
    }
}
