package org.icroco.picture.views.organize.details;

import jakarta.annotation.PostConstruct;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.event.PhotoSelectedEvent;
import org.icroco.picture.metadata.IMetadataExtractor;
import org.icroco.picture.model.EThumbnailType;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.util.FxView;
import org.icroco.picture.views.util.MediaLoader;
import org.icroco.picture.views.util.Styles;
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
public class DetailsView implements FxView<VBox> {
    public static final String FILE_NOT_FOUND         = "File Not Found";
    public static final String IMAGE_METADATA_DETAILS = "imageMetadataDetails";
    //    private final PersistenceService    persistenceService;
//    private final TaskService           taskService;
//    private final PersistenceService    service;
//    private final UserPreferenceService pref;
//    private final IMetadataExtractor    metadataExtractor;

    private final MediaLoader mediaLoader;

    VBox root = new VBox();

    private final Label   name          = createLabel();
    private final Label   txtDbId       = new Label("Id: ");
    private final Label   dbId          = createLabel(0, 30);
    private final Label   creationDate  = createLabel();
    private final Label   gps           = createLabel();
    private final Label   size          = createLabel();
    private final Label   thumbnailType = createLabel();
    private final Label   thumbnailSize = createLabel();
    private final Label   orientation   = createLabel();
    private       TabPane tabs;
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
    private final ObjectProperty<Label> selectedTab = new SimpleObjectProperty<>();

    @PostConstruct
    private void postConstruct() {
        root.getStyleClass().add("v-details");
        root.setVisible(false);
        root.setMinWidth(200);
//        var infoTab = createTabLabel("Info", createInfo());
//        var detailsTab = createTabLabel("Details");
        Tab info = new Tab("Info", createInfo());
        info.setId("imageMetadataInfo");
        Tab details = new Tab("Details", createFullDetails());
        details.setId(IMAGE_METADATA_DETAILS);
        tabs = new TabPane(info, details);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add(atlantafx.base.theme.Styles.DENSE);
        tabs.getSelectionModel().selectFirst();
        tabs.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> selectTab(newValue));

        root.getChildren().add(tabs);

    }

    private void selectTab(Tab newValue) {
        if (newValue.getId().equals(IMAGE_METADATA_DETAILS)) {
            log.info("TODO: Tab selected: {}", newValue);
        }
    }

    private Node createFullDetails() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.TOP_RIGHT);

        return grid;
    }

    private GridPane createInfo() {

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(0, 10, 0, 10));
        grid.add(FontIcon.of(FontAwesomeRegular.FILE), 0, 0);
        grid.add(name, 1, 0);
        txtDbId.getStyleClass().add(Styles.TEXT_SMALL);
        txtDbId.setVisible(false);

        int rowIdx = 0;
        grid.add(txtDbId, 2, rowIdx);
        grid.add(dbId, 3, rowIdx);
        grid.setHgap(10);
        grid.setVgap(10);
        dbId.getStyleClass().add(Styles.TEXT_SMALL);

        rowIdx += 1;
        grid.add(size, 1, rowIdx);

        rowIdx += 2;
        grid.add(FontIcon.of(FontAwesomeRegular.CALENDAR), 0, rowIdx);
        grid.add(creationDate, 1, rowIdx);

        rowIdx += 2;
        grid.add(FontIcon.of(FontAwesomeSolid.LOCATION_ARROW), 0, rowIdx);
        grid.add(gps, 1, rowIdx);

        rowIdx += 2;
        grid.add(FontIcon.of(FontAwesomeRegular.THUMBS_UP), 0, rowIdx);
        grid.add(orientation, 1, rowIdx);

        grid.setAlignment(Pos.TOP_RIGHT);

        return grid;
    }

    static Label createLabel() {
        return createLabel(150, 200);
    }

    static Label createLabel(int minWidth, int prefWidth) {
        Label l = new Label();
        if (minWidth > 0) {
            l.setMinWidth(minWidth);
        }
        if (prefWidth > 0) {
            l.setPrefWidth(prefWidth);
        }

        return l;
    }

    @FxEventListener
    public void updatePhotoSelected(PhotoSelectedEvent event) {
        var mf = event.getMf();
        if (event.getType() == PhotoSelectedEvent.ESelectionType.SELECTED) {
            tabs.getSelectionModel().selectFirst();
            mediaLoader.getCachedValue(mf).ifPresent(t -> {
                thumbnailType.setText(mf.getThumbnailType().toString()); //map(t -> tn.getOrigin().toString()).orElse(FILE_NOT_FOUND));
                if (t.getImage() != null) {
                    thumbnailSize.setText("%d x %d".formatted((int) t.getImage().getWidth(), (int) t.getImage().getHeight()));
                }
            });
            dbId.setText(Long.toString(mf.getId()));
            name.setText(mf.getFileName());
            creationDate.setText(dateTimeFormatter.format(mf.originalDate()));
            if (mf.getGeoLocation() != IMetadataExtractor.NO_WHERE) {
                gps.setText(mf.getGeoLocation().toDMSString());
            }
            size.setText(Objects.toString(mf.getDimension()));
            root.setVisible(true);
        } else {
            root.setVisible(false);
            thumbnailType.setText(EThumbnailType.ABSENT.toString());
            thumbnailSize.setText("");
            size.setText("");
            gps.setText("");
            orientation.setText("");
        }
    }

    @Override
    public VBox getRootContent() {
        return root;
    }
}
