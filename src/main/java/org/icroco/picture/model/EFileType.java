package org.icroco.picture.model;

import com.drew.imaging.FileType;
import lombok.Getter;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * At some point should be related to {@link FileType}
 */
public enum EFileType {
    JPEG(0, "Joint Photographic Experts Group", true, true, "image/jpeg", "jpg", "jpeg", "jpe"),
    PNG(1, "Portable Network Graphics", true, false, "image/png", "png"),
    TIFF(2, "Tagged Image File Format", false, false, "image/tiff", "tiff", "tif"),
    PSD(3, "Photoshop Document", false, false, "image/vnd.adobe.photoshop", "psd"),
    BMP(4, "Device Independent Bitmap", false, false, "image/bmp", "bmp"),
    GIF(5, "Graphics Interchange Format", false, false, "image/gif", "gif"),
    ICO(6, "Windows Icon", false, false, "image/x-icon", "ico"),
    PCX(7, "PiCture eXchange", false, false, "image/x-pcx", "pcx"),
    RIFF(8, "Resource Interchange File Format", false, false, "unknown/riff", "riff"),
    WAV(9, "Waveform Audio File Format", false, false, "audio/vnd.wave", "wav", "wave"),
    AVI(10, "Audio Video Interleaved", false, false, "video/vnd.avi", "avi"),
    WEBP(11, "WebP", false, false, "image/webp", "webp"),
    QUICKTIME(12, "QuickTime Movie", false, false, "video/quicktime", "mov", "qt"),
    MP4(13, "MPEG-4 Part 14", false, false, "video/mp4", "mp4", "m4a", "m4p", "m4b", "m4r", "m4v"),
    HEIF(14, "High Efficiency Image File Format", false, false, "image/heif", "heif", "heic"),
    EPS(15, "Encapsulated PostScript", false, false, "application/postscript", "eps", "epsf", "epsi"),
    MP3(16, "MPEG Audio Layer III", false, false, "audio/mpeg", "mp3"),
    UNKOWN(Short.MIN_VALUE, "Unknown", false, false, "images/unknown");

    @Getter
    private final short        id;
    @Getter
    private final String       description;
    private final boolean      canReadMetadata;
    private final boolean      canWriteMetadata;
    @Getter
    private final String       mimeType;
    @Getter
    private final List<String> extensions;

    EFileType(int id, String description, boolean canReadMetadata, boolean canWriteMetadata, String mimeType, String... extensions) {
        this.id = (short) id;
        this.description = description;
        this.canReadMetadata = canReadMetadata;
        this.canWriteMetadata = canWriteMetadata;
        this.mimeType = mimeType;
        this.extensions = Arrays.stream(extensions).sorted().distinct().toList();
    }

    public final static Pattern SUPPORTED_EXT = Pattern.compile(STR.".*\\.(\{Arrays.stream(EFileType.values())
                                                                                   .filter(e -> e.canReadMetadata)
                                                                                   .flatMap(e -> e.extensions.stream())
                                                                                   .collect(joining("|"))})$",
                                                                Pattern.CASE_INSENSITIVE);

    private final static Map<Short, EFileType>  shortToType    = Arrays.stream(EFileType.values())
                                                                       .collect(Collectors.toMap(EFileType::getId,
                                                                                                 Function.identity()));
    private final static Map<String, EFileType> mimeTypeToType = Arrays.stream(EFileType.values())
                                                                       .collect(Collectors.toMap(EFileType::getMimeType,
                                                                                                 Function.identity()));

    private final static Set<EFileType> READ_METADATA  = Arrays.stream(EFileType.values())
                                                               .filter(e -> e.canReadMetadata)
                                                               .collect(Collectors.toSet());
    private final static Set<EFileType> WRITE_METADATA = Arrays.stream(EFileType.values())
                                                               .filter(e -> e.canWriteMetadata)
                                                               .collect(Collectors.toSet());

    public boolean canReadMetadata() {
        return canReadMetadata;
    }

    public boolean canWriteMetadata() {
        return canWriteMetadata;
    }

    public static EFileType fromId(int id) {
        return shortToType.getOrDefault((short) id, EFileType.UNKOWN);
    }

    public static EFileType fromMimetype(String mimetype) {
        return mimeTypeToType.getOrDefault(mimetype, EFileType.UNKOWN);
    }


    public static boolean isSupportedExtension(Path path) {
        return SUPPORTED_EXT.matcher(path.getFileName().toString()).matches();
    }
}
