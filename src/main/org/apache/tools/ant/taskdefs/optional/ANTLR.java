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

import java.io.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.*;
/**
 * @author Erik Meade, emeade@geekfarm.org
 */
public class ANTLR extends Task {

    private CommandlineJava commandline = new CommandlineJava();
    private File target;
    private File outputDirectory;
    private boolean fork = false;
    private File dir;

    public ANTLR() {
        commandline.setVm("java");
        commandline.setClassname("antlr.Tool");
    }

    public void setTarget(File target) {
        log("Setting target to: " + target.toString(), Project.MSG_VERBOSE);
        this.target = target;
    }

    public void setOutputdirectory(File outputDirectory) {
        log("Setting output directory to: " + outputDirectory.toString(), Project.MSG_VERBOSE);
        this.outputDirectory = outputDirectory;
    }

    public void setFork(boolean s) {
        this.fork = s;
    }

    /**
     * The working directory of the process
     */
    public void setDir(File d) {
        this.dir = d;
    }


    public void execute() throws BuildException {
        validateAttributes();

        //TODO: use ANTLR to parse the grammer file to do this.
        if (target.lastModified() > getGeneratedFile().lastModified()) {
            commandline.createArgument().setValue("-o");
            commandline.createArgument().setValue(outputDirectory.toString());
            commandline.createArgument().setValue(target.toString());

            if (fork) {
                log("Forking " + commandline.toString(), Project.MSG_VERBOSE);
                int err = run(commandline.getCommandline());
                if (err == 1) {
                    throw new BuildException("ANTLR returned: "+err, location);
                }
            }
            else {
                Execute.runCommand(this, commandline.getCommandline());
            }
        }
    }

    private void validateAttributes() throws BuildException{
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
        if (fork && (dir == null || !dir.isDirectory())) {
            throw new BuildException("Invalid working directory: " + dir);
        }
    }

    private File getGeneratedFile() throws BuildException {
        String generatedFileName = null;
        try {
            BufferedReader in = new BufferedReader(new FileReader(target));
            String line;
            while ((line = in.readLine()) != null) {
                int extendsIndex = line.indexOf(" extends ");
                if (line.startsWith("class ") &&  extendsIndex > -1) {
                    generatedFileName = line.substring(6, extendsIndex).trim();
                    break;
                }
            }
            in.close();
        } catch (Exception e) {
            throw new BuildException("Unable to determine generated class");
        }
        if (generatedFileName == null) {
            throw new BuildException("Unable to determine generated class");
        }
        return new File(outputDirectory, generatedFileName + ".java");
    }

    private int run(String[] command) throws BuildException {
        Execute exe = new Execute(new LogStreamHandler(this, Project.MSG_INFO,
                                                       Project.MSG_WARN), null);
        exe.setAntRun(project);
        exe.setWorkingDirectory(dir);
        exe.setCommandline(command);
        try {
            return exe.execute();
        } catch (IOException e) {
            throw new BuildException(e, location);
        }
    }
}
