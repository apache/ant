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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.optional.Javah;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Adapter to the native kaffeh compiler.
 *
 * @since Ant 1.6.3
 */
public class Kaffeh implements JavahAdapter {

    /** the name of the javah adapter - kaffeh */
    public static final String IMPLEMENTATION_NAME = "kaffeh";

    /**
     * Performs the actual compilation.
     * @param javah the calling javah task.
     * @return true if the compilation was successful.
     * @throws BuildException if there is an error.
     * @since Ant 1.6.3
     */
    public boolean compile(Javah javah) throws BuildException {
        Commandline cmd = setupKaffehCommand(javah);
        try {
            Execute.runCommand(javah, cmd.getCommandline());
            return true;
        } catch (BuildException e) {
            if (!e.getMessage().contains("failed with return code")) {
                throw e;
            }
        }
        return false;
    }

    private Commandline setupKaffehCommand(Javah javah) {
        Commandline cmd = new Commandline();
        cmd.setExecutable(JavaEnvUtils.getJdkExecutable("kaffeh"));

        if (javah.getDestdir() != null) {
            cmd.createArgument().setValue("-d");
            cmd.createArgument().setFile(javah.getDestdir());
        }

        if (javah.getOutputfile() != null) {
            cmd.createArgument().setValue("-o");
            cmd.createArgument().setFile(javah.getOutputfile());
        }

        Path cp = new Path(javah.getProject());
        if (javah.getBootclasspath() != null) {
            cp.append(javah.getBootclasspath());
        }
        cp = cp.concatSystemBootClasspath("ignore");
        if (javah.getClasspath() != null) {
            cp.append(javah.getClasspath());
        }
        if (cp.size() > 0) {
            cmd.createArgument().setValue("-classpath");
            cmd.createArgument().setPath(cp);
        }

        if (!javah.getOld()) {
            cmd.createArgument().setValue("-jni");
        }

        cmd.addArguments(javah.getCurrentArgs());

        javah.logAndAddFiles(cmd);
        return cmd;
    }

}
