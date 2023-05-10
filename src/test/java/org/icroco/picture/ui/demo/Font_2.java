package org.icroco.picture.ui.demo;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class Font_2 extends Application {

    // launch the application
    public void start(Stage stage) {

        try {

// set title for the stage
            stage.setTitle("Font");

// create TextFlow
            TextFlow text_flow = new TextFlow();

// create text
            Text text_1 = new Text("GeeksforGeeks\n0 lL 1 O ~ + -");

// set the text color
            text_1.setFill(Color.GREEN);

// create a font
            Font font = Font.font(Font.getFontNames().get(0),
                                  FontWeight.EXTRA_BOLD, 20);

// font weight names
            String weight[] = { "BLACK", "BOLD",
                                "EXTRA_BOLD",
                                "EXTRA_LIGHT",
                                "LIGHT",
                                "MEDIUM",
                                "NORMAL",
                                "SEMI_BOLD",
                                "THIN" };

// Create a combo box
            ComboBox combo_box =
                    new ComboBox(FXCollections.observableArrayList(weight));

// Create a combo box
            ComboBox combo_box1 =
                    new ComboBox(FXCollections.observableArrayList(Font.getFontNames()));

// Create action event
            EventHandler<ActionEvent> event =
                    new EventHandler<ActionEvent>() {

                        public void handle(ActionEvent e) {

// set font of the text
                            text_1.setFont(Font.font((String) combo_box1.getValue(),
                                                     FontWeight.valueOf((String) combo_box.getValue()), 20));
                        }
                    };

// Create action event
            EventHandler<ActionEvent> event1 =
                    new EventHandler<ActionEvent>() {

                        public void handle(ActionEvent e) {

// set font of the text
                            text_1.setFont(Font.font((String) combo_box1.getValue(),
                                                     FontWeight.valueOf((String) combo_box.getValue()), 20));
                        }
                    };

// Set on action
            combo_box.setOnAction(event);
            combo_box1.setOnAction(event1);

// set font of the text
            text_1.setFont(font);

// set text
            text_flow.getChildren().add(text_1);

// set line spacing
            text_flow.setLineSpacing(20.0f);

// create a HBox
            HBox hbox = new HBox(combo_box, combo_box1);

// create VBox
            VBox vbox = new VBox(hbox, text_flow);

// set spacing
            vbox.setSpacing(30.0);

// set alignment of vbox
            vbox.setAlignment(Pos.CENTER);

// create a scene
            Scene scene = new Scene(vbox, 400, 300);

// set the scene
            stage.setScene(scene);

            stage.show();
        }

        catch (Exception e) {

            System.out.println(e.getMessage());
        }
    }

    // Main Method
    public static void main(String args[]) {

// launch the application
        launch(args);
    }
}