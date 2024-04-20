package org.icroco.picture.event;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

@Slf4j
class IiEventTest {

    @Test
    void should_get_same_timestamp() {
        var instant = Instant.now();
        var event   = new IiEvent(this, null);
        var epoch   = event.getTimestamp();

        log.info("event timestamp: {}", instant.atZone(ZoneId.systemDefault()));
        log.info("epoch timestamp: {}", Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()));
    }
}