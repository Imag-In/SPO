package org.icroco.picture.util;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

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
    }

    public boolean isDev() {
        return Arrays.stream(environment.getActiveProfiles())
                     .anyMatch("DEV"::equalsIgnoreCase);
    }
}
