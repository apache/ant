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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.util.FileUtils;

/**
 * Encapsulates a Jikes compiler, by directly executing an external
 * process.
 *
 * <p><strong>As of Ant 1.2, this class is considered to be dead code
 * by the Ant developers and is unmaintained.  Don't use
 * it.</strong></p>
 *
 * @deprecated since 1.2.
 *             Merged into the class Javac.
 */
@Deprecated
public class Jikes {
    // There have been reports that 300 files could be compiled
    // on a command line so 250 is a conservative approach
    private static final int MAX_FILES_ON_COMMAND_LINE = 250;
    // CheckStyle:VisibilityModifier OFF - bc
    protected JikesOutputParser jop;
    protected String command;
    protected Project project;
    // CheckStyle:VisibilityModifier ON

    /**
     * Constructs a new Jikes object.
     * @param jop      Parser to send jike's output to
     * @param command  name of jikes executable
     * @param project  the current project
     */
    protected Jikes(JikesOutputParser jop, String command, Project project) {
        super();

        System.err.println("As of Ant 1.2 released in October 2000, "
            + "the Jikes class");
        System.err.println("is considered to be dead code by the Ant "
            + "developers and is unmaintained.");
        System.err.println("Don't use it!");

        this.jop = jop;
        this.command = command;
        this.project = project;
    }

    /**
     * Do the compile with the specified arguments.
     * @param args - arguments to pass to process on command line
     */
    protected void compile(String[] args) {
        String[] commandArray = null;
        File tmpFile = null;

        try {
            // Windows has a 32k limit on total arg size, so
            // create a temporary file to store all the arguments
            if (Os.isFamily(Os.FAMILY_WINDOWS) && args.length > MAX_FILES_ON_COMMAND_LINE) {
                tmpFile = FileUtils.getFileUtils().createTempFile(project, "jikes",
                        "tmp", null, false, true);
                try (BufferedWriter out = new BufferedWriter(new FileWriter(tmpFile))) {
                    for (String arg : args) {
                        out.write(arg);
                        out.newLine();
                    }
                    out.flush();
                    commandArray = new String[] {command,
                                               "@" + tmpFile.getAbsolutePath()};
                } catch (IOException e) {
                    throw new BuildException("Error creating temporary file", e);
                }
            } else {
                commandArray = new String[args.length + 1];
                commandArray[0] = command;
                System.arraycopy(args, 0, commandArray, 1, args.length);
            }

            // We assume, that everything jikes writes goes to
            // standard output, not to standard error. The option
            // -Xstdout that is given to Jikes in Javac.doJikesCompile()
            // should guarantee this. At least I hope so. :)
            try {
                Execute exe = new Execute(jop);
                exe.setAntRun(project);
                exe.setWorkingDirectory(project.getBaseDir());
                exe.setCommandline(commandArray);
                exe.execute();
            } catch (IOException e) {
                throw new BuildException("Error running Jikes compiler", e);
            }
        } finally {
            if (tmpFile != null && !tmpFile.delete()) {
                tmpFile.deleteOnExit();
            }
        }
    }
}
