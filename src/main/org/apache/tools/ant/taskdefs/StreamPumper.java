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
package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.util.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Copies all data from an input stream to an output stream.
 *
 * @since Ant 1.2
 */
public class StreamPumper implements Runnable {

    private static final int SMALL_BUFFER_SIZE = 128;

    private final InputStream is;
    private final OutputStream os;
    private volatile boolean askedToStop;
    private volatile boolean finished;
    private final boolean closeWhenExhausted;
    private boolean autoflush = false;
    private Exception exception = null;
    private int bufferSize = SMALL_BUFFER_SIZE;
    private boolean started = false;
    private final boolean useAvailable;
    private PostStopHandle postStopHandle;

    /**
     * Create a new StreamPumper.
     *
     * @param is input stream to read data from
     * @param os output stream to write data to.
     * @param closeWhenExhausted if true, the output stream will be closed when
     *        the input is exhausted.
     */
    public StreamPumper(InputStream is, OutputStream os, boolean closeWhenExhausted) {
        this(is, os, closeWhenExhausted, false);
    }

    /**
     * Create a new StreamPumper.
     * <p><b>Note:</b> If you set useAvailable to true, you must
     * explicitly invoke {@link #stop stop} or interrupt the
     * corresponding Thread when you are done or the run method will
     * never finish on some JVMs (namely those where available returns
     * 0 on a closed stream).  Setting it to true may also impact
     * performance negatively.  This flag should only be set to true
     * if you intend to stop the pumper before the input stream gets
     * closed.</p>
     *
     * @param is input stream to read data from
     * @param os output stream to write data to.
     * @param closeWhenExhausted if true, the output stream will be closed when
     *        the input is exhausted.
     * @param useAvailable whether the pumper should use {@link
     *        java.io.InputStream#available available} to determine
     *        whether input is ready, thus trying to emulate
     *        non-blocking behavior.
     *
     * @since Ant 1.8.0
     */
    public StreamPumper(InputStream is, OutputStream os,
                        boolean closeWhenExhausted,
                        boolean useAvailable) {
        this.is = is;
        this.os = os;
        this.closeWhenExhausted = closeWhenExhausted;
        this.useAvailable = useAvailable;
    }

    /**
     * Create a new StreamPumper.
     *
     * @param is input stream to read data from
     * @param os output stream to write data to.
     */
    public StreamPumper(InputStream is, OutputStream os) {
        this(is, os, false);
    }

    /**
     * Set whether data should be flushed through to the output stream.
     * @param autoflush if true, push through data; if false, let it be buffered
     * @since Ant 1.6.3
     */
    /*package*/ void setAutoflush(boolean autoflush) {
        this.autoflush = autoflush;
    }

