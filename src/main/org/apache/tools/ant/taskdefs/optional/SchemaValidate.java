/*
 * Copyright  2004 The Apache Software Foundation
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DTDLocation;
import org.apache.tools.ant.util.XmlConstants;
import org.apache.tools.ant.util.JAXPUtils;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;
import java.net.MalformedURLException;


/**
 * Validate XML Schema documents.
 * This task validates XML schema documents. It requires an XML parser
 * that handles the relevant SAx, Xerces or JAXP options.
 *
 * To resolve remote referencies, Ant may need its proxy set up, using the
 * setproxy task.
 *
 * Hands off most of the work to its parent, {@link XMLValidateTask}
 * @since Ant1.7
 */

public class SchemaValidate extends XMLValidateTask {

    private List schemaLocations= new ArrayList();

    /** full checking of a schema */
    private boolean fullChecking=true;

    /**
     * default URL for nonamespace schemas
     */
    private SchemaLocation anonymousSchema;

    public static final String ERROR_SAX_1 = "SAX1 parsers are not supported";
    public static final String ERROR_NO_XSD_SUPPORT =
            "Parser does not support Xerces or JAXP schema features";
    public static final String ERROR_TOO_MANY_DEFAULT_SCHEMAS =
            "Only one of defaultSchemaFile and defaultSchemaURL allowed";
    public static final String ERROR_PARSER_CREATION_FAILURE = "Could not create parser";

    /**
     * Called by the project to let the task initialize properly. The default
     * implementation is a no-op.
     *
     * @throws BuildException if something goes wrong with the build
     */
    public void init() throws BuildException {
        super.init();
        //validating
        setLenient(false);
    }

    public boolean enableXercesSchemaValidation() {
        try {
            setFeature(XmlConstants.FEATURE_XSD,true);
            //set the schema source for the doc
            setNoNamespaceSchemaProperty(
                    XmlConstants.PROPERTY_NO_NAMESPACE_SCHEMA_LOCATION);
        } catch (BuildException e) {
            log(e.toString(),Project.MSG_VERBOSE);
            return false;
        }
        return true;
    }

    private void setNoNamespaceSchemaProperty(String property) {
        String anonSchema = getNoNamespaceSchemaURL();
        if (anonSchema != null) {
            setProperty(property,
                    anonSchema);
        }
    }

    /**
     * JAXP12 schema attributes
     * @see <A href="http://java.sun.com/xml/jaxp/change-requests-11.html">
     * JAXP 1.2 Approved CHANGES</A>
     * @return
     */
    public boolean enableJAXP12SchemaValidation() {
        try {
            //enable XSD
            setProperty(XmlConstants.FEATURE_JAXP12_SCHEMA_LANGUAGE,
                    XmlConstants.URI_XSD);
            //set the schema source for the doc
            setNoNamespaceSchemaProperty(
                    XmlConstants.FEATURE_JAXP12_SCHEMA_SOURCE);
        } catch (BuildException e) {
            log(e.toString(), Project.MSG_VERBOSE);
            return false;
        }
        return true;
    }

    public void addSchema(SchemaLocation location) {
        schemaLocations.add(location);
    }

    /**
     * enable full schema checking. Slower but better.
     * @param fullChecking
     */
    public void setFullChecking(boolean fullChecking) {
        this.fullChecking = fullChecking;
    }


    /**
     * create a schema location to hold the anonymous
     * schema
     */
    protected void createAnonymousSchema() {
        if(anonymousSchema==null) {
            anonymousSchema=new SchemaLocation();
        }
        anonymousSchema.setNamespace("(no namespace)");
    }
    /**
     * identify the URL of the default schema
     * @param defaultSchemaURL
     */
    public void setNoNamespaceURL(String defaultSchemaURL) {
        createAnonymousSchema();
        this.anonymousSchema.setUrl(defaultSchemaURL);
    }

    /**
     * identify a file containing the default schema
     * @param defaultSchemaFile
     */
    public void setNoNamespaceFile(File defaultSchemaFile) {
        createAnonymousSchema();
        this.anonymousSchema.setFile(defaultSchemaFile);
    }

