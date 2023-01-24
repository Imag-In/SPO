/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.icroco.picture.ui.util;

import org.icroco.picture.ui.model.MediaFile;

import java.util.HashSet;
import java.util.Set;

/**
 * @author selfemp
 */
public class GridCellSelectionModel {

    private final Set<MediaFile> selection = new HashSet<>();

    public void add(MediaFile node) {
        selection.add(node);
        node.setSelected(true);
    }

    public int selectionCount() {
        return selection.size();
    }

    public void remove(final MediaFile node) {
        selection.remove(node);
        node.setSelected(false);
    }

    public void clear() {
        selection.forEach(mf -> mf.setSelected(false));
        selection.clear();
    }

    public boolean contains(final MediaFile node) {
        return selection.contains(node);
    }


    public Set<MediaFile> getSelection() {
        return Set.copyOf(selection);
    }

}
