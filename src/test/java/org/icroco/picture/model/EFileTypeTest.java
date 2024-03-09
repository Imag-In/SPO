package org.icroco.picture.model;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

@Slf4j
class EFileTypeTest {

    @Test
    void check_supported_file_type_to_read_metadata() {
        log.info("Supported extension: {}", EFileType.SUPPORTED_EXT);

        Assertions.assertThat(Arrays.stream(EFileType.values()).filter(EFileType::canReadMetadata).toList())
                  .containsExactly(EFileType.JPEG, EFileType.PNG);
    }

    @Test
    void check_supported_file_type_to_write_metadata() {
        log.info("Supported extension: {}", EFileType.SUPPORTED_EXT);

        Assertions.assertThat(Arrays.stream(EFileType.values()).filter(EFileType::canWriteMetadata).toList())
                  .containsExactly(EFileType.JPEG);
    }

}