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
package org.apache.tools.ant.taskdefs.condition;
import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.selectors.AbstractSelectorContainer;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.apache.tools.ant.util.FileUtils;

/**
 * This is a condition that checks to see if a file passes an embedded selector.
 */
public class IsFileSelected extends AbstractSelectorContainer implements Condition {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private File file;
    private File baseDir;

    /**
     * The file to check.
     * @param file the file to check if if passes the embedded selector.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * The base directory to use.
     * @param baseDir the base directory to use, if null use the project's
     *                basedir.
     */
    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * validate the parameters.
     */
    public void validate() {
        if (selectorCount() != 1) {
            throw new BuildException("Only one selector allowed");
        }
        super.validate();
    }

    /**
     * Evaluate the selector with the file.
     * @return true if the file is selected by the embedded selector.
     */
    public boolean eval() {
        if (file == null) {
            throw new BuildException("file attribute not set");
        }
        validate();
        File myBaseDir = baseDir;
        if (myBaseDir == null) {
            myBaseDir = getProject().getBaseDir();
        }

        FileSelector f = getSelectors(getProject())[0];
        return f.isSelected(
            myBaseDir, FILE_UTILS.removeLeadingPath(myBaseDir, file), file);
    }
}
