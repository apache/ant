/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant;

import java.io.*;
import java.util.*;


/**
 * Logs content written by a thread and forwards the buffers onto the
 * project object which will forward the content to the appropriate
 * task 
 *
 * @author Conor MacNeill
 */
public class DemuxOutputStream extends OutputStream {

    static private final int MAX_SIZE = 1024;
    
    private Hashtable buffers = new Hashtable();
//    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private boolean skip = false;
    private Project project;
    private boolean isErrorStream;
    
    /**
     * Creates a new instance of this class.
     *
     * @param task the task for whom to log
     * @param level loglevel used to log data written to this stream.
     */
    public DemuxOutputStream(Project project, boolean isErrorStream) {
        this.project = project;
        this.isErrorStream = isErrorStream;
    }

    private ByteArrayOutputStream getBuffer() {
        Thread current = Thread.currentThread();
        ByteArrayOutputStream buffer = (ByteArrayOutputStream)buffers.get(current);
        if (buffer == null) {
            buffer = new ByteArrayOutputStream();
            buffers.put(current, buffer);
        }
        return buffer;
    }

    private void resetBuffer() {    
        Thread current = Thread.currentThread();
        buffers.remove(current);
    }
    
    /**
     * Write the data to the buffer and flush the buffer, if a line
     * separator is detected.
     *
     * @param cc data to log (byte).
     */
    public void write(int cc) throws IOException {
        final byte c = (byte)cc;
        if ((c == '\n') || (c == '\r')) {
            if (!skip) {
                processBuffer();
            }
        } else {
            ByteArrayOutputStream buffer = getBuffer();
            buffer.write(cc);
            if (buffer.size() > MAX_SIZE) {
                processBuffer();
            }
        }
        skip = (c == '\r');
    }


    /**
     * Converts the buffer to a string and sends it to <code>processLine</code>
     */
    protected void processBuffer() {
        String output = getBuffer().toString();
        project.demuxOutput(output, isErrorStream);
        resetBuffer();
    }

    /**
     * Writes all remaining
     */
    public void close() throws IOException {
        flush();
    }

    /**
     * Writes all remaining
     */
    public void flush() throws IOException {
        if (getBuffer().size() > 0) {
            processBuffer();
        }
    }
}
