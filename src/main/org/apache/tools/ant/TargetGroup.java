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
package org.apache.tools.ant;

/**
 * A special kind of target that must be empty.
 *
 * @since Ant 1.8.0
 */
public class TargetGroup extends Target {

    // no "clone" constructor since I'm not really sure where it is
    // used

    private static final String NO_CHILDREN_ALLOWED
        = "you must not nest child elements into a target-group";

    /**
     * Throws an exception.
     */
    public final void addTask(Task task) {
        throw new BuildException(NO_CHILDREN_ALLOWED);
    }

    /**
     * Throws an exception.
     */
    public final void addDataType(RuntimeConfigurable r) {
        throw new BuildException(NO_CHILDREN_ALLOWED);
    }
    
}