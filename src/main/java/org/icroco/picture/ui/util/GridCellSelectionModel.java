/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template mf, choose Tools | Templates
 * and open the template in the editor.
 */
package org.icroco.picture.ui.util;

import lombok.RequiredArgsConstructor;
import org.icroco.picture.ui.event.PhotoSelectedEvent;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.task.TaskService;

import java.util.HashSet;
import java.util.Set;


@RequiredArgsConstructor
public class GridCellSelectionModel {
    private final TaskService taskService;
    private final Set<MediaFile> selection = new HashSet<>();

    public void add(MediaFile node) {
        selection.add(node);
        node.setSelected(true);
        taskService.sendEvent(new PhotoSelectedEvent(PhotoSelectedEvent.ESelectionType.SELECTED, node, this));
    }

    public int selectionCount() {
        return selection.size();
    }

    public void remove(final MediaFile node) {
        selection.remove(node);
        node.setSelected(false);
        taskService.sendEvent(new PhotoSelectedEvent(PhotoSelectedEvent.ESelectionType.UNSELECTED, node, this));
    }

    public void clear() {
        selection.forEach(mf -> mf.setSelected(false));
        selection.clear();
        taskService.sendEvent(new PhotoSelectedEvent(PhotoSelectedEvent.ESelectionType.UNSELECTED, null, this));
    }

    public boolean contains(final MediaFile node) {
        return selection.contains(node);
    }


    public Set<MediaFile> getSelection() {
        return Set.copyOf(selection);
    }

}
