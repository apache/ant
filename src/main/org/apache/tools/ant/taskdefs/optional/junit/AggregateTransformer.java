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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.xalan.xslt.XSLTProcessorFactory;
import org.apache.xalan.xslt.XSLTProcessor;
import org.apache.xalan.xslt.XSLTInputSource;
import org.apache.xalan.xslt.XSLTResultTarget;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

import org.xml.sax.SAXException;

/**
 * Transform a JUnit xml report.
 * The default transformation generates an html report in either framed or non-framed
 * style. The non-framed style is convenient to have a concise report via mail, the
 * framed report is much more convenient if you want to browse into different
 * packages or testcases since it is a Javadoc like report.
 * In the framed report, there are 3 frames:
 * <ul>
 *  <li>packageListFrame - list of all packages.
 *  <li>classListFrame - summary of all testsuites belonging to a package or tests list.
 *  <li>classFrame - details of all tests made for a package or for a testsuite.
 * </ul>
 * As a default, the transformer will use its default stylesheets, they may not be
 * be appropriate for users who wants to customize their reports, so you can indicates
 * your own stylesheets by using <tt>setStyleDir()</tt>.
 * Stylesheets must be as follows:
 * <ul>
 *   <li><b>all-packages.xsl</b> create the package list. It creates
 *   all-packages.html file in the html folder and it is load in the packageListFrame</li>
 *   <li><b>all-classes.xsl</b> creates the class list. It creates the all-classes.html
 * file in the html folder is loaded by the 'classListFrame' frame</li>
 *   <li><b>overview-packages.xsl</b> allows to get summary on all tests made
 *   for each packages and each class that not include in a package. The filename
 *   is overview-packages.html</li>
 *   <li><b>class-detail.xsl</b> is the style for the detail of the tests made on a class.
 *   the Html resulting page in write in the directory of the package and the name
 *   of this page is the name of the class with "-detail" element. For instance,
 *   the style is applied on the MyClass testsuite, the resulting filename is
 *   <u>MyClass-detail.html</u>. This file is load in the "classFrame" frame.</li>
 *   <li><b>package-summary.xsl</b> allows to create a summary on the package.
 *   The resulting html file is write in the package directory. The name of this
 *   file is <u>package-summary.html</u> This file is load in the "classFrame" frame.</li>
 *   <li><b>classes-list.xsl</b> create the list of the class in this package.
 *   The resulting html file is write in the package directory and it is load in
 *   the 'classListFrame' frame. The name of the resulting file is <u>class-list.html</u></li>
 * <li>
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 * @author <a href="mailto:ndelahaye@imediation.com">Nicolas Delahaye</a>
 */
public class AggregateTransformer {     
        
    public final static String ALLPACKAGES = "all-packages";
        
    public final static String ALLCLASSES = "all-classes";
        
    public final static String OVERVIEW_PACKAGES = "overview-packages";
        
    public final static String CLASS_DETAILS = "class-details";
        
    public final static String CLASSES_LIST = "classes-list";
        
    public final static String PACKAGE_SUMMARY = "package-summary";
        
    public final static String OVERVIEW_SUMMARY = "overview-summary";

    public final static String FRAMES = "frames";

    public final static String NOFRAMES = "noframes";

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

    /** the file extension of the generated files. As a default it will be <tt>.html</tt> */
    protected String extension;

    /** XML Parser factory */
    protected static final DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();

    public AggregateTransformer(Task task){
        this.task = task;
    }

    public void setFormat(String format){
        this.format = format;
    }

    public void setXmlDocument(Document doc){
        this.document = doc;
    }

    /**
     * Set the xml file to be processed. This is a helper if you want
     * to set the file directly. Much more for testing purposes.
     * @param xmlfile xml file to be processed
     */
    void setXmlfile(File xmlfile) throws BuildException {
        try {
            setXmlDocument(readDocument(xmlfile));
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
        this.extension = ext;
    }

    /** get the extension, if it is null, it will use .html as the default */
    protected String getExtension(){
        if (extension == null) {
            extension = ".html";
        }
        return extension;
    }

    public void transform() throws BuildException {
        checkOptions();
        try {
            Element root = document.getDocumentElement();
                        
            if (NOFRAMES.equals(format)) {
                //createCascadingStyleSheet();
                createSinglePageSummary(root);
            } else {
                createFrameStructure();
                createCascadingStyleSheet();
                createPackageList(root);
                createClassList(root);
                createPackageOverview(root);
                createAllTestSuiteDetails(root);
                createAllPackageDetails(root);
            }
        } catch (Exception e){
            e.printStackTrace();
            throw new BuildException("Errors while applying transformations", e);
        }
    }

    /** check for invalid options */
    protected void checkOptions() throws BuildException {
        if ( !FRAMES.equals(format) && !NOFRAMES.equals(format)) {
            throw new BuildException("Invalid format. Must be 'frames' or 'noframes' but was: '" + format + "'");
        }
        // set the destination directory relative from the project if needed.
        if (toDir == null) {
            toDir = task.getProject().resolveFile(".");
        } else if ( !toDir.isAbsolute() ) {
            toDir = task.getProject().resolveFile(toDir.getPath());
        }
        // create the directories if needed
        if (!toDir.exists()) {
            if (!toDir.mkdirs()){
                throw new BuildException("Could not create directory " + toDir);
            }
        }
    }

    /** create a single page summary */
    protected void createSinglePageSummary(Element root) throws IOException, SAXException {
        transform(root, OVERVIEW_SUMMARY + ".xsl", OVERVIEW_SUMMARY + getExtension());
    }

    /**
     * read the xml file that should be the resuiting file of the testcase.
     * @param filename name of the xml resulting file of the testcase.
     */
    protected Document readDocument(File file) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilder builder = dbfactory.newDocumentBuilder();
        InputStream in = new FileInputStream(file);
        try {
            return builder.parse(in);
        } finally {
            in.close();
        }
    }

