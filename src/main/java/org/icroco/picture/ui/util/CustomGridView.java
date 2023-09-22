package org.icroco.picture.ui.util;

import impl.org.controlsfx.skin.GridViewSkin;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.GridView;
import org.icroco.picture.ui.event.PhotoSelectedEvent;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.task.TaskService;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class CustomGridView<T> extends GridView<T> {
    int selectedRow = 0; // current "selected" GridView row.
    @Getter
    private final GridCellSelectionModel selectionModel;

    public CustomGridView(TaskService taskService, ObservableList<T> items) {
        super(items);
        selectionModel = new GridCellSelectionModel(taskService);
        setCenterShape(true);
    }

    public void addScrollAndKeyhandler() {
        // example for scroll listener
//        this.addEventFilter(ScrollEvent.ANY, e-> System.out.println("*** scroll event fired ***"));

        // add UP and DOWN arrow key listener, to set scroll position
        this.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.UP) {
                oneRowUp();
            }
            if (e.getCode() == KeyCode.DOWN) {
                oneRowDown();
            }
            e.consume();
        });
    }


//    public Optional<T> getFirstCellVisible() {
//        // get the underlying VirtualFlow object
//        VirtualFlow<?> flow = (VirtualFlow<?>) ((GridViewSkin<?>) this.getSkin()).getChildren().get(0);
//
//        return Optional.ofNullable(flow.getCell(0))
//                .flatMap(indexedCell -> indexedCell.getChildrenUnmodifiable().stream().findFirst())
//                .map(node -> node.getUserData())
//                .map()
//    }

    public boolean isCellVisible(Node input) {
        VirtualFlow<?> vf  = (VirtualFlow<?>) getChildrenUnmodifiable().get(0);
        boolean        ret = false;
        if (vf.getFirstVisibleCell() == null) {
            return false;
        }
        int start = vf.getFirstVisibleCell().getIndex();
        int end   = vf.getLastVisibleCell().getIndex();
//        log.info("Visible start-end: {}:{}", start, end);
        if (start == end) {
            return true;
        }
        for (int i = start; i <= end; i++) {
            if (vf.getCell(i).getChildrenUnmodifiable().contains(input)) {
                return true;
            }
        }
        return ret;
    }

    Optional<Cell<T>> findItem(VirtualFlow<?> vf, T mf) {

        if (vf.getFirstVisibleCell() == null) {
            return Optional.empty();
        }

        int start = vf.getFirstVisibleCell().getIndex();
        int end   = vf.getLastVisibleCell().getIndex();
//        log.info("Visible start-end: {}:{}", start, end);
        if (start == end) {
            return Optional.empty();
        }
        for (int i = start; i <= end; i++) {
            var node = vf.getCell(i).getChildrenUnmodifiable().stream()
                         .map(n -> (Cell<T>) n)
                         .filter(mediaFileCell -> mediaFileCell.getItem() == mf)
                         .findFirst();
            if (node.isPresent()) {
                return node;
            }
        }
        return Optional.empty();
    }


    private void oneRowUp() {
//        log.info("*** KeyEvent before oneRowUp: {}  ***", selectedRow);

        // get the underlying VirtualFlow object
        VirtualFlow<?> flow = (VirtualFlow<?>) ((GridViewSkin<?>) this.getSkin()).getChildren().get(0);
        if (flow.getCellCount() == 0) {
            return; // check that rows exist
        }

        var overTop = --selectedRow < 0;
        if (overTop) {
            selectedRow = 0;
        }
        if (selectedRow >= flow.cellCountProperty().get()) {
            selectedRow = flow.getCellCount() - 1;
        }
        flow.scrollTo(selectedRow);
        if (!overTop) {
            getSelectionModel().get().ifPresent(cell -> {
                var idx = getItems().indexOf(cell.getItem()) - getItemsInRow();
                if (idx >= 0) {
                    findItem(flow, (T) getItems().get(idx))
                            .ifPresent(mediaFileCell -> getSelectionModel().set((Cell<MediaFile>) mediaFileCell));
                }
            });
        }
    }

    private void oneRowDown() {
//        log.info("*** KeyEvent before oneRowDown: {}  ***", selectedRow);
        // get the underlying VirtualFlow object
        VirtualFlow<?> flow = (VirtualFlow<?>) ((GridViewSkin<?>) this.getSkin()).getChildren().get(0);
        if (flow.getCellCount() == 0) {
            return; // check that rows exist
        }
        var overLast = ++selectedRow >= flow.cellCountProperty().get();
        if (overLast) {
            selectedRow = flow.getCellCount() - 1;
        }

        flow.scrollTo(selectedRow);
        if (!overLast) {
            getSelectionModel().get().ifPresent(cell -> {
                var idx = getItems().indexOf(cell.getItem()) + getItemsInRow();
                if (idx < getItems().size()) {
                    findItem(flow, (T) getItems().get(idx))
                            .ifPresent(mediaFileCell -> getSelectionModel().set((Cell<MediaFile>) mediaFileCell));
                }
            });
        }
    }

    void updateSelectedRow(Cell<?> node) {
        selectedRow = getItems().indexOf(node.getItem()) / getItemsInRow();
        log.info("Selected row: " + selectedRow);
    }

    public void ensureVisible(T item) {
        // Gross workaround. Couldn't find any other solution
        for (Node n : getChildren()) {
            if (n instanceof VirtualFlow<?> vf) {
                int index = getItems().indexOf(item) / getItemsInRow();
                vf.scrollTo(index);
                selectedRow = index;
                break;
            }
        }
    }

    public int getItemsInRow() {
        return ((GridViewSkin<?>) getSkin()).computeMaxCellsInRow();
    }

    public void refreshItems() {
        ((GridViewSkin<?>) getSkin()).updateGridViewItems();
    }

    @RequiredArgsConstructor
    public class GridCellSelectionModel {
        private final TaskService          taskService;
        private final Set<Cell<MediaFile>> selection = new HashSet<>();

        public void add(Cell<MediaFile> node) {
            selection.add(node);
            node.updateSelected(true);
            updateSelectedRow(node);
            taskService.sendEvent(new PhotoSelectedEvent(PhotoSelectedEvent.ESelectionType.SELECTED, node.getItem(), this));
        }

        Optional<Cell<MediaFile>> get() {
            return selection.stream().findFirst();
        }

        public void set(Cell<MediaFile> node) {
            selection.forEach(c -> c.updateSelected(false));
            selection.clear();
            selection.add(node);
            node.updateSelected(true);
            updateSelectedRow(node);
            node.getItem().setSelected(true);
//        node.setStyle("aaa");
            taskService.sendEvent(new PhotoSelectedEvent(PhotoSelectedEvent.ESelectionType.SELECTED, node.getItem(), this));
        }

        public void addOrRemove(Cell<MediaFile> node) {
            if (selection.remove(node)) {
                node.getItem().setSelected(false);
                taskService.sendEvent(new PhotoSelectedEvent(PhotoSelectedEvent.ESelectionType.UNSELECTED, node.getItem(), this));
            } else {
                selection.add(node);
                node.getItem().setSelected(true);
                taskService.sendEvent(new PhotoSelectedEvent(PhotoSelectedEvent.ESelectionType.SELECTED, node.getItem(), this));
            }
        }


        public int selectionCount() {
            return selection.size();
        }

        public void remove(final Cell<MediaFile> node) {
            node.updateSelected(false);
            selection.remove(node);
            node.getItem().setSelected(false);
            taskService.sendEvent(new PhotoSelectedEvent(PhotoSelectedEvent.ESelectionType.UNSELECTED, node.getItem(), this));
        }

        public void clear() {
            selection.forEach(mf -> mf.getItem().setSelected(false));
            selection.clear();
            taskService.sendEvent(new PhotoSelectedEvent(PhotoSelectedEvent.ESelectionType.UNSELECTED, null, this));
        }

        public boolean contains(final Cell<MediaFile> node) {
            return selection.contains(node);
        }


        public Set<Cell<MediaFile>> getSelection() {
            return Set.copyOf(selection);
        }

    }
}
