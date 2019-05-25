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

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.LogLevel;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.LogOutputResource;
import org.apache.tools.ant.types.resources.StringResource;
import org.apache.tools.ant.util.ResourceUtils;

/**
 * Writes a message to the Ant logging facilities.
 *
 * @since Ant 1.1
 *
 * @ant.task category="utility"
 */
public class Echo extends Task {
    // CheckStyle:VisibilityModifier OFF - bc
    protected String message = "";
    protected File file = null;
    protected boolean append = false;
    /** encoding; set to null or empty means 'default' */
    private String encoding = "";
    private boolean force = false;

    // by default, messages are always displayed
    protected int logLevel = Project.MSG_WARN;
    // CheckStyle:VisibilityModifier ON

    private Resource output;

    /**
     * Does the work.
     *
     * @exception BuildException if something goes wrong with the build
     */
    public void execute() throws BuildException {
        try {
            ResourceUtils.copyResource(
                    new StringResource(message.isEmpty() ? System.lineSeparator() : message),
                    output == null ? new LogOutputResource(this, logLevel) : output,
                    null, null, false, false, append, null,
                    encoding.isEmpty() ? null : encoding, getProject(), force);
        } catch (IOException ioe) {
            throw new BuildException(ioe, getLocation());
        }
    }

    /**
     * Message to write.
     *
     * @param msg Sets the value for the message variable.
     */
    public void setMessage(String msg) {
        this.message = msg == null ? "" : msg;
    }

    /**
     * File to write to.
     * @param file the file to write to, if not set, echo to
     *             standard output
     */
    public void setFile(File file) {
        setOutput(new FileResource(getProject(), file));
    }

    /**
     * Resource to write to.
     * @param output the Resource to write to.
     * @since Ant 1.8
     */
    public void setOutput(Resource output) {
        if (this.output != null) {
            throw new BuildException("Cannot set > 1 output target");
        }
        this.output = output;
        FileProvider fp = output.as(FileProvider.class);
        this.file = fp != null ? fp.getFile() : null;
    }

    /**
     * If true, append to existing file.
     * @param append if true, append to existing file, default
     *               is false.
     */
    public void setAppend(boolean append) {
        this.append = append;
    }

    /**
     * Set a multiline message.
     * @param msg the CDATA text to append to the output text
     */
    public void addText(String msg) {
        message += getProject().replaceProperties(msg);
    }

    /**
     * Set the logging level. Level should be one of
     * <ul>
     *  <li>error</li>
     *  <li>warning</li>
     *  <li>info</li>
     *  <li>verbose</li>
     *  <li>debug</li>
     * </ul>
     * <p>The default is &quot;warning&quot; to ensure that messages are
     * displayed by default when using the -quiet command line option.</p>
     * @param echoLevel the logging level
     */
    public void setLevel(EchoLevel echoLevel) {
        logLevel = echoLevel.getLevel();
    }

    /**
     * Declare the encoding to use when outputting to a file;
     * Use "" for the platform's default encoding.
     * @param encoding the character encoding to use.
     * @since 1.7
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Whether read-only destinations will be overwritten.
     *
     * <p>Defaults to false</p>
     *
     * @param f boolean
     * @since Ant 1.8.2
     */
    public void setForce(boolean f) {
        force = f;
    }

    /**
     * The enumerated values for the level attribute.
     */
    public static class EchoLevel extends LogLevel {
    }
}
