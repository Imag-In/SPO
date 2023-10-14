package org.icroco.picture.persistence.mapper;

import org.assertj.core.api.Assertions;
import org.icroco.picture.model.Keyword;
import org.icroco.picture.model.TagTest;
import org.junit.jupiter.api.Test;


class KeywordMapperTest {
    private final KeywordMapper mapper = new KeywordMapperImpl();

    @Test
    void should_map_from_db() {
        Keyword keyword = TagTest.DUMMY;

        Keyword copy = mapper.toDomain(mapper.toEntity(keyword));

        Assertions.assertThat(copy).isEqualTo(keyword);
    }
}