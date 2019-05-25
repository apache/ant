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

package org.apache.tools.ant.taskdefs.rmic;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecuteJava;
import org.apache.tools.ant.types.Commandline;

/**
 * The implementation of the rmic for Kaffe
 *
 * @since Ant 1.4
 */
public class KaffeRmic extends DefaultRmicAdapter {
    // sorted by newest Kaffe version first
    private static final String[] RMIC_CLASSNAMES = new String[] {
        "gnu.classpath.tools.rmi.rmic.RMIC",
        // pre Kaffe 1.1.5
        "gnu.java.rmi.rmic.RMIC",
        // pre Kaffe 1.1.2
        "kaffe.rmi.rmic.RMIC",
    };

    /**
     * the name of this adapter for users to select
     */
    public static final String COMPILER_NAME = "kaffe";

    /**
     * @since Ant 1.10.3
     */
    @Override
    protected boolean areIiopAndIdlSupported() {
        // actually I don't think Kaffe supports either, but we've
        // accepted the flags prior to 1.10.3
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean execute() throws BuildException {
        getRmic().log("Using Kaffe rmic", Project.MSG_VERBOSE);
        Commandline cmd = setupRmicCommand();

        Class<?> c = getRmicClass();
        if (c == null) {
            StringBuilder buf = new StringBuilder(
                "Cannot use Kaffe rmic, as it is not available.  None of ");
            for (String className : RMIC_CLASSNAMES) {
                if (buf.length() > 0) {
                    buf.append(", ");
                }

                buf.append(className);
            }
            buf.append(
                " have been found. A common solution is to set the environment variable JAVA_HOME or CLASSPATH.");
            throw new BuildException(buf.toString(),
                                     getRmic().getLocation());
        }

        cmd.setExecutable(c.getName());
        if (!c.getName().equals(RMIC_CLASSNAMES[RMIC_CLASSNAMES.length - 1])) { //NOSONAR
            // only supported since Kaffe 1.1.2
            cmd.createArgument().setValue("-verbose");
            getRmic().log(Commandline.describeCommand(cmd));
        }
        ExecuteJava ej = new ExecuteJava();
        ej.setJavaCommand(cmd);
        return ej.fork(getRmic()) == 0;
    }

    /**
     * test for kaffe being on the system
     * @return true if kaffe is on the current classpath
     */
    public static boolean isAvailable() {
        return getRmicClass() != null;
    }

    /**
     * tries to load Kaffe RMIC and falls back to the older class name
     * if necessary.
     *
     * @return null if neither class can get loaded.
     */
    private static Class<?> getRmicClass() {
        for (String className : RMIC_CLASSNAMES) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException cnfe) {
                // Ignore
            }
        }
        return null;
    }
}
