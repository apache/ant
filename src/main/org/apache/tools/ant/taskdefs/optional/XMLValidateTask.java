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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.Vector;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.DTDLocation;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.XMLCatalog;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JAXPUtils;
import org.apache.tools.ant.util.XmlConstants;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.ParserAdapter;

/**
 * Checks XML files are valid (or only well formed). The
 * task uses the SAX2 parser implementation provided by JAXP by default
 * (probably the one that is used by Ant itself), but one can specify any
 * SAX1/2 parser if needed.
 *
 */
public class XMLValidateTask extends Task {

    /**
     * helper for path -> URI and URI -> path conversions.
     */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    protected static final String INIT_FAILED_MSG =
        "Could not start xml validation: ";

    // ant task properties
    // defaults
    // CheckStyle:VisibilityModifier OFF - bc
    protected boolean failOnError = true;
    protected boolean warn = true;
    protected boolean lenient = false;
    protected String readerClassName = null;

    /** file to be validated */
    protected File file = null;
    /** sets of file to be validated */
    protected Vector<FileSet> filesets = new Vector<>();
    protected Path classpath;

    /**
     * the parser is viewed as a SAX2 XMLReader. If a SAX1 parser is specified,
     * it's wrapped in an adapter that make it behave as a XMLReader.
     * a more 'standard' way of doing this would be to use the JAXP1.1 SAXParser
     * interface.
     */
    protected XMLReader xmlReader = null;
    // XMLReader used to validation process
    protected ValidatorErrorHandler errorHandler = new ValidatorErrorHandler();
    // to report sax parsing errors
    // CheckStyle:VisibilityModifier ON

    /** The vector to store all attributes (features) to be set on the parser. **/
    private Vector<Attribute> attributeList = new Vector<>();

    /**
     * List of properties.
     */
    private final Vector<Property> propertyList = new Vector<>();

    private XMLCatalog xmlCatalog = new XMLCatalog();
    /** Message for successful validation */
    public static final String MESSAGE_FILES_VALIDATED
        = " file(s) have been successfully validated.";

    private AntClassLoader readerLoader = null;

    /**
     * Specify how parser error are to be handled.
     * Optional, default is <code>true</code>.
     * <p>
     * If set to <code>true</code> (default), throw a buildException if the
     * parser yields an error.
     * @param fail if set to <code>false</code> do not fail on error
     */
    public void setFailOnError(boolean fail) {
        failOnError = fail;
    }

    /**
     * Specify how parser error are to be handled.
     * <p>
     * If set to <code>true</code> (default), log a warn message for each SAX warn event.
     * @param bool if set to <code>false</code> do not send warnings
     */
    public void setWarn(boolean bool) {
        warn = bool;
    }

    /**
     * Specify whether the parser should be validating. Default
     * is <code>true</code>.
     * <p>
     * If set to false, the validation will fail only if the parsed document
     * is not well formed XML.
     * <p>
     * this option is ignored if the specified class
     * with {@link #setClassName(String)} is not a SAX2 XMLReader.
     * @param bool if set to <code>false</code> only fail on malformed XML
     */
    public void setLenient(boolean bool) {
        lenient = bool;
    }

    /**
     * Specify the class name of the SAX parser to be used. (optional)
     * @param className should be an implementation of SAX2
     * <code>org.xml.sax.XMLReader</code> or SAX2 <code>org.xml.sax.Parser</code>.
     * <p>If className is an implementation of
     * <code>org.xml.sax.Parser</code>, {@link #setLenient(boolean)},
     * will be ignored.</p>
     * <p>If not set, the default will be used.</p>
     * @see org.xml.sax.XMLReader
     * @see org.xml.sax.Parser
     */
    public void setClassName(String className) {
        readerClassName = className;
    }

    /**
     * Specify the classpath to be searched to load the parser (optional)
     * @param classpath the classpath to load the parser
     */
    public void setClasspath(Path classpath) {
        if (this.classpath == null) {
            this.classpath = classpath;
        } else {
            this.classpath.append(classpath);
        }
    }

    /**
     * @see #setClasspath
     * @return the classpath created
     */
    public Path createClasspath() {
        if (this.classpath == null) {
            this.classpath = new Path(getProject());
        }
        return this.classpath.createPath();
    }

    /**
     * Where to find the parser class; optional.
     * @see #setClasspath
     * @param r reference to a classpath defined elsewhere
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * specify the file to be checked; optional.
     * @param file the file to be checked
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * add an XMLCatalog as a nested element; optional.
     * @param catalog XMLCatalog to use
     */
    public void addConfiguredXMLCatalog(XMLCatalog catalog) {
        xmlCatalog.addConfiguredXMLCatalog(catalog);
    }

