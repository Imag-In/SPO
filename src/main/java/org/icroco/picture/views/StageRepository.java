package org.icroco.picture.views;

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Getter
@Component
@RequiredArgsConstructor
public class StageRepository {

    private final ObservableSet<Stage> stages = FXCollections.observableSet();

    public void addStage(Stage stage) {
        stages.add(stage);
    }

    public boolean removeStage(Stage stage) {
        return stages.remove(stage);
    }
}
