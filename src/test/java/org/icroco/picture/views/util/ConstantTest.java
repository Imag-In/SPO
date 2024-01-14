package org.icroco.picture.views.util;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class ConstantTest {

    @Test
    void should_define_spo_home() {
        log.info("SPO Homedirr: {}", Constant.SPO_HOMEDIR);
        Assertions.assertThat(Constant.SPO_HOMEDIR.toString()).contains(Constant.SPO_DIR);
    }

}