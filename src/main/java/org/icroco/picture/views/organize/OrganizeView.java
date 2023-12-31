package org.icroco.picture.views.organize;

import jakarta.annotation.PostConstruct;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.views.ViewConfiguration;
import org.icroco.picture.views.organize.collections.CollectionView;
import org.icroco.picture.views.organize.details.DetailsView;
import org.icroco.picture.views.organize.gallery.GalleryView;
import org.icroco.picture.views.util.FxView;
import org.springframework.stereotype.Component;

import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.InputMap.consume;
import static org.fxmisc.wellbehaved.event.InputMap.sequence;
import static org.fxmisc.wellbehaved.event.Nodes.addInputMap;

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

        addInputMap(root, sequence(consume(keyPressed(KeyCode.ESCAPE), galleryView::escapePressed)));
        addInputMap(root, sequence(consume(keyPressed(KeyCode.LEFT), galleryView::leftPressed)));
        addInputMap(root, sequence(consume(keyPressed(KeyCode.RIGHT), galleryView::rightPressed)));
        addInputMap(root, sequence(consume(keyPressed(KeyCode.E), galleryView::editPressed)));
        addInputMap(root, sequence(consume(keyPressed(KeyCode.A), galleryView::keepPressed)));
        addInputMap(root, sequence(consume(keyPressed(KeyCode.D), galleryView::throwPressed)));
        addInputMap(root, sequence(consume(keyPressed(KeyCode.S), galleryView::undecidePressed)));
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
