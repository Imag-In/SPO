package org.icroco.picture.views.repair;

import jakarta.annotation.PostConstruct;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.persistence.PersistenceService;
import org.icroco.picture.views.CollectionManager;
import org.icroco.picture.views.task.TaskService;
import org.kordamp.ikonli.Ikon;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
@RequiredArgsConstructor
@Slf4j
public class ThumbnailGenerationTool implements RepairTool {
    private final TaskService        taskService;
    private final CollectionManager  collectionManager;
    private final PersistenceService persistenceService;
    private final VBox root = new VBox();


    @PostConstruct
    void init() {
        root.getChildren().add(new Label("Not Yet Implemented"));
    }

    @Override
    public String getName() {
        return "Generate";
    }

    @Override
    public Ikon getIcon() {
        return null;
    }

    @Override
    public Node getView() {
        return root;
    }

    @Override
    public NavTree.Item getGroup() {
        return RepairTool.THUMBNAIL_GENERATE;
    }
}
