/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.types.EnumeratedAttribute;

import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;
import java.text.SimpleDateFormat;

/**
 * Sets TSTAMP, DSTAMP and TODAY
 *
 * @author costin@dnt.ro
 * @author stefano@apache.org
 * @author roxspring@yahoo.com
 * @author Conor MacNeill
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 * @since Ant 1.1
 * @ant.task category="utility"
 */
public class Tstamp extends Task {

    private Vector customFormats = new Vector();
    private String prefix = "";

    /**
     * @since Ant 1.5
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
        if (!this.prefix.endsWith(".")) {
            this.prefix += ".";
        }
    }

    public void execute() throws BuildException {
        try {
            Date d = new Date();

            SimpleDateFormat dstamp = new SimpleDateFormat ("yyyyMMdd");
            project.setNewProperty(prefix + "DSTAMP", dstamp.format(d));

            SimpleDateFormat tstamp = new SimpleDateFormat ("HHmm");
            project.setNewProperty(prefix + "TSTAMP", tstamp.format(d));

            SimpleDateFormat today  = new SimpleDateFormat ("MMMM d yyyy", Locale.US);
            project.setNewProperty(prefix + "TODAY", today.format(d));

            Enumeration i = customFormats.elements();
            while (i.hasMoreElements()) {
                CustomFormat cts = (CustomFormat) i.nextElement();
                cts.execute(project, d, location);
            }

        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    public CustomFormat createFormat()
    {
        CustomFormat cts = new CustomFormat(prefix);
        customFormats.addElement(cts);
        return cts;
    }

    public class CustomFormat
    {
        private TimeZone timeZone;
        private String propertyName;
        private String pattern;
        private String language;
        private String country;
        private String variant;
        private int offset = 0;
        private int field = Calendar.DATE;
        private String prefix = "";

        public CustomFormat(String prefix)
        {
            this.prefix = prefix;
        }

        public void setProperty(String propertyName)
        {
            this.propertyName = prefix + propertyName;
        }

        public void setPattern(String pattern)
        {
            this.pattern = pattern;
        }

        public void setLocale(String locale)
        {
            StringTokenizer st = new StringTokenizer(locale, " \t\n\r\f,");
            try {
                language = st.nextToken();
                if (st.hasMoreElements()) {
                    country = st.nextToken();
                    if (st.hasMoreElements()) {
                        variant = st.nextToken();
                        if (st.hasMoreElements()) {
                            throw new BuildException("bad locale format", 
                                                      getLocation());
                        }
                    }
                }
                else {
                    country = "";
                }
            }
            catch (NoSuchElementException e) {
                throw new BuildException("bad locale format", e, 
                                         getLocation());
            }
        }

        public void setTimezone(String id){
            timeZone = TimeZone.getTimeZone(id);
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        /**
         * @deprecated setUnit(String) is deprecated and is replaced with
         *             setUnit(Tstamp.Unit) to make Ant's
         *             Introspection mechanism do the work and also to
         *             encapsulate operations on the unit in its own
         *             class.
         */
        public void setUnit(String unit) {
            log("DEPRECATED - The setUnit(String) method has been deprecated."
                + " Use setUnit(Tstamp.Unit) instead.");
            Unit u = new Unit();
            u.setValue(unit);
            field = u.getCalendarField();
        }

        public void setUnit(Unit unit) {
            field = unit.getCalendarField();
        }

        public void execute(Project project, Date date, Location location)
        {
            if (propertyName == null) {
                throw new BuildException("property attribute must be provided",
                                         location);
            }

            if (pattern == null) {
                throw new BuildException("pattern attribute must be provided",
                                         location);
            }

            SimpleDateFormat sdf;
            if (language == null) {
                sdf = new SimpleDateFormat(pattern);
            }
            else if (variant == null) {
                sdf = new SimpleDateFormat(pattern,
                                           new Locale(language, country));
            }
            else {
                sdf = new SimpleDateFormat(pattern,
                                           new Locale(language, country,
                                                      variant));
            }
            if (offset != 0) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                calendar.add(field, offset);
                date = calendar.getTime();
            }
            if (timeZone != null){
                sdf.setTimeZone(timeZone);
            }
            project.setNewProperty(propertyName, sdf.format(date));
        }
    }

    public static class Unit extends EnumeratedAttribute {

        private final static String MILLISECOND = "millisecond";
        private final static String SECOND = "second";
        private final static String MINUTE = "minute";
        private final static String HOUR = "hour";
        private final static String DAY = "day";
        private final static String WEEK = "week";
        private final static String MONTH = "month";
        private final static String YEAR = "year";

        private final static String[] units = {
                                                MILLISECOND,
                                                SECOND,
                                                MINUTE,
                                                HOUR,
                                                DAY,
                                                WEEK,
                                                MONTH,
                                                YEAR
                                              };

        private Hashtable calendarFields = new Hashtable();

        public Unit() {
            calendarFields.put(MILLISECOND,
                               new Integer(Calendar.MILLISECOND));
            calendarFields.put(SECOND, new Integer(Calendar.SECOND));
            calendarFields.put(MINUTE, new Integer(Calendar.MINUTE));
            calendarFields.put(HOUR, new Integer(Calendar.HOUR_OF_DAY));
            calendarFields.put(DAY, new Integer(Calendar.DATE));
            calendarFields.put(WEEK, new Integer(Calendar.WEEK_OF_YEAR));
            calendarFields.put(MONTH, new Integer(Calendar.MONTH));
            calendarFields.put(YEAR, new Integer(Calendar.YEAR));
        }

        public int getCalendarField() {
            String key = getValue().toLowerCase();
            Integer i = (Integer) calendarFields.get(key);
            return i.intValue();
        }

        public String[] getValues() {
            return units;
        }
    }
}
