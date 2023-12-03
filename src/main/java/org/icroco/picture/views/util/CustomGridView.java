package org.icroco.picture.views.util;

import impl.org.controlsfx.skin.GridViewSkin;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.GridView;
import org.icroco.picture.event.PhotoSelectedEvent;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.views.task.TaskService;
import org.jooq.lambda.Seq;
import org.springframework.lang.Nullable;

import java.util.HashSet;
import java.util.List;
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
        addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case UP -> oneRowUp();
                case DOWN -> oneRowDown();
                case LEFT -> oneRowLeft();
                case RIGHT -> oneRowRight();
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
        VirtualFlow<?> vf = getVirtualFlow();
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

    Optional<Cell<T>> findItem(VirtualFlow<? extends IndexedCell<T>> vf, T mf) {

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

    private VirtualFlow<? extends IndexedCell<T>> getVirtualFlow() {
        return (VirtualFlow<? extends IndexedCell<T>>) getChildrenUnmodifiable().get(0);
    }

    public Optional<Cell<T>> findFirstItem() {
        if (getItems().isEmpty()) {
            return Optional.empty();
        }

        return findItem(getVirtualFlow(), getItems().get(0));
    }


    private void oneRowUp() {
//        log.info("*** KeyEvent before oneRowUp: {}  ***", selectedRow);

        // get the underlying VirtualFlow object
        VirtualFlow<? extends IndexedCell<T>> flow = getVirtualFlow();
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
                    findItem(flow, getItems().get(idx))
                            .ifPresent(mediaFileCell -> getSelectionModel().set((Cell<MediaFile>) mediaFileCell));
                }
            });
        }
    }

    private void oneRowDown() {
//        log.info("*** KeyEvent before oneRowDown: {}  ***", selectedRow);
        // get the underlying VirtualFlow object
        VirtualFlow<? extends IndexedCell<T>> flow = getVirtualFlow();
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
                    findItem(flow, getItems().get(idx))
                            .ifPresent(mediaFileCell -> getSelectionModel().set((Cell<MediaFile>) mediaFileCell));
                }
            });
        }
    }

    public Optional<T> getLeft(T mediaFile) {
        return getLeftIndex(getIndex(mediaFile));
    }

    Optional<T> getLeftIndex(int idx) {
        if (idx > 0) {
            return Optional.of((T) getItems().get(--idx));
        }
        return Optional.empty();
    }

    public Optional<T> getRight(T mediaFile) {
        return getRightIndex(getIndex(mediaFile));
    }

    Optional<T> getRightIndex(int idx) {
        if (idx < getItems().size() - 1) {
            return Optional.of(getItems().get(++idx));
        }
        return Optional.empty();
    }

    private int getIndex(T mediaFile) {
        return getItems().indexOf(mediaFile);
    }

    public List<T> getLeftAndRight(T mediaFile) {
        var idx = getIndex(mediaFile);

        return Seq.concat(getLeftIndex(idx), getRightIndex(idx), getLeftIndex(idx - 1), getRightIndex(idx + 1)).toList();
    }

    public void oneRowLeft() {
        VirtualFlow<? extends IndexedCell<T>> flow = getVirtualFlow();
        if (flow.getCellCount() == 0) {
            return; // check that rows exist
        }
        getSelectionModel().get().ifPresent(cell -> {
            var idx = getItems().indexOf(cell.getItem());
            if (idx > 0) {
                findItem(flow, getItems().get(--idx))
                        .ifPresent(mediaFileCell -> getSelectionModel().set((Cell<MediaFile>) mediaFileCell));
            }
        });
    }


    public void oneRowRight() {
        VirtualFlow<? extends IndexedCell<T>> flow = getVirtualFlow();
        if (flow.getCellCount() == 0) {
            return; // check that rows exist
        }
        getSelectionModel().get().ifPresent(cell -> {
            var idx = getItems().indexOf(cell.getItem());
            if (idx + 1 < getItems().size()) {
                findItem(flow, getItems().get(++idx))
                        .ifPresent(mediaFileCell -> getSelectionModel().set((Cell<MediaFile>) mediaFileCell));
            }
        });
    }

    void updateSelectedRow(Cell<?> node) {
        selectedRow = getItems().indexOf(node.getItem()) / getItemsInRow();
    }

    public Optional<Cell<T>> getFirstVisible() {
        VirtualFlow<?> flow = getVirtualFlow();
        if (flow.getCellCount() > 0) {
            return Optional.ofNullable(flow.getFirstVisibleCell());
        } else {
            return Optional.empty();
        }
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

        public void add(@Nullable Cell<MediaFile> node) {
            if (node != null) {
                selection.add(node);
                node.updateSelected(true);
                updateSelectedRow(node);
                taskService.sendEvent(PhotoSelectedEvent.builder()
                                                        .mf(node.getItem())
                                                        .type(PhotoSelectedEvent.ESelectionType.SELECTED)
                                                        .source(this)
                                                        .build());
            }
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
            taskService.sendEvent(PhotoSelectedEvent.builder()
                                                    .mf(node.getItem())
                                                    .type(PhotoSelectedEvent.ESelectionType.SELECTED)
                                                    .source(this)
                                                    .build());
        }

        public void addOrRemove(Cell<MediaFile> node) {
            if (selection.remove(node)) {
                node.getItem().setSelected(false);
                taskService.sendEvent(PhotoSelectedEvent.builder()
                                                        .mf(node.getItem())
                                                        .type(PhotoSelectedEvent.ESelectionType.UNSELECTED)
                                                        .source(this)
                                                        .build());
            } else {
                selection.add(node);
                node.getItem().setSelected(true);
                taskService.sendEvent(PhotoSelectedEvent.builder()
                                                        .mf(node.getItem())
                                                        .type(PhotoSelectedEvent.ESelectionType.SELECTED)
                                                        .source(this)
                                                        .build());
            }
        }


        public int selectionCount() {
            return selection.size();
        }

        public void remove(final Cell<MediaFile> node) {
            node.updateSelected(false);
            selection.remove(node);
            node.getItem().setSelected(false);
            taskService.sendEvent(PhotoSelectedEvent.builder()
                                                    .mf(node.getItem())
                                                    .type(PhotoSelectedEvent.ESelectionType.UNSELECTED)
                                                    .source(this)
                                                    .build());
        }

        public void clear() {
            selection.forEach(mf -> mf.getItem().setSelected(false));
            selection.clear();
            taskService.sendEvent(PhotoSelectedEvent.builder().type(PhotoSelectedEvent.ESelectionType.UNSELECTED).source(this).build());
        }

        public boolean contains(final Cell<MediaFile> node) {
            return selection.contains(node);
        }


        public Set<Cell<MediaFile>> getSelection() {
            return Set.copyOf(selection);
        }

        public void refreshSelection() {
            selection.stream().findFirst().ifPresent(cell ->
                                                             taskService.sendEvent(PhotoSelectedEvent.builder()
                                                                                                     .mf(cell.getItem())
                                                                                                     .type(PhotoSelectedEvent.ESelectionType.SELECTED)
                                                                                                     .source(this)
                                                                                                     .build()));
        }
    }
}
