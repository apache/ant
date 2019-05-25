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
import java.util.Collections;
import java.util.stream.Collectors;

import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * ResourceCollection that contains all resources of another
 * collection except for the last <code>count</code> elements, a la
 * the UNIX head command with parameter <code>-n -count</code>.
 * @since Ant 1.9.5
 */
public class AllButLast extends SizeLimitCollection {

    /**
     * Take all elements except for the last <code>count</code> elements.
     * @return a Collection of Resources.
     */
    @Override
    protected Collection<Resource> getCollection() {
        int ct = getValidCount();
        ResourceCollection nested = getResourceCollection();
        if (ct > nested.size()) {
            return Collections.emptyList();
        }
        return nested.stream().limit((long) nested.size() - ct)
            .collect(Collectors.toList());
    }

    @Override
    public synchronized int size() {
        return Math.max(getResourceCollection().size() - getValidCount(), 0);
    }

}
