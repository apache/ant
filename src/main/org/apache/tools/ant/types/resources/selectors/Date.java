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
package org.apache.tools.ant.types.resources.selectors;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.TimeComparison;
import org.apache.tools.ant.util.FileUtils;

/**
 * Date ResourceSelector.  Based on the date FileSelector, with the most
 * notable difference being the lack of support for the includedirs attribute.
 * It is recommended that the effect of includeDirs = "false" be achieved for
 * resources by enclosing a "dir" Type ResourceSelector and a Date
 * ResourceSelector in an Or ResourceSelector.
 * @since Ant 1.7
 */
public class Date implements ResourceSelector {
    private static final String MILLIS_OR_DATETIME
        = "Either the millis or the datetime attribute must be set.";
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private Long millis = null;
    private String dateTime = null;
    private String pattern = null;
    private TimeComparison when = TimeComparison.EQUAL;
    private long granularity = FILE_UTILS.getFileTimestampGranularity();

    /**
     * Set the date/time in milliseconds since 1970.
     * @param m the number of millis.
     */
    public synchronized void setMillis(long m) {
        millis = m;
    }

    /**
     * Get the date/time in ms.
     * @return long number of millis since 1970.
     */
    public synchronized long getMillis() {
        return millis == null ? -1L : millis;
    }

    /**
     * Set the date and time as a String.
     * @param s the date and time to use.
     */
    public synchronized void setDateTime(String s) {
        dateTime = s;
        millis = null;
    }

    /**
     * Get the date and time in String format.
     * @return a String representing a date and time.
     */
    public synchronized String getDatetime() {
        return dateTime;
    }

    /**
     * Set the granularity to use for this ResourceSelector.
     * @param g the timestamp granularity.
     */
    public synchronized void setGranularity(long g) {
        granularity = g;
    }

    /**
     * Get the timestamp granularity used by this ResourceSelector.
     * @return the long granularity.
     */
    public synchronized long getGranularity() {
        return granularity;
    }

    /**
     * Set the optional pattern to use with the datetime attribute.
     * @param p the SimpleDateFormat-compatible pattern string.
     */
    public synchronized void setPattern(String p) {
        pattern = p;
    }

    /**
     * Get the pattern for use with the datetime attribute.
     * @return a SimpleDateFormat-compatible pattern string.
     */
    public synchronized String getPattern() {
        return pattern;
    }

    /**
     * Set the comparison mode.
     * @param c a TimeComparison object.
     */
    public synchronized void setWhen(TimeComparison c) {
        when = c;
    }

    /**
     * Get the comparison mode.
     * @return a TimeComparison object.
     */
    public synchronized TimeComparison getWhen() {
        return when;
    }

    /**
     * Return true if this Resource is selected.
     * @param r the Resource to check.
     * @return whether the Resource was selected.
     */
    public synchronized boolean isSelected(Resource r) {
        if (dateTime == null && millis == null) {
            throw new BuildException(MILLIS_OR_DATETIME);
        }
        if (millis == null) {
            String p = pattern == null ? "MM/dd/yyyy hh:mm a" : pattern;
            DateFormat df = pattern == null
                ? new SimpleDateFormat(p, Locale.US)
                : new SimpleDateFormat(p);
            try {
                long m = df.parse(dateTime).getTime();
                if (m < 0) {
                    throw new BuildException(
                        "Date of %s results in negative milliseconds value relative to epoch (January 1, 1970, 00:00:00 GMT).",
                        dateTime);
                }
                setMillis(m);
            } catch (ParseException pe) {
                throw new BuildException(
                    "Date of %s Cannot be parsed correctly. It should be in '%s' format.",
                    dateTime, p);
            }
        }
        return when.evaluate(r.getLastModified(), millis, granularity);
    }

}
