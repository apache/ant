/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.EnumeratedAttribute;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Date;
import java.util.Calendar;
import java.util.Hashtable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.text.DecimalFormat;

/**
 *Modifies settings in a property file.
 *
 * <p>
 *The following is an example of its usage:
 *    <ul>&lt;target name="setState"&gt;<br>
 *    <ul>&lt;property<br>
 *        <ul>name="header"<br>
 *        value="##Generated file - do not modify!"/&gt;<br>
 *      &lt;propertyfile file="apropfile.properties" comment="${header}"&gt;<br>
 *        &lt;entry key="product.version.major" type="int"  value="5"/&gt;<br>
 *        &lt;entry key="product.version.minor" type="int"  value="0"/&gt;<br>
 *        &lt;entry key="product.build.major"   type="int"  value="0" /&gt;<br>
 *        &lt;entry key="product.build.minor"   type="int"  operation="+" /&gt;<br>
 *        &lt;entry key="product.build.date"    type="date" value="now" /&gt;<br>
 *        &lt;entry key="intSet" type="int" operation="=" value="681"/&gt;<br>
 *        &lt;entry key="intDec" type="int" operation="-"/&gt;<br>
 *        &lt;entry key="StringEquals" type="string" value="testValue"/&gt;<br>
 *     &lt;/propertyfile&gt;<br></ul>
 *   &lt;/target&gt;</ul><p>
 *
 *The &lt;propertyfile&gt; task must have:<br>
 *    <ul><li>file</li></ul>
 *Other parameters are:<br>
 *    <ul><li>comment, key, operation, type and value (the final four being eliminated shortly)</li></ul>
 *
 *The &lt;entry&gt; task must have:<br>
 *    <ul><li>key</li></ul>
 *Other parameters are:<br>
 *    <ul><li>operation</li>
 *        <li>type</li>
 *        <li>value</li>
 *        <li>default</li>
 *        <li>unit</li>
 *    </ul>
 *
 *If type is unspecified, it defaults to string
 *
 *Parameter values:<br>
 *    <ul><li>operation:</li>
 *        <ul><li>"=" (set -- default)</li>
 *        <li>"-" (dec)</li>
 *        <li>"+" (inc)</li>
 *
 *    <li>type:</li>
 *        <ul><li>"int"</li>
 *        <li>"date"</li>
 *        <li>"string"</li></ul></ul>
 *
 *    <li>value:</li>
 *      <ul><li>holds the default value, if the property
 *              was not found in property file</li>
 *          <li>"now" In case of type "date", the
 *              value "now" will be replaced by the current
 *              date/time and used even if a valid date was
 *              found in the property file.</li></ul>
 *
 *
 *String property types can only use the "=" operation.
 *Int property types can only use the "=", "-" or "+" operations.<p>
 *
 *The message property is used for the property file header, with "\\" being
 *a newline delimiter character.
 *
 * @author Thomas Christen <a href="mailto:chr@active.ch">chr@active.ch</a>
 * @author Jeremy Mawson <a href="mailto:jem@loftinspace.com.au">jem@loftinspace.com.au</a>
 * @author Erik Hatcher <a href="mailto:ehatcher@apache.org">ehatcher@apache.org</a>
 */
public class PropertyFile extends Task {

    /* ========================================================================
    *
    * Static variables.
    */

    private static final String NEWLINE = System.getProperty("line.separator");


    /* ========================================================================
    *
    * Instance variables.
    */

    // Use this to prepend a message to the properties file
    private String              comment;

    private Properties          properties;
    private File                propertyfile;

    private Vector entries = new Vector();

    /* ========================================================================
    *
    * Constructors
    */

    /* ========================================================================
    *
    * Methods
    */

    public void execute() throws BuildException {
        checkParameters();
        readFile();
        executeOperation();
        writeFile();
    }

    public Entry createEntry() {
        Entry e = new Entry();
        entries.addElement(e);
        return e;
    }

