package org.icroco.picture.ui;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import lombok.extern.slf4j.Slf4j;
import org.icroco.javafx.FxInitOnce;
import org.icroco.javafx.FxViewBinding;
import org.icroco.picture.ui.catalog.CollectionView;
import org.icroco.picture.ui.details.DetailsView;
import org.icroco.picture.ui.gallery.GalleryView;
import org.icroco.picture.ui.navigation.NavigationView;
import org.icroco.picture.ui.status.StatusBarView;
import org.springframework.beans.factory.annotation.Autowired;

@FxViewBinding(id = "main", fxmlLocation = "main.fxml", isPrimary = true)
@Slf4j
public class MainController extends FxInitOnce {
    @FXML
    BorderPane     main;
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

//    @Autowired
//    TaskView taskView;

    @Override
    protected void initializedOnce() {
        //main.setLeft(viewManager.loadView());
        main.setTop(navView.scene().getRoot());
        main.setBottom(statusView.scene().getRoot());
        main.setLeft(collectionView.scene().getRoot());
        main.setCenter(galleryView.scene().getRoot());
        main.setRight(detailsView.scene().getRoot());
//        main.setRight(taskView.scene().getRoot());
    }
}
