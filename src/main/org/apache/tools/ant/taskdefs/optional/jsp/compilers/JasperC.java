/* 
 * Copyright  2001-2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * @author Matthew Watson <a href="mailto:mattw@i3sp.com">mattw@i3sp.com</a>
 * @author steve loughran
 * @since ant1.5
 */
public class JasperC extends DefaultJspCompilerAdapter {


    /**
     * what produces java classes from .jsp files
     */
    JspMangler mangler;

    public JasperC(JspMangler mangler) {
        this.mangler = mangler;
    }

    /**
     * our execute method
     */
    public boolean execute()
        throws BuildException {
        getJspc().log("Using jasper compiler", Project.MSG_VERBOSE);
        CommandlineJava cmd = setupJasperCommand();


        try {
            // Create an instance of the compiler, redirecting output to
            // the project log
            Java java = (Java) (getProject().createTask("java"));
            if (getJspc().getClasspath() != null) {
                getProject().log("using user supplied classpath: "
                    + getJspc().getClasspath(), Project.MSG_DEBUG);
                java.setClasspath(getJspc().getClasspath()
                                  .concatSystemClasspath("ignore"));
            } else {
                Path classpath = new Path(getProject());
                classpath = classpath.concatSystemClasspath("only");
                getProject().log("using system classpath: " + classpath,
                                 Project.MSG_DEBUG);
                java.setClasspath(classpath);
            }
            java.setDir(getProject().getBaseDir());
            java.setClassname("org.apache.jasper.JspC");
            //this is really irritating; we need a way to set stuff
            String args[] = cmd.getJavaCommand().getArguments();
            for (int i = 0; i < args.length; i++) {
                java.createArg().setValue(args[i]);
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
            } else {
                throw new BuildException("Error running jsp compiler: ",
                                         ex, getJspc().getLocation());
            }
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
        addArg(cmd, "-v" + jspc.getVerbose());
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

    public JspMangler createMangler() {
        return mangler;
    }
}
