package org.icroco.picture.metadata;

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
import org.icroco.picture.persistence.KeywordRepository;
import org.icroco.picture.persistence.mapper.KeywordMapperImpl;
import org.icroco.picture.views.task.TaskService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class ApacheMetadataWritterTest {
    ApacheMetadataWriter writter = new ApacheMetadataWriter();
    IMetadataExtractor   reader  = new DefaultMetadataExtractor(new KeywordManager(Mockito.mock(KeywordRepository.class),
                                                                                   new KeywordMapperImpl()),
                                                                Mockito.mock(TaskService.class));

    @Test
    void should_write_original_date() throws URISyntaxException, ImagingException, IOException {
//        var image = getClass().getResource("./target/test/images/metadata/test.jpg");
//        var path  = Paths.get(image.toURI());
        System.out.println(ByteOrder.nativeOrder());
        var path = Path.of("./target/test-classes/images/metadata/test.jpg");

//        changeExifMetadata(Path.of("./target/test-classes/images/metadata/test-A.jpg").toFile(), Path.of("./target/test-classes/images/metadata/test-A.jpg").toFile());

        var header = reader.header(path);
        Assertions.assertThat(header).isPresent();
        Assertions.assertThat(header.get().orginalDate().toLocalDate()).isEqualTo(LocalDate.of(2023, 1, 15));

        System.out.println(header);
        writter.setOrignialDate(path, LocalDateTime.now());

        header = reader.header(path);
        Assertions.assertThat(header).isPresent();
        Assertions.assertThat(header.get().orginalDate().toLocalDate()).isEqualTo(LocalDate.now());

    }

    @Test
    @Disabled
    void printMetadata() {
        ApacheMetadaExtractor extractor = new ApacheMetadaExtractor();

        extractor.getAllByDirectory(Path.of("/Users/christophe/Pictures/foo/json/Imag'In-Icon_Only-128x128-FF.png"))
                 .forEach(d -> {
                     System.out.println("Dir: " + d.name());
                     d.entries().forEach((key, value) -> System.out.printf("   %s: %s", key, value));
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