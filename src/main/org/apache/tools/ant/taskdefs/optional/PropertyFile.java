/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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
 *
 */

package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 *PropertyFile task uses java.util.Properties to modify integer, String and
 *Date settings in a property file.<p>
 *
 *
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
 *        &lt;entry key="product.build.date"    type="date" operation="now" /&gt;<br>
 *        &lt;entry key="intInc" type="int" operation="=" value="681"/&gt;<br>
 *        &lt;entry key="intDec" type="int" operation="-"/&gt;<br>
 *        &lt;entry key="NeverDate" type="date" operation="never"/&gt;<br>
 *        &lt;entry key="StringEquals" type="string" value="testValue"/&gt;<br>
 *        &lt;entry key="NowDate" type="date" operation="now"/&gt;<br></ul>
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
 *    <ul><li>key, operation, type</li></ul>
 *
 *If type is unspecified, it defaults to string
 *
 *Parameter values:<br>
 *    <ul><li>operation:</li>
 *        <ul><li>"=" (set -- default)</li>
 *        <li>"-" (dec)</li>
 *        <li>"+" (inc)</li>
 *        <li>"now" (date and time)</li>
 *        <li>"never" (empty string)</li></ul>
 *
 *    <li>type:</li>
 *        <ul><li>"int"</li>
 *        <li>"date"</li>
 *        <li>"string"</li></ul></ul>
 *
 *String property types can only use the "=" operation.
 *Date property types can only use the "never" or "now" operations.
 *Int property types can only use the "=", "-" or "+" operations.<p>
 *
 *The message property is used for the property file header, with "\\" being
 *a newline delimiter charater.
 *
 *    @author     Jeremy Mawson <jem@loftinspace.com.au>
*/
public class PropertyFile extends Task 
{

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
    private String              m_comment;

    private Properties          m_properties;
    private File                m_propertyfile;
    
    private Vector entries = new Vector();
    private Entry mainEntry = new Entry();

    /* ========================================================================
    *
    * Constructors
    */

    /* ========================================================================
    *
    * Methods
    */

    public void execute() throws BuildException 
    {
        checkParameters();
        readFile();
        executeOperation();
        writeFile();
    }

    public Entry createEntry() 
    {
        Entry e = new Entry();
        entries.addElement(e);
        return e;
    }
    
    private void executeOperation() throws BuildException 
    {
      //  mainEntry.executeOn(m_properties);
        for (Enumeration e = entries.elements(); e.hasMoreElements(); ) 
        {
            Entry entry = (Entry)e.nextElement();
            entry.executeOn(m_properties);
        }
    }

    private void readFile() throws BuildException 
    {
        // Create the PropertyFile
        m_properties = new Properties();
        try 
        {
            if (m_propertyfile.exists()) 
            {
                log("Updating property file: "+m_propertyfile.getAbsolutePath());
                m_properties.load(new BufferedInputStream(
                                    new FileInputStream(m_propertyfile)));
            }
            else 
            {
                log("Creating new property file: "+
                    m_propertyfile.getAbsolutePath());
                FileOutputStream out = new FileOutputStream(m_propertyfile.getAbsolutePath());
                out.flush();
                out.close();
            }
        }
        catch(IOException ioe) 
        {
            throw new BuildException(ioe.toString());
        }
    }
    
    private void checkParameters() throws BuildException 
    {
        if (!checkParam(m_propertyfile)) 
        {
            throw new BuildException("file token must not be null.", location);
        }
    }
    
    public void setOperation(String op) 
    {
        mainEntry.setOperation(op);
    }

    public void setType(String type) 
    {
        mainEntry.setType(type);
    }

    public void setValue(String value) 
    {
        mainEntry.setValue(value);
    }

    public void setKey(String key) 
    {
        mainEntry.setKey(key);
    }

    public void setFile(String file) 
    {
        m_propertyfile = new File(file);
    }

    public void setComment(String hdr) 
    {
        m_comment = hdr;
    }

