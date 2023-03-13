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
    public static final String FILE_NOT_FOUND = "File Not Found";
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
        var mf        = event.getFile();
        var thumbnail = mediaLoader.getCachedValue(mf);
        thumbnailType.setText(thumbnail.map(tn -> tn.getOrigin().toString()).orElse(FILE_NOT_FOUND));
        thumbnailSize.setText(thumbnail.map(tn -> "%d x %d".formatted((int) tn.getImage().getWidth(), (int) tn.getImage().getHeight())).orElse(FILE_NOT_FOUND));
        creationDate.setText(mf.originalDate().toString());
        gps.setText(mf.getTags().stream().map(Tag::name).collect(Collectors.joining(",")));

    }
}
