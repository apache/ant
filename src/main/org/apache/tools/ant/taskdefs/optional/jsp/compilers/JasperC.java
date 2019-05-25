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

package org.apache.tools.ant.taskdefs.optional.jsp.compilers;

import java.io.File;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.optional.jsp.JspC;
import org.apache.tools.ant.taskdefs.optional.jsp.JspMangler;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;

/**
 * The implementation of the jasper compiler.
 * This is a cut-and-paste of the original Jspc task.
 *
 * @since ant1.5
 */
public class JasperC extends DefaultJspCompilerAdapter {

    // CheckStyle:VisibilityModifier OFF - bc

    /**
     * what produces java classes from .jsp files
     */
    JspMangler mangler;

    // CheckStyle:VisibilityModifier ON

    /**
     * Constructor for JasperC.
     * @param mangler a filename converter
     */
    public JasperC(JspMangler mangler) {
        this.mangler = mangler;
    }

    /**
     * Our execute method.
     * @return true if successful
     * @throws BuildException on error
     */
    @Override
    public boolean execute()
        throws BuildException {
        getJspc().log("Using jasper compiler", Project.MSG_VERBOSE);
        CommandlineJava cmd = setupJasperCommand();

        try {
            // Create an instance of the compiler, redirecting output to
            // the project log
            Java java = new Java(owner);
            Path p = getClasspath();
            if (getJspc().getClasspath() != null) {
                getProject().log("using user supplied classpath: " + p,
                                 Project.MSG_DEBUG);
            } else {
                getProject().log("using system classpath: " + p,
                                 Project.MSG_DEBUG);
            }
            java.setClasspath(p);
            java.setDir(getProject().getBaseDir());
            java.setClassname("org.apache.jasper.JspC");
            //this is really irritating; we need a way to set stuff
            for (String arg : cmd.getJavaCommand().getArguments()) {
                java.createArg().setValue(arg);
            }
            java.setFailonerror(getJspc().getFailonerror());
            //we are forking here to be sure that if JspC calls
            //System.exit() it doesn't halt the build
            java.setFork(true);
            java.setTaskName("jasperc");
            java.execute();
            return true;
        } catch (Exception ex) {
            if (ex instanceof BuildException) {
                throw (BuildException) ex;
            }
            throw new BuildException("Error running jsp compiler: ",
                                         ex, getJspc().getLocation());
        } finally {
            getJspc().deleteEmptyJavaFiles();
        }
    }

    /**
     * build up a command line
     * @return a command line for jasper
     */
    private CommandlineJava setupJasperCommand() {
        CommandlineJava cmd = new CommandlineJava();
        JspC jspc = getJspc();
        addArg(cmd, "-d", jspc.getDestdir());
        addArg(cmd, "-p", jspc.getPackage());

        if (!isTomcat5x()) {
            addArg(cmd, "-v" + jspc.getVerbose());
        } else {
            getProject().log("this task doesn't support Tomcat 5.x properly, "
                             + "please use the Tomcat provided jspc task "
                             + "instead");
        }

        addArg(cmd, "-uriroot", jspc.getUriroot());
        addArg(cmd, "-uribase", jspc.getUribase());
        addArg(cmd, "-ieplugin", jspc.getIeplugin());
        addArg(cmd, "-webinc", jspc.getWebinc());
        addArg(cmd, "-webxml", jspc.getWebxml());
        addArg(cmd, "-die9");

        if (jspc.isMapped()) {
            addArg(cmd, "-mapped");
        }
        if (jspc.getWebApp() != null) {
            File dir = jspc.getWebApp().getDirectory();
            addArg(cmd, "-webapp", dir);
        }
        logAndAddFilesToCompile(getJspc(), getJspc().getCompileList(), cmd);
        return cmd;
    }

    /**
     * @return an instance of the mangler this compiler uses
     */
    @Override
    public JspMangler createMangler() {
        return mangler;
    }

    /**
     * @since Ant 1.6.2
     */
    private Path getClasspath() {
        Path p = getJspc().getClasspath();
        if (p == null) {
            p = new Path(getProject());
            return p.concatSystemClasspath("only");
        }
        return p.concatSystemClasspath("ignore");
    }

    /**
     * @since Ant 1.6.2
     */
    private boolean isTomcat5x() {
        try (AntClassLoader l = getProject().createClassLoader(getClasspath())) {
            l.loadClass("org.apache.jasper.tagplugins.jstl.If");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
