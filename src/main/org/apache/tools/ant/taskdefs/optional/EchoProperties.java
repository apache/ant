/*
 * Copyright  2002-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.PropertySet;
import org.apache.tools.ant.util.CollectionUtils;
import org.apache.tools.ant.util.DOMElementWriter;
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
 *@author     Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">
 *      groboclown@users.sourceforge.net</a>
 *@author     Ingmar Stein <a href="mailto:stein@xtramind.com">
        stein@xtramind.com</a>
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
     *  prefix string will be recorded.  If this is never set, or it is set
     *  to an empty string or <tt>null</tt>, then all properties will be
     *  recorded. <P>
     *
     *  For example, if the property is set as:
     *    <PRE>&lt;echoproperties  prefix="ant." /&gt;</PRE>
     *  then the property "ant.home" will be recorded, but "ant-example"
     *  will not.
     *
     *@param  prefix  The new prefix value
     */
    public void setPrefix(String prefix) {
        PropertySet ps = new PropertySet();
        ps.setProject(getProject());
        ps.appendPrefix(prefix);
        addPropertyset(ps);
    }

    /**
     * A set of properties to write.
     *
     * @since Ant 1.6
     */
    public void addPropertyset(PropertySet ps) {
        propertySets.addElement(ps);
    }

    public void setFormat(FormatAttribute ea) {
        format = ea.getValue();
    }

    public static class FormatAttribute extends EnumeratedAttribute {
        private String [] formats = new String[]{"xml", "text"};

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
        //copy the properties file
        Hashtable allProps = new Hashtable();

        /* load properties from file if specified, otherwise
        use Ant's properties */
        if (inFile == null && propertySets.size() == 0) {
            // add ant properties
            CollectionUtils.putAll(allProps, getProject().getProperties());
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
                CollectionUtils.putAll(allProps, props);
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
                try {
                    if (null != in) {
                        in.close();
                    }
                } catch (IOException ioe) {
                    //ignore
                }
            }
        }

        Enumeration e = propertySets.elements();
        while (e.hasMoreElements()) {
            PropertySet ps = (PropertySet) e.nextElement();
            CollectionUtils.putAll(allProps, ps.getProperties());
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
     *@param  allProps         propfile to save
     *@param  os               output stream
     *@exception  IOException  trouble
     */
    protected void saveProperties(Hashtable allProps, OutputStream os)
             throws IOException, BuildException {
        Properties props = new Properties();
        Enumeration e = allProps.keys();
        while (e.hasMoreElements()) {
            String name = e.nextElement().toString();
            String value = allProps.get(name).toString();
            props.put(name, value);
        }

        if ("text".equals(format)) {
            jdkSaveProperties(props, os, "Ant properties");
        } else if ("xml".equals(format)) {
            xmlSaveProperties(props, os);
        }
    }

    protected void xmlSaveProperties(Properties props,
                                     OutputStream os) throws IOException {
        // create XML document
        Document doc = getDocumentBuilder().newDocument();
        Element rootElement = doc.createElement(PROPERTIES);

        // output properties
        String name;
        Enumeration e = props.propertyNames();
        while (e.hasMoreElements()) {
            name = (String) e.nextElement();
            Element propElement = doc.createElement(PROPERTY);
            propElement.setAttribute(ATTR_NAME, name);
            propElement.setAttribute(ATTR_VALUE, props.getProperty(name));
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
            if (wri != null) {
                wri.close();
            }
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

