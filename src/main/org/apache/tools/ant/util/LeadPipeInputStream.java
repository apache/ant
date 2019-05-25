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

package org.apache.tools.ant.util;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Task;

/**
 * Special <code>PipedInputStream</code> that will not die
 * when the writing <code>Thread</code> is no longer alive.
 * @since Ant 1.6.2
 */
public class LeadPipeInputStream extends PipedInputStream {
    private static final int BYTE_MASK = 0xFF;
    private ProjectComponent managingPc;

    /**
     * Construct a new <code>LeadPipeInputStream</code>.
     */
    public LeadPipeInputStream() {
        super();
    }

    /**
     * Construct a new <code>LeadPipeInputStream</code>
     * with the specified buffer size.
     * @param size   the size of the circular buffer.
     */
    public LeadPipeInputStream(int size) {
        super();
        setBufferSize(size);
    }

    /**
     * Construct a new <code>LeadPipeInputStream</code> to pull
     * from the specified <code>PipedOutputStream</code>.
     * @param src   the <code>PipedOutputStream</code> source.
     * @throws IOException if unable to construct the stream.
     */
    public LeadPipeInputStream(PipedOutputStream src) throws IOException {
        super(src);
    }

    /**
     * Construct a new <code>LeadPipeInputStream</code> to pull
     * from the specified <code>PipedOutputStream</code>, using a
     * circular buffer of the specified size.
     * @param src    the <code>PipedOutputStream</code> source.
     * @param size   the size of the circular buffer.
     * @throws IOException if there is an error.
     */
    public LeadPipeInputStream(PipedOutputStream src, int size) throws IOException {
        super(src);
        setBufferSize(size);
    }

    //inherit doc
    /**
     * Read a byte from the stream.
     * @return the byte (0 to 255) or -1 if there are no more.
     * @throws IOException if there is an error.
     */
    @Override
    public synchronized int read() throws IOException {
        int result = -1;
        try {
            result = super.read();
        } catch (IOException eyeOhEx) {
            String msg = eyeOhEx.getMessage();
            if ("write end dead".equalsIgnoreCase(msg)
                    || "pipe broken".equalsIgnoreCase(msg)) {
                if (super.in > 0 && super.out < super.buffer.length
                    && super.out > super.in) {
                    result = super.buffer[super.out++] & BYTE_MASK;
                }
            } else {
                log("error at LeadPipeInputStream.read():  " + msg,
                    Project.MSG_INFO);
            }
        }
        return result;
    }

    /**
     * Set the size of the buffer.
     * @param size   the new buffer size.  Ignored if &lt;= current size.
     */
    public synchronized void setBufferSize(int size) {
        if (size > buffer.length) {
            byte[] newBuffer = new byte[size];
            if (in >= 0) {
                if (in > out) {
                    System.arraycopy(buffer, out, newBuffer, out, in - out);
                } else {
                    int outlen = buffer.length - out;
                    System.arraycopy(buffer, out, newBuffer, 0, outlen);
                    System.arraycopy(buffer, 0, newBuffer, outlen, in);
                    in += outlen;
                    out = 0;
                }
            }
            buffer = newBuffer;
        }
    }

    /**
     * Set a managing <code>Task</code> for
     * this <code>LeadPipeInputStream</code>.
     * @param task   the managing <code>Task</code>.
     */
    public void setManagingTask(Task task) {
        setManagingComponent(task);
    }

    /**
     * Set a managing <code>ProjectComponent</code> for
     * this <code>LeadPipeInputStream</code>.
     * @param pc   the managing <code>ProjectComponent</code>.
     */
    public void setManagingComponent(ProjectComponent pc) {
        this.managingPc = pc;
    }

    /**
     * Log a message with the specified logging level.
     * @param message    the <code>String</code> message.
     * @param loglevel   the <code>int</code> logging level.
     */
    public void log(String message, int loglevel) {
        if (managingPc != null) {
            managingPc.log(message, loglevel);
        } else {
            if (loglevel > Project.MSG_WARN) {
                System.out.println(message);
            } else {
                System.err.println(message);
            }
        }
    }
}

