/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
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
 * @author Magesh Umasankar
 */
public abstract class BaseFilterReader
    extends FilterReader {
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
        super(new StringReader(new String()));
        try {
            close();
        } catch (IOException  ioe) {
            // Ignore
        }
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
    public final int read(final char cbuf[], final int off,
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
    public final long skip(final long n) throws IOException {
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
            
        StringBuffer line = new StringBuffer();
                
        while (ch != -1) {
            line.append ((char) ch);
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
        return FileUtils.readFully(in, 8192);
    }
}
