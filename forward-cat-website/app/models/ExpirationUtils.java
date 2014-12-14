package models;

import com.google.common.collect.ImmutableSet;
import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.joda.time.Seconds;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import play.i18n.Lang;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Collection of methods for dealing with time and expiration dates
 */
public class ExpirationUtils {

    // Durations
    private static final int UNCONFIRMED_PROXY_EXPIRATION_SEC = 60 * 30;
    private static final int MAX_PROXY_DURATION_DAYS = 15;
    private static final int INCREMENT_DAYS_ADDED = 5;

    private static final DateTimeFormatter DATE_PARSER = ISODateTimeFormat.dateTimeParser();
    private static final DateTimeFormatter DATE_FORMATTER = ISODateTimeFormat.dateTime();
    private static final ImmutableSet<Integer> VALID_DURATIONS = ImmutableSet.of(3, 5, 7);

    /**
     * Converts a {@link DateTime} to a {@link String}
     */
    public static String toStringValue(DateTime dateTime) {
        checkNotNull(dateTime);
        return dateTime.toString(DATE_FORMATTER);
    }

    /**
     * Converts a {@link String} to a {@link DateTime}
     */
    public static DateTime toDateTime(String stringValue) {
        checkNotNull(stringValue);
        return DATE_PARSER.parseDateTime(stringValue);
    }

    /**
     * Returns the number of whole seconds between now and the given datetime
     */
    public static int secondsTo(ReadableInstant instant) {
        checkNotNull(instant);
        return Seconds.secondsBetween(new DateTime(), instant).getSeconds();
    }

    /**
     * Formats a {@link ReadableInstant} with the given {@link Locale} in a user
     * friendly way
     */
    public static String formatInstant(ReadableInstant instant, Lang language) {
        Locale locale = language.toLocale();
        return DateTimeFormat.forStyle("SS").withLocale(locale).print(instant);
    }

    /**
     * Given an expiration time, returns the {@link DateTime} when the alert
     * should be sent
     */
    public static DateTime getAlertTime(DateTime expirationDateTime) {
        checkNotNull(expirationDateTime);
        return expirationDateTime.minusDays(1);
    }

    /**
     * Returns true if the given duration for a proxy is valid
     */
    public static boolean isValidDuration(Integer duration) {
        checkNotNull(duration);
        return VALID_DURATIONS.contains(duration);
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
