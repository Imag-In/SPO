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
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.event.PhotoSelectedEvent;
import org.icroco.picture.metadata.IMetadataExtractor;
import org.icroco.picture.model.EThumbnailType;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.util.FxView;
import org.icroco.picture.views.util.MaskerPane;
import org.icroco.picture.views.util.MediaLoader;
import org.icroco.picture.views.util.Styles;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Component
public class DetailsView implements FxView<VBox> {
    public static final String             FILE_NOT_FOUND         = "File Not Found";
    public static final String             IMAGE_METADATA_DETAILS = "imageMetadataDetails";
    //    private final PersistenceService    persistenceService;
//    private final TaskService           taskService;
//    private final PersistenceService    service;
//    private final UserPreferenceService pref;
    private final       IMetadataExtractor metadataExtractor;

    private final MediaLoader mediaLoader;
    private       MaskerPane<GridPane> maskerPane = new MaskerPane<>(true);
    private       VBox        root       = new VBox();

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
    private       Path                  path        = null;

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
        root.setFocusTraversable(false);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add(atlantafx.base.theme.Styles.DENSE);
        tabs.getSelectionModel().selectFirst();
        tabs.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> selectTab(newValue));

        VBox.setVgrow(tabs, Priority.ALWAYS);
        root.getChildren().add(tabs);
        root.setFocusTraversable(false);
    }

    private void selectTab(Tab newValue) {
        if (newValue.getId().equals(IMAGE_METADATA_DETAILS)) {
            maskerPane.start();
            var      data = metadataExtractor.getAllInformation(path);
            GridPane gp   = maskerPane.getContent();
            gp.getChildren().clear();
            int rowIdx = 0;

            for (var d : data.entrySet().stream().sorted((o1, o2) -> o1.getKey().compareToIgnoreCase(o2.getKey())).toList()) {
                gp.add(createLabel(d.getKey(), 50, 150), 0, rowIdx);
                gp.add(createTextField(Objects.toString(d.getValue()), 100, 150), 1, rowIdx);
                rowIdx++;
            }
            maskerPane.stop();
        }
    }

    private Node createFullDetails() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.TOP_LEFT);
        grid.setPadding(new Insets(0, 10, 0, 10));

        maskerPane.setContent(grid);
        return maskerPane.getRootContent();
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

    static Label createLabel(String text, int minWidth, int prefWidth) {
        Label l = new Label(text);
        if (minWidth > 0) {
            l.setMinWidth(minWidth);
        }
        if (prefWidth > 0) {
            l.setPrefWidth(prefWidth);
        }

        return l;
    }

    static TextField createTextField(String text, int minWidth, int prefWidth) {
        TextField l = new TextField(text);
        l.setEditable(false);
        if (minWidth > 0) {
            l.setMinWidth(minWidth);
        }
        if (prefWidth > 0) {
            l.setPrefWidth(prefWidth);
        }

        return l;
    }

    static Label createLabel(int minWidth, int prefWidth) {
        return createLabel(null, minWidth, prefWidth);
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
            orientation.setText(String.valueOf(mf.getOrientation()));
            path = mf.getFullPath();
        } else {
            root.setVisible(false);
            thumbnailType.setText(EThumbnailType.ABSENT.toString());
            thumbnailSize.setText("");
            size.setText("");
            gps.setText("");
            orientation.setText("");
            maskerPane.getContent().getChildren().clear();
        }
    }

    @Override
    public VBox getRootContent() {
        return root;
    }
}
