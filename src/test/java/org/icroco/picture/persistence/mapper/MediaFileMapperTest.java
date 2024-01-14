package org.icroco.picture.persistence.mapper;

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
        public KeywordMapper createTagMapper() {
            return new KeywordMapperImpl();
        }

        @Bean
        public MediaFileMapper mediaFileMapper() {
            return new MediaFileMapperImpl();
        }

        @Bean
        GeoLocationMapper geoLocationMapper() {return new GeoLocationMapperImpl();}

        @Bean
        CameraMapper cameraMapper() {
            return new CameraMapperImpl();
        }

        @Bean
        DimensionMapper dimensionMapper() {
            return new DimensionMapperImpl();
        }
    }

    @Autowired
    private MediaFileMapper mapper;

    @Test
    void should_map_and_back() {
        MediaFile mediaFile = MediaFileTest.DUMMY;

        MediaFile copy = mapper.toDomain(mapper.toEntity(mediaFile));

        assertThat(copy).isEqualTo(mediaFile);
        assertThat(copy.tags()).hasSize(2);
    }

}