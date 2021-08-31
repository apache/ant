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
package org.apache.tools.ant.filters;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;

/**
 * Base class for core filter readers.
 *
 */
public abstract class BaseFilterReader extends FilterReader {
    /** Buffer size used when reading */
    private static final int BUFFER_SIZE = 8192;

    /** Have the parameters passed been interpreted? */
    private boolean initialized = false;

    /** The Ant project this filter is part of. */
    private Project project = null;

    /**
     * Constructor used by Ant's introspection mechanism.
     * The original filter reader is only used for chaining
     * purposes, never for filtering purposes (and indeed
     * it would be useless for filtering purposes, as it has
     * no real data to filter). ChainedReaderHelper uses
     * this placeholder instance to create a chain of real filters.
     */
    public BaseFilterReader() {
        super(new StringReader(""));
        FileUtils.close(this);
    }

    /**
     * Creates a new filtered reader.
     *
     * @param in A Reader object providing the underlying stream.
     *           Must not be <code>null</code>.
     *
     */
    public BaseFilterReader(final Reader in) {
        super(in);
    }

    /**
     * Reads characters into a portion of an array.  This method will block
     * until some input is available, an I/O error occurs, or the end of the
     * stream is reached.
     *
     * @param      cbuf  Destination buffer to write characters to.
     *                   Must not be <code>null</code>.
     * @param      off   Offset at which to start storing characters.
     * @param      len   Maximum number of characters to read.
     *
     * @return     the number of characters read, or -1 if the end of the
     *             stream has been reached
     *
     * @exception  IOException  If an I/O error occurs
     */
    public final int read(final char[] cbuf, final int off,
                          final int len) throws IOException {
        for (int i = 0; i < len; i++) {
            final int ch = read();
            if (ch == -1) {
                if (i == 0) {
                    return -1;
                } else {
                    return i;
                }
            }
            cbuf[off + i] = (char) ch;
        }
        return len;
    }

    /**
     * Skips characters.  This method will block until some characters are
     * available, an I/O error occurs, or the end of the stream is reached.
     *
     * @param  n  The number of characters to skip
     *
     * @return    the number of characters actually skipped
     *
     * @exception  IllegalArgumentException  If <code>n</code> is negative.
     * @exception  IOException  If an I/O error occurs
     */
    public final long skip(final long n)
        throws IOException, IllegalArgumentException {
        if (n < 0L) {
            throw new IllegalArgumentException("skip value is negative");
        }

        for (long i = 0; i < n; i++) {
            if (read() == -1) {
                return i;
            }
        }
        return n;
    }

    /**
     * Sets the initialized status.
     *
     * @param initialized Whether or not the filter is initialized.
     */
    protected final void setInitialized(final boolean initialized) {
        this.initialized = initialized;
    }

    /**
     * Returns the initialized status.
     *
     * @return whether or not the filter is initialized
     */
    protected final boolean getInitialized() {
        return initialized;
    }

    /**
     * Sets the project to work with.
     *
     * @param project The project this filter is part of.
     *                Should not be <code>null</code>.
     */
    public final void setProject(final Project project) {
        this.project = project;
    }

    /**
     * Returns the project this filter is part of.
     *
     * @return the project this filter is part of
     */
    protected final Project getProject() {
        return project;
    }

    /**
     * Reads a line of text ending with '\n' (or until the end of the stream).
     * The returned String retains the '\n'.
     *
     * @return the line read, or <code>null</code> if the end of the stream
     * has already been reached
     *
     * @exception IOException if the underlying reader throws one during
     *                        reading
     */
    protected final String readLine() throws IOException {
        int ch = in.read();

        if (ch == -1) {
            return null;
        }

        final StringBuilder line = new StringBuilder();

        while (ch != -1) {
            line.append((char) ch);
            if (ch == '\n') {
                break;
            }
            ch = in.read();
        }
        return line.toString();
    }

    /**
     * Reads to the end of the stream, returning the contents as a String.
     *
     * @return the remaining contents of the reader, as a String
     *
     * @exception IOException if the underlying reader throws one during
     *            reading
     */
    protected final String readFully() throws IOException {
        return FileUtils.readFully(in, BUFFER_SIZE);
    }
}
