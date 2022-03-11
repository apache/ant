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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.property.LocalProperties;

/**
 * Task to create local properties in the current scope.
 */
public class Local extends Task {
    /**
     * Nested {@code name} element.
     * @since Ant 1.10.13
     */
    public static class Name implements Consumer<LocalProperties> {
        private String text;

        /**
         * Set the property name.
         * @param text
         */
        public void addText(String text) {
            this.text = text;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void accept(LocalProperties localProperties) {
            if (text == null) {
                throw new BuildException("nested name element is missing text");
            }
            localProperties.addLocal(text);
        }
    }

    private String name;
    
    private final Set<Name> nameElements = new LinkedHashSet<>();

    /**
     * Set the name attribute.
     * @param name the name of the local property.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Create a nested {@code name} element.
     * @return {@link Name}
     * @since Ant 1.10.13
     */
    public Name createName() {
        final Name result = new Name();
        nameElements.add(result);
        return result;
    }

    /**
     * Run the task.
     */
    public void execute() {
        if (name == null && nameElements.isEmpty()) {
            throw new BuildException("Found no configured local property names");
        }
        final LocalProperties localProperties = LocalProperties.get(getProject());

        if (name != null) {
            localProperties.addLocal(name);
        }
        nameElements.forEach(n -> n.accept(localProperties));
    }
}
