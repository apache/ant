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
package org.apache.tools.ant.taskdefs.optional.javah;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.launch.Locator;
import org.apache.tools.ant.taskdefs.ExecuteJava;
import org.apache.tools.ant.taskdefs.optional.Javah;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;


/**
 * Adapter to com.sun.tools.javah.oldjavah.Main or com.sun.tools.javah.Main.
 *
 * @since Ant 1.6.3
 */
public class SunJavah implements JavahAdapter {

    /** the name of the javah adapter - sun */
    public static final String IMPLEMENTATION_NAME = "sun";

    /**
     * Performs the actual compilation.
     * @param javah the calling javah task.
     * @return true if the compilation was successful.
     * @throws BuildException if there is an error.
     * @since Ant 1.6.3
     */
    @Override
    public boolean compile(Javah javah) throws BuildException {
        Commandline cmd = setupJavahCommand(javah);
        ExecuteJava ej = new ExecuteJava();
        Class<?> c;
        try {
            try {
                // first search for the "old" javah class in 1.4.2 tools.jar
                c = Class.forName("com.sun.tools.javah.oldjavah.Main");
            } catch (ClassNotFoundException cnfe) {
                // assume older than 1.4.2 tools.jar
                c = Class.forName("com.sun.tools.javah.Main");
            }
        } catch (ClassNotFoundException ex) {
            throw new BuildException(
                "Can't load javah", ex, javah.getLocation());
        }
        cmd.setExecutable(c.getName());
        ej.setJavaCommand(cmd);
        File f = Locator.getClassSource(c);
        if (f != null) {
            ej.setClasspath(new Path(javah.getProject(), f.getPath()));
        }
        return ej.fork(javah) == 0;
    }

    static Commandline setupJavahCommand(Javah javah) {
        Commandline cmd = new Commandline();

        if (javah.getDestdir() != null) {
            cmd.createArgument().setValue("-d");
            cmd.createArgument().setFile(javah.getDestdir());
        }

        if (javah.getOutputfile() != null) {
            cmd.createArgument().setValue("-o");
            cmd.createArgument().setFile(javah.getOutputfile());
        }

        if (javah.getClasspath() != null) {
            cmd.createArgument().setValue("-classpath");
            cmd.createArgument().setPath(javah.getClasspath());
        }

        if (javah.getVerbose()) {
            cmd.createArgument().setValue("-verbose");
        }
        if (javah.getOld()) {
            cmd.createArgument().setValue("-old");
        }
        if (javah.getForce()) {
            cmd.createArgument().setValue("-force");
        }
        if (javah.getStubs() && !javah.getOld()) {
            throw new BuildException(
                "stubs only available in old mode.", javah.getLocation());
        }

        if (javah.getStubs()) {
            cmd.createArgument().setValue("-stubs");
        }
        Path bcp = new Path(javah.getProject());
        if (javah.getBootclasspath() != null) {
            bcp.append(javah.getBootclasspath());
        }
        bcp = bcp.concatSystemBootClasspath("ignore");
        if (bcp.size() > 0) {
            cmd.createArgument().setValue("-bootclasspath");
            cmd.createArgument().setPath(bcp);
        }

        cmd.addArguments(javah.getCurrentArgs());

        javah.logAndAddFiles(cmd);
        return cmd;
    }

}