    private void executeOperation() throws BuildException {
        for (Enumeration e = entries.elements(); e.hasMoreElements();) {
            Entry entry = (Entry) e.nextElement();
            entry.executeOn(properties);
        }
    }

    private void readFile() throws BuildException {
        // Create the PropertyFile
        properties = new Properties();
        try {
            if (propertyfile.exists()) {
                log("Updating property file: " 
                    + propertyfile.getAbsolutePath());
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(propertyfile);
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    properties.load(bis);
                } finally {
                    if (fis != null) {
                        fis.close();
                    }
                }
            } else {
                log("Creating new property file: " 
                    + propertyfile.getAbsolutePath());
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(propertyfile.getAbsolutePath());
                    out.flush();
                } finally {
                    if (out != null) {
                        out.close();
                    }
                }
            }
        } catch (IOException ioe) {
            throw new BuildException(ioe.toString());
        }
    }

    private void checkParameters() throws BuildException {
        if (!checkParam(propertyfile)) {
            throw new BuildException("file token must not be null.", location);
        }
    }

    /**
     * Location of the property file to be edited; required.
     */
    public void setFile(File file) {
        propertyfile = file;
    }

    /**
     * optional header comment for the file
     */
    public void setComment(String hdr) {
        comment = hdr;
    }

    private void writeFile() throws BuildException {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(propertyfile));

            // Properties.store is not available in JDK 1.1
            Method m =
                Properties.class.getMethod("store",
                                           new Class[] {
                                               OutputStream.class,
                                               String.class});
            m.invoke(properties, new Object[] {bos, comment});

        } catch (NoSuchMethodException nsme) {
            properties.save(bos, comment);
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            throw new BuildException(t, location);
        } catch (IllegalAccessException iae) {
            // impossible
            throw new BuildException(iae, location);
        } catch (IOException ioe) {
            throw new BuildException(ioe, location);
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException ioex) {}
            }
        }
    }

    /**
    * Returns whether the given parameter has been defined.
    * @todo IDEA is saying this method is never used - remove?
    */
    private boolean checkParam(String param) {
        return !((param == null) || (param.equals("null")));
    }

    private boolean checkParam(File param) {
        return !(param == null);
    }

    /**
     * Instance of this class represents nested elements of
     * a task propertyfile.
     */
    public static class Entry {
        private static final int DEFAULT_INT_VALUE = 0;
        private static final String DEFAULT_DATE_VALUE = "now";
        private static final String DEFAULT_STRING_VALUE = "";

        private String              key = null;
        private int                 type = Type.STRING_TYPE;
        private int                 operation = Operation.EQUALS_OPER;
        private String              value = null;
        private String              defaultValue = null;
        private String              pattern = null;
        private int                 field = Calendar.DATE;

        /**
         * Name of the property name/value pair
         */
        public void setKey(String value) {
            this.key = value;
        }
        
        /** 
         * Value to set (=), to add (+) or subtract (-)
         */
        public void setValue(String value) {
            this.value = value;
        }
        
        /**
         * operation to apply. 
         * &quot;+&quot; or &quot;=&quot; 
         *(default) for all datatypes; &quot;-&quot; for date and int only)\.
         */
        public void setOperation(Operation value) {
            this.operation = Operation.toOperation(value.getValue());
        }
        
        /**
         * Regard the value as : int, date or string (default)
         */
        public void setType(Type value) {
            this.type = Type.toType(value.getValue());
        }
        
        /**
         * Initial value to set for a property if it is not
         * already defined in the property file.
         * For type date, an additional keyword is allowed: &quot;now&quot;
         */
                     
        public void setDefault(String value) {
            this.defaultValue = value;
        }
        
        /**
         * For int and date type only. If present, Values will
         * be parsed and formatted accordingly.
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
         * @since Ant 1.5
         */
        public void setUnit(PropertyFile.Unit unit) {
            field = unit.getCalendarField();
        }

        protected void executeOn(Properties props) throws BuildException {
            checkParameters();

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
                    throw new BuildException("Unknown operation type: "
                        + type);
                }
            } catch (NullPointerException npe) {
                // Default to string type
                // which means do nothing
                npe.printStackTrace();
            }
            
            if (value == null) {
                value = "";
            }
            
            // Insert as a string by default
            props.put(key, value);
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

            value = fmt.format(currentValue.getTime());
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
            int newValue  = DEFAULT_INT_VALUE;


            DecimalFormat fmt = (pattern != null) ? new DecimalFormat(pattern)
                                                    : new DecimalFormat();
            try {
                currentValue = fmt.parse(getCurrentValue(oldValue)).intValue();
            } catch (NumberFormatException nfe) {
                // swallow
            } catch (ParseException pe)  {
                // swallow
            }
            
            if (operation == Operation.EQUALS_OPER) {
                newValue = currentValue;
            } else {
                int operationValue = 1;
                if (value != null) {
                    try {
                        operationValue = fmt.parse(value).intValue();
                    } catch (NumberFormatException nfe) {
                        // swallow
                    } catch (ParseException pe)  {
                        // swallow
                    }
                }

                if (operation == Operation.INCREMENT_OPER) {
                    newValue = currentValue + operationValue;
                } else if (operation == Operation.DECREMENT_OPER) {
                    newValue = currentValue - operationValue;
                }
            }

            value = fmt.format(newValue);
        }
        
        /**
        * Handle operations for type <code>string</code>.
        *
        * @param oldValue the current value read from the property file or
        *                 <code>null</code> if the <code>key</code> was
        *                 not contained in the property file.
        */
        private void executeString(String oldValue) throws BuildException {
            String newValue  = DEFAULT_STRING_VALUE;

            String currentValue = getCurrentValue(oldValue);
            
            if (currentValue == null) {
                currentValue = DEFAULT_STRING_VALUE;
            }
            
            if (operation == Operation.EQUALS_OPER) {
                newValue = currentValue;
            } else if (operation == Operation.INCREMENT_OPER) {
                newValue = currentValue + value;
            }
            value = newValue;
        }
        
        /**
         * Check if parameter combinations can be supported
         * @todo make sure the 'unit' attribute is only specified on date
         *      fields
         */
        private void checkParameters() throws BuildException {
            if (type == Type.STRING_TYPE &&
                operation == Operation.DECREMENT_OPER) {
                throw new BuildException("- is not suported for string " 
                    + "properties (key:" + key + ")");
            }
            if (value == null && defaultValue == null) {
                throw new BuildException("\"value\" and/or \"default\" " 
                    + "attribute must be specified (key:" + key + ")");
            }
            if (key == null) {
                throw new BuildException("key is mandatory");
            }
            if (type == Type.STRING_TYPE &&
                pattern != null) {
                throw new BuildException("pattern is not suported for string " 
                    + "properties (key:" + key + ")");
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
                ret = (oldValue == null) ? defaultValue : oldValue;
            }
            
            return ret;
        }
        
        /**
         * Enumerated attribute with the values "+", "-", "="
         */
        public static class Operation extends EnumeratedAttribute {

            // Property type operations
            public static final int INCREMENT_OPER =   0;
            public static final int DECREMENT_OPER =   1;
            public static final int EQUALS_OPER =      2;

            public String[] getValues() {
                return new String[] {"+", "-", "="};
            }

            public static int toOperation(String oper) {
                if ("+".equals(oper)) {
                    return INCREMENT_OPER;
                } else if ("-".equals(oper)) {
                    return DECREMENT_OPER;
                }
                return EQUALS_OPER;
            }
        }

        /**
         * Enumerated attribute with the values "int", "date" and "string".
         */
        public static class Type extends EnumeratedAttribute {

            // Property types
            public static final int INTEGER_TYPE =     0;
            public static final int DATE_TYPE =        1;
            public static final int STRING_TYPE =      2;

            public String[] getValues() {
                return new String[] {"int", "date", "string"};
            }

            public static int toType(String type) {
                if ("int".equals(type)) {
                    return INTEGER_TYPE;
                } else if ("date".equals(type)) {
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

        private static final String[] units = {
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
