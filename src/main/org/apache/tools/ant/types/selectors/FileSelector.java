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

package org.apache.tools.ant.types.selectors;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.selectors.ResourceSelector;

/**
 * This is the interface to be used by all selectors.
 *
 * @since 1.5
 */
public interface FileSelector extends ResourceSelector {

    /**
     * Method that each selector will implement to create their
     * selection behaviour. If there is a problem with the setup
     * of a selector, it can throw a BuildException to indicate
     * the problem.
     *
     * @param basedir A java.io.File object for the base directory
     * @param filename The name of the file to check
     * @param file A File object for this filename
     * @return whether the file should be selected or not
     * @exception BuildException if the selector was not configured correctly
     */
    boolean isSelected(File basedir, String filename, File file)
            throws BuildException;

    /**
     * Implement a basic {@link Resource} selection that delegates to this
     * {@link FileSelector}.
     * @param r resource
     * @return whether the resource is selected
     */
    default boolean isSelected(Resource r) {
        return r.asOptional(FileProvider.class).map(FileProvider::getFile)
                .map(f -> isSelected(null, null, f)).orElse(false);
    }
}

