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

package org.apache.tools.ant.taskdefs.optional.javacc;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.io.IOException;

/*
 *
 * @author thomas.haas@softwired-inc.com
 */
public class JavaCC extends Task {

    private Path userclasspath = null;
    private File metahome = null;
    private File metaworkingdir = null;
    private File target = null;
    private boolean cleanupHack = false;
    private CommandlineJava cmdl = new CommandlineJava();


    public void setMetamatahome(File metamatahome) {
        this.metahome = metamatahome;
    }

    public void setWorkingdir(File workingdir) {
        this.metaworkingdir = workingdir;
    }

    public void setTarget(File target) {
        this.target = target;
    }

    public Path createUserclasspath() {
        if (userclasspath == null) {
            userclasspath = new Path(project);
        }
        
        return userclasspath;
    }


    public void setCleanupHack(boolean value) {
        cleanupHack = value;
    }

    public JavaCC() {
        cmdl.setVm("java");
        cmdl.setClassname("com.metamata.jj.MParse");
    }


    public void execute() throws BuildException {

        if (target == null || !target.isFile()) {
            throw new BuildException("Invalid target: " + target);
        }
        final File javaFile = new File(target.toString().substring(0,
                                                                   target.toString().indexOf(".jj")) + ".java");
        if (javaFile.exists() && target.lastModified() < javaFile.lastModified()) {
            project.log("Target is already build - skipping (" + target + ")");
            return;
        }
        cmdl.createArgument().setValue(target.getAbsolutePath());

        if (metahome == null || !metahome.isDirectory()) {
            throw new BuildException("Metamatahome not valid.");
        }
        if (metaworkingdir == null || !metaworkingdir.isDirectory()) {
            throw new BuildException("Workingdir not set.");
        }
        if (userclasspath == null) {
            throw new BuildException("Userclasspath not set.");
        }

        final Path classpath = cmdl.createClasspath(project);
        classpath.createPathElement().setLocation(metahome.getAbsolutePath() + "/lib/metamatadebug.jar");
        classpath.createPathElement().setLocation(metahome.getAbsolutePath() + "/lib/metamata.jar");
        classpath.createPathElement().setLocation(metahome.getAbsolutePath() + "/lib/JavaCC.zip");

        final Commandline.Argument arg = cmdl.createVmArgument();
        arg.setValue("-mx140M");
        arg.setValue("-Dmwp=" + metaworkingdir.getAbsolutePath());
        arg.setValue("-Dmetamata.home=" + metahome.getAbsolutePath());
        arg.setValue("-Dmetamata.java=java");
        arg.setValue("-Dmetamata.java.options=-mx140M");
        arg.setValue("-Dmetamata.java.options.classpath=-classpath");
        arg.setValue("-Dmetamata.java.compiler=javac");
        arg.setValue("-Dmetamata.java.compiler.options.0=-J-mx64M");
        arg.setValue("-Dmetamata.java.compiler.options.classpath=-classpath");
        arg.setValue("-Dmetamata.language=en");
        arg.setValue("-Dmetamata.country=US");
        arg.setValue("-Dmetamata.classpath=" + userclasspath);

        final Execute process = new Execute(new LogStreamHandler(this,
                                                                 Project.MSG_INFO,
                                                                 Project.MSG_INFO), null);
        log(cmdl.toString(), Project.MSG_VERBOSE);
        process.setCommandline(cmdl.getCommandline());

        try {
            try {
                if (process.execute() != 0) {
                    throw new BuildException("JavaCC failed.");
                }
            } finally {
                if (cleanupHack) {
                    final File oo393 = new File(javaFile.getParent(),
                                                "OO393.class");
                    if (oo393.exists()) {
                        project.log("Removing stale file: " + oo393.getName());
                        oo393.delete();
                    }
                    final File sunjj = new File(javaFile.getParent(),
                                                "__jj" + javaFile.getName().substring(0,
                                                                                      javaFile.getName().indexOf(".java")) + ".sunjj");
                    if (sunjj.exists()) {
                        project.log("Removing stale file: " + sunjj.getName());
                        sunjj.delete();
                    }
                }
            }
        }
        catch (IOException e) {
            throw new BuildException("Failed to launch JavaCC: " + e);
        }
    }

}
