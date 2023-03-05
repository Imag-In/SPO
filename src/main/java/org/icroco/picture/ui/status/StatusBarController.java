package org.icroco.picture.ui.status;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.StatusBar;
import org.icroco.javafx.FxInitOnce;
import org.icroco.javafx.FxViewBinding;
import org.icroco.picture.ui.persistence.MediaFileRepository;
import org.icroco.picture.ui.task.TaskView;
import org.icroco.picture.ui.util.Nodes;
import org.icroco.picture.ui.util.widget.ProgressIndicatorBar;
import org.springframework.scheduling.TaskScheduler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;


@Slf4j
@FxViewBinding(id = "status_bar", fxmlLocation = "status.fxml")
@RequiredArgsConstructor
public class StatusBarController extends FxInitOnce {
    private final MediaFileRepository mediaFileRepository;
    private final TaskView            taskController;
    private final TaskScheduler       scheduler;

    @FXML
    @Getter
    private StatusBar container;
    private PopOver   popOver;

    private ProgressBar          memoryStatus;
    private ProgressIndicatorBar indicator = new ProgressIndicatorBar("");

    @Override
    protected void initializedOnce() {
        container.textProperty().set("");
        memoryStatus = new ProgressBar(0);
        memoryStatus.setPrefWidth(100);
        container.setSkin(new CustomStatusBarSkin(container));
        SimpleListProperty<Task<?>> list = new SimpleListProperty<>(taskController.controller().getTasks());
        container.progressProperty().bind(Bindings.valueAt(list, 0).flatMap(Task::progressProperty));
        taskController.controller().getTasks().addListener(getTaskListChangeListener());
        initPopOver(taskController.scene().getRoot());
        container.getLeftItems().add(new Label("Memory "));
        container.getLeftItems().add(memoryStatus);
        memoryStatus.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 2) {
                Runtime.getRuntime().gc();
                log.info("Memory cleaned");
            }
        });
        memoryStatus.setTooltip(new Tooltip("Cool !"));
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
            memoryStatus.setTooltip(new Tooltip(toolTipToShow));
        });
    }

    private ListChangeListener<Task<?>> getTaskListChangeListener() {
        return c -> {
            c.next();
            final var textProperty = container.textProperty();
            if (c.getList().isEmpty()) {
                textProperty.unbind();
                textProperty.set("");
                popOver.hide();
            } else if (c.getList().size() == 1) {
                textProperty.unbind();
                textProperty.bind(c.getList().get(0).titleProperty());
            } else if (c.getList().size() == 2) {
//                if (c.wasAdded()) {
                textProperty.unbind();
                textProperty.bind(c.getList().get(0).titleProperty());
//                    textProperty.bind(Bindings.size(c.getList()).map(number -> c.getList().get(0).getTitle()+" ("+number + " tasks left)"));
            }
//            }
        };
    }

    public void initPopOver(Node node) {
        popOver = createPopOver(node);
        Nodes.getFirstChild(((CustomStatusBarSkin) container.getSkin()).getChildren().get(0), ProgressBar.class)
             .ifPresent(p -> p.setOnMouseClicked(this::showPopup));
        Nodes.getFirstChild(((CustomStatusBarSkin) container.getSkin()).getChildren().get(0), Label.class)
             .ifPresent(p -> p.setOnMouseClicked(this::showPopup));
    }

    private void showPopup(MouseEvent event) {
        if (popOver.isShowing()) {
            popOver.hide(Duration.ZERO);
        } else if (event.getClickCount() >= 1) {
            var targetX = event.getScreenX();
            var targetY = event.getScreenY();
            popOver.show(container, targetX, targetY);
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
}
