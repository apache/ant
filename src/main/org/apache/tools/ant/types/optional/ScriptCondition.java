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
package org.apache.tools.ant.types.optional;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Condition;

/**
 * A condition that lets you include script.
 * The condition component sets a bean "self", whose attribute "value"
 * must be set to true for the condition to succeed, false to fail.
 * The default is 'false'
 */
public class ScriptCondition extends AbstractScriptComponent implements Condition {

    /**
     * result field
     */
    private boolean value = false;

    /**
     * Is this condition true?
     *
     * @return true if the condition is true
     *
     * @throws BuildException
     *          if an error occurs
     */
    @Override
    public boolean eval() throws BuildException {
        initScriptRunner();
        executeScript("ant_condition");
        return getValue();
    }

    /**
     * get the current value of the condition
     * @return true if the condition
     */
    public boolean getValue() {
        return value;
    }

    /**
     * set the value of the condition.
     * This is used by the script to pass the return value.
     * It can be used by an attribute, in which case it sets the default
     * value
     * @param value the value to set the condition to
     */
    public void setValue(boolean value) {
        this.value = value;
    }
}