    /**
     * init the parser : load the parser class, and set features if necessary It
     * is only after this that the reader is valid
     *
     * @throws BuildException if something went wrong
     */
    protected void initValidator() {
        super.initValidator();
        XMLReader xmlReader = getXmlReader();
        //validate the parser type
        if(isSax1Parser()) {
            throw new BuildException(ERROR_SAX_1);
        }

        //enable schema
        //setFeature(XmlConstants.FEATURE_VALIDATION,false);
        setFeature(XmlConstants.FEATURE_NAMESPACES,true);
        if(!enableXercesSchemaValidation() &&
                !enableJAXP12SchemaValidation()) {
            //couldnt use the xerces or jaxp calls
            throw new BuildException(ERROR_NO_XSD_SUPPORT);
        }

        //enable schema checking
        setFeature(XmlConstants.FEATURE_XSD_FULL_VALIDATION,fullChecking);

        //turn off DTDs
        setFeatureIfSupported(XmlConstants.FEATURE_DISALLOW_DTD,true);
        //schema declarations go in next
        addSchemaLocations();
    }

    /**
     * Create a reader if the use of the class did not specify another one.
     * The reason to not use {@link JAXPUtils#getXMLReader()} was to
     * create our own factory with our own options.
     * @return
     */
    protected XMLReader createDefaultReader() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(true);
        factory.setNamespaceAware(true);
        XMLReader reader = null;
        try {
            SAXParser saxParser = factory.newSAXParser();
            reader = saxParser.getXMLReader();
        } catch (ParserConfigurationException e) {
            throw new BuildException(ERROR_PARSER_CREATION_FAILURE,e);
        } catch (SAXException e) {
            throw new BuildException(ERROR_PARSER_CREATION_FAILURE, e);
        }
        return reader;
    }

    /**
     * build a string list of all schema locations, then set the relevant
     * property.
     */
    protected void addSchemaLocations() {
        Iterator it = schemaLocations.iterator();
        StringBuffer buffer = new StringBuffer();
        int count = 0;
        while (it.hasNext()) {
            if (count > 0) {
                buffer.append(' ');
            }
            SchemaLocation schemaLocation = (SchemaLocation) it.next();
            String tuple = schemaLocation.getURIandLocation();
            buffer.append(tuple);
            count++;
        }
        if (count > 0) {
            setProperty(XmlConstants.PROPERTY_SCHEMA_LOCATION, buffer.toString());
        }

    }

    /**
     * get the URL of the no namespace schema
     * @return
     */
    protected String getNoNamespaceSchemaURL() {
        if(anonymousSchema==null) {
            return null;
        } else {
            return anonymousSchema.getSchemaLocationURL();
        }
    }

    /**
     * set a feature if it is supported, log at verbose level if
     * not
     * @param feature
     * @param value
     */
    protected void setFeatureIfSupported(String feature,boolean value) {
        try {
            getXmlReader().setFeature(feature, value);
        } catch (SAXNotRecognizedException e) {
            log("Not recognizied: "+feature,Project.MSG_VERBOSE);
        } catch (SAXNotSupportedException e) {
            log("Not supported: " + feature, Project.MSG_VERBOSE);
        }
    }

    /**
     * representation of a schema location. This is a URI plus either a file or
     * a url
     */
    public static class SchemaLocation {
        private String namespace;
        private File file;
        private String url;

        public static final String ERROR_NO_URI = "No URI";
        private static final String ERROR_TWO_LOCATIONS =
                "Both URL and File were given for schema ";
        public static final String ERROR_NO_FILE = "File not found: ";
        public static final String ERROR_NO_URL_REPRESENTATION = "Cannot make a URL of ";
        public static final String ERROR_NO_LOCATION = "No file or URL supplied for the schema ";

        public SchemaLocation() {
        }


        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getSchemaLocationURL() {
            boolean hasFile = file != null;
            boolean hasURL = isSet(url);
            //error if both are empty, or both are set
            if(!hasFile && !hasURL) {
                throw new BuildException(
                        ERROR_NO_LOCATION+namespace);
            }
            if (hasFile && hasURL) {
                throw new BuildException(ERROR_TWO_LOCATIONS + namespace);
            }
            String schema = url;
            if (hasFile) {
                if (!file.exists()) {
                    throw new BuildException(ERROR_NO_FILE + file);
                }
                try {
                    schema = file.toURL().toString();
                } catch (MalformedURLException e) {
                    //this is almost implausible, but required handling
                    throw new BuildException(ERROR_NO_URL_REPRESENTATION + file,e);
                }
            }
            return schema;
        }

        /**
         * validate the fields then create a "uri location" string
         *
         * @return string of uri and location
         * @throws BuildException
         */
        public String getURIandLocation() throws BuildException {
            if (!isSet(getNamespace())) {
                throw new BuildException(ERROR_NO_URI);
            }
            StringBuffer buffer = new StringBuffer();
            buffer.append(namespace);
            buffer.append(' ');
            buffer.append(getSchemaLocationURL());
            return new String(buffer);
        }

        private boolean isSet(String property) {
            return property != null && property.length() != 0;
        }
    } //SchemaLocation
}
