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

    // should only be instantiated via factory, this also means we
    // don't have to think about being a reference to a different
    // resource

    /**
     * Wraps an existing resource.
     */
    protected MappedResource(Resource r) {
        wrapped = r;
        isAppendable = wrapped.as(Appendable.class) != null;
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
        return super.as(clazz);
    }

    public static MappedResource map(Resource r) {
        if (r instanceof FileProvider) {
            if (r instanceof Touchable) {
                return new TouchableFileProviderMR(r);
            }
            return new FileProviderMR(r);
        }
        if (r instanceof Touchable) {
            return new TouchableMR(r);
        }
        // no special interface
        return new MappedResource(r);
    }

    private static class FileProviderMR extends MappedResource
        implements FileProvider {
        private final FileProvider p;

        protected FileProviderMR(Resource r) {
            super(r);
            if (!(r instanceof FileProvider)) {
                throw new IllegalArgumentException("trying to wrap something "
                                                   + "that is not a "
                                                   + " FileProvider");
            }
            p = (FileProvider) r;
        }

        /**
         * delegated to the wrapped resource.
         */
        public File getFile() {
            return p.getFile();
        }
    }

    private static class TouchableMR extends MappedResource
        implements Touchable {
        private final Touchable t;

        protected TouchableMR(Resource r) {
            super(r);
            if (!(r instanceof Touchable)) {
                throw new IllegalArgumentException("trying to wrap something "
                                                   + "that is not a "
                                                   + " Touchable");
            }
            t = (Touchable) r;
        }

        /**
         * delegated to the wrapped resource.
         */
        public void touch(long m) {
            t.touch(m);
        }
    }

    private static class TouchableFileProviderMR extends FileProviderMR
        implements Touchable {
        private final Touchable t;

        protected TouchableFileProviderMR(Resource r) {
            super(r);
            if (!(r instanceof Touchable)) {
                throw new IllegalArgumentException("trying to wrap something "
                                                   + "that is not a "
                                                   + " Touchable");
            }
            t = (Touchable) r;
        }

        /**
         * delegated to the wrapped resource.
         */
        public void touch(long m) {
            t.touch(m);
        }
    }

}