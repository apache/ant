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

import org.apache.tools.ant.types.ResourceCollection;

/**
 * A compressed resource.
 *
 * <p>Wraps around another resource, delegates all queries (except
 * getSize) to that other resource but uncompresses/compresses streams
 * on the fly.</p>
 *
 * @since Ant 1.7
 */
public abstract class CompressedResource extends ContentTransformingResource {

    /** no arg constructor */
    protected CompressedResource() {
    }

    /**
     * Constructor with another resource to wrap.
     * @param other the resource to wrap.
     */
    protected CompressedResource(ResourceCollection other) {
        addConfigured(other);
    }

    /**
     * Get the string representation of this Resource.
     * @return this Resource formatted as a String.
     * @since Ant 1.7
     */
    @Override
    public String toString() {
        return getCompressionName() + " compressed " + super.toString();
    }

    /**
     * Get the name of the compression method used.
     * @return the name of the compression method.
     */
    protected abstract String getCompressionName();

}
