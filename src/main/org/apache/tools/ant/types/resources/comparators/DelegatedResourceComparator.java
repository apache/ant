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

import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Resource;

/**
 * Delegates to other ResourceComparators or, if none specified,
 * uses Resources' natural ordering.
 * @since Ant 1.7
 */
public class DelegatedResourceComparator extends ResourceComparator {

    private List<ResourceComparator> resourceComparators = null;

    /**
     * Add a delegate ResourceComparator.
     * @param c the next delegate ResourceComparator.
     */
    public synchronized void add(ResourceComparator c) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (c == null) {
            return;
        }
        resourceComparators = (resourceComparators == null) ? new Vector<>() : resourceComparators;
        resourceComparators.add(c);
        setChecked(false);
    }

    /**
     * Equality method based on the vector of resources,
     * or if a reference, the referred to object.
     * @param o the object to check against.
     * @return true if there is equality.
     */
    @Override
    public synchronized boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (isReference()) {
            return getRef().equals(o);
        }
        if (o instanceof DelegatedResourceComparator) {
            List<ResourceComparator> ov = ((DelegatedResourceComparator) o).resourceComparators;
            return resourceComparators == null ? ov == null : resourceComparators.equals(ov);
        }
        return false;
    }

    /**
     * Hashcode based on the rules for equality.
     * @return a hashcode.
     */
    @Override
    public synchronized int hashCode() {
        if (isReference()) {
            return getRef().hashCode();
        }
        return resourceComparators == null ? 0 : resourceComparators.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    protected synchronized int resourceCompare(Resource foo, Resource bar) {
        return composite(resourceComparators).compare(foo, bar);
    }

    /**
     * Overrides the version from DataType to recurse on nested ResourceSelector
s.
     * @param stk the Stack of references.
     * @param p   the Project to resolve against.
     * @throws BuildException on error.
     */
    @Override
    protected void dieOnCircularReference(Stack<Object> stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            if (resourceComparators != null && !resourceComparators.isEmpty()) {
                for (ResourceComparator resourceComparator : resourceComparators) {
                    if (resourceComparator instanceof DataType) {
                        pushAndInvokeCircularReferenceCheck(resourceComparator, stk, p);
                    }
                }
            }
            setChecked(true);
        }
    }

    private DelegatedResourceComparator getRef() {
        return getCheckedRef(DelegatedResourceComparator.class);
    }

    private static Comparator<Resource> composite(List<? extends Comparator<Resource>> foo) {
        Comparator<Resource> result = null;
        if (foo != null) {
            for (Comparator<Resource> comparator : foo) {
                if (result == null) {
                    result = comparator;
                    continue;
                }
                result = result.thenComparing(comparator);
            }
        }
        return result == null ? Comparator.naturalOrder() : result;
    }
}
