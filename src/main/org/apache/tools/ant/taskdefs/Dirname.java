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

package org.apache.tools.ant.taskdefs;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Determines the directory name of the specified file.
 *
 * This task can accept the following attributes:
 * <ul>
 * <li>file
 * <li>property
 * </ul>
 * Both <b>file</b> and <b>property</b> are required.
 * <p>
 * When this task executes, it will set the specified property to the
 * value of the specified file up to, but not including, the last path
 * element. If file is a file, the directory will be the current
 * directory.
 *
 * @author Diane Holt <a href="mailto:holtdl@apache.org">holtdl@apache.org</a>
 *
 * @version $Revision$
 *
 * @since Ant 1.5
 *
 * @ant.task category="property"
 */

public class Dirname extends Task {
    private File file;
    private String property;

    /**
     * Path to take the dirname of.
     * @param file
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * The name of the property to set.
     * @param property
     */
    public void setProperty(String property) {
        this.property = property;
    }


    // The method executing the task
    public void execute() throws BuildException {
        if (property == null) {
            throw new BuildException("property attribute required", getLocation());
        }
        if (file == null) {
            throw new BuildException("file attribute required", getLocation());
        } else {
            String value = file.getParent();
            getProject().setNewProperty(property, value);
        }
    }
}

