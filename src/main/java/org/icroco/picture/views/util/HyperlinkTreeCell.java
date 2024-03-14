package org.icroco.picture.views.util;

import javafx.scene.control.Hyperlink;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.util.SystemUtil;

import java.nio.file.Path;

@Slf4j
public class HyperlinkTreeCell<T> implements Callback<TreeTableColumn<T, Path>, TreeTableCell<T, Path>> {

    @Override
    public TreeTableCell<T, Path> call(TreeTableColumn<T, Path> arg) {
        return new TreeTableCell<>() {
            private final Hyperlink hyperlink = new Hyperlink();

            {
                hyperlink.setOnAction(_ -> {
                    log.info("Hyperlink: {}", hyperlink.getText());
                    SystemUtil.browseFile(Path.of(hyperlink.getText()));
                });
            }

            @Override
            protected void updateItem(Path path, boolean empty) {
                super.updateItem(path, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    hyperlink.setText(path == null ? "" : path.toString());
                    setGraphic(hyperlink);
                }
            }
        };
    }
}