package org.icroco.picture.ui.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.icroco.picture.ui.util.Constant;
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
public class ImageInConfiguration {

    public static final String THUMBNAILS = "thumbnails";
    public static final String FULL_SIZE  = "fullSize";

    @Bean(name = THUMBNAILS)
    public CaffeineCache thumbnails() {
        return new CaffeineCache(THUMBNAILS,
                                 Caffeine.newBuilder()
                                         .recordStats()
                                         .maximumSize(2000) // TODO: Compute this at runtime.
                                         .expireAfterAccess(1, TimeUnit.DAYS)
                                         .build());
    }

    @Bean(name = FULL_SIZE)
    public CaffeineCache fullSize() {
        return new CaffeineCache(FULL_SIZE,
                                 Caffeine.newBuilder()
                                         .recordStats()
                                         .maximumSize(100)
                                         .expireAfterAccess(1, TimeUnit.HOURS)
                                         .build());
    }

    @Bean
    public TaskExecutor threadPoolTaskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Constant.NB_CORE);
        executor.setMaxPoolSize(Constant.NB_CORE);
        executor.setThreadNamePrefix("IiTask");
        executor.initialize();

        return executor;
    }

    @Bean
    public TaskScheduler threadPoolTaskScheduler() {

        ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
        executor.setThreadNamePrefix("IiSceduler");
        executor.initialize();

        return executor;
    }

    @Bean
    public IMetadataExtractor metadataExtractor() {
        return new DefaultMetadataExtractor();
    }

    @Bean
    public IThumbnailGenerator thumbnailGenerator(final IMetadataExtractor metadataExtractor) {
        return new ImgscalrGenerator(metadataExtractor);
    }
}
