/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2001 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.ParserAdapter;

/** 
 * The <code>XMLValidateTask</code> checks that an XML document is valid,
 * with a SAX validating parser.
 * @author Raphael Pierquin <a href="mailto:raphael.pierquin@agisphere.com">raphael.pierquin@agisphere.com</a>
 */
public class XMLValidateTask extends Task {

    /**
     * The default implementation parser classname used by the task to process
     * validation.
     */
    // The crimson implementation is shipped with ant.
    public static String DEFAULT_XML_READER_CLASSNAME= "org.apache.crimson.parser.XMLReaderImpl";

    protected static String INIT_FAILED_MSG = "Could'nt start xml validation: ";

    // ant task properties
    // defaults
    protected boolean failOnError = true;
    protected boolean warn = true;
    protected boolean lenient = false;
    protected String  readerClassName = DEFAULT_XML_READER_CLASSNAME;
    
    protected File file = null; // file to be validated
    protected Vector filesets = new Vector(); // sets of file to be validated
    protected Path classpath;


    /**
     * the parser is viewed as a SAX2 XMLReader. If a SAX1 parser is specified,
     * it's wrapped in an adapter that make it behave as a XMLReader.
     * a more 'standard' way of doing this would be to use the JAXP1.1 SAXParser
     * interface.
     */
    protected XMLReader xmlReader = null; // XMLReader used to validation process
    protected ValidatorErrorHandler errorHandler
        = new ValidatorErrorHandler(); // to report sax parsing errors
    protected Hashtable features = new Hashtable();


    /**
     * Specify how parser error are to be handled.
     * <p>
     * If set to <code>true</code> (default), throw a buildException if the parser yields an error.
     */
    public void setFailOnError(boolean fail) {
        
        failOnError = fail;
    }

    /**
     * Specify how parser error are to be handled.
     * <p>
     * If set to <code>true</true> (default), log a warn message for each SAX warn event.
     */
    public void setWarn(boolean bool) {
        
        warn = bool;
    }

    /**
     * Specify whether the parser should be validating. Default is <code>true</code>.
     * <p>
     * If set to false, the validation will fail only if the parsed document is not well formed XML.
     * <p>
     * this option is ignored if the specified class with {@link #setClassName(String)} is not a SAX2
     * XMLReader.
     */
    public void setLenient(boolean bool) {

        lenient = bool;
    }
    
    /**
     * Specify the class name of the SAX parser to be used. (optional)
     * @param className should be an implementation of SAX2 <code>org.xml.sax.XMLReader</code>
     * or SAX2 <code>org.xml.sax.Parser</code>.
     * <p> if className is an implementation of <code>org.xml.sax.Parser</code>, {@link #setLenient(boolean)},
     * will be ignored.
     * <p> if not set, the default {@link #DEFAULT_XML_READER_CLASSNAME} will be used.
     * @see org.xml.sax.XMLReader;
     * @see org.xml.sax.Parser;
     */
    public void setClassName(String className) {
        
        readerClassName = className;
    }


    /**
     * Specify the classpath to be searched to load the parser (optional)
     */
    public void setClasspath(Path classpath) {

        if (this.classpath == null) {
            this.classpath = classpath;
        } else {
            this.classpath.append(classpath);
        }
    }

    /**
     * @see #setClassPath
     */
    public Path createClasspath() {
        if (this.classpath == null) {
            this.classpath = new Path(project);
        }
        return this.classpath.createPath();
    }

