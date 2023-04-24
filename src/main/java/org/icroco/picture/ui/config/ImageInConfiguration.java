package org.icroco.picture.ui.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.model.Thumbnail;
import org.icroco.picture.ui.util.Constant;
import org.icroco.picture.ui.util.hash.IHashGenerator;
import org.icroco.picture.ui.util.hash.JdkHashGenerator;
import org.icroco.picture.ui.util.metadata.DefaultMetadataExtractor;
import org.icroco.picture.ui.util.metadata.IMetadataExtractor;
import org.icroco.picture.ui.util.thumbnail.IThumbnailGenerator;
import org.icroco.picture.ui.util.thumbnail.ImgscalrGenerator;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@Slf4j
public class ImageInConfiguration {

    public static final String THUMBNAILS = "thumbnails";
    public static final String FULL_SIZE  = "fullSize";

    @Bean(name = THUMBNAILS)
    public CaffeineCache thumbnails() {
        return new CaffeineCache(THUMBNAILS,
                                 Caffeine.<Long, Thumbnail>newBuilder()
                                         .recordStats()
//                                         .softValues()
                                         .maximumSize(1000) // TODO: Compute this at runtime.
                                         .removalListener((key, value, cause) -> Platform.runLater(() -> {
                                             if (key != null) {
                                                 ((MediaFile) key).setLoaded(false);
                                             }
                                         }))
//                                         .expireAfterAccess(1, TimeUnit.DAYS)
                                         .build());
    }

    @Bean(name = FULL_SIZE)
    public CaffeineCache fullSize() {
        return new CaffeineCache(FULL_SIZE,
                                 Caffeine.newBuilder()
//                                         .softValues()
                                         .recordStats()
                                         .maximumSize(100)
                                         .expireAfterAccess(1, TimeUnit.HOURS)
                                         .build());
    }

    @Bean
    public TaskExecutor threadPoolTaskExecutor() {
        log.info("Nb core: {}", Constant.NB_CORE);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Constant.NB_CORE);
        executor.setMaxPoolSize(Constant.NB_CORE);
        executor.setThreadNamePrefix("iiTask-");
        executor.setKeepAliveSeconds(60);
        executor.initialize();

        return executor;
    }

    @Bean
    public TaskScheduler threadPoolTaskScheduler() {

        ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
        executor.setThreadNamePrefix("iiScheduler-");
        executor.initialize();

        return executor;
    }

    @Bean
    public IMetadataExtractor metadataExtractor() {
        return new DefaultMetadataExtractor();
    }

    @Bean
    public IThumbnailGenerator thumbnailGenerator(final IHashGenerator hashGenerator,
                                                  final IMetadataExtractor metadataExtractor) {
        return new ImgscalrGenerator(hashGenerator, metadataExtractor);
    }

    @Bean
    public IHashGenerator hashGenerator() {
        return new JdkHashGenerator();
    }
}
