/*
 * Copyright  2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Condition;

/**
 * Abstract task to allow defintion of a task or a condition.
 * It has property and value (for the property) attributes.
 *
 * @since Ant 1.7
 *
 * @ant.task category="control"
 */
public abstract class ConditionAndTask extends Task implements Condition {

    private String property;
    private String value = "true";

    /**
     * Set the name of the property which will be set if the particular resource
     * is available.
     *
     * @param property the name of the property to set.
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * Set the value to be given to the property if the desired resource is
     * available.
     *
     * @param value the value to be given.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * This method should be overridden by derived classes.
     * It is used by eval() to evaluate the condition.
     * @return true if the condition passes, false otherwise.
     */
    protected abstract boolean evaluate();

    /**
     * This method evaluates the condition. It calls evaluate in the
     * derived class.
     * It sets the property if a property is present and if the
     * evaluate returns true.
     * @return true if the condition passes, false otherwise.
     */
    public boolean eval() {
        if (evaluate()) {
            if (property != null) {
                getProject().setNewProperty(property, value);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Entry point when operating as a task.
     *
     * @exception BuildException if the task is not configured correctly.
     */
    public void execute() throws BuildException {
        if (property == null) {
            throw new BuildException("property attribute is required",
                                     getLocation());
        }
        eval();
    }

}
