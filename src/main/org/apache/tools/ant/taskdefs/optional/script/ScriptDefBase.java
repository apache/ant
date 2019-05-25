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
package org.apache.tools.ant.taskdefs.optional.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicConfigurator;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Task;

/**
 * The script execution class. This class finds the defining script task
 * and passes control to that task's executeScript method.
 *
 * @since Ant 1.6
 */
public class ScriptDefBase extends Task implements DynamicConfigurator {

    /** Nested elements */
    private Map<String, List<Object>> nestedElementMap = new HashMap<>();

    /** Attributes */
    private Map<String, String> attributes = new HashMap<>();

    private String text;

    /**
     * Locate the script defining task and execute the script by passing
     * control to it
     */
    @Override
    public void execute() {
        getScript().executeScript(attributes, nestedElementMap, this);
    }

    private ScriptDef getScript() {
        String name = getTaskType();
        Map<String, ScriptDef> scriptRepository =
            getProject().getReference(MagicNames.SCRIPT_REPOSITORY);
        if (scriptRepository == null) {
            throw new BuildException("Script repository not found for " + name);
        }

        ScriptDef definition = scriptRepository.get(getTaskType());
        if (definition == null) {
            throw new BuildException("Script definition not found for " + name);
        }
        return definition;
    }

    /**
     * Create a nested element
     *
     * @param name the nested element name
     * @return the element to be configured
     */
    @Override
    public Object createDynamicElement(String name)  {
        List<Object> nestedElementList =
            nestedElementMap.computeIfAbsent(name, k -> new ArrayList<>());
        Object element = getScript().createNestedElement(name);
        nestedElementList.add(element);
        return element;
    }

    /**
     * Set a task attribute
     *
     * @param name the attribute name.
     * @param value the attribute's string value
     */
    @Override
    public void setDynamicAttribute(String name, String value) {
        ScriptDef definition = getScript();
        if (!definition.isAttributeSupported(name)) {
            throw new BuildException(
                "<%s> does not support the \"%s\" attribute", getTaskType(),
                name);
        }
        attributes.put(name, value);
    }

    /**
     * Set the script text.
     *
     * @param text a component of the script text to be added.
     * @since ant1.7
     */
    public void addText(String text) {
        this.text = getProject().replaceProperties(text);
    }

    /**
     * get the text of this element; may be null
     * @return text or null for no nested text
     * @since ant1.7
     */
    public String getText() {
        return text;
    }

    /**
     * Utility method for nested scripts; throws a BuildException
     * with the given message.
     * @param message text to pass to the BuildException
     * @throws BuildException always.
     * @since ant1.7
     */
    public void fail(String message) {
        throw new BuildException(message);
    }
}

