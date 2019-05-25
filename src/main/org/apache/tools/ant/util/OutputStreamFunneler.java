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
 * Manages a set of <code>OutputStream</code>s to
 * write to a single underlying stream, which is
 * closed only when the last &quot;funnel&quot;
 * has been closed.
 */
public class OutputStreamFunneler {

    /**
     * Default timeout.
     * @see #setTimeout(long)
     */
    public static final long DEFAULT_TIMEOUT_MILLIS = 1000;

    private final class Funnel extends OutputStream {
        private boolean closed = false;

        private Funnel() {
            synchronized (OutputStreamFunneler.this) {
                ++count;
            }
        }

        @Override
        public void flush() throws IOException {
            synchronized (OutputStreamFunneler.this) {
                dieIfClosed();
                out.flush();
            }
        }

        @Override
        public void write(int b) throws IOException {
            synchronized (OutputStreamFunneler.this) {
                dieIfClosed();
                out.write(b);
            }
        }

        @Override
        public void write(byte[] b) throws IOException {
            synchronized (OutputStreamFunneler.this) {
                dieIfClosed();
                out.write(b);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            synchronized (OutputStreamFunneler.this) {
                dieIfClosed();
                out.write(b, off, len);
            }
        }

        @Override
        public void close() throws IOException {
            release(this);
        }
    }

    private OutputStream out;
    private int count = 0;
    private boolean closed;
    private long timeoutMillis;

    /**
     * Create a new <code>OutputStreamFunneler</code> for
     * the specified <code>OutputStream</code>.
     * @param out   <code>OutputStream</code>.
     */
    public OutputStreamFunneler(OutputStream out) {
        this(out, DEFAULT_TIMEOUT_MILLIS);
    }

    /**
     * Create a new <code>OutputStreamFunneler</code> for
     * the specified <code>OutputStream</code>, with the
     * specified timeout value.
     * @param out             <code>OutputStream</code>.
     * @param timeoutMillis   <code>long</code>.
     * @see #setTimeout(long)
     */
    public OutputStreamFunneler(OutputStream out, long timeoutMillis) {
        if (out == null) {
            throw new IllegalArgumentException(
                "OutputStreamFunneler.<init>:  out == null");
        }
        this.out = out;
        this.closed = false; //as far as we know
        setTimeout(timeoutMillis);
    }

    /**
     * Set the timeout for this <code>OutputStreamFunneler</code>.
     * This is the maximum time that may elapse between the closure
     * of the last &quot;funnel&quot; and the next call to
     * <code>getOutputStream()</code> without closing the
     * underlying stream.
     * @param timeoutMillis   <code>long</code> timeout value.
     */
    public synchronized void setTimeout(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Get a &quot;funnel&quot; <code>OutputStream</code> instance to
     * write to this <code>OutputStreamFunneler</code>'s underlying
     * <code>OutputStream</code>.
     * @return <code>OutputStream</code>.
     * @throws IOException if unable to create the funnel.
     */
    public synchronized OutputStream getFunnelInstance()
        throws IOException {
        dieIfClosed();
        try {
            return new Funnel();
        } finally {
            notifyAll();
        }
    }

    private synchronized void release(Funnel funnel) throws IOException {
        //ignore release of an already-closed funnel
        if (!funnel.closed) {
            try {
                if (timeoutMillis > 0) {
                    final long start = System.currentTimeMillis();
                    final long end = start + timeoutMillis;
                    long now = System.currentTimeMillis();
                    try {
                        while (now < end) {
                            wait(end - now);
                            now = System.currentTimeMillis();
                        }
                    } catch (InterruptedException eyeEx) {
                        //ignore
                    }
                }
                if (--count == 0) {
                    close();
                }
            } finally {
                funnel.closed = true;
            }
        }
   }

    private synchronized void close() throws IOException {
        try {
            dieIfClosed();
            out.close();
        } finally {
            closed = true;
        }
    }

    private synchronized void dieIfClosed() throws IOException {
        if (closed) {
            throw new IOException("The funneled OutputStream has been closed.");
        }
    }

}
