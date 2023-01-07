package org.icroco.picture.ui.model.mapper;

import org.assertj.core.api.Assertions;
import org.icroco.picture.ui.model.Tag;
import org.icroco.picture.ui.model.TagTest;
import org.junit.jupiter.api.Test;


class TagMapperTest {
    private final TagMapper mapper = new TagMapperImpl();

    @Test
    void should_map_from_db() {
        Tag tag = TagTest.DUMMY;

        Tag copy = mapper.map(mapper.map(tag));

        Assertions.assertThat(copy).isEqualTo(tag);
    }
}