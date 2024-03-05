package org.icroco.picture.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import net.samuelcampos.usbdrivedetector.USBDeviceDetectorManager;
import net.samuelcampos.usbdrivedetector.events.DeviceEventType;
import org.icroco.picture.event.UsbStorageDeviceEvent;
import org.icroco.picture.hash.IHashGenerator;
import org.icroco.picture.hash.JdkHashGenerator;
import org.icroco.picture.metadata.DefaultMetadataExtractor;
import org.icroco.picture.metadata.IKeywordManager;
import org.icroco.picture.metadata.IMetadataExtractor;
import org.icroco.picture.model.Keyword;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.model.Thumbnail;
import org.icroco.picture.thumbnail.IThumbnailGenerator;
import org.icroco.picture.thumbnail.ImgscalrGenerator;
import org.icroco.picture.util.Constant;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.FxPlatformExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@EnableCaching
@Slf4j
public class SpoConfiguration {

    public static final  String CACHE_THUMBNAILS      = "thumbnails";
    private static final String CACHE_THUMBNAILS_RAW  = CACHE_THUMBNAILS + "-raw";
    public static final  String CACHE_IMAGE_FULL_SIZE = "imageFullSize";
    public static final String CACHE_IMAGE_HEADER    = "imageHeader";

    public static final String CACHE_CATALOG     = "catalog";
    public static final String CACHE_KEYWORD = "keyword";
    public static final String CACHE_KEYWORD_RAW = CACHE_KEYWORD + "-raw";
    public static final String DIRECTORY_WATCHER = "DirWatch";
    public static final String FX_EXECUTOR       = "FX_EXECUTOR";
    public static final String IMAG_IN_EXECUTOR  = "IMAG_IN_EXEC";


    @Bean(name = CACHE_THUMBNAILS_RAW)
    public Cache thumbnails() {
        return new CaffeineCache(CACHE_THUMBNAILS_RAW,
                                 Caffeine.<Long, Thumbnail>newBuilder()
                                         .recordStats()
//                                         .softValues()
                                         .maximumSize(1000) // TODO: Compute this at runtime, based on RAM and -Xmx.
                                         .removalListener((key, _, _) -> Platform.runLater(() -> {
                                             if (key != null) {
                                                 ((MediaFile) key).setLoadedInCacheProperty(false);
                                             }
                                         }))
//                                         .expireAfterAccess(1, TimeUnit.DAYS)
                                         .build());
    }

    @Bean(name = CACHE_THUMBNAILS)
    public Map<MediaFile, Thumbnail> rawThumbnails(@Qualifier(CACHE_THUMBNAILS_RAW) Cache cache) {
        return ((com.github.benmanes.caffeine.cache.Cache<MediaFile, Thumbnail>) cache.getNativeCache()).asMap();
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

    @Bean(name = CACHE_KEYWORD_RAW)
    public CaffeineCache rawTagCache() {
        return new CaffeineCache(CACHE_KEYWORD_RAW,
                                 Caffeine.newBuilder()
                                         .build());
    }

    @Bean(name = CACHE_KEYWORD)
    public Map<String, Keyword> tagCache(@Qualifier(CACHE_KEYWORD_RAW) Cache cache) {
        return ((com.github.benmanes.caffeine.cache.Cache<String, Keyword>) cache.getNativeCache()).asMap();
    }

    @Bean(IMAG_IN_EXECUTOR)
    public TaskExecutor threadPoolTaskExecutor() {
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

    @Bean(destroyMethod = "close")
    USBDeviceDetectorManager createUsbDriveDetector(TaskService taskService) {
        var usbDetector = new USBDeviceDetectorManager();
        usbDetector.addDriveListener(evt -> {
            log.info("USB detected: {}", evt);
            taskService.sendEvent(UsbStorageDeviceEvent.builder()
                                                       .deviceName(evt.getStorageDevice().getDeviceName())
                                                       .rootDirectory(evt.getStorageDevice().getRootDirectory().toPath())
                                                       .type(map(evt.getEventType()))
                                                       .source(SpoConfiguration.this)
                                                       .build());
        });
        return usbDetector;
    }

    private static UsbStorageDeviceEvent.EventType map(DeviceEventType eventType) {
        return switch (eventType) {
            case CONNECTED -> UsbStorageDeviceEvent.EventType.CONNECTED;
            case null, default -> UsbStorageDeviceEvent.EventType.REMOVED;
        };
    }

    @Bean(name = DIRECTORY_WATCHER, destroyMethod = "shutdownNow")
    public ExecutorService directyWatcher() {
        return Executors.newSingleThreadExecutor();
    }

    @Bean
    public IMetadataExtractor metadataExtractor(IKeywordManager tagManager, TaskService taskService) {
        return new DefaultMetadataExtractor(tagManager, taskService);
    }

    @Bean
    public IThumbnailGenerator thumbnailGenerator(final IHashGenerator hashGenerator,
                                                  final IMetadataExtractor metadataExtractor) {
        return new ImgscalrGenerator(metadataExtractor);
    }

    @Bean
    public IHashGenerator hashGenerator() {
        return new JdkHashGenerator();
    }
}
