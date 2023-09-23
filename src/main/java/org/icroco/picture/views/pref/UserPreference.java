package org.icroco.picture.views.pref;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
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
        Integer lastViewed;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Gallery {
        Integer cellWidth = 128;
        Integer cellHeight = 128;
    }

    private MainWindow mainWindow = new MainWindow(0D, 0D, 800D, 600D);
    private Collection collection = new Collection(-1);
    private Gallery grid = new Gallery(128, 128);
}
