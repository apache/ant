/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.junit;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.io.PrintWriter;

import java.lang.reflect.Field;

/**
 * Command class that encapsulate specific behavior for each
 * Xalan version. The right executor will be instantiated at
 * runtime via class lookup. For instance, it will check first
 * for Xalan2, then for Xalan1.
 */
abstract class XalanExecutor {
    /** the transformer caller */
    protected AggregateTransformer caller;

    /** set the caller for this object. */
    private final void setCaller(AggregateTransformer caller){
        this.caller = caller;
    }

    /** get the appropriate stream based on the format (frames/noframes) */
    protected OutputStream getOutputStream() throws IOException {
        if (caller.FRAMES.equals(caller.format)){
            // dummy output for the framed report
            // it's all done by extension...
            return new ByteArrayOutputStream();
        } else {
            return new FileOutputStream(new File(caller.toDir, "junit-noframes.html"));
        }
    }

    /** override to perform transformation */
    abstract void execute() throws Exception;

    /**
     * Create a valid Xalan executor. It checks first if Xalan2 is
     * present, if not it checks for xalan1. If none is available, it
     * fails.
     * @param caller object containing the transformation information.
     * @throws BuildException thrown if it could not find a valid xalan
     * executor.
     */
    static XalanExecutor newInstance(AggregateTransformer caller) throws BuildException {
        Class procVersion = null;
        XalanExecutor executor = null;
        try {
            procVersion = Class.forName("org.apache.xalan.processor.XSLProcessorVersion");
            executor = (XalanExecutor) Class.forName(
                "org.apache.tools.ant.taskdefs.optional.junit.Xalan2Executor").newInstance();
        } catch (Exception xalan2missing){
            StringWriter swr = new StringWriter();
            xalan2missing.printStackTrace(new PrintWriter(swr));
            caller.task.log("Didn't find Xalan2.", Project.MSG_DEBUG);
            caller.task.log(swr.toString(), Project.MSG_DEBUG);
            try {
                procVersion = Class.forName("org.apache.xalan.xslt.XSLProcessorVersion");
                executor = (XalanExecutor) Class.forName(
                    "org.apache.tools.ant.taskdefs.optional.junit.Xalan1Executor").newInstance();
            } catch (Exception xalan1missing){
                swr = new StringWriter();
                xalan1missing.printStackTrace(new PrintWriter(swr));
                caller.task.log("Didn't find Xalan1.", Project.MSG_DEBUG);
                caller.task.log(swr.toString(), Project.MSG_DEBUG);
                throw new BuildException("Could not find xalan2 nor xalan1 in the classpath. Check http://xml.apache.org/xalan-j");
            }
        }
        String version = getXalanVersion(procVersion);
        caller.task.log("Using Xalan version: " + version);
        executor.setCaller(caller);
        return executor;
    }

    /** pretty useful data (Xalan version information) to display. */
    private static String getXalanVersion(Class procVersion) {
        try {
            Field f = procVersion.getField("S_VERSION");
            return f.get(null).toString();
        } catch (Exception e){
            return "?";
        }
    }
}
