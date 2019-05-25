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
import java.util.Vector;
import java.util.stream.Collectors;

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

    /**
     * Logs the compilation parameters, adds the files to compile and logs the
     * &quot;niceSourceList&quot;
     * @param jspc the compiler task for logging
     * @param compileList the list of files to compile
     * @param cmd the command line used
     */
    protected void logAndAddFilesToCompile(JspC jspc,
                                           Vector<String> compileList,
                                           CommandlineJava cmd) {
        jspc.log("Compilation " + cmd.describeJavaCommand(),
                 Project.MSG_VERBOSE);

        String niceSourceList = compileList.stream()
                        .peek(arg -> cmd.createArgument().setValue(arg))
                        .map(arg -> String.format("    %s%n", arg))
                        .collect(Collectors.joining(""));
        jspc.log(String.format("File%s to be compiled:%n%s",
                compileList.size() == 1 ? "" : "s", niceSourceList), Project.MSG_VERBOSE);
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
    @Override
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
     * add a single argument to the argument list, if the value isn't null
     * @param cmd the command line
     * @param  argument  The argument
     */
    protected void addArg(CommandlineJava cmd, String argument) {
        if (argument != null && !argument.isEmpty()) {
           cmd.createArgument().setValue(argument);
        }
    }


    /**
     *  add an argument tuple to the argument list, if the value isn't null
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
     *  add an argument tuple to the arg list, if the file parameter isn't null
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
    @Override
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
