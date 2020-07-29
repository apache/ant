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
package org.apache.tools.ant.types;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.helper.ProjectHelper2;
import org.apache.tools.ant.helper.ProjectHelperImpl;


/**
 * Description is used to provide a project-wide description element
 * (that is, a description that applies to a buildfile as a whole).
 * If present, the &lt;description&gt; element is printed out before the
 * target descriptions.
 *
 * Description has no attributes, only text.  There can only be one
 * project description per project.  A second description element will
 * overwrite the first.
 *
 *
 * @ant.datatype ignore="true"
 */
public class Description extends DataType {

    /**
     * Adds descriptive text to the project.
     *
     * @param text the descriptive text
     */
    public void addText(String text) {

        ProjectHelper ph = getProject().getReference(MagicNames.REFID_PROJECT_HELPER);
        if (!(ph instanceof ProjectHelperImpl)) {
            // New behavior for delayed task creation. Description
            // will be evaluated in Project.getDescription()
            return;
        }
        String currentDescription = getProject().getDescription();
        if (currentDescription == null) {
            getProject().setDescription(text);
        } else {
            getProject().setDescription(currentDescription + text);
        }
    }

    /**
     * Return the descriptions from all the targets of
     * a project.
     *
     * @param project the project to get the descriptions for.
     * @return a string containing the concatenated descriptions of
     *         the targets.
     */
    public static String getDescription(Project project) {
        List<Target> targets = project.getReference(ProjectHelper2.REFID_TARGETS);
        if (targets == null) {
            return null;
        }
        StringBuilder description = new StringBuilder();
        for (Target t : targets) {
            concatDescriptions(project, t, description);
        }
        return description.toString();
    }

    private static void concatDescriptions(Project project, Target t,
                                           StringBuilder description) {
        if (t == null) {
            return;
        }
        for (Task task : findElementInTarget(t, "description")) {
            if (task instanceof UnknownElement) {
                UnknownElement ue = (UnknownElement) task;
                String descComp = ue.getWrapper().getText().toString();
                if (descComp != null) {
                    description.append(project.replaceProperties(descComp));
                }
            }
        }
    }

    private static List<Task> findElementInTarget(Target t, String name) {
        return Stream.of(t.getTasks())
            .filter(task -> name.equals(task.getTaskName()))
            .collect(Collectors.toList());
    }

}
