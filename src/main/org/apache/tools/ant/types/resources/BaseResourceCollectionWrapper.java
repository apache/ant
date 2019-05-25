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

import java.util.Collection;
import java.util.Iterator;

import org.apache.tools.ant.types.Resource;

/**
 * Base class for a ResourceCollection that wraps a single nested
 * ResourceCollection.
 * @since Ant 1.7
 */
public abstract class BaseResourceCollectionWrapper
    extends AbstractResourceCollectionWrapper {

    private Collection<Resource> coll = null;

    @Override
    protected Iterator<Resource> createIterator() {
        return cacheCollection().iterator();
    }

    @Override
    protected int getSize() {
        return cacheCollection().size();
    }

    /**
     * Template method for subclasses to return a Collection of Resources.
     * @return Collection.
     */
    protected abstract Collection<Resource> getCollection();

    private synchronized Collection<Resource> cacheCollection() {
        if (coll == null || !isCache()) {
            coll = getCollection();
        }
        return coll;
    }

}
