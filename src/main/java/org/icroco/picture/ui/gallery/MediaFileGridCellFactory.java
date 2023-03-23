package org.icroco.picture.ui.gallery;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.icroco.picture.ui.event.CarouselEvent;
import org.icroco.picture.ui.event.PhotoSelectedEvent;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.task.TaskService;
import org.icroco.picture.ui.util.CustomGridView;
import org.icroco.picture.ui.util.MediaLoader;

@Slf4j
@RequiredArgsConstructor
public class MediaFileGridCellFactory implements Callback<GridView<MediaFile>, GridCell<MediaFile>> {
    private final MediaLoader            mediaLoader;
    private final TaskService            taskService;
    private final BooleanProperty        isExpandCell;
//    private final GridCellSelectionModel selectionModel = new GridCellSelectionModel();

    @Override
    public GridCell<MediaFile> call(final GridView<MediaFile> grid) {
        final var cell = new MediaFileGridCell(true, mediaLoader, isExpandCell);
        cell.setAlignment(Pos.CENTER);
        cell.setEditable(false);

        cell.setOnMouseClicked((t) -> {
            var mf = ((MediaFileGridCell) t.getSource()).getItem();
            if (t.getClickCount() == 1) {
                ((CustomGridView<MediaFile>) grid).getSelectionModel().clear();
                ((CustomGridView<MediaFile>) grid).getSelectionModel().add(cell);
                cell.requestLayout();
                taskService.notifyLater(new PhotoSelectedEvent(mf, this));
            } else if (t.getClickCount() == 2) {
                taskService.notifyLater(CarouselEvent.builder().source(this).mediaFile(mf).eventType(CarouselEvent.EventType.SHOW).build());
            }
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
