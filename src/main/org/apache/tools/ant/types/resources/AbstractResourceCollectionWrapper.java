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

import java.io.File;
import java.util.Iterator;
import java.util.Stack;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * Base class for a ResourceCollection that wraps a single nested
 * ResourceCollection.
 * @since Ant 1.8.2
 */
public abstract class AbstractResourceCollectionWrapper
    extends DataType implements ResourceCollection, Cloneable {
    private static final String ONE_NESTED_MESSAGE
        = " expects exactly one nested resource collection.";

    private ResourceCollection rc;
    private boolean cache = true;

    /**
     * Set whether to cache collections.
     * @param b boolean cache flag.
     */
    public synchronized void setCache(boolean b) {
        cache = b;
    }

    /**
     * Learn whether to cache collections. Default is <code>true</code>.
     * @return boolean cache flag.
     */
    public synchronized boolean isCache() {
        return cache;
    }

    /**
     * Add a ResourceCollection to the container.
     * @param c the ResourceCollection to add.
     * @throws BuildException on error.
     */
    public synchronized void add(ResourceCollection c) throws BuildException {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (c == null) {
            return;
        }
        if (rc != null) {
            throw oneNested();
        }
        rc = c;
        if (Project.getProject(rc) == null) {
            Project p = getProject();
            if (p != null) {
                p.setProjectReference(rc);
            }
        }
        setChecked(false);
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
        return new FailFast(this, createIterator());
    }

    /**
     * Do create an iterator on the resource collection. The creation
     * of the iterator is allowed to not be thread safe whereas the iterator
     * itself should. The returned iterator will be wrapped into the FailFast
     * one.
     *
     * @return the iterator on the resource collection
     */
    protected abstract Iterator<Resource> createIterator();

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
        return getSize();
    }

    /**
     * Do compute the size of the resource collection. The implementation of
     * this function is allowed to be not thread safe.
     *
     * @return size of resource collection.
     */
    protected abstract int getSize();

    /**
     * Fulfill the ResourceCollection contract.
     * @return whether this is a filesystem-only resource collection.
     */
    public synchronized boolean isFilesystemOnly() {
        if (isReference()) {
            return getRef().isFilesystemOnly();
        }
        dieOnCircularReference();

        if (rc == null || rc.isFilesystemOnly()) {
            return true;
        }
        /* now check each Resource in case the child only
           lets through files from any children IT may have: */
        for (Resource r : this) {
            if (r.as(FileProvider.class) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Overrides the version of DataType to recurse on all DataType
     * child elements that may have been added.
     * @param stk the stack of data types to use (recursively).
     * @param p   the project to use to dereference the references.
     * @throws BuildException on error.
     */
    protected synchronized void dieOnCircularReference(Stack<Object> stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            if (rc instanceof DataType) {
                pushAndInvokeCircularReferenceCheck((DataType) rc, stk, p);
            }
            setChecked(true);
        }
    }

    /**
     * Get the nested ResourceCollection.
     * @return a ResourceCollection.
     * @throws BuildException if no nested ResourceCollection has been provided.
     */
    protected final synchronized ResourceCollection getResourceCollection() {
        dieOnCircularReference();
        if (rc == null) {
            throw oneNested();
        }
        return rc;
    }

    /**
     * Format this AbstractResourceCollectionWrapper as a String.
     * @return a descriptive <code>String</code>.
     */
    @Override
    public synchronized String toString() {
        if (isReference()) {
            return getRef().toString();
        }
        if (isEmpty()) {
            return "";
        }
        return stream().map(Object::toString)
            .collect(Collectors.joining(File.pathSeparator));
    }

    private AbstractResourceCollectionWrapper getRef() {
        return getCheckedRef(AbstractResourceCollectionWrapper.class);
    }

    private BuildException oneNested() {
        return new BuildException(super.toString() + ONE_NESTED_MESSAGE);
    }

}
