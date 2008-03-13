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
package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.XSLTProcess;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.TempFile;
import org.apache.tools.ant.util.JAXPUtils;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.URLResource;
import org.apache.tools.ant.types.resources.FileResource;

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
        public String[] getValues() {
            return new String[]{FRAMES, NOFRAMES};
        }
    }

    // CheckStyle:VisibilityModifier OFF - bc
    /** Task */
    protected Task task;

    /** the xml document to process */
    protected Document document;

    /** the style directory. XSLs should be read from here if necessary */
    protected File styleDir;

    /** the destination directory, this is the root from where html should be generated */
    protected File toDir;

    /**
     * The params that will be sent to the XSL transformation
     *
     * @since Ant 1.7
     */
    private List params;

    /**
     * Instance of a utility class to use for file operations.
     *
     * @since Ant 1.7
     */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * Used to ensure the uniqueness of a property
     */
    private static int counter = 0;

    /** the format to use for the report. Must be <tt>FRAMES</tt> or <tt>NOFRAMES</tt> */
    protected String format = FRAMES;

    /** XML Parser factory */
    private static DocumentBuilderFactory privateDBFactory;

    /** XML Parser factory accessible to subclasses */
    protected static DocumentBuilderFactory dbfactory;

    static {
       privateDBFactory = DocumentBuilderFactory.newInstance();
       dbfactory = privateDBFactory;
    }
    // CheckStyle:VisibilityModifier ON

    /**
     * constructor creating the transformer from the junitreport task.
     * @param task  task delegating to this class
     */
    public AggregateTransformer(Task task) {
        this.task = task;
        params = new Vector();
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
     * @param format  Must be <tt>FRAMES</tt> or <tt>NOFRAMES</tt>
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
            InputStream in = new FileInputStream(xmlfile);
            try {
                Document doc = builder.parse(in);
                setXmlDocument(doc);
            } finally {
                in.close();
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
        XSLTProcess.Param p = new XSLTProcess.Param();
        params.add(p);
        return p;
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

        XSLTProcess xsltTask = new XSLTProcess();
        xsltTask.bindToOwner(task);

        xsltTask.setXslResource(getStylesheet());

        // acrobatic cast.
        xsltTask.setIn(((XMLResultAggregator) task).getDestinationFile());
        File outputFile = null;
        if (format.equals(FRAMES)) {
            String tempFileProperty = getClass().getName() + String.valueOf(counter++);
            File tmp = FILE_UTILS.resolveFile(project.getBaseDir(),
                    project.getProperty("java.io.tmpdir"));
            tempFileTask.setDestDir(tmp);
            tempFileTask.setProperty(tempFileProperty);
            tempFileTask.execute();
            outputFile = new File(project.getProperty(tempFileProperty));
        } else {
            outputFile = new File(toDir, "junit-noframes.html");
        }
        xsltTask.setOut(outputFile);
        for (Iterator i = params.iterator(); i.hasNext();) {
            XSLTProcess.Param param = (XSLTProcess.Param) i.next();
            XSLTProcess.Param newParam = xsltTask.createParam();
            newParam.setProject(task.getProject());
            newParam.setName(param.getName());
            newParam.setExpression(param.getExpression());
        }
        XSLTProcess.Param paramx = xsltTask.createParam();
        paramx.setProject(task.getProject());
        paramx.setName("output.dir");
        paramx.setExpression(toDir.getAbsolutePath());
        final long t0 = System.currentTimeMillis();
        try {
            xsltTask.execute();
        } catch (Exception e) {
            throw new BuildException("Errors while applying transformations: "
                    + e.getMessage(), e);
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
        String xslname = "junit-frames.xsl";
        if (NOFRAMES.equals(format)) {
            xslname = "junit-noframes.xsl";
        }
        if (styleDir == null) {
            // If style dir is not specified we have to retrieve
            // the stylesheet from the classloader
            URLResource stylesheet = new URLResource();
            URL stylesheetURL = getClass().getClassLoader().getResource(
                    "org/apache/tools/ant/taskdefs/optional/junit/xsl/" + xslname);
            stylesheet.setURL(stylesheetURL);
            return stylesheet;
        }
        // If we are here, then the style dir is here and we
        // should read the stylesheet from the filesystem
        FileResource stylesheet = new FileResource();
        File stylesheetFile = new File(styleDir, xslname);
        stylesheet.setFile(stylesheetFile);
        return stylesheet;
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
        String xslname = "junit-frames.xsl";
        if (NOFRAMES.equals(format)) {
            xslname = "junit-noframes.xsl";
        }
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

}
