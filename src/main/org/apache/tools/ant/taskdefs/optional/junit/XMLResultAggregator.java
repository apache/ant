/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.DOMElementWriter;


/**
 * <p> This is an helper class that will aggregate all testsuites under a specific
 * directory and create a new single document. It is not particulary clean but
 * should be helpful while I am thinking about another technique.
 *
 * <p> The main problem is due to the fact that a JVM can be forked for a testcase
 * thus making it impossible to aggregate all testcases since the listener is
 * (obviously) in the forked JVM. A solution could be to write a
 * TestListener that will receive events from the TestRunner via sockets. This
 * is IMHO the simplest way to do it to avoid this file hacking thing.
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class XMLResultAggregator extends Task implements XMLConstants {
    
    /** the list of all filesets, that should contains the xml to aggregate */
    protected Vector filesets = new Vector();
    
    /** the name of the result file */
    protected String toFile;
    
    /** the directory to write the file to */
    protected File toDir;
    
    protected Vector transformers = new Vector();
    
    /** the default directory: <tt>.</tt>. It is resolved from the project directory */
    public final static String DEFAULT_DIR = ".";
    
    /** the default file name: <tt>TESTS-TestSuites.xml</tt> */
    public final static String DEFAULT_FILENAME = "TESTS-TestSuites.xml";
    
    
    public AggregateTransformer createReport(){
        AggregateTransformer transformer = new AggregateTransformer(this);
        transformers.addElement(transformer);
        return transformer;
    }
    
    /**
     * Set the name of the file aggregating the results. It must be relative
     * from the <tt>todir</tt> attribute. If not set it will use {@link #DEFAULT_FILENAME}
     * @param  value   the name of the file.
     * @see #setTodir(File)
     */
    public void setTofile(String value){
        toFile = value;
    }
    
    /**
     * Set the destination directory where the results should be written. If not
     * set if will use {@link #DEFAULT_DIR}. When given a relative directory
     * it will resolve it from the project directory.
     * @param value    the directory where to write the results, absolute or
     * relative.
     */
    public void setTodir(File value){
        toDir = value;
    }
    
    /**
     * Add a new fileset containing the xml results to aggregate
     * @param    fs      the new fileset of xml results.
     */
    public void addFileSet(FileSet fs) {
        filesets.addElement(fs);
    }
    
    /**
     * Aggregate all testsuites into a single document and write it to the
     * specified directory and file.
     * @throws	BuildException	thrown if there is a serious error while writing
     *			the document.
     */
    public void execute() throws BuildException {
        Element rootElement = createDocument();
        File destFile = getDestinationFile();
        // write the document
        try {
            writeDOMTree(rootElement.getOwnerDocument(),  destFile );
        } catch (IOException e){
            throw new BuildException("Unable to write test aggregate to '" + destFile + "'", e);
        }
        // apply transformation
        Enumeration enum = transformers.elements();
        while (enum.hasMoreElements()) {
            AggregateTransformer transformer =
                (AggregateTransformer) enum.nextElement();
            transformer.setXmlDocument(rootElement.getOwnerDocument());
            transformer.transform();
        }
    }
    
    /**
     * Get the full destination file where to write the result. It is made of
     * the <tt>todir</tt> and <tt>tofile</tt> attributes.
     * @return the destination file where should be written the result file.
     */
    protected File getDestinationFile(){
        if (toFile == null){
            toFile = DEFAULT_FILENAME;
        }
        if (toDir == null){
            toDir = project.resolveFile(DEFAULT_DIR);
        }
        return new File(toDir, toFile);
    }
    
    /**
     * Get all <code>.xml</code> files in the fileset.
     *
     * @return all files in the fileset that end with a '.xml'.
     */
    protected File[] getFiles() {
        Vector v = new Vector();
        final int size = filesets.size();
        for (int i = 0; i < size; i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(project);
            ds.scan();
            String[] f = ds.getIncludedFiles();
            for (int j = 0; j < f.length; j++) {
                String pathname = f[j];
                if ( pathname.endsWith(".xml") ) {
                    File file = new File(ds.getBasedir(), pathname);
                    file = project.resolveFile(file.getPath());
                    v.addElement( file );
                }
            }
        }
        
        File[] files = new File[v.size()];
        v.copyInto(files);
        return files;
    }
    
    //----- from now, the methods are all related to DOM tree manipulation
    
    /**
     * Write the DOM tree to a file.
     * @param doc the XML document to dump to disk.
     * @param file the filename to write the document to. Should obviouslly be a .xml file.
     * @throws IOException thrown if there is an error while writing the content.
     */
    protected void writeDOMTree(Document doc, File file) throws IOException {
        OutputStream out = new FileOutputStream( file );
        PrintWriter wri = new PrintWriter(new OutputStreamWriter(out, "UTF8"));
        wri.write("<?xml version=\"1.0\"?>\n");
        (new DOMElementWriter()).write(doc.getDocumentElement(), wri, 0, "  ");
        wri.flush();
        wri.close();
        // writers do not throw exceptions, so check for them.
        if (wri.checkError()){
            throw new IOException("Error while writing DOM content");
        }
    }
    
    /**
     * <p> Create a DOM tree. 
     * Has 'testsuites' as firstchild and aggregates all
     * testsuite results that exists in the base directory.
     * @return	the root element of DOM tree that aggregates all testsuites.
     */
    protected Element createDocument() {
        // create the dom tree
        DocumentBuilder builder = getDocumentBuilder();
        Document doc = builder.newDocument();
        Element rootElement = doc.createElement(TESTSUITES);
        doc.appendChild(rootElement);
        
        // get all files and add them to the document
        File[] files = getFiles();
        for (int i = 0; i < files.length; i++) {
            try {
                log("Parsing file: '" + files[i] + "'", Project.MSG_VERBOSE);
                //XXX there seems to be a bug in xerces 1.3.0 that doesn't like file object
                // will investigate later. It does not use the given directory but
                // the vm dir instead ? Works fine with crimson.
                Document testsuiteDoc = builder.parse( "file:///" + files[i].getAbsolutePath() );
                Element elem = testsuiteDoc.getDocumentElement();
                // make sure that this is REALLY a testsuite.
                if ( TESTSUITE.equals(elem.getNodeName()) ) {
                    addTestSuite(rootElement, elem);
                } else {
                    // issue a warning.
                    log("the file " + files[i] + " is not a valid testsuite XML document", Project.MSG_WARN);
                }
            } catch (SAXException e){
                // a testcase might have failed and write a zero-length document,
                // It has already failed, but hey.... mm. just put a warning
                log("The file " + files[i] + " is not a valid XML document. It is possibly corrupted.", Project.MSG_WARN);
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                log(sw.toString(), Project.MSG_DEBUG);
            } catch (IOException e){
                log("Error while accessing file " + files[i] + ": " + e.getMessage(), Project.MSG_ERR);
            }
        }
        return rootElement;
    }
    
    /**
     * <p> Add a new testsuite node to the document.
     * The main difference is that it
     * split the previous fully qualified name into a package and a name.
     * <p> For example: <tt>org.apache.Whatever</tt> will be split into
     * <tt>org.apache</tt> and <tt>Whatever</tt>.
     * @param root the root element to which the <tt>testsuite</tt> node should
     *        be appended.
     * @param testsuite the element to append to the given root. It will slightly
     *        modify the original node to change the name attribute and add
     *        a package one.
     */
    protected void addTestSuite(Element root, Element testsuite){
        String fullclassname = testsuite.getAttribute(ATTR_NAME);
        int pos = fullclassname.lastIndexOf('.');
        
        // a missing . might imply no package at all. Don't get fooled.
        String pkgName = (pos == -1) ? "" : fullclassname.substring(0, pos);
        String classname = (pos == -1) ? fullclassname : fullclassname.substring(pos + 1);
        Element copy = (Element)DOMUtil.importNode(root, testsuite);
        
        // modify the name attribute and set the package
        copy.setAttribute(ATTR_NAME, classname);
        copy.setAttribute(ATTR_PACKAGE, pkgName);
    }
    
    /**
     * Create a new document builder. Will issue an <tt>ExceptionInitializerError</tt>
     * if something is going wrong. It is fatal anyway.
     * @return a new document builder to create a DOM
     * @todo factorize this somewhere else. It is duplicated code.
     */
    private static DocumentBuilder getDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch(Exception exc) {
            throw new ExceptionInInitializerError(exc);
        }
    }
    
}
