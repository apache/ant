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

package org.apache.tools.ant.taskdefs.optional.ejb;

// Standard java imports
import java.io.*;
import java.util.*;

// XML imports
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

// Apache/Ant imports
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;

/**
 * <p>Provides automated ejb jar file creation for ant.  Extends the MatchingTask
 * class provided in the default ant distribution to provide a directory scanning
 * EJB jarfile generator.</p>
 *
 * <p>The task works by taking the deployment descriptors one at a time and
 * parsing them to locate the names of the classes which should be placed in
 * the jar.  The classnames are translated to java.io.Files by replacing periods
 * with File.separatorChar and resolving the generated filename as a relative
 * path under the srcdir attribute.  All necessary files are then assembled into
 * a jarfile.  One jarfile is constructed for each deployment descriptor found.
 * </p>
 *
 * <p>Functionality is currently provided for standard EJB1.1 jars and Weblogic
 * 5.1 jars. The weblogic deployment descriptors, used in constructing the 
 * Weblogic jar, are located based on a simple naming convention. The name of the
 * standard deployment descriptor is taken upto the first instance of a String,
 * specified by the attribute basenameTerminator, and then the regular Weblogic
 * descriptor name is appended. For example if basenameTerminator is set to '-',
 * its default value, and a standard descriptor is called Foo-ejb-jar.xml then
 * the files Foo-weblogic-ejb-jar.xml and Foo-weblogic-cmp-rdbms-jar.xml will be
 * looked for, and if found, included in the jarfile.</p>
 *
 * <p>Attributes and setter methods are provided to support optional generation
 * of Weblogic5.1 jars, optional deletion of generic jar files, setting alternate
 * values for basenameTerminator, and setting the strings to append to the names
 * of the generated jarfiles.</p>
 *
 * @author <a href="mailto:tfennell@sapient.com">Tim Fennell</a>
 */
public class EjbJar extends MatchingTask {

    /** Stores a handle to the directory under which to search for files */
    private File srcdir = null;

    /** Stores a handle to the directory to put the Jar files in */
    private File destdir = null;

    /**
     * Instance variable that determines whether to use a package structure
     * of a flat directory as the destination for the jar files.
     */
    private boolean flatdestdir = false;
    
    /** Instance variable that marks the end of the 'basename' */
    private String basenameTerminator = "-";

    /** Instance variable that stores the suffix for the generated jarfile. */
    private String genericjarsuffix = "-generic.jar";

    /**
     * The list of deployment tools we are going to run.
     */
    private ArrayList deploymentTools = new ArrayList();

    public EJBDeploymentTool createWeblogic() {
        EJBDeploymentTool tool = new WeblogicDeploymentTool();
        tool.setTask(this);
        deploymentTools.add(tool);
        return tool;
    }

    /**
     * Setter used to store the value of srcdir prior to execute() being called.
     * @param inDir the source directory.
     */
    public void setSrcdir(File inDir) {
        this.srcdir = inDir;
    }

    /**
     * Setter used to store the value of destination directory prior to execute()
     * being called.
     * @param inFile the destination directory.
     */
    public void setDestdir(File inDir) {
        this.destdir = inDir;
    }

    /**
     * Setter used to store the value of flatdestdir.
     * @param inValue a string, either 'true' or 'false'.
     */
    public void setFlatdestdir(boolean inValue) {
        this.flatdestdir = inValue;
    }
     
    /**
     * Setter used to store the suffix for the generated jar file.
     * @param inString the string to use as the suffix.
     */
    public void setGenericjarsuffix(String inString) {
        this.genericjarsuffix = inString;
    }

    /**
     * Setter used to store the value of basenameTerminator
     * @param inValue a string which marks the end of the basename.
     */
    public void setBasenameTerminator(String inValue) {
        if (inValue != null) this.basenameTerminator = inValue;
    }

    /**
     * Invoked by Ant after the task is prepared, when it is ready to execute
     * this task.  Parses the XML deployment descriptor to acquire the list of
     * files, then constructs the destination jar file (first deleting it if it
     * already exists) from the list of classfiles encountered and the descriptor
     * itself.  File will be of the expected format with classes under full
     * package hierarchies and the descriptor in META-INF/ejb-jar.xml
     * @exception BuildException thrown whenever a problem is
     *            encountered that cannot be recovered from, to signal to ant
     *            that a major problem occurred within this task.
     */
    public void execute() throws BuildException {
        if (srcdir == null) {
            throw new BuildException("The srcdir attribute must be specified");
        }
        
        if (deploymentTools.size() == 0) {
            GenericDeploymentTool genericTool = new GenericDeploymentTool();
            genericTool.setDestdir(destdir);
            genericTool.setTask(this);
            genericTool.setGenericjarsuffix(genericjarsuffix);

            deploymentTools.add(genericTool);
        }
        
        for (Iterator i = deploymentTools.iterator(); i.hasNext(); ) {
            EJBDeploymentTool tool = (EJBDeploymentTool)i.next();
            tool.configure(basenameTerminator, flatdestdir);
            tool.validateConfigured();
        }
        
        try {
            // Create the parser using whatever parser the system dictates
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setValidating(true);
            SAXParser saxParser = saxParserFactory.newSAXParser();
    
            DirectoryScanner ds = getDirectoryScanner(srcdir);
            ds.scan();
            String[] files = ds.getIncludedFiles();
    
            log(files.length + " deployment descriptors located.",
                Project.MSG_VERBOSE);
            
                            
            // Loop through the files. Each file represents one deployment
            // descriptor, and hence one bean in our model.
            for (int index = 0; index < files.length; ++index) {
                // process the deployment descriptor in each tool
                for (Iterator i = deploymentTools.iterator(); i.hasNext(); ) {
                    EJBDeploymentTool tool = (EJBDeploymentTool)i.next();
                    processDescriptor(files[index], saxParser, tool);
                }
            }    
        }
        catch (SAXException se) {
            String msg = "SAXException while creating parser."
                + "  Details: "
                + se.getMessage();
            throw new BuildException(msg, se);
        }
        catch (ParserConfigurationException pce) {
            String msg = "ParserConfigurationException while creating parser. "
                       + "Details: " + pce.getMessage();
            throw new BuildException(msg, pce);
        }
    } // end of execute()

    
    private void processDescriptor(String descriptorFilename, SAXParser saxParser,
                                   EJBDeploymentTool tool) {

        tool.processDescriptor(srcdir, descriptorFilename, saxParser);
    }
}







