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

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.ProjectHelper;

/**
 * looks for a task or other Ant type that exists. Existence is defined as
 * the type is defined, and its implementation class is present. This
 * will work for datatypes and preset, script and macro definitions.
 */
public class TypeFound extends ProjectComponent implements Condition {

    private String name;
    private String uri;

    /**
     * the task or other type to look for
     * @param name the name of the type
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * The URI for this definition.
     * @param uri the namespace URI. If this is not set, use the
     *            default ant namespace.
     */
    public void setURI(String uri) {
        this.uri = uri;
    }

    /**
     * test for a task or other ant type existing in the current project
     * @param typename the name of the type
     * @return true if the typename exists
     */
    protected boolean doesTypeExist(String typename) {
        ComponentHelper helper =
            ComponentHelper.getComponentHelper(getProject());
        String componentName = ProjectHelper.genComponentName(uri, typename);
        AntTypeDefinition def = helper.getDefinition(componentName);
        if (def == null) {
            return false;
        }
        //now verify that the class has an implementation
        boolean found = def.getExposedClass(getProject()) != null;
        if (!found) {
            String text = helper.diagnoseCreationFailure(componentName, "type");
            log(text, Project.MSG_VERBOSE);
        }
        return found;
    }


    /**
     * Is this condition true?
     * @return true if the condition is true
     * @exception BuildException if an error occurs
     */
    @Override
    public boolean eval() throws BuildException {
        if (name == null) {
            throw new BuildException("No type specified");
        }
        return doesTypeExist(name);
    }
}
