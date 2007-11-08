/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs.optional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Collections;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.PropertySet;
import org.apache.tools.ant.util.CollectionUtils;
import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *  Displays all the current properties in the build. The output can be sent to
 *  a file if desired. <P>
 *
 *  Attribute "destfile" defines a file to send the properties to. This can be
 *  processed as a standard property file later. <P>
 *
 *  Attribute "prefix" defines a prefix which is used to filter the properties
 *  only those properties starting with this prefix will be echoed. <P>
 *
 *  By default, the "failonerror" attribute is enabled. If an error occurs while
 *  writing the properties to a file, and this attribute is enabled, then a
 *  BuildException will be thrown. If disabled, then IO errors will be reported
 *  as a log statement, but no error will be thrown. <P>
 *
 *  Examples: <pre>
 *  &lt;echoproperties  /&gt;
 * </pre> Report the current properties to the log. <P>
 *
 *  <pre>
 *  &lt;echoproperties destfile="my.properties" /&gt;
 * </pre> Report the current properties to the file "my.properties", and will
 *  fail the build if the file could not be created or written to. <P>
 *
 *  <pre>
 *  &lt;echoproperties destfile="my.properties" failonerror="false"
 *      prefix="ant" /&gt;
 * </pre> Report all properties beginning with 'ant' to the file
 *  "my.properties", and will log a message if the file could not be created or
 *  written to, but will still allow the build to continue.
 *
 *@since      Ant 1.5
 */
public class EchoProperties extends Task {

    /**
     * the properties element.
     */
    private static final String PROPERTIES = "properties";

    /**
     * the property element.
     */
    private static final String PROPERTY = "property";

    /**
     * name attribute for property, testcase and testsuite elements.
     */
    private static final String ATTR_NAME = "name";

    /**
     * value attribute for property elements.
     */
    private static final String ATTR_VALUE = "value";

    /**
     * the input file.
     */
    private File inFile = null;

    /**
     *  File object pointing to the output file. If this is null, then
     *  we output to the project log, not to a file.
     */
    private File destfile = null;

    /**
     *  If this is true, then errors generated during file output will become
     *  build errors, and if false, then such errors will be logged, but not
     *  thrown.
     */
    private boolean failonerror = true;

    private Vector propertySets = new Vector();

    private String format = "text";

    private String prefix;

    /**
     * @since Ant 1.7
     */
    private String regex;

    /**
     * Sets the input file.
     *
     * @param file  the input file
     */
    public void setSrcfile(File file) {
        inFile = file;
    }

    /**
     *  Set a file to store the property output.  If this is never specified,
     *  then the output will be sent to the Ant log.
     *
     *@param destfile file to store the property output
     */
    public void setDestfile(File destfile) {
        this.destfile = destfile;
    }


    /**
     * If true, the task will fail if an error occurs writing the properties
     * file, otherwise errors are just logged.
     *
     *@param  failonerror  <tt>true</tt> if IO exceptions are reported as build
     *      exceptions, or <tt>false</tt> if IO exceptions are ignored.
     */
    public void setFailOnError(boolean failonerror) {
        this.failonerror = failonerror;
    }


    /**
     *  If the prefix is set, then only properties which start with this
     *  prefix string will be recorded. If regex is not set and  if this
     *  is never set, or it is set to an empty string or <tt>null</tt>,
     *  then all properties will be recorded. <P>
     *
     *  For example, if the attribute is set as:
     *    <PRE>&lt;echoproperties  prefix="ant." /&gt;</PRE>
     *  then the property "ant.home" will be recorded, but "ant-example"
     *  will not.
     *
     * @param  prefix  The new prefix value
     */
    public void setPrefix(String prefix) {
        if (prefix != null && prefix.length() != 0) {
            this.prefix = prefix;
            PropertySet ps = new PropertySet();
            ps.setProject(getProject());
            ps.appendPrefix(prefix);
            addPropertyset(ps);
        }
    }

    /**
     *  If the regex is set, then only properties whose names match it
     *  will be recorded.  If prefix is not set and if this is never set,
     *  or it is set to an empty string or <tt>null</tt>, then all
     *  properties will be recorded.<P>
     *
     *  For example, if the attribute is set as:
     *    <PRE>&lt;echoproperties  prefix=".*ant.*" /&gt;</PRE>
     *  then the properties "ant.home" and "user.variant" will be recorded,
     *  but "ant-example" will not.
     *
     * @param  regex  The new regex value
     *
     * @since Ant 1.7
     */
    public void setRegex(String regex) {
        if (regex != null && regex.length() != 0) {
            this.regex = regex;
            PropertySet ps = new PropertySet();
            ps.setProject(getProject());
            ps.appendRegex(regex);
            addPropertyset(ps);
        }
    }