    protected void createCascadingStyleSheet() throws IOException, SAXException {
        InputStream in = null;
        if (styleDir == null) {
            in = getResourceAsStream("html/stylesheet.css");
        } else {
            in = new FileInputStream(new File(styleDir, "stylesheet.css"));
        }
        OutputStream out = new FileOutputStream( new File(toDir, "stylesheet.css"));
        copy(in, out);
    }

    protected void createFrameStructure() throws IOException, SAXException{
        InputStream in = null;
        if (styleDir == null) {
            in = getResourceAsStream("html/index.html");
        } else {
            in = new FileInputStream(new File(styleDir, "index.html"));
        }
        OutputStream out = new FileOutputStream( new File(toDir, "index.html") );
        copy(in, out);
    }
        
    /**
     * Create the list of all packages.
     * @param root root of the xml document.
     */
    protected void createPackageList(Node root) throws SAXException {
        transform(root, ALLPACKAGES + ".xsl", ALLPACKAGES + getExtension());
    }

    /**
     * Create the list of all classes.
     * @param root root of the xml document.
     */
    protected void createClassList(Node root) throws SAXException {
        transform(root, ALLCLASSES + ".xsl", ALLCLASSES + getExtension());
    }

    /**
     * Create the summary used in the overview.
     * @param root root of the xml document.
     */
    protected void createPackageOverview(Node root) throws SAXException {
        transform(root,  OVERVIEW_PACKAGES + ".xsl", OVERVIEW_PACKAGES + getExtension());
    }

    /**
     *  @return the list of all packages that exists defined in testsuite nodes
     */
    protected Enumeration getPackages(Element root){
        Hashtable map = new Hashtable();
        NodeList testsuites = root.getElementsByTagName(XMLConstants.TESTSUITE);
        final int size = testsuites.getLength();
        for (int i = 0; i < size; i++){
            Element testsuite = (Element) testsuites.item(i);
            String packageName = testsuite.getAttribute(XMLConstants.ATTR_PACKAGE);
            if (packageName == null){
                //@todo replace the exception by something else
                throw new IllegalStateException("Invalid 'testsuite' node: should contains 'package' attribute");
            }
            map.put(packageName, packageName);
        }
        return map.keys();
    }

    /**
     * create all resulting html pages for all testsuites.
     * @param root should be 'testsuites' node.
     */
    protected void createAllTestSuiteDetails(Element root) throws SAXException {
        NodeList testsuites = root.getElementsByTagName(XMLConstants.TESTSUITE);
        final int size = testsuites.getLength();
        for (int i = 0; i < size; i++){
            Element testsuite = (Element) testsuites.item(i);
            createTestSuiteDetails(testsuite);
        }
    }

    /**
     * create the html resulting page of one testsuite.
     * @param root should be 'testsuite' node.
     */
    protected void createTestSuiteDetails(Element testsuite) throws SAXException {
                
        String packageName = testsuite.getAttribute(XMLConstants.ATTR_PACKAGE);         
        String pkgPath = packageToPath(packageName);
        
        // get the class name
        String name = testsuite.getAttribute(XMLConstants.ATTR_NAME);

        // get the name of the testsuite and create the filename of the ouput html page
        String filename = name + "-details" + getExtension();
        String fullpathname = pkgPath + filename; // there's already end path separator to pkgPath

        // apply the style on the document.
        transform(testsuite, CLASS_DETAILS + ".xsl", fullpathname);
    }