    /**
     * specify a set of file to be checked
     * @param set the fileset to check
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Add an attribute nested element. This is used for setting arbitrary
     * features of the SAX parser.
     * Valid attributes
     * <a href=
     * "http://www.saxproject.org/apidoc/org/xml/sax/package-summary.html#package_description"
     * >include</a>
     * @return attribute created
     * @since ant1.6
     */
    public Attribute createAttribute() {
        final Attribute feature = new Attribute();
        attributeList.addElement(feature);
        return feature;
    }

    /**
     * Creates a property.
     *
     * @return a property.
     * @since ant 1.6.2
     */
    public Property createProperty() {
        final Property prop = new Property();
        propertyList.addElement(prop);
        return prop;
    }

    /**
     * Called by the project to let the task initialize properly.
     *
     * @exception BuildException if something goes wrong with the build
     */
    public void init() throws BuildException {
        super.init();
        xmlCatalog.setProject(getProject());
    }

    /**
     * Create a DTD location record; optional.
     * This stores the location of a DTD. The DTD is identified
     * by its public Id.
     * @return created DTD location
     */
    public DTDLocation createDTD() {
        DTDLocation dtdLocation = new DTDLocation();
        xmlCatalog.addDTD(dtdLocation);
        return dtdLocation;
    }
    /**
     * accessor to the xmlCatalog used in the task
     * @return xmlCatalog reference
     */
    protected EntityResolver getEntityResolver() {
        return xmlCatalog;
    }

    /**
     * get the XML reader. Non-null only after {@link #initValidator()}.
     * If the reader is an instance of  {@link ParserAdapter} then
     * the parser is a SAX1 parser, and you cannot call
     * {@link #setFeature(String, boolean)} or {@link #setProperty(String, String)}
     * on it.
     * @return the XML reader or null.
     */
    protected XMLReader getXmlReader() {
        return xmlReader;
    }

    /**
     * execute the task
     * @throws BuildException if <code>failonerror</code> is true and an error happens
     */
    public void execute() throws BuildException {
        try {
            int fileProcessed = 0;
            if (file == null && filesets.isEmpty()) {
                throw new BuildException(
                    "Specify at least one source - " + "a file or a fileset.");
            }

            if (file != null) {
                if (file.exists() && file.canRead() && file.isFile()) {
                    doValidate(file);
                    fileProcessed++;
                } else {
                    String errorMsg = "File " + file + " cannot be read";
                    if (failOnError) {
                        throw new BuildException(errorMsg);
                    } else {
                        log(errorMsg, Project.MSG_ERR);
                    }
                }
            }

            for (final FileSet fs : filesets) {
                DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                for (String fileName : ds.getIncludedFiles()) {
                    File srcFile = new File(fs.getDir(getProject()), fileName);
                    doValidate(srcFile);
                    fileProcessed++;
                }
            }
            onSuccessfulValidation(fileProcessed);
        } finally {
            cleanup();
        }
    }

    /**
     * handler called on successful file validation.
     * @param fileProcessed number of files processed.
     */
    protected void onSuccessfulValidation(int fileProcessed) {
        log(fileProcessed + MESSAGE_FILES_VALIDATED);
    }

    /**
     * init the parser :
     * load the parser class, and set features if necessary
     * It is only after this that the reader is valid
     * @throws BuildException if something went wrong
     */
    protected void initValidator() {

        xmlReader = createXmlReader();

        xmlReader.setEntityResolver(getEntityResolver());
        xmlReader.setErrorHandler(errorHandler);

        if (!isSax1Parser()) {
            // turn validation on
            if (!lenient) {
                setFeature(XmlConstants.FEATURE_VALIDATION, true);
            }
            // set the feature from the attribute list
            for (final Attribute feature : attributeList) {
                setFeature(feature.getName(), feature.getValue());

            }
            // Sets properties
            for (final Property prop : propertyList) {
                setProperty(prop.getName(), prop.getValue());
            }
        }
    }

    /**
     * test that returns true if we are using a SAX1 parser.
     * @return true when a SAX1 parser is in use
     */
    protected boolean isSax1Parser() {
        return (xmlReader instanceof ParserAdapter);
    }

