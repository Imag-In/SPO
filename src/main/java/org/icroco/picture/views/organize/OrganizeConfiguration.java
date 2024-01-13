package org.icroco.picture.views.organize;

import javafx.beans.property.SimpleBooleanProperty;
import org.icroco.picture.views.util.MediaLoader;
import org.icroco.picture.views.util.widget.ZoomDragPane;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OrganizeConfiguration {

    public static final String GALLERY_ZOOM = "galleryZoom";
    public static final String ORGANIZE_EDIT_MODE = "OrganizeEditMode";

    @Bean(name = GALLERY_ZOOM)
    ZoomDragPane galleryZoomPane(MediaLoader mediaLoader) {
        return new ZoomDragPane(mediaLoader);
    }

    @Bean(name = ORGANIZE_EDIT_MODE)
    public SimpleBooleanProperty organizeEditMode() {
        return new SimpleBooleanProperty(false);
    }

}
