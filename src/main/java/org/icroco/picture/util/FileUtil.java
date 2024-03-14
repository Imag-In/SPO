package org.icroco.picture.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.model.EFileType;
import org.springframework.util.StopWatch;
import org.threeten.extra.AmountFormats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.Integer.parseInt;

@UtilityClass
@Slf4j
public class FileUtil {
    public static Pattern
            DATE_TINE_PATTERN_1 =
            Pattern.compile(
                    ".*[-_](?<DATE>(?<YEAR>[0-9]{4})(?<MONTH>[0-9]{2})(?<DAY>[0-9]{2}))([T_-](?<TIME>(?<HOUR>[0-9]{2})(?<MIN>[0-9]{2})(?<SEC>[0-9]{2})?))?.*");

    public static Pattern
            DATE_TINE_PATTERN_2 =
            Pattern.compile(
                    ".*[-_](?<DATE>(?<DAY>[0-9]{2})(?<MONTH>[0-9]{2})(?<YEAR>[0-9]{4}))([_-](?<TIME>(?<HOUR>[0-9]{2})(?<MIN>[0-9]{2})(?<SEC>[0-9]{2})?))?.*");
//    public static Pattern DATE_TINE_PATTERN = Pattern.compile(".*(-_)(?<DATE>(?<YEAR>[0-9]{4}+)(?<MONTH>[0-9]{2}+)(?<DAY>[0-9]{2}+)).*");


    public static boolean isLastDir(Path path) {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path).filter(Files::isDirectory)) {
                return entries.findFirst().isEmpty();
            } catch (IOException ex) {
                log.debug("Un-expected error", ex);
                return true;
            }
        }

        return false;
    }

    public static void print(StopWatch watch) {
        if (watch.isRunning()) {
            watch.stop();
        }
        log.info(STR."\{watch.getId()} Time: \{AmountFormats.wordBased(Duration.ofMillis(watch.getTotalTimeMillis()),
                                                                       Locale.getDefault())}");
        for (StopWatch.TaskInfo t : watch.getTaskInfo()) {
            log.info(STR."   \{t.getTaskName()} Time: \{AmountFormats.wordBased(Duration.ofMillis(t.getTimeMillis()),
                                                                                Locale.getDefault())}");
        }
    }

    public static Optional<LocalDateTime> extractDateTime(Path path) {
        return extractDateTime(path, DATE_TINE_PATTERN_1)
                .or(() -> extractDateTime(path, DATE_TINE_PATTERN_2));
    }

    public static Optional<LocalDateTime> extractDateTime(Path path, Pattern pattern) {
        var filename = path.getFileName();
        try {
            var matcher = pattern.matcher(filename.toString());
            if (matcher.matches()) {
                var year  = matcher.group("YEAR");
                var month = matcher.group("MONTH");
                var day   = matcher.group("DAY");

                var time = matcher.group("TIME");
                var hour = time == null ? 0 : parseInt(matcher.group("HOUR"));
                var min  = time == null ? 0 : parseInt(matcher.group("MIN"));
                var sec  = time == null ? 0 : matcher.group("SEC") == null ? 0 : parseInt(matcher.group("SEC"));

                return Optional.of(LocalDateTime.of(parseInt(year),
                                                    parseInt(month),
                                                    parseInt(day),
                                                    hour,
                                                    min,
                                                    sec));
            }
        } catch (DateTimeException e) {
            log.warn("Cannot parse date: '{}', error: '{}'", filename, e.getLocalizedMessage());
        }
        return Optional.empty();
    }

    public static List<Path> getAllFiles(Path rootPath) {
        try (var images = Files.walk(rootPath)) {
            return images.filter(p -> !Files.isDirectory(p))   // not a directory
                         .map(Path::normalize)
                         .filter(EFileType::isSupportedExtension)
                         .toList();
        } catch (IOException e) {
            log.error("Cannot walk through directory: '{}'", rootPath);
            return Collections.emptyList();
        }
    }
}
