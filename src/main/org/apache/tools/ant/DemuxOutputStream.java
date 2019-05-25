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

package org.apache.tools.ant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.WeakHashMap;

import org.apache.tools.ant.util.FileUtils;

/**
 * Logs content written by a thread and forwards the buffers onto the
 * project object which will forward the content to the appropriate
 * task.
 *
 * @since 1.4
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
    private static final int INITIAL_SIZE = 132;

    /** Carriage return */
    private static final int CR = 0x0d;

    /** Linefeed */
    private static final int LF = 0x0a;

    /** Mapping from thread to buffer (Thread to BufferInfo). */
    private WeakHashMap<Thread, BufferInfo> buffers = new WeakHashMap<>();

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
        return buffers.computeIfAbsent(current, x -> {
            BufferInfo bufferInfo = new BufferInfo();
            bufferInfo.buffer = new ByteArrayOutputStream(INITIAL_SIZE);
            bufferInfo.crSeen = false;
            return bufferInfo;
        });
    }

    /**
     * Resets the buffer for the current thread.
     */
    private void resetBufferInfo() {
        Thread current = Thread.currentThread();
        BufferInfo bufferInfo = buffers.get(current);
        FileUtils.close(bufferInfo.buffer);
        bufferInfo.buffer = new ByteArrayOutputStream();
        bufferInfo.crSeen = false;
    }

    /**
     * Removes the buffer for the current thread.
     */
    private void removeBuffer() {
        Thread current = Thread.currentThread();
        buffers.remove(current);
    }

    /**
     * Writes the data to the buffer and flushes the buffer if a line
     * separator is detected or if the buffer has reached its maximum size.
     *
     * @param cc data to log (byte).
     * @exception IOException if the data cannot be written to the stream
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
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
