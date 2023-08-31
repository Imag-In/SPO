package org.icroco.picture.ui.gallery;

import jakarta.annotation.PostConstruct;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CarouselView extends VBox {

    private final ImageView                   imageView  = new ImageView();
    private final ListView<MediaFileListCell> thumbnails = new ListView<>();

    @PostConstruct
    private void postConstruct() {
        HBox.setHgrow(this, Priority.ALWAYS);
        VBox.setVgrow(imageView, Priority.ALWAYS);
        getChildren().addAll(imageView, thumbnails);
    }
}
