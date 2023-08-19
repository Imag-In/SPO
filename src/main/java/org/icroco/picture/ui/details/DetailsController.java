package org.icroco.picture.ui.details;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.javafx.FxInitOnce;
import org.icroco.javafx.FxViewBinding;
import org.icroco.picture.ui.event.CollectionEvent;
import org.icroco.picture.ui.event.PhotoSelectedEvent;
import org.icroco.picture.ui.model.Tag;
import org.icroco.picture.ui.util.MediaLoader;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import static javafx.application.Platform.runLater;

@Slf4j
@Component
@FxViewBinding(id = "details", fxmlLocation = "details.fxml")
@RequiredArgsConstructor
public class DetailsController extends FxInitOnce {
    public static final String FILE_NOT_FOUND = "File Not Found";
    //    private final PersistenceService    persistenceService;
//    private final TaskService           taskService;
//    private final PersistenceService    service;
//    private final UserPreferenceService pref;
//    private final IMetadataExtractor    metadataExtractor;

    private final MediaLoader mediaLoader;

    @FXML
    public VBox      container;
    @FXML
    public TextField thumbnailType;
    @FXML
    public TextField dbId;
    @FXML
    public TextField thumbnailSize;
    @FXML
    public TextField creationDate;
    @FXML
    public TextField gps;
    @FXML
    public TextField name;

    protected void initializedOnce() {
        container.setVisible(false);
    }

    @EventListener(PhotoSelectedEvent.class)
    public void updatePhotoSelected(PhotoSelectedEvent event) {
        runLater(() -> {

            var mf = event.getMf();
            mediaLoader.getCachedValue(mf).ifPresent(t -> {
                thumbnailType.setText(mf.getThumbnailType().toString()); //map(t -> tn.getOrigin().toString()).orElse(FILE_NOT_FOUND));
                if (t.getImage() != null) {
                    thumbnailSize.setText("%d x %d".formatted((int) t.getImage().getWidth(), (int) t.getImage().getHeight()));
                }
            });
            dbId.setText(Long.toString(mf.getId()));
            name.setText(mf.getFileName());
            creationDate.setText(mf.originalDate().toString());
            gps.setText(mf.getTags().stream().map(Tag::name).collect(Collectors.joining(",")));
        });
    }

    @EventListener(CollectionEvent.class)
    public void catalogEvent(CollectionEvent event) {
        runLater(() -> {
            container.setVisible(event.getType() != CollectionEvent.EventType.DELETED);
        });
    }
}
