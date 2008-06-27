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

package org.apache.tools.ant.taskdefs.compilers;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Apt;
import org.apache.tools.ant.types.Commandline;

/**
 * The implementation of the apt compiler for JDK 1.5 using an external process
 *
 * @since Ant 1.7
 */
public class AptExternalCompilerAdapter extends DefaultCompilerAdapter {


    /**
     * Get the facade task that fronts this adapter
     *
     * @return task instance
     * @see DefaultCompilerAdapter#getJavac()
     */
    protected Apt getApt() {
        return (Apt) getJavac();
    }

    /**
     * Performs a compile using the Javac externally.
     * @return true  the compilation was successful.
     * @throws BuildException if there is a problem.
     */
    public boolean execute() throws BuildException {
        attributes.log("Using external apt compiler", Project.MSG_VERBOSE);


        // Setup the apt executable
        Apt apt = getApt();
        Commandline cmd = new Commandline();
        cmd.setExecutable(apt.getAptExecutable());
        setupModernJavacCommandlineSwitches(cmd);
        AptCompilerAdapter.setAptCommandlineSwitches(apt, cmd);
        int firstFileName = cmd.size();
        //add the files
        logAndAddFilesToCompile(cmd);

        //run
        return 0 == executeExternalCompile(cmd.getCommandline(),
                firstFileName,
                true);

    }

}

