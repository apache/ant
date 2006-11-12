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
package org.apache.tools.ant.taskdefs.optional.dotnet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;

/**
 * definitions can be conditional. What .NET conditions can not be
 * is in any state other than defined and undefined; you cannot give
 * a definition a value.
 */
public class DotnetDefine {
    private String name;
    private String ifCond;
    private String unlessCond;


    /**
     * the name of a property which must be defined for
     * the definition to be set. Optional.
     * @param condition the name of the property
     */
    public void setIf(String condition) {
        this.ifCond = condition;
    }

    /**
     * the name of a property which must be undefined for
     * the definition to be set. Optional.
     * @param condition the name of the property
     */
    public void setUnless(String condition) {
        this.unlessCond = condition;
    }

    /**
     * Get the name of the definition.
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * the name of the definition. Required.
     * @param name the name value.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * This method gets the value of this definition. Will be null if a condition
     * was declared and not met
     * @param owner owning task
     * @return The value of the definition.
     * @throws BuildException if there is an error.
     */
    public String getValue(Task owner) throws BuildException {
        if (name == null) {
            throw new BuildException("No name provided for the define element",
                owner.getLocation());
        }
        if (!isSet(owner)) {
            return null;
        }
        return name;
    }


    /**
     * logic taken from patternset
     * @param owner the owning task.
     * @return true if the condition is valid
     */
    public boolean isSet(Task owner) {
        Project p = owner.getProject();
        if (ifCond != null && p.getProperty(ifCond) == null) {
            return false;
        } else if (unlessCond != null && p.getProperty(unlessCond) != null) {
            return false;
        }
        return true;
    }
}
