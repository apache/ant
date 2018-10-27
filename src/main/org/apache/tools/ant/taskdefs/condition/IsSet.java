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
package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;

/**
 * Condition that tests whether a given property has been set.
 *
 * @since Ant 1.5
 */
public class IsSet extends ProjectComponent implements Condition {
    private String property;

    /**
     * Set the property attribute
     *
     * @param p the property name
     */
    public void setProperty(String p) {
        property = p;
    }

    /**
     * @return true if the property exists
     * @exception BuildException if the property attribute is not set
     */
    @Override
    public boolean eval() throws BuildException {
        if (property == null) {
            throw new BuildException(
                "No property specified for isset condition");
        }
        return getProject().getProperty(property) != null;
    }

}
