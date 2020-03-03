package service;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.log4j.Logger;
import utilities.TimestampFileAppender;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

@CommonsLog
public class TimeService {

    private static final DateTimeFormatter FORMATTER_ONLY_DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter FORMATTER_ONLY_DAY_IN_STR = DateTimeFormatter.ofPattern("ddMMyyyy");

    private final Properties appProps;
    private final ZoneId zoneId;
    private final TimestampFileAppender timestampFileAppender;

    public TimeService(Properties appProps) {
        this.appProps = appProps;
        this.timestampFileAppender = (TimestampFileAppender) Logger.getRootLogger().getAppender("rollingFile");
        this.zoneId = ZoneId.of(getTimeZone());
    }

    private String getTimeZone() {
        String timeZone = timestampFileAppender.getTimeZone();
        if (timeZone == null || timeZone.isBlank()) {
            return "UTC+05:30";
        } else return timeZone.trim();
    }

    public String getCheckDay() {
        String checkDateStr = appProps.getProperty("candle-validation.checkingDate");
        log.info("CheckDate from props = " + checkDateStr);
        if (checkDateStr != null && !checkDateStr.isBlank()) {
            return LocalDate.parse(checkDateStr, FORMATTER_ONLY_DAY).format(FORMATTER_ONLY_DAY_IN_STR);
        }
        LocalDate localDate = LocalDate.now(zoneId);
        String fileNameDate = localDate.format(FORMATTER_ONLY_DAY_IN_STR);
        log.info("FileNameDate = " + fileNameDate);
        return fileNameDate;
    }

    public String getLogFileName() {
        return this.timestampFileAppender.getFileName();
    }

}
