package org.icroco.picture.util;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class FileUtilTest {

    @Test
    void should_parse_date_time() {

        var dateTime = "IMG_20160805_150220.jpg";

        var matches = FileUtil.DATE_TINE_PATTERN_1.matcher(dateTime);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(matches).matches();

        soft.assertThat(matches.group("DATE")).isEqualTo("20160805");
        soft.assertThat(matches.group("YEAR")).isEqualTo("2016");
        soft.assertThat(matches.group("MONTH")).isEqualTo("08");
        soft.assertThat(matches.group("DAY")).isEqualTo("05");

        soft.assertThat(matches.group("TIME")).isEqualTo("150220");
        soft.assertThat(matches.group("HOUR")).isEqualTo("15");
        soft.assertThat(matches.group("MIN")).isEqualTo("02");
        soft.assertThat(matches.group("SEC")).isEqualTo("20");

        soft.assertAll();
    }

    @Test
    void should_parse_date_time_no_sec() {

        var dateTime = "IMG_20160805_1502.jpg";

        var matches = FileUtil.DATE_TINE_PATTERN_1.matcher(dateTime);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(matches).matches();

        soft.assertThat(matches.group("TIME")).isEqualTo("1502");
        soft.assertThat(matches.group("HOUR")).isEqualTo("15");
        soft.assertThat(matches.group("MIN")).isEqualTo("02");
        soft.assertThat(matches.group("SEC")).isNull();

        soft.assertAll();
    }

    @Test
    void should_parse_date_time_no_time() {

        var dateTime = "IMG_20160805.jpg";

        var matches = FileUtil.DATE_TINE_PATTERN_1.matcher(dateTime);

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(matches).matches();

        soft.assertThat(matches.group("DATE")).isEqualTo("20160805");
        soft.assertThat(matches.group("YEAR")).isEqualTo("2016");
        soft.assertThat(matches.group("MONTH")).isEqualTo("08");
        soft.assertThat(matches.group("DAY")).isEqualTo("05");

        soft.assertAll();
    }

    @Test
    void should_get_datetime() {
        var path = Path.of("IMG_20160805_150220.jpg");

        var dateTime = FileUtil.extractDateTime(path);

        assertThat(dateTime).isPresent()
                            .get()
                            .extracting(LocalDateTime::toLocalDate)
                            .isEqualTo(LocalDate.of(2016, 8, 5));

        assertThat(dateTime).isPresent()
                            .get()
                            .extracting(LocalDateTime::toLocalTime)
                            .isEqualTo(LocalTime.of(15, 2, 20));
    }

    @Test
    void should_get_date_only() {
        var path = Path.of("IMG_20160805_blabla.jpg");

        var dateTime = FileUtil.extractDateTime(path);

        assertThat(dateTime).isPresent()
                            .get()
                            .extracting(LocalDateTime::toLocalDate)
                            .isEqualTo(LocalDate.of(2016, 8, 5));

        assertThat(dateTime).isPresent()
                            .get()
                            .extracting(LocalDateTime::toLocalTime)
                            .isEqualTo(LocalTime.of(0, 0, 0));
    }

    @Test
    void should_get_datetime_no_sec() {
        var path = Path.of("IMG_20160805_1345_blabla.jpg");

        var dateTime = FileUtil.extractDateTime(path);

        assertThat(dateTime).isPresent()
                            .get()
                            .extracting(LocalDateTime::toLocalDate)
                            .isEqualTo(LocalDate.of(2016, 8, 5));

        assertThat(dateTime).isPresent()
                            .get()
                            .extracting(LocalDateTime::toLocalTime)
                            .isEqualTo(LocalTime.of(13, 45, 0));
    }

    @Test
    void should_get_no_datetime_() {
        var path = Path.of("IMG_201605_1345_blabla.jpg");

        var dateTime = FileUtil.extractDateTime(path);

        assertThat(dateTime).isEmpty();
    }

    @Test
    void should_get_datetime_format_fr() {
        var path = Path.of("IMG_05082016_150220.jpg");

        var dateTime = FileUtil.extractDateTime(path);

        assertThat(dateTime).isPresent()
                            .get()
                            .extracting(LocalDateTime::toLocalDate)
                            .isEqualTo(LocalDate.of(2016, 8, 5));

        assertThat(dateTime).isPresent()
                            .get()
                            .extracting(LocalDateTime::toLocalTime)
                            .isEqualTo(LocalTime.of(15, 2, 20));
    }

    @Test
    void should_get_date_only_format_fr() {
        var path = Path.of("IMG_05082016_blabla.jpg");

        var dateTime = FileUtil.extractDateTime(path);

        assertThat(dateTime).isPresent()
                            .get()
                            .extracting(LocalDateTime::toLocalDate)
                            .isEqualTo(LocalDate.of(2016, 8, 5));

        assertThat(dateTime).isPresent()
                            .get()
                            .extracting(LocalDateTime::toLocalTime)
                            .isEqualTo(LocalTime.of(0, 0, 0));
    }

}