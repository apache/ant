/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;

/** 
 *  A bean to use to embed ant in a project. 
 * 
 *  Based on Main.java.
 *
 *  Note: this is the result of refactoring Main. Some methods are not
 *  usefull for embeded use or may have better names ( I used the
 *  option name from Main, for consistency ). I marked them with @experimental.
 *
 * @experimental: the current API is not yet stable. 
 * 
 * @author duncan@x180.com
 * @author Costin Manolache
 * @since ant1.5
 */
public class AntBean extends Task {
 
    /** The default build file name */
    public final static String DEFAULT_BUILD_FILENAME = "build.xml";

    /** Our current message output status. Follows Project.MSG_XXX */
    private int msgOutputLevel = Project.MSG_INFO;

    /** File that we are using for configuration */
    private File buildFile; /** null */
    private String searchForThis=null;

    /** Stream that we are using for logging */
    private PrintStream out = System.out;

    /** Stream that we are using for logging error messages */
    private PrintStream err = System.err;

    /** The build targets */
    private Vector targets = new Vector(5);

    /** Set of properties that can be used by tasks */
    private Properties definedProps = new Properties();

    /** Names of classes to add as listeners to project */
    private Vector listeners = new Vector(5);
    
    /** File names of property files to load on startup */
    private Vector propertyFiles = new Vector(5);

    /**
     * The Ant logger class. There may be only one logger. It will have the
     * right to use the 'out' PrintStream. The class must implements the BuildLogger
     * interface
     */
    private String loggerClassname = null;

    /**
     * Indicates whether output to the log is to be unadorned.
     */
    private boolean emacsMode = false;

    private ClassLoader coreLoader;

    private ProjectHelper helper=null;

    private Project newProject=null;

    private boolean redirectOutput=true;
    
    
    public AntBean() {
    }
    
    // -------------------- Bean properties --------------------
    // extracted from Main's command line processing code

    /** Global verbosity level
     */
    public void setOutputLevel( int level ) {
        msgOutputLevel=level;
    }

    public void setBuildfile( String name ) {
        buildFile = new File(name);
    }

    /** Add a listener class name
     */
    public void addListener( String s ) {
        listeners.addElement(s);
    }

    /** Set the logger class name ( -logger option in command
     *   line ).
     *
     *  @experimental LoggerClassName would be a better name
     */
    public void setLogger( String s ) {
        if (loggerClassname != null) {
            System.out.println("Only one logger class may be specified.");
            return;
        }
        loggerClassname = s;
    }

    /** Emacs mode for the output
     */
    public void setEmacs( boolean b ) {
        emacsMode = b;
    }

    /** The name of the build file to execute, by
     *  searching in the filesystem.
     */
    public void setFind( String s ) {
        if (s==null) {
            searchForThis = s;
        } else {
            searchForThis = DEFAULT_BUILD_FILENAME;
        }
    }

    /** Same as -propertyfile
     */
    public void addPropertyfile( String s ) {
        propertyFiles.addElement(s);
    }

    /** Set the core loader, to be used to execute.
     */
    public void setCoreLoader( ClassLoader coreLoader ) {
        coreLoader=coreLoader;
    }

    /** Add a user-defined property
     */
    public void setUserProperty( String name, String value ) {
        definedProps.put( name, value );
    }


    /** Add a target to be executed
     */
    public void addTarget(String arg ) { 
        targets.addElement(arg);
    }

    /** Log file. It'll redirect the System output and logs to this
     *  file. Supported by -logfile argument in ant - probably
     *  a bad idea if you embed ant in an application.
     *
     *  @experimental - I don't think it's a good idea.
     */
    public void setLogfile( String name ) {
        try {
            File logFile = new File(name);
            out = new PrintStream(new FileOutputStream(logFile));
            err = out;
            System.setOut(out);
            System.setErr(out);
        } catch (IOException ioe) {
            String msg = "Cannot write on the specified log file. " +
                "Make sure the path exists and you have write permissions.";
            System.out.println(msg);
            return;
        }
    }

    /** Redirect the output and set a security manager before
     * executing ant. Defaults to true for backward comptibility,
     * you should set it to false if you embed ant.
     */
    public void setRedirectOutput( boolean b ) {
        redirectOutput=b;
    }
    
    // -------------------- Property getters --------------------
    

    /** Return the build file. If it was not explicitely specified, search
     *   for it in the parent directories
     *
     * <P>Takes the "find" property as a suffix to append to each
     *    parent directory in seach of a build file.  Once the
     *    root of the file-system has been reached an exception
     *    is thrown.
     */
    public File getBuildFile()
        throws BuildException
    {
        // if buildFile was not specified on the command line,
        if (buildFile == null) {
            // but -find then search for it
            if (searchForThis != null) {
                buildFile = findBuildFile(System.getProperty("user.dir"), 
                                          searchForThis);
            } else {
                buildFile = new File(DEFAULT_BUILD_FILENAME);
            }
        }
        getProject().setUserProperty("ant.file" , buildFile.getAbsolutePath() );
        return buildFile;
    }

