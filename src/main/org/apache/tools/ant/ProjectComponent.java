/*
 * Copyright  2001-2004 The Apache Software Foundation
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

package org.apache.tools.ant;

/**
 * Base class for components of a project, including tasks and data types.
 * Provides common facilities.
 *
 */
public abstract class ProjectComponent {

    /**
     * Project object of this component.
     * @deprecated You should not be directly accessing this variable
     *   directly. You should access project object via the getProject()
     *   or setProject() accessor/mutators.
     */
    protected Project project;

    /** Sole constructor. */
    public ProjectComponent() {
    }

    /**
     * Sets the project object of this component. This method is used by
     * Project when a component is added to it so that the component has
     * access to the functions of the project. It should not be used
     * for any other purpose.
     *
     * @param project Project in whose scope this component belongs.
     *                Must not be <code>null</code>.
     */
    public void setProject(Project project) {
        this.project = project;
    }

    /**
     * Returns the project to which this component belongs.
     *
     * @return the components's project.
     */
    public Project getProject() {
        return project;
    }

    /**
     * Logs a message with the default (INFO) priority.
     *
     * @param msg The message to be logged. Should not be <code>null</code>.
     */
    public void log(String msg) {
        log(msg, Project.MSG_INFO);
    }

    /**
     * Logs a message with the given priority.
     *
     * @param msg The message to be logged. Should not be <code>null</code>.
     * @param msgLevel the message priority at which this message is
     *                 to be logged.
     */
    public void log(String msg, int msgLevel) {
        if (project != null) {
            project.log(msg, msgLevel);
        } else {
            // 'reasonable' default, if the component is used without
            // a Project ( for example as a standalone Bean ).
            // Most ant components can be used this way.
            if (msgLevel <= Project.MSG_INFO) {
                System.err.println(msg);
            }
        }
    }
}
