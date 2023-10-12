package org.icroco.picture.views;

import javafx.beans.property.SimpleStringProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ViewConfiguration {
    public static final String CURRENT_VIEW = "currentView";
    public static final String V_NAVIGATION = "v-navigation";
    public static final String V_IMPORT     = "v-import";
    public static final String V_ORGANIZE   = "v-organize";
    public static final String V_DETAILS    = "v-details";
    public static final String V_GALLERY    = "v-gallery";

    @Bean(CURRENT_VIEW)
    SimpleStringProperty currentView() {
        return new SimpleStringProperty();
    }

}
