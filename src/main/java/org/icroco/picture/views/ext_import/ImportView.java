package org.icroco.picture.views.ext_import;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.layout.InputGroup;
import atlantafx.base.theme.Styles;
import jakarta.annotation.PostConstruct;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import org.icroco.picture.event.UsbStorageDeviceEvent;
import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.persistence.PersistenceService;
import org.icroco.picture.views.AbstractView;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.ViewConfiguration;
import org.icroco.picture.views.organize.collections.CollectionView;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.Constant;
import org.icroco.picture.views.util.LangUtils;
import org.icroco.picture.views.util.Nodes;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static javafx.beans.binding.Bindings.and;

@Slf4j
@RequiredArgsConstructor
@Component
public class ImportView extends AbstractView<StackPane> {
    public static final int                FIRST_COL_PREF_WIDTH = 400;
    private final       TaskService        taskService;
    private final       PersistenceService persistenceService;
    private final       CollectionView     collectionView;
    private final       StackPane          root                 = new StackPane();
    private final       CustomTextField    sourceDir            = new CustomTextField();
    private             TextField          targetCollection;
    private final       TextField          exampleTf            = new TextField();
    private final       CustomTextField    filePrefix           = new CustomTextField("");
    private final       Label              filesCounter         = new Label("?");

    private final Button runBtn = new Button("Run");

    private MediaFile fakeMf = MediaFile.builder()
                                        .originalDate(LocalDateTime.now())
                                        .fullPath(Path.of("example.png"))
                                        .build();

    private final SimpleBooleanProperty canImport = new SimpleBooleanProperty(false);

    private SimpleObjectProperty<Path> sourcePath = new SimpleObjectProperty<>(null);
    private Path                       collection = null;
    private Path                       subPath    = null;
    private RenameSuffixStrategy       strategy   = new IncrSuffixStrategy();

    @PostConstruct
    private void postConstruct() {
        root.setId(ViewConfiguration.V_IMPORT);
        root.getStyleClass().add(ViewConfiguration.V_IMPORT);
        root.getChildren().add(createForm());

        sourcePath.addListener((observable, oldValue, newValue) -> updateExanple());
        sourceDir.textProperty().bind(sourcePath.map(Path::toString));
        runBtn.disableProperty().bind(Bindings.not(and(Bindings.isNotNull(sourcePath),
                                                       and(Bindings.isNotEmpty(targetCollection.textProperty()),
                                                           Bindings.isNotEmpty(filePrefix.textProperty())))));
    }

    interface RenameSuffixStrategy {
        default void reset() {
        }

        String computeNewFileName(MediaFile mediaFile);
    }

    class IncrSuffixStrategy implements RenameSuffixStrategy {
        private int index = 0;

        @Override
        public void reset() {
            index = 0;
        }

        @Override
        public String computeNewFileName(MediaFile mediaFile) {
            return String.valueOf(++index);
        }
    }

    class DateSuffixStrategy implements RenameSuffixStrategy {
        private DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        private Set<String>       cache  = new HashSet<>();

        @Override
        public String computeNewFileName(MediaFile mediaFile) {
            var valueOriginal = format.format(mediaFile.getOriginalDate());
            var value         = valueOriginal;

            int idx = 0;
            while (cache.contains(value)) {
                value = valueOriginal + "-" + (++idx);
            }
            return value;
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
        var countGrp = new InputGroup(filesCounter, new Label("files found."));
        grid.add(countGrp, 1, rowIdx);

        rowIdx += 1;
        grid.add(createLabel("Target collection:", 100, 150), 0, rowIdx);
        targetCollection = createCustomText(false, new FontIcon(MaterialDesignF.FOLDER_OPEN_OUTLINE), this::chooseCollectionPath);
        targetCollection.setEditable(false);
        grid.add(targetCollection, 1, rowIdx);

        rowIdx += 1;
        grid.add(createLabel("New directory", 100, 150), 0, rowIdx);
        CheckBox cb           = new CheckBox();
        var      leftLbl1     = new Label("", cb);
        var      targetSubDir = new TextField();
        targetSubDir.setEditable(false);
        cb.selectedProperty().addListener((observable, oldValue, newValue) -> targetSubDir.setEditable(newValue));
        targetSubDir.setPromptText("Directory");
        targetSubDir.textProperty().addListener((observable, oldValue, newValue) -> updateExanple());
        HBox.setHgrow(targetSubDir, Priority.ALWAYS);
        var subDirGrp = new InputGroup(leftLbl1, targetSubDir);
        subDirGrp.setPrefWidth(FIRST_COL_PREF_WIDTH);
        grid.add(subDirGrp, 1, rowIdx);

        rowIdx += 1;
        var genThumbails = new ToggleSwitch("Generate high quality thumbnails");
        genThumbails.setSelected(false);
        genThumbails.selectedProperty().addListener((observable, oldValue, newValue) -> log.info("Selected"));
        grid.add(genThumbails, 0, rowIdx, 2, 1);

        rowIdx += 1;
        var deleteFiles = new ToggleSwitch("Delete imported files");
        deleteFiles.setSelected(false);
        deleteFiles.selectedProperty().addListener((observable, oldValue, newValue) -> log.info("deleted ?"));
//        deleteFiles.setPadding(new Insets(5, 0, 5, 0));
        grid.add(deleteFiles, 0, rowIdx, 2, 1);

        rowIdx += 1;
        grid.add(new Separator(Orientation.HORIZONTAL), 0, rowIdx, 2, 1);

        rowIdx += 1;
        grid.add(createLabel("Filename prefix", 100, 150), 0, rowIdx);
        filePrefix.setPromptText("Ibiza-");
        filePrefix.setPrefWidth(FIRST_COL_PREF_WIDTH);
        filePrefix.textProperty().addListener((obs, oldV, newV) -> {
            updateExanple();
            filePrefix.pseudoClassStateChanged(Styles.STATE_DANGER, false);
            filePrefix.pseudoClassStateChanged(Styles.STATE_SUCCESS, false);
            filePrefix.pseudoClassStateChanged(LangUtils.isBlank(newV)
                                               ? Styles.STATE_DANGER
                                               : Styles.STATE_SUCCESS, true);
        });
        filePrefix.pseudoClassStateChanged(Styles.STATE_DANGER, true);
        filePrefix.setText("");
        grid.add(filePrefix, 1, rowIdx);

        rowIdx += 1;
        grid.add(createLabel("Suffix pattern", 100, 150), 0, rowIdx);
        ComboBox<String> suffixPattern = new ComboBox<>();
        suffixPattern.getItems().addAll("%i", "%yyyy%mm%dd", "%yyyy%mm%dd-%hh%mm&ss");
        grid.add(suffixPattern, 1, rowIdx);
        suffixPattern.selectionModelProperty().addListener((observable, oldValue, newValue) -> updateExanple());


        rowIdx += 1;
        grid.add(new Separator(Orientation.HORIZONTAL), 0, rowIdx, 2, 1);

        rowIdx += 1;
        grid.add(createLabel("Example", 100, 150), 0, rowIdx);
        exampleTf.setEditable(false);
        exampleTf.setFocusTraversable(false);
        grid.add(exampleTf, 1, rowIdx);

        rowIdx += 1;
        runBtn.setDefaultButton(true);
        runBtn.setDisable(true);
        grid.add(runBtn, 0, rowIdx);

        StackPane.setAlignment(grid, Pos.CENTER);
        suffixPattern.getSelectionModel().selectFirst();

        return grid;
    }

