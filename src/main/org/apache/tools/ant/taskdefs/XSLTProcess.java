/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

import org.apache.tools.ant.*;

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;


/**
 * A Task to process via XSLT a set of XML documents. This is
 * useful for building views of XML based documentation.
 * arguments:
 * <ul>
 * <li>basedir
 * <li>destdir
 * <li>style
 * <li>includes
 * <li>excludes
 * </ul>
 * Of these arguments, the <b>sourcedir</b> and <b>destdir</b> are required.
 * <p>
 * This task will recursively scan the sourcedir and destdir
 * looking for XML documents to process via XSLT. Any other files,
 * such as images, or html files in the source directory will be
 * copied into the destination directory.
 *
 * @author <a href="mailto:kvisco@exoffice.com">Keith Visco</a>
 * @author <a href="mailto:rubys@us.ibm.com">Sam Ruby</a>
 * @version $Revision$ $Date$
 */
public class XSLTProcess extends MatchingTask {

    private File destDir = null;

    private File baseDir = null;

    private File xslFile = null;

    private String targetExtension = "html";

    private XSLTLiaison liaison;

    /**
     * Creates a new XSLTProcess Task.
    **/
    public XSLTProcess() {
    } //-- XSLTProcess

    /**
     * Executes the task.
     */

    public void execute() throws BuildException {
	DirectoryScanner scanner;
	String[]         list;
	String[]         dirs;

	if (baseDir == null)
	    baseDir = project.resolveFile(".");
        //-- make sure Source directory exists...
	if (destDir == null ) {
	    String msg = "destdir attributes must be set!";
	    throw new BuildException(msg);
	}
	scanner = getDirectoryScanner(baseDir);
	log("Transforming into "+destDir, Project.MSG_INFO);

        // if processor wasn't specified, default it to xslp or xalan,
        // depending on which is in the classpath
        if (liaison == null) {
           try {
               setProcessor("xslp");
           } catch (Throwable e1) {
               try {
                   setProcessor("xalan");
               } catch (Throwable e2) {
                   throw new BuildException(e2);
               }
           }
        }

	log("Using "+liaison.getClass().toString(), Project.MSG_VERBOSE);

	try {
	    // Create a new XSL processor with the specified stylesheet
	    if (xslFile != null) {
                String file = new File(baseDir,xslFile.toString()).toString();
		log("Loading stylesheet " + file, Project.MSG_INFO);
                liaison.setStylesheet( file );
	    }
	} catch (Exception ex) {
	    log("Failed to read stylesheet " + xslFile, Project.MSG_INFO);
            throw new BuildException(ex);
	}

	// Process all the files marked for styling
	list = scanner.getIncludedFiles();
	for (int i = 0;i < list.length; ++i) {
	    process(baseDir,list[i],destDir);
	}

	// Process all the directoried marked for styling
	dirs = scanner.getIncludedDirectories();
	for (int j = 0;j < dirs.length;++j){
	    list=new File(baseDir,dirs[j]).list();
	    for (int i = 0;i < list.length;++i)
		process(baseDir,list[i],destDir);
	}
    } //-- execute

    /**
     * Set the base directory.
    **/
    public void setBasedir(String dirName) {
	    baseDir = project.resolveFile(dirName);
    } //-- setSourceDir

    /**
     * Set the destination directory into which the XSL result
     * files should be copied to
     * @param dirName the name of the destination directory
    **/
    public void setDestdir(String dirName) {
	    destDir = project.resolveFile(dirName);
    } //-- setDestDir

    /**
     * Set the desired file extension to be used for the target
     * @param name the extension to use
    **/
    public void setExtension(String name) {
	    targetExtension = name;
    } //-- setDestDir

    /**
     * Sets the file to use for styling relative to the base directory.
     */
    public void setStyle(String xslFile) {
	this.xslFile = new File(xslFile);
    }

    /**
     * Sets the file to use for styling relative to the base directory.
     */
    public void setProcessor(String processor) throws Exception {

	if (processor.equals("xslp")) {
            liaison = (XSLTLiaison) Class.forName("org.apache.tools.ant.taskdefs.optional.XslpLiaison").newInstance();
	} else if (processor.equals("xalan")) {
            liaison = (XSLTLiaison) Class.forName("org.apache.tools.ant.taskdefs.optional.XalanLiaison").newInstance();
        } else {
            liaison = (XSLTLiaison) Class.forName(processor).newInstance();
        }

    }

    /*
    private void process(File sourceDir, File destDir)
        throws BuildException
    {

        
        if (!sourceDir.isDirectory()) {
            throw new BuildException(sourceDir.getName() +
                " is not a directory!");
        }
        else if (!destDir.isDirectory()) {
            throw new BuildException(destDir.getName() +
                " is not a directory!");
        }

	    String[] list = sourceDir.list(new DesirableFilter());

	    if (list == null) {
	        return;  //-- nothing to do
	    }

	    for (int i = 0; i < list.length; i++) {

    	    String filename = list[i];

    	    File inFile  = new File(sourceDir, filename);

	        //-- if inFile is a directory, recursively process it
    	    if (inFile.isDirectory()) {
		if (!excluded(filename)) {
		new File(destDir, filename).mkdir();
		process(inFile, new File(destDir, filename));
		}
	    }
	    //-- process XML files
	    else if (hasXMLFileExtension(filename) && ! excluded(filename)) {

	            //-- replace extension with the target extension
	            int idx = filename.lastIndexOf('.');

		        File outFile = new File(destDir,
		            filename.substring(0,idx) + targetExt);

		        if ((inFile.lastModified() > outFile.lastModified()) ||
			    (xslFile != null && xslFile.lastModified() > outFile.lastModified()))
		        {
			    processXML(inFile, outFile);
		        }
		    }
		    else {
		        File outFile = new File(destDir, filename);
		        if (inFile.lastModified() > outFile.lastModified()) {
		            try {
		                copyFile(inFile, outFile);
		            }
		            catch(java.io.IOException ex) {
		                String err = "error copying file: ";
		                err += inFile.getAbsolutePath();
		                err += "; " + ex.getMessage();
		                throw new BuildException(err, ex);
		            }
			        //filecopyList.put(srcFile.getAbsolutePath(),
					    //destFile.getAbsolutePath());
		        }
		    }
		} //-- </for>
    } //-- process(File, File)
    */

    /**
     * Processes the given input XML file and stores the result
     * in the given resultFile.
    **/
    private void process(File baseDir,String xmlFile,File destDir)
        throws BuildException
    {
	String fileExt=targetExtension;
	File   outFile=null;
	File   inFile=null;

	try {
	    inFile = new File(baseDir,xmlFile);
	    outFile = new File(destDir,xmlFile.substring(0,xmlFile.lastIndexOf('.'))+fileExt);
	    if (inFile.lastModified() > outFile.lastModified()) {
		//-- command line status
		log("Processing " + xmlFile + " to " + outFile, Project.MSG_VERBOSE);

		liaison.transform(inFile.toString(), outFile.toString());
	    }
        }
        catch (Exception ex) {
	    // If failed to process document, must delete target document,
	    // or it will not attempt to process it the second time
	    log("Failed to process " + inFile, Project.MSG_INFO);
	    outFile.delete();
            throw new BuildException(ex);
        }

    } //-- processXML

} //-- XSLTProcess
