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
package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.TempFile;
import org.apache.tools.ant.taskdefs.XSLTProcess;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.URLResource;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JAXPUtils;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.w3c.dom.Document;

/**
 * Transform a JUnit xml report.
 * The default transformation generates an html report in either framed or non-framed
 * style. The non-framed style is convenient to have a concise report via mail, the
 * framed report is much more convenient if you want to browse into different
 * packages or testcases since it is a Javadoc like report.
 *
 */
public class AggregateTransformer {
    /**
     * name of the frames format.
     */
    public static final String FRAMES = "frames";

    /**
     * name of the no frames format.
     */
    public static final String NOFRAMES = "noframes";

    /**
     * defines acceptable formats.
     */
    public static class Format extends EnumeratedAttribute {
        /**
         * list authorized values.
         * @return authorized values.
         */
        @Override
        public String[] getValues() {
            return new String[]{FRAMES, NOFRAMES};
        }
    }

    private static final String JDK_INTERNAL_FACTORY =
            "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl";

    // CheckStyle:VisibilityModifier OFF - bc
    /** XML Parser factory */
    private static DocumentBuilderFactory privateDBFactory;

    /** XML Parser factory accessible to subclasses */
    protected static DocumentBuilderFactory dbfactory;

    static {
       privateDBFactory = DocumentBuilderFactory.newInstance();
       dbfactory = privateDBFactory;
    }

    /** Task */
    protected Task task;

    /** the xml document to process */
    protected Document document;

    /** the style directory. XSLs should be read from here if necessary */
    protected File styleDir;

    /** the destination directory, this is the root from where html should be generated */
    protected File toDir;

    /**
     * The internal XSLT task used to perform the transformation.
     *
     * @since Ant 1.9.5
     */
    private XSLTProcess xsltTask;

    /**
     * The JAXP factory used for the internal XSLT task.
     */
    private XSLTProcess.Factory xsltFactory;

    /**
     * Instance of a utility class to use for file operations.
     *
     * @since Ant 1.7
     */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * Used to ensure the uniqueness of a property
     */
    private static volatile int counter = 0;

    /** the format to use for the report. Must be <code>FRAMES</code> or <code>NOFRAMES</code> */
    protected String format = FRAMES;

    // CheckStyle:VisibilityModifier ON

    /**
     * constructor creating the transformer from the junitreport task.
     * @param task  task delegating to this class
     */
    public AggregateTransformer(Task task) {
        this.task = task;
        xsltTask = new XSLTProcess();
        xsltTask.bindToOwner(task);
    }

    /**
     * Get the Document Builder Factory
     *
     * @return the DocumentBuilderFactory instance in use
     */
    protected static DocumentBuilderFactory getDocumentBuilderFactory() {
        return privateDBFactory;
    }

    /**
     * sets the format.
     * @param format  Must be <code>FRAMES</code> or <code>NOFRAMES</code>
     */
    public void setFormat(Format format) {
        this.format = format.getValue();
    }

    /**
     * sets the input document.
     * @param doc input dom tree
     */
    public void setXmlDocument(Document doc) {
        this.document = doc;
    }

    /**
     * Set the xml file to be processed. This is a helper if you want
     * to set the file directly. Much more for testing purposes.
     * @param xmlfile xml file to be processed
     * @throws BuildException if the document cannot be parsed.
     */
    protected void setXmlfile(File xmlfile) throws BuildException {
        try {
            DocumentBuilder builder = privateDBFactory.newDocumentBuilder();
            try (InputStream in = Files.newInputStream(xmlfile.toPath())) {
                Document doc = builder.parse(in);
                setXmlDocument(doc);
            }
        } catch (Exception e) {
            throw new BuildException("Error while parsing document: " + xmlfile, e);
        }
    }

    /**
     * set the style directory. It is optional and will override the
     * default xsl used.
     * @param styledir  the directory containing the xsl files if the user
     * would like to override with its own style.
     */
    public void setStyledir(File styledir) {
        this.styleDir = styledir;
    }

    /** set the destination directory.
     * @param todir the destination directory
     */
    public void setTodir(File todir) {
        this.toDir = todir;
    }

    /** set the extension of the output files
     * @param ext extension.
     */
    public void setExtension(String ext) {
        task.log("extension is not used anymore", Project.MSG_WARN);
    }

    /**
     * Create an instance of an XSL parameter for configuration by Ant.
     *
     * @return an instance of the Param class to be configured.
     * @since Ant 1.7
     */
    public XSLTProcess.Param createParam() {
        return xsltTask.createParam();
    }

    /**
     * Creates a classpath to be used for the internal XSLT task.
     *
     * @return the classpath to be configured
     * @since Ant 1.9.5
     */
    public Path createClasspath() {
        return xsltTask.createClasspath();
    }

    /**
     * Creates a factory configuration to be used for the internal XSLT task.
     *
     * @return the factory description to be configured
     * @since Ant 1.9.5
     */
    public XSLTProcess.Factory createFactory() {
        if (xsltFactory == null) {
            xsltFactory = xsltTask.createFactory();
        }
        return xsltFactory;
    }

