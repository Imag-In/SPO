package org.icroco.picture.views.ext_import;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.layout.InputGroup;
import atlantafx.base.theme.Styles;
import com.dlsc.gemsfx.TagsField;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.icroco.picture.event.ImportDirectoryEvent;
import org.icroco.picture.event.NotificationEvent;
import org.icroco.picture.event.ShowOrganizeEvent;
import org.icroco.picture.event.UsbStorageDeviceEvent;
import org.icroco.picture.metadata.IKeywordManager;
import org.icroco.picture.metadata.IMetadataWriter;
import org.icroco.picture.model.Keyword;
import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.model.converter.KeywordStringConverter;
import org.icroco.picture.persistence.PersistenceService;
import org.icroco.picture.util.LangUtils;
import org.icroco.picture.views.AbstractView;
import org.icroco.picture.views.CollectionManager;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.ViewConfiguration;
import org.icroco.picture.views.task.FxRunAllScope;
import org.icroco.picture.views.task.IFxCallable;
import org.icroco.picture.views.task.ModernTask;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.DirectoryWatcher;
import org.icroco.picture.views.util.I18N;
import org.icroco.picture.views.util.Nodes;
import org.jooq.lambda.Unchecked;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static javafx.beans.binding.Bindings.or;

@Slf4j
@Component
public class ImportView extends AbstractView<StackPane> {
    public static final int              SECOND_COL_PREF_WIDTH = 600;
    private final       TaskService        taskService;
    private final       PersistenceService persistenceService;
    private final       CollectionManager  collectionManager;
    private final       DirectoryWatcher directoryWatcher;
    private final       IKeywordManager  keywordManager;
    private final       IMetadataWriter  metadataWriter;
    private final       I18N             i18N;

    private final StackPane          root           = new StackPane();
    private final CustomTextField    sourceDir      = new CustomTextField();
    private       TextField          targetCollectionTf;
    private       TextField          targetSubDirTf;
    private       TagsField<Keyword> keywords;
    private final TextArea           exampleTf      = new TextArea();
    private final CustomTextField    filePrefix     = new CustomTextField("");
    private final Label              filesCounter   = new Label("");
    private final Button             importBtn      = new Button("Import");
    private final ToggleSwitch       genThumbailsCb = new ToggleSwitch("Generate high quality thumbnails");
    private final ToggleSwitch       deleteFilesCb  = new ToggleSwitch("Delete imported files");

