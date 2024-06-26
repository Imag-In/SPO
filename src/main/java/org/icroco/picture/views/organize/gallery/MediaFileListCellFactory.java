package org.icroco.picture.views.organize.gallery;

import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.event.PhotoSelectedEvent;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.thumbnail.ThumbnailService;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.MediaLoader;

@Slf4j
@RequiredArgsConstructor
public class MediaFileListCellFactory implements Callback<ListView<MediaFile>, ListCell<MediaFile>> {
    private final MediaLoader mediaLoader;
    private final TaskService taskService;
    private final ThumbnailService thumbnailService;

    @Override
    public ListCell<MediaFile> call(ListView<MediaFile> param) {
        final var cell = new MediaFileListCell(mediaLoader, thumbnailService);
        cell.setAlignment(Pos.CENTER);
        cell.setEditable(false);

        cell.setOnMouseClicked((t) -> {
            var mf = ((MediaFileListCell) t.getSource()).getItem();
            if (t.getClickCount() == 1) {
//                cell.requestLayout();
                taskService.sendEvent(PhotoSelectedEvent.builder()
                                                        .mf(mf)
                                                        .type(PhotoSelectedEvent.ESelectionType.SELECTED)
                                                        .source(this)
                                                        .build());
            } else if (t.getClickCount() == 2) {
//                taskService.sendEvent(CarouselEvent.builder().mediaFile(mf).eventType(CarouselEvent.EventType.HIDE).source(this).build());
            }
            t.consume();
        });
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
////////                                              newMediaItem.thumbnail().setThumbnail(mediaLoader.loadThumbnail(newMediaItem.mcId(), newMediaItem.fullPath()));
//////                }
////            }
////        });
//
//        return cell;
//    }
}
