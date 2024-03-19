package org.icroco.picture.views.util;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TreeItem;

import java.lang.reflect.Field;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

// https://github.com/sshahine/JFoenix/blob/master/demo/src/main/java/demos/components/TreeViewDemo.java#L116
public class FilterableTreeItem<T> extends TreeItem<T> {

    private final SimpleObjectProperty<TreeItemPredicate<T>> predicate  = new SimpleObjectProperty<>();
    private final ObservableList<TreeItem<T>>                sourceList = FXCollections.observableArrayList();

    public FilterableTreeItem(T value) {
        super(value);
        FilteredList<TreeItem<T>> filteredList = new FilteredList<>(this.sourceList);
        filteredList.predicateProperty().bind(Bindings.createObjectBinding(() -> child -> {
            // Set the predicate of child items to force filtering
            if (child instanceof FilterableTreeItem<T> filterableChild) {
                filterableChild.setPredicate(this.predicate.get());
            }
            // If there is no predicate, keep this tree item
            if (this.predicate.get() == null) {
                return true;
            }
            // If there are children, keep this tree item
            if (!child.getChildren().isEmpty()) {
                return true;
            }
            // Otherwise, ask the TreeItemPredicate
            return this.predicate.get().test(this, child.getValue());
        }, this.predicate));
        setHiddenFieldChildren(filteredList);
    }

    @SuppressWarnings("unchecked")
    protected void setHiddenFieldChildren(ObservableList<TreeItem<T>> list) {
        try {
            Field childrenField = TreeItem.class.getDeclaredField("children"); //$NON-NLS-1$
            childrenField.setAccessible(true);
            childrenField.set(this, list);

            Field declaredField = TreeItem.class.getDeclaredField("childrenListener"); //$NON-NLS-1$
            declaredField.setAccessible(true);
            list.addListener((ListChangeListener<? super TreeItem<T>>) declaredField.get(this));
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException("Could not set TreeItem.children", e); //$NON-NLS-1$
        }
    }

    public ObservableList<TreeItem<T>> getInternalChildren() {
        return this.sourceList;
    }

    public void setPredicate(TreeItemPredicate<T> predicate) {
        this.predicate.set(predicate);
    }

    public TreeItemPredicate<T> getPredicate() {
        return predicate.get();
    }

    public SimpleObjectProperty<TreeItemPredicate<T>> predicateProperty() {
        return predicate;
    }

    @FunctionalInterface
    public interface TreeItemPredicate<T> extends BiPredicate<TreeItem<T>, T> {
        static <T> TreeItemPredicate<T> create(Predicate<T> predicate) {
            return (_, value) -> predicate.test(value);
        }
    }
}
