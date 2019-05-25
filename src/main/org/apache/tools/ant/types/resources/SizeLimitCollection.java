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
package org.apache.tools.ant.types.resources;

import org.apache.tools.ant.BuildException;

/**
 * ResourceCollection that imposes a size limit on another ResourceCollection.
 * @since Ant 1.7.1
 */
public abstract class SizeLimitCollection extends BaseResourceCollectionWrapper {
    private static final String BAD_COUNT
        = "size-limited collection count should be set to an int >= 0";

    private int count = 1;

    /**
     * Set the number of resources to be included.
     * @param i the count as <code>int</code>.
     */
    public synchronized void setCount(int i) {
        checkAttributesAllowed();
        count = i;
    }

    /**
     * Get the number of resources to be included. Default is 1.
     * @return the count as <code>int</code>.
     */
    public synchronized int getCount() {
        return count;
    }

    /**
     * Efficient size implementation.
     * @return int size
     */
    @Override
    public synchronized int size() {
        return Math.min(getResourceCollection().size(), getValidCount());
    }

    /**
     * Get the count, verifying it is &gt;= 0.
     * @return int count
     */
    protected int getValidCount() {
        int ct = getCount();
        if (ct < 0) {
            throw new BuildException(BAD_COUNT);
        }
        return ct;
    }

}
