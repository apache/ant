/*
 * Copyright  2001-2004 The Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.JavaEnvUtils;

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
    private final void setCaller(AggregateTransformer caller) {
        this.caller = caller;
    }

    /** get the appropriate stream based on the format (frames/noframes) */
    protected OutputStream getOutputStream() throws IOException {
        if (AggregateTransformer.FRAMES.equals(caller.format)) {
            // dummy output for the framed report
            // it's all done by extension...
            return new ByteArrayOutputStream();
        } else {
            return new BufferedOutputStream(new FileOutputStream(new File(caller.toDir, "junit-noframes.html")));
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
        } catch (Exception xalan2missing) {
            StringWriter swr = new StringWriter();
            xalan2missing.printStackTrace(new PrintWriter(swr));
            caller.task.log("Didn't find Xalan2.", Project.MSG_DEBUG);
            caller.task.log(swr.toString(), Project.MSG_DEBUG);
            try {
                procVersion = Class.forName("org.apache.xalan.xslt.XSLProcessorVersion");
                executor = (XalanExecutor) Class.forName(
                    "org.apache.tools.ant.taskdefs.optional.junit.Xalan1Executor").newInstance();
            } catch (Exception xalan1missing) {
                swr = new StringWriter();
                xalan1missing.printStackTrace(new PrintWriter(swr));
                caller.task.log("Didn't find Xalan1.", Project.MSG_DEBUG);
                caller.task.log(swr.toString(), Project.MSG_DEBUG);
                String msg = "Could not find xalan2 nor xalan1 "
                    + "in the classpath. Check http://xml.apache.org/xalan-j/";
                if (!JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)
                    && !JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_2)
                    && !JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_3)
                    && !JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_4)) {
                    msg += "\r\nStarting with JDK 1.5, the built-in processor "
                        + "of the JDK is no longer Xalan\r\nbut XSLTC which is "
                        + "not (yet) supported by the junitreport task.";
                }
                throw new BuildException(msg);
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
        } catch (Exception e) {
            return "?";
        }
    }
}
