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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Vector;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Aggregates all &lt;junit&gt; XML formatter testsuite data under
 * a specific directory and transforms the results via XSLT.
 * It is not particularly clean but
 * should be helpful while I am thinking about another technique.
 *
 * <p>The main problem is due to the fact that a JVM can be forked for a testcase
 * thus making it impossible to aggregate all testcases since the listener is
 * (obviously) in the forked JVM. A solution could be to write a
 * TestListener that will receive events from the TestRunner via sockets. This
 * is IMHO the simplest way to do it to avoid this file hacking thing.</p>
 *
 * @ant.task name="junitreport" category="testing"
 */
public class XMLResultAggregator extends Task implements XMLConstants {

    // CheckStyle:VisibilityModifier OFF - bc
    /** the list of all filesets, that should contains the xml to aggregate */
    protected Vector<FileSet> filesets = new Vector<>();

    /** the name of the result file */
    protected String toFile;

    /** the directory to write the file to */
    protected File toDir;

    protected Vector<AggregateTransformer> transformers = new Vector<>();

    /** The default directory: <code>&#046;</code>. It is resolved from the project directory */
    public static final String DEFAULT_DIR = ".";

    /** the default file name: <code>TESTS-TestSuites.xml</code> */
    public static final String DEFAULT_FILENAME = "TESTS-TestSuites.xml";

    /** the current generated id */
    protected int generatedId = 0;

    /**
     * text checked for in tests, {@value}
     */
    static final String WARNING_IS_POSSIBLY_CORRUPTED
        = " is not a valid XML document. It is possibly corrupted.";
    /**
     * text checked for in tests, {@value}
     */
    static final String WARNING_INVALID_ROOT_ELEMENT
        = " is not a valid testsuite XML document";
    /**
     * text checked for in tests, {@value}
     */
    static final String WARNING_EMPTY_FILE
        = " is empty.\nThis can be caused by the test JVM exiting unexpectedly";
    // CheckStyle:VisibilityModifier ON

    /**
     * Generate a report based on the document created by the merge.
     * @return the report
     */
    public AggregateTransformer createReport() {
        AggregateTransformer transformer = new AggregateTransformer(this);
        transformers.add(transformer);
        return transformer;
    }

    /**
     * Set the name of the aggregated results file. It must be relative
     * from the <code>todir</code> attribute. If not set it will use {@link #DEFAULT_FILENAME}
     * @param  value   the name of the file.
     * @see #setTodir(File)
     */
    public void setTofile(String value) {
        toFile = value;
    }

    /**
     * Set the destination directory where the results should be written. If not
     * set if will use {@link #DEFAULT_DIR}. When given a relative directory
     * it will resolve it from the project directory.
     * @param value    the directory where to write the results, absolute or
     * relative.
     */
    public void setTodir(File value) {
        toDir = value;
    }

    /**
     * Add a new fileset containing the XML results to aggregate
     * @param    fs      the new fileset of xml results.
     */
    public void addFileSet(FileSet fs) {
        filesets.add(fs);
    }

    /**
     * Aggregate all testsuites into a single document and write it to the
     * specified directory and file.
     * @throws  BuildException  thrown if there is a serious error while writing
     *          the document.
     */
    @Override
    public void execute() throws BuildException {
        Element rootElement = createDocument();
        File destFile = getDestinationFile();
        // write the document
        try {
            writeDOMTree(rootElement.getOwnerDocument(), destFile);
        } catch (IOException e) {
            throw new BuildException("Unable to write test aggregate to '" + destFile + "'", e);
        }
        // apply transformation
        for (AggregateTransformer transformer : transformers) {
            transformer.setXmlDocument(rootElement.getOwnerDocument());
            transformer.transform();
        }
    }

    /**
     * Get the full destination file where to write the result. It is made of
     * the <code>todir</code> and <code>tofile</code> attributes.
     * @return the destination file where should be written the result file.
     */
    public File getDestinationFile() {
        if (toFile == null) {
            toFile = DEFAULT_FILENAME;
        }
        if (toDir == null) {
            toDir = getProject().resolveFile(DEFAULT_DIR);
        }
        return new File(toDir, toFile);
    }

    /**
     * Get all <code>.xml</code> files in the fileset.
     *
     * @return all files in the fileset that end with a '.xml'.
     */
    protected File[] getFiles() {
        Project p = getProject();
        return filesets.stream().flatMap(fs -> {
            DirectoryScanner ds = fs.getDirectoryScanner(p);
            ds.scan();
            return Stream.of(ds.getIncludedFiles())
                .filter(pathname -> pathname.endsWith(".xml"))
                    .map(pathname -> p.resolveFile(new File(ds.getBasedir(), pathname).getPath()));
        }).toArray(File[]::new);
    }

