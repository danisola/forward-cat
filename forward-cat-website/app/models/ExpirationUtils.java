package models;

import play.i18n.Lang;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DecimalStyle;
import java.util.Date;
import java.util.Locale;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

/**
 * Collection of methods for dealing with time and expiration dates
 */
public class ExpirationUtils {

    // Durations
    private static final int UNCONFIRMED_PROXY_EXPIRATION_SEC = 60 * 30;
    private static final int MAX_PROXY_DURATION_DAYS = 15;
    private static final int INCREMENT_DAYS_ADDED = 5;

    /**
     * Formats a {@link Date} with the given {@link Locale} in a user
     * friendly way
     */
    public static String formatInstant(Date date, Lang language) {
        Locale locale = language.toLocale();
        return ISO_OFFSET_DATE_TIME.withDecimalStyle(DecimalStyle.of(locale)).format(toZonedDateTime(date));
    }

    public static Date toDate(ZonedDateTime dateTime) {
        return Date.from(dateTime.toInstant());
    }

    public static ZonedDateTime toZonedDateTime(Date date) {
        return ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    public static ZonedDateTime now() {
        return ZonedDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Returns the number of days that a proxy can be active
     */
    public static int getMaxProxyDuration() {
        return MAX_PROXY_DURATION_DAYS;
    }

    /**
     * Returns the number of days that a proxy can be extended
     */
    public static int getIncrementDaysAdded() {
        return INCREMENT_DAYS_ADDED;
    }

    /**
     * Returns the number of seconds that an unconfirmed proxy will last
     */
    public static int getUnconfirmedProxyDuration() {
        return UNCONFIRMED_PROXY_EXPIRATION_SEC;
    }

    private ExpirationUtils() {
        // Non-instantiable
    }
}
