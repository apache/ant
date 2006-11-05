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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.TransformerConfigurationException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.XSLTLiaison3;
import org.apache.tools.ant.taskdefs.XSLTLogger;
import org.apache.tools.ant.taskdefs.XSLTLoggerAware;
import org.apache.tools.ant.taskdefs.XSLTProcess;
import org.apache.tools.ant.types.XMLCatalog;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.URLResource;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JAXPUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Concrete liaison for XSLT processor implementing TraX. (ie JAXP 1.1)
 *
 * @since Ant 1.3
 */
public class TraXLiaison implements XSLTLiaison3, ErrorListener, XSLTLoggerAware {

    /**
     * Helper for transforming filenames to URIs.
     *
     * @since Ant 1.7
     */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * The current <code>Project</code>
     */
    private Project project;

    /**
     * the name of the factory implementation class to use
     * or null for default JAXP lookup.
     */
    private String factoryName = null;

    /** The trax TransformerFactory */
    private TransformerFactory tfactory = null;

    /** stylesheet to use for transformation */
    private Resource stylesheet;

    private XSLTLogger logger;

    /** possible resolver for publicIds */
    private EntityResolver entityResolver;

    /** transformer to use for processing files */
    private Transformer transformer;

    /** The In memory version of the stylesheet */
    private Templates templates;

    /**
     * The modification time of the stylesheet from which the templates
     * are read
     */
    private long templatesModTime;

    /** possible resolver for URIs */
    private URIResolver uriResolver;

    /** transformer output properties */
    private Vector outputProperties = new Vector();

    /** stylesheet parameters */
    private Hashtable params = new Hashtable();

    /** factory attributes */
    private Vector attributes = new Vector();

    /**
     * Constructor for TraXLiaison.
     * @throws Exception never
     */
    public TraXLiaison() throws Exception {
    }

    /**
     * Set the stylesheet file.
     * @param stylesheet a <code>File</code> value
     * @throws Exception on error
     */
    public void setStylesheet(File stylesheet) throws Exception {
        FileResource fr = new FileResource();
        fr.setProject(project);
        fr.setFile(stylesheet);
        setStylesheet(fr);
    }

    /**
     * Set the stylesheet file.
     * @param stylesheet a {@link org.apache.tools.ant.types.Resource} value
     * @throws Exception on error
     */
    public void setStylesheet(Resource stylesheet) throws Exception {
        if (this.stylesheet != null) {
            // resetting the stylesheet - reset transformer
            transformer = null;

            // do we need to reset templates as well
            if (!this.stylesheet.equals(stylesheet)
                || (stylesheet.getLastModified() != templatesModTime)) {
                templates = null;
            }
        }
        this.stylesheet = stylesheet;
    }

