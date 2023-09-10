package org.icroco.picture.ui.util;

import impl.org.controlsfx.skin.GridViewSkin;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.GridView;
import org.icroco.picture.ui.task.TaskService;

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
            if (e.getCode() == KeyCode.UP) oneRowUp();
            if (e.getCode() == KeyCode.DOWN) oneRowDown();
        });
    }

    private void oneRowUp() {
        // get the underlying VirtualFlow object
        VirtualFlow<?> flow = (VirtualFlow<?>) ((GridViewSkin<?>) this.getSkin()).getChildren().get(0);
        if (flow.getCellCount() == 0) return; // check that rows exist

        if (--selectedRow < 0)
            selectedRow = 0;
        if (selectedRow >= flow.cellCountProperty().get())
            selectedRow = flow.getCellCount() - 1;
        log.info("*** KeyEvent oneRowUp: {}  ***", selectedRow);

        flow.scrollTo(selectedRow);
    }

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

    private void oneRowDown() {
        // get the underlying VirtualFlow object
        VirtualFlow<?> flow = (VirtualFlow<?>) ((GridViewSkin<?>) this.getSkin()).getChildren().get(0);
        if (flow.getCellCount() == 0) return; // check that rows exist

        if (++selectedRow >= flow.cellCountProperty().get())
            selectedRow = flow.getCellCount() - 1;
        log.info("*** KeyEvent oneRowDown: {}  ***", selectedRow);

        flow.scrollTo(selectedRow);
    }

    public void ensureVisible(T item) {
        // Gross workaround. Couldn't find any other solution
        for (Node n : getChildren()) {
            if (n instanceof VirtualFlow<?> vf) {
                vf.scrollTo(getItems().indexOf(item) / getItemsInRow());
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
}