    /** Return an (initialized) project constructed using the current
     *  settings.
     *  This will not load the build.xml file - you can 'load' the
     *  project object with tasks manually or execute 'standalone'
     *  tasks in the context of the project.
     */
    public Project getProject() {
        if( newProject!=null )
            return newProject;
        loadProperties();
        
        helper=ProjectHelper.getProjectHelper();
        newProject = helper.createProject(coreLoader);
        newProject.setCoreLoader(coreLoader);

        addBuildListeners(newProject);

        newProject.fireBuildStarted();
        
        newProject.init();
        newProject.setUserProperty("ant.version", getAntVersion());
        
        // set user-define properties
        Enumeration e = definedProps.keys();
        while (e.hasMoreElements()) {
            String arg = (String)e.nextElement();
            String value = (String)definedProps.get(arg);
            newProject.setUserProperty(arg, value);
        }
        
        return newProject;
    }

    private static String antVersion = null;

    /** @experimental
     *  Ant version should be combined with the ProjectHelper version and type,
     *  since it'll determine the set of features supported by ant ( at the xml
     *  level ).
     */
    public static synchronized String getAntVersion() throws BuildException {
        if (antVersion == null) {
            try {
                Properties props = new Properties();
                InputStream in =
                    Main.class.getResourceAsStream("/org/apache/tools/ant/version.txt");
                props.load(in);
                in.close();
                
                String lSep = System.getProperty("line.separator");
                StringBuffer msg = new StringBuffer();
                msg.append("Apache Ant version ");
                msg.append(props.getProperty("VERSION"));
                msg.append(" compiled on ");
                msg.append(props.getProperty("DATE"));
                antVersion = msg.toString();
            } catch (IOException ioe) {
                throw new BuildException("Could not load the version information:"
                                         + ioe.getMessage());
            } catch (NullPointerException npe) {
                throw new BuildException("Could not load the version information.");
            }
        }
        return antVersion;
    }


    
    // -------------------- Bean methods -------------------- 
    Throwable error = null;


    /** Clean up allocated resources and finish the processing of the
     *  current Project. 
     */
    public void done() {
        newProject.fireBuildFinished(error);
    }

    
    /**
     * Process an XML file and execute the targets.
     *
     * This method can be called multiple times, eventually after setting different
     * build file and different targets - all executions will happen in the
     * same execution context ( project ). 
     */
    public void processBuildXml() throws BuildException {
        checkBuildFile();
        File buildFile=getBuildFile();
        Project newProject=getProject();

        // first use the ProjectHelper to create the project object
        // from the given build file.
        String noParserMessage = 
            "No JAXP compliant XML parser found. Please visit http://xml.apache.org for a suitable parser";
        try {
            Class.forName("javax.xml.parsers.SAXParserFactory");
            helper.parse(newProject, buildFile);
        } catch (NoClassDefFoundError ncdfe) {
            throw new BuildException(noParserMessage, ncdfe);
        } catch (ClassNotFoundException cnfe) {
            throw new BuildException(noParserMessage, cnfe);
        } catch (NullPointerException npe) {
            throw new BuildException(noParserMessage, npe);
        }
        
        // make sure that we have a target to execute
        if (targets.size() == 0) {
            targets.addElement(newProject.getDefaultTarget());
        }
        
        newProject.executeTargets(targets);
    }

    public void execute() throws BuildException {

        try {
            if( redirectOutput ) {
                pushSystemOut();
            }

            processBuildXml();
        } catch(RuntimeException exc) {
            error = exc;
            throw exc;
        } catch(Error err) {
            error = err;
            throw err;
        } finally {
            done();
            if( redirectOutput )
                popSystemOut();
        }
    }

    // -------------------- Private methods --------------------
    
    private void checkBuildFile() throws BuildException {
        File buildFile=getBuildFile();
        
        // make sure buildfile exists
        if (!buildFile.exists()) {
            System.out.println("Buildfile: " + buildFile + " does not exist!");
            throw new BuildException("Build failed");
        }

        // make sure it's not a directory (this falls into the ultra
        // paranoid lets check everything catagory
        if (buildFile.isDirectory()) {
            System.out.println("What? Buildfile: " + buildFile + " is a dir!");
            throw new BuildException("Build failed");
        }

        // track when we started
        if (msgOutputLevel >= Project.MSG_INFO) {
            System.out.println("Buildfile: " + buildFile);
        }
    }

    private PrintStream oldErr=null;
    private PrintStream oldOut=null;
    private SecurityManager oldsm = null;
    
