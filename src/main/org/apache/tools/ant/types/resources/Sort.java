/*
 * Copyright 2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.comparators.ResourceComparator;

/**
 * ResourceCollection that sorts another ResourceCollection.
 * @since Ant 1.7
 */
public class Sort extends BaseResourceCollectionContainer {
    private static final String ONE_NESTED_MESSAGE
        = "Sorting is to be applied to exactly one nested resource collection.";

    private Stack compStack = new Stack();

    /**
     * Sort the contained elements.
     * @return a Collection of Resources.
     */
    protected Collection getCollection() {
        List rcs = getResourceCollections();
        if (rcs.size() != 1) {
            throw new BuildException(ONE_NESTED_MESSAGE);
        }
        Iterator nested = ((ResourceCollection) (rcs.get(0))).iterator();
        if (!(nested.hasNext())) {
            return Collections.EMPTY_SET;
        }
        ArrayList al = new ArrayList();
        while (nested.hasNext()) {
            al.add(nested.next());
        }
        if (compStack.empty()) {
            Collections.sort(al);
        } else {
            for (Stack s = (Stack) compStack.clone(); !s.empty();) {
                Collections.sort(al, (ResourceComparator) s.pop());
            }
        }
        return al;
    }

    /**
     * Add a ResourceComparator to this Sort ResourceCollection.
     * If multiple ResourceComparator are added, they will be processed in LIFO order.
     * @param c the ResourceComparator to add.
     */
    public void add(ResourceComparator c) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        compStack.push(c);
    }

    /**
     * Overrides the BaseResourceCollectionContainer version
     * to recurse on nested ResourceComparators.
     * @param stk the stack of data types to use (recursively).
     * @param p   the project to use to dereference the references.
     * @throws BuildException on error.
     */
    protected void dieOnCircularReference(Stack stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            for (Iterator i = compStack.iterator(); i.hasNext();) {
                Object o = i.next();
                if (o instanceof DataType) {
                    stk.push(o);
                    invokeCircularReferenceCheck((DataType) o, stk, p);
                }
            }
            setChecked(true);
        }
    }

}
