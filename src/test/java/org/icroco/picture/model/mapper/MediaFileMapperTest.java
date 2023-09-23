package org.icroco.picture.model.mapper;

import org.icroco.picture.model.MediaFile;
import org.icroco.picture.model.MediaFileTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MediaFileMapperTest.TagConfig.class)
class MediaFileMapperTest {

    @Configuration
    static class TagConfig {
        @Bean
        public TagMapper createTagMapper() {
            return new TagMapperImpl();
        }

        @Bean
        public MediaFileMapper mediaFileMapper() {
            return new MediaFileMapperImpl();
        }

        @Bean
        GeoLocationMapper geoLocationMapper() {return new GeoLocationMapperImpl();}
    }

    @Autowired
    private MediaFileMapper mapper;

    @Test
    void should_map_and_back() {
        MediaFile mediaFile = MediaFileTest.DUMMY;

        MediaFile copy = mapper.map(mapper.map(mediaFile));

        assertThat(copy).isEqualTo(mediaFile);
        assertThat(copy.tags()).hasSize(2);
    }

}