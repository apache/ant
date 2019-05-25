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
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;

/**
 * Special <code>InputStream</code> that will
 * concatenate the contents of Resources from a single ResourceCollection.
 * @since Ant 1.7
 */
public class ConcatResourceInputStream extends InputStream {

    private static final int EOF = -1;
    private boolean eof = false;
    private Iterator<Resource> iter;
    private InputStream currentStream;
    private ProjectComponent managingPc;
    private boolean ignoreErrors = false;

  /**
   * Construct a new ConcatResourceInputStream
   * for the specified ResourceCollection.
   * @param rc the ResourceCollection to combine.
   */
    public ConcatResourceInputStream(ResourceCollection rc) {
        iter = rc.iterator();
    }

    /**
     * Set whether this ConcatResourceInputStream ignores errors.
     * @param b whether to ignore errors.
     */
    public void setIgnoreErrors(boolean b) {
        ignoreErrors = b;
    }

    /**
     * Find out whether this ConcatResourceInputStream ignores errors.
     * @return boolean ignore-errors flag.
     */
    public boolean isIgnoreErrors() {
        return ignoreErrors;
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
        if (eof) {
            return EOF;
        }
        int result = readCurrent();
        if (result == EOF) {
            nextResource();
            result = readCurrent();
        }
        return result;
    }

    /**
     * Set a managing <code>ProjectComponent</code> for
     * this <code>ConcatResourceInputStream</code>.
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
            (loglevel > Project.MSG_WARN ? System.out : System.err).println(message);
        }
    }

    private int readCurrent() throws IOException {
        return eof || currentStream == null ? EOF : currentStream.read();
    }

    private void nextResource() throws IOException {
        closeCurrent();
        while (iter.hasNext()) {
            Resource r = iter.next();
            if (!r.isExists()) {
                continue;
            }
            log("Concatenating " + r.toLongString(), Project.MSG_VERBOSE);
            try {
                currentStream = new BufferedInputStream(r.getInputStream());
                return;
            } catch (IOException eyeOhEx) {
                if (!ignoreErrors) {
                    log("Failed to get input stream for " + r, Project.MSG_ERR);
                    throw eyeOhEx;
                }
            }
        }
        eof = true;
    }

    private void closeCurrent() {
        FileUtils.close(currentStream);
        currentStream = null;
    }
}
