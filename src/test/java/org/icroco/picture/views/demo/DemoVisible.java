package org.icroco.picture.views.demo;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * A little demo showing how the "visible" and "managed" property can be used
 * to make a node disappear in such a way that the layout pane / container can
 * reclaim the space previously occupied by the now invisible node.
 */
public class DemoVisible extends Application {

        @Override
public void start(Stage primaryStage) {

Label label1 = createLabel("Label 1");
Label label2 = createLabel("Label 2");
Label label3 = createLabel("Label 3");
Label label4 = createLabel("Label 4");

CheckBox visibleBox = new CheckBox("Visible");
CheckBox managedBox = new CheckBox("Managed");

visibleBox.setSelected(true);
managedBox.setSelected(true);

label2.visibleProperty().bind(visibleBox.selectedProperty());
label2.managedProperty().bind(managedBox.selectedProperty());

HBox hBox1 = new HBox(10, new Label("Label 2 settings:"), visibleBox, managedBox);

HBox hBox2 = new HBox(10, label1, label2, label3, label4);
hBox2.setStyle("-fx-background-color: lightgray; -fx-padding: 20");

VBox vBox = new VBox(20, hBox1, hBox2);
vBox.setFillWidth(false);
vBox.setPadding(new Insets(20));

primaryStage.setTitle("Visible / Managed Demo");
primaryStage.setScene(new Scene(vBox));
primaryStage.sizeToScene();
primaryStage.centerOnScreen();
primaryStage.show();
}

        private Label createLabel(String text) {
Label label = new Label(text);
label.setStyle("-fx-background-color: orange; -fx-background-radius: 4; -fx-padding: 20;");
label.setPrefSize(200, 200);
return label;
}

        public static void main(String[] args) {
launch();
}
}