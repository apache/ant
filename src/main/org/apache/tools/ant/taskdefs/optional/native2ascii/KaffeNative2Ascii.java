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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.ExecuteJava;
import org.apache.tools.ant.taskdefs.optional.Native2Ascii;
import org.apache.tools.ant.types.Commandline;

/**
 * Adapter to kaffe.tools.native2ascii.Native2Ascii.
 *
 * @since Ant 1.6.3
 */
public final class KaffeNative2Ascii extends DefaultNative2Ascii {

    // sorted by newest Kaffe version first
    private static final String[] N2A_CLASSNAMES = new String[] {
        "gnu.classpath.tools.native2ascii.Native2ASCII",
        // pre Kaffe 1.1.5
        "kaffe.tools.native2ascii.Native2Ascii",
    };

    /**
     * Identifies this adapter.
     */
    public static final String IMPLEMENTATION_NAME = "kaffe";

    /** {@inheritDoc} */
    @Override
    protected void setup(Commandline cmd, Native2Ascii args)
        throws BuildException {
        if (args.getReverse()) {
            throw new BuildException("-reverse is not supported by Kaffe");
        }
        super.setup(cmd, args);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean run(Commandline cmd, ProjectComponent log)
        throws BuildException {
        ExecuteJava ej = new ExecuteJava();
        Class<?> c = getN2aClass();
        if (c == null) {
            throw new BuildException(
                "Couldn't load Kaffe's Native2Ascii class");
        }

        cmd.setExecutable(c.getName());
        ej.setJavaCommand(cmd);
        ej.execute(log.getProject());
        // otherwise ExecuteJava has thrown an exception
        return true;
    }

    /**
     * tries to load Kaffe Native2Ascii and falls back to the older
     * class name if necessary.
     *
     * @return null if neither class can get loaded.
     */
    private static Class<?> getN2aClass() {
        for (String className : N2A_CLASSNAMES) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException cnfe) {
                // Ignore
            }
        }
        return null;
    }

}
