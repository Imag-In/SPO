package org.icroco.picture.ui.gallery;

import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.event.CarouselEvent;
import org.icroco.picture.ui.event.PhotoSelectedEvent;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.task.TaskService;
import org.icroco.picture.ui.util.MediaLoader;

@Slf4j
@RequiredArgsConstructor
public class MediaFileListCellFactory implements Callback<ListView<MediaFile>, ListCell<MediaFile>> {
    private final MediaLoader mediaLoader;
    private final TaskService taskService;

    @Override
    public ListCell<MediaFile> call(ListView<MediaFile> param) {
        final var cell = new MediaFileListCell(mediaLoader);
        cell.setAlignment(Pos.CENTER);
        cell.setEditable(false);

        cell.setOnMouseClicked((t) -> {
            var mf = ((MediaFileListCell) t.getSource()).getItem();
            if (t.getClickCount() == 1) {
//                cell.requestLayout();
                taskService.fxNotifyLater(new PhotoSelectedEvent(mf, this));
            } else if (t.getClickCount() == 2) {
                taskService.fxNotifyLater(CarouselEvent.builder().source(this).mediaFile(mf).eventType(CarouselEvent.EventType.HIDE).build());
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

//    @Override
//    public GridCell<MediaFile> call(GridView<MediaFile> param) {
//        final var cell = new MediaFileGridCell(true, mediaLoader, true);
//        cell.setAlignment(Pos.CENTER);
//        cell.setEditable(true);
//
//        cell.setOnMouseClicked((t) -> {
//            if (t.getClickCount() == 1) {
//                selectionModel.clear();
//                var mediaFile = ((MediaFileGridCell) t.getSource()).getItem();
//                selectionModel.add(mediaFile);
//                cell.requestLayout();
//                taskService.notifyLater(new PhotoSelectedEvent(mediaFile, this));
//                t.consume();
//            } else if (t.getClickCount() == 2) {
//                taskService.notifyLater(CarouselEvent.builder().source(this).eventType(CarouselEvent.EventType.SHOW).build());
//            }
//        });
//
////        cell.itemProperty().addListener((ov, oldMediaItem, newMediaItem) -> {
//////            if (newMediaItem != null) {
////            log.info("old: {}, new: {}", oldMediaItem, newMediaItem);
////            if (newMediaItem != null && oldMediaItem == null) {
//////                Platform.runLater(() -> {
//////                    newMediaItem.getThumbnailType().set(EThumbnailType.ABSENT);
//////                    mediaLoader.getCachedValue(newMediaItem)
//////                               .map(Thumbnail::getOrigin)
//////                               .ifPresent(tn -> newMediaItem.getThumbnailType().set(tn));
//////                });
////
//////                log.info("new Cell: "+newMediaItem.fullPath());
//////                if (newMediaItem.getThumbnailType().get() == EThumbnailType.ABSENT) {
//////                    mediaLoader.loadThumbnailFromFx(newMediaItem);
////////                                              newMediaItem.thumbnail().setThumbnail(mediaLoader.loadThumbnail(newMediaItem.id(), newMediaItem.fullPath()));
//////                }
////            }
////        });
//
//        return cell;
//    }
}
