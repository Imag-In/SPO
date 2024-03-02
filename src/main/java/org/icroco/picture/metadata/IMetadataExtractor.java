package org.icroco.picture.metadata;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import org.apache.commons.lang3.StringUtils;
import org.icroco.picture.model.GeoLocation;
import org.slf4j.Logger;
import org.springframework.lang.NonNull;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.*;

public interface IMetadataExtractor {
    Logger      log      = org.slf4j.LoggerFactory.getLogger(DefaultMetadataExtractor.class);
    GeoLocation NO_WHERE = new GeoLocation(190D, 190D);

    record DirectorEntryKey(Integer id, String path) implements Comparable<DirectorEntryKey> {
        public DirectorEntryKey(Integer id, String path) {
            this.id = id == null ? 0xFFFF : id;
            this.path = StringUtils.isBlank(path) ? null : path;
        }

        static DirectorEntryKey ofId(Integer id) {
            return new DirectorEntryKey(id, null);
        }

        static DirectorEntryKey ofPath(String path) {
            return new DirectorEntryKey(null, path);
        }

        @Override
        public String toString() {
            if (path != null) {
                return path;
            }
            return String.format("Id: %d (0x%04x)", id, id);
        }

        @Override
        public int compareTo(@NonNull DirectorEntryKey o) {
            if (path != null) {
                return path.compareToIgnoreCase(o.path);
            }
            return id.compareTo(o.id);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            DirectorEntryKey that = (DirectorEntryKey) o;

            if (!Objects.equals(id, that.id)) {
                return false;
            }
            return Objects.equals(path, that.path);
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (path != null ? path.hashCode() : 0);
            return result;
        }
    }

    record DirectorEntryValue(String description, Object value) {
    }


    record MetadataDirectory(Class<?> clazz, String simpleName, Map<DirectorEntryKey, DirectorEntryValue> entries) {
        public MetadataDirectory {
            if (!(entries instanceof TreeMap)) {
                entries = new TreeMap<>(entries);
            }
        }

        public MetadataDirectory(Class<?> clazz, Map<DirectorEntryKey, DirectorEntryValue> entries) {
            this(clazz, clazz.getSimpleName(), entries);
        }
    }

    Map<String, Object> getAllInformation(Path path);

    List<MetadataDirectory> getAllByDirectory(Path path);


//    default Optional<Integer> orientation(Path path) {
//        try (var input = new BufferedInputStream(new FileInputStream(path.toFile()))) {
//            return orientation(input);
//        }
//        catch (IOException e) {
//            throw new RuntimeException("Path: " + path, e);
//        }
//    }

    //    Optional<Integer> orientation(InputStream input);
    Optional<Integer> orientation(Path path);


//    default Optional<MetadataHeader> header(Path path) {
//        try (var input = new BufferedInputStream(new FileInputStream(path.toFile()))) {
//            return header(path, input);
//        }
//        catch (Throwable t) {
//            log.warn("Cannot read header for file: {}, message: {}", path, t.getLocalizedMessage());
//            return Optional.empty();
//        }
//    }

    default boolean isFileTypeSupported(Path path) {
        try (var input = new BufferedInputStream(new FileInputStream(path.toFile()))) {
            return FileTypeDetector.detectFileType(input) != FileType.Unknown;
        } catch (Throwable t) {
            log.warn("Cannot read header for file: {}, message: {}", path, t.getLocalizedMessage());
            return false;
        }
    }

    Optional<MetadataHeader> header(final Path path);


}
