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
package org.apache.tools.ant.types.resources.comparators;

import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.util.ResourceUtils;

/**
 * Compares Resources by content.
 * @since Ant 1.7
 */
public class Content extends ResourceComparator {

    private boolean binary = true;

    /**
     * Set binary mode for this Content ResourceComparator. If this
     * attribute is set to false, Resource content will be compared
     * ignoring platform line-ending conventions.
     * Default is <code>true</code>.
     * @param b whether to compare content in binary mode.
     */
    public void setBinary(boolean b) {
        binary = b;
    }

    /**
     * Learn whether this Content ResourceComparator is operating in binary mode.
     * @return boolean binary flag.
     */
    public boolean isBinary() {
        return binary;
    }

    /**
     * Compare two Resources by content.
     * @param foo the first Resource.
     * @param bar the second Resource.
     * @return a negative integer, zero, or a positive integer as the first
     *         argument is less than, equal to, or greater than the second.
     * @throws BuildException if I/O errors occur.
     * @see org.apache.tools.ant.util.ResourceUtils#compareContent(Resource, Resource, boolean)
     */
    protected int resourceCompare(Resource foo, Resource bar) {
        try {
            return ResourceUtils.compareContent(foo, bar, !binary);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

}