    /**
     * Copies data from the input stream to the output stream.
     *
     * Terminates as soon as the input stream is closed or an error occurs.
     */
    @Override
    public void run() {
        synchronized (this) {
            started = true;
        }
        finished = false;

        final byte[] buf = new byte[bufferSize];

        try {
            int length;
            while (!this.askedToStop && !Thread.interrupted()) {
                waitForInput(is);

                if (askedToStop || Thread.interrupted()) {
                    break;
                }

                length = is.read(buf);
                if (length < 0) {
                    // EOF
                    break;
                }
                if (length > 0) {
                    // we did read something, so write it out
                    os.write(buf, 0, length);
                    if (autoflush) {
                        os.flush();
                    }
                }
            }
            this.doPostStop();
        } catch (InterruptedException ie) {
            // likely PumpStreamHandler trying to stop us
        } catch (Exception e) {
            synchronized (this) {
                exception = e;
            }
        } finally {
            if (closeWhenExhausted) {
                FileUtils.close(os);
            }
            finished = true;
            askedToStop = false;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * Tells whether the end of the stream has been reached.
     * @return true is the stream has been exhausted.
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * This method blocks until the StreamPumper finishes.
     * @throws InterruptedException if interrupted.
     * @see #isFinished()
     */
    public synchronized void waitFor() throws InterruptedException {
        while (!isFinished()) {
            wait();
        }
    }

    /**
     * Set the size in bytes of the read buffer.
     * @param bufferSize the buffer size to use.
     * @throws IllegalStateException if the StreamPumper is already running.
     */
    public synchronized void setBufferSize(int bufferSize) {
        if (started) {
            throw new IllegalStateException("Cannot set buffer size on a running StreamPumper");
        }
        this.bufferSize = bufferSize;
    }

    /**
     * Get the size in bytes of the read buffer.
     *
     * @return the int size of the read buffer.
     */
    public synchronized int getBufferSize() {
        return bufferSize;
    }

    /**
     * Get the exception encountered, if any.
     * @return the Exception encountered.
     */
    public synchronized Exception getException() {
        return exception;
    }

    /**
     * Stop the pumper as soon as possible.
     * Note that it may continue to block on the input stream
     * but it will really stop the thread as soon as it gets EOF
     * or any byte, and it will be marked as finished.
     * @return Returns a {@link PostStopHandle} for the callers to
     * know if the status of post-stop activities, that happen, before this
     * {@link StreamPumper} is actually finished
     * @since Ant 1.6.3
     * @since Ant 10.2.0 this method returns a {@link PostStopHandle}
     */
    /*package*/
    synchronized PostStopHandle stop() {
        askedToStop = true;
        postStopHandle = new PostStopHandle();
        notifyAll();
        return postStopHandle;
    }

    private static final long POLL_INTERVAL = 100;

    private void waitForInput(InputStream is)
            throws IOException, InterruptedException {
        if (useAvailable) {
            while (!askedToStop && is.available() == 0) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                synchronized (this) {
                    this.wait(POLL_INTERVAL);
                }
            }
        }
    }

    private void doPostStop() throws IOException {
        try {
            final byte[] buf = new byte[bufferSize];
            int length;
            // We were asked to stop, the contract allows us to do any non-blocking
            // final bits of reads, before actually finishing. So we try and drain any (non-blocking) available
            // data. We *don't* check the thread interrupt status, anymore, once we start draining this non-blocking
            // available data, to allow us to cleanly write out any available data.
            if (askedToStop) {
                int bytesReadableWithoutBlocking;
                while ((bytesReadableWithoutBlocking = is.available()) > 0) {
                    length = is.read(buf, 0, Math.min(bytesReadableWithoutBlocking, buf.length));
                    if (length <= 0) {
                        break;
                    }
                    os.write(buf, 0, length);
                }
            }
            // this can potentially be blocking, but that's OK since our post stop activity is allowed to
            // cleanup/flush any data and the PostStopHandle let's the caller control over how long they want
            // this to go, before actually interrupting the thread
            os.flush();
        } finally {
            if (this.postStopHandle != null) {
                this.postStopHandle.latch.countDown();
                this.postStopHandle.inPostStopTasks = false;
            }
        }
    }

    /**
     * A handle that can be used after {@link #stop()} has been invoked to check if the
     * {@link StreamPumper} is in the process of do some post-stop tasks (like flushing
     * of streams), before finishing.
     */
    final class PostStopHandle {
        private boolean inPostStopTasks = true;
        private final CountDownLatch latch = new CountDownLatch(1);

        /**
         * Returns true if the {@link StreamPumper} is doing post-stop tasks (like flushing of streams).
         * Else returns false.
         * @return
         */
        boolean isInPostStopTasks() {
            return inPostStopTasks;
        }

        /**
         * Waits for a maximum of {@code timeout} time for the post-stop activities to complete.
         *
         * @param timeout  The maximum amount of time to wait for the post-stop activities to complete
         * @param timeUnit The unit of {@code timeout}
         * @return Returns true if the post-stop activities completed within the specified {@code timeout}.
         * Else returns false
         * @throws InterruptedException If the current thread was interrupted while waiting
         */
        boolean awaitPostStopCompletion(final long timeout, final TimeUnit timeUnit) throws InterruptedException {
            return this.latch.await(timeout, timeUnit);
        }
    }

}
