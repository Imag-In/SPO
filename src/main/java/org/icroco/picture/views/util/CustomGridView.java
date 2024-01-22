package org.icroco.picture.views.util;

import impl.org.controlsfx.skin.GridViewSkin;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Cell;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.ScrollEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.GridView;
import org.icroco.picture.event.PhotoSelectedEvent;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.util.LangUtils;
import org.icroco.picture.views.task.TaskService;
import org.jooq.lambda.Seq;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Optional.ofNullable;

@Slf4j
public class CustomGridView<T> extends GridView<T> {
    int selectedRow = 0; // current "selected" GridView row.
//    @Getter
//    private final GridCellSelectionModel selectionModel;

    public CustomGridView(ObservableList<T> items, Consumer<T> scrollEvenConsummer) {
        super(items);
//        selectionModel = new GridCellSelectionModel(taskService);
        setCenterShape(true);
        setSelectionModel(new GridViewSelectionModel<>(this));
        getSelectionModel().selectedIndexProperty().addListener((_, _, newValue) -> {
            if (newValue != null && newValue.intValue() >= 0) {
                updateSelectedRow(newValue.intValue());
            }
        });

        skinProperty().addListener((_, _, _) -> {
            Platform.runLater(() -> {
                getVirtualFlow().addEventHandler(ScrollEvent.SCROLL, _ -> {
                    // TODO: Add tempo.
                    getFirstVisible().ifPresent(tCell -> scrollEvenConsummer.accept(tCell.getItem()));
                });
            });
        });
    }

    public void addScrollAndKeyhandler() {
//        // example for scroll listener
////        this.addEventFilter(ScrollEvent.ANY, e-> System.out.println("*** scroll event fired ***"));
//
//        // add UP and DOWN arrow key listener, to set scroll position
//        addEventHandler(KeyEvent.KEY_PRESSED, e -> {
//            switch (e.getCode()) {
//                case UP -> Nodes.runAndConsume(e, this::oneRowUp);
//                case DOWN -> Nodes.runAndConsume(e, this::oneRowDown);
//                case LEFT -> Nodes.runAndConsume(e, this::oneRowLeft);
//                case RIGHT -> Nodes.runAndConsume(e, this::oneRowRight);
//            }
//        });
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

    public void oneRowUp() {
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
            ofNullable(getSelectionModel().getSelectedItem()).ifPresent(item -> {
//                log.info("U idx: {}",getSelectionModel().getSelectedIndex());
                getSelectionModel().clearSelection();
                var idx = getItems().indexOf(item) - getItemsInRow();
                if (idx >= 0) {
                    findItem(flow, getItems().get(idx))
                            .ifPresent(mediaFileCell -> getSelectionModel().select(mediaFileCell.getItem()));
                }
            });
        }
    }

