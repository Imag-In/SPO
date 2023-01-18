package org.icroco.picture.ui.model;

import javafx.scene.image.Image;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class Thumbnail {

    long id;
    private Image            thumbnail;
    private String           hash;
    private LocalDate        hashDate;
    private LocalDate        lastAccess;
    private EThumbnailStatus origin;

//    private SimpleObjectProperty<EThumbnailStatus> origin = new SimpleObjectProperty<>(EThumbnailStatus.ABSENT);
//
//    public void setThumbnailFromFile(Image thumbnail) {
//        this.thumbnail = thumbnail;
//        origin.set(EThumbnailStatus.FROM_FILE);
//    }
//
//    public void setGeneratedThumbnail(Image thumbnail) {
//        this.thumbnail = thumbnail;
//        origin.set(EThumbnailStatus.GENERATED);
//    }
}
