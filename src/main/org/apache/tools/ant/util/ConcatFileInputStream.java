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

import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.FileInputStream;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;

/**
 * Special <CODE>InputStream</CODE> that will
 * concatenate the contents of an array of files.
 */
public class ConcatFileInputStream extends InputStream {

    private static final int EOF = -1;
    private int currentIndex = -1;
    private boolean eof = false;
    private File[] file;
    private InputStream currentStream;
    private Task managingTask;

  /**
   * Construct a new <CODE>ConcatFileInputStream</CODE>
   * with the specified <CODE>File[]</CODE>.
   * @param file   <CODE>File[]</CODE>.
   * @throws IOException if I/O errors occur.
   */
    public ConcatFileInputStream(File[] file) throws IOException {
        this.file = file;
    }

    // inherit doc
    public void close() throws IOException {
        closeCurrent();
        eof = true;
    }

    // inherit doc
    public int read() throws IOException {
        int result = readCurrent();
        if (result == EOF && !eof) {
            openFile(++currentIndex);
            result = readCurrent();
        }
        return result;
    }

    /**
     * Set a managing <CODE>Task</CODE> for
     * this <CODE>ConcatFileInputStream</CODE>.
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

    private int readCurrent() throws IOException {
        return (eof || currentStream == null) ? EOF : currentStream.read();
    }

    private void openFile(int index) throws IOException {
        closeCurrent();
        if (file != null && index < file.length) {
            log("Opening " + file[index], Project.MSG_VERBOSE);
            try {
                currentStream = new BufferedInputStream(
                    new FileInputStream(file[index]));
            } catch (IOException eyeOhEx) {
                log("Failed to open " + file[index], Project.MSG_ERR);
                throw eyeOhEx;
            }
        } else {
            eof = true;
        }
    }

    private void closeCurrent() {
        if (currentStream != null) {
            try {
                currentStream.close();
            } catch (IOException eyeOhEx) {
            }
            currentStream = null;
        }
    }
}

