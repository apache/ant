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

package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.xml.sax.AttributeList;
import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Inner class used by EjbJar to facilitate the parsing of deployment
 * descriptors and the capture of appropriate information. Extends
 * HandlerBase so it only implements the methods needed. During parsing
 * creates a hashtable consisting of entries mapping the name it should be
 * inserted into an EJB jar as to a File representing the file on disk. This
 * list can then be accessed through the getFiles() method.
 */
public class DescriptorHandler extends HandlerBase {
    private static final int DEFAULT_HASH_TABLE_SIZE = 10;
    private static final int STATE_LOOKING_EJBJAR = 1;
    private static final int STATE_IN_EJBJAR = 2;
    private static final int STATE_IN_BEANS = 3;
    private static final int STATE_IN_SESSION = 4;
    private static final int STATE_IN_ENTITY = 5;
    private static final int STATE_IN_MESSAGE = 6;

    private Task owningTask;

    private String publicId = null;

    /**
     * Bunch of constants used for storing entries in a hashtable, and for
     * constructing the filenames of various parts of the ejb jar.
     */
    private static final String EJB_REF               = "ejb-ref";
    private static final String EJB_LOCAL_REF         = "ejb-local-ref";
    private static final String HOME_INTERFACE        = "home";
    private static final String REMOTE_INTERFACE      = "remote";
    private static final String LOCAL_HOME_INTERFACE  = "local-home";
    private static final String LOCAL_INTERFACE       = "local";
    private static final String BEAN_CLASS            = "ejb-class";
    private static final String PK_CLASS              = "prim-key-class";
    private static final String EJB_NAME              = "ejb-name";
    private static final String EJB_JAR               = "ejb-jar";
    private static final String ENTERPRISE_BEANS      = "enterprise-beans";
    private static final String ENTITY_BEAN           = "entity";
    private static final String SESSION_BEAN          = "session";
    private static final String MESSAGE_BEAN          = "message-driven";

    /**
     * The state of the parsing
     */
    private int parseState = STATE_LOOKING_EJBJAR;

    // CheckStyle:VisibilityModifier OFF - bc
    /**
     * Instance variable used to store the name of the current element being
     * processed by the SAX parser.  Accessed by the SAX parser call-back methods
     * startElement() and endElement().
     */
    protected String currentElement = null;

    /**
     * The text of the current element
     */
    protected String currentText = null;

    /**
     * Instance variable that stores the names of the files as they will be
     * put into the jar file, mapped to File objects  Accessed by the SAX
     * parser call-back method characters().
     */
    protected Hashtable<String, File> ejbFiles = null;

    /**
     * Instance variable that stores the value found in the &lt;ejb-name&gt; element
     */
    protected String ejbName = null;

    private Map<String, File> fileDTDs = new Hashtable<>();

    private Map<String, String> resourceDTDs = new Hashtable<>();

    private boolean inEJBRef = false;

    private Map<String, URL> urlDTDs = new Hashtable<>();
    // CheckStyle:VisibilityModifier OFF - bc

    /**
     * The directory containing the bean classes and interfaces. This is
     * used for performing dependency file lookups.
     */
    private File srcDir;

    /**
     * Constructor for DescriptorHandler.
     * @param task the task that owns this descriptor
     * @param srcDir the source directory
     */
    public DescriptorHandler(Task task, File srcDir) {
        this.owningTask = task;
        this.srcDir = srcDir;
    }

    /**
     * Register a dtd with a location.
     * The location is one of a filename, a resource name in the classpath, or
     * a URL.
     * @param publicId the public identity of the dtd
     * @param location the location of the dtd
     */
    public void registerDTD(String publicId, String location) {
        if (location == null) {
            return;
        }

        File fileDTD = new File(location);
        if (!fileDTD.exists()) {
            // resolve relative to project basedir
            fileDTD = owningTask.getProject().resolveFile(location);
        }

        if (fileDTD.exists()) {
            if (publicId != null) {
                fileDTDs.put(publicId, fileDTD);
                owningTask.log("Mapped publicId " + publicId + " to file "
                    + fileDTD, Project.MSG_VERBOSE);
            }
            return;
        }

        if (getClass().getResource(location) != null) {
            if (publicId != null) {
                resourceDTDs.put(publicId, location);
                owningTask.log("Mapped publicId " + publicId + " to resource "
                    + location, Project.MSG_VERBOSE);
            }
        }

        try {
            if (publicId != null) {
                URL urldtd = new URL(location);
                urlDTDs.put(publicId, urldtd);
            }
        } catch (MalformedURLException e) {
            //ignored
        }

    }

