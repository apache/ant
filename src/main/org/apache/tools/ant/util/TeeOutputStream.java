/*
 * Copyright  2003-2004 The Apache Software Foundation
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

import java.io.OutputStream;
import java.io.IOException;

/**
 * A simple T-piece to replicate an output stream into two separate streams
 *
 */
public class TeeOutputStream extends OutputStream {
    private OutputStream left;
    private OutputStream right;

    public TeeOutputStream(OutputStream left, OutputStream right) {
        this.left = left;
        this.right = right;
    }

    public void close() throws IOException {
        left.close();
        right.close();
    }

    public void flush() throws IOException {
        left.flush();
        right.flush();
    }

    public void write(byte[] b) throws IOException {
        left.write(b);
        right.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        left.write(b, off, len);
        right.write(b, off, len);
    }

    public void write(int b) throws IOException {
        left.write(b);
        right.write(b);
    }
}

