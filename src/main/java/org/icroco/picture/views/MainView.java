package org.icroco.picture.views;

import atlantafx.base.controls.ModalPane;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.dialog.ExceptionDialog;
import org.icroco.picture.infra.github.GitHubClient;
import org.icroco.picture.util.Error;
import org.icroco.picture.util.I18N;
import org.icroco.picture.views.ext_import.ImportView;
import org.icroco.picture.views.navigation.NavigationView;
import org.icroco.picture.views.organize.OrganizeView;
import org.icroco.picture.views.repair.RepairView;
import org.icroco.picture.views.status.StatusBarView;
import org.icroco.picture.views.util.FxView;
import org.icroco.picture.views.util.Nodes;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Component
public class MainView implements FxView<StackPane> {

    private final GitHubClient ghClient;
    private final I18N         i18N;

    private final NavigationView       navView;
    private final StatusBarView        statusView;
    private final OrganizeView         organizeView;
    private final ImportView           importView;
    private final RepairView repairView;
    @Qualifier(ViewConfiguration.CURRENT_VIEW)
    private final SimpleStringProperty currentView;

    private final StackPane root       = new StackPane();
    private final StackPane centerView = new StackPane();
    private final ModalPane modalPane = new ModalPane();

    private final Map<String, FxView<?>> views = new HashMap<>(10);

    @PostConstruct
    protected void initializedOnce() {
        log.debug("Primary screen: {}", Screen.getPrimary());
        Screen.getScreens().forEach(screen -> log.debug("Screen: {}", screen));
        root.setId(ViewConfiguration.V_MAIN);
        root.getStyleClass().add(ViewConfiguration.V_MAIN);
        var borderPane = new BorderPane();
        borderPane.setTop(navView.getRootContent());
        borderPane.setBottom(statusView.getRootContent());

        views.putAll(Stream.<FxView<?>>of(importView, organizeView, repairView)
                           .collect(Collectors.toMap(fxView -> fxView.getRootContent().getId(), Function.identity())));

        centerView.getChildren().addAll(views.values()
                                             .stream()
                                             .peek(fxView -> fxView.getRootContent().setVisible(false))
                                             .map(FxView::getRootContent).toList());
        currentView.addListener(this::currentViewListener);

        borderPane.setCenter(centerView);
        root.getChildren().addAll(modalPane, borderPane);
        Platform.runLater(() -> currentViewListener(null, null, currentView.get()));
    }

    private void currentViewListener(ObservableValue<? extends String> observableValue, String oldView, String newView) {
        if (oldView != null) {
            Optional.ofNullable(views.get(oldView)).ifPresent(v -> v.getRootContent().setVisible(false));
        }
        Optional.ofNullable(views.get(newView)).ifPresent(v -> v.getRootContent().setVisible(true));
    }

//    @EventListener(SceneReadyEvent.class)
//    public void sceneReady(SceneReadyEvent event) throws BackingStoreException {
//        log.info("READY, source: {}", event.getSource());
//        event.getScene().getStylesheets().addAll(Resources.resolve("/styles/index.css"));
//
////        Resources.getPreferences().put("FOO", "BAR");
////        Resources.getPreferences().flush();
////        Resources.printPreferences(Resources.getPreferences(), "");
//        if (Boolean.getBoolean("SCENIC")) {
//            ScenicView.show(event.getScene());
//        }
////        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
////            log.info("call: {}", element);
////        }
//    }

    @Override
    public StackPane getRootContent() {
        return root;
    }

    public void showErrorToUser(Thread thread, Throwable throwable) {
        log.error("An unexpected error occurred in thread: {}, ", thread, throwable);
        Throwable       t   = Error.findOwnedException(throwable);
        ExceptionDialog dlg = new ExceptionDialog(t);
        dlg.setTitle("Unexpected error"); // I18N:

        HBox h = new HBox();
        h.setPadding(new Insets(15, 15, 5, 15));
        h.setAlignment(Pos.CENTER_RIGHT);
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(true);
        h.getChildren().addAll(new Label("Would you like to report this issue ?"), checkBox); // I18N:
        dlg.getDialogPane().setHeader(h);
        Nodes.show(dlg, root.getScene())
             .filter(bt -> bt == ButtonType.OK)
             .filter(_ -> checkBox.isSelected())
             .ifPresent(_ -> ghClient.reportIssue(throwable));
//        DialogPane dialogPane = new DialogPane();
//
//        // --- expandable content
//        StringWriter sw = new StringWriter();
//        PrintWriter  pw = new PrintWriter(sw);
//        t.printStackTrace(pw);
//        String exceptionText = sw.toString();
//
////        Label label = new Label( localize(getString("exception.dlg.label"))); //$NON-NLS-1$
//
//        TextArea textArea = new TextArea(exceptionText);
//        textArea.setEditable(false);
//        textArea.setWrapText(true);
//
//        textArea.setMaxWidth(Double.MAX_VALUE);
//        textArea.setMaxHeight(Double.MAX_VALUE);
//        GridPane.setVgrow(textArea, Priority.ALWAYS);
//        GridPane.setHgrow(textArea, Priority.ALWAYS);
//        GridPane root = new GridPane();
//        root.setMaxWidth(Double.MAX_VALUE);
//        root.add(h, 0, 0, 2, 1);
//        root.add(textArea, 0, 1);
//
//        dialogPane.setExpandableContent(root);
//        var dialog = MpDialog.builder()
////                             .width(400)
////                             .header(i18N.labelForKey("confirmation.deleteFiles.title"))
//                             .header(new Label("An unexpected error occurred"))
//                             .subHeader(new Label(throwable.getLocalizedMessage()))
////                             .body(dialogPane)
//                             .build();
//
//        dialog.show(modalPane, exitMode -> {
//            if (exitMode == MpDialog.EXIT_MODE.OK) {
//                ghClient.reportIssue(throwable);
//            }
//        });
    }
}
