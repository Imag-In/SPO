package org.icroco.picture.views.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.icroco.picture.model.MediaFile;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class MultiplePredicatesTest {


    ObservableList<MediaFile> files = FXCollections.observableArrayList();

    {
        files.add(MediaFile.builder().fileName("a.txt").build());
        files.add(MediaFile.builder().fileName("b.txt").build());
        files.add(MediaFile.builder().fileName("bb.txt").build());
        files.add(MediaFile.builder().fileName("c.txt").build());
        files.add(MediaFile.builder().fileName("ccc.txt").build());
    }

    @Test
    void should_return_true_when_empty() {
        var predicates = new MultiplePredicates<>();
        assertThat(files).hasSize(5);

        var filtered = new DynamicFilteredList<>(files);
        assertThat(files).hasSize(5);

        filtered.setPredicate(predicates);
        assertThat(filtered).hasSize(5);
    }

    @Test
    void should_filter() {
        var predicates = new MultiplePredicates<MediaFile>();
        var filtered   = new DynamicFilteredList<>(files);


        filtered.setPredicate(predicates);
        assertThat(filtered).hasSize(5);

        predicates.add(mf -> mf.fileName().contains("a"));
        assertThat(filtered).hasSize(1);
    }

    @Test
    void should_reset() {
        var predicates = new MultiplePredicates<MediaFile>();
        var filtered   = new DynamicFilteredList<>(files);


        filtered.setPredicate(predicates);
        assertThat(filtered).hasSize(5);

        predicates.add(mf -> mf.fileName().contains("a"));
        assertThat(filtered).hasSize(1);

        predicates.clear();
        assertThat(filtered).hasSize(5);
    }

    @Test
    void should_remove() {
        var predicates = new MultiplePredicates<MediaFile>();
        var filtered   = new DynamicFilteredList<>(files);


        filtered.setPredicate(predicates);
        assertThat(filtered).hasSize(5);

        Predicate<MediaFile> aPredicate = mf -> mf.fileName().contains("a");
        predicates.add(aPredicate);
        assertThat(filtered).hasSize(1);

        predicates.remove(aPredicate);
        assertThat(filtered).hasSize(5);

    }

    @Test
    void should_and_multiple_predicate() {
        var predicates = new MultiplePredicates<MediaFile>();
        var filtered   = new DynamicFilteredList<>(files);


        filtered.setPredicate(predicates);
        assertThat(filtered).hasSize(5);

        Predicate<MediaFile> p1 = mf -> !mf.fileName().contains("a");
        predicates.add(p1);
        assertThat(filtered).hasSize(4);

        Predicate<MediaFile> p2 = mf -> mf.fileName().length() > 5;
        predicates.add(p2);
        files.stream().filter(predicates).forEach(System.out::println);
        assertThat(filtered).hasSize(2);
    }
}