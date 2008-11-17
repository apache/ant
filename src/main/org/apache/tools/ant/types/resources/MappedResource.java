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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;

/**
 * A decorator around a different resource that uses a mapper to
 * dynamically remap the resource's name.
 *
 * <p>Does not change any of the other methods, in particular
 * getFile() does not map the file name.</p>
 *
 * <p>Overwrites all setters to throw exceptions.</p>
 *
 * @since Ant 1.8.0
 */
public class MappedResource extends Resource {
    private final Resource wrapped;
    private final boolean isAppendable;
    private final boolean isTouchable;

    // should only be instantiated via factory, this also means we
    // don't have to think about being a reference to a different
    // resource

    /**
     * Wraps an existing resource.
     */
    protected MappedResource(Resource r) {
        wrapped = r;
        isAppendable = wrapped.as(Appendable.class) != null;
        isTouchable = wrapped.as(Touchable.class) != null;
    }

    /**
     * Maps the name.
     */
    public String getName() {
        return wrapped.getName();
    }

    /**
     * Not supported.
     */
    public void setName(String name) {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
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
        if (clazz == Appendable.class && isAppendable) {
            return new Appendable() {
                public OutputStream getAppendOutputStream() throws IOException {
                    return ((Appendable) wrapped.as(Appendable.class))
                        .getAppendOutputStream();
                }
            };
        }
        if (clazz == Touchable.class && isTouchable) {
            return new Touchable() {
                public void touch(long modTime) {
                    ((Touchable) wrapped.as(Touchable.class)).touch(modTime);
                }
            };
        }
        return super.as(clazz);
    }

    public static MappedResource map(Resource r) {
        return r.as(FileProvider.class) != null
            ? new FileProviderMR(r) : new MappedResource(r);
    }

    private static class FileProviderMR extends MappedResource
        implements FileProvider {
        private final FileProvider p;

        protected FileProviderMR(Resource r) {
            super(r);
            p = (FileProvider) r.as(FileProvider.class);
            if (p == null) {
                throw new IllegalArgumentException("trying to wrap something "
                                                   + "that is not adaptable to "
                                                   + " FileProvider");
            }
        }

        /**
         * delegated to the wrapped resource.
         */
        public File getFile() {
            return p.getFile();
        }
    }

}