    /**
     * create the XML reader.
     * This is one by instantiating anything specified by {@link #readerClassName},
     * falling back to a default reader if not.
     * If the returned reader is an instance of {@link ParserAdapter} then
     * we have created and wrapped a SAX1 parser.
     * @return the new XMLReader.
     */
    protected XMLReader createXmlReader() {
        Object reader = null;
        if (readerClassName == null) {
            reader = createDefaultReaderOrParser();
        } else {

            Class<?> readerClass = null;
            try {
                // load the parser class
                if (classpath != null) {
                    readerLoader = getProject().createClassLoader(classpath);
                    readerClass = Class.forName(readerClassName, true,
                                                readerLoader);
                } else {
                    readerClass = Class.forName(readerClassName);
                }

                reader = readerClass.getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException
                    | NoSuchMethodException | InvocationTargetException e) {
                throw new BuildException(INIT_FAILED_MSG + readerClassName, e);
            }
        }

        // then check it implements XMLReader
        XMLReader newReader;
        if (reader instanceof XMLReader) {
            newReader = (XMLReader) reader;
            log(
                "Using SAX2 reader " + reader.getClass().getName(),
                Project.MSG_VERBOSE);
        } else {

            // see if it is a SAX1 Parser
            if (reader instanceof Parser) {
                newReader = new ParserAdapter((Parser) reader);
                log(
                    "Using SAX1 parser " + reader.getClass().getName(),
                    Project.MSG_VERBOSE);
            } else {
                throw new BuildException(
                    INIT_FAILED_MSG
                        + reader.getClass().getName()
                        + " implements nor SAX1 Parser nor SAX2 XMLReader.");
            }
        }
        return newReader;
    }

    /**
     * Cleans up resources.
     *
     * @since Ant 1.8.0
     */
    protected void cleanup() {
        if (readerLoader != null) {
            readerLoader.cleanup();
            readerLoader = null;
        }
    }

    /**
     * Returns a SAX-based XMLReader or a SAX-based Parser.
     * @return reader or parser
     */
    private Object createDefaultReaderOrParser() {
        Object reader;
        try {
            reader = createDefaultReader();
        } catch (BuildException exc) {
            reader = JAXPUtils.getParser();
        }
        return reader;
    }

    /**
     * Create a reader if the use of the class did not specify another one.
     * If a BuildException is thrown, the caller may revert to an alternate
     * reader.
     * @return a new reader.
     * @throws BuildException if something went wrong
     */
    protected XMLReader createDefaultReader() {
        return JAXPUtils.getXMLReader();
    }

    /**
     * Set a feature on the parser.
     * @param feature the name of the feature to set
     * @param value the value of the feature
     * @throws BuildException if the feature was not supported
     */
    protected void setFeature(String feature, boolean value)
        throws BuildException {
        log("Setting feature " + feature + "=" + value, Project.MSG_DEBUG);
        try {
            xmlReader.setFeature(feature, value);
        } catch (SAXNotRecognizedException e) {
            throw new BuildException(
                "Parser "
                    + xmlReader.getClass().getName()
                    + " doesn't recognize feature "
                    + feature,
                e,
                getLocation());
        } catch (SAXNotSupportedException e) {
            throw new BuildException(
                "Parser "
                    + xmlReader.getClass().getName()
                    + " doesn't support feature "
                    + feature,
                e,
                getLocation());
        }
    }

    /**
     * Sets a property.
     *
     * @param name a property name
     * @param value a property value.
     * @throws BuildException if an error occurs.
     * @throws BuildException if the property was not supported
     */
    protected void setProperty(String name, String value) throws BuildException {
        // Validates property
        if (name == null || value == null) {
            throw new BuildException("Property name and value must be specified.");
        }

        try {
            xmlReader.setProperty(name, value);
        } catch (SAXNotRecognizedException e) {
            throw new BuildException(
                "Parser "
                    + xmlReader.getClass().getName()
                    + " doesn't recognize property "
                    + name,
                e,
                getLocation());
        } catch (SAXNotSupportedException e) {
            throw new BuildException(
                "Parser "
                    + xmlReader.getClass().getName()
                    + " doesn't support property "
                    + name,
                e,
                getLocation());
        }
    }

