package org.icroco.picture.views.repair;

import jakarta.annotation.PostConstruct;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.persistence.PersistenceService;
import org.icroco.picture.views.AbstractView;
import org.icroco.picture.views.CollectionManager;
import org.icroco.picture.views.ViewConfiguration;
import org.icroco.picture.views.task.TaskService;
import org.springframework.stereotype.Component;

import java.util.List;

import static javafx.scene.layout.Priority.ALWAYS;

@Slf4j
@Component
public class RepairView extends AbstractView<BorderPane> {
    public static final int SECOND_COL_PREF_WIDTH    = 600;
    static final        int PAGE_TRANSITION_DURATION = 500; // ms

    private final TaskService        taskService;
    private final PersistenceService persistenceService;
    private final CollectionManager  collectionManager;
    private final BorderPane         root     = new BorderPane();
    private final RepairModel        model    = new RepairModel();
    private final StackPane toolPane = new StackPane();


    public RepairView(TaskService taskService,
                      PersistenceService persistenceService,
                      CollectionManager collectionManager,
                      List<RepairTool> tools) {
        this.taskService = taskService;
        this.persistenceService = persistenceService;
        this.collectionManager = collectionManager;
        model.createTree(tools);
    }

    @PostConstruct
    private void postConstruct() {
        root.setId(ViewConfiguration.V_REPAIR);
        root.getStyleClass().add(ViewConfiguration.V_REPAIR);
        root.setLeft(createSideBar());
        root.setCenter(toolPane);
        toolPane.setPadding(new Insets(10, 10, 10, 10));
        HBox.setHgrow(toolPane, ALWAYS);
        initListeners();
    }

    private Node createSideBar() {
        return new SideBar(model);
    }

    private void initListeners() {
        model.selectedPageProperty().addListener((_, _, val) -> {
            if (val != null) {
                loadTool(val);
            }
        });

//        model.currentSubLayerProperty().addListener((obs, old, val) -> {
//            switch (val) {
//                case PAGE -> hideSourceCode();
//                case SOURCE_CODE -> showSourceCode();
//            }
//        });
    }

    private void loadTool(RepairTool nextTool) {
        toolPane.getChildren().clear();
        toolPane.getChildren().add(nextTool.getView());
//        if (nextTool.getView() instanceof Pane pane) {
//            pane.toFront();
//        }
//        final var prevTool = toolPane.getChildren().stream()
//                                     .filter(RepairTool.class::isInstance)
//                                     .map(RepairTool.class::cast)
//                                     .findFirst()
//                                     .orElse(null);
//
//        if (root.getScene() == null || prevTool == null) {
//            toolPane.getChildren().add(nextTool.getView());
//            return;
//        }
//
//        toolPane.getChildren().add(nextTool.getView());
//        toolPane.getChildren().remove(prevTool.getView());
//        var transition = new FadeTransition(Duration.millis(PAGE_TRANSITION_DURATION), nextTool.getView());
//        transition.setFromValue(0.0);
//        transition.setToValue(1.0);
//        transition.setOnFinished(t -> {
//            if (nextTool.getView() instanceof Pane nextPane) {
//                nextPane.toFront();
//            }
//        });
//        transition.play();
    }

    @Override
    public BorderPane getRootContent() {
        return root;
    }

}
