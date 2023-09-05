package org.icroco.picture.ui;

import jakarta.annotation.PostConstruct;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.javafx.SceneReadyEvent;
import org.icroco.picture.ui.collections.CollectionView;
import org.icroco.picture.ui.details.DetailsView;
import org.icroco.picture.ui.gallery.GalleryView;
import org.icroco.picture.ui.navigation.NavigationView;
import org.icroco.picture.ui.status.StatusBarView;
import org.icroco.picture.ui.util.Resources;
import org.scenicview.ScenicView;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.prefs.BackingStoreException;

//@FxViewBinding(id = "main", fxmlLocation = "main.fxml", isPrimary = true)
@Slf4j
@RequiredArgsConstructor
@Component
public class MainView implements FxView<BorderPane> {

    private final BorderPane     root = new BorderPane();
    private final GalleryView    galleryView;
    private final NavigationView navView;
    private final StatusBarView  statusView;
    private final CollectionView collectionView;
    private final DetailsView    detailsView;

//    @FXML
//    AnchorPane importContainer;
//    @FXML
//    BorderPane selectContainer;

//    @Autowired
//    TaskView taskView;

    @PostConstruct
    protected void initializedOnce() {
        log.info("Primary screen: {}", Screen.getPrimary());
        Screen.getScreens().forEach(screen -> {
            log.info("Screen: {}", screen);
        });
        //main.setLeft(viewManager.loadView());
        root.getStyleClass().remove("root");
        root.getStyleClass().add("navigation-page");

        root.setTop(navView.getRootContent());
        root.setBottom(statusView.getRootContent());

        BorderPane selectContainer = new BorderPane();

        selectContainer.setLeft(collectionView.getRootContent());
        selectContainer.setCenter(galleryView.getRootContent());
        selectContainer.setRight(detailsView.getRootContent());

        root.setCenter(selectContainer);
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

    @Override
    public BorderPane getRootContent() {
        return root;
    }
}
