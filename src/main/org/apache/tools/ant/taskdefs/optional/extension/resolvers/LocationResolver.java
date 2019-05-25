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
package org.apache.tools.ant.taskdefs.optional.extension.resolvers;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.extension.Extension;
import org.apache.tools.ant.taskdefs.optional.extension.ExtensionResolver;

/**
 * Resolver that just returns s specified location.
 *
 */
public class LocationResolver implements ExtensionResolver {
    private String location;

    /**
     * Sets the location for this resolver
     * @param location the location
     */
    public void setLocation(final String location) {
        this.location = location;
    }

    /**
     * Returns the resolved file
     * @param extension the extension
     * @param project the project
     * @return the file resolved
     * @throws BuildException if no location is set
     */
    @Override
    public File resolve(final Extension extension,
                        final Project project) throws BuildException {
        if (null == location) {
            throw new BuildException("No location specified for resolver");
        }
        return project.resolveFile(location);
    }
    /**
     * Returns a string representation of the Location
     * @return the string representation
     */
    @Override
    public String toString() {
        return "Location[" + location + "]";
    }
}
