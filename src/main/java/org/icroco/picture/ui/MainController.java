package org.icroco.picture.ui;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import lombok.extern.slf4j.Slf4j;
import org.icroco.javafx.FxInitOnce;
import org.icroco.javafx.FxViewBinding;
import org.icroco.picture.ui.collections.CollectionView;
import org.icroco.picture.ui.details.DetailsView;
import org.icroco.picture.ui.gallery.GalleryView;
import org.icroco.picture.ui.navigation.NavigationView;
import org.icroco.picture.ui.status.StatusBarView;
import org.springframework.beans.factory.annotation.Autowired;

@FxViewBinding(id = "main", fxmlLocation = "main.fxml", isPrimary = true)
@Slf4j
public class MainController extends FxInitOnce {
    @Autowired
    GalleryView    galleryView;
    @Autowired
    NavigationView navView;
    @Autowired
    StatusBarView  statusView;
    @Autowired
    CollectionView collectionView;
    @Autowired
    DetailsView    detailsView;


    @FXML
    BorderPane main;
    @FXML
    AnchorPane importContainer;
    @FXML
    BorderPane selectContainer;

//    @Autowired
//    TaskView taskView;

    @Override
    protected void initializedOnce() {
        log.info("Primary screen: {}", Screen.getPrimary());
        Screen.getScreens().forEach(screen -> {
            log.info("Screen: {}", screen);
        });
        //main.setLeft(viewManager.loadView());
        main.setTop(navView.scene().getRoot());
        main.setBottom(statusView.scene().getRoot());
        selectContainer.setLeft(collectionView.scene().getRoot());
        selectContainer.setCenter(galleryView.scene().getRoot());
        selectContainer.setRight(detailsView.scene().getRoot());
//        main.setRight(taskView.scene().getRoot());

//        main.addEventFilter(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
//            log.info("Key pressed: {}", event);
//            if (event.getCode() == KeyCode.ESCAPE) {
//                log.info("ESCAPE pressed");
//            }
//            event.consume();
//        });
    }
}
