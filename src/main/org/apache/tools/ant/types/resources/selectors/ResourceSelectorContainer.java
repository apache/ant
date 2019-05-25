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
package org.apache.tools.ant.types.resources.selectors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;

/**
 * ResourceSelector container.
 * @since Ant 1.7
 */
public class ResourceSelectorContainer extends DataType {

    private final List<ResourceSelector> resourceSelectors = new ArrayList<>();

    /**
     * Default constructor.
     */
    public ResourceSelectorContainer() {
    }

    /**
     * Construct a new ResourceSelectorContainer with the specified array of selectors.
     * @param resourceSelectors the ResourceSelector[] to add.
     */
    public ResourceSelectorContainer(ResourceSelector... resourceSelectors) {
        for (ResourceSelector rsel : resourceSelectors) {
            add(rsel);
        }
    }

    /**
     * Add a ResourceSelector to the container.
     * @param s the ResourceSelector to add.
     */
    public void add(ResourceSelector s) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (s == null) {
            return;
        }
        resourceSelectors.add(s);
        setChecked(false);
    }

    /**
     * Learn whether this ResourceSelectorContainer has selectors.
     * @return boolean indicating whether selectors have been added to the container.
     */
    public boolean hasSelectors() {
        if (isReference()) {
            return getRef().hasSelectors();
        }
        dieOnCircularReference();
        return !resourceSelectors.isEmpty();
    }

    /**
     * Get the count of nested selectors.
     * @return the selector count as int.
     */
    public int selectorCount() {
        if (isReference()) {
            return getRef().selectorCount();
        }
        dieOnCircularReference();
        return resourceSelectors.size();
    }

    /**
     * Return an Iterator over the nested selectors.
     * @return Iterator of ResourceSelectors.
     */
    public Iterator<ResourceSelector> getSelectors() {
        if (isReference()) {
            return getRef().getSelectors();
        }
        return getResourceSelectors().iterator();
    }

    /**
     * Get the configured {@link ResourceSelector}s as a {@link List}.
     * @return {@link List} of {@link ResourceSelector}
     */
    public List<ResourceSelector> getResourceSelectors() {
        if (isReference()) {
            return getRef().getResourceSelectors();
        }
        dieOnCircularReference();
        return Collections.unmodifiableList(resourceSelectors);
    }

    /**
     * Overrides the version from DataType to recurse on nested ResourceSelectors.
     * @param stk the Stack of references.
     * @param p   the Project to resolve against.
     * @throws BuildException on error.
     */
    protected void dieOnCircularReference(Stack<Object> stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            for (ResourceSelector resourceSelector : resourceSelectors) {
                if (resourceSelector instanceof DataType) {
                    pushAndInvokeCircularReferenceCheck((DataType) resourceSelector, stk, p);
                }
            }
            setChecked(true);
        }
    }

    private ResourceSelectorContainer getRef() {
        return getCheckedRef(ResourceSelectorContainer.class);
    }
}
