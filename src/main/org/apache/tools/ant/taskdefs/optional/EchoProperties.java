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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.PropertySet;
import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Displays all the current properties in the build. The output can be sent to
 * a file if desired.
 * <p>
 * Attribute "destfile" defines a file to send the properties to. This can be
 * processed as a standard property file later.
 * </p>
 * <p>
 * Attribute "prefix" defines a prefix which is used to filter the properties
 * only those properties starting with this prefix will be echoed.
 * </p>
 * <p>
 * By default, the "failonerror" attribute is enabled. If an error occurs while
 * writing the properties to a file, and this attribute is enabled, then a
 * BuildException will be thrown. If disabled, then IO errors will be reported
 * as a log statement, but no error will be thrown.
 * </p>
 * <p>Examples:</p><pre>
 *  &lt;echoproperties  /&gt;
 * </pre>
 * Report the current properties to the log.
 * <pre>
 *  &lt;echoproperties destfile="my.properties" /&gt;
 * </pre>
 * Report the current properties to the file "my.properties", and will
 * fail the build if the file could not be created or written to.
 * <pre>
 *  &lt;echoproperties destfile="my.properties" failonerror="false"
 *      prefix="ant" /&gt;
 * </pre>
 * Report all properties beginning with 'ant' to the file
 * "my.properties", and will log a message if the file could not be created or
 * written to, but will still allow the build to continue.
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
     * File object pointing to the output file. If this is null, then
     * we output to the project log, not to a file.
     */
    private File destfile = null;

    /**
     * If this is true, then errors generated during file output will become
     * build errors, and if false, then such errors will be logged, but not
     * thrown.
     */
    private boolean failonerror = true;

    private List<PropertySet> propertySets = new Vector<>();

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
     * @param destfile file to store the property output
     */
    public void setDestfile(File destfile) {
        this.destfile = destfile;
    }

    /**
     * If true, the task will fail if an error occurs writing the properties
     * file, otherwise errors are just logged.
     *
     * @param failonerror  <code>true</code> if IO exceptions are reported as build
     *      exceptions, or <code>false</code> if IO exceptions are ignored.
     */
    public void setFailOnError(boolean failonerror) {
        this.failonerror = failonerror;
    }

    /**
     * If the prefix is set, then only properties which start with this
     * prefix string will be recorded. If regex is not set and  if this
     * is never set, or it is set to an empty string or <code>null</code>,
     * then all properties will be recorded.
     *
     * <p>For example, if the attribute is set as:</p>
     * <pre>&lt;echoproperties  prefix="ant." /&gt;</pre>
     * then the property "ant.home" will be recorded, but "ant-example"
     * will not.
     *
     * @param  prefix  The new prefix value
     */
    public void setPrefix(String prefix) {
        if (prefix != null && !prefix.isEmpty()) {
            this.prefix = prefix;
            PropertySet ps = new PropertySet();
            ps.setProject(getProject());
            ps.appendPrefix(prefix);
            addPropertyset(ps);
        }
    }

    /**
     * If the regex is set, then only properties whose names match it
     * will be recorded.  If prefix is not set and if this is never set,
     * or it is set to an empty string or <code>null</code>, then all
     * properties will be recorded.
     *
     * <p>For example, if the attribute is set as:</p>
     * <pre>&lt;echoproperties  prefix=".*ant.*" /&gt;</pre>
     * then the properties "ant.home" and "user.variant" will be recorded,
     * but "ant-example" will not.
     *
     * @param  regex  The new regex value
     *
     * @since Ant 1.7
     */
    public void setRegex(String regex) {
        if (regex != null && !regex.isEmpty()) {
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
        propertySets.add(ps);
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
        private String[] formats = new String[] {"xml", "text"};

        /**
         * @see EnumeratedAttribute#getValues()
         * @return accepted values
         */
        @Override
        public String[] getValues() {
            return formats;
        }
    }

    /**
     * Run the task.
     *
     * @exception BuildException  trouble, probably file IO
     */
    @Override
    public void execute() throws BuildException {
        if (prefix != null && regex != null) {
            throw new BuildException(
                "Please specify either prefix or regex, but not both",
                getLocation());
        }
        //copy the properties file
        Hashtable<Object, Object> allProps = new Hashtable<>();

        /* load properties from file if specified, otherwise
        use Ant's properties */
        if (inFile == null && propertySets.isEmpty()) {
            // add ant properties
            allProps.putAll(getProject().getProperties());
        } else if (inFile != null) {
            if (inFile.isDirectory()) {
                String message = "srcfile is a directory!";
                if (failonerror) {
                    throw new BuildException(message, getLocation());
                }
                log(message, Project.MSG_ERR);
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

            try (InputStream in = Files.newInputStream(inFile.toPath())) {
                Properties props = new Properties();
                props.load(in);
                allProps.putAll(props);
            } catch (FileNotFoundException fnfe) {
                String message =
                    "Could not find file " + inFile.getAbsolutePath();
                if (failonerror) {
                    throw new BuildException(message, fnfe, getLocation());
                }
                log(message, Project.MSG_WARN);
                return;
            } catch (IOException ioe) {
                String message =
                    "Could not read file " + inFile.getAbsolutePath();
                if (failonerror) {
                    throw new BuildException(message, ioe, getLocation());
                }
                log(message, Project.MSG_WARN);
                return;
            }
        }

        propertySets.stream().map(PropertySet::getProperties)
            .forEach(allProps::putAll);

        try (OutputStream os = createOutputStream()) {
            if (os != null) {
                saveProperties(allProps, os);
            }
        } catch (IOException ioe) {
            if (failonerror) {
                throw new BuildException(ioe, getLocation());
            }
            log(ioe.getMessage(), Project.MSG_INFO);
        }
    }

    /**
     * Send the key/value pairs in the hashtable to the given output stream.
     * Only those properties matching the <code>prefix</code> constraint will be
     * sent to the output stream.
     * The output stream will be closed when this method returns.
     *
     * @param  allProps         propfile to save
     * @param  os               output stream
     * @throws IOException      on output errors
     * @throws BuildException   on other errors
     */
    protected void saveProperties(Hashtable<Object, Object> allProps, OutputStream os)
        throws IOException, BuildException {
        final List<Object> keyList = new ArrayList<>(allProps.keySet());

        Properties props = new Properties() {
            private static final long serialVersionUID = 5090936442309201654L;

            @Override
            public Enumeration<Object> keys() {
                return keyList.stream()
                    .sorted(Comparator.comparing(Object::toString))
                    .collect(Collectors.collectingAndThen(Collectors.toList(),
                        Collections::enumeration));
            }

            @Override
            public Set<Map.Entry<Object, Object>> entrySet() {
                Set<Map.Entry<Object, Object>> result = super.entrySet();
                if (JavaEnvUtils.isKaffe()) {
                    Set<Map.Entry<Object, Object>> t =
                        new TreeSet<>(Comparator.comparing(
                            ((Function<Map.Entry<Object, Object>, Object>) Map.Entry::getKey)
                                .andThen(Object::toString)));
                    t.addAll(result);
                    return t;
                }
                return result;
            }
        };
        allProps.forEach((k, v) -> props.put(String.valueOf(k), String.valueOf(v)));

        if ("text".equals(format)) {
            jdkSaveProperties(props, os, "Ant properties");
        } else if ("xml".equals(format)) {
            xmlSaveProperties(props, os);
        }
    }

    /**
     * a tuple for the sort list.
     */
    private static final class Tuple implements Comparable<Tuple> {
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
        @Override
        public int compareTo(Tuple o) {
            return Comparator.<String> naturalOrder().compare(key, o.key);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o == null || o.getClass() != getClass()) {
                return false;
            }
            Tuple that = (Tuple) o;
            return Objects.equals(key, that.key)
                && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }

    private List<Tuple> sortProperties(Properties props) {
        // sort the list. Makes SCM and manual diffs easier.
        return props.stringPropertyNames().stream()
            .map(k -> new Tuple(k, props.getProperty(k))).sorted()
            .collect(Collectors.toList());
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

        List<Tuple> sorted = sortProperties(props);

        // output properties
        for (Tuple tuple : sorted) {
            Element propElement = doc.createElement(PROPERTY);
            propElement.setAttribute(ATTR_NAME, tuple.key);
            propElement.setAttribute(ATTR_VALUE, tuple.value);
            rootElement.appendChild(propElement);
        }

        try (Writer wri = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
            wri.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            new DOMElementWriter().write(rootElement, wri, 0, "\t");
            wri.flush();
        } catch (IOException ioe) {
            throw new BuildException("Unable to write XML file", ioe);
        }
    }

    /**
     * JDK 1.2 allows for the safer method
     * <code>Properties.store(OutputStream, String)</code>, which throws an
     * <code>IOException</code> on an output error.
     *
     * @param props the properties to record
     * @param os record the properties to this output stream
     * @param header prepend this header to the property output
     * @exception IOException on an I/O error during a write.
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

    private OutputStream createOutputStream() throws IOException {
        if (destfile == null) {
            return new LogOutputStream(this);
        }
        if (destfile.exists() && destfile.isDirectory()) {
            String message = "destfile is a directory!";
            if (failonerror) {
                throw new BuildException(message, getLocation());
            }
            log(message, Project.MSG_ERR);
            return null;
        }
        if (destfile.exists() && !destfile.canWrite()) {
            String message =
                "Can not write to the specified destfile!";
            if (failonerror) {
                throw new BuildException(message, getLocation());
            }
            log(message, Project.MSG_ERR);
            return null;
        }
        return Files.newOutputStream(this.destfile.toPath());
    }

    /**
     * Uses the DocumentBuilderFactory to get a DocumentBuilder instance.
     *
     * @return The DocumentBuilder instance
     */
    private static DocumentBuilder getDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
