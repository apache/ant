/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.DynamicConfigurator;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.TraXLiaison;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.XMLCatalog;
import org.apache.tools.ant.util.FileUtils;

/**
 * Processes a set of XML documents via XSLT. This is
 * useful for building views of XML based documentation.
 *
 * @version $Revision$
 *
 * @author <a href="mailto:kvisco@exoffice.com">Keith Visco</a>
 * @author <a href="mailto:rubys@us.ibm.com">Sam Ruby</a>
 * @author <a href="mailto:russgold@acm.org">Russell Gold</a>
 * @author Stefan Bodewig
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 *
 * @since Ant 1.1
 *
 * @ant.task name="xslt" category="xml"
 */

public class XSLTProcess extends MatchingTask implements XSLTLogger {
    /** destination directory */
    private File destDir = null;

    /** where to find the source XML file, default is the project's basedir */
    private File baseDir = null;

    /** XSL stylesheet */
    private String xslFile = null;

    /** extension of the files produced by XSL processing */
    private String targetExtension = ".html";

    /** additional parameters to be passed to the stylesheets */
    private Vector params = new Vector();

    /** Input XML document to be used */
    private File inFile = null;

    /** Output file */
    private File outFile = null;

    /** The name of the XSL processor to use */
    private String processor;

    /** Classpath to use when trying to load the XSL processor */
    private Path classpath = null;

    /** The Liason implementation to use to communicate with the XSL
     *  processor */
    private XSLTLiaison liaison;

    /** Flag which indicates if the stylesheet has been loaded into
     *  the processor */
    private boolean stylesheetLoaded = false;

    /** force output of target files even if they already exist */
    private boolean force = false;

    /** Utilities used for file operations */
    private FileUtils fileUtils;

    /** XSL output properties to be used */
    private Vector outputProperties = new Vector();

    /** for resolving entities such as dtds */
    private XMLCatalog xmlCatalog = new XMLCatalog();

    /** Name of the TRAX Liaison class */
    private static final String TRAX_LIAISON_CLASS =
                        "org.apache.tools.ant.taskdefs.optional.TraXLiaison";

    /** Name of the now-deprecated XSLP Liaison class */
    private static final String XSLP_LIAISON_CLASS =
                        "org.apache.tools.ant.taskdefs.optional.XslpLiaison";

    /** Name of the now-deprecated Xalan liaison class */
    private static final String XALAN_LIAISON_CLASS =
                        "org.apache.tools.ant.taskdefs.optional.XalanLiaison";

    /**
     * Whether to style all files in the included directories as well.
     *
     * @since Ant 1.5
     */
    private boolean performDirectoryScan = true;

    /**
     * factory element for TraX processors only
     * @since Ant 1.6
     */
    private Factory factory = null;

    /**
     * whether to reuse Transformer if transforming multiple files.
     * @since 1.5.2
     */
    private boolean reuseLoadedStylesheet = true;

    /**
     * Creates a new XSLTProcess Task.
     */
    public XSLTProcess() {
        fileUtils = FileUtils.newFileUtils();
    } //-- XSLTProcess

    /**
     * Whether to style all files in the included directories as well;
     * optional, default is true.
     *
     * @param b true if files in included directories are processed.
     * @since Ant 1.5
     */
    public void setScanIncludedDirectories(boolean b) {
        performDirectoryScan = b;
    }

    /**
     * Controls whether the stylesheet is reloaded for every transform.
     *
     * <p>Setting this to true may get around a bug in certain
     * Xalan-J versions, default is false.</p>
     *
     * @since Ant 1.5.2
     */
    public void setReloadStylesheet(boolean b) {
        reuseLoadedStylesheet = !b;
    }