    public void oneRowDown() {
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
            ofNullable(getSelectionModel().getSelectedItem()).ifPresent(item -> {
//                log.info("D idx: {}",getSelectionModel().getSelectedIndex());
                getSelectionModel().clearSelection();
                var idx = getItems().indexOf(item) + getItemsInRow();
                if (idx < getItems().size()) {
                    findItem(flow, getItems().get(idx))
                            .ifPresent(mediaFileCell -> getSelectionModel().select(mediaFileCell.getItem()));
                }
            });
        }
    }


    public void pageUp() {
        VirtualFlow<? extends IndexedCell<T>> flow  = getVirtualFlow();
        var                                   first = flow.getFirstVisibleCell();
        var                                   last  = flow.getLastVisibleCell();

        var diff = last.getIndex() - first.getIndex();
        log.info("page UP: first: {}, last: {}, diff: {}, next: {}", first.getIndex(), last.getIndex(), diff, first.getIndex() - diff);
        flow.scrollTo(Math.max(0, first.getIndex() - diff));
    }

    public void pageDown() {
        VirtualFlow<? extends IndexedCell<T>> flow  = getVirtualFlow();
        var                                   first = flow.getFirstVisibleCell();
        var                                   last  = flow.getLastVisibleCell();

        var diff = last.getIndex() - first.getIndex();
        log.info("page UP: first: {}, last: {}, diff: {}, next: {}, count: {}",
                 first.getIndex(),
                 last.getIndex(),
                 diff,
                 last.getIndex() + diff,
                 flow.getCellCount());
        flow.scrollTo(Math.min(flow.getCellCount(), last.getIndex() + diff));
    }

    public void oneRowLeft() {
        VirtualFlow<? extends IndexedCell<T>> flow = getVirtualFlow();
        if (flow.getCellCount() == 0) {
            return; // check that rows exist
        }
        ofNullable(getSelectionModel().getSelectedItem()).ifPresent(item -> {
//            log.info("L idx: {}",getSelectionModel().getSelectedIndex());
            getSelectionModel().clearSelection();
            var idx = getItems().indexOf(item);
            if (idx > 0) {
                findItem(flow, getItems().get(--idx))
                        .ifPresent(mediaFileCell -> getSelectionModel().select(mediaFileCell.getItem()));
            }
        });
    }


    public void oneRowRight() {
        VirtualFlow<? extends IndexedCell<T>> flow = getVirtualFlow();
        if (flow.getCellCount() == 0) {
            return; // check that rows exist
        }
        ofNullable(getSelectionModel().getSelectedItem()).ifPresent(item -> {
//            log.info("R idx: {}",getSelectionModel().getSelectedIndex());
            getSelectionModel().clearSelection();
            var idx = getItems().indexOf(item);
            if (idx + 1 < getItems().size()) {
                findItem(flow, getItems().get(++idx))
                        .ifPresent(mediaFileCell -> getSelectionModel().select(mediaFileCell.getItem()));
            }
        });
    }

    void updateSelectedRow(T item) {
        selectedRow = getItems().indexOf(item) / getItemsInRow();
    }

    void updateSelectedRow(int idx) {
        selectedRow = idx / getItemsInRow();
    }

    public Optional<Cell<T>> getFirstVisible() {
        try {
            VirtualFlow<? extends IndexedCell<T>> vf = getVirtualFlow();
            if (vf.getCellCount() > 0) {
                return vf.getFirstVisibleCell().getChildrenUnmodifiable().stream().map(n -> (Cell<T>) n).findFirst();
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            log.warn("Cannot get first visible cell", e);
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

    private final ObjectProperty<SingleSelectionModel<T>> selectionModel = new SimpleObjectProperty<>(this, "selectionModel") {
        private SingleSelectionModel<T> oldSM = null;

        @Override
        protected void invalidated() {
            if (oldSM != null) {
                oldSM.selectedItemProperty().removeListener(selectedItemListener);
            }
            SingleSelectionModel<T> sm = get();
            oldSM = sm;
            if (sm != null) {
                sm.selectedItemProperty().addListener(selectedItemListener);
            }
        }
    };

    public final void setSelectionModel(SingleSelectionModel<T> value) {
        selectionModel.set(value);
    }

    public final SingleSelectionModel<T> getSelectionModel() {
        return selectionModel.get();
    }

    public final ObjectProperty<SingleSelectionModel<T>> selectionModelProperty() {
        return selectionModel;
    }

    // Listen to changes in the selectedItem property of the SelectionModel.
    // When it changes, set the selectedItem in the value property.
    private final ChangeListener<T> selectedItemListener = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends T> ov, T t, T t1) {
            if (t1 != null) {
                findItem(getVirtualFlow(), t).ifPresent(Parent::requestLayout);
                findItem(getVirtualFlow(), t1).ifPresent(Parent::requestLayout);
            }
        }
    };

    @RequiredArgsConstructor
    static class GridViewSelectionModel<T> extends SingleSelectionModel<T> implements ListChangeListener<T> {
        private final CustomGridView<T>         grid;
        private final WeakListChangeListener<T> weakItemsContentObserver = new WeakListChangeListener<>(this);

        @Override
        protected T getModelItem(int index) {
            if (index < 0 || LangUtils.isNullOrEmpty(grid.getItems())) {
                return null;
            }
            return grid.getItems().get(index);
        }

        @Override
        protected int getItemCount() {
            if (grid.getItems() == null || grid.getItems().isEmpty()) {
                return 0;
            }
            return grid.getItems().size();
        }


        @Override
        public void onChanged(Change<? extends T> c) {
            if (LangUtils.isNullOrEmpty(grid.getItems())) {
                setSelectedIndex(-1);
            } else if (getSelectedIndex() == -1 && getSelectedItem() != null) {
                int newIndex = grid.getItems().indexOf(getSelectedItem());
                if (newIndex != -1) {
                    setSelectedIndex(newIndex);
                }
            }
            int shift = 0;
            while (c.next()) {
                if (c.wasReplaced()) {
                    // no-op
                } else if (c.wasAdded() || c.wasRemoved()) {
                    if (c.getFrom() <= getSelectedIndex() && getSelectedIndex() != -1) {
                        shift += c.wasAdded() ? c.getAddedSize() : -c.getRemovedSize();
                    }
                }
            }

            if (shift != 0) {
                clearAndSelect(getSelectedIndex() + shift);
            } else if (getSelectedIndex() >= 0 && getSelectedItem() != null) {
                // try to find the previously selected item
//                T selectedItem = getSelectedItem();
//                for (int i = 0; i < grid.getItems().size(); i++) {
//                    if (selectedItem.equals(grid.getItems().get(i))) {
//                        grid.setValue(null);
//                        setSelectedItem(null);
//                        setSelectedIndex(i);
//                        break;
//                    }
//                }
            }
        }
    }

    @RequiredArgsConstructor
    public class GridCellSelectionModel {
        private final TaskService          taskService;
        private final Set<Cell<MediaFile>> selection = new HashSet<>();

//        public void add(@Nullable Cell<MediaFile> node) {
//            if (node != null) {
//                selection.add(node);
//                node.updateSelected(true);
//                updateSelectedRow(node);
//                taskService.sendEvent(PhotoSelectedEvent.builder()
//                                                        .mf(node.getItem())
//                                                        .type(PhotoSelectedEvent.ESelectionType.SELECTED)
//                                                        .source(this)
//                                                        .build());
//            }
//        }

        Optional<Cell<MediaFile>> get() {
            return selection.stream().findFirst();
        }


//        public void set(Cell<MediaFile> node) {
//            selection.forEach(c -> c.updateSelected(false));
//            selection.clear();
//            selection.add(node);
//            node.updateSelected(true);
//            updateSelectedRow(node);
//            node.getItem().setSelected(true);
////        node.setStyle("aaa");
//            taskService.sendEvent(PhotoSelectedEvent.builder()
//                                                    .mf(node.getItem())
//                                                    .type(PhotoSelectedEvent.ESelectionType.SELECTED)
//                                                    .source(this)
//                                                    .build());
//        }

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
