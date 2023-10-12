package org.icroco.picture.model.mapper;

import org.assertj.core.api.Assertions;
import org.icroco.picture.model.Tag;
import org.icroco.picture.model.TagTest;
import org.icroco.picture.persistence.mapper.TagMapper;
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