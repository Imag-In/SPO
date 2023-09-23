package org.icroco.picture.views.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class DrawText extends Application {
    public static void main(String[] args) {
        Application.launch(args);//from ww w  .j a va2s .c o m
    }

    @Override
    public void start(Stage stage) {
        // Create a canvas
        Canvas canvas = new Canvas(300, 100);

        // Get the graphics context of the canvas
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.TRANSPARENT);
        // Draw text
        gc.strokeText("Hello Canvas", 150, 20);

        Pane root = new Pane();
        root.getChildren().add(canvas);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Drawing on a Canvas");
        stage.show();
    }
}