/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.util;

import java.text.ChoiceFormat;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper methods to deal with date/time formatting with a specific
 * defined format (<a href="https://www.w3.org/TR/NOTE-datetime">ISO8601</a>)
 * or a correct pluralization of elapsed time in minutes and seconds.
 *
 * @since Ant 1.5
 *
 */
public final class DateUtils {

    private static final int ONE_SECOND = 1000;
    private static final int ONE_MINUTE = 60;
    private static final int ONE_HOUR = 60;
    private static final int TEN = 10;
    /**
     * ISO8601-like pattern for date-time. It does not support timezone.
     *  <code>yyyy-MM-ddTHH:mm:ss</code>
     */
    public static final String ISO8601_DATETIME_PATTERN
            = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * ISO8601-like pattern for date. <code>yyyy-MM-dd</code>
     */
    public static final String ISO8601_DATE_PATTERN
            = "yyyy-MM-dd";

    /**
     * ISO8601-like pattern for time.  <code>HH:mm:ss</code>
     */
    public static final String ISO8601_TIME_PATTERN
            = "HH:mm:ss";

    /**
     * Format used for SMTP (and probably other) Date headers.
     * @deprecated DateFormat is not thread safe, and we cannot guarantee that
     * some other code is using the format in parallel.
     * Deprecated since ant 1.8
     */
    @Deprecated
    public static final DateFormat DATE_HEADER_FORMAT
        = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ", Locale.US);

    private static final DateFormat DATE_HEADER_FORMAT_INT =
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ", Locale.US);

// code from Magesh moved from DefaultLogger and slightly modified
    private static final MessageFormat MINUTE_SECONDS
            = new MessageFormat("{0}{1}");

    private static final double[] LIMITS = {0, 1, 2};

    private static final String[] MINUTES_PART = {"", "1 minute ", "{0,number,###############} minutes "};

    private static final String[] SECONDS_PART = {"0 seconds", "1 second", "{1,number} seconds"};

    private static final ChoiceFormat MINUTES_FORMAT =
            new ChoiceFormat(LIMITS, MINUTES_PART);

    private static final ChoiceFormat SECONDS_FORMAT =
            new ChoiceFormat(LIMITS, SECONDS_PART);

