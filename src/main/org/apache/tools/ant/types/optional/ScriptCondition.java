/*
 * Copyright  2005 The Apache Software Foundation
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
package org.apache.tools.ant.types.optional;

import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.ScriptRunner;
import org.apache.tools.ant.taskdefs.condition.Condition;

import java.io.File;

/**
 * A condition that lets you include script.
 * The condition component sets a bean "self", whose attribute "result"
 * must be set to true for the condition to succeed, false to fail.
 * The default is 'false'
 */
public class ScriptCondition extends ProjectComponent implements Condition {

    /**
     * script runner
     */
    private ScriptRunner runner = new ScriptRunner();

    /**
     * result field
     */
    private boolean value = false;

    /**
     * Load the script from an external file ; optional.
     *
     * @param file the file containing the script source.
     */
    public void setSrc(File file) {
        runner.setSrc(file);
    }

    /**
     * The script text.
     *
     * @param text a component of the script text to be added.
     */
    public void addText(String text) {
        runner.addText(text);
    }

    /**
     * Defines the language (required).
     *
     * @param language the scripting language name for the script.
     */
    public void setLanguage(String language) {
        runner.setLanguage(language);
    }


    /**
     * Is this condition true?
     *
     * @return true if the condition is true
     *
     * @throws org.apache.tools.ant.BuildException
     *          if an error occurs
     */
    public boolean eval() throws BuildException {
        runner.bindToComponent(this);
        runner.executeScript("ant_condition");
        return getValue();
    }

    /**
     * get the current value of the conditon
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
     * @param value
     */
    public void setValue(boolean value) {
        this.value = value;
    }
}
