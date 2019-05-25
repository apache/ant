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

package org.apache.tools.ant.taskdefs.optional;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.LayoutPreservingProperties;

/**
 * Modifies settings in a property file.
 *
 * <p>The following is an example of its usage:</p>
 * <pre>
 *    &lt;target name="setState"&gt;
 *      &lt;property
 *        name="header"
 *        value="##Generated file - do not modify!"/&gt;
 *      &lt;propertyfile file="apropfile.properties" comment="${header}"&gt;
 *        &lt;entry key="product.version.major" type="int"  value="5"/&gt;
 *        &lt;entry key="product.version.minor" type="int"  value="0"/&gt;
 *        &lt;entry key="product.build.major"   type="int"  value="0" /&gt;
 *        &lt;entry key="product.build.minor"   type="int"  operation="+"/&gt;
 *        &lt;entry key="product.build.date"    type="date" value="now"/&gt;
 *        &lt;entry key="intSet" type="int" operation="=" value="681"/&gt;
 *        &lt;entry key="intDec" type="int" operation="-"/&gt;
 *        &lt;entry key="StringEquals" type="string" value="testValue"/&gt;
 *     &lt;/propertyfile&gt;
 *   &lt;/target&gt;
 * </pre>
 * <p>
 * The &lt;propertyfile&gt; task must have:
 * </p>
 * <ul>
 *   <li>file</li>
 * </ul>
 * Other parameters are:
 * <ul>
 *   <li>comment</li>
 *   <li>key</li>
 *   <li>operation</li>
 *   <li>type</li>
 *   <li>value (the final four being eliminated shortly)</li>
 * </ul>
 * <p>
 * The &lt;entry&gt; task must have:
 * </p>
 * <ul>
 *   <li>key</li>
 * </ul>
 * Other parameters are:
 * <ul>
 *   <li>operation</li>
 *   <li>type</li>
 *   <li>value</li>
 *   <li>default</li>
 *   <li>unit</li>
 * </ul>
 * <p>
 * If type is unspecified, it defaults to string.
 * </p>
 * Parameter values:
 * <dl>
 *   <dt>operation:</dt>
 *   <dd>
 *   <ul>
 *     <li>"=" (set -- default)</li>
 *     <li>"-" (dec)</li>
 *     <li>"+" (inc)</li>
 *   </ul>
 *   </dd>
 *   <dt>type:</dt>
 *   <dd>
 *   <ul>
 *     <li>"int"</li>
 *     <li>"date"</li>
 *     <li>"string"</li>
 *   </ul>
 *   </dd>
 *   <dt>value:</dt>
 *   <dd>
 *   <ul>
 *     <li>holds the default value, if the property
 *              was not found in property file</li>
 *     <li>"now" In case of type "date", the
 *              value "now" will be replaced by the current
 *              date/time and used even if a valid date was
 *              found in the property file.</li>
 *   </ul>
 *   </dd>
 * </dl>
 *
 * <p>String property types can only use the "=" operation.
 * Int property types can only use the "=", "-" or "+" operations.<p>
 *
 * The message property is used for the property file header, with "\\" being
 * a newline delimiter character.
 *
 */
public class PropertyFile extends Task {

    /* ========================================================================
     *
     * Instance variables.
     */

    // Use this to prepend a message to the properties file
    private String              comment;

    private Properties          properties;
    private File                propertyfile;
    private boolean             useJDKProperties;

    private Vector<Entry> entries = new Vector<>();

    /* ========================================================================
     *
     * Constructors
     */

    /* ========================================================================
     *
     * Methods
     */

    /**
     * Execute the task.
     * @throws BuildException on error.
     */
    @Override
    public void execute() throws BuildException {
        checkParameters();
        readFile();
        executeOperation();
        writeFile();
    }

    /**
     * The entry nested element.
     * @return an entry nested element to be configured.
     */
    public Entry createEntry() {
        Entry e = new Entry();
        entries.addElement(e);
        return e;
    }

    private void executeOperation() throws BuildException {
        entries.forEach(e -> e.executeOn(properties));
    }

    private void readFile() throws BuildException {
        if (useJDKProperties) {
            // user chose to use standard Java properties, which loose
            // comments and layout
            properties = new Properties();
        } else {
            properties = new LayoutPreservingProperties();
        }
        try {
            if (propertyfile.exists()) {
                log("Updating property file: "
                    + propertyfile.getAbsolutePath());
                try (InputStream fis = Files.newInputStream(propertyfile.toPath());
                     BufferedInputStream bis = new BufferedInputStream(fis)) {
                    properties.load(bis);
                }
            } else {
                log("Creating new property file: "
                    + propertyfile.getAbsolutePath());
                try (OutputStream out =
                     Files.newOutputStream(propertyfile.toPath())) {
                    out.flush();
                }
            }
        } catch (IOException ioe) {
            throw new BuildException(ioe.toString());
        }
    }

