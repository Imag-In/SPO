package org.icroco.picture.views.status;

import atlantafx.base.controls.Popover;
import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import jakarta.annotation.PostConstruct;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.event.NewVersionEvent;
import org.icroco.picture.views.FxEventListener;
import org.icroco.picture.views.ViewConfiguration;
import org.icroco.picture.views.task.TaskView;
import org.icroco.picture.views.util.FxView;
import org.jooq.lambda.Unchecked;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.temporal.ChronoUnit;


@Slf4j
@RequiredArgsConstructor
@Component
public class StatusBarView implements FxView<HBox> {
    private final TaskView      taskView;
    private final TaskScheduler scheduler;

    private final HBox        root             = new HBox();
    private final Tooltip     tooltip          = new Tooltip("");
    private final Label       progressLabel    = new Label();
    //    private final Label       versionAvailable = new Label();
    private final Hyperlink   versionAvailable = new Hyperlink();

    FontIcon versionAvailableIcon = new FontIcon(Material2OutlinedMZ.NEW_RELEASES);
    private final ProgressBar progressBar      = new ProgressBar(0.5);
    private       ProgressBar memoryStatus;
    private       Popover     popOver;

    private URI downloadUrl;

    @PostConstruct
    protected void initializedOnce() {
        root.setId(ViewConfiguration.V_STATUSBAR);
        root.getStyleClass().add(ViewConfiguration.V_STATUSBAR);

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(10, 10, 10, 10));
        memoryStatus = new ProgressBar(0);
        memoryStatus.setPrefWidth(100);

        progressBar.setVisible(false);
        progressBar.setMinWidth(200);
        progressBar.setMaxWidth(200);
        progressBar.getStyleClass().add(Styles.SMALL);
        progressBar.setPadding(new Insets(0, 0, 0, 5));

