/*
 * Copyright 2004 The Apache Software Foundation
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

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;

/**
 * Special <CODE>PipedInputStream</CODE> that will not die
 * when the writing <CODE>Thread</CODE> is no longer alive.
 */
public class LeadPipeInputStream extends PipedInputStream {
    private Task managingTask;

    /**
     * Construct a new <CODE>LeadPipeInputStream</CODE>.
     */
    public LeadPipeInputStream() {
        super();
    }

    /**
     * Construct a new <CODE>LeadPipeInputStream</CODE> to pull
     * from the specified <CODE>PipedOutputStream</CODE>.
     * @param src   the <CODE>PipedOutputStream</CODE> source.
     */
    public LeadPipeInputStream(PipedOutputStream src) throws IOException {
        super(src);
    }

    //inherit doc
    public synchronized int read() throws IOException {
        int result = -1;
        try {
            result = super.read();
        } catch (IOException eyeOhEx) {
            if ("write end dead".equalsIgnoreCase(eyeOhEx.getMessage())) {
                if (super.in > 0 && super.out < super.buffer.length
                    && super.out > super.in) {
                    result = super.buffer[super.out++] & 0xFF;
                }
            } else {
                log("error at LeadPipeInputStream.read():  "
                    + eyeOhEx.getMessage(), Project.MSG_INFO);
            }
        }
        return result;
    }

    /**
     * Set a managing <CODE>Task</CODE> for
     * this <CODE>LeadPipeInputStream</CODE>.
     * @param task   the managing <CODE>Task</CODE>.
     */
    public void setManagingTask(Task task) {
        this.managingTask = task;
    }

    /**
     * Log a message with the specified logging level.
     * @param message    the <CODE>String</CODE> message.
     * @param loglevel   the <CODE>int</CODE> logging level.
     */
    public void log(String message, int loglevel) {
        if (managingTask != null) {
            managingTask.log(message, loglevel);
        } else {
            if (loglevel > Project.MSG_WARN) {
                System.out.println(message);
            } else {
                System.err.println(message);
            }
        }
    }
}

