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

package org.apache.tools.ant.taskdefs.optional.javacc;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

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
 * Runs the JJDoc compiler compiler.
 *
 */
public class JJDoc extends Task {

    // keys to optional attributes
    private static final String OUTPUT_FILE       = "OUTPUT_FILE";
    private static final String TEXT              = "TEXT";
    private static final String ONE_TABLE         = "ONE_TABLE";

    private final Map<String, Object> optionalAttrs = new Hashtable<>();

    private String outputFile = null;
    private boolean plainText = false;

    private static final String DEFAULT_SUFFIX_HTML = ".html";
    private static final String DEFAULT_SUFFIX_TEXT = ".txt";

    // required attributes
    private File targetFile      = null;
    private File javaccHome      = null;

    private CommandlineJava cmdl = new CommandlineJava();

    private String maxMemory = null;

    /**
     * Sets the TEXT BNF documentation option.
     * @param plainText a <code>boolean</code> value.
     */
    public void setText(boolean plainText) {
        optionalAttrs.put(TEXT, plainText);
        this.plainText = plainText;
    }

    /**
     * Sets the ONE_TABLE documentation option.
     * @param oneTable a <code>boolean</code> value.
     */
    public void setOnetable(boolean oneTable) {
        optionalAttrs.put(ONE_TABLE, oneTable);
    }

    /**
     * The outputfile to write the generated BNF documentation file to.
     * If not set, the file is written with the same name as
     * the JavaCC grammar file with a suffix .html or .txt.
     * @param outputFile the name of the output file.
     */
    public void setOutputfile(String outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * The javacc grammar file to process.
     * @param target the grammar file.
     */
    public void setTarget(File target) {
        this.targetFile = target;
    }

    /**
     * The directory containing the JavaCC distribution.
     * @param javaccHome the home directory.
     */
    public void setJavacchome(File javaccHome) {
        this.javaccHome = javaccHome;
    }

    /**
     * Corresponds -Xmx.
     *
     * @param max max memory parameter.
     * @since Ant 1.8.3
     */
    public void setMaxmemory(String max) {
        maxMemory = max;
    }

    /**
     * Constructor
     */
    public JJDoc() {
        cmdl.setVm(JavaEnvUtils.getJreExecutable("java"));
    }

    /**
     * Do the task.
     * @throws BuildException if there is an error.
     */
    @Override
    public void execute() throws BuildException {

        // load command line with optional attributes
        optionalAttrs.forEach((name, value) -> cmdl.createArgument()
            .setValue("-" + name + ":" + value.toString()));

        if (targetFile == null || !targetFile.isFile()) {
            throw new BuildException("Invalid target: %s", targetFile);
        }

        if (outputFile != null) {
            cmdl.createArgument() .setValue("-" + OUTPUT_FILE + ":"
                                            + outputFile.replace('\\', '/'));
        }

        // use the directory containing the target as the output directory
        File javaFile = new File(createOutputFileName(targetFile, outputFile,
                                                      plainText));

        if (javaFile.exists()
             && targetFile.lastModified() < javaFile.lastModified()) {
            log("Target is already built - skipping (" + targetFile + ")",
                Project.MSG_VERBOSE);
            return;
        }

        cmdl.createArgument().setValue(targetFile.getAbsolutePath());

        final Path classpath = cmdl.createClasspath(getProject());
        final File javaccJar = JavaCC.getArchiveFile(javaccHome);
        classpath.createPathElement().setPath(javaccJar.getAbsolutePath());
        classpath.addJavaRuntime();

        cmdl.setClassname(JavaCC.getMainClass(classpath,
                                              JavaCC.TASKDEF_TYPE_JJDOC));

        cmdl.setMaxmemory(maxMemory);
        final Commandline.Argument arg = cmdl.createVmArgument();
        arg.setValue("-Dinstall.root=" + javaccHome.getAbsolutePath());

        final Execute process =
            new Execute(new LogStreamHandler(this,
                                             Project.MSG_INFO,
                                             Project.MSG_INFO),
                        null);
        log(cmdl.describeCommand(), Project.MSG_VERBOSE);
        process.setCommandline(cmdl.getCommandline());

        try {
            if (process.execute() != 0) {
                throw new BuildException("JJDoc failed.");
            }
        } catch (IOException e) {
            throw new BuildException("Failed to launch JJDoc", e);
        }
    }

    private String createOutputFileName(File destFile, String optionalOutputFile,
                                        boolean plain) {
        String suffix = DEFAULT_SUFFIX_HTML;
        String javaccFile = destFile.getAbsolutePath().replace('\\', '/');

        if (plain) {
            suffix = DEFAULT_SUFFIX_TEXT;
        }

        if (optionalOutputFile == null || optionalOutputFile.isEmpty()) {
            int filePos = javaccFile.lastIndexOf('/');

            if (filePos >= 0) {
                javaccFile = javaccFile.substring(filePos + 1);
            }

            int suffixPos = javaccFile.lastIndexOf('.');

            if (suffixPos == -1) {
                optionalOutputFile = javaccFile + suffix;
            } else {
                String currentSuffix = javaccFile.substring(suffixPos);

                if (currentSuffix.equals(suffix)) {
                    optionalOutputFile = javaccFile + suffix;
                } else {
                    optionalOutputFile = javaccFile.substring(0, suffixPos)
                        + suffix;
                }
            }
        } else {
            optionalOutputFile = optionalOutputFile.replace('\\', '/');
        }

        return (getProject().getBaseDir() + "/" + optionalOutputFile)
            .replace('\\', '/');
    }
}
