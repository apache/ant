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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.property.LocalProperties;

/**
 * Task to create a local property in the current scope.
 */
public class Local extends Task {
    private String name;

    /**
     * Set the name attribute.
     * @param name the name of the local property.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Run the task.
     */
    public void execute() {
        if (name == null) {
            throw new BuildException("Missing attribute name");
        }
        LocalProperties.get(getProject()).addLocal(name);
    }
}
