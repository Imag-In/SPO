package org.icroco.picture.views.util;

import atlantafx.base.controls.Spacer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.util.Constant;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

@UtilityClass
@Slf4j
public class Nodes {

    public static void applyAndConsume(KeyEvent event, Consumer<KeyEvent> eventConsumer) {
        eventConsumer.accept(event);
        event.consume();
    }

    public static <R> Optional<R> show(Dialog<R> alert, Scene scene) {
        alert.initOwner(scene.getWindow());

        // copy customized styles, like changed accent color etc
        try {
            for (var pc : scene.getRoot().getPseudoClassStates()) {
                alert.getDialogPane().pseudoClassStateChanged(pc, true);
            }
            alert.getDialogPane().getStylesheets().addAll(scene.getRoot().getStylesheets());
        } catch (Exception ignored) {
            // yes, ignored
        }
        return alert.showAndWait();
    }

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
        log.info("Bounds x: {}, y: {}, w: {}, h: {}", x, y, width, height);
        final Screen lastScreen  = screens.get(screens.size() - 1);
        final Screen firstScreen = screens.get(0);
        var          conf        = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
        Window       window      = stage.getScene().getWindow();
        var
                s =
                Screen.getScreensForRectangle(new Rectangle2D(window.getX(), window.getY(), window.getWidth(), window.getHeight()));
        if (lastScreen.getVisualBounds().getMaxX() < x + width) {
            x = Math.max(firstScreen.getVisualBounds().getMinX(), x - ((x + width) - lastScreen.getVisualBounds().getMaxX()));
            width = Math.min(width, lastScreen.getVisualBounds().getMaxX());
        }
        if (lastScreen.getVisualBounds().getMaxY() < y + height) {
            y = Math.max(firstScreen.getVisualBounds().getMinY(), y - ((y + height) - lastScreen.getVisualBounds().getMaxY()));
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
        log.info("Bounds adjusted x: {}, y: {}, w: {}, h: {}", x, y, width, height);
        stage.setX(x);
        stage.setY(y);
        stage.setWidth(width);
        stage.setHeight(height);
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

    public <T> Optional<TreeItem<T>> searchTreeItem(TreeItem<T> treeNode, T value) {
        if (value == null || treeNode == null) {
            return Optional.empty();
        }

        if (treeNode.getValue().equals(value)) {
            return Optional.of(treeNode);
        }

        // Loop through each child node.
        for (TreeItem<T> node : treeNode.getChildren()) {
            // If the current node has children then check them.
            var found = searchTreeItem(node, value);
            if (found.isPresent()) {
                return found;
            }
        }

        return Optional.empty();
    }

    public <T> Optional<TreeItem<T>> searchTreeItemByPredicate(TreeItem<T> treeItem, Predicate<T> predicate) {
        if (treeItem == null || predicate == null) {
            return Optional.empty();
        }

        if (predicate.test(treeItem.getValue())) {
            return Optional.of(treeItem);
        }

        // Loop through each child node.
        for (TreeItem<T> node : treeItem.getChildren()) {
            // If the current node has children then check them.
            var found = searchTreeItemByPredicate(node, predicate);
            if (found.isPresent()) {
                return found;
            }
        }

        return Optional.empty();
    }

//    @Nullable
//    private TreeItem<CollectionView.CollectionNode> searchTreeItem(@NonNull TreeItem<CollectionView.CollectionNode> item, @Nullable Path path) {
//        if (path == null) {
//            return null;
//        }
//
//        if (item.getValue().path().equals(path)) {
//            return item; // hit!
//        }
//
//        // continue on the children:
//        TreeItem<CollectionView.CollectionNode> result = null;
//        for (TreeItem<CollectionView.CollectionNode> child : item.getChildren()) {
//            result = searchTreeItem(child, path);
//            if (result != null) {
//                return result; // hit!
//            }
//        }
//        //no hit:
//        return null;
//    }

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

    public static void toggleVisibility(Node node, boolean on) {
        node.setVisible(on);
        node.setManaged(on);
    }

    public static void setAnchors(Node node, Insets insets) {
        if (insets.getTop() >= 0) {
            AnchorPane.setTopAnchor(node, insets.getTop());
        }
        if (insets.getRight() >= 0) {
            AnchorPane.setRightAnchor(node, insets.getRight());
        }
        if (insets.getBottom() >= 0) {
            AnchorPane.setBottomAnchor(node, insets.getBottom());
        }
        if (insets.getLeft() >= 0) {
            AnchorPane.setLeftAnchor(node, insets.getLeft());
        }
    }

    public static void setScrollConstraints(ScrollPane scrollPane,
                                            ScrollPane.ScrollBarPolicy vbarPolicy, boolean fitHeight,
                                            ScrollPane.ScrollBarPolicy hbarPolicy, boolean fitWidth) {
        scrollPane.setVbarPolicy(vbarPolicy);
        scrollPane.setFitToHeight(fitHeight);
        scrollPane.setHbarPolicy(hbarPolicy);
        scrollPane.setFitToWidth(fitWidth);
    }

    public static boolean isDoubleClick(MouseEvent e) {
        return e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2;
    }

    public static <T> T getChildByIndex(Parent parent, int index, Class<T> contentType) {
        List<Node> children = parent.getChildrenUnmodifiable();
        if (index < 0 || index >= children.size()) {
            return null;
        }
        Node node = children.get(index);
        return contentType.isInstance(node) ? contentType.cast(node) : null;
    }

    public static boolean isDescendant(Node ancestor, Node descendant) {
        if (ancestor == null) {
            return true;
        }

        while (descendant != null) {
            if (descendant == ancestor) {
                return true;
            }
            descendant = descendant.getParent();
        }
        return false;
    }

    public Node createDecoratorNode(Color color) {
        javafx.scene.shape.Rectangle d = new Rectangle(7, 7);
        d.setFill(color);
        return d;
    }
}
