/*
 * Copyright  2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.apache.tools.ant.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Class that delays opening the output file until the first bytes
 * shall be written or the method {@link #open open} has been invoked
 * explicitly.
 *
 * @since Ant 1.6
 */
public class LazyFileOutputStream extends OutputStream {

    private FileOutputStream fos;
    private File file;
    private boolean append;
    private boolean alwaysCreate;
    private boolean opened = false;
    private boolean closed = false;

    /**
     * Creates a stream that will eventually write to the file with
     * the given name and replace it.
     */
    public LazyFileOutputStream(String name) {
        this(name, false);
    }

    /**
     * Creates a stream that will eventually write to the file with
     * the given name and optionally append to instead of replacing
     * it.
     */
    public LazyFileOutputStream(String name, boolean append) {
        this(new File(name), append);
    }

    /**
     * Creates a stream that will eventually write to the file with
     * the given name and replace it.
     */
    public LazyFileOutputStream(File f) {
        this(f, false);
    }

    /**
     * Creates a stream that will eventually write to the file with
     * the given name and optionally append to instead of replacing
     * it.
     */
    public LazyFileOutputStream(File file, boolean append) {
        this(file, append, false);
    }

    /**
     * Creates a stream that will eventually write to the file with
     * the given name, optionally append to instead of replacing
     * it, and optionally always create a file (even if zero length).
     */
    public LazyFileOutputStream(File file, boolean append,
                                boolean alwaysCreate) {
        this.file = file;
        this.append = append;
        this.alwaysCreate = alwaysCreate;
    }

    /**
     * Explicitly open the file for writing.
     *
     * <p>Returns silently if the file has already been opened.</p>
     */
    public void open() throws IOException {
        ensureOpened();
    }

    public synchronized void close() throws IOException {
        if (alwaysCreate) {
            ensureOpened();
        }
        if (opened) {
            fos.close();
        }
        closed = true;
    }

    /**
     * Delegates to the three arg version.
     */
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public synchronized void write(byte[] b, int offset, int len)
        throws IOException {
        ensureOpened();
        fos.write(b, offset, len);
    }

    public synchronized void write(int b) throws IOException {
        ensureOpened();
        fos.write(b);
    }

    private synchronized void ensureOpened() throws IOException {
        if (closed) {
            throw new IOException(file + " has already been closed.");
        }

        if (!opened) {
            fos = new FileOutputStream(file.getAbsolutePath(), append);
            opened = true;
        }
    }
}
