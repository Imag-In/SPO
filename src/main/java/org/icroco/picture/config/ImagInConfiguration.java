package org.icroco.picture.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.hash.IHashGenerator;
import org.icroco.picture.hash.JdkHashGenerator;
import org.icroco.picture.metadata.DefaultMetadataExtractor;
import org.icroco.picture.metadata.IMetadataExtractor;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.model.Thumbnail;
import org.icroco.picture.thumbnail.IThumbnailGenerator;
import org.icroco.picture.thumbnail.ImgscalrGenerator;
import org.icroco.picture.views.util.Constant;
import org.icroco.picture.views.util.FxPlatformExecutor;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@Slf4j
public class ImagInConfiguration {

    public static final String CACHE_THUMBNAILS      = "thumbnails";
    public static final String CACHE_IMAGE_FULL_SIZE = "imageFullSize";
    public static final String CACHE_IMAGE_HEADER    = "imageHeader";

    public static final String CACHE_CATALOG = "catalog";
    public static final String DIRECTORY_WATCHER = "DirWatch";
    public static final String FX_EXECUTOR       = "FX_EXECUTOR";
    public static final String IMAG_IN_EXECUTOR  = "IMAG_IN_EXEC";

    @Bean(name = CACHE_THUMBNAILS)
    public CaffeineCache thumbnails() {
        return new CaffeineCache(CACHE_THUMBNAILS,
                                 Caffeine.<Long, Thumbnail>newBuilder()
                                         .recordStats()
//                                         .softValues()
                                         .maximumSize(1000) // TODO: Compute this at runtime, based on RAM and -Xmx.
                                         .removalListener((key, value, cause) -> Platform.runLater(() -> {
                                             if (key != null) {
                                                 ((MediaFile) key).setLoadedInCache(false);
                                             }
                                         }))
//                                         .expireAfterAccess(1, TimeUnit.DAYS)
                                         .build());
    }

    @Bean(name = CACHE_IMAGE_FULL_SIZE)
    public CaffeineCache fullSize() {
        return new CaffeineCache(CACHE_IMAGE_FULL_SIZE,
                                 Caffeine.newBuilder()
//                                         .softValues()
                                         .recordStats()
                                         .maximumSize(50) // TODO: Compute this at runtime, based on RAM and -Xmx.
                                         .expireAfterAccess(1, TimeUnit.HOURS)
                                         .build());
    }

    @Bean(name = CACHE_CATALOG)
    public CaffeineCache catalogCache() {
        return new CaffeineCache(CACHE_CATALOG,
                                 Caffeine.newBuilder()
//                                         .softValues()
                                         .recordStats()
                                         .build());
    }

    @Bean(name = CACHE_IMAGE_HEADER)
    public CaffeineCache catalogImageHeader() {
        return new CaffeineCache(CACHE_IMAGE_HEADER,
                                 Caffeine.newBuilder()
                                         .recordStats()
                                         .maximumSize(200) // TODO: Compute this at runtime, based on RAM and -Xmx.
                                         .build());
    }

    @Bean(IMAG_IN_EXECUTOR)
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

    @Bean(name = FX_EXECUTOR)
    Executor fxExecutor() {
        return new FxPlatformExecutor();
    }

    @Bean
    public TaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
        executor.setThreadNamePrefix("iiScheduler-");
        executor.initialize();

        return executor;
    }

    @Bean(name = DIRECTORY_WATCHER, destroyMethod = "shutdownNow")
    public ExecutorService directyWatcher() {
        return Executors.newSingleThreadExecutor();
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
