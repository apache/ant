/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.ProjectHelperRepository;
import org.apache.tools.ant.Task;

/**
 * Task to install project helper into Ant's runtime
 *
 * @since Ant 1.8.2
 */
public class ProjectHelperTask extends Task {

    private List<ProjectHelper> projectHelpers = new ArrayList<>();

    public synchronized void addConfigured(ProjectHelper projectHelper) {
        this.projectHelpers.add(projectHelper);
    }

    @Override
    public void execute() throws BuildException {
        projectHelpers.stream().map(ProjectHelper::getClass).forEach(
            ProjectHelperRepository.getInstance()::registerProjectHelper);
    }
}
