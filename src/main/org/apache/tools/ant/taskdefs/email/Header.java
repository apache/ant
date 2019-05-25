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

package org.apache.tools.ant.taskdefs.email;

/**
 * Class representing a generic key-value header.
 * TODO: This should be moved out of the email package
 *
 * @since Ant 1.7
 */
public class Header {
    private String name;
    private String value;

    /**
     * Set the name of this Header.
     * @param name the name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the name of this Header.
     * @return name as String.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the value of this Header.
     * @param value the value to set.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Get the value of this Header.
     * @return value as String.
     */
    public String getValue() {
        return value;
    }

}
