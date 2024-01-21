package org.icroco.picture.model;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

@Getter
public class MediaCollection {

    private final int                       id;
    private final Path                      path;
    private final Set<MediaFile>            medias;
    private final Set<MediaCollectionEntry> subPaths;
    private final SimpleBooleanProperty     connectedProperty;

    @Builder
    public MediaCollection(int id, Path path, Set<MediaFile> medias, Set<MediaCollectionEntry> subPaths, Boolean connected) {
        this.id = id;
        this.path = path.normalize();
        this.medias = medias;
        this.subPaths = subPaths;
        this.connectedProperty = new SimpleBooleanProperty(connected == null ? Boolean.FALSE : Boolean.TRUE);
    }

    public void replaceMedias(Collection<MediaFile> mf) {
        medias.removeAll(mf);
        medias.addAll(mf);
    }

    public void replaceSubPaths(Collection<MediaCollectionEntry> sb) {
        subPaths.removeAll(sb);
        subPaths.addAll(sb);
    }

    @Override
    public String toString() {
        return "MediaCollection{" +
               "id:" + id +
               ", path:" + path +
               ", subDir:" + subPaths.size() +
               ", files:" + medias.size() +
               '}';
    }

    public final int id() {
        return id;
    }

    public final Path path() {
        return path;
    }

    public final Set<MediaCollectionEntry> subPaths() {
        return subPaths;
    }

    public final Set<MediaFile> medias() {
        return medias;
    }

    public boolean isConnected() {
        return connectedProperty.get();
    }

    public void setConnected(boolean value) {
        Platform.runLater(() -> connectedProperty.set(value));
    }

    public ReadOnlyBooleanProperty connectedProperty() {
        return connectedProperty;
    }
}
