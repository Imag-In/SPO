package org.icroco.picture.views.demo;


import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class FadeExample extends Application {

    private BorderPane root;
    private Pane       headerContainer;
    private Screen     loginScreen;
    private Screen     mainScreen;
    private Duration   fadeTime = Duration.seconds(1);

    private final ObjectProperty<Screen> currentScreen = new SimpleObjectProperty<>();

    @Override
    public void start(Stage primaryStage) {
        root = new BorderPane();

        headerContainer = new StackPane();
        headerContainer.getStyleClass().add("header-container");

        currentScreen.addListener((obs, previousScreen, newScreen) -> switchScreens(previousScreen, newScreen));

        loginScreen = new Screen(createHeader("Login", "Please provide your username and password"), createLoginContent());
        mainScreen = new Screen(createHeader("Application", "Welcome to the application. This is a test."), createMainContent());

        root.setTop(headerContainer);

        Scene scene = new Scene(root, 600, 600);

        scene.getStylesheets().add("style.css");

        primaryStage.setScene(scene);
        primaryStage.show();

        currentScreen.set(loginScreen);

    }

    private void switchScreens(Screen previous, Screen next) {


        // Animation for switching screens fades out old screen (if there is one),
        // then fades new screen (if there is one) in:
        SequentialTransition sceneSwitch = new SequentialTransition();


        if (previous != null) {
            // fade out simultaneously fades out header and content:
            ParallelTransition fadeOut = new ParallelTransition(
                    createFade(1, 0, previous.getHeader()),
                    createFade(1, 0, previous.getContent())
            );

            // when fade out is complete, replace content with new content:
            fadeOut.setOnFinished(e -> {
                headerContainer.getChildren().setAll(next.getHeader());
                root.setCenter(next.getContent());
            });

            sceneSwitch.getChildren().add(fadeOut);
        }
        if (next != null) {
            // fade in simultaneously fades in header and content:
            ParallelTransition fadeIn = new ParallelTransition(
                    createFade(0, 1, next.getHeader()),
                    createFade(0, 1, next.getContent())
            );

            // when fade in starts, replace content with new content:
            fadeIn.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                if (newStatus == Animation.Status.RUNNING) {
                    headerContainer.getChildren().setAll(next.getHeader());
                    root.setCenter(next.getContent());
                }
            });

            sceneSwitch.getChildren().add(fadeIn);
        }

        // play fade out, then fade in:
        sceneSwitch.play();
    }

    private FadeTransition createFade(double start, double end, Node node) {
        FadeTransition fade = new FadeTransition(fadeTime, node);
        fade.setFromValue(start);
        fade.setToValue(end);
        return fade;
    }

    private Node createLoginContent() {
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        ColumnConstraints leftCol = new ColumnConstraints();
        leftCol.setHgrow(Priority.NEVER);
        leftCol.setHalignment(HPos.RIGHT);
        ColumnConstraints rightCol = new ColumnConstraints();
        rightCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(leftCol, rightCol);

        grid.add(new Label("Username:"), 0, 0);
        grid.add(new TextField(), 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(new PasswordField(), 1, 1);

        Button loginButton = new Button("Login");
        loginButton.setOnAction(e -> currentScreen.set(mainScreen));
        GridPane.setHalignment(loginButton, HPos.CENTER);
        grid.add(loginButton, 0, 2, 2, 1);

        return grid;
    }

    private Node createMainContent() {
        Label  label  = new Label("Here is the application");
        Button logout = new Button("Logout");
        logout.setOnAction(e -> currentScreen.set(loginScreen));
        return new VBox(label, logout);
    }

    private Pane createHeader(String text, String description) {
        Label title = new Label(text);
        title.getStyleClass().add("title");

        Label descriptionLabel = new Label(description);

        return new VBox(title, descriptionLabel);
    }

    private static class Screen {
        private final Node header;
        private final Node content;

        public Screen(Node header, Node content) {
            this.header = header;
            this.content = content;

            header.getStyleClass().add("screen-header");
            content.getStyleClass().add("screen-content");
        }

        public Node getHeader() {
            return header;
        }

        public Node getContent() {
            return content;
        }

    }

    public static void main(String[] args) {
        launch(args);
    }
}
