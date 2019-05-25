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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.DeweyDecimal;

/**
 * An Ant version condition.
 * @since Ant 1.7
 */
public class AntVersion extends Task implements Condition {

    private String atLeast = null;
    private String exactly = null;
    private String propertyname = null;

    /**
     * Run as a task.
     * @throws BuildException if an error occurs.
     */
    @Override
    public void execute() throws BuildException {
        if (propertyname == null) {
            throw new BuildException("'property' must be set.");
        }
        if (atLeast != null || exactly != null) {
            // If condition values are set, evaluate the condition
            if (eval()) {
                getProject().setNewProperty(propertyname, getVersion().toString());
            }
        } else {
            // Raw task
            getProject().setNewProperty(propertyname, getVersion().toString());
        }
    }

    /**
     * Evaluate the condition.
     * @return true if the condition is true.
     * @throws BuildException if an error occurs.
     */
    @Override
    public boolean eval() throws BuildException {
        validate();
        DeweyDecimal actual = getVersion();
        if (null != atLeast) {
            return actual.isGreaterThanOrEqual(new DeweyDecimal(atLeast));
        }
        if (null != exactly) {
            return actual.isEqual(new DeweyDecimal(exactly));
        }
        //default
        return false;
    }

    private void validate() throws BuildException {
        if (atLeast != null && exactly != null) {
            throw new BuildException("Only one of atleast or exactly may be set.");
        }
        if (null == atLeast && null == exactly) {
            throw new BuildException("One of atleast or exactly must be set.");
        }
        if (atLeast != null) {
            try {
                // only created for side effect
                new DeweyDecimal(atLeast); //NOSONAR
            } catch (NumberFormatException e) {
                throw new BuildException(
                    "The 'atleast' attribute is not a Dewey Decimal eg 1.1.0 : %s",
                    atLeast);
            }
        } else {
            try {
                // only created for side effect
                new DeweyDecimal(exactly); //NOSONAR
            } catch (NumberFormatException e) {
                throw new BuildException(
                    "The 'exactly' attribute is not a Dewey Decimal eg 1.1.0 : %s",
                    exactly);
            }
        }
    }

    private DeweyDecimal getVersion() {
        Project p = new Project();
        p.init();
        StringBuilder sb = new StringBuilder();
        boolean foundFirstDigit = false;
        for (char versionChar : p.getProperty(MagicNames.ANT_VERSION).toCharArray()) {
            if (Character.isDigit(versionChar)) {
                sb.append(versionChar);
                foundFirstDigit = true;
            }
            if (versionChar == '.' && foundFirstDigit) {
                sb.append(versionChar);
            }
            if (Character.isLetter(versionChar) && foundFirstDigit) {
                break;
            }
        }
        return new DeweyDecimal(sb.toString());
    }

    /**
     * Get the atleast attribute.
     * @return the atleast attribute.
     */
    public String getAtLeast() {
        return atLeast;
    }

    /**
     * Set the atleast attribute.
     * This is of the form major.minor.point.
     * For example 1.7.0.
     * @param atLeast the version to check against.
     */
    public void setAtLeast(String atLeast) {
        this.atLeast = atLeast;
    }

    /**
     * Get the exactly attribute.
     * @return the exactly attribute.
     */
    public String getExactly() {
        return exactly;
    }

    /**
     * Set the exactly attribute.
     * This is of the form major.minor.point.
     * For example 1.7.0.
     * @param exactly the version to check against.
     */
    public void setExactly(String exactly) {
        this.exactly = exactly;
    }

    /**
     * Get the name of the property to hold the ant version.
     * @return the name of the property.
     */
    public String getProperty() {
        return propertyname;
    }

    /**
     * Set the name of the property to hold the ant version.
     * @param propertyname the name of the property.
     */
    public void setProperty(String propertyname) {
        this.propertyname = propertyname;
    }

}
