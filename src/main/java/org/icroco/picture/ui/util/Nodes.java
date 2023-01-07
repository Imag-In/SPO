package org.icroco.picture.ui.util;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Dialog;
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


    static class ProgressBeanPostProcessor implements BeanPostProcessor {
        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
//            return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
            return bean;
        }
    }
}
