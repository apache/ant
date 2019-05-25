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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Task;

/**
 * Special <code>InputStream</code> that will
 * concatenate the contents of an array of files.
 */
public class ConcatFileInputStream extends InputStream {

    private static final int EOF = -1;
    private int currentIndex = -1;
    private boolean eof = false;
    private File[] file;
    private InputStream currentStream;
    private ProjectComponent managingPc;

  /**
   * Construct a new <code>ConcatFileInputStream</code>
   * with the specified <code>File[]</code>.
   * @param file   <code>File[]</code>.
   * @throws IOException if I/O errors occur.
   */
    public ConcatFileInputStream(File[] file) throws IOException {
        this.file = file;
    }

    /**
     * Close the stream.
     * @throws IOException if there is an error.
     */
    @Override
    public void close() throws IOException {
        closeCurrent();
        eof = true;
    }

    /**
     * Read a byte.
     * @return the byte (0 - 255) or -1 if this is the end of the stream.
     * @throws IOException if there is an error.
     */
    @Override
    public int read() throws IOException {
        int result = readCurrent();
        if (result == EOF && !eof) {
            openFile(++currentIndex);
            result = readCurrent();
        }
        return result;
    }

    /**
     * Set a managing <code>Task</code> for
     * this <code>ConcatFileInputStream</code>.
     * @param task   the managing <code>Task</code>.
     */
    public void setManagingTask(Task task) {
        setManagingComponent(task);
    }

    /**
     * Set a managing <code>Task</code> for
     * this <code>ConcatFileInputStream</code>.
     * @param pc the managing <code>Task</code>.
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

    private int readCurrent() throws IOException {
        return (eof || currentStream == null) ? EOF : currentStream.read();
    }

    private void openFile(int index) throws IOException {
        closeCurrent();
        if (file != null && index < file.length) {
            log("Opening " + file[index], Project.MSG_VERBOSE);
            try {
                currentStream = new BufferedInputStream(
                    Files.newInputStream(file[index].toPath()));
            } catch (IOException eyeOhEx) {
                log("Failed to open " + file[index], Project.MSG_ERR);
                throw eyeOhEx;
            }
        } else {
            eof = true;
        }
    }

    private void closeCurrent() {
        FileUtils.close(currentStream);
        currentStream = null;
    }
}
