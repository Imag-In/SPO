package org.icroco.picture.ui.util;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@UtilityClass
@Slf4j
public class Nodes {
    public static void hideNodeAfterTime(Node node, int timeInSec, boolean showInScene) {
        Platform.runLater(() -> {
            PauseTransition wait = new PauseTransition(Duration.seconds(timeInSec));
            wait.setOnFinished((e) -> {
                node.setVisible(false);
                node.setManaged(showInScene);
            });
            wait.play();
        });
    }

    public static <T> void showDialog(Dialog<T> dlg, Consumer<T> consumer) {
        dlg.initOwner(Window.getWindows().stream().findFirst().orElseThrow());
        dlg.showAndWait().ifPresent(consumer);
    }

    public static <T> void showDialog(Dialog<T> dlg) {
        showDialog(dlg, Constant.emptyConsumer());
    }


    public void setStageSizeAndPos(Stage stage, double x, double y, double width, double height) {
        final List<Screen> screens = new ArrayList<>(Screen.getScreens());

        screens.sort(Comparator.comparingDouble(o -> o.getVisualBounds().getMinX()));
        for (final Screen screen : screens) {
            log.debug("Find screen, bounds: {}, visualBounds: {}", screen.getBounds(), screen.getVisualBounds());
        }
        log.debug("Bounds x: {}, y: {}, w: {}, h: {}", x, y, width, height);
        final Screen lastScreen  = screens.get(screens.size() - 1);
        final Screen firstScreen = screens.get(0);

        if (lastScreen.getVisualBounds().getMaxX() < x + width) {
            x = Math.max(firstScreen.getVisualBounds().getMinX(),
                         x - ((x + width) - lastScreen.getVisualBounds().getMaxX()));
            width = Math.min(width, lastScreen.getVisualBounds().getMaxX());
        }
        if (lastScreen.getVisualBounds().getMaxY() < y + height) {
            y = Math.max(firstScreen.getVisualBounds().getMinY(),
                         y - ((y + height) - lastScreen.getVisualBounds().getMaxY()));
            height = Math.min(height, lastScreen.getVisualBounds().getMaxX());
        }
        if (x < firstScreen.getVisualBounds().getMinX()) {
            x = firstScreen.getVisualBounds().getMinX();
            width = Math.min(width, firstScreen.getVisualBounds().getMaxX());
        }
        if (y < firstScreen.getVisualBounds().getMinY()) {
            y = firstScreen.getVisualBounds().getMinY();
            height = Math.min(height, firstScreen.getVisualBounds().getMaxY());
        }
        log.debug("Bounds adjusted x: {}, y: {}, w: {}, h: {}", x, y, width, height);
        stage.setX(x);
        stage.setY(y);
        stage.setWidth(width);
        stage.setHeight(height);

        stage.setOnCloseRequest(event -> {
            // save user pref.
        });

    }

    public static <T> Optional<T> getFirstParent(Node node, Class<T> type) {
        Parent parent = node.getParent();

        while(parent != null) {
            if (type.isInstance(parent)) {
                return Optional.of(type.cast(parent));
            }
            parent = parent.getParent();
        }

        return Optional.empty();
    }

    static class ProgressBeanPostProcessor implements BeanPostProcessor {
        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
//            return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
            return bean;
        }
    }

    public static void addRightGraphic(TitledPane tp, Label... labels) {
        HBox contentPane = new HBox();
        contentPane.setAlignment(Pos.CENTER);

        // Set padding on the left side to avoid overlapping the TitlePane's expand arrow
        // We will also pad the right side
        contentPane.setPadding(new Insets(0, 10, 0, 5));

        // Now, since the TitlePane's graphic node generally has a fixed size, we need to bind our
        // content pane's width to match the width of the TitledPane. This will account for resizing as well
        contentPane.minWidthProperty().bind(tp.widthProperty().subtract(labels.length * 25));
        tp.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        // Create a Region to act as a separator for the title and button
        HBox region = new HBox();
        region.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(region, Priority.ALWAYS);

        for (Label l : labels) {
            l.prefHeightProperty().bind(tp.prefHeightProperty());
            l.setVisible(false);
        }
        // Add our nodes to the contentPane
        contentPane.getChildren().addAll(new Label(tp.getText()), region);

        contentPane.setOnMouseEntered(event -> {
            for (Label l : labels) {
                l.setVisible(true);
            }
        });
        contentPane.setOnMouseExited(event -> {
            for (Label l : labels) {
                l.setVisible(false);
            }
        });
        contentPane.getChildren().addAll(labels);

        tp.setGraphic(contentPane);
    }
}
