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
package org.apache.tools.ant.taskdefs.optional.extension;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Interface to locate a File that satisfies extension.
 *
 */
public interface ExtensionResolver {
    /**
     * Attempt to locate File that satisfies
     * extension via resolver.
     *
     * @param extension the extension
     * @param project the Ant project instance
     * @return the File satisfying extension, null
     *         if can not resolve extension
     * @throws BuildException if error occurs attempting to
     *         resolve extension
     */
    File resolve(Extension extension, Project project)
        throws BuildException;
}
