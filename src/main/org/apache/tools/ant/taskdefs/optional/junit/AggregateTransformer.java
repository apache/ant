/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.EnumeratedAttribute;

import java.io.*;

import java.util.Enumeration;
import java.util.Hashtable;
import java.lang.reflect.Field;
import java.net.URL;

import org.apache.xalan.xslt.XSLTProcessorFactory;
import org.apache.xalan.xslt.XSLTProcessor;
import org.apache.xalan.xslt.XSLTInputSource;
import org.apache.xalan.xslt.XSLTResultTarget;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

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

    public final static String FRAMES = "frames";

    public final static String NOFRAMES = "noframes";

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
    protected String format;

    /** XML Parser factory */
    protected static final DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();

    public AggregateTransformer(Task task){
        this.task = task;
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
            DocumentBuilder builder = dbfactory.newDocumentBuilder();
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
        try {
            Element root = document.getDocumentElement();
            XalanExecutor executor = XalanExecutor.newInstance(this);
            executor.execute();
        } catch (Exception e){
            throw new BuildException("Errors while applying transformations", e);
        }
        final long dt = System.currentTimeMillis() - t0;
        task.log("Transform time: " + dt + "ms");
    }

    /** check for invalid options */
    protected void checkOptions() throws BuildException {
        // set the destination directory relative from the project if needed.
        if (toDir == null) {
            toDir = task.getProject().resolveFile(".");
        } else if ( !toDir.isAbsolute() ) {
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
        URL url = null;
        if (styleDir == null){
            url = getClass().getResource("xsl/" + xslname);
            if (url == null){
                throw new FileNotFoundException("Could not find jar resource " + xslname);
            }
        } else {
            File file = new File(styleDir, xslname);
            if (!file.exists()){
                throw new FileNotFoundException("Could not find file '" + file + "'");
            }
            url = new URL("file", "", file.getAbsolutePath());
        }
        return url.toExternalForm();
    }

}

/**
 * Command class that encapsulate specific behavior for each
 * Xalan version. The right executor will be instantiated at
 * runtime via class lookup. For instance, it will check first
 * for Xalan2, then for Xalan1.
 */
abstract class XalanExecutor {
    /** the transformer caller */
    protected AggregateTransformer caller;

    /** set the caller for this object. */
    private final void setCaller(AggregateTransformer caller){
        this.caller = caller;
    }

    /** get the appropriate stream based on the format (frames/noframes) */
    protected OutputStream getOutputStream() throws IOException {
        if (caller.FRAMES.equals(caller.format)){
            // dummy output for the framed report
            // it's all done by extension...
            return new ByteArrayOutputStream();
        } else {
            return new FileOutputStream(new File(caller.toDir, "junit-noframes.html"));
        }
    }

    /** override to perform transformation */
    abstract void execute() throws Exception;

    /**
     * Create a valid Xalan executor. It checks first if Xalan2 is
     * present, if not it checks for xalan1. If none is available, it
     * fails.
     * @param caller object containing the transformation information.
     * @throws BuildException thrown if it could not find a valid xalan
     * executor.
     */
    static XalanExecutor newInstance(AggregateTransformer caller) throws BuildException {
        Class procVersion = null;
        XalanExecutor executor = null;
        try {
            procVersion = Class.forName("org.apache.xalan.processor.XSLProcessorVersion");
            executor = new Xalan2Executor();
        } catch (Exception xalan2missing){
            try {
                procVersion = Class.forName("org.apache.xalan.xslt.XSLProcessorVersion");
                executor = new Xalan1Executor();
            } catch (Exception xalan1missing){
                throw new BuildException("Could not find xalan2 nor xalan1 in the classpath. Check http://xml.apache.org/xalan-j");
            }
        }
        String version = getXalanVersion(procVersion);
        caller.task.log("Using Xalan version: " + version);
        executor.setCaller(caller);
        return executor;
    }

    /** pretty useful data (Xalan version information) to display. */
    private static String getXalanVersion(Class procVersion) {
        try {
            Field f = procVersion.getField("S_VERSION");
            return f.get(null).toString();
        } catch (Exception e){
            return "?";
        }
    }
}

/**
 * Xalan executor via JAXP. Nothing special must exists in the classpath
 * besides of course, a parser, jaxp and xalan.
 */
class Xalan2Executor extends XalanExecutor {
    void execute() throws Exception {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        String system_id = caller.getStylesheetSystemId();
        Source xsl_src = new StreamSource(system_id);
        Transformer tformer = tfactory.newTransformer(xsl_src);
        Source xml_src = new DOMSource(caller.document);
        OutputStream os = getOutputStream();
        tformer.setParameter("output.dir", caller.toDir.getAbsolutePath());
        Result result = new StreamResult(os);
        tformer.transform(xml_src, result);
    }
}

/**
 * Xalan 1 executor. It will need a lot of things in the classpath:
 * xerces for the serialization, xalan and bsf for the extension.
 * @todo do everything via reflection to avoid compile problems ?
 */
class Xalan1Executor extends XalanExecutor {
    void execute() throws Exception {
        XSLTProcessor processor = XSLTProcessorFactory.getProcessor();
        // need to quote otherwise it breaks because of "extra illegal tokens"
        processor.setStylesheetParam("output.dir", "'" + caller.toDir.getAbsolutePath() + "'");
        XSLTInputSource xml_src = new XSLTInputSource(caller.document);
        String system_id = caller.getStylesheetSystemId();
        XSLTInputSource xsl_src = new XSLTInputSource(system_id);
        OutputStream os = getOutputStream();
        XSLTResultTarget target = new XSLTResultTarget(os);
        processor.process( xml_src, xsl_src, target);
    }
}