    private void pushSystemOut() {
        oldErr = System.err;
        oldOut = System.out;
        
        // use a system manager that prevents from System.exit()
        // only in JDK > 1.1
        if ( !Project.JAVA_1_0.equals(Project.getJavaVersion()) &&
             !Project.JAVA_1_1.equals(Project.getJavaVersion()) ){
            oldsm = System.getSecurityManager();
            
            //SecurityManager can not be installed here for backwards 
            //compatability reasons (PD). Needs to be loaded prior to
            //ant class if we are going to implement it.
            //System.setSecurityManager(new NoExitSecurityManager());
        }
        System.setOut(new PrintStream(new DemuxOutputStream(getProject(), false)));
        System.setErr(new PrintStream(new DemuxOutputStream(getProject(), true)));
    }
    
    private void popSystemOut() {
        // put back the original security manager
        //The following will never eval to true. (PD)
        if (oldsm != null){
            System.setSecurityManager(oldsm);
        }

        if( oldOut!=null && oldErr!=null ) {
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
    }
    
    protected void addBuildListeners(Project newProject) {

        // Add the default listener
        newProject.addBuildListener(createLogger());

        for (int i = 0; i < listeners.size(); i++) {
            String className = (String) listeners.elementAt(i);
            try {
                BuildListener listener =
                    (BuildListener) Class.forName(className).newInstance();
                newProject.addBuildListener(listener);
            }
            catch(Throwable exc) {
                throw new BuildException("Unable to instantiate listener " + className, exc);
            }
        }
    }

    /**
     *  Creates the default build logger for sending build events to the ant log.
     */
    protected BuildLogger createLogger() {
        BuildLogger logger = null;
        if (loggerClassname != null) {
            try {
                logger = (BuildLogger)(Class.forName(loggerClassname).newInstance());
            } catch (ClassCastException e) {
                System.err.println("The specified logger class " + loggerClassname +
                                         " does not implement the BuildLogger interface");
                throw new RuntimeException();
            } catch (Exception e) {
                System.err.println("Unable to instantiate specified logger class " +
                                           loggerClassname + " : " + e.getClass().getName());
                throw new RuntimeException();
            }
        }
        else {
            logger = new DefaultLogger();
        }

        logger.setMessageOutputLevel(msgOutputLevel);
        logger.setOutputPrintStream(out);
        logger.setErrorPrintStream(err);
        logger.setEmacsMode(emacsMode);

        return logger;
    }

    /** Load all propertyFiles
     */
    private void loadProperties()
    {
        // Load the property files specified by -propertyfile
        for (int propertyFileIndex=0;
             propertyFileIndex < propertyFiles.size();
             propertyFileIndex++) {
            String filename = (String) propertyFiles.elementAt(propertyFileIndex);
            Properties props = new Properties();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(filename);
                props.load(fis);
            }
            catch (IOException e) {
                System.out.println("Could not load property file "
                   + filename + ": " + e.getMessage());
            } finally {
                if (fis != null){
                    try {
                        fis.close();
                    } catch (IOException e){
                }
              }
            }
            
            // ensure that -D properties take precedence
            Enumeration propertyNames = props.propertyNames();
            while (propertyNames.hasMoreElements()) {
                String name = (String) propertyNames.nextElement();
                if (definedProps.getProperty(name) == null) {
                    definedProps.put(name, props.getProperty(name));
                }
            }
        }
    }

    // -------------------- XXX Move to FileUtil -------------------- 
    
    /**
     * Helper to get the parent file for a given file.
     *
     * <P>Added to simulate File.getParentFile() from JDK 1.2.
     *
     * @param file   File
     * @return       Parent file or null if none
     */
    private File getParentFile(File file) {
        String filename = file.getAbsolutePath();
        file = new File(filename);
        filename = file.getParent();

        if (filename != null && msgOutputLevel >= Project.MSG_VERBOSE) {
            System.out.println("Searching in "+filename);
        }

        return (filename == null) ? null : new File(filename);
    }

    /**
     * Search parent directories for the build file.
     *
     * <P>Takes the given target as a suffix to append to each
     *    parent directory in seach of a build file.  Once the
     *    root of the file-system has been reached an exception
     *    is thrown.
     *
     * @param suffix    Suffix filename to look for in parents.
     * @return          A handle to the build file
     *
     * @exception BuildException    Failed to locate a build file
     */
    private File findBuildFile(String start, String suffix) throws BuildException {
        if (msgOutputLevel >= Project.MSG_INFO) {
            System.out.println("Searching for " + suffix + " ...");
        }

        File parent = new File(new File(start).getAbsolutePath());
        File file = new File(parent, suffix);
        
        // check if the target file exists in the current directory
        while (!file.exists()) {
            // change to parent directory
            parent = getParentFile(parent);
            
            // if parent is null, then we are at the root of the fs,
            // complain that we can't find the build file.
            if (parent == null) {
                throw new BuildException("Could not locate a build file!");
            }
            
            // refresh our file handle
            file = new File(parent, suffix);
        }
        
        return file;
    }

}
