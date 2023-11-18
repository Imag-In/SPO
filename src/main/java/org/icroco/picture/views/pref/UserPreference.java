package org.icroco.picture.views.pref;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.util.PathSerializer;

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
        private Double posX;
        private Double posY;
        private Double width;
        private Double height;
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

    private MainWindow mainWindow = new MainWindow(0D, 0D, 1024D, 800D);
    private Collection collection = new Collection(-1, null);
    private Gallery    grid       = new Gallery(128, 128, 0);

    public void setLastViewed(int id, Path path) {
        collection.setLastViewed(id);
        collection.setLastPath(path);
    }
}
