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
import java.util.Stack;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.comparators.ResourceComparator;
import org.apache.tools.ant.types.resources.comparators.DelegatedResourceComparator;
import org.apache.tools.ant.util.CollectionUtils;

/**
 * ResourceCollection that sorts another ResourceCollection.
 *
 * Note that Sort must not be used in cases where the ordering of the objects
 * being sorted might change during the sorting process.
 *
 * @since Ant 1.7
 */
public class Sort extends BaseResourceCollectionWrapper {

    private DelegatedResourceComparator comp = new DelegatedResourceComparator();

    /**
     * Sort the contained elements.
     * @return a Collection of Resources.
     */
    protected synchronized Collection getCollection() {
        ResourceCollection rc = getResourceCollection();
        Iterator iter = rc.iterator();
        if (!(iter.hasNext())) {
            return Collections.EMPTY_SET;
        }
        List result = (List) CollectionUtils.asCollection(iter);
        Collections.sort(result, comp);
        return result;
    }

    /**
     * Add a ResourceComparator to this Sort ResourceCollection.
     * If multiple ResourceComparators are added, they will be processed in LIFO order.
     * @param c the ResourceComparator to add.
     */
    public synchronized void add(ResourceComparator c) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        comp.add(c);
        FailFast.invalidate(this);
        setChecked(false);
    }

    /**
     * Overrides the BaseResourceCollectionContainer version
     * to recurse on nested ResourceComparators.
     * @param stk the stack of data types to use (recursively).
     * @param p   the project to use to dereference the references.
     * @throws BuildException on error.
     */
    protected synchronized void dieOnCircularReference(Stack stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        
        // check nested collection
        super.dieOnCircularReference(stk, p);

        if (!isReference()) {
            DataType.pushAndInvokeCircularReferenceCheck(comp, stk, p);
            setChecked(true);
        }
    }

}
