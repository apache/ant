/*
 * Copyright  2005 The Apache Software Foundation
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

    /**
     * Identifies this adapter.
     */
    public static final String IMPLEMENTATION_NAME = "kaffe";

    protected void setup(Commandline cmd, Native2Ascii args)
        throws BuildException {
        if (args.getReverse()) {
            throw new BuildException("-reverse is not supported by Kaffe");
        }
        super.setup(cmd, args);
    }

    protected boolean run(Commandline cmd, ProjectComponent log)
        throws BuildException {
        ExecuteJava ej = new ExecuteJava();
        cmd.setExecutable("kaffe.tools.native2ascii.Native2Ascii");
        ej.setJavaCommand(cmd);
        ej.execute(log.getProject());
        // otherwise ExecuteJava has thrown an exception
        return true;
    }
}