    private final MediaFile[] fakeMf = new MediaFile[] { MediaFile.builder()
                                                                  .originalDate(LocalDateTime.now())
                                                                  .fullPath(Path.of("example.png"))
                                                                 .build(),
                                                         MediaFile.builder()
                                                                  .originalDate(LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 0, 0))
                                                                                             .plusDays(1))
                                                                  .fullPath(Path.of("luna.jpg"))
                                                                 .build(),
                                                         MediaFile.builder()
                                                                  .originalDate(LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 0, 0))
                                                                                             .plusDays(1))
                                                                  .fullPath(Path.of("luna.jpg"))
                                                                 .build()
    };

    private final SimpleBooleanProperty            isRunning          = new SimpleBooleanProperty(false);
    private final SimpleObjectProperty<Path>       sourcePath         = new SimpleObjectProperty<>(null);
    private final ComboBox<IRenameFilesStrategy>   renamingStrategyCb = new ComboBox<>();
    private       Path                             targetCollection;
    private final SortedList<IRenameFilesStrategy> strategies;
    private final List<Keyword> allKeywords = new ArrayList<>();

    public ImportView(TaskService taskService,
                      PersistenceService persistenceService,
                      CollectionManager collectionManager,
                      DirectoryWatcher directoryWatcher,
                      IKeywordManager keywordManager,
                      IMetadataWriter metadataWriter, I18N i18N,
                      Collection<IRenameFilesStrategy> strategies) {
        this.taskService = taskService;
        this.persistenceService = persistenceService;
        this.collectionManager = collectionManager;
        this.directoryWatcher = directoryWatcher;
        this.keywordManager = keywordManager;
        this.metadataWriter = metadataWriter;
        this.i18N = i18N;
        this.strategies = new SortedList<>(FXCollections.observableArrayList(strategies),
                                           Comparator.comparing(IRenameFilesStrategy::displayName));
    }

    @PostConstruct
    private void postConstruct() {
        root.setId(ViewConfiguration.V_IMPORT);
        root.getStyleClass().add(ViewConfiguration.V_IMPORT);
        root.getChildren().add(createForm());

        sourcePath.addListener((observable, oldValue, newValue) -> newSourcePath(newValue));
        sourceDir.textProperty().bind(sourcePath.map(Path::toString));
        importBtn.disableProperty().bind(or(isRunning, or(Bindings.isNull(sourcePath),
                                                          or(Bindings.isEmpty(targetCollectionTf.textProperty()),
                                                             Bindings.isEmpty(filePrefix.textProperty())))));
        renamingStrategyCb.setConverter(new StrategyStringConverter());
        exampleTf.requestFocus();
        keywords.setSuggestionProvider(request -> allKeywords.stream()
                                                             .filter(kw -> kw.name()
                                                                             .toLowerCase()
                                                                             .contains(request.getUserText().toLowerCase()))
                                                             .collect(Collectors.toList()));
        keywords.setNewItemProducer(ImportView::keywordProducer);
        keywords.setConverter(new KeywordStringConverter());
        keywords.setMatcher((kw, searchText) -> kw.name().toLowerCase().startsWith(searchText.toLowerCase()));
        keywords.setComparator(Comparator.comparing(Keyword::name));

        root.visibleProperty().addListener((observable, oldValue, newValue) -> rooVisible(newValue));
    }

    void rooVisible(boolean isVisible) {
        if (isVisible) {
            allKeywords.addAll(keywordManager.getAll());
        } else {
            allKeywords.clear();
        }
    }

    private void newSourcePath(Path newValue) {
        filesCounter.setText("");
        if (newValue != null && Files.exists(newValue)) {
            final var task = collectionManager.scanDirectory(newValue, false);
            task.setOnSucceeded(_ -> {
                filesCounter.setText(STR."'\{task.getValue().size()}' files found."); // I18N:
                updateExample();
            });
            task.setOnFailed(_ -> updateExample());
            taskService.supply(task);
        }
    }

    private Node createForm() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setPadding(new Insets(40, 10, 10, 10));
        grid.setVgap(10); //vertical gap in pixels
        int rowIdx = 0;

        grid.add(createLabel("Scan directory:", 200, 300), 0, rowIdx); // I18N:
        sourceDir.setEditable(false);
        sourceDir.setPromptText("Select a directory to import"); // I18N:
        FontIcon selectInputDirIco = new FontIcon(MaterialDesignF.FOLDER_OPEN_OUTLINE);
        selectInputDirIco.setOnMouseClicked(this::chooseDirectory);
        selectInputDirIco.setCursor(Cursor.HAND);

        sourceDir.setRight(selectInputDirIco);
        sourceDir.setPrefWidth(SECOND_COL_PREF_WIDTH);
        sourceDir.pseudoClassStateChanged(Styles.STATE_DANGER, true);
        sourceDir.textProperty()
                 .addListener((_, _, newV) -> {
                     sourceDir.pseudoClassStateChanged(Styles.STATE_DANGER, false);
                     sourceDir.pseudoClassStateChanged(Styles.STATE_SUCCESS, false);
                     sourceDir.pseudoClassStateChanged(LangUtils.isBlank(newV)
                                                       ? Styles.STATE_DANGER
                                                       : Styles.STATE_SUCCESS, true);
                 });
        grid.add(sourceDir, 1, rowIdx);

        rowIdx += 1;
