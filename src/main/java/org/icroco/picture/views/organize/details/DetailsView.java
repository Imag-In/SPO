package org.icroco.picture.views.organize.details;

import atlantafx.base.controls.Calendar;
import atlantafx.base.controls.Popover;
import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import com.dlsc.gemsfx.TagsField;
import jakarta.annotation.PostConstruct;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.icroco.picture.event.ForceGenerateThumbnailEvent;
import org.icroco.picture.event.NotificationEvent;
import org.icroco.picture.event.PhotoSelectedEvent;
import org.icroco.picture.metadata.DefaultMetadataExtractor;
import org.icroco.picture.metadata.IKeywordManager;
import org.icroco.picture.metadata.IMetadataExtractor;
import org.icroco.picture.metadata.IMetadataWriter;
import org.icroco.picture.model.ERotation;
import org.icroco.picture.model.EThumbnailType;
import org.icroco.picture.model.Keyword;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.model.converter.KeywordStringConverter;
import org.icroco.picture.persistence.PersistenceService;
import org.icroco.picture.persistence.ThumbnailRepository;
import org.icroco.picture.util.Env;
import org.icroco.picture.util.FileUtil;
import org.icroco.picture.util.LocalDateStringConverter;
import org.icroco.picture.views.AbstractView;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.ViewConfiguration;
import org.icroco.picture.views.organize.OrganizeConfiguration;
import org.icroco.picture.views.organize.PathSelection;
import org.icroco.picture.views.task.ModernTask;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.I18N;
import org.icroco.picture.views.util.MaskerPane;
import org.icroco.picture.views.util.MediaLoader;
import org.icroco.picture.views.util.Nodes;
import org.icroco.picture.views.util.widget.FxUtil;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedAL;
import org.kordamp.ikonli.materialdesign2.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class DetailsView extends AbstractView<VBox> {
    private final       ThumbnailRepository thumbnailRepository;
    public static final String              FILE_NOT_FOUND         = "File Not Found";
    public static final String              IMAGE_METADATA_DETAILS = "imageMetadataDetails";

    private final TaskService           taskService;
    private final IMetadataExtractor    metadataExtractor;
    private final IMetadataWriter       metadataWriter;
    private final MediaLoader           mediaLoader;
    private final Env                   env;
    private final PersistenceService    persistenceService;
    private final IKeywordManager      keywordManager;
    private final I18N                 i18N;
    @Qualifier(OrganizeConfiguration.ORGANIZE_EDIT_MODE)
    private final SimpleBooleanProperty editMode;
    private       MaskerPane<GridPane> maskerPane          = new MaskerPane<>(true);
    private final VBox                 root                = new VBox();
    private final Label                name                = createLabel();
    private final Label                dbId                = createLabel(0, 100);
    private final Label                creationDate        = createLabel();
    private final Label                creationTime        = createLabel();
    private final DatePicker           originalDate        = new DatePicker();
    private final Label                gps                 = createLabel();
    private final Label                size                = createLabel();
    private final Label                thumbnailType       = createLabel();
    private final Label                thumbnailSize       = createLabel();
    private final Label                orientation         = createLabel();
    private final Label                cameraMake          = createLabel();
    private final Label                cameraModel         = createLabel();
    //    private final Label                 keywords            = createLabel();
    private final TagsField<Keyword>   keywords            = new TagsField<>();
    private final Label                filePathError       = createLabel();
    private       TabPane               tabs;
    private       Path                 path                = null;
    private final Button               printImageDetails   = new Button(null, FontIcon.of(MaterialDesignC.CONSOLE_LINE));
    private final Button               refreshThumbnail    = new Button(null, FontIcon.of(MaterialDesignR.RESTART));
    private final Button               saveThumbnail       = new Button(null, FontIcon.of(MaterialDesignC.CONTENT_SAVE_OUTLINE));
    private final Button               editDate            = new Button(null, FontIcon.of(Material2OutlinedAL.CREATE));
    private final Button               editTime            = new Button(null, FontIcon.of(Material2OutlinedAL.CREATE));
    private final Button               extractDateFromFile = new Button(null, FontIcon.of(MaterialDesignC.CALENDAR_CHECK_OUTLINE));
    private final FontIcon             filePathErrorIcon   = FontIcon.of(MaterialDesignL.LINK_OFF);

    private final ChangeListener<LocalDateTime> reloadNeeded = this::reload;
    private       MediaFile                     mediaFile    = null;

    @PostConstruct
    private void postConstruct() {
        root.setId(ViewConfiguration.V_MEDIA_DETAILS);
        root.getStyleClass().add(ViewConfiguration.V_DETAILS);
        root.setVisible(false);
        root.setPrefWidth(330);

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

        originalDate.setConverter(new LocalDateStringConverter());
        originalDate.setEditable(false);

        filePathErrorIcon.setVisible(false);
        filePathErrorIcon.getStyleClass().add(Styles.DANGER);

        extractDateFromFile.setTooltip(new Tooltip("Extract date/time from filename"));

        setBindings();
    }

    private void selectTab(Tab newValue) {
        if (newValue.getId().equals(IMAGE_METADATA_DETAILS)) {
            maskerPane.start();
            var      directories = metadataExtractor.getAllByDirectory(path);
            GridPane gp          = maskerPane.getContent();
            gp.getChildren().clear();
            int rowIdx = 0;

            for (var dir : directories) {
                var dirLbl = createLabel(dir.simpleName(), 50, 300);
                dirLbl.getStyleClass().add(Styles.TITLE_4);
                dirLbl.setPadding(new Insets(0, 0, 5, 0));
                gp.add(dirLbl, 0, rowIdx, 2, 1);
                rowIdx += 2;
                for (var d : dir.entries().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                    Label label = createLabel(d.getValue().description(), 50, 150);
                    label.setTooltip(new Tooltip(STR."id: \{d.getKey().toString()}"));
                    label.getStyleClass().add(Styles.SMALL);
                    gp.add(label, 0, rowIdx);
                    gp.add(createTextField(Objects.toString(d.getValue().value()), 100, 150), 1, rowIdx);
                    rowIdx++;
                }
                gp.add(new Separator(Orientation.HORIZONTAL), 0, rowIdx, 2, 1);
                rowIdx += 1;
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
        grid.add(extractDateFromFile, 2, rowIdx);
        rowIdx += 1;

        grid.add(size, 1, rowIdx);
        rowIdx += 2;

        grid.add(FontIcon.of(MaterialDesignC.CALENDAR_BLANK), 0, rowIdx);
        grid.add(creationDate, 1, rowIdx);
        grid.add(editDate, 2, rowIdx);
        rowIdx += 1;
        grid.add(creationTime, 1, rowIdx);
        grid.add(editTime, 2, rowIdx);

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

        // Last one is indicator to alert if file path is not found.
        grid.add(filePathErrorIcon, 0, rowIdx);
        grid.add(filePathError, 1, rowIdx);

        rowIdx += 1;


        // Thumbnail section
        grid.add(new Label("Thumbnail"), 0, rowIdx, 2, 1);
        rowIdx += 1;
        grid.add(FontIcon.of(MaterialDesignI.IMAGE_SIZE_SELECT_LARGE), 0, rowIdx);
        grid.add(thumbnailType, 1, rowIdx);

        FxUtil.styleCircleButton(extractDateFromFile);
        FxUtil.styleCircleButton(refreshThumbnail);
        FxUtil.styleCircleButton(printImageDetails);
        FxUtil.styleCircleButton(saveThumbnail);
        FxUtil.styleCircleButton(editDate);
        FxUtil.styleCircleButton(editTime);
        refreshThumbnail.setTooltip(new Tooltip("Re-generate thumbnail from image")); // I18N:
        saveThumbnail.setTooltip(new Tooltip("Save thumbnail into image")); // I18N:
        saveThumbnail.setDisable(true);
        printImageDetails.setTooltip(new Tooltip("Print metadata (log file)")); // I18N:
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

    void setBindings() {
        var notEditing = Bindings.not(editMode);

        extractDateFromFile.setDisable(true);
        extractDateFromFile.disableProperty().bind(notEditing);
        extractDateFromFile.setOnAction(_ -> dateExtractedAction());

        editDate.disableProperty().bind(notEditing);
        editDate.setOnAction(_ -> dateEditedAction());

//        editTime.disableProperty().bind(notEditing);
        editTime.setDisable(true);
        editTime.setTooltip(new Tooltip("Not Yet Implemented")); // TODO:

        keywords.setConverter(new KeywordStringConverter());
        keywords.getEditor().disableProperty().bind(notEditing);
        keywords.getEditor().visibleProperty().bind(editMode);
        keywords.getEditor().managedProperty().bind(editMode);
        keywords.setMatcher((kw, searchText) -> kw.name().toLowerCase().startsWith(searchText.toLowerCase()));
        keywords.setComparator(Comparator.comparing(Keyword::name));
        keywords.getEditor().setPromptText("Start typing keywors ..."); // I18N:
        keywords.setComparator(Comparator.comparing(Keyword::name));
        keywords.setSuggestionProvider(request -> keywordManager.getAll().stream()
                                                                .filter(kw -> kw.name().toLowerCase()
                                                                                .contains(request.getUserText().toLowerCase()))
                                                                .collect(Collectors.toList()));
        keywords.setNewItemProducer(name -> new Keyword(null, name));
        keywords.getEditor().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                Nodes.applyAndConsume(event, _ -> DetailsView.this.root.requestFocus());
            }
        });

        editMode.addListener((_, _, newValue) -> { // Hack.
            if (!newValue && mediaFile != null) {
                if (!CollectionUtils.containsAll(mediaFile.getKeywords(), keywords.getTags())) {
                    var tags = keywordManager.addMissingKw(keywords.getTags());
                    mediaFile.setKeywords(tags);
                    persistenceService.saveMediaFile(mediaFile, mf -> metadataWriter.addKeywords(mf.fullPath(), mf.getKeywords()));
                }
            }
        });
    }

    private void dateExtractedAction() {
        FileUtil.extractDateTime(mediaFile.getFullPath())
                .ifPresentOrElse(dt -> {
                    var pop = new Popover();

                    var vbox = new VBox();
                    vbox.setSpacing(5);
                    var hbDate = new HBox();
                    hbDate.getChildren().addAll(new Label("Date "), new TextField(dt.toLocalDate().toString()));
                    hbDate.setAlignment(Pos.CENTER);

                    var hbTime = new HBox();
                    hbTime.getChildren().addAll(new Label("Time "), new TextField(dt.toLocalTime().toString()));
                    hbTime.setAlignment(Pos.CENTER);

                    var hbApply = new HBox();
                    var save    = new Button(null, FontIcon.of(MaterialDesignC.CONTENT_SAVE_OUTLINE));
                    FxUtil.styleCircleButton(save);
                    save.setCursor(Cursor.HAND);
                    save.setOnAction(_ -> {
                        updateDate(mediaFile, dt);
                        pop.hide();
                    });
//                    hbApply.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                    hbApply.setAlignment(Pos.CENTER_RIGHT);
                    hbApply.getChildren().addAll(save);

                    vbox.getChildren().addAll(hbDate, hbTime, hbApply);
                    pop.setContentNode(vbox);
                    pop.setAnimated(true);
                    pop.setCloseButtonEnabled(true);
                    pop.setArrowLocation(Popover.ArrowLocation.TOP_RIGHT);
                    pop.show(extractDateFromFile);
                }, () -> taskService.sendEvent(NotificationEvent.builder()
                                                                .type(NotificationEvent.NotificationType.ERROR)
                                                                .message("Cannot extract Date/Time from filename: '%s'".formatted(mediaFile.getFullPath()
                                                                                                                                           .getFileName()))
                                                                .source(this)
                                                                .build()));
    }

    private void dateEditedAction() {
        var calendar = new Calendar();
        calendar.setValue(mediaFile == null ? LocalDate.now() : mediaFile.getOriginalDate().toLocalDate());
//            var calendar = new CalendarView();
//            calendar.setShowTodayButton(true);
//            calendar.setMonthSelectionViewEnabled(true);
//            calendar.setYearSelectionViewEnabled(true);
//            calendar.setShowMonthDropdown(true);
//            calendar.setShowYearDropdown(true);
//            calendar.setShowToday(true);
//            calendar.getSelectionModel().setSelectedDate(mediaFile == null ? LocalDate.now() : mediaFile.getOriginalDate().toLocalDate());
        var pop = new Popover(calendar);
        pop.setAnimated(true);
        pop.setArrowLocation(Popover.ArrowLocation.TOP_RIGHT);

        calendar.setOnMouseClicked(_ -> {
            pop.hide();
            if (mediaFile != null) {
                LocalDateTime newDateTime = LocalDateTime.of(calendar.getValue(), mediaFile.getOriginalDate().toLocalTime());
                updateDate(mediaFile, newDateTime);
            }
        });
        pop.show(editDate);
    }

    private void updateDate(MediaFile file, LocalDateTime dateTime) {
        file.setOriginalDate(dateTime);
        persistenceService.saveMediaFile(file, mf -> metadataWriter.setOrignialDate(mf.getFullPath(), dateTime));
    }

    @FxEventListener
    public void updatePhotoSelected(PhotoSelectedEvent event) {
        clearForm();
        mediaFile = event.getMf();
        if (mediaFile == null) {
            return;
        }
        log.debug("Print details for item: {}", event);
        if (event.getType() == PhotoSelectedEvent.ESelectionType.SELECTED) {
            mediaFile.getLastUpdated().addListener(reloadNeeded);
            printImageDetails.setOnMouseClicked(_ -> DefaultMetadataExtractor.printInformation(mediaFile.getFullPath()));
            saveThumbnail.setTooltip(new Tooltip("Not Yet Implemented")); // I18N:
            saveThumbnail.setOnMouseClicked(_ -> {
                saveThumbnail.setDisable(true);
                var task = ModernTask.builder()
                                     .execute(self -> {
                                         Optional.ofNullable(mediaFile)
                                                 .map(MediaFile::getId)
                                                 .flatMap(thumbnailRepository::findImageByMfId)// TODO: Look at cache level.
                                                 .map(ThumbnailRepository.IdAndBytes::getImage)
                                                 .ifPresent(bytes -> metadataWriter.setThumbnail(mediaFile.fullPath(), bytes));
                                         return null;
                                     })
                                     .onFinished(() -> saveThumbnail.setDisable(false))
                                     .build();
                taskService.supply(task);
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
//        keywords.setText("");
        keywords.clear();
        keywords.clearTags();
        thumbnailSize.setText("");
        creationTime.textProperty().unbind();
        creationTime.setText("");
        creationDate.textProperty().unbind();
        creationDate.setText("");
        mediaFile = null;
        filePathErrorIcon.setVisible(false);
        filePathError.setText("");
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
//        log.info("MIN: {}, current: {}", LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.ofHours(0)), mediaFile.originalDate());
//        if (!mediaFile.originalDate().isEqual(LocalDateTime.MIN)) {
//            creationDate.setText(dateTimeFormatter.format(mediaFile.originalDate()));
//            originalDate.setValue(mediaFile.originalDate().toLocalDate());
        creationDate.textProperty().bind(mediaFile.getOriginalDateProperty().map(DateTimeFormatter.ISO_DATE::format));
        creationTime.textProperty().bind(mediaFile.getOriginalDateProperty().map(DateTimeFormatter.ISO_TIME::format));
//        }
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
        keywords.addTags(mediaFile.getKeywords().toArray(new Keyword[0]));
//        keywords.setText(mediaFile.getKeywords().stream().map(Keyword::name).collect(Collectors.joining(",")));
        if (!Files.exists(mediaFile.fullPath())) {
            filePathErrorIcon.setVisible(true);
            filePathError.setText("File path doesn't exist !");
        }
    }

    private void reload(ObservableValue<? extends LocalDateTime> observable, LocalDateTime oldValue, LocalDateTime newValue) {
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
