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

package org.apache.tools.ant.taskdefs.optional;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.util.LoaderUtils;
import org.apache.tools.ant.util.TeeOutputStream;

/**
 *  Invokes the ANTLR Translator generator on a grammar file.
 *
 */
public class ANTLR extends Task {

    private CommandlineJava commandline = new CommandlineJava();

    /** the file to process */
    private File targetFile;

    /** where to output the result */
    private File outputDirectory;

    /** an optional super grammar file */
    private File superGrammar;

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

    /** working directory */
    private File workingdir = null;

    /** captures ANTLR's output */
    private ByteArrayOutputStream bos = new ByteArrayOutputStream();

    /** The debug attribute */
    private boolean debug;


    /** Instance of a utility class to use for file operations. */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /** Constructor for ANTLR task. */
    public ANTLR() {
        commandline.setVm(JavaEnvUtils.getJreExecutable("java"));
        commandline.setClassname("antlr.Tool");
    }

    /**
     * The grammar file to process.
     * @param target the grammar file
     */
    public void setTarget(File target) {
        log("Setting target to: " + target.toString(), Project.MSG_VERBOSE);
        this.targetFile = target;
    }

    /**
     * The directory to write the generated files to.
     * @param outputDirectory the output directory
     */
    public void setOutputdirectory(File outputDirectory) {
        log("Setting output directory to: " + outputDirectory.toString(), Project.MSG_VERBOSE);
        this.outputDirectory = outputDirectory;
    }

    /**
     * Sets an optional super grammar file.
     * Use setGlib(File superGrammar) instead.
     * @param superGrammar the super grammar filename
     * @deprecated  since ant 1.6
     */
    @Deprecated
    public void setGlib(String superGrammar) {
        String sg;
        if (Os.isFamily("dos")) {
            sg = superGrammar.replace('\\', '/');
        } else {
            sg = superGrammar;
        }
        setGlib(FILE_UTILS.resolveFile(getProject().getBaseDir(), sg));
    }

    /**
     * Sets an optional super grammar file
     * @param superGrammar the super grammar file
     * @since ant 1.6
     */
    public void setGlib(File superGrammar) {
        this.superGrammar = superGrammar;
    }

    /**
     * Sets a flag to enable ParseView debugging
     * @param enable a <code>boolean</code> value
     */
    public void setDebug(boolean enable) {
        this.debug = enable;
    }

    /**
     * If true, emit html
     * @param enable a <code>boolean</code> value
     */
    public void setHtml(boolean enable) {
        html = enable;
    }

    /**
     * Sets a flag to emit diagnostic text
     * @param enable a <code>boolean</code> value
     */
    public void setDiagnostic(boolean enable) {
        diagnostic = enable;
    }

    /**
     * If true, enables all tracing.
     * @param enable a <code>boolean</code> value
     */
    public void setTrace(boolean enable) {
        trace = enable;
    }

    /**
     * If true, enables parser tracing.
     * @param enable a <code>boolean</code> value
     */
    public void setTraceParser(boolean enable) {
        traceParser = enable;
    }

    /**
     * If true, enables lexer tracing.
     * @param enable a <code>boolean</code> value
     */
    public void setTraceLexer(boolean enable) {
        traceLexer = enable;
    }

    /**
     * Sets a flag to allow the user to enable tree walker tracing
     * @param enable a <code>boolean</code> value
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
     * @param s a <code>boolean</code> value
     */
    public void setFork(boolean s) {
    }

    /**
     * The working directory of the process
     * @param d the working directory
     */
    public void setDir(File d) {
        this.workingdir = d;
    }

