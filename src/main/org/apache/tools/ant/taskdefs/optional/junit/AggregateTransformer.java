/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
        public String[] getValues(){
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

    public AggregateTransformer(Task task){
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
    
    public void setFormat(Format format){
        this.format = format.getValue();
    }

    public void setXmlDocument(Document doc){
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
        } catch (Exception e){
            throw new BuildException("Error while parsing document: " + xmlfile, e);
        }
    }

    /**
     * set the style directory. It is optional and will override the
     * default xsl used.
     * @param styledir  the directory containing the xsl files if the user
     * would like to override with its own style.
     */
    public void setStyledir(File styledir){
        this.styleDir = styledir;
    }

    /** set the destination directory */
    public void setTodir(File todir){
        this.toDir = todir;
    }

    /** set the extension of the output files */
    public void setExtension(String ext){
        task.log("extension is not used anymore", Project.MSG_WARN);
    }

    public void transform() throws BuildException {
        checkOptions();
        final long t0 = System.currentTimeMillis();
        XalanExecutor executor = XalanExecutor.newInstance(this);
        try {
            executor.execute();
        } catch (Exception e){
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
        if (NOFRAMES.equals(format)){
            xslname = "junit-noframes.xsl";
        }
        if (styleDir == null){
            URL url = getClass().getResource("xsl/" + xslname);
            if (url == null){
                throw new FileNotFoundException("Could not find jar resource " + xslname);
            }
            return url.toExternalForm();
        }
        File file = new File(styleDir, xslname);
        if (!file.exists()){
            throw new FileNotFoundException("Could not find file '" + file + "'");
        }
        return JAXPUtils.getSystemId(file);
    }

}