    /**
     * Resolve the entity.
     * @see org.xml.sax.EntityResolver#resolveEntity(String, String)
     * @param publicId The public identifier, or <code>null</code>
     *                 if none is available.
     * @param systemId The system identifier provided in the XML
     *                 document. Will not be <code>null</code>.
     * @return an inputsource for this identifier
     * @throws SAXException if there is a problem.
     */
    @Override
    public InputSource resolveEntity(String publicId, String systemId)
        throws SAXException {
        this.publicId = publicId;

        File dtdFile = fileDTDs.get(publicId);
        if (dtdFile != null) {
            try {
                owningTask.log("Resolved " + publicId + " to local file "
                    + dtdFile, Project.MSG_VERBOSE);
                return new InputSource(Files.newInputStream(dtdFile.toPath()));
            } catch (IOException ex) {
                // ignore
            }
        }

        String dtdResourceName = resourceDTDs.get(publicId);
        if (dtdResourceName != null) {
            InputStream is = this.getClass().getResourceAsStream(dtdResourceName);
            if (is != null) {
                owningTask.log("Resolved " + publicId + " to local resource "
                    + dtdResourceName, Project.MSG_VERBOSE);
                return new InputSource(is);
            }
        }

        URL dtdUrl = urlDTDs.get(publicId);
        if (dtdUrl != null) {
            try {
                InputStream is = dtdUrl.openStream();
                owningTask.log("Resolved " + publicId + " to url "
                    + dtdUrl, Project.MSG_VERBOSE);
                return new InputSource(is);
            } catch (IOException ioe) {
                //ignore
            }
        }

        owningTask.log("Could not resolve (publicId: " + publicId
            + ", systemId: " + systemId + ") to a local entity", Project.MSG_INFO);

        return null;
    }

    /**
     * Getter method that returns the set of files to include in the EJB jar.
     * @return the map of files
     */
    public Hashtable<String, File> getFiles() {
        return ejbFiles == null ? new Hashtable<>(Collections.emptyMap()) : ejbFiles;
    }

    /**
     * Get the publicId of the DTD
     * @return the public id
     */
    public String getPublicId() {
        return publicId;
    }

     /**
     * Getter method that returns the value of the &lt;ejb-name&gt; element.
     * @return the ejb name
     */
    public String getEjbName() {
        return ejbName;
    }

    /**
     * SAX parser call-back method that is used to initialize the values of some
     * instance variables to ensure safe operation.
     * @throws SAXException on error
     */
    @Override
    public void startDocument() throws SAXException {
        this.ejbFiles = new Hashtable<>(DEFAULT_HASH_TABLE_SIZE, 1);
        this.currentElement = null;
        inEJBRef = false;
    }

    /**
     * SAX parser call-back method that is invoked when a new element is entered
     * into.  Used to store the context (attribute name) in the currentAttribute
     * instance variable.
     * @param name The name of the element being entered.
     * @param attrs Attributes associated to the element.
     * @throws SAXException on error
     */
    @Override
    public void startElement(String name, AttributeList attrs)
        throws SAXException {
        this.currentElement = name;
        currentText = "";
        if (EJB_REF.equals(name) || EJB_LOCAL_REF.equals(name)) {
            inEJBRef = true;
        } else if (parseState == STATE_LOOKING_EJBJAR && EJB_JAR.equals(name)) {
            parseState = STATE_IN_EJBJAR;
        } else if (parseState == STATE_IN_EJBJAR && ENTERPRISE_BEANS.equals(name)) {
            parseState = STATE_IN_BEANS;
        } else if (parseState == STATE_IN_BEANS && SESSION_BEAN.equals(name)) {
            parseState = STATE_IN_SESSION;
        } else if (parseState == STATE_IN_BEANS && ENTITY_BEAN.equals(name)) {
            parseState = STATE_IN_ENTITY;
        } else if (parseState == STATE_IN_BEANS && MESSAGE_BEAN.equals(name)) {
            parseState = STATE_IN_MESSAGE;
        }
    }

