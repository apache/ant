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
package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.util.CollectionUtils;
import org.apache.tools.ant.types.EnumeratedAttribute;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Enumeration;
import java.util.Hashtable;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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

    /**
     *  Prefix string controls which properties to save.
     */
    private String prefix = null;


    private String format = "text";

    /**
     * Sets the input file.
     *
     * @param file  the input file
     */
    public void setSrcfile( File file ) {
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
        this.prefix = prefix;
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
        if(inFile == null) {
            // add ant properties
            CollectionUtils.putAll(allProps, getProject().getProperties());
        } else {
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
                    throw new BuildException( message, getLocation() );
                } else {
                    log( message, Project.MSG_ERR );
                }
                return;
            }

            FileInputStream in = null;
            try {
                in = new FileInputStream( inFile );
                Properties props = new Properties();
                props.load(in);
                CollectionUtils.putAll(allProps, props);
            } catch(FileNotFoundException fnfe) {
                String message =
                    "Could not find file " + inFile.getAbsolutePath();
                if (failonerror) {
                    throw new BuildException(message, fnfe, getLocation());
                } else {
                    log( message, Project.MSG_WARN );
                }
                return;
            } catch( IOException ioe ) {
                String message =
                    "Could not read file " + inFile.getAbsolutePath();
                if (failonerror) {
                    throw new BuildException(message, ioe, getLocation());
                } else {
                    log( message, Project.MSG_WARN );
                }
                return;
            } finally {
                try {
                    if( null != in ) {
                        in.close();
                    }
                } catch(IOException ioe) {}
            }
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
                } catch (IOException e) {
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
        Enumeration enum = allProps.keys();
        while (enum.hasMoreElements()) {
            String name = enum.nextElement().toString();
            String value = allProps.get(name).toString();
            if (prefix == null || name.indexOf(prefix) == 0) {
                props.put(name, value);
            }
        }

        if ("text".equals(format)) {
            jdkSaveProperties(props, os, "Ant properties");
        } else if ("xml".equals(format)) {
            xmlSaveProperties(props, os );
        }
    }

    protected void xmlSaveProperties(Properties props,
                                     OutputStream os) throws IOException {
        // create XML document
        Document doc = getDocumentBuilder().newDocument();
        Element rootElement = doc.createElement( PROPERTIES );

        // output properties
        String name;
        Enumeration e = props.propertyNames();
        while( e.hasMoreElements() ) {
            name = (String)e.nextElement();
            Element propElement = doc.createElement( PROPERTY );
            propElement.setAttribute( ATTR_NAME, name );
            propElement.setAttribute( ATTR_VALUE, props.getProperty( name ) );
            rootElement.appendChild( propElement );
        }

        Writer wri = null;
        try {
            wri = new OutputStreamWriter( os, "UTF8" );
            wri.write( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
            ( new DOMElementWriter() ).write( rootElement, wri, 0, "\t" );
            wri.flush();
        } catch( IOException ioe ) {
            throw new BuildException( "Unable to write XML file", ioe );
        } finally {
            if( wri != null ) {
                wri.close();
            }
        }
    }

    /**
     *  JDK 1.2 allows for the safer method
     *  <tt>Properties.store( OutputStream, String )</tt>, which throws an
     *  <tt>IOException</tt> on an output error.  This method attempts to
     *  use the JDK 1.2 method first, and if that does not exist, then the
     *  JDK 1.0 compatible method
     *  <tt>Properties.save( OutputStream, String )</tt> is used instead.
     *
     *@param props the properties to record
     *@param os record the properties to this output stream
     *@param header prepend this header to the property output
     *@exception IOException on an I/O error during a write.  Only thrown
     *      for JDK 1.2+.
     */
    protected void jdkSaveProperties(Properties props, OutputStream os,
                                     String header) throws IOException {
        try {
            java.lang.reflect.Method m = props.getClass().getMethod(
                "store", new Class[]{OutputStream.class, String.class});
            m.invoke(props, new Object[]{os, header});
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            if (t instanceof IOException) {
                throw (IOException) t;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }

            // not an expected exception.  Resort to JDK 1.0 to execute
            // this method
            jdk10SaveProperties(props, os, header);
        } catch (ThreadDeath td) {
            // don't trap thread death errors.
            throw td;
        } catch (Throwable ex) {
            // this 'store' method is not available, so resort to the JDK 1.0
            // compatible method.
            jdk10SaveProperties(props, os, header);
        }
    }


    /**
     * Save the properties to the output stream using the JDK 1.0 compatible
     * method.  This won't throw an <tt>IOException</tt> on an output error.
     *
     *@param props the properties to record
     *@param os record the properties to this output stream
     *@param header prepend this header to the property output
     */
    protected void jdk10SaveProperties(Properties props, OutputStream os,
                                       String header) {
        props.save(os, header);
    }

    /**
     * Uses the DocumentBuilderFactory to get a DocumentBuilder instance.
     *
     * @return   The DocumentBuilder instance
     */
    private static DocumentBuilder getDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch( Exception e ) {
            throw new ExceptionInInitializerError( e );
        }
    }
}

