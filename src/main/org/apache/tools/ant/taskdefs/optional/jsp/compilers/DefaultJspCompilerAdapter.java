/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
 * @author Matthew Watson <a href="mailto:mattw@i3sp.com">mattw@i3sp.com</a>
 */
public abstract class DefaultJspCompilerAdapter
    implements JspCompilerAdapter {

    private static String lSep = System.getProperty("line.separator");

    /**
     * Logs the compilation parameters, adds the files to compile and logs the 
     * &quot;niceSourceList&quot;
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

        Enumeration enum = compileList.elements();
        while (enum.hasMoreElements()) {
            String arg = (String) enum.nextElement();
            cmd.createArgument().setValue(arg);
            niceSourceList.append("    " + arg + lSep);
        }

        jspc.log(niceSourceList.toString(), Project.MSG_VERBOSE);
    }

    /**
     * our owner
     */
    protected JspC owner;

    /**
     * set the owner
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
     *
     * @param  argument  The argument
     */
    protected void addArg(CommandlineJava cmd, String argument) {
        if (argument != null && argument.length() != 0) {
           cmd.createArgument().setValue(argument);
        }
    }


    /**
     *  add an argument tuple to the argument list, if the value aint null
     *
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
     *
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

