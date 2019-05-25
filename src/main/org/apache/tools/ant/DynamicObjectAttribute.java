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
 * Enables a task to control unknown attributes.
 * Same as {@link DynamicAttribute} but authorize arbitrary Object as value
 * rather than String
 *
 * @see DynamicAttribute
 * @since Ant 1.9
 */
public interface DynamicObjectAttribute {

    /**
     * Set a named attribute to the given value
     *
     * @param name the name of the attribute
     * @param value the new value of the attribute
     * @throws BuildException when any error occurs
     */
    void setDynamicAttribute(String name, Object value)
            throws BuildException;

}
