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

import org.apache.tools.ant.types.FileSet;

/**
 * LibFileSet represents a fileset containing libraries.
 * Associated with the libraries is data pertaining to
 * how they are to be handled when building manifests.
 *
 */
public class LibFileSet extends FileSet {
    /**
     * Flag indicating whether should include the
     * "Implementation-URL" attribute in manifest.
     * Defaults to false.
     */
    private boolean includeURL;

    /**
     * Flag indicating whether should include the
     * "Implementation-*" attributes in manifest.
     * Defaults to false.
     */
    private boolean includeImpl;

    /**
     * String that is the base URL for the libraries
     * when constructing the "Implementation-URL"
     * attribute. For instance setting the base to
     * "https://jakarta.apache.org/avalon/libs/" and then
     * including the library "excalibur-cli-1.0.jar" in the
     * fileset will result in the "Implementation-URL" attribute
     * being set to "https://jakarta.apache.org/avalon/libs/excalibur-cli-1.0.jar"
     *
     * Note this is only used if the library does not define
     * "Implementation-URL" itself.
     *
     * Note that this also implies includeURL=true
     */
    private String urlBase;

    /**
     * Flag indicating whether should include the
     * "Implementation-URL" attribute in manifest.
     * Defaults to false.
     *
     * @param includeURL the flag
     */
    public void setIncludeUrl(boolean includeURL) {
        this.includeURL = includeURL;
    }

    /**
     * Flag indicating whether should include the
     * "Implementation-*" attributes in manifest.
     * Defaults to false.
     *
     * @param includeImpl the flag
     */
    public void setIncludeImpl(boolean includeImpl) {
        this.includeImpl = includeImpl;
    }

    /**
     * Set the url base for fileset.
     *
     * @param urlBase the base url
     */
    public void setUrlBase(String urlBase) {
        this.urlBase = urlBase;
    }

    /**
     * Get the includeURL flag.
     *
     * @return the includeURL flag.
     */
    boolean isIncludeURL() {
        return includeURL;
    }

    /**
     * Get the includeImpl flag.
     *
     * @return the includeImpl flag.
     */
    boolean isIncludeImpl() {
        return includeImpl;
    }

    /**
     * Get the urlbase.
     *
     * @return the urlbase.
     */
    String getUrlBase() {
        return urlBase;
    }
}
