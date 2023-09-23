package org.icroco.picture.views.grid;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;

import java.util.ArrayList;
import java.util.List;

public class Main extends Application {
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        List<String> itemsFoo = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemsFoo.add("foo-" + i);
        }

        List<String> itemsBar = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            itemsBar.add("bar-" + i);
        }

        GridView<String> grid = new GridView<>();
        grid.setCellFactory(param -> new SimpleCell());

        ObservableList<String> items = FXCollections.observableArrayList();
        items.setAll(itemsFoo);
        grid.setItems(items);

        Button btnFoo = new Button("foo");
        btnFoo.setOnAction(event -> items.setAll(itemsFoo));

        Button btnBar = new Button("bar");
        btnBar.setOnAction(event -> items.setAll(itemsBar));

        VBox root = new VBox(8);
        root.setPrefSize(400, 355);
        root.setPadding(new Insets(8));
        root.getChildren().addAll(btnFoo, btnBar, grid);
        VBox.setVgrow(grid, Priority.ALWAYS);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private static class SimpleCell extends GridCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setStyle("");
//                System.out.println("empty idx: "+this.getIndex());
            } else {
                System.out.println("updateItem: " + item + " idx: " + this.getIndex());
                setText(item);
                if (item.startsWith("f")) {
                    setStyle("-fx-background-color: red");
                } else {
                    setStyle("-fx-background-color: green");
                }
            }
        }
    }
}