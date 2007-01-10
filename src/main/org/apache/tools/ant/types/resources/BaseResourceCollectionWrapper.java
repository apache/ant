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

import java.io.File;
import java.util.Stack;
import java.util.Iterator;
import java.util.Collection;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * Base class for a ResourceCollection that wraps a single nested
 * ResourceCollection.
 * @since Ant 1.7
 */
public abstract class BaseResourceCollectionWrapper
    extends DataType implements ResourceCollection, Cloneable {
    private static final String ONE_NESTED_MESSAGE
        = " expects exactly one nested resource collection.";

    private ResourceCollection rc;
    private Collection coll = null;
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
    public final synchronized Iterator iterator() {
        if (isReference()) {
            return ((BaseResourceCollectionWrapper) getCheckedRef()).iterator();
        }
        dieOnCircularReference();
        return new FailFast(this, cacheCollection().iterator());
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return number of elements as int.
     */
    public synchronized int size() {
        if (isReference()) {
            return ((BaseResourceCollectionWrapper) getCheckedRef()).size();
        }
        dieOnCircularReference();
        return cacheCollection().size();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return whether this is a filesystem-only resource collection.
     */
    public synchronized boolean isFilesystemOnly() {
        if (isReference()) {
            return ((BaseResourceCollectionContainer) getCheckedRef()).isFilesystemOnly();
        }
        dieOnCircularReference();

        if (rc == null || rc.isFilesystemOnly()) {
            return true;
        }
        /* now check each Resource in case the child only
           lets through files from any children IT may have: */
        for (Iterator i = cacheCollection().iterator(); i.hasNext();) {
            if (!(i.next() instanceof FileResource)) {
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
    protected synchronized void dieOnCircularReference(Stack stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            if (rc instanceof DataType) {
                stk.push(rc);
                invokeCircularReferenceCheck((DataType) rc, stk, p);
                stk.pop();
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
     * Template method for subclasses to return a Collection of Resources.
     * @return Collection.
     */
    protected abstract Collection getCollection();

    /**
     * Format this BaseResourceCollectionWrapper as a String.
     * @return a descriptive <code>String</code>.
     */
    public synchronized String toString() {
        if (isReference()) {
            return getCheckedRef().toString();
        }
        if (cacheCollection().size() == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (Iterator i = coll.iterator(); i.hasNext();) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparatorChar);
            }
            sb.append(i.next());
        }
        return sb.toString();
    }

    private synchronized Collection cacheCollection() {
        if (coll == null || !isCache()) {
            coll = getCollection();
        }
        return coll;
    }

    private BuildException oneNested() {
        return new BuildException(super.toString() + ONE_NESTED_MESSAGE);
    }

}