    /**
     * parse the file
     * @param afile the file to validate.
     * @return true if the file validates.
     */
    protected boolean doValidate(File afile) {
        //for every file, we have a new instance of the validator
        initValidator();
        boolean result = true;
        try {
            log("Validating " + afile.getName() + "... ", Project.MSG_VERBOSE);
            errorHandler.init(afile);
            InputSource is = new InputSource(Files.newInputStream(afile.toPath()));
            String uri = FILE_UTILS.toURI(afile.getAbsolutePath());
            is.setSystemId(uri);
            xmlReader.parse(is);
        } catch (SAXException ex) {
            log("Caught when validating: " + ex.toString(), Project.MSG_DEBUG);
            if (failOnError) {
                throw new BuildException(
                    "Could not validate document " + afile);
            }
            log("Could not validate document " + afile + ": " + ex.toString());
            result = false;
        } catch (IOException ex) {
            throw new BuildException(
                "Could not validate document " + afile,
                ex);
        }
        if (errorHandler.getFailure()) {
            if (failOnError) {
                throw new BuildException(
                    afile + " is not a valid XML document.");
            }
            result = false;
            log(afile + " is not a valid XML document", Project.MSG_ERR);
        }
        return result;
    }

    /**
     * ValidatorErrorHandler role :
     * <ul>
     * <li> log SAX parse exceptions,
     * <li> remember if an error occurred
     * </ul>
     */
    protected class ValidatorErrorHandler implements ErrorHandler {

        // CheckStyle:VisibilityModifier OFF - bc
        protected File currentFile = null;
        protected String lastErrorMessage = null;
        protected boolean failed = false;
        // CheckStyle:VisibilityModifier ON
        /**
         * initialises the class
         * @param file file used
         */
        public void init(File file) {
            currentFile = file;
            failed = false;
        }
        /**
         * did an error happen during last parsing ?
         * @return did an error happen during last parsing ?
         */
        public boolean getFailure() {
            return failed;
        }

        /**
         * record a fatal error
         * @param exception the fatal error
         */
        public void fatalError(SAXParseException exception) {
            failed = true;
            doLog(exception, Project.MSG_ERR);
        }
        /**
         * receive notification of a recoverable error
         * @param exception the error
         */
        public void error(SAXParseException exception) {
            failed = true;
            doLog(exception, Project.MSG_ERR);
        }
        /**
         * receive notification of a warning
         * @param exception the warning
         */
        public void warning(SAXParseException exception) {
            // depending on implementation, XMLReader can yield hips of warning,
            // only output then if user explicitly asked for it
            if (warn) {
                doLog(exception, Project.MSG_WARN);
            }
        }

        private void doLog(SAXParseException e, int logLevel) {

            log(getMessage(e), logLevel);
        }

        private String getMessage(SAXParseException e) {
            String sysID = e.getSystemId();
            if (sysID != null) {
                String name = sysID;
                if (sysID.startsWith("file:")) {
                    try {
                        name = FILE_UTILS.fromURI(sysID);
                    } catch (Exception ex) {
                        // if this is not a valid file: just use the uri
                    }
                }
                int line = e.getLineNumber();
                int col = e.getColumnNumber();
                return name
                    + (line == -1
                       ? ""
                       : (":" + line + (col == -1 ? "" : (":" + col))))
                    + ": "
                    + e.getMessage();
            }
            return e.getMessage();
        }
    }

    /**
     * The class to create to set a feature of the parser.
     * @since ant1.6
     */
    public static class Attribute {
        /**
         * The name of the attribute to set.
         *
         * Valid attributes <a href="http://www.saxproject.org/apidoc/org/xml/sax/package-summary.html#package_description">include</a>
         */
        private String attributeName = null;

        /**
         * The value of the feature.
         **/
        private boolean attributeValue;

        /**
         * Set the feature name.
         * @param name the name to set
         */
        public void setName(String name) {
            attributeName = name;
        }
        /**
         * Set the feature value to true or false.
         * @param value feature value
         */
        public void setValue(boolean value) {
            attributeValue = value;
        }

        /**
         * Gets the attribute name.
         * @return the feature name
         */
        public String getName() {
            return attributeName;
        }

        /**
         * Gets the attribute value.
         * @return the feature value
         */
        public boolean getValue() {
            return attributeValue;
        }
    }

    /**
     * A Parser property.
     * See <a href="https://xml.apache.org/xerces-j/properties.html">
     * XML parser properties</a> for usable properties
     * @since ant 1.6.2
     */
    public static final class Property {

        private String name;
        private String value;
        /**
         * accessor to the name of the property
         * @return name of the property
         */
        public String getName() {
            return name;
        }
        /**
         * setter for the name of the property
         * @param name name of the property
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * getter for the value of the property
         * @return value of the property
         */
        public String getValue() {
            return value;
        }
        /**
         * sets the value of the property
         * @param value value of the property
         */
        public void setValue(String value) {
            this.value = value;
        }

    } // Property



}
