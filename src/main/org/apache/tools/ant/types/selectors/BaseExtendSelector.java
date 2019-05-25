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
import org.apache.tools.ant.types.Parameter;

/**
 * Convenience base class for all selectors accessed through ExtendSelector.
 * It provides support for gathering the parameters together as well as for
 * assigning an error message and throwing a build exception if an error is
 * detected.
 *
 * @since 1.5
 */
public abstract class BaseExtendSelector extends BaseSelector
    implements ExtendFileSelector {

    // CheckStyle:VisibilityModifier OFF - bc

    /** The passed in parameter array. */
    protected Parameter[] parameters = null;

    // CheckStyle:VisibilityModifier ON

    /**
     * Set all the Parameters for this custom selector, collected by
     * the ExtendSelector class.
     *
     * @param parameters the complete set of parameters for this selector
     */
    @Override
    public void setParameters(Parameter... parameters) {
        this.parameters = parameters;
    }

    /**
     * Allows access to the parameters gathered and set within the
     * &lt;custom&gt; tag.
     *
     * @return the set of parameters defined for this selector
     */
    protected Parameter[] getParameters() {
        return parameters;
    }

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
     * @exception BuildException if an error occurs
     */
    public abstract boolean isSelected(File basedir, String filename,
                                       File file)
            throws BuildException;

}

