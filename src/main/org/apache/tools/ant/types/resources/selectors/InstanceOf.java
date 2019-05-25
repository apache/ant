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

package org.apache.tools.ant.types.resources.selectors;

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.types.Resource;

/**
 * InstanceOf ResourceSelector.
 * @since Ant 1.7
 */
public class InstanceOf implements ResourceSelector {
    private static final String ONE_ONLY = "Exactly one of class|type must be set.";

    private Project project;
    private Class<?> clazz;
    private String type;
    private String uri;

    /**
     * Set the Project instance for this InstanceOf selector.
     * @param p the Project instance used for type comparisons.
     */
    public void setProject(Project p) {
        project = p;
    }

    /**
     * Set the class to compare against.
     * @param c the class.
     */
    public void setClass(Class<?> c) {
        if (clazz != null) {
            throw new BuildException("The class attribute has already been set.");
        }
        clazz = c;
    }

    /**
     * Set the Ant type to compare against.
     * @param s the type name.
     */
    public void setType(String s) {
        type = s;
    }

    /**
     * Set the URI in which the Ant type, if specified, should be defined.
     * @param u the URI.
     */
    public void setURI(String u) {
        uri = u;
    }

    /**
     * Get the comparison class.
     * @return the Class object.
     */
    public Class<?> getCheckClass() {
        return clazz;
    }

    /**
     * Get the comparison type.
     * @return the String typename.
     */
    public String getType() {
        return type;
    }

    /**
     * Get the type's URI.
     * @return the String URI.
     */
    public String getURI() {
        return uri;
    }

    /**
     * Return true if this Resource is selected.
     * @param r the Resource to check.
     * @return whether the Resource was selected.
     * @throws BuildException if an error occurs.
     */
    public boolean isSelected(Resource r) {
        if ((clazz == null) == (type == null)) {
            throw new BuildException(ONE_ONLY);
        }
        Class<?> c = clazz;
        if (type != null) {
            if (project == null) {
                throw new BuildException(
                    "No project set for InstanceOf ResourceSelector; the type attribute is invalid.");
            }
            AntTypeDefinition d = ComponentHelper.getComponentHelper(
                project).getDefinition(ProjectHelper.genComponentName(uri, type));
            if (d == null) {
                throw new BuildException("type %s not found.",type);
            }
            try {
                c = d.innerGetTypeClass();
            } catch (ClassNotFoundException e) {
                throw new BuildException(e);
            }
        }
        return c.isAssignableFrom(r.getClass());
    }

}