    /**
     * @see #setClassPath
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * specifify the file to be checked
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * specifify a set of file to be checked
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    public void execute() throws BuildException {

        int fileProcessed = 0;
        if (file == null && (filesets.size()==0) ) {
            throw new BuildException("Specify at least one source - a file or a fileset.");
        }

        initValidator();

        if (file != null) {
            if (file.exists() && file.canRead() && file.isFile())  {
                doValidate(file);
                fileProcessed++;
            }
            else {
                String errorMsg = "File " + file + " cannot be read";
                if (failOnError)
                    throw new BuildException(errorMsg);
                else
                    log(errorMsg, Project.MSG_ERR);
            }
        }
            
        for (int i=0; i<filesets.size(); i++) {

            FileSet fs = (FileSet) filesets.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(project);
            String[] files = ds.getIncludedFiles();
            
            for (int j=0; j < files.length ; j++)  {
                File srcFile = new File(fs.getDir(project), files[j]);
                doValidate(srcFile);
                fileProcessed++;
            }
        }
        log(fileProcessed + " file(s) have been successfully validated.");
    }

    /**
     * init the parser :
     * load the parser class, and set features if necessary
     */
    private void initValidator() {

        try {
            // load the parser class
            // with JAXP, we would use a SAXParser factory
            Class readerClass= null;
            //Class readerImpl = null;
            //Class parserImpl = null;
            if (classpath != null) {
                AntClassLoader loader = new AntClassLoader(project, classpath);
//                loader.addSystemPackageRoot("org.xml"); // needed to avoid conflict
                readerClass = loader.loadClass(readerClassName);
                AntClassLoader.initializeClass(readerClass);
            } else 
                readerClass = Class.forName(readerClassName);
            
            // then check it implements XMLReader
            if (XMLReader.class.isAssignableFrom(readerClass)) {

                xmlReader = (XMLReader) readerClass.newInstance();
                log("Using SAX2 reader " + readerClassName, Project.MSG_VERBOSE);
            } else {
                
                // see if it is a SAX1 Parser
                if (Parser.class.isAssignableFrom(readerClass)) {
                    Parser parser = (Parser) readerClass.newInstance();
                    xmlReader = new ParserAdapter(parser);
                    log("Using SAX1 parser " + readerClassName, Project.MSG_VERBOSE);
                }  else {
                    throw new BuildException(INIT_FAILED_MSG
                                             + readerClassName
                                             + " implements nor SAX1 Parser nor SAX2 XMLReader.");
                }
            }
        } catch (ClassNotFoundException e) {
            throw new BuildException(INIT_FAILED_MSG + readerClassName, e);
        } catch (InstantiationException e) {
            throw new BuildException(INIT_FAILED_MSG + readerClassName, e);
        } catch (IllegalAccessException e) {
            throw new BuildException(INIT_FAILED_MSG + readerClassName, e);
        }

        xmlReader.setErrorHandler(errorHandler);
        
        if (! (xmlReader instanceof ParserAdapter)) {
            // turn validation on
            if (! lenient) {
                boolean ok = setFeature("http://xml.org/sax/features/validation",true,true);
                if (! ok) {
                    throw new BuildException(INIT_FAILED_MSG
                                             + readerClassName
                                             + " doesn't provide validation");
                }
            }
            // set other features
            Enumeration enum = features.keys();
            while(enum.hasMoreElements()) {
                String featureId = (String) enum.nextElement();
                setFeature(featureId, ((Boolean) features.get(featureId)).booleanValue(), true);
            }
        }
    }

    /*
     * set a feature on the parser.
     * TODO: find a way to set any feature from build.xml
     */
    private boolean setFeature(String feature, boolean value, boolean warn) {

        boolean  toReturn = false;
        try {
            xmlReader.setFeature(feature,value);
            toReturn = true;
        } catch (SAXNotRecognizedException e) {
            if (warn)
                log("Could'nt set feature '"
                    + feature
                    + "' because the parser doesn't recognize it", 
                    Project.MSG_WARN);
        } catch (SAXNotSupportedException  e) {
            if (warn)
                log("Could'nt set feature '"
                    + feature 
                    + "' because the parser doesn't support it",
                    Project.MSG_WARN);
        }
        return toReturn;
    }
    /*
     * parse the file
     */
    private void doValidate(File afile) {
        try {
            log("Validating " + afile.getName() + "... ", Project.MSG_VERBOSE);
            errorHandler.init(afile);
            InputSource is = new InputSource(new FileReader(afile));
            String uri = "file:" + afile.getAbsolutePath().replace('\\', '/');
            for (int index = uri.indexOf('#'); index != -1;
                 index = uri.indexOf('#')) {
                uri = uri.substring(0, index) + "%23" + uri.substring(index+1);
            }
            is.setSystemId(uri);
            xmlReader.parse(is);
        } catch (SAXException ex) {
            if (failOnError)
                throw new BuildException("Could'nt validate document " + afile);
        } catch (IOException ex) {
            throw new BuildException("Could'nt validate document " + afile, ex);
        }
        
        if (errorHandler.getFailure()) {
            if (failOnError)
                throw new BuildException(afile + " is not a valid XML document.");
            else
                log(afile + " is not a valid XML document",Project.MSG_ERR);
        }        
    }

    /*
     * ValidatorErrorHandler role : 
     * <ul>
     * <li> log SAX parse exceptions,
     * <li> remember if an error occured
     * </ul>
     */
    protected class ValidatorErrorHandler implements ErrorHandler {
        
        protected File currentFile = null;
        protected String lastErrorMessage = null;
        protected boolean failed = false;
        
        public void init(File file) {
            currentFile = file;
            failed = false;
        }
        
        // did an error happen during last parsing ?
        public boolean getFailure() {
            
            return failed;
        }

        public void fatalError(SAXParseException exception) {

            failed = true;
            doLog(exception,Project.MSG_ERR);
        }

        public void error(SAXParseException exception) {

            failed = true;
            doLog(exception,Project.MSG_ERR);
        }
        
        public void warning(SAXParseException exception) {
            // depending on implementation, XMLReader can yield hips of warning, 
            // only output then if user explicitely asked for it
            if (warn)
                doLog(exception,Project.MSG_WARN);
        }
        
        private void doLog(SAXParseException e, int logLevel) {
            
            log(getMessage(e), logLevel);
        }

        private String getMessage(SAXParseException e) {
            String sysID = e.getSystemId();
            if (sysID != null) {
                try {
                    int line = e.getLineNumber();
                    int col = e.getColumnNumber();
                    return new URL(sysID).getFile() +
                        (line == -1 ? "" : (":" + line +
                                            (col == -1 ? "" : (":" + col)))) +
                        ": " + e.getMessage();
                } catch (MalformedURLException mfue) {
                }
            }
            return e.getMessage();
        }
    }
}
