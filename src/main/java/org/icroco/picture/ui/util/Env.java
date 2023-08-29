package org.icroco.picture.ui.util;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@Slf4j
public class Env {

    private Environment environment;

    @PostConstruct
    void init() {
        log.info("Default Profile: {}", Arrays.stream(environment.getDefaultProfiles()).collect(Collectors.joining(", ")));
        log.info("Default Profile: {}", Arrays.stream(environment.getActiveProfiles()).collect(Collectors.joining(", ")));
    }

    public boolean isDev() {
        return Arrays.stream(environment.getActiveProfiles())
                     .anyMatch("DEV"::equalsIgnoreCase);
    }
}
