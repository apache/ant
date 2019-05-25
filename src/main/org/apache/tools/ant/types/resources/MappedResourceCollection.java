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
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.MergingMapper;

/**
 * Wrapper around a resource collections that maps the names of the
 * other collection using a configured mapper.
 * @since Ant 1.8.0
 */
public class MappedResourceCollection
        extends DataType implements ResourceCollection, Cloneable {

    private ResourceCollection nested = null;
    private Mapper mapper = null;
    private boolean enableMultipleMappings = false;
    private boolean cache = false;
    private Collection<Resource> cachedColl = null;

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
        cachedColl = null;
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
        cachedColl = null;
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
     * Set method of handling mappers that return multiple
     * mappings for a given source path.
     * @param enableMultipleMappings If true the type will
     *        use all the mappings for a given source path, if
     *        false, only the first mapped name is
     *        processed.
     *        By default, this setting is false to provide backward
     *        compatibility with earlier releases.
     * @since Ant 1.8.1
     */
    public void setEnableMultipleMappings(boolean enableMultipleMappings) {
        this.enableMultipleMappings = enableMultipleMappings;
    }

    /**
     * Set whether to cache collections.
     * @param cache boolean
     * @since Ant 1.8.1
     */
    public void setCache(boolean cache) {
        this.cache = cache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFilesystemOnly() {
        if (isReference()) {
            return getRef().isFilesystemOnly();
        }
        checkInitialized();
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        if (isReference()) {
            return getRef().size();
        }
        checkInitialized();
        return cacheCollection().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Resource> iterator() {
        if (isReference()) {
            return getRef().iterator();
        }
        checkInitialized();
        return cacheCollection().iterator();
    }

    /**
     * Overrides the base version.
     * @param r the Reference to set.
     */
    @Override
    public void setRefid(Reference r) {
        if (nested != null || mapper != null) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Implement clone.  The nested resource collection and mapper are copied.
     * @return a cloned instance.
     */
    @Override
    public Object clone() {
        try {
            MappedResourceCollection c =
                (MappedResourceCollection) super.clone();
            c.nested = nested;
            c.mapper = mapper;
            c.cachedColl = null;
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
    @Override
    protected synchronized void dieOnCircularReference(Stack<Object> stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            checkInitialized();
            if (mapper != null) {
                pushAndInvokeCircularReferenceCheck(mapper, stk, p);
            }
            if (nested instanceof DataType) {
                pushAndInvokeCircularReferenceCheck((DataType) nested, stk, p);
            }
            setChecked(true);
        }
    }

    private void checkInitialized() {
        if (nested == null) {
            throw new BuildException(
                "A nested resource collection element is required",
                getLocation());
        }
        dieOnCircularReference();
    }

    private synchronized Collection<Resource> cacheCollection() {
        if (cachedColl == null || !cache) {
            cachedColl = getCollection();
        }
        return cachedColl;
    }

    private Collection<Resource> getCollection() {
        FileNameMapper m =
            mapper == null ? new IdentityMapper() : mapper.getImplementation();

        Stream<MappedResource> stream;
        if (enableMultipleMappings) {
            stream = nested.stream()
                .flatMap(r -> Stream.of(m.mapFileName(r.getName()))
                    .filter(Objects::nonNull)
                    .map(MergingMapper::new)
                    .map(mm -> new MappedResource(r, mm)));
        } else {
            stream = nested.stream().map(r -> new MappedResource(r, m));
        }
        return stream.collect(Collectors.toList());
    }

    /**
     * Format this resource collection as a String.
     * @return a descriptive <code>String</code>.
     */
    @Override
    public String toString() {
        if (isReference()) {
            return getRef().toString();
        }
        return isEmpty() ? "" : stream().map(Object::toString)
            .collect(Collectors.joining(File.pathSeparator));
    }

    private MappedResourceCollection getRef() {
        return getCheckedRef(MappedResourceCollection.class);
    }
}
