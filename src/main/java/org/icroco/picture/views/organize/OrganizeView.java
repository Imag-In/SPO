package org.icroco.picture.views.organize;

import jakarta.annotation.PostConstruct;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.views.ViewConfiguration;
import org.icroco.picture.views.organize.collections.CollectionView;
import org.icroco.picture.views.organize.details.DetailsView;
import org.icroco.picture.views.organize.gallery.GalleryView;
import org.icroco.picture.views.util.FxView;
import org.springframework.stereotype.Component;

import static org.fxmisc.wellbehaved.event.EventPattern.anyOf;
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

        addInputMap(root,
                    sequence(consume(keyPressed(KeyCode.ENTER), galleryView::enterPressed),
                             consume(anyOf(keyPressed(KeyCode.ESCAPE), keyPressed(KeyCode.LEFT, KeyCombination.META_ANY)),
                                     galleryView::escapePressed),
//                             consume(anyOf(keyPressed(KeyCode.ESCAPE),
//                                           keyPressed(KeyCode.LEFT, SystemUtil.isMac()
//                                                                    ? KeyCombination.META_ANY
//                                                                    : KeyCombination.CONTROL_ANY)),
//                                     galleryView::escapePressed),
                             consume(keyPressed(KeyCode.LEFT), galleryView::leftPressed),
                             consume(keyPressed(KeyCode.RIGHT), galleryView::rightPressed),
                             consume(keyPressed(KeyCode.UP), galleryView::upPressed),
                             consume(keyPressed(KeyCode.DOWN), galleryView::downPressed),
                             consume(keyPressed(KeyCode.E), galleryView::editPressed),
                             consume(keyPressed(KeyCode.A), galleryView::keepPressed),
                             consume(keyPressed(KeyCode.D), galleryView::throwPressed),
                             consume(keyPressed(KeyCode.S), galleryView::undecidePressed),
                             consume(keyPressed(KeyCode.PAGE_DOWN), galleryView::pageDownPressed),
                             consume(keyPressed(KeyCode.PAGE_UP), galleryView::pageUpPressed)
                    )
        );
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