    private void checkParameters() throws BuildException {
        if (!checkParam(propertyfile)) {
            throw new BuildException("file token must not be null.",
                                     getLocation());
        }
    }

    /**
     * Location of the property file to be edited; required.
     * @param file the property file.
     */
    public void setFile(File file) {
        propertyfile = file;
    }

    /**
     * optional header comment for the file
     * @param hdr the string to use for the comment.
     */
    public void setComment(String hdr) {
        comment = hdr;
    }

    /**
     * optional flag to use original Java properties (as opposed to
     * layout preserving properties)
     * @param val boolean
     */
    public void setJDKProperties(boolean val) {
        useJDKProperties = val;
    }

    private void writeFile() throws BuildException {
        // Write to RAM first, as an OOME could otherwise produce a truncated file:
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            properties.store(baos, comment);
        } catch (IOException x) { // should not happen
            throw new BuildException(x, getLocation());
        }
        try {
            try (OutputStream os = Files.newOutputStream(propertyfile.toPath())) {
                os.write(baos.toByteArray());
            } catch (IOException x) { // possibly corrupt
                FileUtils.getFileUtils().tryHardToDelete(propertyfile);
                throw x;
            }
        } catch (IOException x) { // opening, writing, or closing
            throw new BuildException(x, getLocation());
        }
    }

    private boolean checkParam(File param) {
        return param != null;
    }

    /**
     * Instance of this class represents nested elements of
     * a task propertyfile.
     */
    public static class Entry {
        private static final int DEFAULT_INT_VALUE = 0;
        private static final String DEFAULT_DATE_VALUE = "now";
        private static final String DEFAULT_STRING_VALUE = "";

        private String key = null;
        private int    type = Type.STRING_TYPE;
        private int    operation = Operation.EQUALS_OPER;
        private String value = null;
        private String defaultValue = null;
        private String newValue = null;
        private String pattern = null;
        private int    field = Calendar.DATE;

        /**
         * Name of the property name/value pair
         * @param value the key.
         */
        public void setKey(String value) {
            this.key = value;
        }

        /**
         * Value to set (=), to add (+) or subtract (-)
         * @param value the value.
         */
        public void setValue(String value) {
            this.value = value;
        }

        /**
         * operation to apply.
         * &quot;+&quot; or &quot;=&quot;
         *(default) for all datatypes; &quot;-&quot; for date and int only)\.
         * @param value the operation enumerated value.
         */
        public void setOperation(Operation value) {
            this.operation = Operation.toOperation(value.getValue());
        }

        /**
         * Regard the value as : int, date or string (default)
         * @param value the type enumerated value.
         */
        public void setType(Type value) {
            this.type = Type.toType(value.getValue());
        }

        /**
         * Initial value to set for a property if it is not
         * already defined in the property file.
         * For type date, an additional keyword is allowed: &quot;now&quot;
         * @param value the default value.
         */
        public void setDefault(String value) {
            this.defaultValue = value;
        }

        /**
         * For int and date type only. If present, Values will
         * be parsed and formatted accordingly.
         * @param value the pattern to use.
         */
        public void setPattern(String value) {
            this.pattern = value;
        }

        /**
         * The unit of the value to be applied to date +/- operations.
         *            Valid Values are:
         *            <ul>
         *               <li>millisecond</li>
         *               <li>second</li>
         *               <li>minute</li>
         *               <li>hour</li>
         *               <li>day (default)</li>
         *               <li>week</li>
         *               <li>month</li>
         *               <li>year</li>
         *            </ul>
         *            This only applies to date types using a +/- operation.
         * @param unit the unit enumerated value.
         * @since Ant 1.5
         */
        public void setUnit(PropertyFile.Unit unit) {
            field = unit.getCalendarField();
        }

        /**
         * Apply the nested element to the properties.
         * @param props the properties to apply the entry on.
         * @throws BuildException if there is an error.
         */
        protected void executeOn(Properties props) throws BuildException {
            checkParameters();

            if (operation == Operation.DELETE_OPER) {
                props.remove(key);
                return;
            }

            // type may be null because it wasn't set
            String oldValue = (String) props.get(key);
            try {
                if (type == Type.INTEGER_TYPE) {
                    executeInteger(oldValue);
                } else if (type == Type.DATE_TYPE) {
                    executeDate(oldValue);
                } else if (type == Type.STRING_TYPE) {
                    executeString(oldValue);
                } else {
                    throw new BuildException("Unknown operation type: %d", type);
                }
            } catch (NullPointerException npe) {
                // Default to string type
                // which means do nothing
                npe.printStackTrace(); //NOSONAR
            }

            if (newValue == null) {
                newValue = "";
            }

            // Insert as a string by default
            props.put(key, newValue);
        }

        /**
         * Handle operations for type <code>date</code>.
         *
         * @param oldValue the current value read from the property file or
         *                 <code>null</code> if the <code>key</code> was
         *                 not contained in the property file.
         */
        private void executeDate(String oldValue) throws BuildException {
            Calendar currentValue = Calendar.getInstance();

            if (pattern == null) {
                pattern = "yyyy/MM/dd HH:mm";
            }
            DateFormat fmt = new SimpleDateFormat(pattern);

            String currentStringValue = getCurrentValue(oldValue);
            if (currentStringValue == null) {
                currentStringValue = DEFAULT_DATE_VALUE;
            }

            if ("now".equals(currentStringValue)) {
                currentValue.setTime(new Date());
            } else {
                try {
                    currentValue.setTime(fmt.parse(currentStringValue));
                } catch (ParseException pe)  {
                    // swallow
                }
            }

            if (operation != Operation.EQUALS_OPER) {
                int offset = 0;
                try {
                    offset = Integer.parseInt(value);
                    if (operation == Operation.DECREMENT_OPER) {
                        offset = -1 * offset;
                    }
                } catch (NumberFormatException e) {
                    throw new BuildException("Value not an integer on " + key);
                }
                currentValue.add(field, offset);
            }

            newValue = fmt.format(currentValue.getTime());
        }

        /**
         * Handle operations for type <code>int</code>.
         *
         * @param oldValue the current value read from the property file or
         *                 <code>null</code> if the <code>key</code> was
         *                 not contained in the property file.
         */
        private void executeInteger(String oldValue) throws BuildException {
            int currentValue = DEFAULT_INT_VALUE;
            int newV  = DEFAULT_INT_VALUE;

            DecimalFormat fmt = (pattern != null) ? new DecimalFormat(pattern)
                : new DecimalFormat();
            try {
                String curval = getCurrentValue(oldValue);
                if (curval != null) {
                    currentValue = fmt.parse(curval).intValue();
                } else {
                    currentValue = 0;
                }
            } catch (NumberFormatException | ParseException nfe) {
                // swallow
            }

            if (operation == Operation.EQUALS_OPER) {
                newV = currentValue;
            } else {
                int operationValue = 1;
                if (value != null) {
                    try {
                        operationValue = fmt.parse(value).intValue();
                    } catch (NumberFormatException | ParseException nfe) {
                        // swallow
                    }
                }

                if (operation == Operation.INCREMENT_OPER) {
                    newV = currentValue + operationValue;
                } else if (operation == Operation.DECREMENT_OPER) {
                    newV = currentValue - operationValue;
                }
            }

            this.newValue = fmt.format(newV);
        }

        /**
         * Handle operations for type <code>string</code>.
         *
         * @param oldValue the current value read from the property file or
         *                 <code>null</code> if the <code>key</code> was
         *                 not contained in the property file.
         */
        private void executeString(String oldValue) throws BuildException {
            String newV  = DEFAULT_STRING_VALUE;

            String currentValue = getCurrentValue(oldValue);

            if (currentValue == null) {
                currentValue = DEFAULT_STRING_VALUE;
            }

            if (operation == Operation.EQUALS_OPER) {
                newV = currentValue;
            } else if (operation == Operation.INCREMENT_OPER) {
                newV = currentValue + value;
            }
            this.newValue = newV;
        }

        /**
         * Check if parameter combinations can be supported
         * @todo make sure the 'unit' attribute is only specified on date
         *      fields
         */
        private void checkParameters() throws BuildException {
            if (type == Type.STRING_TYPE
                && operation == Operation.DECREMENT_OPER) {
                throw new BuildException("- is not supported for string "
                                         + "properties (key:" + key + ")");
            }
            if (value == null && defaultValue == null  && operation != Operation.DELETE_OPER) {
                throw new BuildException(
                    "\"value\" and/or \"default\" attribute must be specified (key: %s)",
                    key);
            }
            if (key == null) {
                throw new BuildException("key is mandatory");
            }
            if (type == Type.STRING_TYPE && pattern != null) {
                throw new BuildException(
                    "pattern is not supported for string properties (key: %s)",
                    key);
            }
        }

        private String getCurrentValue(String oldValue) {
            String ret = null;
            if (operation == Operation.EQUALS_OPER) {
                // If only value is specified, the property is set to it
                // regardless of its previous value.
                if (value != null && defaultValue == null) {
                    ret = value;
                }

                // If only default is specified and the property previously
                // existed in the property file, it is unchanged.
                if (value == null && defaultValue != null && oldValue != null) {
                    ret = oldValue;
                }

                // If only default is specified and the property did not
                // exist in the property file, the property is set to default.
                if (value == null && defaultValue != null && oldValue == null) {
                    ret = defaultValue;
                }

                // If value and default are both specified and the property
                // previously existed in the property file, the property
                // is set to value.
                if (value != null && defaultValue != null && oldValue != null) {
                    ret = value;
                }

                // If value and default are both specified and the property
                // did not exist in the property file, the property is set
                // to default.
                if (value != null && defaultValue != null && oldValue == null) {
                    ret = defaultValue;
                }
            } else {
                ret = oldValue == null ? defaultValue : oldValue;
            }

            return ret;
        }

        /**
         * Enumerated attribute with the values "+", "-", "="
         */
        public static class Operation extends EnumeratedAttribute {

            // Property type operations
            /** + */
            public static final int INCREMENT_OPER = 0;
            /** - */
            public static final int DECREMENT_OPER = 1;
            /** = */
            public static final int EQUALS_OPER =    2;
            /** del */
            public static final int DELETE_OPER =    3;

            /** {@inheritDoc}. */
            @Override
            public String[] getValues() {
                return new String[] {"+", "-", "=", "del"};
            }

            /**
             * Convert string to index.
             * @param oper the string to convert.
             * @return the index.
             */
            public static int toOperation(String oper) {
                if ("+".equals(oper)) {
                    return INCREMENT_OPER;
                }
                if ("-".equals(oper)) {
                    return DECREMENT_OPER;
                }
                if ("del".equals(oper)) {
                    return DELETE_OPER;
                }
                return EQUALS_OPER;
            }
        }

        /**
         * Enumerated attribute with the values "int", "date" and "string".
         */
        public static class Type extends EnumeratedAttribute {

            // Property types
            /** int */
            public static final int INTEGER_TYPE = 0;
            /** date */
            public static final int DATE_TYPE =    1;
            /** string */
            public static final int STRING_TYPE =  2;

            /** {@inheritDoc} */
            @Override
            public String[] getValues() {
                return new String[] {"int", "date", "string"};
            }

            /**
             * Convert string to index.
             * @param type the string to convert.
             * @return the index.
             */
            public static int toType(String type) {
                if ("int".equals(type)) {
                    return INTEGER_TYPE;
                }
                if ("date".equals(type)) {
                    return DATE_TYPE;
                }
                return STRING_TYPE;
            }
        }
    }

    /**
     * Borrowed from Tstamp
     * @todo share all this time stuff across many tasks as a datetime datatype
     * @since Ant 1.5
     */
    public static class Unit extends EnumeratedAttribute {

        private static final String MILLISECOND = "millisecond";
        private static final String SECOND = "second";
        private static final String MINUTE = "minute";
        private static final String HOUR = "hour";
        private static final String DAY = "day";
        private static final String WEEK = "week";
        private static final String MONTH = "month";
        private static final String YEAR = "year";

        private static final String[] UNITS = {MILLISECOND, SECOND, MINUTE, HOUR, DAY, WEEK, MONTH,
                YEAR};

        private Map<String, Integer> calendarFields = new HashMap<>();

        /** no arg constructor */
        public Unit() {
            calendarFields.put(MILLISECOND,
                    Calendar.MILLISECOND);
            calendarFields.put(SECOND, Calendar.SECOND);
            calendarFields.put(MINUTE, Calendar.MINUTE);
            calendarFields.put(HOUR, Calendar.HOUR_OF_DAY);
            calendarFields.put(DAY, Calendar.DATE);
            calendarFields.put(WEEK, Calendar.WEEK_OF_YEAR);
            calendarFields.put(MONTH, Calendar.MONTH);
            calendarFields.put(YEAR, Calendar.YEAR);
        }

        /**
         * Convert the value to a Calendar field index.
         * @return the calendar value.
         */
        public int getCalendarField() {
            return calendarFields.get(getValue().toLowerCase());
        }

        /** {@inheritDoc}. */
        @Override
        public String[] getValues() {
            return UNITS;
        }
    }
}
