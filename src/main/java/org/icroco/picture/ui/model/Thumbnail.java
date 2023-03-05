package org.icroco.picture.ui.model;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import lombok.Data;
import lombok.Getter;

import java.nio.file.Path;
import java.time.LocalDate;

@Data
public class Thumbnail {

    long id;
    private Path           fullPath;
    private Image          image;
    private String         hash;
    private LocalDate      hashDate;
    private LocalDate      lastAccess;
    private EThumbnailType origin;

    @Getter
    private final SimpleObjectProperty<Image> imageProperty = new SimpleObjectProperty<>(null);

    public Thumbnail(long id,
                     Path fullPath,
                     Image image,
                     String hash,
                     LocalDate hashDate,
                     LocalDate lastAccess,
                     EThumbnailType origin) {
        this.id = id;
        this.fullPath = fullPath;
        this.image = image;
        this.hash = hash;
        this.hashDate = hashDate;
        this.lastAccess = lastAccess;
        this.origin = origin;
    }


    public void initImageFromFx() {
        imageProperty.set(image);
    }

    public static ThumbnailBuilder builder() {return new ThumbnailBuilder();}


    public static class ThumbnailBuilder {
        private long           id;
        private Path           fullPath;
        private Image          image;
        private String         hash;
        private LocalDate      hashDate;
        private LocalDate      lastAccess;
        private EThumbnailType origin;

        ThumbnailBuilder() {}

        public ThumbnailBuilder id(long id) {
            this.id = id;
            return this;
        }

        public ThumbnailBuilder fullPath(Path fullPath) {
            this.fullPath = fullPath;
            return this;
        }

        public ThumbnailBuilder image(Image image) {
            this.image = image;
            return this;
        }

        public ThumbnailBuilder hash(String hash) {
            this.hash = hash;
            return this;
        }

        public ThumbnailBuilder hashDate(LocalDate hashDate) {
            this.hashDate = hashDate;
            return this;
        }

        public ThumbnailBuilder lastAccess(LocalDate lastAccess) {
            this.lastAccess = lastAccess;
            return this;
        }

        public ThumbnailBuilder origin(EThumbnailType origin) {
            this.origin = origin;
            return this;
        }


        public Thumbnail build() {
            return new Thumbnail(id, fullPath, image, hash, hashDate, lastAccess, origin);
        }

        public String toString() {
            return "Thumbnail.ThumbnailBuilder(id=" + this.id + ", fullPath=" + this.fullPath + ", image=" + this.image + ", hash=" + this.hash +
                   ", hashDate=" +
                   this.hashDate + ", lastAccess=" + this.lastAccess + ", origin=" + this.origin + ")";
        }
    }
}
