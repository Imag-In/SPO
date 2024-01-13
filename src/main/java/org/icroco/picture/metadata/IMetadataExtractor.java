package org.icroco.picture.metadata;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import org.icroco.picture.model.GeoLocation;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IMetadataExtractor {
    Logger      log      = org.slf4j.LoggerFactory.getLogger(DefaultMetadataExtractor.class);
    GeoLocation NO_WHERE = new GeoLocation(190D, 190D);

    record MetadataDirectory(String name, Map<String, Object> entries) {
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
