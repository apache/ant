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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Task;

/**
 * Base class for tasks that that can be used in antlibs.
 * For handling uri and class loading.
 *
 * @since Ant 1.6
 */
public class AntlibDefinition extends Task {

    private String uri = "";
    private ClassLoader antlibClassLoader;

    /**
     * The URI for this definition.
     * If the URI is "antlib:org.apache.tools.ant",
     * (this is the default uri)
     * the uri will be set to "".
     * URIs that start with "ant:" are reserved
     * and are not allowed in this context.
     * @param uri the namespace URI
     * @throws BuildException if a reserved URI is used
     */
    public void setURI(String uri) throws BuildException {
        if (ProjectHelper.ANT_CORE_URI.equals(uri)) {
            uri = "";
        }
        if (uri.startsWith("ant:")) {
            throw new BuildException("Attempt to use a reserved URI %s", uri);
        }
        this.uri = uri;
    }

    /**
     * The URI for this definition.
     * @return The URI for this definition.
     */
    public String getURI() {
        return uri;
    }

    /**
     * Set the class loader of the loading object
     *
     * @param classLoader a <code>ClassLoader</code> value
     */
    public void setAntlibClassLoader(ClassLoader classLoader) {
        this.antlibClassLoader = classLoader;
    }

    /**
     * The current antlib classloader
     * @return the antlib classloader for the definition, this
     *         is null if the definition is not used in an antlib.
     */
    public ClassLoader getAntlibClassLoader() {
        return antlibClassLoader;
    }
}
