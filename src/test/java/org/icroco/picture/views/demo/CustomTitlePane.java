package org.icroco.picture.views.demo;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class CustomTitlePane {
//    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(new StackPane(), 300, 250);

        primaryStage.setScene(scene);

        primaryStage.setOnShown(e -> {
            TitledPane title = new TitledPane("Title",
                    new StackPane(new Label("Graphic to the Right")));

            ImageView imageView = new ImageView(new Image(getClass().getResource("unlock24.png").toExternalForm()));

            title.setGraphic(imageView);
            title.setContentDisplay(ContentDisplay.RIGHT);

            scene.setRoot(title);

            // apply css and force layout of nodes
            title.applyCss();
            title.layout();

            // title region
            Node titleRegion=title.lookup(".title");
            // padding
            Insets padding=((StackPane)titleRegion).getPadding();
            // image width
            double graphicWidth=imageView.getLayoutBounds().getWidth();
            // arrow
            double arrowWidth=titleRegion.lookup(".arrow-button").getLayoutBounds().getWidth();
            // text
            double labelWidth=titleRegion.lookup(".text").getLayoutBounds().getWidth();

            double nodesWidth = graphicWidth+padding.getLeft()+padding.getRight()+arrowWidth+labelWidth;

            title.graphicTextGapProperty().bind(title.widthProperty().subtract(nodesWidth));
        });

        primaryStage.show();

    }
}
