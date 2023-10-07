package org.icroco.picture.views.organize.gallery;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Cell;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.views.task.TaskService;
import org.icroco.picture.views.util.CustomGridView;
import org.icroco.picture.views.util.CustomMouseEvent;
import org.icroco.picture.views.util.MediaLoader;

import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
public class MediaFileGridCellFactory implements Callback<GridView<MediaFile>, GridCell<MediaFile>> {
    private final MediaLoader                               mediaLoader;
    private final TaskService                               taskService;
    private final BooleanProperty                           isExpandCell;
    private final BiConsumer<MouseEvent, MediaFileGridCell> callBack;

    @Override
    public GridCell<MediaFile> call(final GridView<MediaFile> grid) {
        final var cell = new MediaFileGridCell(true, mediaLoader, isExpandCell, (CustomGridView<MediaFile>) grid);

        cell.setAlignment(Pos.CENTER);
        cell.setEditable(false);
        cell.setOnMouseClicked(e -> callBack.accept(e, cell));
        cell.addEventHandler(CustomMouseEvent.MOUSE_DOUBLE_CLICKED, e -> callBack.accept(e, cell));

        cell.itemProperty().addListener((ov, oldMediaItem, newMediaItem) -> {
//            log.info("old: {}, new: {}", oldMediaItem, newMediaItem);
//            if (newMediaItem != null && oldMediaItem == null) {
            if (newMediaItem != null && oldMediaItem != newMediaItem) {
//                if (isCellVisible(grid, cell)) {
//                    log.debug("is Cell really visible?, old: '{}', new: '{}'", Optional.ofNullable(oldMediaItem).map(MediaFile::getId).orElse(null),
//                              Optional.of(newMediaItem).map(MediaFile::getId).orElse(null));
                mediaLoader.loadAndCachedValue(newMediaItem);
//                }
            }
        });

        return cell;
    }

    public static boolean isCellVisible(GridView<MediaFile> grid, MediaFileGridCell cell) {
        VirtualFlow<? extends IndexedCell<MediaFile>>
                vf =
                (VirtualFlow<? extends IndexedCell<MediaFile>>) grid.getChildrenUnmodifiable().get(0);
        boolean ret = false;
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