    /**
     * Executes the task.
     *
     * @exception BuildException if there is an execution problem.
     * @todo validate that if either in or our is defined, then both are
     */
    public void execute() throws BuildException {
        File savedBaseDir = baseDir;

        DirectoryScanner scanner;
        String[]         list;
        String[]         dirs;

        if (xslFile == null) {
            throw new BuildException("no stylesheet specified", getLocation());
        }

        if (inFile != null && !inFile.exists()) {
            throw new BuildException("input file " + inFile.toString() + " does not exist", getLocation());
        }
        try {
            if (baseDir == null) {
                baseDir = getProject().resolveFile(".");
            }

            liaison = getLiaison();

            // check if liaison wants to log errors using us as logger
            if (liaison instanceof XSLTLoggerAware) {
                ((XSLTLoggerAware) liaison).setLogger(this);
            }

            log("Using " + liaison.getClass().toString(), Project.MSG_VERBOSE);

            File stylesheet = getProject().resolveFile(xslFile);
            if (!stylesheet.exists()) {
                stylesheet = fileUtils.resolveFile(baseDir, xslFile);
                /*
                 * shouldn't throw out deprecation warnings before we know,
                 * the wrong version has been used.
                 */
                if (stylesheet.exists()) {
                    log("DEPRECATED - the style attribute should be relative "
                        + "to the project\'s");
                    log("             basedir, not the tasks\'s basedir.");
                }
            }

            // if we have an in file and out then process them
            if (inFile != null && outFile != null) {
                process(inFile, outFile, stylesheet);
                return;
            }

            /*
             * if we get here, in and out have not been specified, we are
             * in batch processing mode.
             */

            //-- make sure Source directory exists...
            if (destDir == null) {
                String msg = "destdir attributes must be set!";
                throw new BuildException(msg);
            }
            scanner = getDirectoryScanner(baseDir);
            log("Transforming into " + destDir, Project.MSG_INFO);

            // Process all the files marked for styling
            list = scanner.getIncludedFiles();
            for (int i = 0; i < list.length; ++i) {
                process(baseDir, list[i], destDir, stylesheet);
            }
            if (performDirectoryScan) {
                // Process all the directories marked for styling
                dirs = scanner.getIncludedDirectories();
                for (int j = 0; j < dirs.length; ++j) {
                    list = new File(baseDir, dirs[j]).list();
                    for (int i = 0; i < list.length; ++i) {
                        process(baseDir, list[i], destDir, stylesheet);
                    }
                }
            }
        } finally {
            liaison = null;
            stylesheetLoaded = false;
            baseDir = savedBaseDir;
        }
    }

    /**
     * Set whether to check dependencies, or always generate;
     * optional, default is false.
     *
     * @param force true if always generate.
     */
    public void setForce(boolean force) {
        this.force = force;
    }

    /**
     * Set the base directory;
     * optional, default is the project's basedir.
     *
     * @param dir the base directory
     **/
    public void setBasedir(File dir) {
        baseDir = dir;
    }

    /**
     * Set the destination directory into which the XSL result
     * files should be copied to;
     * required, unless <tt>in</tt> and <tt>out</tt> are
     * specified.
     * @param dir the name of the destination directory
     **/
    public void setDestdir(File dir) {
        destDir = dir;
    }

    /**
     * Set the desired file extension to be used for the target;
     * optional, default is html.
     * @param name the extension to use
     **/
    public void setExtension(String name) {
        targetExtension = name;
    }

    /**
     * Name of the stylesheet to use - given either relative
     * to the project's basedir or as an absolute path; required.
     *
     * @param xslFile the stylesheet to use
     */
    public void setStyle(String xslFile) {
        this.xslFile = xslFile;
    }

    /**
     * Set the optional classpath to the XSL processor
     *
     * @param classpath the classpath to use when loading the XSL processor
     */
    public void setClasspath(Path classpath) {
        createClasspath().append(classpath);
    }

