package org.icroco.picture.views.util;

import atlantafx.base.controls.Spacer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
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

    public static Screen getScreen(Scene scene) {
        List<Screen> interScreens = Screen.getScreensForRectangle(scene.getWindow().getX(),
                                                                  scene.getWindow().getY(),
                                                                  scene.getWindow().getWidth(),
                                                                  scene.getWindow().getHeight());
        if (interScreens.size() == 0) {
            return Screen.getPrimary();
        }
        return interScreens.get(0);
    }

    public static <T> Optional<T> getFirstParent(Node node, Class<T> type) {
        Parent parent = node.getParent();

        while (parent != null) {
            if (type.isInstance(parent)) {
                return Optional.of(type.cast(parent));
            }
            parent = parent.getParent();
        }

        return Optional.empty();
    }

    public static <T> Optional<T> getFirstChild(Node node, Class<T> type) {
        if (type.isInstance(node)) {
            return Optional.of(type.cast(node));
        }
        if (node instanceof Parent parent) {
            var children = parent.getChildrenUnmodifiable();

            if (children.isEmpty()) {
                return Optional.empty();
            }

            return children.stream()
                           .filter(type::isInstance)
                           .map(type::cast)
                           .findFirst()
                           .or(() ->
                                       children.stream()
                                               .filter(Parent.class::isInstance)
                                               .map(Parent.class::cast)
                                               .flatMap(p -> p.getChildrenUnmodifiable().stream())
                                               .map(n -> getFirstChild(n, type))
                                               .filter(Optional::isPresent)
                                               .map(Optional::get)
                                               .findFirst()
                           );
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
        contentPane.setPadding(new Insets(0, 2, 0, 5));

        // Now, since the TitlePane's graphic node generally has a fixed size, we need to bind our
        // content pane's width to match the width of the TitledPane. This will account for resizing as well
        contentPane.minWidthProperty().bind(tp.widthProperty().subtract(labels.length * 40));
        tp.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        // Create a Region to act as a separator for the title and button
//        HBox region = new HBox();
//        region.setMaxWidth(Double.MAX_VALUE);
//        HBox.setHgrow(region, Priority.ALWAYS);

        for (Label l : labels) {
//            l.prefHeightProperty().bind(tp.prefHeightProperty());
            l.setVisible(false);
        }
        // Add our nodes to the contentPane
        contentPane.getChildren().addAll(new Label(tp.getText()), new Spacer());

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

    public static <T> TreeItem<T> getLast(TreeItem<T> item) {
        TreeItem<T> last = item.getChildren().stream().findFirst().orElse(null);

        if (last == null) {
            return item;
        }
        return getLast(last);
    }

    public static <T> TreeItem<T> getRoot(TreeItem<T> item) {
        if (item.getParent() == null) {
            return item;
        }

        return getRoot(item.getParent());
    }

    public static <T> TreeItem<T> removeLast(TreeItem<T> item) {
        TreeItem<T> last = getLast(item);

        if (last == item) {
            return null;
        }
        TreeItem<T> parent = last.getParent();
        parent.getChildren().remove(last);

        return last;
    }

    public static Node pick(Node node, double sceneX, double sceneY) {
        Point2D p = node.sceneToLocal(sceneX, sceneY, true /* rootScene */);

        // check if the given node has the point inside it, or else we drop out
        if (!node.contains(p)) {
            return null;
        }

        // at this point we know that _at least_ the given node is a valid
        // answer to the given point, so we will return that if we don't find
        // a better child option
        if (node instanceof Parent) {
            // we iterate through all children in reverse order, and stop when we find a match.
            // We do this as we know the elements at the end of the list have a higher
            // z-order, and are therefore the better match, compared to children that
            // might also intersect (but that would be underneath the element).
            Node       bestMatchingChild = null;
            List<Node> children          = ((Parent) node).getChildrenUnmodifiable();
            for (int i = children.size() - 1; i >= 0; i--) {
                Node child = children.get(i);
                p = child.sceneToLocal(sceneX, sceneY, true /* rootScene */);
                if (child.isVisible() && !child.isMouseTransparent() && child.contains(p)) {
                    bestMatchingChild = child;
                    break;
                }
            }

            if (bestMatchingChild != null) {
                return pick(bestMatchingChild, sceneX, sceneY);
            }
        }

        return node;
    }
}
