/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * ResourceCollection representing the intersection
 * of multiple nested ResourceCollections.
 * @since Ant 1.7
 */
public class Intersect extends BaseResourceCollectionContainer {

    /**
     * Calculate the intersection of the nested ResourceCollections.
     * @return a Collection of Resources.
     */
    protected Collection<Resource> getCollection() {
        List<ResourceCollection> rcs = getResourceCollections();
        int size = rcs.size();
        if (size < 2) {
            throw new BuildException("The intersection of " + size
                + " resource collection" + ((size == 1) ? "" : "s")
                + " is undefined.");
        }
        Iterator<ResourceCollection> rc = rcs.iterator();
        Set<Resource> s = new LinkedHashSet<Resource>(collect(rc.next()));
        while (rc.hasNext()) {
            s.retainAll(collect(rc.next()));
        }
        return s;
    }

    private Set<Resource> collect(ResourceCollection rc) {
        Set<Resource> result = new LinkedHashSet<Resource>();
        for (Resource r : rc) {
            result.add(r);
        }
        return result;
    }
}
