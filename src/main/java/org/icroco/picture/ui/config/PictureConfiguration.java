package org.icroco.picture.ui.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import javafx.application.Platform;
import org.icroco.picture.ui.util.Constant;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class PictureConfiguration {

    public static final String THUMBNAILS = "thumbnails";
    public static final String FULL_SIZE = "fullSize";

    @Bean(name = THUMBNAILS)
    public CaffeineCache thumbnails() {
        return new CaffeineCache(THUMBNAILS,
                                 Caffeine.newBuilder()
                                         .recordStats()
                                         .maximumSize(10000)
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
}
