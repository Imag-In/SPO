package org.icroco.picture.views.ext_import;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.layout.InputGroup;
import atlantafx.base.theme.Styles;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.geometry.HorizontalDirection;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.event.ImportDirectoryEvent;
import org.icroco.picture.event.NotificationEvent;
import org.icroco.picture.event.ShowOrganizeEvent;
import org.icroco.picture.event.UsbStorageDeviceEvent;
import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.persistence.PersistenceService;
import org.icroco.picture.views.AbstractView;
import org.icroco.picture.views.CollectionManager;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.ViewConfiguration;
import org.icroco.picture.views.task.AbstractTask;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.LangUtils;
import org.icroco.picture.views.util.Nodes;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static javafx.beans.binding.Bindings.or;

@Slf4j
@RequiredArgsConstructor
@Component
public class ImportView extends AbstractView<StackPane> {
    public static final int                FIRST_COL_PREF_WIDTH = 400;
    private final       TaskService        taskService;
    private final       PersistenceService persistenceService;
    private final       CollectionManager  collectionManager;
    private final       StackPane          root                 = new StackPane();
    private final       CustomTextField    sourceDir            = new CustomTextField();
    private             TextField          targetCollectionTf;
    private             TextField          tags;
    private final       TextArea           exampleTf            = new TextArea();
    private final       CustomTextField    filePrefix           = new CustomTextField("");
    private final       Label              filesCounter         = new Label("");
    private final       Button             importBtn            = new Button("Import");
    private final       ToggleSwitch       genThumbailsCb       = new ToggleSwitch("Generate high quality thumbnails");
    private final       ToggleSwitch       deleteFilesCb        = new ToggleSwitch("Delete imported files");

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

