/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.types.selectors;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Locale;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.taskdefs.condition.Os;

/**
 * Selector that chooses files based on their last modified date.
 *
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 * @since 1.5
 */
public class DateSelector extends BaseExtendSelector {

    private long millis = -1;
    private String dateTime = null;
    private boolean includeDirs = false;
    private int granularity = 0;
    private int cmp = 2;
    public final static String MILLIS_KEY = "millis";
    public final static String DATETIME_KEY = "datetime";
    public final static String CHECKDIRS_KEY = "checkdirs";
    public final static String GRANULARITY_KEY = "granularity";
    public final static String WHEN_KEY = "when";

    public DateSelector() {
        if (Os.isFamily("dos")) {
            granularity = 2000;
        }
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("{dateselector date: ");
        buf.append(dateTime);
        buf.append(" compare: ");
        if (cmp == 0) {
            buf.append("before");
        }
        else if (cmp == 1) {
            buf.append("after");
        } else {
            buf.append("equal");
        }
        buf.append(" granularity: ");
        buf.append(granularity);
        buf.append("}");
        return buf.toString();
    }

    /**
     * For users that prefer to express time in milliseconds since 1970
     *
     * @param millis the time to compare file's last modified date to,
     *        expressed in milliseconds
     */
    public void setMillis(long millis) {
        this.millis = millis;
    }

    /**
     * Returns the millisecond value the selector is set for.
     */
    public long getMillis() {
        return millis;
    }

    /**
     * Sets the date. The user must supply it in MM/DD/YYYY HH:MM AM_PM
     * format
     *
     * @param dateTime a string in MM/DD/YYYY HH:MM AM_PM format
     */
    public void setDatetime(String dateTime) {
        this.dateTime = dateTime;
        if (dateTime != null) {
            DateFormat df = DateFormat.getDateTimeInstance(
                                                    DateFormat.SHORT,
                                                    DateFormat.SHORT,
                                                    Locale.US);
            try {
                setMillis(df.parse(dateTime).getTime());
                if (millis < 0) {
                    setError("Date of " + dateTime
                        + " results in negative milliseconds value relative"
                        + " to epoch (January 1, 1970, 00:00:00 GMT).");
                }
            } catch (ParseException pe) {
                    setError("Date of " + dateTime
                        + " Cannot be parsed correctly. It should be in"
                        + " MM/DD/YYYY HH:MM AM_PM format.");
            }
        }
    }

    /**
     * Should we be checking dates on directories?
     *
     * @param includeDirs whether to check the timestamp on directories
     */
    public void setCheckdirs(boolean includeDirs) {
        this.includeDirs = includeDirs;
    }

    /**
     * Sets the number of milliseconds leeway we will give before we consider
     * a file not to have matched a date.
     */
    public void setGranularity(int granularity) {
        this.granularity = granularity;
    }

    /**
     * Sets the type of comparison to be done on the file's last modified
     * date.
     *
     * @param cmp The comparison to perform, an EnumeratedAttribute
     */
    public void setWhen(TimeComparisons cmp) {
        this.cmp = cmp.getIndex();
    }

    /**
     * When using this as a custom selector, this method will be called.
     * It translates each parameter into the appropriate setXXX() call.
     *
     * @param parameters the complete set of parameters for this selector
     */
    public void setParameters(Parameter[] parameters) {
        super.setParameters(parameters);
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                String paramname = parameters[i].getName();
                if (MILLIS_KEY.equalsIgnoreCase(paramname)) {
                    try {
                        setMillis(new Long(parameters[i].getValue()
                                ).longValue());
                    } catch (NumberFormatException nfe) {
                        setError("Invalid millisecond setting " +
                            parameters[i].getValue());
                    }
                }
                else if (DATETIME_KEY.equalsIgnoreCase(paramname)) {
                    setDatetime(parameters[i].getValue());
                }
                else if (CHECKDIRS_KEY.equalsIgnoreCase(paramname)) {
                    setCheckdirs(Project.toBoolean(parameters[i].getValue()));
                }
                else if (GRANULARITY_KEY.equalsIgnoreCase(paramname)) {
                    try {
                        setGranularity(new Integer(parameters[i].getValue()
                                ).intValue());
                    } catch (NumberFormatException nfe) {
                        setError("Invalid granularity setting " +
                            parameters[i].getValue());
                    }
                }
                else if (WHEN_KEY.equalsIgnoreCase(paramname)) {
                    TimeComparisons cmp = new TimeComparisons();
                    cmp.setValue(parameters[i].getValue());
                    setWhen(cmp);
                }
                else {
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
        }
        else if (millis < 0) {
            setError("Date of " + dateTime
                + " results in negative milliseconds"
                + " value relative to epoch (January 1, 1970, 00:00:00 GMT).");
        }
    }

    /**
     * The heart of the matter. This is where the selector gets to decide
     * on the inclusion of a file in a particular fileset.
     *
     * @param basedir the base directory the scan is being done from
     * @param filename is the name of the file to check
     * @param file is a java.io.File object the selector can use
     * @return whether the file should be selected or not
     */
    public boolean isSelected(File basedir, String filename, File file) {
        validate();
        if (file.isDirectory() && (includeDirs == false)) {
            return true;
        }
        if (cmp == 0) {
            return ((file.lastModified() - granularity) < millis);
        }
        else if (cmp == 1) {
            return ((file.lastModified() + granularity) > millis);
        }
        else {
            return (Math.abs(file.lastModified() -  millis) <= granularity);
        }
    }

    /**
     * Enumerated attribute with the values for time comparison.
     * <p>
     */
    public static class TimeComparisons extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"before", "after", "equal"};
        }
    }

}


