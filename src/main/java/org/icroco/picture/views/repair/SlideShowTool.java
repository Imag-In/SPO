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
public class SlideShowTool implements RepairTool {
    private final TaskService        taskService;
    private final CollectionManager  collectionManager;
    private final PersistenceService persistenceService;
    private final VBox               vb = new VBox();


    @PostConstruct
    void init() {
        vb.getChildren().add(new Label("Not Yet Implemented"));
    }

    @Override
    public String getName() {
        return "Slideshow";
    }

    @Override
    public Ikon getIcon() {
        return null;
    }

    @Override
    public Node getView() {
        return vb;
    }

    @Override
    public NavTree.Item getGroup() {
        return RepairTool.OTHERS;
    }
}
