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

package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.Resource;

/**
 * Condition that checks whether a given resource exists.
 *
 * @since Ant 1.8.0
 */
public class ResourceExists extends ProjectComponent implements Condition {
    private Resource resource;

    /**
     * The resource to test.
     *
     * @param r Resource
     */
    public void add(Resource r) {
        if (resource != null) {
            throw new BuildException("only one resource can be tested");
        }
        resource = r;
    }

    /**
     * Argument validation.
     */
    protected void validate() throws BuildException {
        if (resource == null) {
            throw new BuildException("resource is required");
        }
    }

    public boolean eval() throws BuildException {
        validate();
        return resource.isExists();
    }
}
