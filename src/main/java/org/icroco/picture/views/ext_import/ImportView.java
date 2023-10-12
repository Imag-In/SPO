package org.icroco.picture.views.ext_import;

import jakarta.annotation.PostConstruct;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.views.AbstractView;
import org.icroco.picture.views.ViewConfiguration;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ImportView extends AbstractView<StackPane> {
    private StackPane root = new StackPane();

    @PostConstruct
    private void postConstruct() {
        root.setId(ViewConfiguration.V_IMPORT);
        root.getStyleClass().add(ViewConfiguration.V_IMPORT);

        root.getChildren().add(createForm());
    }

    private Node createForm() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(10, 10, 10, 10));
        int rowIdx = 0;

        grid.add(createLabel("Source directory", 100, 150), 0, rowIdx);

        rowIdx += 2;
        grid.add(createLabel("Target collection", 100, 150), 0, rowIdx);

        rowIdx += 2;
        grid.add(createLabel("directory", 100, 150), 0, rowIdx);

        rowIdx += 2;
        grid.add(createLabel("Generate thumbnail", 100, 150), 0, rowIdx);

        StackPane.setAlignment(grid, Pos.CENTER);

        return grid;
    }

    @Override
    public StackPane getRootContent() {
        return root;
    }
}