    private void chooseCollectionPath(MouseEvent e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Choose a collection path");
        alert.setHeaderText(null);
        TreeItem<Path> rootTreeItem = new TreeItem<Path>(Path.of("root"));
        TreeView<Path> treeView     = new TreeView<>(rootTreeItem);
        persistenceService.findAllMediaCollection()
                          .forEach(mediaCollection -> createTreeView(rootTreeItem, mediaCollection));
        treeView.setCellFactory(param -> new TreeCell<Path>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                log.info("render path: {}", empty || item == null ? "" : item.getFileName().toString());
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
                 targetCollection.setText(treeView.getSelectionModel().getSelectedItem().getValue().toString());
             });
    }

    private void createTreeView(final TreeItem<Path> rootTreeItem, final MediaCollection mediaCollection) {
        var pathTreeItem = new TreeItem<>(mediaCollection.path());
        rootTreeItem.getChildren().add(pathTreeItem);
        pathTreeItem.setExpanded(true);
        log.info("Root Path: {}", pathTreeItem.getValue());

        mediaCollection.subPaths().forEach(subPath -> {
            var p = mediaCollection.path().resolve(subPath.name());
            log.info("Path: {}", p);
            addSubDir(pathTreeItem, p, mediaCollection.id());
        });
    }

    private void addSubDir(TreeItem<Path> current, Path path, int id) {
        path = current.getValue().relativize(path);
        for (int i = 0; i < path.getNameCount(); i++) {
//            var p = mediaCollection.path().relativize(c.name());
            log.info("Current: {}, path: {}", current.getValue(), path);
            var child = new TreeItem<>(current.getValue().resolve(path.subpath(0, i + 1)));
            if (current.getChildren().stream().noneMatch(pathTreeItem -> pathTreeItem.getValue().equals(child.getValue()))) {
                current.getChildren().add(child);
            }
            current.getChildren()
                   .sort(Comparator.comparing(ti -> ti.getValue().getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
            current = child;
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
        strategy.reset();
        StringBuilder stringBuilder = new StringBuilder();

//        if (sourcePath.get() != null) {
//            stringBuilder.append(sourcePath.get());
//            stringBuilder.append("/");
//        }
        if (collection != null) {
            stringBuilder.append(collection.getFileName().toString());
            stringBuilder.append("/");
        }
        if (subPath != null) {
            stringBuilder.append(subPath);
            stringBuilder.append("/");
        }

        stringBuilder.append(filePrefix.getText());
        stringBuilder.append(strategy.computeNewFileName(fakeMf));

        exampleTf.setText(stringBuilder.toString());
    }

    private void chooseDirectory(MouseEvent event) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        if (sourcePath.get() != null) {
            directoryChooser.setInitialDirectory(sourcePath.get().toFile());
        }

        final File selectedDirectory = directoryChooser.showDialog(root.getScene().getWindow());

        if (selectedDirectory != null) {
            var newImportPath = selectedDirectory.toPath().normalize();
            sourcePath.set(newImportPath);
            // TODO: Add into a task;
            try (var filesStream = Files.walk(newImportPath, 1)) {
                Set<Path> files = filesStream.filter(p -> !Files.isDirectory(p))   // not a directory
                                             .map(Path::normalize)
                                             .filter(Constant::isSupportedExtension)
                                             .collect(Collectors.toSet());
                filesCounter.setText(String.valueOf(files.size()));
                log.info("Files: {}", files.size());
            } catch (IOException ex) {
                log.error("Cannot scan directory: {}", newImportPath, ex);
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