        progressLabel.setMinWidth(250);
        progressBar.setVisible(false);
        SimpleListProperty<Task<?>> list = new SimpleListProperty<>(taskView.getTasks());
        progressBar.progressProperty().bind(Bindings.valueAt(list, 0).flatMap(Task::progressProperty));
        taskView.getTasks().addListener(getTaskListChangeListener());
        initPopOver(taskView.getRootContent());
        Label memory = new Label("Memory ");
        memory.setTooltip(tooltip);
        memoryStatus.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 2) {
                Runtime.getRuntime().gc();
                log.info("Memory cleaned");
            }
        });
        versionAvailable.setManaged(false);
        versionAvailable.setPadding(new Insets(0, 0, 0, 10));

        tooltip.setShowDelay(Duration.seconds(4));
        versionAvailable.getStyleClass().add(Styles.ACCENT);
        versionAvailableIcon.setManaged(false);
        versionAvailableIcon.getStyleClass().add(Styles.ACCENT);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem openRelease = new MenuItem("Release page"); // I18N:
        openRelease.setOnAction(this::goToReleaseUrl);

        MenuItem download = new MenuItem("Download"); // I18N:
        download.setOnAction(this::openRelease);
        download.setDisable(true);

        contextMenu.getItems().addAll(openRelease, download);
        versionAvailable.setOnAction(event -> contextMenu.show(versionAvailable, Side.TOP, 0, 0));

        memoryStatus.setTooltip(tooltip);
        root.getChildren().addAll(memory, memoryStatus, new Spacer(), progressLabel, progressBar, versionAvailable, versionAvailableIcon);
        scheduler.scheduleAtFixedRate(this::updateMemory, java.time.Duration.of(5, ChronoUnit.SECONDS));
    }

    private void updateMemory() {
        Runtime jvmRuntime             = Runtime.getRuntime();
        long    totalMemory            = jvmRuntime.totalMemory();
        long    maxMemory              = jvmRuntime.maxMemory();
        long    usedMemory             = totalMemory - jvmRuntime.freeMemory();
        long    totalMemoryInMebibytes = totalMemory / (1024 * 1024);
        long    maxMemoryInMebibytes   = maxMemory / (1024 * 1024);
        long    usedMemoryInMebibytes  = usedMemory / (1024 * 1024);
        double
                usedPct =
                new BigDecimal((double) usedMemory / (double) totalMemory).setScale(2, RoundingMode.HALF_UP).doubleValue();
        final String textToShow = usedMemoryInMebibytes + "MiB of " + totalMemoryInMebibytes + "MiB";
        final String toolTipToShow = "Heap size: " + usedMemoryInMebibytes + "MiB of total: " + totalMemoryInMebibytes + "MiB max: "
                                     + maxMemoryInMebibytes + "MiB";
        log.debug("{}, percentage {}", textToShow, usedPct);
        Platform.runLater(() -> {
            memoryStatus.setProgress(usedPct);
//            memoryStatus.(textToShow);
            tooltip.setText(toolTipToShow);
        });
    }

    private ListChangeListener<Task<?>> getTaskListChangeListener() {
        return c -> {
            c.next();
            final var textProperty = progressLabel.textProperty();
            if (c.getList().isEmpty()) {
                textProperty.unbind();
                textProperty.set("");
                progressLabel.setVisible(false);
                popOver.hide();
                progressBar.setVisible(false);
            } else {
                textProperty.unbind();
                textProperty.bind(c.getList().get(0).titleProperty());
                progressLabel.setVisible(true);
                progressBar.setVisible(true);
            }
//            else if (c.getList().size() == 2) {
////                if (c.wasAdded()) {
//                textProperty.unbind();
//                textProperty.bind(c.getList().get(0).titleProperty());
////                    textProperty.bind(Bindings.size(c.getList()).map(number -> c.getList().get(0).getTitle()+" ("+number + " tasks left)"));
//            }
//            }
        };
    }

    public void initPopOver(Node node) {
        popOver = createPopOver(node);
        progressLabel.setOnMouseClicked(this::showPopup);
        progressBar.setOnMouseClicked(this::showPopup);
    }

    private void showPopup(MouseEvent event) {
        if (popOver.isShowing()) {
            popOver.hide(Duration.ZERO);
        } else if (event.getClickCount() >= 1) {
            var targetX = event.getScreenX();
            var targetY = event.getScreenY();
            popOver.show(progressBar, targetX, targetY);
        }
    }

    private Popover createPopOver(Node node) {
        Popover popOver = new Popover();
//        popOver.skinProperty().set(new PopoverSkin(popOver));
        popOver.setContentNode(node);
        popOver.setDetachable(false);
        popOver.setMinSize(800, 400);
        popOver.setDetached(false);
        popOver.setTitle("Tasks");
        popOver.setHeaderAlwaysVisible(true);
        popOver.setAutoHide(true);
        popOver.setArrowLocation(Popover.ArrowLocation.BOTTOM_RIGHT);
//        popOver.arrowSizeProperty().bind(masterArrowSize);
//        popOver.arrowIndentProperty().bind(masterArrowIndent);
//        popOver.arrowLocationProperty().bind(masterArrowLocation);
//        popOver.cornerRadiusProperty().bind(masterCornerRadius);
//        popOver.headerAlwaysVisibleProperty().bind(masterHeaderAlwaysVisible);
        popOver.setAnimated(true);
        popOver.setCloseButtonEnabled(true);
//        popOver.closeButtonEnabledProperty().bind(closeButtonEnabled.selectedProperty());
        return popOver;
    }

    @Override
    public HBox getRootContent() {
        return root;
    }

    @FxEventListener
    public void newVersionAvailable(NewVersionEvent event) {
        versionAvailable.setText("V" + event.getVersion() + " available");
        versionAvailable.setManaged(true);
        versionAvailableIcon.setManaged(true);
        downloadUrl = event.getUrl();
        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(1), versionAvailableIcon);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.0);
        fadeTransition.setCycleCount(100);
        fadeTransition.play();
    }

    final void goToReleaseUrl(ActionEvent value) {
        Unchecked.runnable(() -> Desktop.getDesktop().browse(downloadUrl)).run();
    }

    final void openRelease(ActionEvent value) {
        // TODO
    }
}