    /**
     * Adds a classpath to be set
     * because a directory might be given for Antlr debug.
     * @return a path to be configured
     */
    public Path createClasspath() {
        return commandline.createClasspath(getProject()).createPath();
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
     * @throws BuildException on error
     */
    @Override
    public void init() throws BuildException {
        addClasspathEntry("/antlr/ANTLRGrammarParseBehavior.class");
    }

    /**
     * Search for the given resource and add the directory or archive
     * that contains it to the classpath.
     *
     * <p>Doesn't work for archives in JDK 1.1 as the URL returned by
     * getResource doesn't contain the name of the archive.</p>
     * @param resource the resource name to search for
     */
    protected void addClasspathEntry(String resource) {
        /*
         * pre Ant 1.6 this method used to call getClass().getResource
         * while Ant 1.6 will call ClassLoader.getResource().
         *
         * The difference is that Class.getResource expects a leading
         * slash for "absolute" resources and will strip it before
         * delegating to ClassLoader.getResource - so we now have to
         * emulate Class's behavior.
         */
        if (resource.startsWith("/")) {
            resource = resource.substring(1);
        } else {
            resource = "org/apache/tools/ant/taskdefs/optional/"
                + resource;
        }

        File f = LoaderUtils.getResourceSource(getClass().getClassLoader(),
                                               resource);
        if (f != null) {
            log("Found " + f.getAbsolutePath(), Project.MSG_DEBUG);
            createClasspath().setLocation(f);
        } else {
            log("Couldn't find " + resource, Project.MSG_VERBOSE);
        }
    }

    /**
     * Execute the task.
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {
        validateAttributes();

        //TODO: use ANTLR to parse the grammar file to do this.
        File generatedFile = getGeneratedFile();
        boolean targetIsOutOfDate =
            targetFile.lastModified() > generatedFile.lastModified();
        boolean superGrammarIsOutOfDate  = superGrammar != null
                && (superGrammar.lastModified() > generatedFile.lastModified());
        if (targetIsOutOfDate || superGrammarIsOutOfDate) {
            if (targetIsOutOfDate) {
                log("Compiling " + targetFile + " as it is newer than "
                    + generatedFile, Project.MSG_VERBOSE);
            } else {
                log("Compiling " + targetFile + " as " + superGrammar
                    + " is newer than " + generatedFile, Project.MSG_VERBOSE);
            }
            populateAttributes();
            commandline.createArgument().setValue(targetFile.toString());

            log(commandline.describeCommand(), Project.MSG_VERBOSE);
            int err = run(commandline.getCommandline());
            if (err != 0) {
                throw new BuildException("ANTLR returned: " + err, getLocation());
            }
            String output = bos.toString();
            if (output.contains("error:")) {
                throw new BuildException("ANTLR signaled an error: "
                                         + output, getLocation());
            }
        } else {
            log("Skipped grammar file. Generated file " + generatedFile
                + " is newer.", Project.MSG_VERBOSE);
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
            commandline.createArgument().setValue(superGrammar.toString());
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
            if (is272()) {
                commandline.createArgument().setValue("-traceTreeParser");
            } else {
                commandline.createArgument().setValue("-traceTreeWalker");
            }
        }
        if (debug) {
            commandline.createArgument().setValue("-debug");
        }
    }

    private void validateAttributes() throws BuildException {
        if (targetFile == null || !targetFile.isFile()) {
            throw new BuildException("Invalid target: " + targetFile);
        }
        // if no output directory is specified, used the target's directory
        if (outputDirectory == null) {
            setOutputdirectory(new File(targetFile.getParent()));
        }
        if (!outputDirectory.isDirectory()) {
            throw new BuildException("Invalid output directory: " + outputDirectory);
        }
    }

    private File getGeneratedFile() throws BuildException {
        String generatedFileName = null;
        try (BufferedReader in =
            new BufferedReader(new FileReader(targetFile))) {
            String line;
            while ((line = in.readLine()) != null) {
                int extendsIndex = line.indexOf(" extends ");
                if (line.startsWith("class ") && extendsIndex > -1) {
                    generatedFileName = line.substring(
                        "class ".length(), extendsIndex).trim();
                    break;
                }
            }
        } catch (Exception e) {
            throw new BuildException("Unable to determine generated class", e);
        }
        if (generatedFileName == null) {
            throw new BuildException("Unable to determine generated class");
        }
        return new File(outputDirectory, generatedFileName
                        + (html ? ".html" : ".java"));
    }

    /** execute in a forked VM */
    private int run(String[] command) throws BuildException {
        PumpStreamHandler psh =
            new PumpStreamHandler(new LogOutputStream(this, Project.MSG_INFO),
                                  new TeeOutputStream(
                                                      new LogOutputStream(this,
                                                                          Project.MSG_WARN),
                                                      bos)
                                  );
        Execute exe = new Execute(psh, null);
        exe.setAntRun(getProject());
        if (workingdir != null) {
            exe.setWorkingDirectory(workingdir);
        }
        exe.setCommandline(command);
        try {
            return exe.execute();
        } catch (IOException e) {
            throw new BuildException(e, getLocation());
        } finally {
            FileUtils.close(bos);
        }
    }

    /**
     * Whether the antlr version is 2.7.2 (or higher).
     *
     * @return true if the version of Antlr present is 2.7.2 or later.
     * @since Ant 1.6
     */
    protected boolean is272() {
        try (AntClassLoader l =
             getProject().createClassLoader(commandline.getClasspath())) {
            l.loadClass("antlr.Version");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
