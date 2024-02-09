package org.icroco.picture.model;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Builder;
import lombok.Getter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Getter
public class MediaCollection {

    private final int                   id;
    private final Path                  path;
    private final Set<MediaFile>        medias;
    private final SimpleBooleanProperty connectedProperty;
    private final Set<Path>             subdir = new HashSet<>(32); // TODO: Replace by an ObservableList.

    @Builder
    public MediaCollection(int id, Path path, Set<MediaFile> medias, Boolean connected) {
        this.id = id;
        this.path = path.normalize();
        this.medias = medias;
        this.connectedProperty = new SimpleBooleanProperty(connected == null ? Boolean.FALSE : Boolean.TRUE);
        resetSubDir();
    }

    public void replaceMedias(Collection<MediaFile> mf) {
        medias.removeAll(mf);
        medias.addAll(mf);
    }

    @Override
    public String toString() {
        return STR."MediaCollection{mcId:\{id}, path:\{path}, files:\{medias.size()}\{'}'}";
    }

    public final int id() {
        return id;
    }

    public final Path path() {
        return path;
    }

    public final Set<MediaFile> medias() {
        return Collections.unmodifiableSet(medias);
    }

    public final boolean removeMediaFilesIf(Predicate<? super MediaFile> predicate) {
        var res = medias.removeIf(predicate);
        resetSubDir();
        return res;
    }

    public final void addAllMediaFiles(Collection<MediaFile> files) {
        medias.addAll(files);
        resetSubDir();
    }

    public final void removeAllMediaFiles(Collection<MediaFile> files) {
        medias.removeAll(files);
        resetSubDir();
    }

    public boolean isConnected() {
        return connectedProperty.get();
    }

    public void setConnected(boolean value) {
        Platform.runLater(() -> connectedProperty.set(value));
    }

    private void resetSubDir() {
        subdir.clear();
        subdir.addAll(medias.stream()
                            .map(MediaFile::getFullPath)
                            .map(Path::getParent)
                            .filter(Files::isDirectory)
                            .collect(Collectors.toSet()));
    }

    public ReadOnlyBooleanProperty connectedProperty() {
        return connectedProperty;
    }
}