    /**
     * SAX parser call-back method that is invoked when an element is exited.
     * Used to blank out (set to the empty string, not nullify) the name of
     * the currentAttribute.  A better method would be to use a stack as an
     * instance variable, however since we are only interested in leaf-node
     * data this is a simpler and workable solution.
     * @param name The name of the attribute being exited. Ignored
     *        in this implementation.
     * @throws SAXException on error
     */
    @Override
    public void endElement(String name) throws SAXException {
        processElement();
        currentText = "";
        this.currentElement = "";
        if (name.equals(EJB_REF) || name.equals(EJB_LOCAL_REF)) {
            inEJBRef = false;
        } else if (parseState == STATE_IN_ENTITY && name.equals(ENTITY_BEAN)) {
            parseState = STATE_IN_BEANS;
        } else if (parseState == STATE_IN_SESSION && name.equals(SESSION_BEAN)) {
            parseState = STATE_IN_BEANS;
        } else if (parseState == STATE_IN_MESSAGE && name.equals(MESSAGE_BEAN)) {
            parseState = STATE_IN_BEANS;
        } else if (parseState == STATE_IN_BEANS && name.equals(ENTERPRISE_BEANS)) {
            parseState = STATE_IN_EJBJAR;
        } else if (parseState == STATE_IN_EJBJAR && name.equals(EJB_JAR)) {
            parseState = STATE_LOOKING_EJBJAR;
        }
    }

    /**
     * SAX parser call-back method invoked whenever characters are located within
     * an element.  currentAttribute (modified by startElement and endElement)
     * tells us whether we are in an interesting element (one of the up to four
     * classes of an EJB).  If so then converts the classname from the format
     * org.apache.tools.ant.Parser to the convention for storing such a class,
     * org/apache/tools/ant/Parser.class.  This is then resolved into a file
     * object under the srcdir which is stored in a Hashtable.
     * @param ch A character array containing all the characters in
     *        the element, and maybe others that should be ignored.
     * @param start An integer marking the position in the char
     *        array to start reading from.
     * @param length An integer representing an offset into the
     *        char array where the current data terminates.
     * @throws SAXException on error
     */
    @Override
    public void characters(char[] ch, int start, int length)
        throws SAXException {
        currentText += new String(ch, start, length);
    }

    /**
     * Called when an endelement is seen.
     * This may be overridden in derived classes.
     * This updates the ejbfiles if the element is an interface or a bean class.
     * This updates the ejbname if the element is an ejb name.
     */
    protected void processElement() {
        if (inEJBRef
            || (parseState != STATE_IN_ENTITY
                && parseState != STATE_IN_SESSION
                && parseState != STATE_IN_MESSAGE)) {
            return;
        }

        if (HOME_INTERFACE.equals(currentElement)
            || REMOTE_INTERFACE.equals(currentElement)
            || LOCAL_INTERFACE.equals(currentElement)
            || LOCAL_HOME_INTERFACE.equals(currentElement)
            || BEAN_CLASS.equals(currentElement)
            || PK_CLASS.equals(currentElement)) {

            // Get the filename into a String object
            String className = currentText.trim();

            // If it's a primitive wrapper then we shouldn't try and put
            // it into the jar, so ignore it.
            if (!className.startsWith("java.")
                && !className.startsWith("javax.")) {
                // Translate periods into path separators, add .class to the
                // name, create the File object and add it to the Hashtable.
                className = className.replace('.', File.separatorChar);
                className += ".class";
                ejbFiles.put(className, new File(srcDir, className));
            }
        }

        // Get the value of the <ejb-name> tag.  Only the first occurrence.
        if (currentElement.equals(EJB_NAME)) {
            if (ejbName == null) {
                ejbName = currentText.trim();
            }
        }
    }
}
