package org.icroco.picture.views.organize;

import jakarta.annotation.PostConstruct;
import javafx.scene.layout.BorderPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.views.organize.collections.CollectionView;
import org.icroco.picture.views.organize.details.DetailsView;
import org.icroco.picture.views.organize.gallery.GalleryView;
import org.icroco.picture.views.util.FxView;
import org.springframework.stereotype.Component;

//@FxViewBinding(id = "main", fxmlLocation = "main.fxml", isPrimary = true)
@Slf4j
@RequiredArgsConstructor
@Component
public class OrganizeView implements FxView<BorderPane> {

    private final BorderPane     root = new BorderPane();
    private final CollectionView collectionView;
    private final GalleryView    galleryView;
    private final DetailsView    detailsView;

    @PostConstruct
    protected void initializedOnce() {
        root.getStyleClass().add("v-organize");
        root.setLeft(collectionView.getRootContent());
        root.setCenter(galleryView.getRootContent());
        root.setRight(detailsView.getRootContent());

        collectionView.getPathSelectionProperty().addListener(galleryView::CollectionPathChange);
    }

    @Override
    public BorderPane getRootContent() {
        return root;
    }
}
