package org.icroco.picture.util;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

import java.awt.*;
import java.util.Arrays;

//@Component
@AllArgsConstructor
@Slf4j
public class Env {

    private Environment environment;

    @PostConstruct
    void init() {
//        log.info("Default Profile: {}", String.join(", ", environment.getDefaultProfiles()));
        log.atDebug().log(() -> "Profiles: %s".formatted(String.join(", ", environment.getActiveProfiles())));
        if (isDev()) {
            log.info("MOVE_TO_TRASH     supported: '{}'", Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH));
            log.info("APP_OPEN_URI      supported: '{}'", Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_URI));
            log.info("BROWSE            supported: '{}'", Desktop.getDesktop().isSupported(Desktop.Action.BROWSE));
            log.info("BROWSE_FILE_DIR   supported: '{}'", Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR));
            log.info("OPEN              supported: '{}'", Desktop.getDesktop().isSupported(Desktop.Action.OPEN));
        }
    }

    public boolean isDev() {
        return Arrays.stream(environment.getActiveProfiles())
                     .anyMatch("DEV"::equalsIgnoreCase);
    }
}