    private void writeFile() throws BuildException 
    {
        BufferedOutputStream bos = null;
        try 
        {
            bos = new BufferedOutputStream(new FileOutputStream(m_propertyfile));

// Write the message if we have one.
//            if (m_comment != null) 
//            {
//                // FIXME: would like to use \n as the newline rather than \\.
//                StringTokenizer tok = new StringTokenizer(m_comment, "\\");
//                  while (tok.hasMoreTokens()) 
//                  {
//                      bos.write("# ".getBytes());
//                      bos.write(((String)tok.nextToken()).getBytes());
//                      bos.write(NEWLINE.getBytes());
//                  }
//                  bos.write(NEWLINE.getBytes());
//                  bos.flush();
//            }
//            Enumeration enumValues = m_properties.elements();
//            Enumeration enumKeys = m_properties.keys();
//            while (enumKeys.hasMoreElements()) 
//            {
//                bos.write(((String)enumKeys.nextElement()).getBytes());
//                bos.write("=".getBytes());
//                bos.write(((String)enumValues.nextElement()).getBytes());
//                bos.write(NEWLINE.getBytes());
//                bos.flush();
//            }

            // Properties.store is not available in JDK 1.1
            Method m = 
                Properties.class.getMethod("store", 
                                           new Class[] {
                                               OutputStream.class,
                                               String.class}
                                           );
            m.invoke(m_properties, new Object[] {bos, m_comment});

        } catch (NoSuchMethodException nsme) {
            m_properties.save(bos, m_comment);
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            throw new BuildException(t, location);
        } catch (IllegalAccessException iae) {
            // impossible
            throw new BuildException(iae, location);
        }
        catch (IOException ioe) 
        {
            throw new BuildException(ioe, location);
        }
        finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException ioex) {}
            }
        }
    }

    /*
    * Returns whether the given parameter has been defined.
    */
    private boolean checkParam(String param) 
    {
        return !((param == null) || (param.equals("null")));
    }

    private boolean checkParam(File param) 
    {
        return !(param == null);
    }
    
    public static class Entry 
    {
        // Property types
        private static final String INTEGER_TYPE =          "int";
        private static final String DATE_TYPE =             "date";
        private static final String STRING_TYPE =           "string";
        
        // Property type operations
        private static final String INCREMENT_OPER =        "+";
        private static final String DECREMENT_OPER =        "-";
        private static final String EQUALS_OPER =           "=";

        // Special values
        private static final String NOW_VALUE =             "now";
        private static final String NULL_VALUE =            "never";
        private static final int    DEFAULT_INT_VALUE =     1;
        private static final GregorianCalendar
            DEFAULT_DATE_VALUE = new GregorianCalendar();

        private String              m_key;
        private String              m_type = null;
        private String              m_operation = null;
        private String              m_value;
        private int                 m_intValue = DEFAULT_INT_VALUE;
        private GregorianCalendar   m_dateValue = DEFAULT_DATE_VALUE;
        
        public void setKey(String value) 
        {
            this.m_key = value;
        }
        public void setValue(String value) 
        {
            this.m_value = value;
            this.setOperation(EQUALS_OPER);
        }
        public void setOperation(String value) 
        {
            this.m_operation = value;
        }
        public void setType(String value) 
        {

            this.m_type = value;
        }

        protected void executeOn(Properties props) throws BuildException 
        {
            // Fork off process per the operation type requested

            // m_type may be null because it wasn't set
            try {
            if (m_type.equals(INTEGER_TYPE)) 
            {
                executeInteger((String)props.get(m_key));
            }
            else if (m_type.equals(DATE_TYPE)) 
            {
                executeDate((String)props.get(m_key));
            }
            else if (m_type.equals(STRING_TYPE)) 
            {
            }
            else 
            {
                throw new BuildException("Unknown operation type: "+m_type+"");
            }
            
            } catch (NullPointerException npe) {
                // Default to string type
                // which means do nothing
            }
            // Insert as a string by default
            props.put(m_key, m_value);

        }
        /*
        * Continue execution for Date values
        * TODO: Modify for different locales and formats
        */
        private void executeDate(String oldValue) throws BuildException 
        {
            StringBuffer dateString = new StringBuffer();

            // If value is defined then interpret what's given
            if (m_operation.equals(NULL_VALUE)) 
            {
                m_dateValue = null;
            }
            else 
            {
                Date now = new Date();
                m_dateValue.setTime(now);
                dateString.append(m_dateValue.get(Calendar.YEAR));
                dateString.append("/");
                dateString.append((m_dateValue.get(Calendar.MONTH)+1 < 10) ? "0" : "");
                dateString.append(m_dateValue.get(Calendar.MONTH)+1);
                dateString.append("/");
                dateString.append((m_dateValue.get(Calendar.DATE) < 10) ? "0" : "");
                dateString.append(m_dateValue.get(Calendar.DATE));
                dateString.append(" ");
                dateString.append((m_dateValue.get(Calendar.HOUR_OF_DAY) < 10) ? "0" : "");
                dateString.append(m_dateValue.get(Calendar.HOUR_OF_DAY));
                dateString.append(":");
                dateString.append((m_dateValue.get(Calendar.MINUTE) < 10) ? "0" : "");
                dateString.append(m_dateValue.get(Calendar.MINUTE));
                m_value = dateString.toString();
            }

            m_value = dateString.toString();
        }


        /*
        * Continue execution for int values
        */
        private void executeInteger(String oldValue) throws BuildException 
        {
            String newValue = "";
            int currentValue = 0;
            try 
            {
                currentValue = new Integer(oldValue).intValue();
            }
            catch (NumberFormatException nfe) 
            {
                // Do nothing
            }

            if (m_operation.equals(INCREMENT_OPER)) 
            {
                currentValue++;
                m_value = new String(""+currentValue);
            }
            else if (m_operation.equals(DECREMENT_OPER)) 
            {
                currentValue--;
                m_value = new String(""+currentValue);
            }
        }

    }
}