    //----- from now, the methods are all related to DOM tree manipulation

    /**
     * Write the DOM tree to a file.
     * @param doc the XML document to dump to disk.
     * @param file the filename to write the document to. Should obviously be a .xml file.
     * @throws IOException thrown if there is an error while writing the content.
     */
    protected void writeDOMTree(Document doc, File file) throws IOException {
        try (PrintWriter wri = new PrintWriter(new OutputStreamWriter(
                new BufferedOutputStream(Files.newOutputStream(file.toPath())),
                StandardCharsets.UTF_8))) {
            wri.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
            new DOMElementWriter().write(doc.getDocumentElement(), wri, 0,
                "  ");
            wri.flush();
            // writers do not throw exceptions, so check for them.
            if (wri.checkError()) {
                throw new IOException("Error while writing DOM content");
            }
        }
    }

    /**
     * Create a DOM tree.
     * Has 'testsuites' as firstchild and aggregates all
     * testsuite results that exists in the base directory.
     * @return  the root element of DOM tree that aggregates all testsuites.
     */
    protected Element createDocument() {
        // create the dom tree
        DocumentBuilder builder = getDocumentBuilder();
        Document doc = builder.newDocument();
        Element rootElement = doc.createElement(TESTSUITES);
        doc.appendChild(rootElement);

        generatedId = 0;

        // get all files and add them to the document
        for (File file : getFiles()) {
            try {
                log("Parsing file: '" + file + "'", Project.MSG_VERBOSE);
                if (file.length() > 0) {
                    Document testsuiteDoc = builder.parse(FileUtils
                            .getFileUtils().toURI(file.getAbsolutePath()));
                    Element elem = testsuiteDoc.getDocumentElement();
                    // make sure that this is REALLY a testsuite.
                    if (TESTSUITE.equals(elem.getNodeName())) {
                        addTestSuite(rootElement, elem);
                        generatedId++;
                    } else {
                        //wrong root element name
                        // issue a warning.
                        log("the file " + file
                                + WARNING_INVALID_ROOT_ELEMENT,
                                Project.MSG_WARN);
                    }
                } else {
                    log("the file " + file
                            + WARNING_EMPTY_FILE,
                            Project.MSG_WARN);
                }
            } catch (SAXException e) {
                // a testcase might have failed and write a zero-length document,
                // It has already failed, but hey.... mm. just put a warning
                log("The file " + file + WARNING_IS_POSSIBLY_CORRUPTED, Project.MSG_WARN);
                log(StringUtils.getStackTrace(e), Project.MSG_DEBUG);
            } catch (IOException e) {
                log("Error while accessing file " + file + ": "
                    + e.getMessage(), Project.MSG_ERR);
                log("Error while accessing file " + file + ": "
                    + e.getMessage(), e, Project.MSG_VERBOSE);
            }
        }
        return rootElement;
    }

    /**
     * <p>Add a new testsuite node to the document.
     * The main difference is that it
     * split the previous fully qualified name into a package and a name.</p>
     * <p>For example: <code>org.apache.Whatever</code> will be split into
     * <code>org.apache</code> and <code>Whatever</code>.</p>
     *
     * @param root the root element to which the <code>testsuite</code> node should
     *        be appended.
     * @param testsuite the element to append to the given root. It will slightly
     *        modify the original node to change the name attribute and add
     *        a package one.
     */
    protected void addTestSuite(Element root, Element testsuite) {
        String fullclassname = testsuite.getAttribute(ATTR_NAME);
        int pos = fullclassname.lastIndexOf('.');

        // a missing . might imply no package at all. Don't get fooled.
        String pkgName = (pos == -1) ? "" : fullclassname.substring(0, pos);
        String classname = (pos == -1) ? fullclassname : fullclassname.substring(pos + 1);
        Element copy = (Element) DOMUtil.importNode(root, testsuite);

        // modify the name attribute and set the package
        copy.setAttribute(ATTR_NAME, classname);
        copy.setAttribute(ATTR_PACKAGE, pkgName);
        copy.setAttribute(ATTR_ID, Integer.toString(generatedId));
    }

    /**
     * Create a new document builder. Will issue an <code>ExceptionInitializerError</code>
     * if something is going wrong. It is fatal anyway.
     * @todo factorize this somewhere else. It is duplicated code.
     * @return a new document builder to create a DOM
     */
    private static DocumentBuilder getDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception exc) {
            throw new ExceptionInInitializerError(exc);
        }
    }

}