    /**
     * Set the optional classpath to the XSL processor
     *
     * @return a path instance to be configured by the Ant core.
     */
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath.createPath();
    }

    /**
     * Set the reference to an optional classpath to the XSL processor
     *
     * @param r the id of the Ant path instance to act as the classpath
     *          for loading the XSL processor
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * Set the name of the XSL processor to use; optional, default trax.
     * Other values are "xalan" for Xalan1 and "xslp" for XSL:P, though the
     * later is strongly deprecated.
     *
     * @param processor the name of the XSL processor
     */
    public void setProcessor(String processor) {
        this.processor = processor;
    }

    /**
     * Add the catalog to our internal catalog
     *
     * @param xmlCatalog the XMLCatalog instance to use to look up DTDs
     */
    public void addConfiguredXMLCatalog(XMLCatalog xmlCatalog) {
        this.xmlCatalog.addConfiguredXMLCatalog(xmlCatalog);
    }

    /**
     * Load processor here instead of in setProcessor - this will be
     * called from within execute, so we have access to the latest
     * classpath.
     *
     * @param proc the name of the processor to load.
     * @exception Exception if the processor cannot be loaded.
     */
    private void resolveProcessor(String proc) throws Exception {
        if (proc.equals("trax")) {
            final Class clazz = loadClass(TRAX_LIAISON_CLASS);
            liaison = (XSLTLiaison) clazz.newInstance();
        } else if (proc.equals("xslp")) {
            log("DEPRECATED - xslp processor is deprecated. Use trax "
                + "instead.");
            final Class clazz = loadClass(XSLP_LIAISON_CLASS);
            liaison = (XSLTLiaison) clazz.newInstance();
        } else if (proc.equals("xalan")) {
            log("DEPRECATED - xalan processor is deprecated. Use trax "
                + "instead.");
            final Class clazz = loadClass(XALAN_LIAISON_CLASS);
            liaison = (XSLTLiaison) clazz.newInstance();
        } else {
            liaison = (XSLTLiaison) loadClass(proc).newInstance();
        }
    }

    /**
     * Load named class either via the system classloader or a given
     * custom classloader.
     *
     * @param classname the name of the class to load.
     * @return the requested class.
     * @exception Exception if the class could not be loaded.
     */
    private Class loadClass(String classname) throws Exception {
        if (classpath == null) {
            return Class.forName(classname);
        } else {
            AntClassLoader al = getProject().createClassLoader(classpath);
            Class c = Class.forName(classname, true, al);
            return c;
        }
    }

    /**
     * Specifies the output name for the styled result from the
     * <tt>in</tt> attribute; required if <tt>in</tt> is set
     *
     * @param outFile the output File instance.
     */
    public void setOut(File outFile) {
        this.outFile = outFile;
    }

    /**
     * specifies a single XML document to be styled. Should be used
     * with the <tt>out</tt> attribute; ; required if <tt>out</tt> is set
     *
     * @param inFile the input file
     */
    public void setIn(File inFile) {
        this.inFile = inFile;
    }

    /**
     * Processes the given input XML file and stores the result
     * in the given resultFile.
     *
     * @param baseDir the base directory for resolving files.
     * @param xmlFile the input file
     * @param destDir the destination directory
     * @param stylesheet the stylesheet to use.
     * @exception BuildException if the processing fails.
     */
    private void process(File baseDir, String xmlFile, File destDir,
                         File stylesheet)
        throws BuildException {

        String fileExt = targetExtension;
        File   outFile = null;
        File   inFile = null;

        try {
            long styleSheetLastModified = stylesheet.lastModified();
            inFile = new File(baseDir, xmlFile);

            if (inFile.isDirectory()) {
                log("Skipping " + inFile + " it is a directory.",
                    Project.MSG_VERBOSE);
                return;
            }

            int dotPos = xmlFile.lastIndexOf('.');
            if (dotPos > 0) {
                outFile = new File(destDir,
                    xmlFile.substring(0, xmlFile.lastIndexOf('.')) + fileExt);
            } else {
                outFile = new File(destDir, xmlFile + fileExt);
            }
            if (force
                || inFile.lastModified() > outFile.lastModified()
                || styleSheetLastModified > outFile.lastModified()) {
                ensureDirectoryFor(outFile);
                log("Processing " + inFile + " to " + outFile);

                configureLiaison(stylesheet);
                liaison.transform(inFile, outFile);
            }
        } catch (Exception ex) {
            // If failed to process document, must delete target document,
            // or it will not attempt to process it the second time
            log("Failed to process " + inFile, Project.MSG_INFO);
            if (outFile != null) {
                outFile.delete();
            }

            throw new BuildException(ex);
        }

    } //-- processXML

    /**
     * Process the input file to the output file with the given stylesheet.
     *
     * @param inFile the input file to process.
     * @param outFile the detination file.
     * @param stylesheet the stylesheet to use.
     * @exception BuildException if the processing fails.
     */
    private void process(File inFile, File outFile, File stylesheet)
         throws BuildException {
        try {
            long styleSheetLastModified = stylesheet.lastModified();
            log("In file " + inFile + " time: " + inFile.lastModified(),
                Project.MSG_DEBUG);
            log("Out file " + outFile + " time: " + outFile.lastModified(),
                Project.MSG_DEBUG);
            log("Style file " + xslFile + " time: " + styleSheetLastModified,
                Project.MSG_DEBUG);
            if (force || inFile.lastModified() > outFile.lastModified()
                || styleSheetLastModified > outFile.lastModified()) {
                ensureDirectoryFor(outFile);
                log("Processing " + inFile + " to " + outFile,
                    Project.MSG_INFO);
                configureLiaison(stylesheet);
                liaison.transform(inFile, outFile);
            }
        } catch (Exception ex) {
            log("Failed to process " + inFile, Project.MSG_INFO);
            if (outFile != null) {
                outFile.delete();
            }
            throw new BuildException(ex);
        }
    }

    /**
     * Ensure the directory exists for a given file
     *
     * @param targetFile the file for which the directories are required.
     * @exception BuildException if the directories cannot be created.
     */
    private void ensureDirectoryFor(File targetFile)
         throws BuildException {
        File directory = fileUtils.getParentFile(targetFile);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new BuildException("Unable to create directory: "
                                         + directory.getAbsolutePath());
            }
        }
    }

    /**
     * Get the Liason implementation to use in processing.
     *
     * @return an instance of the XSLTLiason interface.
     */
    protected XSLTLiaison getLiaison() {
        // if processor wasn't specified, see if TraX is available.  If not,
        // default it to xslp or xalan, depending on which is in the classpath
        if (liaison == null) {
            if (processor != null) {
                try {
                    resolveProcessor(processor);
                } catch (Exception e) {
                    throw new BuildException(e);
                }
            } else {
                try {
                    resolveProcessor("trax");
                } catch (Throwable e1) {
                    try {
                        resolveProcessor("xalan");
                    } catch (Throwable e2) {
                        try {
                            resolveProcessor("xslp");
                        } catch (Throwable e3) {
                            e3.printStackTrace();
                            e2.printStackTrace();
                            throw new BuildException(e1);
                        }
                    }
                }
            }
        }
        return liaison;
    }

    /**
     * Create an instance of an XSL parameter for configuration by Ant.
     *
     * @return an instance of the Param class to be configured.
     */
    public Param createParam() {
        Param p = new Param();
        params.addElement(p);
        return p;
    }

    /**
     * The Param inner class used to store XSL parameters
     */
    public static class Param {
        /** The parameter name */
        private String name = null;

        /** The parameter's value */
        private String expression = null;

        private String ifProperty;
        private String unlessProperty;
        private Project project;

        /**
         * Set the current project
         *
         * @param project the current project
         */
        public void setProject(Project project) {
            this.project = project;
        }

        /**
         * Set the parameter name.
         *
         * @param name the name of the parameter.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * The parameter value
         * NOTE : was intended to be an XSL expression.
         * @param expression the parameter's value.
         */
        public void setExpression(String expression) {
            this.expression = expression;
        }

        /**
         * Get the parameter name
         *
         * @return the parameter name
         * @exception BuildException if the name is not set.
         */
        public String getName() throws BuildException {
            if (name == null) {
                throw new BuildException("Name attribute is missing.");
            }
            return name;
        }

        /**
         * Get the parameter's value
         *
         * @return the parameter value
         * @exception BuildException if the value is not set.
         */
        public String getExpression() throws BuildException {
            if (expression == null) {
                throw new BuildException("Expression attribute is missing.");
            }
            return expression;
        }

        /**
         * Set whether this param should be used.  It will be
         * used if the property has been set, otherwise it won't.
         * @param ifProperty name of property
         */
        public void setIf(String ifProperty) {
            this.ifProperty = ifProperty;
        }

        /**
         * Set whether this param should NOT be used. It
         * will not be used if the property has been set, orthwise it
         * will be used.
         * @param unlessProperty name of property
         */
        public void setUnless(String unlessProperty) {
            this.unlessProperty = unlessProperty;
        }
        /**
         * Ensures that the param passes the conditions placed
         * on it with <code>if</code> and <code>unless</code> properties.
         */
        public boolean shouldUse() {
            if (ifProperty != null && project.getProperty(ifProperty) == null) {
                return false;
            } else if (unlessProperty != null
                    && project.getProperty(unlessProperty) != null) {
                return false;
            }
            
            return true;
        }
    } // Param


    /**
     * Create an instance of an output property to be configured.
     * @return the newly created output property.
     * @since Ant 1.5
     */
    public OutputProperty createOutputProperty() {
        OutputProperty p = new OutputProperty();
        outputProperties.addElement(p);
        return p;
    }


    /**
     * Specify how the result tree should be output as specified
     * in the <a href="http://www.w3.org/TR/xslt#output">
     * specification</a>.
     * @since Ant 1.5
     */
    public static class OutputProperty {
        /** output property name */
        private String name;

        /** output property value */
        private String value;

        /**
         * @return the output property name.
         */
        public String getName() {
            return name;
        }

        /**
         * set the name for this property
         * @param name A non-null String that specifies an
         * output property name, which may be namespace qualified.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return the output property value.
         */
        public String getValue() {
            return value;
        }

        /**
         * set the value for this property
         * @param value The non-null string value of the output property.
         */
        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * Initialize internal instance of XMLCatalog
     */
    public void init() throws BuildException {
        super.init();
        xmlCatalog.setProject(getProject());
    }

    /**
     * Loads the stylesheet and set xsl:param parameters.
     *
     * @param stylesheet the file form which to load the stylesheet.
     * @exception BuildException if the stylesheet cannot be loaded.
     */
    protected void configureLiaison(File stylesheet) throws BuildException {
        if (stylesheetLoaded && reuseLoadedStylesheet) {
            return;
        }
        stylesheetLoaded = true;

        try {
            log("Loading stylesheet " + stylesheet, Project.MSG_INFO);
            liaison.setStylesheet(stylesheet);
            for (Enumeration e = params.elements(); e.hasMoreElements();) {
                Param p = (Param) e.nextElement();
                if (p.shouldUse()) {
                    liaison.addParam(p.getName(), p.getExpression());
                }
            }
            if (liaison instanceof TraXLiaison) {
                configureTraXLiaison((TraXLiaison) liaison);
            }
        } catch (Exception ex) {
            log("Failed to transform using stylesheet " + stylesheet, Project.MSG_INFO);
            throw new BuildException(ex);
        }
    }

    /**
     * Specific configuration for the TRaX liaison. Support for
     * all others has been dropped so this liaison will soon look
     * like the exact copy of JAXP interface..
     * @param liaison the TRaXLiaison to configure.
     */
    protected void configureTraXLiaison(TraXLiaison liaison) {
        if (factory != null) {
            liaison.setFactory(factory.getName());

            // configure factory attributes
            for (Enumeration attrs = factory.getAttributes();
                    attrs.hasMoreElements();) {
                Factory.Attribute attr =
                        (Factory.Attribute) attrs.nextElement();
                liaison.setAttribute(attr.getName(), attr.getValue());
            }
        }

        // use XMLCatalog as the entity resolver and URI resolver
        if (xmlCatalog != null) {
            liaison.setEntityResolver(xmlCatalog);
            liaison.setURIResolver(xmlCatalog);
        }

        // configure output properties
        for (Enumeration props = outputProperties.elements();
                props.hasMoreElements();) {
            OutputProperty prop = (OutputProperty) props.nextElement();
            liaison.setOutputProperty(prop.getName(), prop.getValue());
        }
    }

    /**
     * Create the factory element to configure a trax liaison.
     * @return the newly created factory element.
     * @throws BuildException if the element is created more than one time.
     */
    public Factory createFactory() throws BuildException {
        if (factory != null) {
            throw new BuildException("'factory' element must be unique");
        }
        factory = new Factory();
        return factory;
    }

    /**
     * The factory element to configure a transformer factory
     * @since Ant 1.6
     */
    public static class Factory {

        /** the factory class name to use for TraXLiaison */
        private String name;

        /**
         * the list of factory attributes to use for TraXLiaison
         */
        private Vector attributes = new Vector();

        /**
         * @return the name of the factory.
         */
        public String getName() {
            return name;
        }

        /**
         * Set the name of the factory
         * @param name the name of the factory.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Create an instance of a factory attribute.
         * the newly created factory attribute
         */
        public void addAttribute(Attribute attr) {
            attributes.addElement(attr);
        }

        /**
         * return the attribute elements.
         * @return the enumeration of attributes
         */
        public Enumeration getAttributes() {
            return attributes.elements();
        }

        /**
         * A JAXP factory attribute. This is mostly processor specific, for
         * example for Xalan 2.3+, the following attributes could be set:
         * <ul>
         *  <li>http://xml.apache.org/xalan/features/optimize (true|false) </li>
         *  <li>http://xml.apache.org/xalan/features/incremental (true|false) </li>
         * </ul>
         */
        public static class Attribute implements DynamicConfigurator {

            /** attribute name, mostly processor specific */
            private String name;

            /** attribute value, often a boolean string */
            private Object value;

            /**
             * @return the attribute name.
             */
            public String getName() {
                return name;
            }

            /**
             * @return the output property value.
             */
            public Object getValue() {
                return value;
            }

            public Object createDynamicElement(String name) throws BuildException {
                return null;
            }

            public void setDynamicAttribute(String name, String value)
                    throws BuildException {
                // only 'name' and 'value' exist.
                if ("name".equalsIgnoreCase(name)) {
                    this.name = value;
                } else if ("value".equalsIgnoreCase(name)) {
                    // a value must be of a given type
                    // say boolean|integer|string that are mostly used.
                    if ("true".equalsIgnoreCase(value)
                            || "false".equalsIgnoreCase(value)) {
                        this.value = new Boolean(value);
                    } else {
                        try {
                            this.value = new Integer(value);
                        } catch (NumberFormatException e) {
                            this.value = value;
                        }
                    }
                } else {
                    throw new BuildException("Unsupported attribute: " + name);
                }
            }
        } // -- class Attribute

    } // -- class Factory

} //-- XSLTProcess
