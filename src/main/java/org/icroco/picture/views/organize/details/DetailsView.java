package org.icroco.picture.views.organize.details;

import atlantafx.base.controls.Spacer;
import com.ashampoo.kim.Kim;
import jakarta.annotation.PostConstruct;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.JpegImageData;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.icroco.picture.event.ForceGenerateThumbnailEvent;
import org.icroco.picture.event.PhotoSelectedEvent;
import org.icroco.picture.metadata.DefaultMetadataExtractor;
import org.icroco.picture.metadata.IMetadataExtractor;
import org.icroco.picture.model.ERotation;
import org.icroco.picture.model.EThumbnailType;
import org.icroco.picture.model.Keyword;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.persistence.ThumbnailRepository;
import org.icroco.picture.util.Env;
import org.icroco.picture.views.AbstractView;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.ViewConfiguration;
import org.icroco.picture.views.organize.PathSelection;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.MaskerPane;
import org.icroco.picture.views.util.MediaLoader;
import org.icroco.picture.views.util.widget.FxUtil;
import org.jooq.lambda.Unchecked;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
    private final       ThumbnailRepository thumbnailRepository;
    public static final String              FILE_NOT_FOUND         = "File Not Found";
    public static final String              IMAGE_METADATA_DETAILS = "imageMetadataDetails";

    private final TaskService        taskService;
    private final IMetadataExtractor metadataExtractor;
    private final MediaLoader        mediaLoader;
    private final Env                env;

    private       MaskerPane<GridPane> maskerPane    = new MaskerPane<>(true);
    private final VBox                 root          = new VBox();
    private final Label                name          = createLabel();
    private final Label                txtDbId       = new Label("Id: ");
    private final Label                dbId          = createLabel(0, 100);
    private final Label                creationDate  = createLabel();
    private final Label                gps           = createLabel();
    private final Label                size          = createLabel();
    private final Label                thumbnailType = createLabel();
    private final Label                thumbnailSize = createLabel();
    private final Label                orientation   = createLabel();
    private final Label                cameraMake    = createLabel();
    private final Label                cameraModel   = createLabel();
    private final Label                keywords      = createLabel();
    private       TabPane              tabs;
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
    private final ObjectProperty<Label> selectedTab       = new SimpleObjectProperty<>();
    private       Path                  path              = null;
    private final Button                printImageDetails = new Button(null, FontIcon.of(MaterialDesignC.CONSOLE_LINE));
    private final Button                refreshThumbnail  = new Button(null, FontIcon.of(MaterialDesignR.REFRESH));
    private final Button                saveThumbnail     = new Button(null, FontIcon.of(MaterialDesignC.CONTENT_SAVE_OUTLINE));

    private final ChangeListener<LocalDateTime> reloadNeeded = this::reload;
    private       MediaFile                     mediaFile    = null;

    @PostConstruct
    private void postConstruct() {
        root.setId(ViewConfiguration.V_MEDIA_DETAILS);
        root.getStyleClass().add(ViewConfiguration.V_DETAILS);
        root.setVisible(false);
        root.setPrefWidth(330);

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
        rowIdx += 1;
        grid.add(new Separator(Orientation.HORIZONTAL), 0, rowIdx, 2, 1);
        rowIdx += 1;
        grid.add(new Label("Thumbnail"), 0, rowIdx);
        rowIdx += 1;
        grid.add(FontIcon.of(MaterialDesignI.IMAGE_SIZE_SELECT_LARGE), 0, rowIdx);
        grid.add(thumbnailType, 1, rowIdx);
        FxUtil.styleCircleButton(refreshThumbnail);
        FxUtil.styleCircleButton(printImageDetails);
        FxUtil.styleCircleButton(saveThumbnail);
        refreshThumbnail.setTooltip(new Tooltip("Re-generate thumbnail from image"));
        saveThumbnail.setTooltip(new Tooltip("Save thumbnail into image"));
        saveThumbnail.setDisable(true);
        printImageDetails.setTooltip(new Tooltip("Print metadata (log file)"));
        rowIdx += 1;
        grid.add(thumbnailSize, 1, rowIdx);
        rowIdx++;

        HBox hb = new HBox(refreshThumbnail, saveThumbnail, new Spacer());
        if (env.isDev()) {
            hb.getChildren().add(printImageDetails);
        }
        grid.add(hb, 1, rowIdx);

        return grid;
    }


    @FxEventListener
    public void updatePhotoSelected(PhotoSelectedEvent event) {
        clearForm();
        mediaFile = event.getMf();
        log.debug("Print details for item: {}", event);
        if (event.getType() == PhotoSelectedEvent.ESelectionType.SELECTED) {
            mediaFile.getLastUpdated().addListener(reloadNeeded);
            printImageDetails.setOnMouseClicked(_ -> DefaultMetadataExtractor.printInformation(mediaFile.getFullPath()));
            saveThumbnail.setOnMouseClicked(_ -> {

                thumbnailRepository.findImageByMfId(mediaFile.getId())
                                   .map(ThumbnailRepository.IdAndBytes::getImage)
                                   .ifPresent(bytes -> {
                                       Unchecked.runnable(() -> {
                                           log.info("Thumbnail old size: {}",
                                                    Kim.readMetadata(Files.readAllBytes(mediaFile.getFullPath()))
                                                       .getExifThumbnailBytes().length);
                                           final ImageMetadata metadata = Imaging.getMetadata(mediaFile.getFullPath().toFile());
                                           if (metadata instanceof JpegImageMetadata jpegMetadata) {
                                               TiffOutputSet outputSet = jpegMetadata.getExif().getOutputSet();
                                               outputSet.getExifDirectory().setJpegImageData(new JpegImageData(0, bytes.length, bytes));
                                               new ExifRewriter().updateExifMetadataLossy(Files.readAllBytes(mediaFile.getFullPath()),
                                                                                          Files.newOutputStream(mediaFile.getFullPath(),
                                                                                                                StandardOpenOption.WRITE,
                                                                                                                StandardOpenOption.TRUNCATE_EXISTING),
                                                                                          outputSet);
                                               log.info("Thumbnail new size: {}",
                                                        Kim.readMetadata(Files.readAllBytes(mediaFile.getFullPath()))
                                                           .getExifThumbnailBytes().length);

                                           }
                                       }).run();
                                   });
//                thumbnailRepository.findImageByMfId(mediaFile.getId())
//                                   .map(ThumbnailRepository.IdAndBytes::getImage)
//                                   .map(bytes -> {
//                                       if (bytes.length >= JpegConstants.MAX_SEGMENT_SIZE) {
//                                           log.warn("Thumbnail too large: {}, max is: {}", bytes.length, JpegConstants.MAX_SEGMENT_SIZE);
//                                       }
//                                       return bytes;
//                                   })
//                                   .ifPresent(bytes -> {
//                                       Unchecked.runnable(() -> Kim.updateThumbnail(Files.readAllBytes(mediaFile.getFullPath()),
//                                                                                    bytes)).run();
//                                   });

            });
            refreshThumbnail.setOnMouseClicked(_ -> taskService.sendEvent(ForceGenerateThumbnailEvent.builder()
                                                                                                     .mediaFile(mediaFile)
                                                                                                     .source(this)
                                                                                                     .build()));
            tabs.getSelectionModel().selectFirst();
            fillForm();
            root.setVisible(true);
        }
        maskerPane.stop();
    }

    private void clearForm() {
        if (mediaFile != null) {
            mediaFile.getLastUpdated().removeListener(reloadNeeded);
        }
        printImageDetails.setOnMouseClicked(null);
        refreshThumbnail.setOnMouseClicked(null);
        saveThumbnail.setOnMouseClicked(null);
        root.setVisible(false);
        thumbnailType.setText(EThumbnailType.ABSENT.toString());
        thumbnailSize.setText("");
        size.setText("");
        gps.setText("");
        orientation.setText("");
        cameraMake.setText("");
        cameraModel.setText("");
        keywords.setText("");
        thumbnailSize.setText("");
        mediaFile = null;
    }

    private void fillForm() {
        mediaLoader.getCachedValue(mediaFile).ifPresent(t -> {
            thumbnailType.setText(mediaFile.getThumbnailType()
                                           .toString()); //map(t -> tn.getOrigin().toString()).orElse(FILE_NOT_FOUND));
            thumbnailSize.setText(Objects.toString(t.getDimension()));
        });
//        log.info("Header: {}", metadataExtractor.header(mediaFile.getFullPath()));
        dbId.setText(Long.toString(mediaFile.getId()));
        name.setText(mediaFile.getFileName());
        log.info("MIN: {}, current: {}", LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.ofHours(0)), mediaFile.originalDate());
        if (!mediaFile.originalDate().isEqual(LocalDateTime.MIN)) {
            creationDate.setText(dateTimeFormatter.format(mediaFile.originalDate()));
        }
        if (mediaFile.getGeoLocation().isSomewhere()) {
            gps.setText(mediaFile.getGeoLocation().toDMSString());
        }
        size.setText(Objects.toString(mediaFile.getDimension()));
        orientation.setText(Arrays.stream(ERotation.fromOrientation(mediaFile.getOrientation()))
                                  .sorted()
                                  .map(Objects::toString)
                                  .collect(Collectors.joining(",")));
        cameraMake.setText(mediaFile.camera().make());
        cameraModel.setText(mediaFile.camera().model());
        path = mediaFile.getFullPath();
        keywords.setText(mediaFile.getKeywords().stream().map(Keyword::name).collect(Collectors.joining(",")));
    }

    private void reload(ObservableValue<? extends LocalDateTime> observable, LocalDateTime oldValue, LocalDateTime newValue) {
        log.info("Reload needed");
        fillForm();
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
