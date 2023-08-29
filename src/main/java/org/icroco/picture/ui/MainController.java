package org.icroco.picture.ui;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import lombok.extern.slf4j.Slf4j;
import org.icroco.javafx.FxInitOnce;
import org.icroco.javafx.FxViewBinding;
import org.icroco.javafx.SceneReadyEvent;
import org.icroco.picture.ui.collections.CollectionView;
import org.icroco.picture.ui.details.DetailsView;
import org.icroco.picture.ui.gallery.GalleryView;
import org.icroco.picture.ui.navigation.NavigationView;
import org.icroco.picture.ui.status.StatusBarView;
import org.icroco.picture.ui.util.Resources;
import org.scenicview.ScenicView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import java.util.prefs.BackingStoreException;

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
        main.getStyleClass().remove("root");
        main.getStyleClass().add("navigation-page");

        main.setTop(navView);
        main.setBottom(statusView.scene().getRoot());

        selectContainer.setLeft(collectionView);
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

    @EventListener(SceneReadyEvent.class)
    public void sceneReady(SceneReadyEvent event) throws BackingStoreException {
        log.info("READY, source: {}", event.getSource());
        event.getScene().getStylesheets().addAll(Resources.resolve("/styles/index.css"));

        Resources.getPreferences().put("FOO", "BAR");
        Resources.getPreferences().flush();
        Resources.printPreferences(Resources.getPreferences(), "");
        if (Boolean.getBoolean("SCENIC")) {
            ScenicView.show(event.getScene());
        }
//        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
//            log.info("call: {}", element);
//        }
    }
}
