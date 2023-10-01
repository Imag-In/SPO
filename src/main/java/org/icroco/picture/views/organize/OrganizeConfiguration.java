package org.icroco.picture.views.organize;

import org.icroco.picture.views.util.MediaLoader;
import org.icroco.picture.views.util.widget.ZoomDragPane;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OrganizeConfiguration {

    public static final String GALLERY_ZOOM = "galleryZoom";

    @Bean(name = GALLERY_ZOOM)
    ZoomDragPane galleryZoomPane(MediaLoader mediaLoader) {
        return new ZoomDragPane(mediaLoader);
    }
}
