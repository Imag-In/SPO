package org.icroco.picture.views.organize;

import jakarta.annotation.PostConstruct;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.BorderPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.views.ViewConfiguration;
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

    private final BorderPane root = new BorderPane();
    private final CollectionView collectionView;
    private final GalleryView    galleryView;
    private final DetailsView    detailsView;

    @PostConstruct
    protected void initializedOnce() {
        root.setId(ViewConfiguration.V_ORGANIZE);
        root.getStyleClass().add(ViewConfiguration.V_ORGANIZE);
        root.setLeft(collectionView.getRootContent());
        root.setCenter(galleryView.getRootContent());
        root.setRight(detailsView.getRootContent());
        root.visibleProperty().addListener(this::rootVisibleCb);

        collectionView.getPathSelectionProperty().addListener(galleryView::collectionPathChange);
        collectionView.getPathSelectionProperty().addListener(detailsView::collectionPathChange);
        galleryView.getRootContent().requestFocus();
    }

    private void rootVisibleCb(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
        if (newValue) {
            galleryView.getRootContent().requestFocus();
        }
    }

    @Override
    public BorderPane getRootContent() {
        return root;
    }
}
