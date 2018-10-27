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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * Reads a resource as text document and creates a resource for each
 * line.
 * @since Ant 1.8.0
 */
public class ResourceList extends DataType implements ResourceCollection {
    private final Vector<FilterChain> filterChains = new Vector<>();
    private final ArrayList<ResourceCollection> textDocuments = new ArrayList<>();
    private final Union cachedResources = new Union();
    private volatile boolean cached = false;
    private String encoding = null;

    public ResourceList() {
        cachedResources.setCache(true);
    }

    /**
     * Adds a source.
     */
    public void add(ResourceCollection rc) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        textDocuments.add(rc);
        setChecked(false);
    }

    /**
     * Adds a FilterChain.
     */
    public final void addFilterChain(FilterChain filter) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        filterChains.add(filter);
        setChecked(false);
    }

    /**
     * Encoding to use for input, defaults to the platform's default
     * encoding. <p>
     *
     * For a list of possible values see
     * <a href="http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html">
     * http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html
     * </a>.</p>
     */
    public final void setEncoding(String encoding) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.encoding = encoding;
    }

    /**
     * Makes this instance in effect a reference to another ResourceList
     * instance.
     */
    @Override
    public void setRefid(Reference r) throws BuildException {
        if (encoding != null) {
            throw tooManyAttributes();
        }
        if (!(filterChains.isEmpty() && textDocuments.isEmpty())) {
            throw noChildrenAllowed();
        }
        super.setRefid(r);
    }

    /**
     * Fulfill the ResourceCollection contract. The Iterator returned
     * will throw ConcurrentModificationExceptions if ResourceCollections
     * are added to this container while the Iterator is in use.
     * @return a "fail-fast" Iterator.
     */
    @Override
    public final synchronized Iterator<Resource> iterator() {
        if (isReference()) {
            return getCheckedRef().iterator();
        }
        return cache().iterator();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return number of elements as int.
     */
    @Override
    public synchronized int size() {
        if (isReference()) {
            return getCheckedRef().size();
        }
        return cache().size();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return whether this is a filesystem-only resource collection.
     */
    @Override
    public synchronized boolean isFilesystemOnly() {
        if (isReference()) {
            return getCheckedRef().isFilesystemOnly();
        }
        return cache().isFilesystemOnly();
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
            for (ResourceCollection resourceCollection : textDocuments) {
                if (resourceCollection instanceof DataType) {
                    pushAndInvokeCircularReferenceCheck((DataType) resourceCollection, stk, p);
                }
            }
            for (FilterChain filterChain : filterChains) {
                pushAndInvokeCircularReferenceCheck(filterChain, stk, p);
            }
            setChecked(true);
        }
    }

    @Override
    protected ResourceList getCheckedRef() {
        return (ResourceList) super.getCheckedRef();
    }

    private synchronized ResourceCollection cache() {
        if (!cached) {
            dieOnCircularReference();
            textDocuments.stream().flatMap(ResourceCollection::stream)
                .map(this::read).forEach(cachedResources::add);
            cached = true;
        }
        return cachedResources;
    }

    private ResourceCollection read(Resource r) {
        try (BufferedReader reader = new BufferedReader(open(r))) {
            Union streamResources = new Union();
            streamResources.setCache(true);
            reader.lines().map(this::parse).forEach(streamResources::add);
            return streamResources;
        } catch (final IOException ioe) {
            throw new BuildException("Unable to read resource " + r.getName()
                                     + ": " + ioe, ioe, getLocation());
        }
    }

    private Reader open(Resource r) throws IOException {
        ChainReaderHelper crh = new ChainReaderHelper();
        crh.setPrimaryReader(new InputStreamReader(
            new BufferedInputStream(r.getInputStream()), encoding == null
                ? Charset.defaultCharset() : Charset.forName(encoding)));
        crh.setFilterChains(filterChains);
        crh.setProject(getProject());
        return crh.getAssembledReader();
    }

    private Resource parse(final String line) {
        PropertyHelper propertyHelper =
            PropertyHelper.getPropertyHelper(getProject());
        Object expanded = propertyHelper.parseProperties(line);
        if (expanded instanceof Resource) {
            return (Resource) expanded;
        }
        String expandedLine = expanded.toString();
        int colon = expandedLine.indexOf(':');
        if (colon >= 0) {
            // could be an URL or an absolute file on an OS with drives
            try {
                return new URLResource(expandedLine);
            } catch (BuildException mfe) {
                // a translated MalformedURLException

                // probably it's an absolute path fall back to file
                // resource
            }
        }
        return new FileResource(getProject(), expandedLine);
    }
    
}
