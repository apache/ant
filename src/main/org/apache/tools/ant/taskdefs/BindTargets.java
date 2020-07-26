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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.ProjectHelper.OnMissingExtensionPoint;
import org.apache.tools.ant.Task;

/**
 * Simple task which bind some targets to some defined extension point
 */
public class BindTargets extends Task {

    private String extensionPoint;

    private final List<String> targets = new ArrayList<>();

    private OnMissingExtensionPoint onMissingExtensionPoint;

    public void setExtensionPoint(final String extensionPoint) {
        this.extensionPoint = extensionPoint;
    }

    public void setOnMissingExtensionPoint(final String onMissingExtensionPoint) {
        try {
            this.onMissingExtensionPoint = OnMissingExtensionPoint.valueOf(onMissingExtensionPoint);
        } catch (final IllegalArgumentException e) {
            throw new BuildException("Invalid onMissingExtensionPoint: " + onMissingExtensionPoint);
        }
    }

    public void setOnMissingExtensionPoint(final OnMissingExtensionPoint onMissingExtensionPoint) {
        this.onMissingExtensionPoint = onMissingExtensionPoint;
    }

    public void setTargets(final String target) {
        Stream.of(target.split(",")).map(String::trim).filter(s -> !s.isEmpty())
            .forEach(targets::add);
    }

    @Override
    public void execute() throws BuildException {
        if (extensionPoint == null) {
            throw new BuildException("extensionPoint required", getLocation());
        }

        if (getOwningTarget() == null
                || !getOwningTarget().getName().isEmpty()) {
            throw new BuildException("bindtargets only allowed as a top-level task");
        }

        if (onMissingExtensionPoint == null) {
            onMissingExtensionPoint = OnMissingExtensionPoint.FAIL;
        }
        final ProjectHelper helper = getProject().getReference(MagicNames.REFID_PROJECT_HELPER);

        for (String target : targets) {
            helper.getExtensionStack().add(new String[] {extensionPoint,
                target, onMissingExtensionPoint.name()});
        }
    }
}
