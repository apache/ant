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

package org.apache.tools.ant.taskdefs;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Sets a property to the base name of a specified file, optionally minus a
 * suffix.
 *
 * This task can accept the following attributes:
 * <ul>
 * <li>file
 * <li>property
 * <li>suffix
 * </ul>
 * The <b>file</b> and <b>property</b> attributes are required. The
 * <b>suffix</b> attribute can be specified either with or without
 * the &quot;.&quot;, and the result will be the same (ie., the
 * returned file name will be minus the .suffix).
 * <p>
 * When this task executes, it will set the specified property to the
 * value of the last element in the specified file. If file is a
 * directory, the basename will be the last directory element. If file
 * is a full-path filename, the basename will be the simple file name.
 * If a suffix is specified, and the specified file ends in that suffix,
 * the basename will be the simple file name without the suffix.
 *
 *
 * @since Ant 1.5
 *
 * @ant.task category="property"
 */

public class Basename extends Task {
    private File file;
    private String property;
    private String suffix;

    /**
     * file or directory to get base name from
     * @param file file or directory to get base name from
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
    * Property to set base name to.
     * @param property name of property
    */
    public void setProperty(String property) {
        this.property  = property;
    }

    /**
    * Optional suffix to remove from base name.
     * @param suffix suffix to remove from base name
    */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    /**
     * do the work
     * @throws BuildException if required attributes are not supplied
     * property and attribute are required attributes
     */
    @Override
    public void execute() throws BuildException {
        if (property == null) {
            throw new BuildException("property attribute required", getLocation());
        }
        if (file == null) {
            throw new BuildException("file attribute required", getLocation());
        }
        getProject().setNewProperty(property,
            removeExtension(file.getName(), suffix));
    }

    private String removeExtension(String s, String ext) {
        if (ext == null || !s.endsWith(ext)) {
            return s;
        }
        int clipFrom = s.length() - ext.length();
        // if the suffix does not starts with a '.' and the
        // char preceding the suffix is a '.', we assume the user
        // wants to remove the '.' as well (see docs)
        if (ext.charAt(0) != '.' && clipFrom > 0 && s.charAt(clipFrom - 1) == '.') {
            clipFrom -= 1;
        }
        return s.substring(0, clipFrom);
    }
}
