package org.icroco.picture.views.organize.details;

import jakarta.annotation.PostConstruct;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.event.PhotoSelectedEvent;
import org.icroco.picture.metadata.DefaultMetadataExtractor;
import org.icroco.picture.metadata.IMetadataExtractor;
import org.icroco.picture.model.ERotation;
import org.icroco.picture.model.EThumbnailType;
import org.icroco.picture.model.Keyword;
import org.icroco.picture.util.Env;
import org.icroco.picture.views.AbstractView;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.ViewConfiguration;
import org.icroco.picture.views.organize.PathSelection;
import org.icroco.picture.views.util.MaskerPane;
import org.icroco.picture.views.util.MediaLoader;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class DetailsView extends AbstractView<VBox> {
    public static final String             FILE_NOT_FOUND         = "File Not Found";
    public static final String             IMAGE_METADATA_DETAILS = "imageMetadataDetails";
    //    private final PersistenceService    persistenceService;
//    private final TaskService           taskService;
//    private final PersistenceService    service;
//    private final UserPreferenceService pref;
    private final       IMetadataExtractor metadataExtractor;

    private final MediaLoader          mediaLoader;
    private final Env                  env;
    private       MaskerPane<GridPane> maskerPane = new MaskerPane<>(true);
    private       VBox                 root       = new VBox();

    private final Label   name          = createLabel();
    private final Label   txtDbId       = new Label("Id: ");
    private final Label   dbId          = createLabel(0, 100);
    private final Label   creationDate  = createLabel();
    private final Label   gps           = createLabel();
    private final Label   size          = createLabel();
    private final Label   thumbnailType = createLabel();
    private final Label   thumbnailSize = createLabel();
    private final Label   orientation   = createLabel();
    private final Label   cameraMake    = createLabel();
    private final Label   cameraModel   = createLabel();
    private final Label   keywords      = createLabel();
    private       TabPane tabs;
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
    private final ObjectProperty<Label> selectedTab = new SimpleObjectProperty<>();
    private       Path                  path        = null;
    private final FontIcon              printImageDetails = FontIcon.of(MaterialDesignI.IMAGE_TEXT);

    @PostConstruct
    private void postConstruct() {
        root.setId(ViewConfiguration.V_MEDIA_DETAILS);
        root.getStyleClass().add(ViewConfiguration.V_DETAILS);
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
        tabs.setFocusTraversable(false);

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
        grid.setPadding(new Insets(10, 10, 10, 10));

        maskerPane.setContent(grid);
        return maskerPane.getRootContent();
    }

    private GridPane createInfo() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.TOP_LEFT);

        // Stsrt filling the grid:
        int rowIdx = 0;

        if (env.isDev()) {
//            txtDbId.getStyleClass().add(Styles.TEXT_SMALL);
//            txtDbId.setVisible(false);mdi2k-key-outline
            grid.add(FontIcon.of(MaterialDesignK.KEY_OUTLINE), 0, rowIdx);
//            dbId.getStyleClass().add(Styles.TEXT_SMALL);

//            grid.add(txtDbId, 2, rowIdx);
            grid.add(dbId, 1, rowIdx);
            rowIdx++;
            grid.add(FontIcon.of(MaterialDesignI.IMAGE_SIZE_SELECT_LARGE), 0, rowIdx);
            grid.add(thumbnailType, 1, rowIdx);
            rowIdx++;
            grid.add(printImageDetails, 0, rowIdx);

            rowIdx += 2;
        }
        grid.add(FontIcon.of(FontAwesomeRegular.FILE), 0, rowIdx);
        grid.add(name, 1, rowIdx);
        rowIdx += 1;

        grid.add(size, 1, rowIdx);
        rowIdx += 2;

        grid.add(FontIcon.of(MaterialDesignC.CALENDAR_BLANK), 0, rowIdx);
        grid.add(creationDate, 1, rowIdx);
        rowIdx += 2;

        grid.add(FontIcon.of(MaterialDesignM.MAP_MARKER_OUTLINE), 0, rowIdx);
        grid.add(gps, 1, rowIdx);
        rowIdx += 2;


        grid.add(FontIcon.of(MaterialDesignC.CAMERA_OUTLINE), 0, rowIdx);
        grid.add(cameraMake, 1, rowIdx);
        rowIdx++;
        grid.add(cameraModel, 1, rowIdx);
        rowIdx += 1;

        if (env.isDev()) {
            grid.add(FontIcon.of(MaterialDesignP.PHONE_ROTATE_LANDSCAPE), 0, rowIdx);
            grid.add(orientation, 1, rowIdx);
            rowIdx += 1;
        }

        grid.add(FontIcon.of(MaterialDesignT.TAG_OUTLINE), 0, rowIdx);
        grid.add(keywords, 1, rowIdx);

        return grid;
    }


    @FxEventListener
    public void updatePhotoSelected(PhotoSelectedEvent event) {
        var mf = event.getMf();
        log.debug("Print details for item: {}", event);
        if (event.getType() == PhotoSelectedEvent.ESelectionType.SELECTED) {
            printImageDetails.setOnMouseClicked(_ -> DefaultMetadataExtractor.printInformation(mf.getFullPath()));

            tabs.getSelectionModel().selectFirst();
            mediaLoader.getCachedValue(mf).ifPresent(t -> {
                thumbnailType.setText(mf.getThumbnailType().toString()); //map(t -> tn.getOrigin().toString()).orElse(FILE_NOT_FOUND));
                if (t.getImage() != null) {
                    thumbnailSize.setText("%d x %d".formatted((int) t.getImage().getWidth(), (int) t.getImage().getHeight()));
                }
            });
            log.info("Header: {}", metadataExtractor.header(mf.getFullPath()));
            dbId.setText(Long.toString(mf.getId()));
            name.setText(mf.getFileName());
            log.info("MIN: {}, current: {}", LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.ofHours(0)), mf.originalDate());
            if (!mf.originalDate().isEqual(LocalDateTime.MIN)) {
                creationDate.setText(dateTimeFormatter.format(mf.originalDate()));
            }
            if (mf.getGeoLocation().isSomewhere()) {
                gps.setText(mf.getGeoLocation().toDMSString());
            }
            size.setText(Objects.toString(mf.getDimension()));
            orientation.setText(Arrays.stream(ERotation.fromOrientation(mf.getOrientation()))
                                      .sorted()
                                      .map(Objects::toString)
                                      .collect(Collectors.joining(",")));
            cameraMake.setText(mf.camera().make());
            cameraModel.setText(mf.camera().model());
            path = mf.getFullPath();
            keywords.setText(mf.getKeywords().stream().map(Keyword::name).collect(Collectors.joining(",")));
            root.setVisible(true);
        } else {
            printImageDetails.setOnMouseClicked(null);
            root.setVisible(false);
            thumbnailType.setText(EThumbnailType.ABSENT.toString());
            thumbnailSize.setText("");
            size.setText("");
            gps.setText("");
            orientation.setText("");
            cameraMake.setText("");
            cameraModel.setText("");
            keywords.setText("");
        }
        maskerPane.stop();
    }

    @Override
    public VBox getRootContent() {
        return root;
    }

    public void collectionPathChange(ObservableValue<? extends PathSelection> observableValue,
                                     PathSelection pathSelection,
                                     PathSelection pathSelection1) {
        updatePhotoSelected(PhotoSelectedEvent.builder().mf(null).type(PhotoSelectedEvent.ESelectionType.UNSELECTED).source(this).build());
    }
}