    /**
     * A set of properties to write.
     * @param ps the property set to write
     * @since Ant 1.6
     */
    public void addPropertyset(PropertySet ps) {
        propertySets.addElement(ps);
    }

    /**
     * Set the output format - xml or text.
     * @param ea an enumerated <code>FormatAttribute</code> value
     */
    public void setFormat(FormatAttribute ea) {
        format = ea.getValue();
    }

    /**
     * A enumerated type for the format attribute.
     * The values are "xml" and "text".
     */
    public static class FormatAttribute extends EnumeratedAttribute {
        private String [] formats = new String[]{"xml", "text"};

        /**
         * @see EnumeratedAttribute#getValues()
         * @return accepted values
         */
        public String[] getValues() {
            return formats;
        }
    }

    /**
     *  Run the task.
     *
     *@exception  BuildException  trouble, probably file IO
     */
    public void execute() throws BuildException {
        if (prefix != null && regex != null) {
            throw new BuildException("Please specify either prefix"
                    + " or regex, but not both", getLocation());
        }
        //copy the properties file
        Hashtable allProps = new Hashtable();

        /* load properties from file if specified, otherwise
        use Ant's properties */
        if (inFile == null && propertySets.size() == 0) {
            // add ant properties
            allProps.putAll(getProject().getProperties());
        } else if (inFile != null) {
            if (inFile.exists() && inFile.isDirectory()) {
                String message = "srcfile is a directory!";
                if (failonerror) {
                    throw new BuildException(message, getLocation());
                } else {
                    log(message, Project.MSG_ERR);
                }
                return;
            }

            if (inFile.exists() && !inFile.canRead()) {
                String message = "Can not read from the specified srcfile!";
                if (failonerror) {
                    throw new BuildException(message, getLocation());
                } else {
                    log(message, Project.MSG_ERR);
                }
                return;
            }

            FileInputStream in = null;
            try {
                in = new FileInputStream(inFile);
                Properties props = new Properties();
                props.load(in);
                allProps.putAll(props);
            } catch (FileNotFoundException fnfe) {
                String message =
                    "Could not find file " + inFile.getAbsolutePath();
                if (failonerror) {
                    throw new BuildException(message, fnfe, getLocation());
                } else {
                    log(message, Project.MSG_WARN);
                }
                return;
            } catch (IOException ioe) {
                String message =
                    "Could not read file " + inFile.getAbsolutePath();
                if (failonerror) {
                    throw new BuildException(message, ioe, getLocation());
                } else {
                    log(message, Project.MSG_WARN);
                }
                return;
            } finally {
                FileUtils.close(in);
            }
        }

        Enumeration e = propertySets.elements();
        while (e.hasMoreElements()) {
            PropertySet ps = (PropertySet) e.nextElement();
            allProps.putAll(ps.getProperties());
        }

        OutputStream os = null;
        try {
            if (destfile == null) {
                os = new ByteArrayOutputStream();
                saveProperties(allProps, os);
                log(os.toString(), Project.MSG_INFO);
            } else {
                if (destfile.exists() && destfile.isDirectory()) {
                    String message = "destfile is a directory!";
                    if (failonerror) {
                        throw new BuildException(message, getLocation());
                    } else {
                        log(message, Project.MSG_ERR);
                    }
                    return;
                }

                if (destfile.exists() && !destfile.canWrite()) {
                    String message =
                        "Can not write to the specified destfile!";
                    if (failonerror) {
                        throw new BuildException(message, getLocation());
                    } else {
                        log(message, Project.MSG_ERR);
                    }
                    return;
                }
                os = new FileOutputStream(this.destfile);
                saveProperties(allProps, os);
            }
        } catch (IOException ioe) {
            if (failonerror) {
                throw new BuildException(ioe, getLocation());
            } else {
                log(ioe.getMessage(), Project.MSG_INFO);
            }
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ex) {
                    //ignore
                }
            }
        }
    }


    /**
     *  Send the key/value pairs in the hashtable to the given output stream.
     *  Only those properties matching the <tt>prefix</tt> constraint will be
     *  sent to the output stream.
     *  The output stream will be closed when this method returns.
     *
     * @param  allProps         propfile to save
     * @param  os               output stream
     * @throws IOException      on output errors
     * @throws BuildException   on other errors
     */
    protected void saveProperties(Hashtable allProps, OutputStream os)
        throws IOException, BuildException {
        final List keyList = new ArrayList(allProps.keySet());
        Collections.sort(keyList);
        Properties props = new Properties() {
            private static final long serialVersionUID = 5090936442309201654L;
            public Enumeration keys() {
                return CollectionUtils.asEnumeration(keyList.iterator());
            }
            public Set entrySet() {
                Set result = super.entrySet();
                if (JavaEnvUtils.isKaffe()) {
                    TreeSet t = new TreeSet(new Comparator() {
                        public int compare(Object o1, Object o2) {
                            String key1 = (String) ((Map.Entry) o1).getKey();
                            String key2 = (String) ((Map.Entry) o2).getKey();
                            return key1.compareTo(key2);
                        }
                    });
                    t.addAll(result);
                    result = t;
                }
                return result;
            }
        };
        for (int i = 0; i < keyList.size(); i++) {
            String name = keyList.get(i).toString();
            String value = allProps.get(name).toString();
            props.setProperty(name, value);
        }
        if ("text".equals(format)) {
            jdkSaveProperties(props, os, "Ant properties");
        } else if ("xml".equals(format)) {
            xmlSaveProperties(props, os);
        }
    }

    /**
     * a tuple for the sort list.
     */
    private static final class Tuple implements Comparable {
        private String key;
        private String value;

        private Tuple(String key, String value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Compares this object with the specified object for order.
         * @param o the Object to be compared.
         * @return a negative integer, zero, or a positive integer as this object is
         *         less than, equal to, or greater than the specified object.
         * @throws ClassCastException if the specified object's type prevents it
         *                            from being compared to this Object.
         */
        public int compareTo(Object o) {
            Tuple that = (Tuple) o;
            return key.compareTo(that.key);
        }
    }

    private List sortProperties(Properties props) {
        //sort the list. Makes SCM and manual diffs easier.
        List sorted = new ArrayList(props.size());
        Enumeration e = props.propertyNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            sorted.add(new Tuple(name, props.getProperty(name)));
        }
        Collections.sort(sorted);
        return sorted;
    }

    /**
     * Output the properties as xml output.
     * @param props the properties to save
     * @param os    the output stream to write to (Note this gets closed)
     * @throws IOException on error in writing to the stream
     */
    protected void xmlSaveProperties(Properties props,
                                     OutputStream os) throws IOException {
        // create XML document
        Document doc = getDocumentBuilder().newDocument();
        Element rootElement = doc.createElement(PROPERTIES);

        List sorted = sortProperties(props);


        // output properties
        Iterator iten = sorted.iterator();
        while (iten.hasNext()) {
            Tuple tuple = (Tuple) iten.next();
            Element propElement = doc.createElement(PROPERTY);
            propElement.setAttribute(ATTR_NAME, tuple.key);
            propElement.setAttribute(ATTR_VALUE, tuple.value);
            rootElement.appendChild(propElement);
        }

        Writer wri = null;
        try {
            wri = new OutputStreamWriter(os, "UTF8");
            wri.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            (new DOMElementWriter()).write(rootElement, wri, 0, "\t");
            wri.flush();
        } catch (IOException ioe) {
            throw new BuildException("Unable to write XML file", ioe);
        } finally {
            FileUtils.close(wri);
        }
    }

    /**
     *  JDK 1.2 allows for the safer method
     *  <tt>Properties.store(OutputStream, String)</tt>, which throws an
     *  <tt>IOException</tt> on an output error.
     *
     *@param props the properties to record
     *@param os record the properties to this output stream
     *@param header prepend this header to the property output
     *@exception IOException on an I/O error during a write.
     */
    protected void jdkSaveProperties(Properties props, OutputStream os,
                                     String header) throws IOException {
       try {
           props.store(os, header);

       } catch (IOException ioe) {
           throw new BuildException(ioe, getLocation());
       } finally {
           if (os != null) {
               try {
                   os.close();
               } catch (IOException ioex) {
                   log("Failed to close output stream");
               }
           }
       }
    }


    /**
     * Uses the DocumentBuilderFactory to get a DocumentBuilder instance.
     *
     * @return   The DocumentBuilder instance
     */
    private static DocumentBuilder getDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}

