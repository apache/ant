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

import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * ResourceCollection representing the difference between
 * two or more nested ResourceCollections.
 * @since Ant 1.7
 */
public class Difference extends BaseResourceCollectionContainer {

    /**
     * Calculate the difference of the nested ResourceCollections.
     * @return a Collection of Resources.
     */
    protected Collection getCollection() {
        List rc = getResourceCollections();
        int size = rc.size();
        if (size < 2) {
            throw new BuildException("The difference of " + size
                + " resource collection" + ((size == 1) ? "" : "s")
                + " is undefined.");
        }
        HashSet hs = new HashSet();
        ArrayList al = new ArrayList();
        for (Iterator rcIter = rc.iterator(); rcIter.hasNext();) {
            for (Iterator r = nextRC(rcIter).iterator(); r.hasNext();) {
                Object next = r.next();
                if (hs.add(next)) {
                    al.add(next);
                } else {
                    al.remove(next);
                }
            }
        }
        return al;
    }

    private static ResourceCollection nextRC(Iterator i) {
        return (ResourceCollection) i.next();
    }

}
