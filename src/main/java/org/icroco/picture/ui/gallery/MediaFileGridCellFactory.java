package org.icroco.picture.ui.gallery;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.icroco.picture.ui.event.PhotoSelectedEvent;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.task.TaskService;
import org.icroco.picture.ui.util.GridCellSelectionModel;
import org.icroco.picture.ui.util.MediaLoader;

@Slf4j
@RequiredArgsConstructor
public class MediaFileGridCellFactory implements Callback<GridView<MediaFile>, GridCell<MediaFile>> {
    private final MediaLoader            mediaLoader;
    private final TaskService            taskService;
    private final BooleanProperty        isExpandCell;
    private final GridCellSelectionModel selectionModel = new GridCellSelectionModel();

    @Override
    public GridCell<MediaFile> call(GridView<MediaFile> param) {
        final var cell = new MediaFileGridCell(true, mediaLoader, isExpandCell);
        cell.setAlignment(Pos.CENTER);
        cell.setEditable(true);

        cell.setOnMouseClicked((t) -> {
            selectionModel.clear();
            var mediaFile = ((MediaFileGridCell) t.getSource()).getItem();
            selectionModel.add(mediaFile);
            cell.requestLayout();
            taskService.notifyLater(new PhotoSelectedEvent(mediaFile, this));
            t.consume();
        });

//        cell.itemProperty().addListener((ov, oldMediaItem, newMediaItem) -> {
////            if (newMediaItem != null) {
//            log.info("old: {}, new: {}", oldMediaItem, newMediaItem);
//            if (newMediaItem != null && oldMediaItem == null) {
////                Platform.runLater(() -> {
////                    newMediaItem.getThumbnailType().set(EThumbnailType.ABSENT);
////                    mediaLoader.getCachedValue(newMediaItem)
////                               .map(Thumbnail::getOrigin)
////                               .ifPresent(tn -> newMediaItem.getThumbnailType().set(tn));
////                });
//
////                log.info("new Cell: "+newMediaItem.fullPath());
////                if (newMediaItem.getThumbnailType().get() == EThumbnailType.ABSENT) {
////                    mediaLoader.loadThumbnailFromFx(newMediaItem);
//////                                              newMediaItem.thumbnail().setThumbnail(mediaLoader.loadThumbnail(newMediaItem.id(), newMediaItem.fullPath()));
////                }
//            }
//        });

        return cell;
    }
}
