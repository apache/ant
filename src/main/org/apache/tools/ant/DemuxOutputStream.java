/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;

/**
 * Logs content written by a thread and forwards the buffers onto the
 * project object which will forward the content to the appropriate
 * task.
 *
 * @since 1.4
 * @author Conor MacNeill
 */
public class DemuxOutputStream extends OutputStream {

    /**
     * A data class to store information about a buffer. Such information
     * is stored on a per-thread basis.
     */
    private static class BufferInfo {
        /**
         * The per-thread output stream.
         */
        private ByteArrayOutputStream buffer;

        /**
         * Indicates we have just seen a carriage return. It may be part of
         * a crlf pair or a single cr invoking processBuffer twice.
         */
         private boolean crSeen = false;
    }

    /** Maximum buffer size. */
    private static final int MAX_SIZE = 1024;

    /** Initial buffer size. */
    private static final int INTIAL_SIZE = 132;

    /** Carriage return */
    private static final int CR = 0x0d;

    /** Linefeed */
    private static final int LF = 0x0a;

    /** Mapping from thread to buffer (Thread to BufferInfo). */
    private Hashtable buffers = new Hashtable();

    /**
     * The project to send output to.
     */
    private Project project;

    /**
     * Whether or not this stream represents an error stream.
     */
    private boolean isErrorStream;

    /**
     * Creates a new instance of this class.
     *
     * @param project The project instance for which output is being
     *                demultiplexed. Must not be <code>null</code>.
     * @param isErrorStream <code>true</code> if this is the error string,
     *                      otherwise a normal output stream. This is
     *                      passed to the project so it knows
     *                      which stream it is receiving.
     */
    public DemuxOutputStream(Project project, boolean isErrorStream) {
        this.project = project;
        this.isErrorStream = isErrorStream;
    }

    /**
     * Returns the buffer associated with the current thread.
     *
     * @return a BufferInfo for the current thread to write data to
     */
    private BufferInfo getBufferInfo() {
        Thread current = Thread.currentThread();
        BufferInfo bufferInfo = (BufferInfo) buffers.get(current);
        if (bufferInfo == null) {
            bufferInfo = new BufferInfo();
            bufferInfo.buffer = new ByteArrayOutputStream(INTIAL_SIZE);
            bufferInfo.crSeen = false;
            buffers.put(current, bufferInfo);
        }
        return bufferInfo;
    }

    /**
     * Resets the buffer for the current thread.
     */
    private void resetBufferInfo() {
        Thread current = Thread.currentThread();
        BufferInfo bufferInfo = (BufferInfo) buffers.get(current);
        try {
            bufferInfo.buffer.close();
        } catch (IOException e) {
            // Shouldn't happen
        }
        bufferInfo.buffer = new ByteArrayOutputStream();
        bufferInfo.crSeen = false;
    }

    /**
     * Removes the buffer for the current thread.
     */
    private void removeBuffer() {
        Thread current = Thread.currentThread();
        buffers.remove (current);
    }

    /**
     * Writes the data to the buffer and flushes the buffer if a line
     * separator is detected or if the buffer has reached its maximum size.
     *
     * @param cc data to log (byte).
     * @exception IOException if the data cannot be written to the stream
     */
    public void write(int cc) throws IOException {
        final byte c = (byte) cc;

        BufferInfo bufferInfo = getBufferInfo();

        if (c == '\n') {
            // LF is always end of line (i.e. CRLF or single LF)
            bufferInfo.buffer.write(cc);
            processBuffer(bufferInfo.buffer);
        } else {
            if (bufferInfo.crSeen) {
                // CR without LF - send buffer then add char
                processBuffer(bufferInfo.buffer);
            }
            // add into buffer
            bufferInfo.buffer.write(cc);
        }
        bufferInfo.crSeen = (c == '\r');
        if (!bufferInfo.crSeen && bufferInfo.buffer.size() > MAX_SIZE) {
            processBuffer(bufferInfo.buffer);
        }
    }

    /**
     * Converts the buffer to a string and sends it to the project.
     *
     * @param buffer the ByteArrayOutputStream used to collect the output
     * until a line separator is seen.
     *
     * @see Project#demuxOutput(String,boolean)
     */
    protected void processBuffer(ByteArrayOutputStream buffer) {
        String output = buffer.toString();
        project.demuxOutput(output, isErrorStream);
        resetBufferInfo();
    }

    /**
     * Converts the buffer to a string and sends it to the project.
     *
     * @param buffer the ByteArrayOutputStream used to collect the output
     * until a line separator is seen.
     *
     * @see Project#demuxOutput(String,boolean)
     */
    protected void processFlush(ByteArrayOutputStream buffer) {
        String output = buffer.toString();
        project.demuxFlush(output, isErrorStream);
        resetBufferInfo();
    }

    /**
     * Equivalent to flushing the stream.
     *
     * @exception IOException if there is a problem closing the stream.
     *
     * @see #flush
     */
    public void close() throws IOException {
        flush();
        removeBuffer();
    }

    /**
     * Writes all remaining data in the buffer associated
     * with the current thread to the project.
     *
     * @exception IOException if there is a problem flushing the stream.
     */
    public void flush() throws IOException {
        BufferInfo bufferInfo = getBufferInfo();
        if (bufferInfo.buffer.size() > 0) {
            processFlush(bufferInfo.buffer);
        }
    }

    /**
     * Write a block of characters to the output stream
     *
     * @param b the array containing the data
     * @param off the offset into the array where data starts
     * @param len the length of block
     *
     * @throws IOException if the data cannot be written into the stream.
     */
    public void write(byte[] b, int off, int len) throws IOException {
        // find the line breaks and pass other chars through in blocks
        int offset = off;
        int blockStartOffset = offset;
        int remaining = len;
        BufferInfo bufferInfo = getBufferInfo();
        while (remaining > 0) {
            while (remaining > 0 && b[offset] != LF && b[offset] != CR) {
                offset++;
                remaining--;
            }
            // either end of buffer or a line separator char
            int blockLength = offset - blockStartOffset;
            if (blockLength > 0) {
                bufferInfo.buffer.write(b, blockStartOffset, blockLength);
            }
            while (remaining > 0 && (b[offset] == LF || b[offset] == CR)) {
                write(b[offset]);
                offset++;
                remaining--;
            }
            blockStartOffset = offset;
        }
    }
}
