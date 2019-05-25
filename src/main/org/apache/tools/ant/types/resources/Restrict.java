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

import java.util.Iterator;
import java.util.Stack;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.selectors.ResourceSelector;
import org.apache.tools.ant.types.resources.selectors.ResourceSelectorContainer;

/**
 * ResourceCollection that allows a number of selectors to be
 * applied to a single ResourceCollection for the purposes of
 * restricting or narrowing results.
 * @since Ant 1.7
 */
public class Restrict
    extends ResourceSelectorContainer implements ResourceCollection {

    private LazyResourceCollectionWrapper w = new  LazyResourceCollectionWrapper() {
        /**
         * Restrict the nested ResourceCollection based on the nested selectors.
         */
        @Override
        protected boolean filterResource(Resource r) {
            return getResourceSelectors().stream().anyMatch(rsel -> !rsel.isSelected(r));
        }
    };

    /**
     * Add the ResourceCollection.
     * @param c the ResourceCollection to add.
     */
    public synchronized void add(ResourceCollection c) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (c == null) {
            return;
        }
        w.add(c);
        setChecked(false);
    }

    /**
     * Set whether to cache collections.
     * @param b boolean cache flag.
     */
    public synchronized void setCache(boolean b) {
        w.setCache(b);
    }

    /**
     * Learn whether to cache collections. Default is <code>true</code>.
     * @return boolean cache flag.
     */
    public synchronized boolean isCache() {
        return w.isCache();
    }

    /**
     * Add a ResourceSelector.
     * @param s the ResourceSelector to add.
     */
    @Override
    public synchronized void add(ResourceSelector s) {
        if (s == null) {
            return;
        }
        super.add(s);
        FailFast.invalidate(this);
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return an Iterator of Resources.
     */
    @Override
    public final synchronized Iterator<Resource> iterator() {
        if (isReference()) {
            return getRef().iterator();
        }
        dieOnCircularReference();
        return w.iterator();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return number of elements as int.
     */
    @Override
    public synchronized int size() {
        if (isReference()) {
            return getRef().size();
        }
        dieOnCircularReference();
        return w.size();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return whether this is a filesystem-only resource collection.
     */
    @Override
    public synchronized boolean isFilesystemOnly() {
        if (isReference()) {
            return getRef().isFilesystemOnly();
        }
        dieOnCircularReference();
        return w.isFilesystemOnly();
    }

    /**
     * Format this Restrict collection as a String.
     * @return the String value of this collection.
     */
    @Override
    public synchronized String toString() {
        if (isReference()) {
            return getRef().toString();
        }
        dieOnCircularReference();
        return w.toString();
    }

    @Override
    protected synchronized void dieOnCircularReference(Stack<Object> stk, Project p) {
        if (isChecked()) {
            return;
        }

        // takes care of Selectors
        super.dieOnCircularReference(stk, p);

        if (!isReference()) {
            pushAndInvokeCircularReferenceCheck(w, stk, p);
            setChecked(true);
        }
    }

    private Restrict getRef() {
        return getCheckedRef(Restrict.class);
    }
}
