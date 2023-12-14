package org.icroco.picture.views.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.util.function.Predicate;

@Getter
public class MultiplePredicates<T> implements Predicate<T> {
    ObservableList<Predicate<T>> predicates = FXCollections.observableArrayList();

    @Override
    public boolean test(T mediaFile) {
        for (int i = 0; i < predicates.size(); i++) {
            if (!predicates.get(i).test(mediaFile)) {
                return false;
            }
        }
        return true;
        // TODO: Add JMH benchmark
//        return predicates.stream().allMatch(p -> p.test(mediaFile));
        //        return predicates.stream().reduce(x->true, Predicate::and).test(mediaFile);

    }


    public int size() {
        return predicates.size();
    }

    public void clear() {
        predicates.clear();
    }

    public void add(Predicate<T> predicate) {
        predicates.add(predicate);
    }

    public void addFirst(Predicate<T> predicate) {
        predicates.addFirst(predicate);
    }

    public void remove(Predicate<T> predicate) {
        predicates.remove(predicate);
    }

    public <P> void remove(Class<P> clazz) {
        predicates.removeIf(tPredicate -> tPredicate.getClass().equals(clazz));
    }
}
