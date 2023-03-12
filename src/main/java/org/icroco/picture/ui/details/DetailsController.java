package org.icroco.picture.ui.details;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.javafx.FxInitOnce;
import org.icroco.javafx.FxViewBinding;
import org.icroco.picture.ui.event.PhotoSelectedEvent;
import org.icroco.picture.ui.model.Tag;
import org.icroco.picture.ui.util.MediaLoader;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
@FxViewBinding(id = "details", fxmlLocation = "details.fxml")
@RequiredArgsConstructor
public class DetailsController extends FxInitOnce {
//    private final PersistenceService    persistenceService;
//    private final TaskService           taskService;
//    private final PersistenceService    service;
//    private final UserPreferenceService pref;
//    private final IMetadataExtractor    metadataExtractor;

    private final MediaLoader mediaLoader;

    @FXML
    public TextField thumbnailType;
    @FXML
    public TextField thumbnailSize;
    @FXML
    public TextField creationDate;
    @FXML
    public TextField gps;

    protected void initializedOnce() {
    }

    @EventListener(PhotoSelectedEvent.class)
    public void updatePhotoSelected(PhotoSelectedEvent event) {
        var file = event.getFile();
        thumbnailType.setText(file.getThumbnail().get().getOrigin().toString());
        thumbnailSize.setText((int) file.getThumbnail().get().getImage().getWidth() + " x " + (int) file.getThumbnail().get().getImage().getHeight());
        creationDate.setText(file.originalDate().toString());
        gps.setText(file.getTags().stream().map(Tag::name).collect(Collectors.joining(",")));

    }
}
