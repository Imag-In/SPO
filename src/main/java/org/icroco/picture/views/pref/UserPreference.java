package org.icroco.picture.views.pref;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.model.Dimension;
import org.icroco.picture.util.*;
import org.icroco.picture.views.theme.SamplerTheme;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

@Getter
@Setter
@RequiredArgsConstructor
@Slf4j
public class UserPreference {

    @Setter
    @Getter
    public static class General {
        @JsonSerialize(converter = LocaleCustomConverter.class)
        @JsonDeserialize(converter = StringToCustomLocaleConverter.class)
        @PropertySettings(category = "Appearance", groupOrder = 1, displayName = "Language")
        private       Locale                       locale         = Constant.getDefaultLocale();
        @JsonIgnore
        private final SimpleObjectProperty<Locale> localeProperty = new SimpleObjectProperty<>(locale);

        public void setLocale(Locale locale) {
            this.locale = locale;
            localeProperty.set(locale);
        }

        @JsonIgnore
        public ObjectProperty<Locale> localeProperty() {
            return localeProperty;
        }

        //
//        public Locale getLocal() {
//            return Objects.requireNonNullElse(local, Constant.getDefaultLocale());
//        }
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Window {
        private Double       posX;
        private Double       posY;
        private Double       width;
        private Double       height;
        private int          screenIdx   = -1;
        private boolean      isMaximized = false;
        @PropertySettings(category = "Appearance", groupOrder = 2, displayName = "Theme")
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

        @JsonIgnore
        public void restoreWindowDimension(Stage primaryStage) {
            var scene = primaryStage.getScene();
            if (exist()) {
                scene.getWindow().setX(getPosX());
                scene.getWindow().setY(getPosY());
                scene.getWindow().setWidth(getWidth());
                scene.getWindow().setHeight(getHeight());
            } else {
                primaryStage.centerOnScreen();
                primaryStage.setWidth(Screen.getPrimary().getBounds().getWidth() - 100);
                primaryStage.setHeight(Screen.getPrimary().getBounds().getHeight() - 50);
            }
            if (isMaximized()) {
                primaryStage.setMaximized(true);
            }
        }

    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettingsDialog {
        private Dimension dimension;
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
    public static class Safety {
        @PropertySettings(category = "Safety", groupOrder = 2, propertyOrder = 1, isEditable = false, displayName = "Delete or move files:")
        private Boolean deleteFiles;
        @PropertySettings(category = "Safety", propertyOrder = 2, isEditable = false, displayName = "Move files into directory:")
        @JsonSerialize(using = PathSerializer.class)
        private Path    backupDir;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Gallery {
        private Integer cellWidth     = 128;
        private Integer cellHeight    = 128;
        private Integer cellPerRow    = 5;
        private Integer maxCellPerRow = 12;
        private Integer gridZoomFactor;

        public Integer getMaxCellPerRow() {
            return Math.max(12, Objects.requireNonNullElse(maxCellPerRow, 12));
        }

        public Integer getCellPerRow() {
            return Objects.requireNonNullElse(cellPerRow, 5);
        }
    }

    private General general    = new General();
    private Window  mainWindow = new Window(Double.MIN_VALUE, Double.MIN_VALUE, 1024D, 800D, -1, false, null);
    private Window  diffWindow = new Window(Double.MIN_VALUE, Double.MIN_VALUE, 1024D, 800D, -1, false, null);
    private Collection     collection  = new Collection(-1, null);
    private Gallery        grid        = new Gallery(128, 128, 0, 5, 12);
    private SettingsDialog settings    = new SettingsDialog(new Dimension(600, 400));
    private String         catalogName = "pictures";
    private Safety  safety     = new Safety(false, Path.of(System.getProperty("imagin.spo.backup",
                                                                              STR."\{System.getProperty("user.home")}\{File.separatorChar}SPO_BIN")));

    public void setLastViewed(int id, Path path) {
        collection.setLastViewed(id);
        collection.setLastPath(path);
    }

    public String getCatalogName() {
        return Objects.requireNonNullElse(catalogName, "pictures");
    }
}