    /**
     * create the html resulting page of the summary of each package of the root element.
     * @param root should be 'testsuites' node.
     */
    protected void createAllPackageDetails(Element root) throws SAXException, ParserConfigurationException {
        Enumeration packages = getPackages(root);
        while ( packages.hasMoreElements() ){
            String pkgname = (String)packages.nextElement();
            // for each package get the list of its testsuite.
            DOMUtil.NodeFilter pkgFilter = new PackageFilter(pkgname);
            NodeList testsuites = DOMUtil.listChildNodes(root, pkgFilter, false);
            Element doc = buildDocument(testsuites);
            // skip package details if the package does not exist (root package)
            if( !pkgname.equals("") ){
                createPackageDetails(doc, pkgname);
            }
        }
    }

    protected String packageToPath(String pkgname){
        if (!pkgname.equals("")) {
            return pkgname.replace('.', File.separatorChar) + File.separatorChar;
        }
        return "." + File.separatorChar;
    }

    /**
     * create the html resulting page of the summary of a package .
     * @param root should be 'testsuites' node.
     * @param pkgname Name of the package that we want a summary.
     */
    protected void createPackageDetails(Node root, String pkgname) throws SAXException {
        String path = packageToPath(pkgname);

        // apply style to get the list of the classes of this package and
        // display it in the classListFrame.
        transform(root, CLASSES_LIST + ".xsl", path + CLASSES_LIST + getExtension());

        // apply style to get a summary on this package.
        transform(root, PACKAGE_SUMMARY + ".xsl", path + PACKAGE_SUMMARY + getExtension());
    }

    /**
     * Create an element root ("testsuites")
     * and import all nodes as children of this element.
     *
     */
    protected Element buildDocument(NodeList list) throws ParserConfigurationException {
        DocumentBuilder builder = dbfactory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element elem = doc.createElement(XMLConstants.TESTSUITES);
        final int len = list.getLength();
        for(int i=0 ; i < len ; i++) {
            DOMUtil.importNode(elem, list.item(i));
        }
        return elem;
    }

    /**
     * Apply a template on a part of the xml document.
     *
     * @param root root of the document fragment
     * @param xslfile style file
     * @param outfilename filename of the result of the style applied on the Node
     *
     * @throws SAXException SAX Parsing Error on the style Sheet.
     */
    protected void transform(Node root, String xslname, String htmlname) throws SAXException {
        try{
            final long t0 = System.currentTimeMillis();
            XSLTInputSource xsl_source = getXSLStreamSource(xslname);
            XSLTProcessor processor = XSLTProcessorFactory.getProcessor();
            File htmlfile = new File(toDir, htmlname);
            // create the directory if it does not exist
            File dir = new File(htmlfile.getParent()); // getParentFile is in JDK1.2+
            if (!dir.exists()) {
                dir.mkdirs();
            }
            task.log("Applying '" + xslname + "'. Generating '" + htmlfile + "'", Project.MSG_VERBOSE);
            processor.process( new XSLTInputSource(root), xsl_source, new XSLTResultTarget(htmlfile.getAbsolutePath()) );
            final long dt = System.currentTimeMillis() - t0;
            task.log("Transform time for " + xslname + ": " + dt + "ms");
        } catch (IOException e){
            task.log(e.getMessage(), Project.MSG_ERR);
            e.printStackTrace(); //@todo bad, change this
            throw new SAXException(e.getMessage());
        }
    }


    /**
     * default xsls are embedded in the distribution jar. As a default we will use
     * them, otherwise we will get the one supplied by the client in a given
     * directory. It must have the same name.
     */
    protected XSLTInputSource getXSLStreamSource(String name) throws IOException {
        InputStream in;
        String systemId; //we need this because there are references in xsls
        if (styleDir == null){
            in = getResourceAsStream("xsl/" + name);
            systemId = getClass().getResource("xsl/" + name).toString();
        } else {
            File f = new File(styleDir, name);
            in= new FileInputStream(f);
            systemId = f.getAbsolutePath();
        }
        XSLTInputSource ss = new XSLTInputSource(in);
        ss.setSystemId(systemId);
        return ss;
    }

    private InputStream getResourceAsStream(String name) throws FileNotFoundException {
        InputStream in = getClass().getResourceAsStream(name);
        if (in == null) {
            throw new FileNotFoundException("Could not find resource '" + name + "'");
        }
        return in;
    }

    /** Do some raw stream copying */
    private static void copy(InputStream in, OutputStream out) throws IOException {
        int size = -1;
        byte[] buffer =  new byte[1024];
        // Make the copy
        while( (size = in.read(buffer)) != -1){
            out.write(buffer,0,size);
        }
    }


    /**
     * allow us to check if the node is a object of a specific package.
     */
    protected static class PackageFilter implements DOMUtil.NodeFilter {
        private final String pkgName;

        PackageFilter(String pkgname) {
            this.pkgName = pkgname;
        }
        /**
         * if the node receive is not a element then return false
         * check if the node is a class of this package.
         */
        public boolean accept(Node node) {
            String pkgname = DOMUtil.getNodeAttribute(node, XMLConstants.ATTR_PACKAGE);
            return pkgName.equals(pkgname);
        }
    }
}
