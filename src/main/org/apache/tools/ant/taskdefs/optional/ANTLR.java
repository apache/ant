/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 *  Invokes the ANTLR Translator generator on a grammar file. 
 *
 * @author <a href="mailto:emeade@geekfarm.org">Erik Meade</a>
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 * @author <a href="mailto:aphid@browsecode.org">Stephen Chin</a>
 */
public class ANTLR extends Task {

    private CommandlineJava commandline = new CommandlineJava();

    /** the file to process */
    private File target;

    /** where to output the result */
    private File outputDirectory;

    /** an optional super grammar file */
    private String superGrammar;

    /** optional flag to enable parseView debugging */
    private boolean debug;

    /** optional flag to enable html output */
    private boolean html;

    /** optional flag to print out a diagnostic file */
    private boolean diagnostic;

    /** optional flag to add trace methods */
    private boolean trace;

    /** optional flag to add trace methods to the parser only */
    private boolean traceParser;

    /** optional flag to add trace methods to the lexer only */
    private boolean traceLexer;

    /** optional flag to add trace methods to the tree walker only */
    private boolean traceTreeWalker;

    /** should fork ? */
    private final boolean fork = true;

    /** working directory */
    private File workingdir = null;

    public ANTLR() {
        commandline.setVm(JavaEnvUtils.getJreExecutable("java"));
        commandline.setClassname("antlr.Tool");
    }

    /**
     * The grammar file to process.
     */
    public void setTarget(File target) {
        log("Setting target to: " + target.toString(), Project.MSG_VERBOSE);
        this.target = target;
    }

    /**
     * The directory to write the generated files to.
     */
    public void setOutputdirectory(File outputDirectory) {
        log("Setting output directory to: " + outputDirectory.toString(), Project.MSG_VERBOSE);
        this.outputDirectory = outputDirectory;
    }

    /**
     * Sets an optional super grammar file.
     */
    public void setGlib(String superGrammar) {
        this.superGrammar = superGrammar;
    }

    /**
     * Sets a flag to enable ParseView debugging
     */
    public void setDebug(boolean enable) {
        debug = enable;
    }

    /**
     * If true, emit html
     */
    public void setHtml(boolean enable) {
        html = enable;
    }

    /**
     * Sets a flag to emit diagnostic text
     */
    public void setDiagnostic(boolean enable) {
        diagnostic = enable;
    }

    /**
     * If true, enables all tracing.
     */
    public void setTrace(boolean enable) {
        trace = enable;
    }

    /**
     * If true, enables parser tracing.
     */
    public void setTraceParser(boolean enable) {
        traceParser = enable;
    }

    /**
     * If true, enables lexer tracing.
     */
    public void setTraceLexer(boolean enable) {
        traceLexer = enable;
    }

    /**
     * Sets a flag to allow the user to enable tree walker tracing
     */
    public void setTraceTreeWalker(boolean enable) {
        traceTreeWalker = enable;
    }

    // we are forced to fork ANTLR since there is a call
    // to System.exit() and there is nothing we can do
    // right now to avoid this. :-( (SBa)
    // I'm not removing this method to keep backward compatibility
    /**
     * @ant.attribute ignore="true"
     */
    public void setFork(boolean s) {
        //this.fork = s;
    }

    /**
     * The working directory of the process
     */
    public void setDir(File d) {
        this.workingdir = d;
    }

    /**
     * Adds a classpath to be set
     * because a directory might be given for Antlr debug.
     */
    public Path createClasspath() {
        return commandline.createClasspath(project).createPath();
    }

    /**
     * Adds a new JVM argument.
     * @return  create a new JVM argument so that any argument can be passed to the JVM.
     * @see #setFork(boolean)
     */
    public Commandline.Argument createJvmarg() {
        return commandline.createVmArgument();
    }

    /**
     * Adds the jars or directories containing Antlr
     * this should make the forked JVM work without having to
     * specify it directly.
     */
    public void init() throws BuildException {
        addClasspathEntry("/antlr/Tool.class");
    }

