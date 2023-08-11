package org.icroco.picture.ui.gallery;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Cell;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.skin.VirtualFlow;
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
    private final MediaLoader     mediaLoader;
    private final TaskService     taskService;
    private final BooleanProperty isExpandCell;

    @Override
    public GridCell<MediaFile> call(final GridView<MediaFile> grid) {
        final var selectionModel = ((CustomGridView<MediaFile>) grid).getSelectionModel();
        final var cell           = new MediaFileGridCell(true, mediaLoader, isExpandCell, selectionModel, (CustomGridView<MediaFile>) grid);

        cell.setAlignment(Pos.CENTER);
        cell.setEditable(false);
        cell.setOnMouseClicked((t) -> {
            var mf = ((MediaFileGridCell) t.getSource()).getItem();
            if (mf != null && t.getClickCount() == 1) {
                ((CustomGridView<MediaFile>) grid).getSelectionModel().clear();
                ((CustomGridView<MediaFile>) grid).getSelectionModel().add(mf);
                cell.requestLayout();
                taskService.sendFxEvent(new PhotoSelectedEvent(mf, this));
            } else if (t.getClickCount() == 2) {
                taskService.sendFxEvent(CarouselEvent.builder().source(this).mediaFile(mf).eventType(CarouselEvent.EventType.SHOW).build());
            }
            t.consume();
        });

        cell.itemProperty().addListener((ov, oldMediaItem, newMediaItem) -> {
//            log.info("old: {}, new: {}", oldMediaItem, newMediaItem);
            if (newMediaItem != null && oldMediaItem == null) {
//                if (newMediaItem != null && oldMediaItem != newMediaItem) {
                if (isCellVisible(grid, cell)) {
//                    log.debug("is Cell really visible?, old: '{}', new: '{}'", Optional.ofNullable(oldMediaItem).map(MediaFile::getId).orElse(null),
//                              Optional.of(newMediaItem).map(MediaFile::getId).orElse(null));
                    mediaLoader.loadAndCachedValue(newMediaItem);
                }
            }
        });

        return cell;
    }

    public static boolean isCellVisible(GridView<MediaFile> grid, MediaFileGridCell cell) {
        VirtualFlow<? extends IndexedCell<MediaFile>> vf  = (VirtualFlow<? extends IndexedCell<MediaFile>>) grid.getChildrenUnmodifiable().get(0);
        boolean                                       ret = false;
        if (vf.getFirstVisibleCell() == null) {
//            log.info("Cell: {}, visible: {}, index: {} null", cell.getItem().getFileName(), false, cell.getIndex());
            return false;
        }
        int start = vf.getFirstVisibleCell().getIndex();
        int end   = vf.getLastVisibleCell().getIndex();

        if (start == end) {
//            log.info("Cell: {}, visible: {}, index: {} ==", cell.getItem().getFileName(), true, cell.getIndex());
            return true;
        }

//        log.info("<<<<");
        for (int i = start; i <= end; i++) {
//            log.info("row: {}, value: {}", i, vf.getCell(i).getChildrenUnmodifiable().stream().map(MediaFileGridCell.class::cast).map(Cell::getItem).map(MediaFile::getId).toList());
            if (vf.getCell(i)
                  .getChildrenUnmodifiable()
                  .stream()
                  .map(MediaFileGridCell.class::cast)
                  .map(Cell::getItem)
                  .map(MediaFile::getId)
                  .toList()
                  .contains(cell.getItem().getId())) {
                ret = true;
                break;
            }
        }

//        log.info(">>>> Cell: {}:{}, visible: {}, index: {}", cell.getItem().getId(), cell.getItem().getFileName(), ret, cell.getIndex());

        return ret;
    }
}