    /**
     * Transform an input file.
     * @param infile the file to transform
     * @param outfile the result file
     * @throws Exception on error
     */
    public void transform(File infile, File outfile) throws Exception {
        if (transformer == null) {
            createTransformer();
        }

        InputStream fis = null;
        OutputStream fos = null;
        try {
            fis = new BufferedInputStream(new FileInputStream(infile));
            fos = new BufferedOutputStream(new FileOutputStream(outfile));
            StreamResult res = new StreamResult(fos);
            // not sure what could be the need of this...
            res.setSystemId(JAXPUtils.getSystemId(outfile));
            Source src = getSource(fis, infile);

            // set parameters on each transformation, maybe something has changed
            //(e.g. value of file name parameter)
            setTransformationParameters();

            transformer.transform(src, res);
        } finally {
            // make sure to close all handles, otherwise the garbage
            // collector will close them...whenever possible and
            // Windows may complain about not being able to delete files.
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ignored) {
                // ignore
            }
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    /**
     * Get the source instance from the stream and id of the file.
     * @param is the stream containing the stylesheet data.
     * @param infile the file that will be used for the systemid.
     * @return the configured source instance matching the stylesheet.
     * @throws ParserConfigurationException if a parser cannot be created which
     * satisfies the requested configuration.
     * @throws SAXException in case of problem detected by the SAX parser.
     */
    private Source getSource(InputStream is, File infile)
        throws ParserConfigurationException, SAXException {
        // todo: is this comment still relevant ??
        // FIXME: need to use a SAXSource as the source for the transform
        // so we can plug in our own entity resolver
        Source src = null;
        if (entityResolver != null) {
            if (getFactory().getFeature(SAXSource.FEATURE)) {
                SAXParserFactory spFactory = SAXParserFactory.newInstance();
                spFactory.setNamespaceAware(true);
                XMLReader reader = spFactory.newSAXParser().getXMLReader();
                reader.setEntityResolver(entityResolver);
                src = new SAXSource(reader, new InputSource(is));
            } else {
                throw new IllegalStateException("xcatalog specified, but "
                    + "parser doesn't support SAX");
            }
        } else {
            // WARN: Don't use the StreamSource(File) ctor. It won't work with
            // xalan prior to 2.2 because of systemid bugs.
            src = new StreamSource(is);
        }
        src.setSystemId(JAXPUtils.getSystemId(infile));
        return src;
    }

    private Source getSource(InputStream is, Resource resource)
        throws ParserConfigurationException, SAXException {
        // todo: is this comment still relevant ??
        // FIXME: need to use a SAXSource as the source for the transform
        // so we can plug in our own entity resolver
        Source src = null;
        if (entityResolver != null) {
            if (getFactory().getFeature(SAXSource.FEATURE)) {
                SAXParserFactory spFactory = SAXParserFactory.newInstance();
                spFactory.setNamespaceAware(true);
                XMLReader reader = spFactory.newSAXParser().getXMLReader();
                reader.setEntityResolver(entityResolver);
                src = new SAXSource(reader, new InputSource(is));
            } else {
                throw new IllegalStateException("xcatalog specified, but "
                    + "parser doesn't support SAX");
            }
        } else {
            // WARN: Don't use the StreamSource(File) ctor. It won't work with
            // xalan prior to 2.2 because of systemid bugs.
            src = new StreamSource(is);
        }
        // The line below is a hack: the system id must an URI, but it is not
        // cleat to get the URI of an resource, so just set the name of the
        // resource as a system id
        src.setSystemId(resourceToURI(resource));
        return src;
    }

    private String resourceToURI(Resource resource) {
        if (resource instanceof FileResource) {
            File f = ((FileResource) resource).getFile();
            return FILE_UTILS.toURI(f.getAbsolutePath());
        }
        if (resource instanceof URLResource) {
            URL u = ((URLResource) resource).getURL();
            return String.valueOf(u);
        } else {
            return resource.getName();
        }
    }

    /**
     * Read in templates from the stylesheet
     */
    private void readTemplates()
        throws IOException, TransformerConfigurationException,
               ParserConfigurationException, SAXException {

        // Use a stream so that you can close it yourself quickly
        // and avoid keeping the handle until the object is garbaged.
        // (always keep control), otherwise you won't be able to delete
        // the file quickly on windows.
        InputStream xslStream = null;
        try {
            xslStream
                = new BufferedInputStream(stylesheet.getInputStream());
            templatesModTime = stylesheet.getLastModified();
            Source src = getSource(xslStream, stylesheet);
            templates = getFactory().newTemplates(src);
        } finally {
            if (xslStream != null) {
                xslStream.close();
            }
        }
    }

    /**
     * Create a new transformer based on the liaison settings
     * @throws Exception thrown if there is an error during creation.
     * @see #setStylesheet(java.io.File)
     * @see #addParam(java.lang.String, java.lang.String)
     * @see #setOutputProperty(java.lang.String, java.lang.String)
     */
    private void createTransformer() throws Exception {
        if (templates == null) {
            readTemplates();
        }

        transformer = templates.newTransformer();

        // configure the transformer...
        transformer.setErrorListener(this);
        if (uriResolver != null) {
            transformer.setURIResolver(uriResolver);
        }
        for (int i = 0; i < outputProperties.size(); i++) {
            final String[] pair = (String[]) outputProperties.elementAt(i);
            transformer.setOutputProperty(pair[0], pair[1]);
        }
    }

    /**
     * Sets the paramters for the transformer.
     */
    private void setTransformationParameters() {
        for (final Enumeration enumeration = params.keys();
             enumeration.hasMoreElements();) {
            final String name = (String) enumeration.nextElement();
            final String value = (String) params.get(name);
            transformer.setParameter(name, value);
        }
    }

    /**
     * return the Transformer factory associated to this liaison.
     * @return the Transformer factory associated to this liaison.
     * @throws BuildException thrown if there is a problem creating
     * the factory.
     * @see #setFactory(String)
     * @since Ant 1.5.2
     */
    private TransformerFactory getFactory() throws BuildException {
        if (tfactory != null) {
            return tfactory;
        }
        // not initialized yet, so create the factory
        if (factoryName == null) {
            tfactory = TransformerFactory.newInstance();
        } else {
            try {
                Class clazz = Class.forName(factoryName);
                tfactory = (TransformerFactory) clazz.newInstance();
            } catch (Exception e) {
                throw new BuildException(e);
            }
        }
        tfactory.setErrorListener(this);

        // specific attributes for the transformer
        for (int i = 0; i < attributes.size(); i++) {
            final Object[] pair = (Object[]) attributes.elementAt(i);
            tfactory.setAttribute((String) pair[0], pair[1]);
        }

        if (uriResolver != null) {
            tfactory.setURIResolver(uriResolver);
        }
        return tfactory;
    }


    /**
     * Set the factory name to use instead of JAXP default lookup.
     * @param name the fully qualified class name of the factory to use
     * or null for the default JAXP look up mechanism.
     * @since Ant 1.6
     */
    public void setFactory(String name) {
        factoryName = name;
    }

    /**
     * Set a custom attribute for the JAXP factory implementation.
     * @param name the attribute name.
     * @param value the value of the attribute, usually a boolean
     * string or object.
     * @since Ant 1.6
     */
    public void setAttribute(String name, Object value) {
        final Object[] pair = new Object[]{name, value};
        attributes.addElement(pair);
    }

    /**
     * Set the output property for the current transformer.
     * Note that the stylesheet must be set prior to calling
     * this method.
     * @param name the output property name.
     * @param value the output property value.
     * @since Ant 1.5
     * @since Ant 1.5
     */
    public void setOutputProperty(String name, String value) {
        final String[] pair = new String[]{name, value};
        outputProperties.addElement(pair);
    }

    /**
     * Set the class to resolve entities during the transformation.
     * @param aResolver the resolver class.
     */
    public void setEntityResolver(EntityResolver aResolver) {
        entityResolver = aResolver;
    }

    /**
     * Set the class to resolve URIs during the transformation
     * @param aResolver a <code>EntityResolver</code> value
     */
    public void setURIResolver(URIResolver aResolver) {
        uriResolver = aResolver;
    }

    /**
     * Add a parameter.
     * @param name the name of the parameter
     * @param value the value of the parameter
     */
    public void addParam(String name, String value) {
        params.put(name, value);
    }

    /**
     * Set a logger.
     * @param l a logger.
     */
    public void setLogger(XSLTLogger l) {
        logger = l;
    }

    /**
     * Log an error.
     * @param e the exception to log.
     */
    public void error(TransformerException e) {
        logError(e, "Error");
    }

    /**
     * Log a fatal error.
     * @param e the exception to log.
     */
    public void fatalError(TransformerException e) {
        logError(e, "Fatal Error");
        throw new BuildException("Fatal error during transformation", e);
    }

    /**
     * Log a warning.
     * @param e the exception to log.
     */
    public void warning(TransformerException e) {
        logError(e, "Warning");
    }

    private void logError(TransformerException e, String type) {
        if (logger == null) {
            return;
        }

        StringBuffer msg = new StringBuffer();
        SourceLocator locator = e.getLocator();
        if (locator != null) {
            String systemid = locator.getSystemId();
            if (systemid != null) {
                String url = systemid;
                if (url.startsWith("file:")) {
                    url = FileUtils.getFileUtils().fromURI(url);
                }
                msg.append(url);
            } else {
                msg.append("Unknown file");
            }
            int line = locator.getLineNumber();
            if (line != -1) {
                msg.append(":");
                msg.append(line);
                int column = locator.getColumnNumber();
                if (column != -1) {
                    msg.append(":");
                    msg.append(column);
                }
            }
        }
        msg.append(": ");
        msg.append(type);
        msg.append("! ");
        msg.append(e.getMessage());
        if (e.getCause() != null) {
            msg.append(" Cause: ");
            msg.append(e.getCause());
        }

        logger.log(msg.toString());
    }

    // kept for backwards compatibility
    /**
     * @param file the filename to use for the systemid
     * @return the systemid
     * @deprecated since 1.5.x.
     *             Use org.apache.tools.ant.util.JAXPUtils#getSystemId instead.
     */
    protected String getSystemId(File file) {
        return JAXPUtils.getSystemId(file);
    }


    /**
     * Specific configuration for the TRaX liaison.
     * @param xsltTask the XSLTProcess task instance from which this liasion
     *        is to be configured.
     */
    public void configure(XSLTProcess xsltTask) {
        project = xsltTask.getProject();
        XSLTProcess.Factory factory = xsltTask.getFactory();
        if (factory != null) {
            setFactory(factory.getName());

            // configure factory attributes
            for (Enumeration attrs = factory.getAttributes();
                    attrs.hasMoreElements();) {
                XSLTProcess.Factory.Attribute attr =
                        (XSLTProcess.Factory.Attribute) attrs.nextElement();
                setAttribute(attr.getName(), attr.getValue());
            }
        }

        XMLCatalog xmlCatalog = xsltTask.getXMLCatalog();
        // use XMLCatalog as the entity resolver and URI resolver
        if (xmlCatalog != null) {
            setEntityResolver(xmlCatalog);
            setURIResolver(xmlCatalog);
        }


        // configure output properties
        for (Enumeration props = xsltTask.getOutputProperties();
                props.hasMoreElements();) {
            XSLTProcess.OutputProperty prop
                = (XSLTProcess.OutputProperty) props.nextElement();
            setOutputProperty(prop.getName(), prop.getValue());
        }
    }
}
