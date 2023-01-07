package org.icroco.picture.ui.util;

import javafx.scene.image.Image;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.config.PictureConfiguration;
import org.icroco.picture.ui.model.MediaFile;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.nio.file.Path;

@Component
@Slf4j
public class MediaLoader {

    private static Image loading = loadImage(MediaLoader.class.getResource("/images/LoaDING_2.gif"));

    @Cacheable(cacheNames = PictureConfiguration.THUMBNAILS, unless = "#result == null")
    public Image loadThumbnail(Path p) {
        try {
//            log.info("Cache miss: {}", p);
            return new Image(p.toUri().toString(), 400D, 0, true, false);
        }
        catch (Exception e) {
            log.error("invalid file: {}", p, e);
        }
        return null;
    }

    public Image loadThumbnail(MediaFile p) {
        return loadThumbnail(p.fullPath());
    }

    @SneakyThrows
    private static Image loadImage(URL url) {
        return new Image(url.toURI().toString());
    }

    @Cacheable(cacheNames = PictureConfiguration.FULL_SIZE, unless = "#result == null")
    public Image loadImage(Path p) {
        try {
            return new Image(p.toUri().toString(), 1024D, 0, true, true);
        }
        catch (Exception e) {
            log.error("invalid file: {}", p, e);
        }
        return null;
    }

    @CachePut(key = "p")
    public Image updateCache(Path p, Image image) {
        return image;

    }
}
