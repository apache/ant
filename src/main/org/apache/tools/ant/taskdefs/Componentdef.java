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

/**
 * Adds a component definition to the current project.
 * <p>Used in the current project two attributes are needed, the name that identifies
 * this component uniquely, and the full name of the class (including the packages) that
 * implements this component.</p>
 *
 * @since Ant 1.8
 * @ant.task category="internal"
 */
public class Componentdef extends Definer {

    /**
     * Default constructor.
     * Creates a new Componentdef instance.
     * Sets the restrict attribute to true.
     */
    public Componentdef() {
        setRestrict(true);
    }
}
