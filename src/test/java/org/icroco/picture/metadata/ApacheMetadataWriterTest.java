package org.icroco.picture.metadata;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.assertj.core.api.Assertions;
import org.icroco.picture.model.ERotation;
import org.icroco.picture.model.Keyword;
import org.icroco.picture.thumbnail.IThumbnailGenerator;
import org.icroco.picture.thumbnail.ImgscalrGenerator;
import org.icroco.picture.views.task.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class ApacheMetadataWriterTest {
    ApacheMetadataWriter writer = new ApacheMetadataWriter();

    IKeywordManager kwmMock = new InMemoryKeywordManager();

    IMetadataExtractor reader = new DefaultMetadataExtractor(kwmMock,
                                                             Mockito.mock(TaskService.class));

    @BeforeEach
    void beforeAll() {
//        Mockito.reset(kwmMock);
    }

    @Test
    void should_write_original_date() throws URISyntaxException, ImagingException, IOException {
//        var image = getClass().getResource("./target/test/images/metadata/test.jpg");
//        var path  = Paths.get(image.toURI());
        System.out.println(ByteOrder.nativeOrder());
        var original = Path.of("./target/test-classes/images/metadata/test.jpg");
        var tmpPath  = Paths.get(original.getParent().toString(), STR."kw_\{original.getFileName().toString()}");

//        writer.setOrignialDate(original, LocalDateTime.of(2023, 1, 15, 12, 0, 0));
        try {
            Files.copy(original, tmpPath);

            var header = reader.header(tmpPath);
            Assertions.assertThat(header).isPresent();
            Assertions.assertThat(header.get().orginalDate().toLocalDate()).isEqualTo(LocalDate.of(2023, 1, 15));

            System.out.println(header);
            LocalDateTime now = header.get().orginalDate().plusYears(1);
            writer.setOrignialDate(tmpPath, now);

            header = reader.header(tmpPath);

            System.out.println(header);
            Assertions.assertThat(header).isPresent();
            Assertions.assertThat(header.get().orginalDate().toLocalDate()).isEqualTo(now.toLocalDate());
        } finally {
            Files.deleteIfExists(tmpPath);
        }
    }

    @Test
    void addKeywords() throws IOException {
        var original = Path.of("src/test/resources/images/metadata/keywords/update_keywords.jpg");
        var tmpPath = Paths.get(original.getParent().toString(), STR."tmp_\{original.getFileName().toString()}");
        try {
            Files.copy(original, tmpPath);

            var header = reader.header(tmpPath);


            Assertions.assertThat(header).isPresent();
            Assertions.assertThat(header.get().keywords())
                      .extracting(Keyword::name).containsExactlyInAnyOrder("bar", "foo");

            writer.addKeywords(tmpPath, Set.of(Keyword.builder().name("42").build()));

            header = reader.header(tmpPath);

            Assertions.assertThat(header.get().keywords())
                      .extracting(Keyword::name).containsExactlyInAnyOrder("bar", "42", "foo");
        } finally {
            Files.deleteIfExists(tmpPath);
        }
    }

    @Test
    void changeThumbnail() throws IOException {
        var original = Path.of("src/test/resources/images/metadata/thumbnail/update_thumbnail.jpg");
        var tmpPath  = Paths.get(original.getParent().toString(), STR."tmp_\{original.getFileName().toString()}");
        try {
            Files.deleteIfExists(tmpPath);
            Files.copy(original, tmpPath);

            IThumbnailGenerator
                    generator =
                    new ImgscalrGenerator(new DefaultMetadataExtractor(new InMemoryKeywordManager(), Mockito.mock(TaskService.class)));
            System.out.println(Files.size(tmpPath));
            var originalThnumbail = generator.extractThumbnail(tmpPath);
            System.out.println(Files.size(tmpPath));

            Assertions.assertThat(originalThnumbail).isNotNull();

            byte[] bytes = Files.readAllBytes(Path.of("src/test/resources/images/metadata/thumbnail/dummy.jpg"));
            System.out.println(STR."New thumbnail size: \{bytes.length}");
            writer.setThumbnail(tmpPath, bytes);

            var modifiedThnumbail = generator.extractThumbnail(tmpPath);
            System.out.println(getRawImage(modifiedThnumbail.getImage()).length);

//            Assertions.assertThat(getRawImage(modifiedThnumbail.getImage())).isEqualTo(bytes);
        } finally {
            Files.deleteIfExists(tmpPath);
        }
    }

    public static byte[] getRawImage(Image image) {
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();

// Create a new Byte Buffer, but we'll use BGRA (1 byte for each channel) //

        byte[] buf = new byte[w * h * 4];

/* Since you can get the output in whatever format with a WritablePixelFormat,
  we'll use an already created one for ease-of-use. */

        image.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getByteBgraInstance(), buf, 0, w * 4);
        return buf;
    }

    @Test
    void changeOrientation() throws IOException {
        var original = Path.of("src/test/resources/images/metadata/thumbnail/update_thumbnail.jpg");
        var tmpPath  = Paths.get(original.getParent().toString(), STR."tmp_\{original.getFileName().toString()}");
        try {
            Files.copy(original, tmpPath);

            var header = reader.header(tmpPath);


            Assertions.assertThat(header).isPresent();
            Assertions.assertThat(header.get().orientation())
                      .isEqualTo((short) 1);

            writer.setOrientation(tmpPath, ERotation.CW_90);

            header = reader.header(tmpPath);

            Assertions.assertThat(header.get().orientation()).isEqualTo(ERotation.CW_90.getOrientation());
        } finally {
            Files.deleteIfExists(tmpPath);
        }
    }

    @Test
    @Disabled
    void printMetadata() {
        ApacheMetadaExtractor extractor = new ApacheMetadaExtractor();
//        var path = Path.of("/Users/christophe/Pictures/foo/json/Imag'In-Icon_Only-128x128-FF.png");
        var path = Path.of("src/test/resources/images/metadata/keywords/update_keywords.jpg");

//        reader.getAllByDirectory(path).forEach(d -> {
//            System.out.println(STR."Directory: \{d.name()}");
//            d.entries().entrySet().forEach(e -> System.out.println(STR."   \{e.getKey()}: \{e.getValue()}"));
//        });
        extractor.getAllByDirectory(path)
                 .forEach(d -> {
                     System.out.println(STR."Dir: \{d.name()}");
                     d.entries().forEach((key, value) -> System.out.printf("   %s: %s%n", key, value));
                 });
    }

    public void changeExifMetadata(final File jpegImageFile, final File dst) throws IOException, ImagingException, ImagingException {
        byte[] source = Files.readAllBytes(jpegImageFile.toPath());
        try (FileOutputStream fos = new FileOutputStream(dst);
             OutputStream os = new BufferedOutputStream(fos)) {

            TiffOutputSet outputSet = null;

            // note that metadata might be null if no metadata is found.
            final ImageMetadata     metadata     = Imaging.getMetadata(source);
            final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            if (null != jpegMetadata) {
                // note that exif might be null if no Exif metadata is found.
                final TiffImageMetadata exif = jpegMetadata.getExif();

                if (null != exif) {
                    // TiffImageMetadata class is immutable (read-only).
                    // TiffOutputSet class represents the Exif data to write.
                    //
                    // Usually, we want to update existing Exif metadata by
                    // changing
                    // the values of a few fields, or adding a field.
                    // In these cases, it is easiest to use getOutputSet() to
                    // start with a "copy" of the fields read from the image.
                    outputSet = exif.getOutputSet();
                }
            }

            // if file does not contain any exif metadata, we create an empty
            // set of exif metadata. Otherwise, we keep all of the other
            // existing tags.
            if (null == outputSet) {
                outputSet = new TiffOutputSet();
            }

            {
                // Example of how to add a field/tag to the output set.
                //
                // Note that you should first remove the field/tag if it already
                // exists in this directory, or you may end up with duplicate
                // tags. See above.
                //
                // Certain fields/tags are expected in certain Exif directories;
                // Others can occur in more than one directory (and often have a
                // different meaning in different directories).
                //
                // TagInfo constants often contain a description of what
                // directories are associated with a given tag.
                //
                final TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
                // make sure to remove old value if present (this method will
                // not fail if the tag does not exist).
                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_APERTURE_VALUE);
                exifDirectory.add(ExifTagConstants.EXIF_TAG_APERTURE_VALUE, new RationalNumber(3, 10));
                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
                exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL,
                                  DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss").format(LocalDateTime.now()));


            }

            {
                // Example of how to add/update GPS info to output set.

                // New York City
                final double longitude = -74.0; // 74 degrees W (in Degrees East)
                final double latitude  = 40 + 43 / 60.0; // 40 degrees N (in Degrees
                // North)

                outputSet.setGPSInDegrees(longitude, latitude);
            }

            // printTagValue(jpegMetadata, TiffConstants.TIFF_TAG_DATE_TIME);

            new ExifRewriter().updateExifMetadataLossless(source, os, outputSet);
        }
    }


}