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

package org.apache.tools.ant.taskdefs.condition;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.Touch;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Resource;

/**
 * Condition that makes assertions about the last modified date of a
 * resource.
 *
 * @since Ant 1.8.0
 */
public class IsLastModified extends ProjectComponent implements Condition {
    private long millis = -1;
    private String dateTime = null;
    private Touch.DateFormatFactory dfFactory = Touch.DEFAULT_DF_FACTORY;
    private Resource resource;
    private CompareMode mode = CompareMode.EQUALS;

    /**
     * Set the new modification time of file(s) touched
     * in milliseconds since midnight Jan 1 1970.
     * @param millis the <code>long</code> timestamp to use.
     */
    public void setMillis(long millis) {
        this.millis = millis;
    }

    /**
     * Set the new modification time of file(s) touched
     * in the format &quot;MM/DD/YYYY HH:MM AM <i>or</i> PM&quot;
     * or &quot;MM/DD/YYYY HH:MM:SS AM <i>or</i> PM&quot;.
     * @param dateTime the <code>String</code> date in the specified format.
     */
    public void setDatetime(String dateTime) {
        this.dateTime = dateTime;
    }

    /**
     * Set the format of the datetime attribute.
     * @param pattern the <code>SimpleDateFormat</code>-compatible
     * format pattern.
     */
    public void setPattern(final String pattern) {
        dfFactory = new Touch.DateFormatFactory() {
            @Override
            public DateFormat getPrimaryFormat() {
                return new SimpleDateFormat(pattern);
            }
            @Override
            public DateFormat getFallbackFormat() {
                return null;
            }
        };
    }

    /**
     * The resource to test.
     * @param r the resource to test
     */
    public void add(Resource r) {
        if (resource != null) {
            throw new BuildException("only one resource can be tested");
        }
        resource = r;
    }

    /**
     * The type of comparison to test.
     * @param mode the mode of comparison.
     */
    public void setMode(CompareMode mode) {
        this.mode = mode;
    }

    /**
     * Argument validation.
     * @throws BuildException if the required attributes are not supplied or
     * if there is an inconsistency in the attributes.
     */
    protected void validate() throws BuildException {
        if (millis >= 0 && dateTime != null) {
            throw new BuildException(
                "Only one of dateTime and millis can be set");
        }
        if (millis < 0 && dateTime == null) {
            throw new BuildException("millis or dateTime is required");
        }
        if (resource == null) {
            throw new BuildException("resource is required");
        }
    }

    /**
     * Calculate timestamp as millis either based on millis or
     * dateTime (and pattern) attribute.
     * @return time in milliseconds
     * @throws BuildException if the date cannot be parsed.
     */
    protected long getMillis() throws BuildException {
        if (millis >= 0) {
            return millis;
        }
        if ("now".equalsIgnoreCase(dateTime)) {
            return System.currentTimeMillis();
        }
        DateFormat df = dfFactory.getPrimaryFormat();
        ParseException pe;
        try {
            return df.parse(dateTime).getTime();
        } catch (ParseException peOne) {
            df = dfFactory.getFallbackFormat();
            if (df == null) {
                pe = peOne;
            } else {
                try {
                    return df.parse(dateTime).getTime();
                } catch (ParseException peTwo) {
                    pe = peTwo;
                }
            }
        }
        throw new BuildException(pe.getMessage(), pe, getLocation());
    }

    /**
     * evaluate the condition
     * @return true or false depending on the comparison mode and the time of the resource
     * @throws BuildException if something goes wrong
     */
    @Override
    public boolean eval() throws BuildException {
        validate();
        long expected = getMillis();
        long actual = resource.getLastModified();
        log("expected timestamp: " + expected + " (" + new Date(expected) + ")"
            + ", actual timestamp: " + actual + " (" + new Date(actual) + ")",
            Project.MSG_VERBOSE);
        if (CompareMode.EQUALS_TEXT.equals(mode.getValue())) {
            return expected == actual;
        }
        if (CompareMode.BEFORE_TEXT.equals(mode.getValue())) {
            return expected > actual;
        }
        if (CompareMode.NOT_BEFORE_TEXT.equals(mode.getValue())) {
            return expected <= actual;
        }
        if (CompareMode.AFTER_TEXT.equals(mode.getValue())) {
            return expected < actual;
        }
        if (CompareMode.NOT_AFTER_TEXT.equals(mode.getValue())) {
            return expected >= actual;
        }
        throw new BuildException("Unknown mode " + mode.getValue());
    }

    /**
     * describes comparison modes.
     */
    public static class CompareMode extends EnumeratedAttribute {
        private static final String EQUALS_TEXT = "equals";
        private static final String BEFORE_TEXT = "before";
        private static final String AFTER_TEXT = "after";
        private static final String NOT_BEFORE_TEXT = "not-before";
        private static final String NOT_AFTER_TEXT = "not-after";

        private static final CompareMode EQUALS = new CompareMode(EQUALS_TEXT);

        /**
         * creates a CompareMode instance of type equals
         */
        public CompareMode() {
            this(EQUALS_TEXT);
        }

        /**
         * creates a comparemode instance
         * @param s one of the authorized values for comparemode
         */
        public CompareMode(String s) {
            super();
            setValue(s);
        }

        /** {@inheritDoc}. */
        @Override
        public String[] getValues() {
            return new String[] {
                EQUALS_TEXT, BEFORE_TEXT, AFTER_TEXT, NOT_BEFORE_TEXT,
                NOT_AFTER_TEXT,
            };
        }
    }
}
