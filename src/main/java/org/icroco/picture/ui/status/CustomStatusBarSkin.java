package org.icroco.picture.ui.status;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.controlsfx.control.StatusBar;

public class CustomStatusBarSkin extends SkinBase<StatusBar> {
    private final HBox        leftBox;
    private final HBox        rightBox;
    private final Label       label;
    private final ProgressBar progressBar;

    public CustomStatusBarSkin(StatusBar statusBar) {
        super(statusBar);
        final BooleanBinding notZeroProgressProperty = Bindings.notEqual(0, statusBar.progressProperty());

        GridPane gridPane = new GridPane();

        leftBox = new HBox();
        leftBox.getStyleClass().add("left-items"); //$NON-NLS-1$
        leftBox.setAlignment(Pos.CENTER_LEFT);

        rightBox = new HBox();
        rightBox.getStyleClass().add("right-items"); //$NON-NLS-1$
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        progressBar = new ProgressBar();
        progressBar.progressProperty().bind(statusBar.progressProperty());
        progressBar.visibleProperty().bind(notZeroProgressProperty);
        progressBar.managedProperty().bind(notZeroProgressProperty);

        label = new Label();
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        label.textProperty().bind(statusBar.textProperty());
        label.visibleProperty().bind(notZeroProgressProperty);
        label.managedProperty().bind(notZeroProgressProperty);
        label.graphicProperty().bind(statusBar.graphicProperty());
        label.styleProperty().bind(getSkinnable().styleProperty());
        label.setAlignment(Pos.CENTER_RIGHT);
        label.setPadding(new Insets(0, 10, 0, 0));
        label.getStyleClass().add("status-label"); //$NON-NLS-1$

        leftBox.getChildren().setAll(getSkinnable().getLeftItems());
        rightBox.getChildren().setAll(getSkinnable().getRightItems());

        statusBar.getLeftItems().addListener(
                (Observable evt) -> leftBox.getChildren().setAll(
                        getSkinnable().getLeftItems()));

        statusBar.getRightItems().addListener(
                (Observable evt) -> rightBox.getChildren().setAll(
                        getSkinnable().getRightItems()));

        GridPane.setFillHeight(leftBox, true);
        GridPane.setFillHeight(rightBox, true);
        GridPane.setFillHeight(label, true);
        GridPane.setFillHeight(progressBar, true);

        GridPane.setVgrow(leftBox, Priority.ALWAYS);
        GridPane.setVgrow(rightBox, Priority.ALWAYS);
        GridPane.setVgrow(label, Priority.ALWAYS);
        GridPane.setVgrow(progressBar, Priority.ALWAYS);

        GridPane.setHgrow(label, Priority.ALWAYS);

        gridPane.add(leftBox, 0, 0);
        gridPane.add(rightBox, 1, 0);
        gridPane.add(label, 2, 0);
        gridPane.add(progressBar, 3, 0);

        getChildren().add(gridPane);
    }
}