    /**
     * Provides a thread-local US-style date format. Exactly as used by
     * {@code <touch>}, to minute precision:
     * {@code SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US)}
     * @since Ant 1.10.2
     */
    public static final ThreadLocal<DateFormat> EN_US_DATE_FORMAT_MIN =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US));

    /**
     * Provides a thread-local US-style date format. Exactly as used by
     * {@code <touch>}, to second precision:
     * {@code SimpleDateFormat("MM/dd/yyyy hh:mm:ss a", Locale.US)}
     * @since Ant 1.10.2
     */
    public static final ThreadLocal<DateFormat> EN_US_DATE_FORMAT_SEC =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a", Locale.US));

    static {
        MINUTE_SECONDS.setFormat(0, MINUTES_FORMAT);
        MINUTE_SECONDS.setFormat(1, SECONDS_FORMAT);
    }

    /** private constructor */
    private DateUtils() {
    }


    /**
     * Format a date/time into a specific pattern.
     * @param date the date to format expressed in milliseconds.
     * @param pattern the pattern to use to format the date.
     * @return the formatted date.
     */
    public static String format(long date, String pattern) {
        return format(new Date(date), pattern);
    }


    /**
     * Format a date/time into a specific pattern.
     * @param date the date to format expressed in milliseconds.
     * @param pattern the pattern to use to format the date.
     * @return the formatted date.
     */
    public static String format(Date date, String pattern) {
        DateFormat df = createDateFormat(pattern);
        return df.format(date);
    }


    /**
     * Format an elapsed time into a pluralization correct string.
     * It is limited only to report elapsed time in minutes and
     * seconds and has the following behavior.
     * <ul>
     * <li>minutes are not displayed when 0. (ie: "45 seconds")</li>
     * <li>seconds are always displayed in plural form (ie "0 seconds" or
     * "10 seconds") except for 1 (ie "1 second")</li>
     * </ul>
     * @param millis the elapsed time to report in milliseconds.
     * @return the formatted text in minutes/seconds.
     */
    public static String formatElapsedTime(long millis) {
        long seconds = millis / ONE_SECOND;
        long minutes = seconds / ONE_MINUTE;
        return MINUTE_SECONDS.format(new Object[]{minutes, seconds % ONE_MINUTE});
    }

    /**
     * return a lenient date format set to GMT time zone.
     * @param pattern the pattern used for date/time formatting.
     * @return the configured format for this pattern.
     */
    private static DateFormat createDateFormat(String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        sdf.setTimeZone(gmt);
        sdf.setLenient(true);
        return sdf;
    }

    /**
     * Calculate the phase of the moon for a given date.
     *
     * <p>Code heavily influenced by hacklib.c in <a
     * href="https://www.nethack.org/">Nethack</a></p>
     *
     * <p>The Algorithm:
     *
     * <pre>
     * moon period = 29.53058 days ~= 30, year = 365.2422 days
     *
     * days moon phase advances on first day of year compared to preceding year
     *  = 365.2422 - 12 * 29.53058 ~= 11
     *
     * years in Metonic cycle (time until same phases fall on the same days of
     *  the month) = 18.6 ~= 19
     *
     * moon phase on first day of year (epact) ~= (11*(year%19) + 18) % 30
     *  (18 as initial condition for 1900)
     *
     * current phase in days = first day phase + days elapsed in year
     *
     * 6 moons ~= 177 days
     * 177 ~= 8 reported phases * 22
     * + 11 / 22 for rounding
     * </pre>
     *
     * @param cal the calendar.
     *
     * @return The phase of the moon as a number between 0 and 7 with
     *         0 meaning new moon and 4 meaning full moon.
     *
     * @since 1.2, Ant 1.5
     */
    public static int getPhaseOfMoon(Calendar cal) {
        // CheckStyle:MagicNumber OFF
        int dayOfTheYear = cal.get(Calendar.DAY_OF_YEAR);
        int yearInMetonicCycle = ((cal.get(Calendar.YEAR) - 1900) % 19) + 1;
        int epact = (11 * yearInMetonicCycle + 18) % 30;
        if ((epact == 25 && yearInMetonicCycle > 11) || epact == 24) {
            epact++;
        }
        return (((((dayOfTheYear + epact) * 6) + 11) % 177) / 22) & 7;
        // CheckStyle:MagicNumber ON
    }

    /**
     * Returns the current Date in a format suitable for a SMTP date
     * header.
     * @return the current date.
     * @since Ant 1.5.2
     */
    public static String getDateForHeader() {
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        int offset = tz.getOffset(cal.get(Calendar.ERA),
                                  cal.get(Calendar.YEAR),
                                  cal.get(Calendar.MONTH),
                                  cal.get(Calendar.DAY_OF_MONTH),
                                  cal.get(Calendar.DAY_OF_WEEK),
                                  cal.get(Calendar.MILLISECOND));
        StringBuilder tzMarker = new StringBuilder(offset < 0 ? "-" : "+");
        offset = Math.abs(offset);
        int hours = offset / (ONE_HOUR * ONE_MINUTE * ONE_SECOND);
        int minutes = offset / (ONE_MINUTE * ONE_SECOND) - ONE_HOUR * hours;
        if (hours < TEN) {
            tzMarker.append("0");
        }
        tzMarker.append(hours);
        if (minutes < TEN) {
            tzMarker.append("0");
        }
        tzMarker.append(minutes);
        synchronized (DATE_HEADER_FORMAT_INT) {
            return DATE_HEADER_FORMAT_INT.format(cal.getTime()) + tzMarker.toString();
        }
    }

    /**
     * Parses the string in a format suitable for a SMTP date header.
     *
     * @param datestr string to be parsed
     *
     * @return a java.util.Date object as parsed by the format.
     * @exception ParseException if the supplied string cannot be parsed by
     * this pattern.
     * @since Ant 1.8.0
     */
    public static Date parseDateFromHeader(String datestr) throws ParseException {
        synchronized (DATE_HEADER_FORMAT_INT) {
            return DATE_HEADER_FORMAT_INT.parse(datestr);
        }
    }

    /**
     * Parse a string as a datetime using the ISO8601_DATETIME format which is
     * <code>yyyy-MM-dd'T'HH:mm:ss</code>
     *
     * @param datestr string to be parsed
     *
     * @return a java.util.Date object as parsed by the format.
     * @exception ParseException if the supplied string cannot be parsed by
     * this pattern.
     * @since Ant 1.6
     */
    public static Date parseIso8601DateTime(String datestr)
        throws ParseException {
        return new SimpleDateFormat(ISO8601_DATETIME_PATTERN).parse(datestr);
    }

    /**
     * Parse a string as a date using the ISO8601_DATE format which is
     * <code>yyyy-MM-dd</code>
     *
     * @param datestr string to be parsed
     *
     * @return a java.util.Date object as parsed by the format.
     * @exception ParseException if the supplied string cannot be parsed by
     * this pattern.
     * @since Ant 1.6
     */
    public static Date parseIso8601Date(String datestr) throws ParseException {
        return new SimpleDateFormat(ISO8601_DATE_PATTERN).parse(datestr);
    }

    /**
     * Parse a string as a date using the either the ISO8601_DATETIME
     * or ISO8601_DATE formats.
     *
     * @param datestr string to be parsed
     *
     * @return a java.util.Date object as parsed by the formats.
     * @exception ParseException if the supplied string cannot be parsed by
     * either of these patterns.
     * @since Ant 1.6
     */
    public static Date parseIso8601DateTimeOrDate(String datestr)
        throws ParseException {
        try {
            return parseIso8601DateTime(datestr);
        } catch (ParseException px) {
            return parseIso8601Date(datestr);
        }
    }

    private static final ThreadLocal<DateFormat> iso8601WithTimeZone =
            ThreadLocal.withInitial(() -> {
              // An arbitrary easy-to-read format to normalize to.
              return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");
            });
    private static final Pattern iso8601normalizer = Pattern.compile(
        "^(\\d{4,}-\\d{2}-\\d{2})[Tt ]" +           // yyyy-MM-dd
        "(\\d{2}:\\d{2}(:\\d{2}(\\.\\d{3})?)?) ?" + // HH:mm:ss.SSS
        "(?:Z|([+-]\\d{2})(?::?(\\d{2}))?)?$");     // Z

    /**
     * Parse a lenient ISO 8601, ms since epoch, or {@code <touch>}-style date.
     * That is:
     * <ul>
     * <li>Milliseconds since 1970-01-01 00:00</li>
     * <li><code>YYYY-MM-DD{T| }HH:MM[:SS[.SSS]][ ][&plusmn;ZZ[[:]ZZ]]</code></li>
     * <li><code>MM/DD/YYYY HH:MM[:SS] {AM|PM}</code></li></ul>
     * where {a|b} indicates that you must choose one of a or b, and [c]
     * indicates that you may use or omit c. &plusmn;ZZZZ is the timezone offset, and
     * may be literally "Z" to mean GMT.
     *
     * @param dateStr String
     * @return Date
     * @throws ParseException if date string does not match ISO 8601
     * @since Ant 1.10.2
     */
    public static Date parseLenientDateTime(String dateStr) throws ParseException {
        try {
            return new Date(Long.parseLong(dateStr));
        } catch (NumberFormatException ignored) {
        }

        try {
            return EN_US_DATE_FORMAT_MIN.get().parse(dateStr);
        } catch (ParseException ignored) {
        }

        try {
           return EN_US_DATE_FORMAT_SEC.get().parse(dateStr);
        } catch (ParseException ignored) {
        }

        Matcher m = iso8601normalizer.matcher(dateStr);
        if (!m.find()) {
            throw new ParseException(dateStr, 0);
        }
        String normISO = m.group(1) + " "
            + (m.group(3) == null ? m.group(2) + ":00" : m.group(2))
            + (m.group(4) == null ? ".000 " : " ")
            + (m.group(5) == null ? "+00" : m.group(5))
            + (m.group(6) == null ? "00" : m.group(6));
        return iso8601WithTimeZone.get().parse(normISO);
    }
}
