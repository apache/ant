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

package org.apache.tools.ant.helper;

import java.util.Hashtable;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Executor;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;

/**
 * Target executor implementation that ignores dependencies. Runs each
 * target by calling <code>target.performTasks()</code> directly. If an
 * error occurs, behavior is determined by the Project's "keep-going" mode.
 * To be used when you know what you're doing.
 *
 * @since Ant 1.7.1
 */
public class IgnoreDependenciesExecutor implements Executor {

    private static final SingleCheckExecutor SUB_EXECUTOR = new SingleCheckExecutor();

    /** {@inheritDoc}. */
    public void executeTargets(Project project, String[] targetNames)
        throws BuildException {
        Hashtable<String, Target> targets = project.getTargets();
        BuildException thrownException = null;
        for (String targetName : targetNames) {
            try {
                Target t = targets.get(targetName);
                if (t == null) {
                    throw new BuildException("Unknown target " + targetName);
                }
                t.performTasks();
            } catch (BuildException ex) {
                if (project.isKeepGoingMode()) {
                    thrownException = ex;
                } else {
                    throw ex;
                }
            }
        }
        if (thrownException != null) {
            throw thrownException;
        }
    }

    /** {@inheritDoc}. */
    public Executor getSubProjectExecutor() {
        return SUB_EXECUTOR;
    }

}
