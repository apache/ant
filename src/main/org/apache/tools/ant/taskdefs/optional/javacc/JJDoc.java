/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.javacc;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
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
 * @author Jene Jasper <a href="mailto:jjasper@abz.nl">jjasper@abz.nl</a>
 * @author thomas.haas@softwired-inc.com
 * @author Michael Saunders 
 *         <a href="mailto:michael@amtec.com">michael@amtec.com</a>
 */
public class JJDoc extends Task {

    // keys to optional attributes
    private static final String OUTPUT_FILE       = "OUTPUT_FILE";
    private static final String TEXT              = "TEXT";
    private static final String ONE_TABLE         = "ONE_TABLE";

    private final Hashtable optionalAttrs = new Hashtable();

    private String outputFile = null;
    private boolean plainText = false;

    private static final String DEFAULT_SUFFIX_HTML = ".html";
    private static final String DEFAULT_SUFFIX_TEXT = ".txt";

    // required attributes
    private File target          = null;
    private File javaccHome      = null;

    private CommandlineJava cmdl = new CommandlineJava();


    /**
     * Sets the TEXT BNF documentation option.
     */
    public void setText(boolean plainText) {
        optionalAttrs.put(TEXT, new Boolean(plainText));
        this.plainText = plainText;
    }

    /**
     * Sets the ONE_TABLE documentation option.
     */
    public void setOnetable(boolean oneTable) {
        optionalAttrs.put(ONE_TABLE, new Boolean(oneTable));
    }

    /**
     * The outputfile to write the generated BNF documentation file to.
     * If not set, the file is written with the same name as
     * the JavaCC grammar file with a suffix .html or .txt.
     */
    public void setOutputfile(String outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * The javacc grammar file to process.
     */
    public void setTarget(File target) {
        this.target = target;
    }

    /**
     * The directory containing the JavaCC distribution.
     */
    public void setJavacchome(File javaccHome) {
        this.javaccHome = javaccHome;
    }

    public JJDoc() {
        cmdl.setVm(JavaEnvUtils.getJreExecutable("java"));
    }

    public void execute() throws BuildException {

        // load command line with optional attributes
        Enumeration iter = optionalAttrs.keys();
        while (iter.hasMoreElements()) {
            String name  = (String) iter.nextElement();
            Object value = optionalAttrs.get(name);
            cmdl.createArgument()
                .setValue("-" + name + ":" + value.toString());
        }

        if (target == null || !target.isFile()) {
            throw new BuildException("Invalid target: " + target);
        }

        if (outputFile != null) {
            cmdl.createArgument() .setValue("-" + OUTPUT_FILE + ":" 
                                            + outputFile.replace('\\', '/'));
        }

        // use the directory containing the target as the output directory
        File javaFile = new File(createOutputFileName(target, outputFile, 
                                                      plainText));

        if (javaFile.exists()
             && target.lastModified() < javaFile.lastModified()) {
            log("Target is already built - skipping (" + target + ")",
                Project.MSG_VERBOSE);
            return;
        }

        cmdl.createArgument().setValue(target.getAbsolutePath());

        cmdl.setClassname(JavaCC.getMainClass(javaccHome, 
                                              JavaCC.TASKDEF_TYPE_JJDOC));

        final Path classpath = cmdl.createClasspath(getProject());
        final File javaccJar = JavaCC.getArchiveFile(javaccHome);
        classpath.createPathElement().setPath(javaccJar.getAbsolutePath());
        classpath.addJavaRuntime();

        final Commandline.Argument arg = cmdl.createVmArgument();
        arg.setValue("-mx140M");
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

    private String createOutputFileName(File target, String optionalOutputFile,
                                        boolean plainText) {
        String suffix = DEFAULT_SUFFIX_HTML;
        String javaccFile = target.getAbsolutePath().replace('\\','/');

        if (plainText) {
            suffix = DEFAULT_SUFFIX_TEXT;
        }

        if ((optionalOutputFile == null) || optionalOutputFile.equals("")) {
            int filePos = javaccFile.lastIndexOf("/");

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
            optionalOutputFile = optionalOutputFile.replace('\\','/');
        }

        return (getProject().getBaseDir() + "/" + optionalOutputFile)
            .replace('\\', '/');
    }
}
