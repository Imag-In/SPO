package org.icroco.picture.ui.gallery;

import javafx.geometry.Pos;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.util.MediaLoader;

import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Slf4j
@RequiredArgsConstructor
public class MediaFileGridCellFactory implements Callback<GridView<MediaFile>, GridCell<MediaFile>> {
    private final MediaLoader mediaLoader;

    @Override
    public GridCell<MediaFile> call(GridView<MediaFile> param) {
        final var cell = new MediaFileGridCell(true, mediaLoader);
        cell.setAlignment(Pos.CENTER);
        cell.setEditable(false);

        cell.setOnMouseClicked((t) -> {
//            cell.setSelected
            t.consume();
        });

        cell.itemProperty().addListener((ov, oldMediaItem, newMediaItem) -> {
            if (newMediaItem != null && oldMediaItem == null) {
//                log.info("new Cell: "+newMediaItem.fullPath());
                if (newMediaItem.isLoading()) {
                    mediaLoader.loadThumbnail(newMediaItem);
//                                              newMediaItem.cachedInfo().setThumbnail(mediaLoader.loadThumbnail(newMediaItem.id(), newMediaItem.fullPath()));
                }
            }
        });

        return cell;
    }
}
