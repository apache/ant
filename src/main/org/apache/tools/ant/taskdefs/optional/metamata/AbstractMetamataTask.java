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
package org.apache.tools.ant.taskdefs.optional.metamata;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.DirectoryScanner;

import java.io.*;
import java.util.*;

/**
 * Somewhat abstract framework to be used for other metama 2.0 tasks.
 * This should include, audit, metrics, cover and mparse.
 *
 * For more information, visit the website at
 * <a href="http://www.metamata.com">www.metamata.com</a>
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public abstract class AbstractMetamataTask extends Task{

    //--------------------------- ATTRIBUTES -----------------------------------

    /**
     * The user classpath to be provided. It matches the -classpath of the
     * command line. The classpath must includes both the <tt>.class</tt> and the
     * <tt>.java</tt> files for accurate audit.
     */
    protected Path classPath = null;

    /** the path to the source file */
    protected Path sourcePath = null;

    /**
     * Metamata home directory. It will be passed as a <tt>metamata.home</tt> property
     * and should normally matches the environment property <tt>META_HOME</tt>
     * set by the Metamata installer.
     */
    protected File metamataHome = null;

    /** the command line used to run MAudit */
    protected CommandlineJava cmdl = new CommandlineJava();

    /** the set of files to be audited */
    protected Vector fileSets = new Vector();

    /** the options file where are stored the command line options */
    protected File optionsFile = null;

    // this is used to keep track of which files were included. It will
    // be set when calling scanFileSets();
    protected Hashtable includedFiles = null;

    public AbstractMetamataTask(){
    }

    /** initialize the task with the classname of the task to run */
    protected AbstractMetamataTask(String className) {
        cmdl.setVm("java");
        cmdl.setClassname(className);
    }

    /** the metamata.home property to run all tasks. */
    public void setMetamatahome(final File metamataHome){
        this.metamataHome = metamataHome;
    }

    /** user classpath */
    public Path createClasspath() {
        if (classPath == null) {
            classPath = new Path(project);
        }
        return classPath;
    }

    /** create the source path for this task */
    public Path createSourcepath(){
        if (sourcePath == null){
            sourcePath = new Path(project);
        }
        return sourcePath;
    }

    /** Creates a nested jvmarg element. */
    public Commandline.Argument createJvmarg() {
        return cmdl.createVmArgument();
    }

    /**  -mx or -Xmx depending on VM version */
    public void setMaxmemory(String max){
        if (Project.getJavaVersion().startsWith("1.1")) {
            createJvmarg().setValue("-mx" + max);
        } else {
            createJvmarg().setValue("-Xmx" + max);
        }
    }


    /** The java files or directory to be audited */
    public void addFileSet(FileSet fs) {
        fileSets.addElement(fs);
    }

    /** execute the command line */
    public void execute() throws BuildException {
        try {
            setUp();
            ExecuteStreamHandler handler = createStreamHandler();
            execute0(handler);
        } finally {
            cleanUp();
        }
    }

    //--------------------- PRIVATE/PROTECTED METHODS --------------------------

    /** check the options and build the command line */
    protected void setUp() throws BuildException {
        checkOptions();

        // set the classpath as the jar file
        File jar = getMetamataJar(metamataHome);
        final Path classPath = cmdl.createClasspath(project);
        classPath.createPathElement().setLocation(jar);

        // set the metamata.home property
        final Commandline.Argument vmArgs = cmdl.createVmArgument();
        vmArgs.setValue("-Dmetamata.home=" + metamataHome.getAbsolutePath() );

        // retrieve all the files we want to scan
        includedFiles = scanFileSets();
        log(includedFiles.size() + " files added for audit", Project.MSG_VERBOSE);

        // write all the options to a temp file and use it ro run the process
        Vector options = getOptions();
        optionsFile = createTmpFile();
        generateOptionsFile(optionsFile, options);
        Commandline.Argument args = cmdl.createArgument();
        args.setLine("-arguments " + optionsFile.getAbsolutePath());
    }

    /**
     * create a stream handler that will be used to get the output since
     * metamata tools do not report with convenient files such as XML.
     */
    protected abstract ExecuteStreamHandler createStreamHandler();


    /** execute the process with a specific handler */
    protected void execute0(ExecuteStreamHandler handler) throws BuildException {
        final Execute process = new Execute(handler);
        log(cmdl.toString(), Project.MSG_VERBOSE);
        process.setCommandline(cmdl.getCommandline());
        try {
            if (process.execute() != 0) {
                throw new BuildException("Metamata task failed.");
            }
        } catch (IOException e){
            throw new BuildException("Failed to launch Metamata task: " + e);
        }
    }

    /** clean up all the mess that we did with temporary objects */
    protected void cleanUp(){
        if (optionsFile != null){
            optionsFile.delete();
            optionsFile = null;
        }
    }

    /** return the location of the jar file used to run */
    protected final File getMetamataJar(File home){
        return new File(new File(home.getAbsolutePath()), "lib/metamata.jar");
    }

    /** validate options set */
    protected void checkOptions() throws BuildException {
        // do some validation first
        if (metamataHome == null || !metamataHome.exists()){
            throw new BuildException("'metamatahome' must point to Metamata home directory.");
        }
        metamataHome = project.resolveFile(metamataHome.getPath());
        File jar = getMetamataJar(metamataHome);
        if (!jar.exists()){
            throw new BuildException( jar + " does not exist. Check your metamata installation.");
        }
    }

    /** return all options of the command line as string elements */
    protected abstract Vector getOptions();


    protected void generateOptionsFile(File tofile, Vector options) throws BuildException {
        FileWriter fw = null;
        try {
            fw = new FileWriter(tofile);
            PrintWriter pw = new PrintWriter(fw);
            final int size = options.size();
            for (int i = 0; i < size; i++){
                pw.println( options.elementAt(i) );
            }
            pw.flush();
        } catch (IOException e){
            throw new BuildException("Error while writing options file " + tofile, e);
        } finally {
            if (fw != null){
                try {
                    fw.close();
                } catch (IOException ignored){}
            }
        }
    }


    protected Hashtable getFileMapping(){
        return includedFiles;
    }
    /**
     * convenient method for JDK 1.1. Will copy all elements from src to dest
     */
    protected static final void addAllVector(Vector dest, Enumeration files){
        while (files.hasMoreElements()) {
            dest.addElement( files.nextElement() );
        }
    }
    
    protected final static File createTmpFile(){
        // must be compatible with JDK 1.1 !!!!
        final long rand = (new Random(System.currentTimeMillis())).nextLong();
        File file = new File("metamata" + rand + ".tmp");
        return file;
    }

    /**
     * @return the list of .java files (as their absolute path) that should
     *         be audited.
     */
    protected Hashtable scanFileSets(){
        Hashtable files = new Hashtable();
        for (int i = 0; i < fileSets.size(); i++){
            FileSet fs = (FileSet) fileSets.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(project);
            ds.scan();
            String[] f = ds.getIncludedFiles();
            log(i + ") Adding " + f.length + " files from directory " + ds.getBasedir(), Project.MSG_VERBOSE);
            for (int j = 0; j < f.length; j++){
                String pathname = f[j];
                if ( pathname.endsWith(".java") ){
                    File file = new File( ds.getBasedir(), pathname);
//                  file = project.resolveFile(file.getAbsolutePath());
                    String classname = pathname.substring(0, pathname.length()-".java".length());
                    classname = classname.replace(File.separatorChar, '.');
                   files.put( file.getAbsolutePath(), classname ); // it's a java file, add it.
                }
            }
        }
        return files;
    }

}
