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
import java.nio.file.Files;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.util.FileUtils;

/**
 * Tests whether the current process can create a symbolic link.
 *
 * <p>Actually tries to create a temporary symbolic link so is completely independent of the current platform.</p>
 *
 * @since Ant 1.10.16
 */
public class CanCreateSymbolicLink extends ProjectComponent implements Condition {
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    @Override
    public boolean eval() {
        File linkTarget = FILE_UTILS.createTempFile(getProject(), null, null, null, true, true);
        File link = FILE_UTILS.createTempFile(getProject(), null, null, null, false, false);
        try {
            Files.createSymbolicLink(link.toPath(), linkTarget.toPath());
        } catch (Exception ex) {
            log("Cannot create symbolic links, caught " + ex.getClass()
                + " exception with message " + ex.getMessage(),
                Project.MSG_VERBOSE);
            return false;
        }
        return true;
    }
}
