/*
 * Copyright  2002,2004 Apache Software Foundation
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
package org.apache.tools.ant.util;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * A facade that makes logging nicers to use.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public final class TaskLogger {
    /**
     * Task to use to do logging.
     */
    private Task m_task;

    public TaskLogger(final Task task) {
        this.m_task = task;
    }

    public void info(final String message) {
        m_task.log(message, Project.MSG_INFO);
    }

    public void error(final String message) {
        m_task.log(message, Project.MSG_ERR);
    }

    public void warning(final String message) {
        m_task.log(message, Project.MSG_WARN);
    }

    public void verbose(final String message) {
        m_task.log(message, Project.MSG_VERBOSE);
    }

    public void debug(final String message) {
        m_task.log(message, Project.MSG_DEBUG);
    }
}
