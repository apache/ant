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
import org.apache.tools.ant.util.FileUtils;

/**
 * Reads a resource as text document and creates a resource for each
 * line.
 * @since Ant 1.8.0
 */
public class ResourceList extends DataType implements ResourceCollection {
    private final Vector<FilterChain> filterChains = new Vector<FilterChain>();
    private final ArrayList<ResourceCollection> textDocuments = new ArrayList<ResourceCollection>();
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
    public void setRefid(Reference r) throws BuildException {
        if (encoding != null) {
            throw tooManyAttributes();
        }
        if (filterChains.size() > 0 || textDocuments.size() > 0) {
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
    public final synchronized Iterator<Resource> iterator() {
        if (isReference()) {
            return ((ResourceList) getCheckedRef()).iterator();
        }
        return cache().iterator();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return number of elements as int.
     */
    public synchronized int size() {
        if (isReference()) {
            return ((ResourceList) getCheckedRef()).size();
        }
        return cache().size();
    }

    /**
     * Fulfill the ResourceCollection contract.
     * @return whether this is a filesystem-only resource collection.
     */
    public synchronized boolean isFilesystemOnly() {
        if (isReference()) {
            return ((ResourceList) getCheckedRef()).isFilesystemOnly();
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

    private synchronized ResourceCollection cache() {
        if (!cached) {
            dieOnCircularReference();
            for (ResourceCollection rc : textDocuments) {
                for (Resource r : rc) {
                    cachedResources.add(read(r));
                }
            }
            cached = true;
        }
        return cachedResources;
    }

    private ResourceCollection read(Resource r) {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(r.getInputStream());
            Reader input = null;
            if (encoding == null) {
                input = new InputStreamReader(bis);
            } else {
                input = new InputStreamReader(bis, encoding);
            }
            ChainReaderHelper crh = new ChainReaderHelper();
            crh.setPrimaryReader(input);
            crh.setFilterChains(filterChains);
            crh.setProject(getProject());
            BufferedReader reader = new BufferedReader(crh.getAssembledReader());

            Union streamResources = new Union();
            streamResources.setCache(true);

            String line = null;
            while ((line = reader.readLine()) != null) {
                streamResources.add(parse(line));
            }

            return streamResources;
        } catch (final IOException ioe) {
            throw new BuildException("Unable to read resource " + r.getName()
                                     + ": " + ioe, ioe, getLocation());
        } finally {
            FileUtils.close(bis);
        }
    }

    private Resource parse(final String line) {
        PropertyHelper propertyHelper
            = (PropertyHelper) PropertyHelper.getPropertyHelper(getProject());
        Object expanded = propertyHelper.parseProperties(line);
        if (expanded instanceof Resource) {
            return (Resource) expanded;
        }
        String expandedLine = expanded.toString();
        int colon = expandedLine.indexOf(":");
        if (colon != -1) {
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
