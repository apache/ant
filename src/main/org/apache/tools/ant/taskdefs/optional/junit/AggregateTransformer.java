/*
 * Copyright  2001-2002,2004 The Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.JAXPUtils;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.w3c.dom.Document;

/**
 * Transform a JUnit xml report.
 * The default transformation generates an html report in either framed or non-framed
 * style. The non-framed style is convenient to have a concise report via mail, the
 * framed report is much more convenient if you want to browse into different
 * packages or testcases since it is a Javadoc like report.
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class AggregateTransformer {

    public static final String FRAMES = "frames";

    public static final String NOFRAMES = "noframes";

    public static class Format extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[]{FRAMES, NOFRAMES};
        }
    }

    /** Task */
    protected Task task;

    /** the xml document to process */
    protected Document document;

    /** the style directory. XSLs should be read from here if necessary */
    protected File styleDir;

    /** the destination directory, this is the root from where html should be generated */
    protected File toDir;

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

    public AggregateTransformer(Task task) {
        this.task = task;
    }

    /**
     * Get the Document Builder Factory
     *
     * @return the DocumentBuilderFactory instance in use
     */
    protected static DocumentBuilderFactory getDocumentBuilderFactory() {
        return privateDBFactory;
    }

    public void setFormat(Format format) {
        this.format = format.getValue();
    }

    public void setXmlDocument(Document doc) {
        this.document = doc;
    }

    /**
     * Set the xml file to be processed. This is a helper if you want
     * to set the file directly. Much more for testing purposes.
     * @param xmlfile xml file to be processed
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

    /** set the destination directory */
    public void setTodir(File todir) {
        this.toDir = todir;
    }

    /** set the extension of the output files */
    public void setExtension(String ext) {
        task.log("extension is not used anymore", Project.MSG_WARN);
    }

    public void transform() throws BuildException {
        checkOptions();
        final long t0 = System.currentTimeMillis();
        XalanExecutor executor = XalanExecutor.newInstance(this);
        try {
            executor.execute();
        } catch (Exception e) {
            throw new BuildException("Errors while applying transformations: "
                + e.getMessage(), e);
        }
        final long dt = System.currentTimeMillis() - t0;
        task.log("Transform time: " + dt + "ms");
    }

    /** check for invalid options */
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
