/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.jsp.compilers;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.taskdefs.optional.jsp.JspC;
import org.apache.tools.ant.taskdefs.*;

/**
 * The implementation of the jasper compiler.
 * This is a cut-and-paste of the original Jspc task.
 *
 * @author Matthew Watson <a href="mailto:mattw@i3sp.com">mattw@i3sp.com</a>
 */
public class JasperC extends DefaultCompilerAdapter
{
    /* ------------------------------------------------------------ */
    public boolean execute()
        throws BuildException
    {
        getJspc().log("Using jasper compiler", Project.MSG_VERBOSE);
        Commandline cmd = setupJasperCommand();

        try {
            // Create an instance of the compiler, redirecting output to
            // the project log
            Java java = (Java)(getJspc().getProject()).createTask("java");
            if (getJspc().getClasspath() != null)
                java.setClasspath(getJspc().getClasspath());
            java.setClassname("org.apache.jasper.JspC");
            String args[] = cmd.getArguments();
            for (int  i =0; i < args.length; i++)
                java.createArg().setValue(args[i]);
            java.setFailonerror(true);
            java.execute();
            return true;
        }
        catch (Exception ex) {
            if (ex instanceof BuildException) {
                throw (BuildException) ex;
            } else {
                throw new BuildException("Error running jsp compiler: ",
                                         ex, getJspc().getLocation());
            }
        }
    }
    /* ------------------------------------------------------------ */
    private Commandline setupJasperCommand() {
        Commandline cmd = new Commandline();
        JspC jspc = getJspc();
        if (jspc.getDestdir() != null) {
            cmd.createArgument().setValue("-d");
            cmd.createArgument().setFile(jspc.getDestdir());
        }
        if (jspc.getPackage() != null){
            cmd.createArgument().setValue("-p");
            cmd.createArgument().setValue(jspc.getPackage());
        }
        if (jspc.getVerbose() != 0) {
            cmd.createArgument().setValue("-v" + jspc.getVerbose());
        }
        if (jspc.isMapped()){
            cmd.createArgument().setValue("-mapped");
        }
        if (jspc.getIeplugin() != null){
            cmd.createArgument().setValue("-ieplugin");
            cmd.createArgument().setValue(jspc.getIeplugin());
        }
        if (jspc.getUriroot() != null){
            cmd.createArgument().setValue("-uriroot");
            cmd.createArgument().setValue(jspc.getUriroot().toString());
        }
        if (jspc.getUribase() != null){
            cmd.createArgument().setValue("-uribase");
            cmd.createArgument().setValue(jspc.getUribase().toString());
        }
        logAndAddFilesToCompile(getJspc(), getJspc().getCompileList(), cmd);
        return cmd;
    }
    /* ------------------------------------------------------------ */        
}
