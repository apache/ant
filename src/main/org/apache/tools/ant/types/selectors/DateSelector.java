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

package org.apache.tools.ant.types.selectors;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.TimeComparison;
import org.apache.tools.ant.util.FileUtils;

/**
 * Selector that chooses files based on their last modified date.
 *
 * @since 1.5
 */
public class DateSelector extends BaseExtendSelector {

    /** Key to used for parameterized custom selector */
    public static final String MILLIS_KEY = "millis";
    /** Key to used for parameterized custom selector */
    public static final String DATETIME_KEY = "datetime";
    /** Key to used for parameterized custom selector */
    public static final String CHECKDIRS_KEY = "checkdirs";
    /** Key to used for parameterized custom selector */
    public static final String GRANULARITY_KEY = "granularity";
    /** Key to used for parameterized custom selector */
    public static final String WHEN_KEY = "when";
    /** Key to used for parameterized custom selector */
    public static final String PATTERN_KEY = "pattern";

    /** Utilities used for file operations */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private long millis = -1;
    private String dateTime = null;
    private boolean includeDirs = false;
    private long granularity = FILE_UTILS.getFileTimestampGranularity();
    private String pattern;
    private TimeComparison when = TimeComparison.EQUAL;

    /**
     * @return a string describing this object
     */
    public String toString() {
        StringBuilder buf = new StringBuilder("{dateselector date: ");
        buf.append(dateTime);
        buf.append(" compare: ").append(when.getValue());
        buf.append(" granularity: ").append(granularity);
        if (pattern != null) {
            buf.append(" pattern: ").append(pattern);
        }
        buf.append("}");
        return buf.toString();
    }

    /**
     * Set the time; for users who prefer to express time in ms since 1970.
     *
     * @param millis the time to compare file's last modified date to,
     *        expressed in milliseconds.
     */
    public void setMillis(long millis) {
        this.millis = millis;
    }

    /**
     * Returns the millisecond value the selector is set for.
     * @return the millisecond value.
     */
    public long getMillis() {
        if (dateTime != null) {
            validate();
        }
        return millis;
    }

    /**
     * Sets the date. The user must supply it in MM/DD/YYYY HH:MM AM_PM format,
     * unless an alternate pattern is specified via the pattern attribute.
     *
     * @param dateTime a formatted date <code>String</code>.
     */
    public void setDatetime(String dateTime) {
        this.dateTime = dateTime;
        millis = -1;
    }

    /**
     * Set whether to check dates on directories.
     *
     * @param includeDirs whether to check the timestamp on directories.
     */
    public void setCheckdirs(boolean includeDirs) {
        this.includeDirs = includeDirs;
    }

    /**
     * Sets the number of milliseconds leeway we will give before we consider
     * a file not to have matched a date.
     * @param granularity the number of milliseconds leeway.
     */
    public void setGranularity(int granularity) {
        this.granularity = granularity;
    }

    /**
     * Sets the type of comparison to be done on the file's last modified
     * date.
     *
     * @param tcmp The comparison to perform, an EnumeratedAttribute.
     */
    public void setWhen(TimeComparisons tcmp) {
        setWhen((TimeComparison) tcmp);
    }

    /**
     * Set the comparison type.
     * @param t TimeComparison object.
     */
    public void setWhen(TimeComparison t) {
        when = t;
    }

    /**
     * Sets the pattern to be used for the SimpleDateFormat.
     *
     * @param pattern the pattern that defines the date format.
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /**
     * When using this as a custom selector, this method will be called.
     * It translates each parameter into the appropriate setXXX() call.
     *
     * @param parameters the complete set of parameters for this selector.
     */
    public void setParameters(Parameter... parameters) {
        super.setParameters(parameters);
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                String paramname = parameter.getName();
                if (MILLIS_KEY.equalsIgnoreCase(paramname)) {
                    try {
                        setMillis(Long.parseLong(parameter.getValue()));
                    } catch (NumberFormatException nfe) {
                        setError("Invalid millisecond setting "
                                + parameter.getValue());
                    }
                } else if (DATETIME_KEY.equalsIgnoreCase(paramname)) {
                    setDatetime(parameter.getValue());
                } else if (CHECKDIRS_KEY.equalsIgnoreCase(paramname)) {
                    setCheckdirs(Project.toBoolean(parameter.getValue()));
                } else if (GRANULARITY_KEY.equalsIgnoreCase(paramname)) {
                    try {
                        setGranularity(Integer.parseInt(parameter.getValue()));
                    } catch (NumberFormatException nfe) {
                        setError("Invalid granularity setting "
                                + parameter.getValue());
                    }
                } else if (WHEN_KEY.equalsIgnoreCase(paramname)) {
                    setWhen(new TimeComparison(parameter.getValue()));
                } else if (PATTERN_KEY.equalsIgnoreCase(paramname)) {
                    setPattern(parameter.getValue());
                } else {
                    setError("Invalid parameter " + paramname);
                }
            }
        }
    }

    /**
     * This is a consistency check to ensure the selector's required
     * values have been set.
     */
    public void verifySettings() {
        if (dateTime == null && millis < 0) {
            setError("You must provide a datetime or the number of "
                    + "milliseconds.");
        } else if (millis < 0 && dateTime != null) {
            String p = pattern == null ? "MM/dd/yyyy hh:mm a" : pattern;
            DateFormat df = pattern == null
                ? new SimpleDateFormat(p, Locale.US)
                : new SimpleDateFormat(p);

            try {
                setMillis(df.parse(dateTime).getTime());
                if (millis < 0) {
                    setError("Date of " + dateTime
                        + " results in negative milliseconds value"
                        + " relative to epoch (January 1, 1970, 00:00:00 GMT).");
                }
            } catch (ParseException pe) {
                setError("Date of " + dateTime
                        + " Cannot be parsed correctly. It should be in '"
                         + p + "' format.", pe);
            }
        }
    }

    /**
     * The heart of the matter. This is where the selector gets to decide
     * on the inclusion of a file in a particular fileset.
     *
     * @param basedir the base directory from which the scan is being performed.
     * @param filename is the name of the file to check.
     * @param file is a java.io.File object the selector can use.
     * @return whether the file is selected.
     */
    public boolean isSelected(File basedir, String filename, File file) {
        validate();
        return (file.isDirectory() && !includeDirs)
            || when.evaluate(file.lastModified(), millis, granularity);
    }

    /**
     * Enumerated attribute with the values for time comparison.
     */
    public static class TimeComparisons extends TimeComparison {
    }

}


