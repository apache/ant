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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.XSLTLiaison4;
import org.apache.tools.ant.taskdefs.XSLTLogger;
import org.apache.tools.ant.taskdefs.XSLTLoggerAware;
import org.apache.tools.ant.taskdefs.XSLTProcess;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.XMLCatalog;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.URLProvider;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JAXPUtils;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.util.StreamUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Concrete liaison for XSLT processor implementing TraX. (ie JAXP 1.1)
 *
 * @since Ant 1.3
 */
public class TraXLiaison implements XSLTLiaison4, ErrorListener, XSLTLoggerAware {

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
    private final Vector<String[]> outputProperties = new Vector<>();

    /** stylesheet parameters */
    private final Hashtable<String, Object> params = new Hashtable<>();

    /** factory attributes */
    private final List<Object[]> attributes = new ArrayList<>();

    /** factory features */
    private final Map<String, Boolean> features = new HashMap<>();

    /** whether to suppress warnings */
    private boolean suppressWarnings = false;

    /** optional trace configuration. */
    private XSLTProcess.TraceConfiguration traceConfiguration = null;

    /**
     * Constructor for TraXLiaison.
     * @throws Exception never
     */
    public TraXLiaison() throws Exception { //NOSONAR
    }

    /**
     * Set the stylesheet file.
     * @param stylesheet a <code>File</code> value
     * @throws Exception on error
     */
    public void setStylesheet(final File stylesheet) throws Exception {
        final FileResource fr = new FileResource();
        fr.setProject(project);
        fr.setFile(stylesheet);
        setStylesheet(fr);
    }

