package org.icroco.picture.views;

import javafx.beans.property.SimpleStringProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ViewConfiguration {
    public static final String CURRENT_VIEW       = "currentView";
    public static final String V_NAVIGATION       = "v-navigation";
    public static final String V_IMPORT           = "v-import";
    public static final String V_ORGANIZE         = "v-organize";
    public static final String V_DETAILS          = "v-details";
    public static final String V_GALLERY          = "v-gallery";
    public static final String V_MAIN             = "v-main";
    public static final String V_MEDIA_COLLECTION = "v-media-collection";
    public static final String V_MEDIA_DETAILS    = "v-media-details";
    public static final String V_STATUSBAR        = "v-status";
    public static final String V_TASKS            = "v-tasks";
    public static final String V_PREFERENCES      = "v-preferences";
    public static final String V_REPAIR = "v-repair";


    @Bean(CURRENT_VIEW)
    SimpleStringProperty currentView() {
        return new SimpleStringProperty();
    }

}