    /**
     * Search for the given resource and add the directory or archive
     * that contains it to the classpath.
     *
     * <p>Doesn't work for archives in JDK 1.1 as the URL returned by
     * getResource doesn't contain the name of the archive.</p>
     */
    protected void addClasspathEntry(String resource) {
        URL url = getClass().getResource(resource);
        if (url != null) {
            String u = url.toString();
            if (u.startsWith("jar:file:")) {
                int pling = u.indexOf("!");
                String jarName = u.substring(9, pling);
                log("Implicitly adding " + jarName + " to classpath",
                        Project.MSG_DEBUG);
                createClasspath().setLocation(new File((new File(jarName)).getAbsolutePath()));
            } else if (u.startsWith("file:")) {
                int tail = u.indexOf(resource);
                String dirName = u.substring(5, tail);
                log("Implicitly adding " + dirName + " to classpath",
                        Project.MSG_DEBUG);
                createClasspath().setLocation(new File((new File(dirName)).getAbsolutePath()));
            } else {
                log("Don\'t know how to handle resource URL " + u,
                        Project.MSG_DEBUG);
            }
        } else {
            log("Couldn\'t find " + resource, Project.MSG_DEBUG);
        }
    }

    public void execute() throws BuildException {
        validateAttributes();
        //TODO: use ANTLR to parse the grammer file to do this.
        if (target.lastModified() > getGeneratedFile().lastModified()) {
            populateAttributes();
            commandline.createArgument().setValue(target.toString());

            log(commandline.describeCommand(), Project.MSG_VERBOSE);
            int err = run(commandline.getCommandline());
            if (err == 1) {
                throw new BuildException("ANTLR returned: " + err, location);
            }
        } else {
            log("Skipped grammar file. Generated file is newer.", Project.MSG_VERBOSE);
        }
    }

    /**
     * A refactored method for populating all the command line arguments based
     * on the user-specified attributes.
     */
    private void populateAttributes() {
        commandline.createArgument().setValue("-o");
        commandline.createArgument().setValue(outputDirectory.toString());
        if (superGrammar != null) {
            commandline.createArgument().setValue("-glib");
            commandline.createArgument().setValue(superGrammar);
        }
        if (html) {
            commandline.createArgument().setValue("-html");
        }
        if (diagnostic) {
            commandline.createArgument().setValue("-diagnostic");
        }
        if (trace) {
            commandline.createArgument().setValue("-trace");
        }
        if (traceParser) {
            commandline.createArgument().setValue("-traceParser");
        }
        if (traceLexer) {
            commandline.createArgument().setValue("-traceLexer");
        }
        if (traceTreeWalker) {
            commandline.createArgument().setValue("-traceTreeWalker");
        }
    }

    private void validateAttributes() throws BuildException {
        if (target == null || !target.isFile()) {
            throw new BuildException("Invalid target: " + target);
        }

        // if no output directory is specified, used the target's directory
        if (outputDirectory == null) {
            String fileName = target.toString();
            setOutputdirectory(new File(target.getParent()));
        }
        if (!outputDirectory.isDirectory()) {
            throw new BuildException("Invalid output directory: " + outputDirectory);
        }
    }

    private File getGeneratedFile() throws BuildException {
        String generatedFileName = null;
        try {
            BufferedReader in = new BufferedReader(new FileReader(target));
            String line;
            while ((line = in.readLine()) != null) {
                int extendsIndex = line.indexOf(" extends ");
                if (line.startsWith("class ") && extendsIndex > -1) {
                    generatedFileName = line.substring(6, extendsIndex).trim();
                    break;
                }
            }
            in.close();
        } catch (Exception e) {
            throw new BuildException("Unable to determine generated class", e);
        }
        if (generatedFileName == null) {
            throw new BuildException("Unable to determine generated class");
        }
        return new File(outputDirectory, generatedFileName + ".java");
    }

    /** execute in a forked VM */
    private int run(String[] command) throws BuildException {
        Execute exe = new Execute(new LogStreamHandler(this, Project.MSG_INFO,
                Project.MSG_WARN), null);
        exe.setAntRun(project);
        if (workingdir != null) {
            exe.setWorkingDirectory(workingdir);
        }
        exe.setCommandline(command);
        try {
            return exe.execute();
        } catch (IOException e) {
            throw new BuildException(e, location);
        }
    }
}
