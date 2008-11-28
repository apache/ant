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

import java.util.Iterator;
import java.util.Stack;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.IdentityMapper;

/**
 * Wrapper around a resource collections that maps the names of the
 * other collection using a configured mapper.
 * @since Ant 1.8.0
 */
public class MappedResourceCollection
    extends DataType implements ResourceCollection, Cloneable {

    private ResourceCollection nested = null;
    private Mapper mapper = null;

    /**
     * Adds the required nested ResourceCollection.
     * @param c the ResourceCollection to add.
     * @throws BuildException on error.
     */
    public synchronized void add(ResourceCollection c) throws BuildException {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (nested != null) {
            throw new BuildException("Only one resource collection can be"
                                     + " nested into mappedresources",
                                     getLocation());
        }
        setChecked(false);
        nested = c;
    }

    /**
     * Define the mapper to map source to destination files.
     * @return a mapper to be configured.
     * @exception BuildException if more than one mapper is defined.
     */
    public Mapper createMapper() throws BuildException {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (mapper != null) {
            throw new BuildException("Cannot define more than one mapper",
                                     getLocation());
        }
        setChecked(false);
        mapper = new Mapper(getProject());
        return mapper;
    }

    /**
     * Add a nested filenamemapper.
     * @param fileNameMapper the mapper to add.
     * @since Ant 1.6.3
     */
    public void add(FileNameMapper fileNameMapper) {
        createMapper().add(fileNameMapper);
    }

    /**
     * @return false
     */
    public boolean isFilesystemOnly() {
        if (isReference()) {
            return ((MappedResourceCollection) getCheckedRef())
                .isFilesystemOnly();
        }
        checkInitialized();
        return false;
    }

    /**
     * @return size of the nested resource collection.
     */
    public int size() {
        if (isReference()) {
            return ((MappedResourceCollection) getCheckedRef()).size();
        }
        checkInitialized();
        return nested.size();
    }

    public Iterator iterator() {
        if (isReference()) {
            return ((MappedResourceCollection) getCheckedRef()).iterator();
        }
        checkInitialized();
        return new MappedIterator(nested.iterator(), mapper);
    }

    /**
     * Overrides the base version.
     * @param r the Reference to set.
     */
    public void setRefid(Reference r) {
        if (nested != null || mapper != null) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Implement clone.  The nested resource collection and mapper are
     * copied.
     * @return a cloned instance.
     */
    public Object clone() {
        try {
            MappedResourceCollection c =
                (MappedResourceCollection) super.clone();
            c.nested = nested;
            c.mapper = mapper;
            return c;
        } catch (CloneNotSupportedException e) {
            throw new BuildException(e);
        }
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
            checkInitialized();
            if (mapper != null) {
                stk.push(mapper);
                invokeCircularReferenceCheck(mapper, stk, p);
                stk.pop();
            }
            if (nested instanceof DataType) {
                stk.push(nested);
                invokeCircularReferenceCheck((DataType) nested, stk, p);
                stk.pop();
            }
            setChecked(true);
        }
    }

    private void checkInitialized() {
        if (nested == null) {
            throw new BuildException("A nested resource collection element is"
                                     + " required", getLocation());
        }
        dieOnCircularReference();
    }

    private static class MappedIterator implements Iterator {
        private final Iterator sourceIterator;
        private final FileNameMapper mapper;

        private MappedIterator(Iterator source, Mapper m) {
            sourceIterator = source;
            if (m != null) {
                mapper = m.getImplementation();
            } else {
                mapper = new IdentityMapper();
            }
        }

        public boolean hasNext() {
            return sourceIterator.hasNext();
        }

        public Object next() {
            return new MappedResource((Resource) sourceIterator.next(),
                                      mapper);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}