    /**
     * Set the stylesheet file.
     * @param stylesheet a {@link org.apache.tools.ant.types.Resource} value
     * @throws Exception on error
     */
    public void setStylesheet(final Resource stylesheet) throws Exception {
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
    public void transform(final File infile, final File outfile) throws Exception {
        if (transformer == null) {
            createTransformer();
        }

        // autoclose all handles, otherwise the garbage collector will close them...
        // and Windows may complain about not being able to delete files.
        try (InputStream fis = new BufferedInputStream(Files.newInputStream(infile.toPath()));
             OutputStream fos = new BufferedOutputStream(Files.newOutputStream(outfile.toPath()))) {
            final StreamResult res = new StreamResult(fos);
            // not sure what could be the need of this...
            res.setSystemId(JAXPUtils.getSystemId(outfile));
            // set parameters on each transformation, maybe something has changed
            //(e.g. value of file name parameter)
            setTransformationParameters();

            transformer.transform(getSource(fis, infile), res);
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
    private Source getSource(final InputStream is, final File infile)
        throws ParserConfigurationException, SAXException {
        // todo: is this comment still relevant ??
        // FIXME: need to use a SAXSource as the source for the transform
        // so we can plug in our own entity resolver
        Source src = null;
        if (entityResolver != null) {
            if (getFactory().getFeature(SAXSource.FEATURE)) {
                final SAXParserFactory spFactory = SAXParserFactory.newInstance();
                spFactory.setNamespaceAware(true);
                final XMLReader reader = spFactory.newSAXParser().getXMLReader();
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

    private Source getSource(final InputStream is, final Resource resource)
        throws ParserConfigurationException, SAXException {
        // todo: is this comment still relevant ??
        // FIXME: need to use a SAXSource as the source for the transform
        // so we can plug in our own entity resolver
        Source src = null;
        if (entityResolver != null) {
            if (getFactory().getFeature(SAXSource.FEATURE)) {
                final SAXParserFactory spFactory = SAXParserFactory.newInstance();
                spFactory.setNamespaceAware(true);
                final XMLReader reader = spFactory.newSAXParser().getXMLReader();
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

    private String resourceToURI(final Resource resource) {
        final FileProvider fp = resource.as(FileProvider.class);
        if (fp != null) {
            return FILE_UTILS.toURI(fp.getFile().getAbsolutePath());
        }
        final URLProvider up = resource.as(URLProvider.class);
        if (up != null) {
            final URL u = up.getURL();
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
        try (InputStream xslStream =
             new BufferedInputStream(stylesheet.getInputStream())) {
            templatesModTime = stylesheet.getLastModified();
            final Source src = getSource(xslStream, stylesheet);
            templates = getFactory().newTemplates(src);
        }
    }

    /**
     * Create a new transformer based on the liaison settings
     * @see #setStylesheet(java.io.File)
     * @see #addParam(java.lang.String, java.lang.String)
     * @see #setOutputProperty(java.lang.String, java.lang.String)
     */
    private void createTransformer()
        throws IOException, ParserConfigurationException, SAXException, TransformerException {
        if (templates == null) {
            readTemplates();
        }

        transformer = templates.newTransformer();

        // configure the transformer...
        transformer.setErrorListener(this);
        if (uriResolver != null) {
            transformer.setURIResolver(uriResolver);
        }
        for (final String[] pair : outputProperties) {
            transformer.setOutputProperty(pair[0], pair[1]);
        }

        if (traceConfiguration != null) {
            if ("org.apache.xalan.transformer.TransformerImpl" //NOSONAR
                .equals(transformer.getClass().getName())) {
                try {
                    final Class<?> traceSupport =
                        Class.forName("org.apache.tools.ant.taskdefs.optional."
                                      + "Xalan2TraceSupport", true,
                                      Thread.currentThread()
                                      .getContextClassLoader());
                    final XSLTTraceSupport ts =
                        (XSLTTraceSupport) traceSupport.getDeclaredConstructor().newInstance();
                    ts.configureTrace(transformer, traceConfiguration);
                } catch (final Exception e) {
                    final String msg = "Failed to enable tracing because of " + e;
                    if (project != null) {
                        project.log(msg, Project.MSG_WARN);
                    } else {
                        System.err.println(msg);
                    }
                }
            } else {
                final String msg = "Not enabling trace support for transformer"
                    + " implementation" + transformer.getClass().getName();
                if (project != null) {
                    project.log(msg, Project.MSG_WARN);
                } else {
                    System.err.println(msg);
                }
            }
        }
    }

    /**
     * Sets the parameters for the transformer.
     */
    private void setTransformationParameters() {
        params.forEach((key, value) -> transformer.setParameter(key, value));
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
                Class<?> clazz = null;
                try {
                    clazz =
                        Class.forName(factoryName, true,
                                      Thread.currentThread()
                                      .getContextClassLoader());
                } catch (final ClassNotFoundException cnfe) {
                    final String msg = "Failed to load " + factoryName
                        + " via the configured classpath, will try"
                        + " Ant's classpath instead.";
                    if (logger != null) {
                        logger.log(msg);
                    } else if (project != null) {
                        project.log(msg, Project.MSG_WARN);
                    } else {
                        System.err.println(msg);
                    }
                }

                if (clazz == null) {
                    clazz = Class.forName(factoryName);
                }
                tfactory = (TransformerFactory) clazz.getDeclaredConstructor().newInstance();
            } catch (final Exception e) {
                throw new BuildException(e);
            }
        }

        applyReflectionHackForExtensionMethods();

        tfactory.setErrorListener(this);

        // specific attributes for the transformer
        for (final Object[] pair : attributes) {
            tfactory.setAttribute((String) pair[0], pair[1]);
        }

        for (Map.Entry<String, Boolean> feature : features.entrySet()) {
            try {
                tfactory.setFeature(feature.getKey(), feature.getValue());
            } catch (TransformerConfigurationException ex) {
                throw new BuildException(ex);
            }
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
    public void setFactory(final String name) {
        factoryName = name;
    }

    /**
     * Set a custom attribute for the JAXP factory implementation.
     * @param name the attribute name.
     * @param value the value of the attribute, usually a boolean
     * string or object.
     * @since Ant 1.6
     */
    public void setAttribute(final String name, final Object value) {
        final Object[] pair = new Object[]{name, value};
        attributes.add(pair);
    }

    /**
     * Set a custom feature for the JAXP factory implementation.
     * @param name the feature name.
     * @param value the value of the feature
     * @since Ant 1.9.8
     */
    public void setFeature(final String name, final boolean value) {
        features.put(name, value);
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
    public void setOutputProperty(final String name, final String value) {
        final String[] pair = new String[]{name, value};
        outputProperties.addElement(pair);
    }

    /**
     * Set the class to resolve entities during the transformation.
     * @param aResolver the resolver class.
     */
    public void setEntityResolver(final EntityResolver aResolver) {
        entityResolver = aResolver;
    }

    /**
     * Set the class to resolve URIs during the transformation
     * @param aResolver a <code>EntityResolver</code> value
     */
    public void setURIResolver(final URIResolver aResolver) {
        uriResolver = aResolver;
    }

    /**
     * Add a parameter.
     * @param name the name of the parameter
     * @param value the value of the parameter
     */
    public void addParam(final String name, final String value) {
        params.put(name, value);
    }

    /**
     * Add a parameter.
     * @param name the name of the parameter
     * @param value the value of the parameter
     * @since Ant 1.9.3
     */
    public void addParam(final String name, final Object value) {
        params.put(name, value);
    }

    /**
     * Set a logger.
     * @param l a logger.
     */
    public void setLogger(final XSLTLogger l) {
        logger = l;
    }

    /**
     * Log an error.
     * @param e the exception to log.
     */
    public void error(final TransformerException e) {
        logError(e, "Error");
    }

    /**
     * Log a fatal error.
     * @param e the exception to log.
     */
    public void fatalError(final TransformerException e) {
        logError(e, "Fatal Error");
        throw new BuildException("Fatal error during transformation using " + stylesheet + ": " + e.getMessageAndLocation(), e);
    }

    /**
     * Log a warning.
     * @param e the exception to log.
     */
    public void warning(final TransformerException e) {
        if (!suppressWarnings) {
            logError(e, "Warning");
        }
    }

    private void logError(final TransformerException e, final String type) {
        if (logger == null) {
            return;
        }

        final StringBuilder msg = new StringBuilder();
        final SourceLocator locator = e.getLocator();
        if (locator != null) {
            final String systemid = locator.getSystemId();
            if (systemid != null) {
                String url = systemid;
                if (url.startsWith("file:")) {
                    url = FileUtils.getFileUtils().fromURI(url);
                }
                msg.append(url);
            } else {
                msg.append("Unknown file");
            }
            final int line = locator.getLineNumber();
            if (line != -1) {
                msg.append(":");
                msg.append(line);
                final int column = locator.getColumnNumber();
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
    @Deprecated
    protected String getSystemId(final File file) {
        return JAXPUtils.getSystemId(file);
    }


    /**
     * Specific configuration for the TRaX liaison.
     * @param xsltTask the XSLTProcess task instance from which this liaison
     *        is to be configured.
     */
    public void configure(final XSLTProcess xsltTask) {
        project = xsltTask.getProject();
        final XSLTProcess.Factory factory = xsltTask.getFactory();
        if (factory != null) {
            setFactory(factory.getName());
            // configure factory attributes
            StreamUtils.enumerationAsStream(factory.getAttributes())
                    .forEach(attr -> setAttribute(attr.getName(), attr.getValue()));
            factory.getFeatures()
                    .forEach(feature -> setFeature(feature.getName(), feature.getValue()));
        }

        final XMLCatalog xmlCatalog = xsltTask.getXMLCatalog();
        // use XMLCatalog as the entity resolver and URI resolver
        if (xmlCatalog != null) {
            setEntityResolver(xmlCatalog);
            setURIResolver(xmlCatalog);
        }

        // configure output properties
        StreamUtils.enumerationAsStream(xsltTask.getOutputProperties())
                .forEach(prop -> setOutputProperty(prop.getName(), prop.getValue()));

        suppressWarnings = xsltTask.getSuppressWarnings();

        traceConfiguration = xsltTask.getTraceConfiguration();
    }

    private void applyReflectionHackForExtensionMethods() {
        // Jigsaw doesn't allow reflection to work, so we can stop trying
        if (!JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9)) {
            try { // #51668, #52382
                final Field _isNotSecureProcessing = tfactory.getClass().getDeclaredField("_isNotSecureProcessing");
                _isNotSecureProcessing.setAccessible(true);
                _isNotSecureProcessing.set(tfactory, Boolean.TRUE);
            } catch (final Exception x) {
                if (project != null) {
                    project.log(x.toString(), Project.MSG_DEBUG);
                }
            }
        }
    }

}
