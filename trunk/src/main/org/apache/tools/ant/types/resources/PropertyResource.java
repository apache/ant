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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;

import org.apache.tools.ant.Project;
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
        Project p = getProject();
        return p == null ? null : p.getProperty(getName());
    }

    /**
     * Find out whether this Resource exists.
     * @return true if the Property is set, false otherwise.
     */
    public boolean isExists() {
        return getValue() != null;
    }

    /**
     * Get the size of this Resource.
     * @return the size, as a long, 0 if the Resource does not exist (for
     *         compatibility with java.io.File), or UNKNOWN_SIZE if not known.
     */
    public long getSize() {
        if (isReference()) {
            return ((Resource) getCheckedRef()).getSize();
        }
        return isExists() ? (long) getValue().length() : 0L;
    }

    /**
     * Get the hash code for this Resource.
     * @return hash code as int.
     */
    public int hashCode() {
        if (isReference()) {
            return getCheckedRef().hashCode();
        }
        return super.hashCode() * PROPERTY_MAGIC;
    }

    /**
     * Get the string.
     *
     * @return the string contents of the resource.
     * @since Ant 1.7
     */
    public String toString() {
        if (isReference()) {
            return getCheckedRef().toString();
        }
        return String.valueOf(getValue());
    }

    /**
     * Get an InputStream for the Resource.
     * @return an InputStream containing this Resource's content.
     * @throws IOException if unable to provide the content of this
     *         Resource as a stream.
     * @throws UnsupportedOperationException if InputStreams are not
     *         supported for this Resource type.
     */
    public InputStream getInputStream() throws IOException {
        if (isReference()) {
            return ((Resource) getCheckedRef()).getInputStream();
        }
        return isExists() ? new ByteArrayInputStream(getValue().getBytes()) : UNSET;
    }

    /**
     * Get an OutputStream for the Resource.
     * @return an OutputStream to which content can be written.
     * @throws IOException if unable to provide the content of this
     *         Resource as a stream.
     * @throws UnsupportedOperationException if OutputStreams are not
     *         supported for this Resource type.
     */
    public OutputStream getOutputStream() throws IOException {
        if (isReference()) {
            return ((Resource) getCheckedRef()).getOutputStream();
        }
        if (isExists()) {
            throw new ImmutableResourceException();
        }
        return new PropertyOutputStream(getProject(), getName());
    }

}