    private final SimpleBooleanProperty      isRunning      = new SimpleBooleanProperty(false);
    private final SimpleObjectProperty<Path> sourcePath     = new SimpleObjectProperty<>(null);
    private final ComboBox<ERenameStrategy>  renameStrategy = new ComboBox<>();
    private       Path                       targetCollection;

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
    }

    private void newSourcePath(Path newValue) {
        filesCounter.setText("");
        if (newValue != null && Files.exists(newValue)) {
            final var task = collectionManager.scanDirectory(newValue, false);
            task.setOnSucceeded(event -> {
                filesCounter.setText("'" + task.getValue().size() + "' files found.");
                updateExanple();
            });
            task.setOnFailed(unused -> updateExanple());
            taskService.supply(task);
        }
    }

    private Node createForm() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setPadding(new Insets(40, 10, 10, 10));
        grid.setVgap(10); //vertical gap in pixels
        int rowIdx = 0;

        grid.add(createLabel("Scan directory:", 100, 150), 0, rowIdx);
        sourceDir.setEditable(false);
        FontIcon selectInputDirIco = new FontIcon(MaterialDesignF.FOLDER_OPEN_OUTLINE);
        selectInputDirIco.setOnMouseClicked(this::chooseDirectory);
        selectInputDirIco.setCursor(Cursor.HAND);

        sourceDir.setRight(selectInputDirIco);
        sourceDir.setPrefWidth(FIRST_COL_PREF_WIDTH);
        sourceDir.pseudoClassStateChanged(Styles.STATE_DANGER, true);
        sourceDir.textProperty()
                 .addListener((obs, oldV, newV) -> {
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
        grid.add(createLabel("Target collection:", 100, 150), 0, rowIdx);
        targetCollectionTf = createCustomText(false, new FontIcon(MaterialDesignF.FOLDER_OPEN_OUTLINE), this::chooseCollectionPath);
        targetCollectionTf.setEditable(false);
        grid.add(targetCollectionTf, 1, rowIdx);

        rowIdx += 1;
        grid.add(createLabel("Sub-directory", 100, 150), 0, rowIdx);
        CheckBox cb           = new CheckBox();
        var      leftLbl1     = new Label("", cb);
        var      targetSubDir = new TextField();
        targetSubDir.setEditable(false);
        cb.selectedProperty().addListener((observable, oldValue, newValue) -> targetSubDir.setEditable(newValue));
        targetSubDir.setPromptText("Create a new sub-directory into collection.");
        targetSubDir.textProperty().addListener((observable, oldValue, newValue) -> updateExanple());
        HBox.setHgrow(targetSubDir, Priority.ALWAYS);
        var subDirGrp = new InputGroup(leftLbl1, targetSubDir);
        subDirGrp.setPrefWidth(FIRST_COL_PREF_WIDTH);
        grid.add(subDirGrp, 1, rowIdx);

        rowIdx += 1;
        // TODO: Add inoformation icon with help.
        genThumbailsCb.setSelected(false);
        genThumbailsCb.setDisable(true);
        genThumbailsCb.setLabelPosition(HorizontalDirection.RIGHT);
        genThumbailsCb.selectedProperty().addListener((observable, oldValue, newValue) -> log.info("Selected"));
        grid.add(genThumbailsCb, 1, rowIdx);
        var info = new FontIcon(MaterialDesignI.INFORMATION_OUTLINE);
        info.getStyleClass().addAll(Styles.SMALL, Styles.ACCENT);
        grid.add(info, 0, rowIdx);

        rowIdx += 1;
        deleteFilesCb.setSelected(false);
        deleteFilesCb.setDisable(true);
//        deleteFiles.selectedProperty().addListener((observable, oldValue, newValue) -> log.info("deleted ?"));
//        deleteFiles.setPadding(new Insets(5, 0, 5, 0));
        grid.add(deleteFilesCb, 0, rowIdx, 2, 1);

        rowIdx += 1;
        grid.add(new Separator(Orientation.HORIZONTAL), 0, rowIdx, 2, 1);

        rowIdx += 1;
        grid.add(createLabel("Add tags", 100, 150), 0, rowIdx);
        tags = new TextField();
        tags.setEditable(false);
        tags.setPromptText("Not Yet Implemented");
        grid.add(tags, 1, rowIdx);

        rowIdx += 1;
        grid.add(createLabel("Filename prefix", 100, 150), 0, rowIdx);
        filePrefix.setPromptText("Ibiza-");
        filePrefix.setPrefWidth(FIRST_COL_PREF_WIDTH);
        filePrefix.textProperty().addListener((obs, oldV, newV) -> {
            updateExanple();
            filePrefix.pseudoClassStateChanged(Styles.STATE_DANGER, false);
            filePrefix.pseudoClassStateChanged(Styles.STATE_SUCCESS, false);
            filePrefix.pseudoClassStateChanged(LangUtils.isBlank(newV) ? Styles.STATE_DANGER : Styles.STATE_SUCCESS, true);
        });
        filePrefix.pseudoClassStateChanged(Styles.STATE_DANGER, true);
        filePrefix.setText("");
        grid.add(filePrefix, 1, rowIdx);

        rowIdx += 1;
        grid.add(createLabel("File pattern", 100, 150), 0, rowIdx);
        renameStrategy.getItems().addAll(ERenameStrategy.values());
        grid.add(renameStrategy, 1, rowIdx);
        renameStrategy.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> updateExanple());


        rowIdx += 1;
        grid.add(new Separator(Orientation.HORIZONTAL), 0, rowIdx, 2, 1);

        rowIdx += 1;
        grid.add(createLabel("Example", 100, 150), 0, rowIdx);
        exampleTf.setEditable(false);
        exampleTf.setFocusTraversable(false);
        exampleTf.setWrapText(true);
        exampleTf.setPrefRowCount(4);
        grid.add(exampleTf, 1, rowIdx);

        rowIdx += 1;
        importBtn.setDefaultButton(true);
        importBtn.setDisable(true);
        importBtn.setOnMouseClicked(this::runImport);
        Button reset = new Button("", new FontIcon(MaterialDesignR.REFRESH));
        reset.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        reset.setMnemonicParsing(true);
        reset.setOnMouseClicked(event -> reset());
        HBox hbox = new HBox(20, reset, importBtn);
        hbox.setAlignment(Pos.CENTER_RIGHT);
        grid.add(hbox, 0, rowIdx, 2, 1);

        StackPane.setAlignment(grid, Pos.CENTER);
        renameStrategy.getSelectionModel().selectFirst();

        return grid;
    }


    public record RenameFile(MediaFile source, Path target) {
    }

    private void runImport(MouseEvent event) {
        if (Files.exists(sourcePath.get())) {
            final var                  task        = collectionManager.scanDirectory(sourcePath.get(), false);
            final IRenameFilesStrategy strategy    = renameStrategy.getSelectionModel().getSelectedItem().getStrategy();
            final LocalDate            now         = LocalDate.now();
            final boolean              deleteFiles = deleteFilesCb.isSelected();
            final MediaCollection mc = persistenceService.findAllMediaCollection()
                                                         .stream()
                                                         .filter(mediaCollection -> targetCollection.startsWith(mediaCollection.path()))
                                                         .findFirst()
                                                         .orElseThrow();
            strategy.reset();
            isRunning.set(true);
            taskService.supply(task)
                       .thenAccept(paths -> {
                           var files = paths.stream()
                                            .peek(path -> log.debug("Importing: '{}'", path))
                                            .flatMap(f -> collectionManager.create(now, f).stream())
                                            .map(mf -> new RenameFile(mf, createDestinationPath(mf, strategy)))
                                            .toList();
                           Task<List<MediaFile>> copyFiles = importFiles(files, deleteFiles);
                           copyFiles.setOnSucceeded(event1 -> taskService.sendEvent(NotificationEvent.builder()
                                                                                                     .message("'%s' file(s) imported !".formatted(
                                                                                                             copyFiles.getValue()
                                                                                                                      .size()))
                                                                                                     .type(NotificationEvent.NotificationType.SUCCESS)
                                                                                                     .source(this)
                                                                                                     .build()));
                           taskService.supply(copyFiles)
                                      .thenRun(() -> Platform.runLater(() -> {
                                          isRunning.set(false);
                                          taskService.sendEvent(ShowOrganizeEvent.builder()
                                                                                 .collectionsId(mc.id())
                                                                                 .directory(targetCollection)
                                                                                 .source(this)
                                                                                 .build());
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

    private Task<List<MediaFile>> importFiles(List<RenameFile> files, boolean deleteFiles) {
        return new AbstractTask<>() {

            @Override
            protected List<MediaFile> call() throws Exception {
                updateTitle("Copy files");
                updateProgress(0, files.size());
                var i = new AtomicInteger(0);
                return files.stream()
                            .peek(rf -> {
                                updateMessage("Copy: " + rf.source.getFullPath().getFileName());
                                updateProgress(i.incrementAndGet(), files.size());
                            })
                            .map(rf -> {
                                log.info("File copied from: '{}', to: '{}'", rf.source, rf.target);
                                try {
                                    Files.copy(rf.source.getFullPath(), rf.target);
                                    return rf;
                                } catch (FileAlreadyExistsException e) {
                                    log.error("Cannot copy file, it already exists: '{}'", rf.target);
                                    return null;
                                } catch (IOException e) {
                                    log.error("Cannot copy file: {}, error", rf.target, e);
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .peek(rf -> {
                                if (deleteFiles) {
                                    // TODO: Delete if flag is check.
                                    log.info("File deleted: {}", rf.source());
                                }
                            })
                            .peek(rf -> rf.source.setFullPath(rf.target))
                            .map(rf -> rf.source)
                            .toList();
            }
        };
    }

    private void chooseCollectionPath(MouseEvent e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Choose a collection path");
        alert.setHeaderText(null);
        TreeItem<Path> rootTreeItem = new TreeItem<Path>(Path.of("root"));
        TreeView<Path> treeView     = new TreeView<>(rootTreeItem);
        persistenceService.findAllMediaCollection()
                          .forEach(mediaCollection -> createTreeView(rootTreeItem, mediaCollection));
        treeView.setCellFactory(param -> new TreeCell<>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
//                log.info("render path: {}", empty || item == null ? "" : item.getFileName().toString());
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
             .ifPresent(buttonType -> {
                 log.info("OK: {}", treeView.getSelectionModel().getSelectedItem().getValue());
                 targetCollection = treeView.getSelectionModel().getSelectedItem().getValue();
                 targetCollectionTf.setText(targetCollection.toString());
                 updateExanple();
             });
    }

    private void createTreeView(final TreeItem<Path> rootTreeItem, final MediaCollection mediaCollection) {
        var pathTreeItem = new TreeItem<>(mediaCollection.path());
        rootTreeItem.getChildren().add(pathTreeItem);
        pathTreeItem.setExpanded(true);
        log.info("Root Path: {}", pathTreeItem.getValue());

        mediaCollection.subPaths().forEach(subPath -> {
            var p = mediaCollection.path().resolve(subPath.name());
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
        textField.textProperty().addListener((obs, oldV, newV) -> {
            if (isUpdateExample) {
                updateExanple();
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

        textField.setPrefWidth(FIRST_COL_PREF_WIDTH);

        return textField;
    }

    private InputGroup createText(boolean isUpdateExample, FontIcon icon, Consumer<MouseEvent> cb) {
        var textField = new TextField();
        var button    = new Button("", icon);

        button.setCursor(Cursor.HAND);
        button.getStyleClass().add(Styles.BUTTON_ICON);

        textField.pseudoClassStateChanged(Styles.STATE_DANGER, true);
        button.pseudoClassStateChanged(Styles.STATE_DANGER, true);
        textField.textProperty().addListener((obs, oldV, newV) -> {
            if (isUpdateExample) {
                updateExanple();
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
        group.setPrefWidth(FIRST_COL_PREF_WIDTH);

        return group;
    }

    private void updateExanple() {
        exampleTf.setText("");
        var strategy      = renameStrategy.getSelectionModel().getSelectedItem().getStrategy();
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
        tags.setText("");
        deleteFilesCb.setSelected(false);
        genThumbailsCb.setSelected(false);
    }


    Path createDestinationPath(MediaFile mediaFile, IRenameFilesStrategy strategy) {
        if (targetCollection != null) {
            return targetCollection.resolve(filePrefix.getText() + strategy.computeNewFileName(mediaFile));
        }
        return Path.of(filePrefix.getText() + strategy.computeNewFileName(mediaFile));
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
                // TODO: Add into a task;
//                try (var filesStream = Files.walk(newImportPath, 1)) {
//                    Set<Path> files = filesStream.filter(p -> !Files.isDirectory(p))   // not a directory
//                                                 .map(Path::normalize)
//                                                 .filter(Constant::isSupportedExtension)
//                                                 .collect(Collectors.toSet());
//                    filesCounter.setText("'"+files.size()+"' files found.");
//                    log.info("Files: {}", files.size());
//                } catch (IOException ex) {
//                    log.error("Cannot scan directory: {}", newImportPath, ex);
//                }
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
