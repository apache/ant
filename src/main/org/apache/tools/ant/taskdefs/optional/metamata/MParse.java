/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.metamata;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.JavaEnvUtils;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Vector;
import java.util.Random;

/**
 * Simple Metamata MParse task.
 * Based on the original written by
 * <a href="mailto:thomas.haas@softwired-inc.com">Thomas Haas</a>.
 * 
 * This version was written for Metamata 2.0 available at
 * <a href="http://www.metamata.com">http://www.metamata.com</a>
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 * @todo make a subclass of AbstractMetaMataTask
 */
public class MParse extends Task {

    private Path classpath = null;
    private Path sourcepath = null;
    private File metahome = null;
    private File target = null;
    private boolean verbose = false;
    private boolean debugparser = false;
    private boolean debugscanner = false;    
    private boolean cleanup = false;
    private CommandlineJava cmdl = new CommandlineJava();
    private File optionsFile = null;

    /**
     * The home directory containing the Metamata distribution; required
     */
    public void setMetamatahome(File metamatahome) {
        this.metahome = metamatahome;
    }

    /** The .jj file to process; required. */
    public void setTarget(File target) {
        this.target = target;
    }

    /** create a classpath entry */
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(project);
        }        
        return classpath;
    }

    /** creates a sourcepath entry */
    public Path createSourcepath() {
        if (sourcepath == null) {
            sourcepath = new Path(project);
        }        
        return sourcepath;
    }

    /** set verbose mode; optional, default false */
    public void setVerbose(boolean flag){
        verbose = flag;
    }

    /** set scanner debug mode; optional, default false */
    public void setDebugscanner(boolean flag){
        debugscanner = flag;
    }

    /** set parser debug mode; optional, default false */
    public void setDebugparser(boolean flag){
        debugparser = flag;
    }

    /** Remove the intermediate Sun JavaCC file 
     * ; optional, default false.
     */
    public void setCleanup(boolean value) {
        cleanup = value;
    }

    /**
     * Additional optional parameters to pass to the JVM. 
     */    
    public Commandline.Argument createJvmarg() {
        return cmdl.createVmArgument();
    }

    /**
     * Set the maximum memory for the JVM; optional.
     * -mx or -Xmx depending on VM version 
     */
    public void setMaxmemory(String max){
        cmdl.setMaxmemory(max);
    }

    public MParse() {
        cmdl.setVm(JavaEnvUtils.getJreExecutable("java"));
        cmdl.setClassname("com.metamata.jj.MParse");
    }

   
    /** execute the command line */
    public void execute() throws BuildException {
        try {
            setUp();
            ExecuteStreamHandler handler = createStreamHandler();
            _execute(handler);
        } finally {
            cleanUp();
        }
    }
    
    /** return the default stream handler for this task */
    protected ExecuteStreamHandler createStreamHandler(){
        return new LogStreamHandler(this, Project.MSG_INFO, Project.MSG_INFO);
    }
    
    /**
     * check the options and build the command line
     */
    protected void setUp() throws BuildException {
        checkOptions();

        // set the classpath as the jar files
        File[] jars = getMetamataLibs();
        final Path classPath = cmdl.createClasspath(project);
        for (int i = 0; i < jars.length; i++){
            classPath.createPathElement().setLocation(jars[i]);
        }

        // set the metamata.home property
        final Commandline.Argument vmArgs = cmdl.createVmArgument();
        vmArgs.setValue("-Dmetamata.home=" + metahome.getAbsolutePath());


        // write all the options to a temp file and use it ro run the process
        String[] options = getOptions();
        optionsFile = createTmpFile();
        generateOptionsFile(optionsFile, options);
        Commandline.Argument args = cmdl.createArgument();
        args.setLine("-arguments " + optionsFile.getAbsolutePath());
    }    

    
    /** execute the process with a specific handler */
    protected void _execute(ExecuteStreamHandler handler) throws BuildException {
        // target has been checked as a .jj, see if there is a matching
        // java file and if it is needed to run to process the grammar
        String pathname = target.getAbsolutePath();
        int pos = pathname.length() - ".jj".length();
        pathname = pathname.substring(0, pos) + ".java";
        File javaFile = new File(pathname);
        if (javaFile.exists() && target.lastModified() < javaFile.lastModified()) {
            project.log("Target is already build - skipping (" + target + ")");
            return;
        }
        
        final Execute process = new Execute(handler);
        log(cmdl.describeCommand(), Project.MSG_VERBOSE);
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
        if (cleanup) {
            String name = target.getName();
            int pos = name.length() - ".jj".length();
            name = "__jj" + name.substring(0, pos) + ".sunjj";
            final File sunjj = new File(target.getParent(), name);
            if (sunjj.exists()) {
                project.log("Removing stale file: " + sunjj.getName());
                sunjj.delete();
            }
        }
    }
    
    /**
     * return an array of files containing the path to the needed
     * libraries to run metamata. The file are not checked for
     * existence. You should do this yourself if needed or simply let the
     * forked process do it for you.
     * @return array of jars/zips needed to run metamata.
     */
    protected File[] getMetamataLibs(){
        Vector files = new Vector();
        files.addElement(new File(metahome, "lib/metamata.jar"));
        files.addElement(new File(metahome, "bin/lib/JavaCC.zip"));
        
        File[] array = new File[ files.size() ];
        files.copyInto(array);
        return array;
    }
    
    
    /**
     * validate options set and resolve files and paths
     * @throws BuildException thrown if an option has an incorrect state.
     */
    protected void checkOptions() throws BuildException {       
        // check that the home is ok.
        if (metahome == null || !metahome.exists()){
            throw new BuildException("'metamatahome' must point to Metamata home directory.");
        }
        metahome = project.resolveFile(metahome.getPath());
        
        // check that the needed jar exists.
        File[] jars = getMetamataLibs();
        for (int i = 0; i < jars.length; i++){
            if (!jars[i].exists()){
                throw new BuildException(jars[i] 
                    + " does not exist. Check your metamata installation.");
            }           
        }
        
        // check that the target is ok and resolve it.
        if (target == null || !target.isFile() 
            || !target.getName().endsWith(".jj")) {
            throw new BuildException("Invalid target: " + target);
        }
        target = project.resolveFile(target.getPath());
    }    
    
    /**
     * return all options of the command line as string elements
     * @return an array of options corresponding to the setted options.
     */
    protected String[] getOptions(){
        Vector options = new Vector();
        if (verbose){
            options.addElement("-verbose");
        }
        if (debugscanner){
            options.addElement("-ds");
        }
        if (debugparser){
            options.addElement("-dp");
        }
        if (classpath != null){
            options.addElement("-classpath");
            options.addElement(classpath.toString());
        }
        if (sourcepath != null){
            options.addElement("-sourcepath");
            options.addElement(sourcepath.toString());
        }
        options.addElement(target.getAbsolutePath());
        
        String[] array = new String[options.size()];
        options.copyInto(array);
        return array;
    }
    
    /**
     * write all options to a file with one option / line
     * @param tofile the file to write the options to.
     * @param options the array of options element to write to the file.
     * @throws BuildException thrown if there is a problem while writing
     * to the file.
     */
    protected void generateOptionsFile(File tofile, String[] options) throws BuildException {
        FileWriter fw = null;
        try {
            fw = new FileWriter(tofile);
            PrintWriter pw = new PrintWriter(fw);
            for (int i = 0; i < options.length; i++){
                pw.println(options[i]);
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
    
    /**
     * create a temporary file in the current directory
     * @todo move to using the FileUtils implementation
     */
    protected static final File createTmpFile(){
        // must be compatible with JDK 1.1 !!!!
        final long rand = (new Random(System.currentTimeMillis())).nextLong();
        File file = new File("metamata" + rand + ".tmp");
        return file;
    }

}
