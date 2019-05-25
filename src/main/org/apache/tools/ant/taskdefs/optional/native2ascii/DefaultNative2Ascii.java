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
package org.apache.tools.ant.taskdefs.optional.native2ascii;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.optional.Native2Ascii;
import org.apache.tools.ant.types.Commandline;

/**
 * encapsulates the handling common to different Native2AsciiAdapter
 * implementations.
 *
 * @since Ant 1.6.3
 */
public abstract class DefaultNative2Ascii implements Native2AsciiAdapter {

    /** No-arg constructor. */
    public DefaultNative2Ascii() {
    }

    /**
     * Splits the task into setting up the command line switches
     * @param args the native 2 ascii arguments.
     * @param srcFile the source file.
     * @param destFile the destination file.
     * @return run if the conversion was successful.
     * @throws BuildException if there is a problem.
     * (delegated to {@link #setup setup}), adding the file names
     * (delegated to {@link #addFiles addFiles}) and running the tool
     * (delegated to {@link #run run}).
     */
    @Override
    public final boolean convert(Native2Ascii args, File srcFile,
                                 File destFile) throws BuildException {
        Commandline cmd = new Commandline();
        setup(cmd, args);
        addFiles(cmd, args, srcFile, destFile);
        return run(cmd, args);
    }

    /**
     * Sets up the initial command line.
     *
     * <p>only the -encoding argument and nested arg elements get
     * handled here.</p>
     *
     * @param cmd Command line to add to
     * @param args provides the user-setting and access to Ant's
     * logging system.
     * @throws BuildException if there was a problem.
     */
    protected void setup(Commandline cmd, Native2Ascii args)
        throws BuildException {
        if (args.getEncoding() != null) {
            cmd.createArgument().setValue("-encoding");
            cmd.createArgument().setValue(args.getEncoding());
        }
        cmd.addArguments(args.getCurrentArgs());
    }

    /**
     * Adds source and dest files to the command line.
     *
     * <p>This implementation adds them without any leading
     * qualifiers, source first.</p>
     *
     * @param cmd Command line to add to
     * @param log provides access to Ant's logging system.
     * @param src the source file
     * @param dest the destination file
     * @throws BuildException if there was a problem.
     */
    protected void addFiles(Commandline cmd, ProjectComponent log, File src,
                            File dest) throws BuildException {
        cmd.createArgument().setFile(src);
        cmd.createArgument().setFile(dest);
    }

    /**
     * Executes the command.
     *
     * @param cmd Command line to execute
     * @param log provides access to Ant's logging system.
     * @return whether execution was successful
     * @throws BuildException if there was a problem.
     */
    protected abstract boolean run(Commandline cmd, ProjectComponent log)
        throws BuildException;
}