//        var countGrp = new InputGroup(filesCounter, new Label("files found."));
        HBox.setHgrow(filesCounter, Priority.ALWAYS);
        grid.add(filesCounter, 1, rowIdx, 2, 1);

        rowIdx += 1;
        grid.add(createLabel("Target collection:", 200, 300), 0, rowIdx); // I18N:
        targetCollectionTf = createCustomText(false, new FontIcon(MaterialDesignF.FOLDER_OPEN_OUTLINE), this::chooseCollectionPath);
        targetCollectionTf.setPromptText("Select a collection and/or a subpath"); // I18N:

        targetCollectionTf.setEditable(false);
        grid.add(targetCollectionTf, 1, rowIdx);

        rowIdx += 1;
        grid.add(createLabel("Sub-directory", 200, 300), 0, rowIdx); // I18N:
        targetSubDirTf = new TextField();
        targetSubDirTf.setPromptText("Create a new sub-directory into collection."); // I18N:
        targetSubDirTf.textProperty().addListener((_, _, _) -> updateExample());
        // TODO check if directory already exits into collections.
        grid.add(targetSubDirTf, 1, rowIdx);

        rowIdx += 1;
        // TODO: Add inoformation icon with help.
        genThumbailsCb.setSelected(false);
        genThumbailsCb.setDisable(true);
        genThumbailsCb.setLabelPosition(HorizontalDirection.RIGHT);
        genThumbailsCb.selectedProperty().addListener((_, _, _) -> log.info("Selected"));
        grid.add(genThumbailsCb, 1, rowIdx);
        var infoThumb = new Label("", new FontIcon(MaterialDesignI.INFORMATION_OUTLINE));
        infoThumb.setPadding(new Insets(0, 10, 0, 0));
        infoThumb.getStyleClass().addAll(Styles.SMALL, Styles.ACCENT);
        infoThumb.setTooltip(new Tooltip("Generate thumbnail even if image already contains one.")); // I18N:
        GridPane.setHalignment(infoThumb, HPos.RIGHT);
        grid.add(infoThumb, 0, rowIdx);

        rowIdx += 1;
        deleteFilesCb.setSelected(false);
        deleteFilesCb.setDisable(false);
        deleteFilesCb.setLabelPosition(HorizontalDirection.RIGHT);
        grid.add(deleteFilesCb, 1, rowIdx);
        var infoDelete = new Label("", new FontIcon(MaterialDesignI.INFORMATION_OUTLINE));
        infoDelete.setPadding(new Insets(0, 10, 0, 0));
        infoDelete.getStyleClass().addAll(Styles.SMALL, Styles.ACCENT);
        Tooltip tt = new Tooltip("""
                                         !! After being imported,
                                            Files will be deleted from source drive !!
                                         """); // I18N:
        infoDelete.setTooltip(tt);
        GridPane.setHalignment(infoDelete, HPos.RIGHT);
        grid.add(infoDelete, 0, rowIdx);

        rowIdx += 1;
        grid.add(new Separator(Orientation.HORIZONTAL), 0, rowIdx, 2, 1);

        rowIdx += 1;
        grid.add(createLabel(i18N.get("import.keyword.label"), 200, 300), 0, rowIdx);
        keywords = new TagsField<>();
        keywords.getEditor().setPromptText(i18N.get("import.keyword.editor.prompt"));

        grid.add(keywords, 1, rowIdx);

        rowIdx += 1;
        grid.add(createLabel("Filename prefix", 200, 300), 0, rowIdx); // I18N:
        filePrefix.setPromptText("Add a filename prefix like: Ibiza-"); // I18N:
        filePrefix.setPrefWidth(SECOND_COL_PREF_WIDTH);
        filePrefix.textProperty().addListener((_, _, newV) -> {
            updateExample();
            filePrefix.pseudoClassStateChanged(Styles.STATE_DANGER, false);
            filePrefix.pseudoClassStateChanged(Styles.STATE_SUCCESS, false);
            filePrefix.pseudoClassStateChanged(LangUtils.isBlank(newV) ? Styles.STATE_DANGER : Styles.STATE_SUCCESS, true);
        });
        filePrefix.pseudoClassStateChanged(Styles.STATE_DANGER, true);
        filePrefix.setText("");
        grid.add(filePrefix, 1, rowIdx);

        rowIdx += 1;
        grid.add(createLabel("File pattern", 200, 350), 0, rowIdx); // I18N:
        renamingStrategyCb.setItems(strategies);
        grid.add(renamingStrategyCb, 1, rowIdx);
        renamingStrategyCb.getSelectionModel().selectedItemProperty().addListener((_, _, _) -> updateExample());

        rowIdx += 1;
        grid.add(new Separator(Orientation.HORIZONTAL), 0, rowIdx, 2, 1);

        rowIdx += 1;
        grid.add(createLabel("Example", 200, 300), 0, rowIdx); // I18N:
        exampleTf.setEditable(false);
        exampleTf.setFocusTraversable(false);
        exampleTf.setWrapText(true);
        exampleTf.setPrefRowCount(4);
        exampleTf.setMinWidth(400);
        exampleTf.setPrefWidth(SECOND_COL_PREF_WIDTH);
        grid.add(exampleTf, 1, rowIdx);

        rowIdx += 1;
        importBtn.setDefaultButton(true);
        importBtn.setDisable(true);
        importBtn.setOnMouseClicked(this::runImport);

        Button reset = new Button("", new FontIcon(MaterialDesignR.REFRESH));
        reset.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        reset.setMnemonicParsing(true);
        reset.setOnMouseClicked(_ -> reset());
        HBox hbox = new HBox(20, reset, importBtn);
        hbox.setAlignment(Pos.CENTER_RIGHT);
        grid.add(hbox, 0, rowIdx, 2, 1);

        renamingStrategyCb.getSelectionModel().selectFirst();

        TitledPane titled = new TitledPane("Copy and Import photos from removable drive", grid); // I18N:
        titled.getStyleClass().add(Styles.ELEVATED_4);
        titled.setCollapsible(false);
        titled.setCenterShape(true);
        titled.setMaxWidth(0);
        titled.setAlignment(Pos.CENTER);
        StackPane.setAlignment(titled, Pos.CENTER);

        return titled;
    }

    private static Keyword keywordProducer(String name) {
        return Keyword.builder().id(null).name(name).build();
    }


    public record RenameFile(MediaFile source, Path target) {
    }

    private void runImport(MouseEvent event) {
        if (Files.exists(sourcePath.get())) {
            final var                  task        = collectionManager.scanDirectory(sourcePath.get(), false);
            final IRenameFilesStrategy strategy = renamingStrategyCb.getSelectionModel().getSelectedItem();
            final LocalDate            now         = LocalDate.now();
            final boolean              deleteFiles = deleteFilesCb.isSelected();
            final MediaCollection mc = persistenceService.findAllMediaCollection()
                                                         .stream()
                                                         .filter(mediaCollection -> targetCollection.startsWith(mediaCollection.path()))
                                                         .findFirst()
                                                         .orElseThrow();
            strategy.reset();
            isRunning.set(true);

//            try (var scope = new FxRunAllScope<Set<Path>>(taskService, "Import files.")) {
//                var dirTask= scope.fork(IFxCallable.wrap(STR."Scanning directory: \{sourcePath.get()}",
//                                                        () -> collectionManager.scanDir(sourcePath.get(), false)));
//                scope.join();
//                dirTask.get().re;
//            } catch (InterruptedException e) {
//                log.error("Unexpected error", e);
//            }


            taskService.supply(task)
                       .thenAccept(paths -> {
                           Path target = LangUtils.isBlank(targetSubDirTf.getText())
                                         ? targetCollection
                                         : Unchecked.<Path>unaryOperator(Files::createDirectories)
                                                    .apply(targetCollection.resolve(targetSubDirTf.getText()));
                           directoryWatcher.registerAll(target);
                           var files = paths.stream()
                                            .peek(path -> log.debug("Importing: '{}'", path))
                                            .flatMap(f -> collectionManager.create(now, f, false).stream())
                                            .map(mf -> new RenameFile(mf, createDestinationPath(mf, strategy)))
                                            .toList();
                           Task<List<MediaFile>> copyFilesTask = importFiles(files);
                           copyFilesTask.setOnSucceeded(_ -> taskService.sendEvent(NotificationEvent.builder()
                                                                                                    .message("'%s' file(s) imported !".formatted(
                                                                                                            copyFilesTask.getValue()
                                                                                                                         .size()))
                                                                                                    .type(NotificationEvent.NotificationType.SUCCESS)
                                                                                                    .source(this)
                                                                                                    .build()));
                           taskService.supply(copyFilesTask)
                                      .thenAccept(mediaFiles -> Platform.runLater(() -> {
                                          isRunning.set(false);
                                          taskService.sendEvent(ShowOrganizeEvent.builder()
                                                                                 .collectionsId(mc.id())
                                                                                 .directory(target)
                                                                                 .source(this)
                                                                                 .build());
                                          var filesToBeDeleted = askForDeletion(mediaFiles, deleteFiles, sourcePath.get());
                                          Thread.ofVirtual().start(() -> deleteAllFiles(filesToBeDeleted));
                                          reset();
                                      }));
                       })
                       .exceptionally(throwable -> {
                           log.error("Unexpected error", throwable);
                           Platform.runLater(() -> isRunning.set(false));
                           return null;
                       });
        }
    }

    private Task<List<MediaFile>> importFiles(List<RenameFile> files) {
        directoryWatcher.setSuspend(true);

        return ModernTask.<List<MediaFile>>builder()
                         .execute(self -> {
                             self.updateTitle("Copy files"); // I18N:
                             self.updateProgress(0, files.size());
                             var i = new AtomicInteger(0);
                             Set<Keyword> kw = Set.copyOf(keywords.getTags());
                             return files.stream()
                                         .peek(rf -> {
                                             self.updateMessage(STR."Copy: \{rf.source.getFullPath().getFileName()}"); // I18N:
                                             self.updateProgress(i.incrementAndGet(), files.size());
                                         })
                                         .map(ImportView::copyAndRename)
                                         .peek(renameFile -> addKeywords(renameFile, kw))
                                         .filter(Objects::nonNull)
                                         .map(rf -> rf.source)
                                         .toList();
                         })
                         .onFinished(() -> directoryWatcher.setSuspend(false))
                         .build();
    }

    private void addKeywords(RenameFile renameFile, Set<Keyword> kw) {
        if (!kw.isEmpty()) {
            metadataWriter.addKeywords(renameFile.target, Set.copyOf(kw));
        }
    }

    private List<MediaFile> askForDeletion(List<MediaFile> files, boolean deleteFiles, Path path) {
        if (deleteFiles) {
            log.info("Delete '{}' files.", files.size());
            final Alert dlg = new Alert(Alert.AlertType.CONFIRMATION, """
                    Do you really want to deleted all source files ?
                      '%d' files living into: '%s'.
                    """.formatted(files.size(), path)); // I18N:
            dlg.initOwner(getRootContent().getScene().getWindow());
            dlg.setTitle("Delete files"); // I18N:

            return Nodes.show(dlg, getRootContent().getScene())
                        .map(buttonType -> buttonType == ButtonType.OK)
                        .map(_ -> files)
                        .orElse(emptyList());
        }
        return emptyList();
    }

    private void deleteAllFiles(List<MediaFile> mediaFiles) {
        if (!mediaFiles.isEmpty()) {
            try (var scope = new FxRunAllScope<>(taskService, STR."Deletes '\{mediaFiles.size()}' files.")) {
                final AtomicInteger i = new AtomicInteger(1);
                var tasks = mediaFiles.stream()
                                      .map(mf -> IFxCallable.wrap(STR."File: '\{mf.getFullPath().getFileName()}' deleted.", () -> {
                                          FileUtils.deleteQuietly(mf.getFullPath().toFile());
                                          return null;
                                      }))
                                      .map(scope::fork)
                                      .toList();
                scope.join();
                taskService.sendEvent(NotificationEvent.builder()
                                                       .message("'%s' file(s) deleted !".formatted(tasks.size()))
                                                       .type(NotificationEvent.NotificationType.SUCCESS)
                                                       .source(this)
                                                       .build());
            } catch (InterruptedException e) {
                log.error("Unexpected error", e);
            }
        }
    }

    private static RenameFile copyAndRename(RenameFile rf) {
        log.info("File copied from: '{}', to: '{}'", rf.source, rf.target);
        try {
            Files.createDirectories(rf.target.getParent());
            Files.copy(rf.source.getFullPath(), rf.target);
            // Add keywords:
            return rf;
        } catch (FileAlreadyExistsException e) {
            log.error("Cannot copy file, it already exists: '{}'", rf.target);
            return null;
        } catch (IOException e) {
            log.error("Cannot copy file: {}, error", rf.target, e);
            return null;
        }
    }

    private void chooseCollectionPath(MouseEvent e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Choose a collection path");
        alert.setHeaderText(null);
        TreeItem<Path> rootTreeItem = new TreeItem<>(Path.of("root"));
        TreeView<Path> treeView     = new TreeView<>(rootTreeItem);
        persistenceService.findAllMediaCollection()
                          .forEach(mediaCollection -> createTreeView(rootTreeItem, mediaCollection));
        treeView.setCellFactory(_ -> new TreeCell<>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getFileName().toString());
            }
        });
        treeView.setMinWidth(400);
        treeView.setPrefWidth(300);
        treeView.setShowRoot(false);
        treeView.setEditable(false);
        rootTreeItem.setExpanded(true);
        alert.getDialogPane().contentProperty().setValue(treeView);
        Nodes.show(alert, getRootContent().getScene()).filter(buttonType -> buttonType == ButtonType.OK)
             .ifPresent(_ -> {
                 if (treeView.getSelectionModel().getSelectedItem() != null) {
                     log.info("OK: {}", treeView.getSelectionModel().getSelectedItem().getValue());
                     targetCollection = treeView.getSelectionModel().getSelectedItem().getValue();
                     targetCollectionTf.setText(targetCollection.toString());
                     updateExample();
                 }
             });
    }

    private void createTreeView(final TreeItem<Path> rootTreeItem, final MediaCollection mediaCollection) {
        var pathTreeItem = new TreeItem<>(mediaCollection.path());
        rootTreeItem.getChildren().add(pathTreeItem);
        pathTreeItem.setExpanded(false);
        log.info("Root Path: {}", pathTreeItem.getValue());

        mediaCollection.getSubdir()
                       .forEach(subPath -> {
                           var p = mediaCollection.path().resolve(subPath);
                           addSubDir(pathTreeItem, p, mediaCollection.id());
                       });
    }

    private void addSubDir(TreeItem<Path> current, Path path, int id) {
        path = current.getValue().relativize(path);
        for (int i = 0; i < path.getNameCount(); i++) {
            var       subPath = path.subpath(i, i + 1);
            final var c       = current;
            final var child   = c.getValue().resolve(subPath);
            current = current.getChildren()
                             .stream()
                             .filter(pathTreeItem -> pathTreeItem.getValue().equals(child))
                             .findFirst()
                             .orElseGet(() -> {
                                 var newItem = new TreeItem<>(child);
                                 c.getChildren().add(newItem);
                                 c.getChildren()
                                  .sort(Comparator.comparing(ti -> ti.getValue().getFileName().toString(),
                                                             String.CASE_INSENSITIVE_ORDER));
                                 return newItem;
                             });
        }
    }

    private CustomTextField createCustomText(boolean isUpdateExample, FontIcon icon, Consumer<MouseEvent> cb) {
        var textField = new CustomTextField();

        textField.setRight(icon);
        icon.setCursor(Cursor.HAND);
        icon.setOnMouseClicked(cb::accept);

        textField.pseudoClassStateChanged(Styles.STATE_DANGER, true);
        textField.textProperty().addListener((_, _, newV) -> {
            if (isUpdateExample) {
                updateExample();
            }

            textField.pseudoClassStateChanged(Styles.STATE_DANGER, false);
            textField.pseudoClassStateChanged(Styles.STATE_SUCCESS, false);
            if (LangUtils.isBlank(newV)) {
                textField.pseudoClassStateChanged(Styles.STATE_DANGER, true);
            } else {
                textField.pseudoClassStateChanged(Styles.STATE_SUCCESS, true);
            }
        });
        HBox.setHgrow(textField, Priority.ALWAYS);

        textField.setPrefWidth(SECOND_COL_PREF_WIDTH);

        return textField;
    }

    private InputGroup createText(boolean isUpdateExample, FontIcon icon, Consumer<MouseEvent> cb) {
        var textField = new TextField();
        var button    = new Button("", icon);

        button.setCursor(Cursor.HAND);
        button.getStyleClass().add(Styles.BUTTON_ICON);

        textField.pseudoClassStateChanged(Styles.STATE_DANGER, true);
        button.pseudoClassStateChanged(Styles.STATE_DANGER, true);
        textField.textProperty().addListener((_, _, newV) -> {
            if (isUpdateExample) {
                updateExample();
            }
            button.pseudoClassStateChanged(Styles.STATE_DANGER, false);
            button.pseudoClassStateChanged(Styles.STATE_SUCCESS, true);

            textField.pseudoClassStateChanged(Styles.STATE_DANGER, false);
            textField.pseudoClassStateChanged(Styles.STATE_SUCCESS, false);
            if (LangUtils.isBlank(newV)) {
                textField.pseudoClassStateChanged(Styles.STATE_DANGER, true);
                button.pseudoClassStateChanged(Styles.STATE_DANGER, true);
            } else {
                textField.pseudoClassStateChanged(Styles.STATE_SUCCESS, true);
                button.pseudoClassStateChanged(Styles.STATE_SUCCESS, true);
            }
        });
        HBox.setHgrow(textField, Priority.ALWAYS);

        var group = new InputGroup(textField, button);
        group.setPrefWidth(SECOND_COL_PREF_WIDTH);

        return group;
    }

    private void updateExample() {
        exampleTf.setText("");
        var strategy = renamingStrategyCb.getSelectionModel().getSelectedItem();
        var stringBuilder = new StringBuilder();

        strategy.reset();
        for (var mf : fakeMf) {
            stringBuilder.append(createDestinationPath(mf, strategy));
            stringBuilder.append(System.lineSeparator());
        }
        exampleTf.setText(stringBuilder.toString());
    }

    private void reset() {
        sourcePath.set(null);
        targetCollection = null;
        targetCollectionTf.setText("");
        filePrefix.setText("");
        exampleTf.setText("");
        filesCounter.setText("");
        keywords.clear();
        deleteFilesCb.setSelected(false);
        genThumbailsCb.setSelected(false);
    }


    Path createDestinationPath(MediaFile mediaFile, IRenameFilesStrategy strategy) {
        String target = LangUtils.isBlank(targetSubDirTf.getText())
                        ? filePrefix.getText()
                        : Paths.get(targetSubDirTf.getText(), filePrefix.getText()).toString();
        if (targetCollection != null) {
            return targetCollection.resolve(target + strategy.computeNewFileName(mediaFile));
        }
        return Path.of(target + strategy.computeNewFileName(mediaFile));
    }

    private void chooseDirectory(MouseEvent event) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        if (sourcePath.get() != null) {
            directoryChooser.setInitialDirectory(sourcePath.get().toFile());
        }

        final File selectedDirectory = directoryChooser.showDialog(root.getScene().getWindow());
        if (selectedDirectory != null) {
            var newImportPath = selectedDirectory.toPath().normalize();
            if (collectionManager.isSameOrSubCollection(newImportPath).isPresent()) {
                taskService.sendEvent(NotificationEvent.builder()
                                                       .message("Diretory is already available into medias collections: '%s'"
                                                                        .formatted(newImportPath))
                                                       .type(NotificationEvent.NotificationType.ERROR)
                                                       .source(this)
                                                       .build());
            } else {
                sourcePath.set(newImportPath);
            }
        }
    }

    @FxEventListener
    public void importDir(ImportDirectoryEvent event) {
        sourcePath.set(event.getRootDirectory().normalize());
    }

    @FxEventListener
    public void listenEvent(UsbStorageDeviceEvent event) {
        log.info("event: {}", event);
        if (event.getType() == UsbStorageDeviceEvent.EventType.REMOVED) {
            sourcePath.set(null);
        }
    }

    @Override
    public StackPane getRootContent() {
        return root;
    }

}
