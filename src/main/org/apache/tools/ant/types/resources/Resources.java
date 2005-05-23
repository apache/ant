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
import java.util.Vector;
import java.util.Iterator;
import java.util.Collection;
import java.util.AbstractCollection;
import java.util.NoSuchElementException;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * Generic ResourceCollection: Either stores nested ResourceCollections,
 * making no attempt to remove duplicates, or references another ResourceCollection.
 * @since Ant 1.7
 */
public class Resources extends DataType implements ResourceCollection {

    private class MyCollection extends AbstractCollection {
        int size;

        MyCollection() {
            size = 0;
            for (Iterator rci = rc.iterator(); rci.hasNext();) {
                size += ((ResourceCollection) rci.next()).size();
            }
        }
        public int size() {
            return size;
        }
        public Iterator iterator() {
            return new MyIterator();
        }
        private class MyIterator implements Iterator {
            Iterator rci = rc.iterator();
            Iterator ri = null;
            public boolean hasNext() {
                if ((ri == null || !ri.hasNext()) && rci.hasNext()) {
                    ri = ((ResourceCollection) rci.next()).iterator();
                }
                return ri != null && ri.hasNext();
            }
            public Object next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return ri.next();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
    }

    private Vector rc = null;
    private Collection coll = null;

    /**
     * Add a ResourceCollection.
     * @param c the ResourceCollection to add.
     */
    public synchronized void add(ResourceCollection c) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        rc = (rc == null) ? new Vector() : rc;
        rc.add(c);
        FailFast.invalidate(this);
        coll = null;
        setChecked(false);
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return an Iterator of Resources.
     */
    public synchronized Iterator iterator() {
        if (isReference()) {
            return getRef().iterator();
        }
        validate();
        return new FailFast(this, coll.iterator());
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return number of elements as int.
     */
    public synchronized int size() {
        if (isReference()) {
            return getRef().size();
        }
        validate();
        return coll.size();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return true if all Resources represent files.
     */
    public boolean isFilesystemOnly() {
        if (isReference()) {
            return getRef().isFilesystemOnly();
        }
        validate();
        //first the easy way, if all children are filesystem-only, return true:
        boolean goEarly = true;
        for (Iterator i = rc.iterator(); goEarly && i.hasNext();) {
            goEarly &= ((ResourceCollection) i.next()).isFilesystemOnly();
        }
        if (goEarly) {
            return true;
        }
        /* now check each Resource in case the child only
           lets through files from any children IT may have: */
        for (Iterator i = coll.iterator(); i.hasNext();) {
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
    protected void dieOnCircularReference(Stack stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            for (Iterator i = rc.iterator(); i.hasNext();) {
                Object o = i.next();
                if (o instanceof DataType) {
                    invokeCircularReferenceCheck((DataType) o, stk, p);
                }
            }
            setChecked(true);
        }
    }

    /**
     * Resolves references, allowing any ResourceCollection.
     * @return the referenced ResourceCollection.
     */
    private ResourceCollection getRef() {
        return (ResourceCollection) getCheckedRef(
            ResourceCollection.class, "ResourceCollection");
    }

    private synchronized void validate() {
        dieOnCircularReference();
        if (rc == null || rc.size() == 0) {
            throw new BuildException("Resources:  no resources specified.");
        }
        coll = (coll == null) ? new MyCollection() : coll;
    }

}
