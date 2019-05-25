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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.util.PropertyOutputStream;

/**
 * Exposes an Ant property as a Resource.
 * @since Ant 1.7
 */
public class PropertyResource extends Resource {

    /** Magic number */
    private static final int PROPERTY_MAGIC
        = Resource.getMagicNumber("PropertyResource".getBytes());

    private static final InputStream UNSET = new InputStream() {
        @Override
        public int read() {
            return -1;
        }
    };

    /**
     * Default constructor.
     */
    public PropertyResource() {
    }

    /**
     * Construct a new PropertyResource with the specified name.
     * @param p the project to use.
     * @param n the String name of this PropertyResource (Ant property name/key).
     */
    public PropertyResource(Project p, String n) {
        super(n);
        setProject(p);
    }

    /**
     * Get the value of this PropertyResource.
     * @return the value of the specified Property.
     */
    public String getValue() {
        if (isReference()) {
            return getRef().getValue();
        }
        Project p = getProject();
        return p == null ? null : p.getProperty(getName());
    }

    /**
     * Get the Object value of this PropertyResource.
     * @return the Object value of the specified Property.
     * @since Ant 1.8.1
     */
    public Object getObjectValue() {
        if (isReference()) {
            return getRef().getObjectValue();
        }
        Project p = getProject();
        return p == null ? null : PropertyHelper.getProperty(p, getName());
    }

    /**
     * Find out whether this Resource exists.
     * @return true if the Property is set, false otherwise.
     */
    @Override
    public boolean isExists() {
        if (isReferenceOrProxy()) {
            return getReferencedOrProxied().isExists();
        }
        return getObjectValue() != null;
    }

    /**
     * Get the size of this Resource.
     * @return the size, as a long, 0 if the Resource does not exist (for
     *         compatibility with java.io.File), or UNKNOWN_SIZE if not known.
     */
    @Override
    public long getSize() {
        if (isReferenceOrProxy()) {
            return getReferencedOrProxied().getSize();
        }
        Object o = getObjectValue();
        return o == null ? 0L : (long) String.valueOf(o).length();
    }

    /**
     * Override to implement equality with equivalent Resources,
     * since we are capable of proxying them.
     * @param o object to compare
     * @return true if equal to o
     */
    @Override
    public boolean equals(Object o) {
        return super.equals(o) || isReferenceOrProxy() && getReferencedOrProxied().equals(o);
    }

    /**
     * Get the hash code for this Resource.
     * @return hash code as int.
     */
    @Override
    public int hashCode() {
        if (isReferenceOrProxy()) {
            return getReferencedOrProxied().hashCode();
        }
        return super.hashCode() * PROPERTY_MAGIC;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (isReferenceOrProxy()) {
            return getReferencedOrProxied().toString();
        }
        return getValue();
    }

    /**
     * Get an InputStream for the Resource.
     * @return an InputStream containing this Resource's content.
     * @throws IOException if unable to provide the content of this
     *         Resource as a stream.
     * @throws UnsupportedOperationException if InputStreams are not
     *         supported for this Resource type.
     */
    @Override
    public InputStream getInputStream() throws IOException {
        if (isReferenceOrProxy()) {
            return getReferencedOrProxied().getInputStream();
        }
        Object o = getObjectValue();
        return o == null ? UNSET : new ByteArrayInputStream(String.valueOf(o).getBytes());
    }

    /**
     * Get an OutputStream for the Resource.
     * @return an OutputStream to which content can be written.
     * @throws IOException if unable to provide the content of this
     *         Resource as a stream.
     * @throws UnsupportedOperationException if OutputStreams are not
     *         supported for this Resource type.
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        if (isReferenceOrProxy()) {
            return getReferencedOrProxied().getOutputStream();
        }
        if (isExists()) {
            throw new ImmutableResourceException();
        }
        return new PropertyOutputStream(getProject(), getName());
    }

    /**
     * Learn whether this PropertyResource either refers to another Resource
     * or proxies another Resource due to its object property value being said Resource.
     * @return boolean
     */
    protected boolean isReferenceOrProxy() {
        return isReference() || getObjectValue() instanceof Resource;
    }

    /**
     * Get the referenced or proxied Resource, if applicable.
     * @return Resource
     * @throws IllegalStateException if this PropertyResource neither proxies nor
     *                               references another Resource.
     */
    protected Resource getReferencedOrProxied() {
        if (isReference()) {
            return super.getRef();
        }
        Object o = getObjectValue();
        if (o instanceof Resource) {
            return (Resource) o;
        }
        throw new IllegalStateException(
                "This PropertyResource does not reference or proxy another Resource");
    }

    @Override
    protected PropertyResource getRef() {
        return getCheckedRef(PropertyResource.class);
    }
}
