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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * A resource that transforms the content of another resource.
 *
 * <p>Wraps around another resource, delegates all queries (except
 * getSize) to that other resource but transforms stream content
 * on the fly.</p>
 *
 * @since Ant 1.8
 */
public abstract class ContentTransformingResource extends ResourceDecorator {

    private static final int BUFFER_SIZE = 8192;

    /** no arg constructor */
    protected ContentTransformingResource() {
    }

    /**
     * Constructor with another resource to wrap.
     * @param other the resource to wrap.
     */
    protected ContentTransformingResource(final ResourceCollection other) {
        super(other);
    }

    /**
     * Get the size of this Resource.
     * @return the size, as a long, 0 if the Resource does not exist (for
     *         compatibility with java.io.File), or UNKNOWN_SIZE if not known.
     */
    @Override
    public long getSize() {
        if (isExists()) {
            try (InputStream in = getInputStream()) {
                final byte[] buf = new byte[BUFFER_SIZE];
                int size = 0;
                int readNow;
                while ((readNow = in.read(buf, 0, buf.length)) > 0) {
                    size += readNow;
                }
                return size;
            } catch (final IOException ex) {
                throw new BuildException(
                    "caught exception while reading " + getName(), ex);
            }
        }
        return 0;
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
        InputStream in = getResource().getInputStream();
        if (in != null) {
            in = wrapStream(in);
        }
        return in;
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
        OutputStream out = getResource().getOutputStream();
        if (out != null) {
            out = wrapStream(out);
        }
        return out;
    }

    /**
     * Suppress FileProvider, re-implement Appendable
     */
    @Override
    public <T> T as(final Class<T> clazz) {
        if (Appendable.class.isAssignableFrom(clazz)) {
            if (isAppendSupported()) {
                final Appendable a = getResource().as(Appendable.class);
                if (a != null) {
                    return clazz.cast((Appendable) () -> {
                        OutputStream out = a.getAppendOutputStream();
                        return out == null ? null : wrapStream(out);
                    });
                }
            }
            return null;
        }
        return FileProvider.class.isAssignableFrom(clazz)
            ? null : getResource().as(clazz);
    }

    /**
     * Learn whether the transformation performed allows appends.
     *
     * <p>In general compressed outputs will become invalid if they
     * are appended to, for example.</p>
     *
     * <p>This implementations returns false.</p>
     *
     * @return boolean false
     */
    protected boolean isAppendSupported() {
        return false;
    }

    /**
     * Get a content-filtering/transforming InputStream.
     *
     * @param in InputStream to wrap, will never be null.
     * @return a compressed InputStream.
     * @throws IOException if there is a problem.
     */
    protected abstract InputStream wrapStream(InputStream in)
        throws IOException;

    /**
     * Get a content-filtering/transforming OutputStream.
     *
     * @param out OutputStream to wrap, will never be null.
     * @return a compressed OutputStream.
     * @throws IOException if there is a problem.
     */
    protected abstract OutputStream wrapStream(OutputStream out)
        throws IOException;

}
