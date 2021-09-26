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

package org.apache.tools.ant.taskdefs.compilers;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Performs a compile using javac externally.
 *
 * @since Ant 1.4
 */
public class JavacExternal extends DefaultCompilerAdapter {

    /**
     * Performs a compile using the Javac externally.
     * @return true if the compilation succeeded
     * @throws BuildException on error
     */
    @Override
    public boolean execute() throws BuildException {
        attributes.log("Using external javac compiler", Project.MSG_VERBOSE);

        Commandline cmd = new Commandline();
        cmd.setExecutable(getJavac().getJavacExecutable());
        if (assumeJava1_3Plus()) {
            setupModernJavacCommandlineSwitches(cmd);
        } else {
            setupJavacCommandlineSwitches(cmd, true);
        }

        int openVmsFirstFileName = assumeJava1_2Plus() ? cmd.size() : -1;

        logAndAddFilesToCompile(cmd);
        //On VMS platform, we need to create a special java options file
        //containing the arguments and classpath for the javac command.
        //The special file is supported by the "-V" switch on the VMS JVM.
        if (Os.isFamily("openvms")) {
            return execOnVMS(cmd, openVmsFirstFileName);
        }

        String[] commandLine = cmd.getCommandline();
        int firstFileName;

        if (assumeJava1_2Plus()) {
            firstFileName = moveArgFileEligibleOptionsToEnd(commandLine);
        } else {
            firstFileName = -1;
        }

        return executeExternalCompile(commandLine, firstFileName,
                true)
                == 0;
    }

    /**
     * Moves all -J and @argfiles arguments to the beginning
     * So that all command line arguments could be written to file, but -J and @argfile
     * As per javac documentation:
     *      you can specify one or more files that contain arguments to the javac command (except -J options)
     * @param commandLine command line to process
     * @return int index of first argument that could be put into argfile
     */
    private int moveArgFileEligibleOptionsToEnd(String[] commandLine) {
        int nonArgFileOptionIdx = 1; // 0 for javac executable
        while(nonArgFileOptionIdx < commandLine.length &&
                !isArgFileEligible(commandLine[nonArgFileOptionIdx])) {
            nonArgFileOptionIdx++;
        }

        for(int i = nonArgFileOptionIdx + 1; i < commandLine.length; i++) {
            if (!isArgFileEligible(commandLine[i])) {
                String option = commandLine[i];
                if (i - nonArgFileOptionIdx >= 0) {
                    System.arraycopy( commandLine, nonArgFileOptionIdx, commandLine, nonArgFileOptionIdx + 1, i - nonArgFileOptionIdx );
                }
                commandLine[nonArgFileOptionIdx] = option;
                nonArgFileOptionIdx++;
            }
        }

        return nonArgFileOptionIdx;
    }

    private static boolean isArgFileEligible(String option) {
        return !(option.startsWith("-J") || option.startsWith("@"));
    }

    /**
     * helper method to execute our command on VMS.
     * @param cmd Commandline
     * @param firstFileName int
     * @return boolean
     */
    private boolean execOnVMS(Commandline cmd, int firstFileName) {
        File vmsFile = null;
        try {
            vmsFile = JavaEnvUtils.createVmsJavaOptionFile(cmd.getArguments());
            String[] commandLine = {cmd.getExecutable(),
                                    "-V",
                                    vmsFile.getPath()};
            return 0 == executeExternalCompile(commandLine,
                            firstFileName,
                            true);

        } catch (IOException e) {
            throw new BuildException(
                "Failed to create a temporary file for \"-V\" switch");
        } finally {
            FileUtils.delete(vmsFile);
        }
    }

}

