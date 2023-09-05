package org.icroco.picture.ui.details;

import jakarta.annotation.PostConstruct;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.FxEventListener;
import org.icroco.picture.ui.FxView;
import org.icroco.picture.ui.event.CollectionEvent;
import org.icroco.picture.ui.event.PhotoSelectedEvent;
import org.icroco.picture.ui.model.EThumbnailType;
import org.icroco.picture.ui.util.MediaLoader;
import org.icroco.picture.ui.util.Styles;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Component
public class DetailsView implements FxView<GridPane> {
    public static final String FILE_NOT_FOUND = "File Not Found";
    //    private final PersistenceService    persistenceService;
//    private final TaskService           taskService;
//    private final PersistenceService    service;
//    private final UserPreferenceService pref;
//    private final IMetadataExtractor    metadataExtractor;

    private final MediaLoader mediaLoader;

    GridPane root = new GridPane();

    private final Label name          = createLabel();
    private final Label txtDbId       = new Label("Id: ");
    private final Label dbId          = createLabel(0, 30);
    private final Label creationDate  = createLabel();
    private final Label gps           = createLabel();
    private final Label size          = createLabel();
    private final Label thumbnailType = createLabel();
    private final Label thumbnailSize = createLabel();

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);

    @PostConstruct
    private void postConstruct() {
        root.setVisible(false);
        root.setPadding(new Insets(0, 10, 0, 10));
        root.add(FontIcon.of(FontAwesomeRegular.FILE), 0, 0);
        root.add(name, 1, 0);
        txtDbId.getStyleClass().add(Styles.TEXT_SMALL);
        txtDbId.setVisible(false);

        root.add(txtDbId, 2, 0);
        root.add(dbId, 3, 0);
        root.setHgap(10);
        root.setVgap(10);
        dbId.getStyleClass().add(Styles.TEXT_SMALL);

        root.add(FontIcon.of(FontAwesomeRegular.CALENDAR), 0, 1);
        root.add(creationDate, 1, 1);

        root.add(FontIcon.of(FontAwesomeSolid.LOCATION_ARROW), 0, 2);
        root.add(gps, 1, 2);

        root.setAlignment(Pos.TOP_RIGHT);
    }

    static Label createLabel() {
        return createLabel(150, 200);
    }

    static Label createLabel(int minWidth, int prefWidth) {
        Label l = new Label();
        if (minWidth > 0)
            l.setMinWidth(minWidth);
        if (prefWidth > 0)
            l.setPrefWidth(prefWidth);

        return l;
    }

    @FxEventListener
    public void updatePhotoSelected(PhotoSelectedEvent event) {
        root.setVisible(true);
        var mf = event.getMf();
        thumbnailType.setText(EThumbnailType.ABSENT.toString());
        thumbnailSize.setText("");
        size.setText("");
        gps.setText("");
        mediaLoader.getCachedValue(mf).ifPresent(t -> {
            thumbnailType.setText(mf.getThumbnailType().toString()); //map(t -> tn.getOrigin().toString()).orElse(FILE_NOT_FOUND));
            if (t.getImage() != null) {
                thumbnailSize.setText("%d x %d".formatted((int) t.getImage().getWidth(), (int) t.getImage().getHeight()));
            }
        });
        dbId.setText(Long.toString(mf.getId()));
        name.setText(mf.getFileName());
        creationDate.setText(dateTimeFormatter.format(mf.originalDate()));
        gps.setText(mf.getGeoLocation().toDMSString());
        size.setText(Objects.toString(mf.getDimension()));
    }

    @FxEventListener
    public void catalogEvent(CollectionEvent event) {
        log.info("Recieve Collection event: {}", event);
        boolean visible = event.getType() != CollectionEvent.EventType.DELETED;
        root.setVisible(visible);
        txtDbId.setVisible(visible);
    }

    @Override
    public GridPane getRootContent() {
        return root;
    }
}
