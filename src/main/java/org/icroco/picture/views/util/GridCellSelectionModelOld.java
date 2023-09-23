/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template mf, choose Tools | Templates
 * and open the template in the editor.
 */
package org.icroco.picture.views.util;

import javafx.scene.control.Cell;
import lombok.RequiredArgsConstructor;
import org.icroco.picture.event.PhotoSelectedEvent;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.views.task.TaskService;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;


@RequiredArgsConstructor
public class GridCellSelectionModelOld {
    private final TaskService          taskService;
    private final Set<Cell<MediaFile>> selection = new HashSet<>();

    public void add(Cell<MediaFile> node) {
        selection.add(node);
        node.updateSelected(true);
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
