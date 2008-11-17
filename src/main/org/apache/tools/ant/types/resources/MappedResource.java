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
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.util.FileNameMapper;

/**
 * A decorator around a different resource that uses a mapper to
 * dynamically remap the resource's name.
 *
 * <p>Strips the FileProvider interface from decorated resources since
 * it may be used to circumvent name mapping.</p>
 *
 * <p>Overwrites all setters to throw exceptions.</p>
 *
 * @since Ant 1.8.0
 */
public class MappedResource extends Resource {
    private final Resource wrapped;
    private final FileNameMapper mapper;
    private final boolean isAppendable;
    private final boolean isTouchable;

    // should only be instantiated via factory, this also means we
    // don't have to think about being a reference to a different
    // resource

    /**
     * Wraps an existing resource.
     */
    public MappedResource(Resource r, FileNameMapper m) {
        wrapped = r;
        mapper = m;
        isAppendable = wrapped.as(Appendable.class) != null;
        isTouchable = wrapped.as(Touchable.class) != null;
    }

    /**
     * Maps the name.
     */
    public String getName() {
        String[] mapped = mapper.mapFileName(wrapped.getName());
        return mapped != null && mapped.length > 0 ? mapped[0] : null;
    }

    /**
     * Not supported.
     */
    public void setName(String name) {
        throw new BuildException(new ImmutableResourceException());
    }

    /**
     * delegated to the wrapped resource.
     */
    public boolean isExists() {
        return wrapped.isExists();
    }

    /**
     * Not supported.
     */
    public void setExists(boolean exists) {
        throw new BuildException(new ImmutableResourceException());
    }

    /**
     * delegated to the wrapped resource.
     */
    public long getLastModified() {
        return wrapped.getLastModified();
    }

    /**
     * Not supported.
     */
    public void setLastModified(long lastmodified) {
        throw new BuildException(new ImmutableResourceException());
    }

    /**
     * delegated to the wrapped resource.
     */
    public boolean isDirectory() {
        return wrapped.isDirectory();
    }

    /**
     * Not supported.
     */
    public void setDirectory(boolean directory) {
        throw new BuildException(new ImmutableResourceException());
    }

    /**
     * delegated to the wrapped resource.
     */
    public long getSize() {
        return wrapped.getSize();
    }

    /**
     * Not supported.
     */
    public void setSize(long size) {
        throw new BuildException(new ImmutableResourceException());
    }

    /**
     * delegated to the wrapped resource.
     */
    public InputStream getInputStream() throws IOException {
        return wrapped.getInputStream();
    }

    /**
     * delegated to the wrapped resource.
     */
    public OutputStream getOutputStream() throws IOException {
        return wrapped.getOutputStream();
    }

    /**
     * delegated to the wrapped resource.
     */
    public boolean isFilesystemOnly() {
        return wrapped.isFilesystemOnly();
    }

    public String toString() {
        return "mapped " + wrapped.toString();
    }

    /**
     * Not supported.
     */
    public void setRefid(Reference r) {
        throw new UnsupportedOperationException();
    }

    public Object as(Class clazz) {
        return FileProvider.class.isAssignableFrom(clazz) 
            ? null : wrapped.as(clazz);
    }

}