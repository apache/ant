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
package org.apache.tools.ant;

/**
 * An extension point build files can provide as a place where other
 * build files can add new dependencies.
 *
 * @since Ant 1.8.0
 */
public class ExtensionPoint extends Target {

    public ExtensionPoint() {
    }

    /**
     * Cloning constructor.
     * @param other the Target to clone.
     */
    public ExtensionPoint(Target other) {
        //Should we have a clone constructor taking an ExtensionPoint as parameter?
        super(other);
    }


    private static final String NO_CHILDREN_ALLOWED
        = "you must not nest child elements into an extension-point";

    /**
     * Throws an exception.
     */
    @Override
    public final void addTask(Task task) {
        throw new BuildException(NO_CHILDREN_ALLOWED);
    }

    /**
     * Throws an exception.
     */
    @Override
    public final void addDataType(RuntimeConfigurable r) {
        throw new BuildException(NO_CHILDREN_ALLOWED);
    }

}
