package org.icroco.picture.views.pref;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;


@Slf4j
class UserPreferenceServiceTest {

    @Test
    void chekk_conf_file() {
        log.info("Config: {}", UserPreferenceService.PREF_FILENAME);
        Assertions.assertThat(UserPreferenceService.PREF_FILENAME).endsWith(Path.of("configuration.yml"));
    }
}