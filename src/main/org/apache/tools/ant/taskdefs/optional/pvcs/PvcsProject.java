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

package org.apache.tools.ant.taskdefs.optional.pvcs;



/**
 * represents a project within the PVCS repository to extract files from.
 */
public class PvcsProject {
    private String name;

    /**
     * Set the name of the project
     * @param name the value to use.
     */
    public void setName(String name) {
        PvcsProject.this.name = name;
    }

    /**
     * Get the name of the project
     * @return the name of the project.
     */
    public String getName() {
        return name;
    }
}