    /**
     * transformation
     * @throws BuildException exception if something goes wrong with the transformation.
     */
    public void transform() throws BuildException {
        checkOptions();
        Project project = task.getProject();

        TempFile tempFileTask = new TempFile();
        tempFileTask.bindToOwner(task);

        xsltTask.setXslResource(getStylesheet());

        // acrobatic cast.
        xsltTask.setIn(((XMLResultAggregator) task).getDestinationFile());
        File outputFile;
        if (FRAMES.equals(format)) {
            String tempFileProperty = getClass().getName() + counter++; //NOSONAR
            File tmp = FILE_UTILS.resolveFile(project.getBaseDir(), project
                    .getProperty("java.io.tmpdir"));
            tempFileTask.setDestDir(tmp);
            tempFileTask.setProperty(tempFileProperty);
            tempFileTask.execute();
            outputFile = new File(project.getProperty(tempFileProperty));
        } else {
            outputFile = new File(toDir, "junit-noframes.html");
        }
        xsltTask.setOut(outputFile);
        XSLTProcess.Param paramx = xsltTask.createParam();
        paramx.setProject(task.getProject());
        paramx.setName("output.dir");
        paramx.setExpression(toDir.getAbsolutePath());
        configureForRedirectExtension();
        final long t0 = System.currentTimeMillis();
        try {
            xsltTask.execute();
        } catch (Exception e) {
            throw new BuildException("Errors while applying transformations: " + e.getMessage(), e);
        }
        final long dt = System.currentTimeMillis() - t0;
        task.log("Transform time: " + dt + "ms");
        if (format.equals(FRAMES)) {
            Delete delete = new Delete();
            delete.bindToOwner(task);
            delete.setFile(outputFile);
            delete.execute();
        }
    }

    /**
     * access the stylesheet to be used as a resource.
     * @return stylesheet as a resource
     */
    protected Resource getStylesheet() {
        final String xslname = getXslName();
        if (styleDir == null) {
            // If style dir is not specified we have to retrieve
            // the stylesheet from the classloader
            URL stylesheetURL = getClass().getClassLoader().getResource(
                    "org/apache/tools/ant/taskdefs/optional/junit/xsl/" + xslname);
            return new URLResource(stylesheetURL);
        }
        // If we are here, then the style dir is here and we
        // should read the stylesheet from the filesystem
        return new FileResource(new File(styleDir, xslname));
    }

    /**
     * Gets the filename of the XSL stylesheet
     *
     * Will provide Xalan or Saxon specific
     * stylesheets.
     *
     * @return The filename of the stylesheet
     */
    private String getXslName() {
        final String suffix;

        final String xsltFactoryName = xsltFactory == null ? null :  xsltFactory.getName();
        if ("net.sf.saxon.TransformerFactoryImpl".equals(xsltFactoryName)) {
            suffix = "-saxon.xsl";
        } else {
            suffix = ".xsl";
        }
        final String xslname;
        if (NOFRAMES.equals(format)) {
            xslname = "junit-noframes" + suffix;
        } else {
            xslname = "junit-frames" + suffix;
        }
        return xslname;
    }

    /** check for invalid options
     * @throws BuildException if something goes wrong.
     */
    protected void checkOptions() throws BuildException {
        // set the destination directory relative from the project if needed.
        if (toDir == null) {
            toDir = task.getProject().resolveFile(".");
        } else if (!toDir.isAbsolute()) {
            toDir = task.getProject().resolveFile(toDir.getPath());
        }
    }

    /**
     * Get the systemid of the appropriate stylesheet based on its
     * name and styledir. If no styledir is defined it will load
     * it as a java resource in the xsl child package, otherwise it
     * will get it from the given directory.
     * @return system ID of the stylesheet.
     * @throws IOException thrown if the requested stylesheet does
     * not exist.
     */
    protected String getStylesheetSystemId() throws IOException {
        final String xslname = getXslName();
        if (styleDir == null) {
            URL url = getClass().getResource("xsl/" + xslname);
            if (url == null) {
                throw new FileNotFoundException("Could not find jar resource " + xslname);
            }
            return url.toExternalForm();
        }
        File file = new File(styleDir, xslname);
        if (!file.exists()) {
            throw new FileNotFoundException("Could not find file '" + file + "'");
        }
        return JAXPUtils.getSystemId(file);
    }

    /**
     * If we end up using the JDK's own TraX factory on Java 9+, then
     * set the features and attributes necessary to allow redirect
     * extensions to be used.
     * @since Ant 1.9.8
     */
    protected void configureForRedirectExtension() {
        XSLTProcess.Factory factory = createFactory();
        String factoryName = factory.getName();
        if (factoryName == null) {
            try {
                factoryName = TransformerFactory.newInstance().getClass().getName();
            } catch (TransformerFactoryConfigurationError exc) {
                throw new BuildException(exc);
            }
        }
        if (JDK_INTERNAL_FACTORY.equals(factoryName)
            && JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9)) {
            factory.addFeature(new XSLTProcess.Factory.Feature(
                "http://www.oracle.com/xml/jaxp/properties/enableExtensionFunctions",
                true));
        }
    }
}
