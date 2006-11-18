/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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
import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.jsp.JspC;
import org.apache.tools.ant.types.CommandlineJava;

/**
 * This is the default implementation for the JspCompilerAdapter interface.
 * This is currently very light on the ground since only one compiler type is
 * supported.
 *
 */
public abstract class DefaultJspCompilerAdapter
    implements JspCompilerAdapter {

    private static String lSep = System.getProperty("line.separator");

    /**
     * Logs the compilation parameters, adds the files to compile and logs the
     * &quot;niceSourceList&quot;
     * @param jspc the compiler task for logging
     * @param compileList the list of files to compile
     * @param cmd the command line used
     */
    protected void logAndAddFilesToCompile(JspC jspc,
                                           Vector compileList,
                                           CommandlineJava cmd) {
        jspc.log("Compilation " + cmd.describeJavaCommand(),
                 Project.MSG_VERBOSE);

        StringBuffer niceSourceList = new StringBuffer("File");
        if (compileList.size() != 1) {
            niceSourceList.append("s");
        }
        niceSourceList.append(" to be compiled:");

        niceSourceList.append(lSep);

        Enumeration e = compileList.elements();
        while (e.hasMoreElements()) {
            String arg = (String) e.nextElement();
            cmd.createArgument().setValue(arg);
            niceSourceList.append("    ");
            niceSourceList.append(arg);
            niceSourceList.append(lSep);
        }

        jspc.log(niceSourceList.toString(), Project.MSG_VERBOSE);
    }

    // CheckStyle:VisibilityModifier OFF - bc

    /**
     * our owner
     */
    protected JspC owner;

    // CheckStyle:VisibilityModifier ON

    /**
     * set the owner
     * @param owner the owner JspC compiler
     */
    public void setJspc(JspC owner) {
        this.owner = owner;
    }

    /** get the owner
     * @return the owner; should never be null
     */
    public JspC getJspc() {
        return owner;
    }


    /**
     *  add an argument oneple to the argument list, if the value aint null
     * @param cmd the command line
     * @param  argument  The argument
     */
    protected void addArg(CommandlineJava cmd, String argument) {
        if (argument != null && argument.length() != 0) {
           cmd.createArgument().setValue(argument);
        }
    }


    /**
     *  add an argument tuple to the argument list, if the value aint null
     * @param cmd the command line
     * @param  argument  The argument
     * @param  value     the parameter
     */
    protected void addArg(CommandlineJava cmd, String argument, String value) {
        if (value != null) {
            cmd.createArgument().setValue(argument);
            cmd.createArgument().setValue(value);
        }
    }

    /**
     *  add an argument tuple to the arg list, if the file parameter aint null
     * @param cmd the command line
     * @param  argument  The argument
     * @param  file     the parameter
     */
    protected void addArg(CommandlineJava cmd, String argument, File file) {
        if (file != null) {
            cmd.createArgument().setValue(argument);
            cmd.createArgument().setFile(file);
        }
    }

    /**
     * ask if compiler can sort out its own dependencies
     * @return true if the compiler wants to do its own
     * depends
     */
    public boolean implementsOwnDependencyChecking() {
        return false;
    }

    /**
     * get our project
     * @return owner project data
     */
    public Project getProject() {
        return getJspc().getProject();
    }
}

