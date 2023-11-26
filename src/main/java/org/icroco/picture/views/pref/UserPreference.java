package org.icroco.picture.views.pref;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.util.PathSerializer;
import org.icroco.picture.views.theme.SamplerTheme;

import java.nio.file.Path;

@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class UserPreference {


    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainWindow {
        private Double       posX;
        private Double       posY;
        private Double       width;
        private Double       height;
        private boolean      isMaximized = false;
        private SamplerTheme theme;

        public Double getPosX() {
            return posX == Double.MIN_VALUE ? 0D : posX;
        }

        public Double getPosY() {
            return posY == Double.MIN_VALUE ? 0D : posY;
        }

        public boolean exist() {
            return !(posX == Double.MIN_VALUE || posY == Double.MIN_VALUE);
        }
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Collection {
        private Integer lastViewed;
        @JsonSerialize(using = PathSerializer.class)
        private Path    lastPath;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Gallery {
        private Integer cellWidth  = 128;
        private Integer cellHeight = 128;
        private Integer gridZoomFactor;
    }


    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPrefSettings {
        private Double width;
        private Double height;
        private Double divider;
        private String theme;
    }

    private MainWindow       mainWindow = new MainWindow(Double.MIN_VALUE, Double.MIN_VALUE, 1024D, 800D, false, null);
    private Collection       collection = new Collection(-1, null);
    private Gallery          grid       = new Gallery(128, 128, 0);
    private UserPrefSettings userPref   = new UserPrefSettings(800D, 500D, 0.2, null);

    public void setLastViewed(int id, Path path) {
        collection.setLastViewed(id);
        collection.setLastPath(path);
    }
}
