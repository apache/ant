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
import java.io.OutputStream;

/**
 * A simple T-piece to replicate an output stream into two separate streams
 *
 */
public class TeeOutputStream extends OutputStream {
    private OutputStream left;
    private OutputStream right;

    /**
     * Constructor for TeeOutputStream.
     * @param left one of the output streams.
     * @param right the other output stream.
     */
    public TeeOutputStream(OutputStream left, OutputStream right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Close both output streams.
     * @throws IOException on error.
     */
    @Override
    public void close() throws IOException {
        try {
            left.close();
        } finally {
            right.close();
        }
    }

    /**
     * Flush both output streams.
     * @throws IOException on error
     */
    @Override
    public void flush() throws IOException {
        left.flush();
        right.flush();
    }

    /**
     * Write a byte array to both output streams.
     * @param b an array of bytes.
     * @throws IOException on error.
     */
    @Override
    public void write(byte[] b) throws IOException {
        left.write(b);
        right.write(b);
    }

    /**
     * Write a byte array to both output streams.
     * @param b     the data.
     * @param off   the start offset in the data.
     * @param len   the number of bytes to write.
     * @throws IOException on error.
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        left.write(b, off, len);
        right.write(b, off, len);
    }

    /**
     * Write a byte to both output streams.
     * @param b the byte to write.
     * @throws IOException on error.
     */
    @Override
    public void write(int b) throws IOException {
        left.write(b);
        right.write(b);
    }
}
