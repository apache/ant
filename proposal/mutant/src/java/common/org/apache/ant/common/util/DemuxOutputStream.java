/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
package org.apache.ant.common.util;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.io.OutputStream;
import java.util.Hashtable;

/**
 * Buffers content written per thread and forwards the buffers onto the
 * given receiver
 *
 * @author Conor MacNeill
 * @created 22 February 2002
 */
public class DemuxOutputStream extends OutputStream {

    /**
     * A data class to store information about a buffer. Such informatio is
     * stored on a per-thread basis.
     *
     * @author Conor MacNeill
     * @created 22 February 2002
     */
    private static class BufferInfo {
        /** The per-thread output stream  */
        private ByteArrayOutputStream buffer;

        /**
         * Whether the next line-terminator should be skipped in terms of
         * processing the buffer or not. Used to avoid \r\n invoking
         * processBuffer twice.
         */
        private boolean skip = false;
    }

    /** Maximum buffer size */
    private static final int MAX_SIZE = 1024;
    /** Mapping from thread to buffer (Thread to BufferInfo) */
    private Hashtable buffers = new Hashtable();

    /** The object which receives the output  */
    private DemuxOutputReceiver receiver;

    /** Whether or not this stream represents an error stream  */
    private boolean isErrorStream;

    /**
     * Creates a new instance of this class.
     *
     * @param isErrorStream true if this is the error string, otherwise a
     *      normal output stream. This is passed to the project so it knows
     *      which stream it is receiving.
     * @param receiver The receiver to which demux'd content is sent.
     */
    public DemuxOutputStream(DemuxOutputReceiver receiver,
        boolean isErrorStream) {
        this.receiver = receiver;
        this.isErrorStream = isErrorStream;
    }

    /**
     * Writes the data to the buffer and flushes the buffer if a line
     * separator is detected or if the buffer has reached its maximum size.
     *
     * @param cc data to log (byte).
     * @exception IOException if the data cannot be written to the stream
     */
    public void write(int cc) throws IOException {
        final byte c = (byte)cc;

        BufferInfo bufferInfo = getBufferInfo();
        if ((c == '\n') || (c == '\r')) {
            if (!bufferInfo.skip) {
                processBuffer(bufferInfo.buffer);
            }
        } else {
            bufferInfo.buffer.write(cc);
            if (bufferInfo.buffer.size() > MAX_SIZE) {
                processBuffer(bufferInfo.buffer);
            }
        }
        bufferInfo.skip = (c == '\r');
    }

    /**
     * Equivalent to calling {@link #flush flush} on the stream.
     *
     * @exception IOException if there is a problem closing the stream.
     */
    public void close() throws IOException {
        flush();
    }

    /**
     * Writes all remaining data in the buffer associated with the current
     * thread to the project.
     *
     * @exception IOException if there is a problem flushing the stream.
     */
    public void flush() throws IOException {
        BufferInfo bufferInfo = getBufferInfo();
        if (bufferInfo.buffer.size() > 0) {
            processBuffer(bufferInfo.buffer);
        }
    }


    /**
     * Converts the buffer to a string and sends it to {@link
     * Project#demuxOutput(String,boolean) Project.demuxOutput}.
     *
     * @param buffer the ByteArrayOutputStream used to collect the output
     *      until a line separator is seen.
     */
    protected void processBuffer(ByteArrayOutputStream buffer) {
        String output = buffer.toString();
        receiver.threadOutput(output, isErrorStream);
        resetBufferInfo();
    }

    /**
     * Returns the buffer associated with the current thread.
     *
     * @return a ByteArrayOutputStream for the current thread to write data
     *      to
     */
    private BufferInfo getBufferInfo() {
        Thread current = Thread.currentThread();
        BufferInfo bufferInfo = (BufferInfo)buffers.get(current);
        if (bufferInfo == null) {
            bufferInfo = new BufferInfo();
            bufferInfo.buffer = new ByteArrayOutputStream();
            bufferInfo.skip = false;
            buffers.put(current, bufferInfo);
        }
        return bufferInfo;
    }

    /** Resets the buffer for the current thread.  */
    private void resetBufferInfo() {
        Thread current = Thread.currentThread();
        buffers.remove(current);
    }
}

