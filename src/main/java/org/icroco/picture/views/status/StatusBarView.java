package org.icroco.picture.views.status;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.PopOver;
import org.icroco.picture.persistence.MediaFileRepository;
import org.icroco.picture.views.task.TaskView;
import org.icroco.picture.views.util.FxView;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;


@Slf4j
@RequiredArgsConstructor
@Component
public class StatusBarView implements FxView<HBox> {
    private final MediaFileRepository mediaFileRepository;
    private final TaskView            taskView;
    private final TaskScheduler       scheduler;

    private final HBox        root          = new HBox();
    private final Tooltip     tooltip       = new Tooltip("");
    private final Label       progressLabel = new Label();
    private final ProgressBar progressBar   = new ProgressBar(0.5);
    private       ProgressBar memoryStatus;
    private       PopOver     popOver;

    @PostConstruct
    protected void initializedOnce() {
        root.getStyleClass().add("v-status");

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(10, 10, 10, 10));
        memoryStatus = new ProgressBar(0);
        memoryStatus.setPrefWidth(100);

        progressBar.setMinWidth(200);
        progressBar.setMaxWidth(200);
        progressBar.getStyleClass().add(Styles.SMALL);
        progressLabel.setMinWidth(250);
        SimpleListProperty<Task<?>> list = new SimpleListProperty<>(taskView.getTasks());
        progressBar.progressProperty().bind(Bindings.valueAt(list, 0).flatMap(Task::progressProperty));
        taskView.getTasks().addListener(getTaskListChangeListener());
        initPopOver(progressBar);
        Label memory = new Label("Memory ");
        memory.setTooltip(tooltip);
        memoryStatus.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 2) {
                Runtime.getRuntime().gc();
                log.info("Memory cleaned");
            }
        });

        tooltip.setShowDelay(Duration.seconds(4));

        memoryStatus.setTooltip(tooltip);
        root.getChildren().addAll(memory, memoryStatus, new Spacer(), progressLabel, progressBar);
        scheduler.scheduleAtFixedRate(this::updateMemory, java.time.Duration.of(5, ChronoUnit.SECONDS));
    }

    private void updateMemory() {
        Runtime      jvmRuntime             = Runtime.getRuntime();
        long         totalMemory            = jvmRuntime.totalMemory();
        long         maxMemory              = jvmRuntime.maxMemory();
        long         usedMemory             = totalMemory - jvmRuntime.freeMemory();
        long         totalMemoryInMebibytes = totalMemory / (1024 * 1024);
        long         maxMemoryInMebibytes   = maxMemory / (1024 * 1024);
        long         usedMemoryInMebibytes  = usedMemory / (1024 * 1024);
        double       usedPct                = new BigDecimal((double) usedMemory / (double) totalMemory).setScale(2, RoundingMode.HALF_UP).doubleValue();
        final String textToShow             = usedMemoryInMebibytes + "MiB of " + totalMemoryInMebibytes + "MiB";
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

    private PopOver createPopOver(Node node) {
        PopOver popOver = new PopOver(node);
        popOver.setDetachable(false);
        popOver.setMinSize(600, 400);
        popOver.setDetached(false);
        popOver.setTitle("Tasks");
        popOver.setHeaderAlwaysVisible(true);
        popOver.setAutoHide(true);
        popOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_RIGHT);
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
}
