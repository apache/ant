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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.apache.tools.ant.util.FileUtils;

/**
 * Copies standard output and error of subprocesses to standard output and
 * error of the parent process.
 *
 * @since Ant 1.2
 */
public class PumpStreamHandler implements ExecuteStreamHandler {

    private Thread outputThread;
    private Thread errorThread;
    private Thread inputThread;

    private OutputStream out;
    private OutputStream err;
    private InputStream input;
    private final boolean nonBlockingRead;

    /**
     * Construct a new <code>PumpStreamHandler</code>.
     * @param out the output <code>OutputStream</code>, must not be null.
     * @param err the error <code>OutputStream</code>, must not be null.
     * @param input the input <code>InputStream</code>.
     * @param nonBlockingRead set it to <code>true</code> if the input should be
     *                      read with simulated non blocking IO.
     */
    public PumpStreamHandler(OutputStream out, OutputStream err,
                             InputStream input, boolean nonBlockingRead) {
        if (out == null) {
            throw new NullPointerException("out must not be null");
        }
        if (err == null) {
            throw new NullPointerException("err must not be null");
        }
        this.out = out;
        this.err = err;
        this.input = input;
        this.nonBlockingRead = nonBlockingRead;
    }

    /**
     * Construct a new <code>PumpStreamHandler</code>.
     * @param out the output <code>OutputStream</code>, must not be null.
     * @param err the error <code>OutputStream</code>, must not be null.
     * @param input the input <code>InputStream</code>.
     */
    public PumpStreamHandler(OutputStream out, OutputStream err,
                             InputStream input) {
        this(out, err, input, false);
    }

    /**
     * Construct a new <code>PumpStreamHandler</code>.
     * @param out the output <code>OutputStream</code>, must not be null.
     * @param err the error <code>OutputStream</code>, must not be null.
     */
    public PumpStreamHandler(OutputStream out, OutputStream err) {
        this(out, err, null);
    }

    /**
     * Construct a new <code>PumpStreamHandler</code>.
     * @param outAndErr the output/error <code>OutputStream</code>, must not be null.
     */
    public PumpStreamHandler(OutputStream outAndErr) {
        this(outAndErr, outAndErr);
    }

    /**
     * Construct a new <code>PumpStreamHandler</code>.
     */
    public PumpStreamHandler() {
        this(System.out, System.err);
    }

    /**
     * Set the <code>InputStream</code> from which to read the
     * standard output of the process.
     * @param is the <code>InputStream</code>.
     */
    public void setProcessOutputStream(InputStream is) {
        createProcessOutputPump(is, out);
    }

    /**
     * Set the <code>InputStream</code> from which to read the
     * standard error of the process.
     * @param is the <code>InputStream</code>.
     */
    public void setProcessErrorStream(InputStream is) {
        createProcessErrorPump(is, err);
    }

    /**
     * Set the <code>OutputStream</code> by means of which
     * input can be sent to the process.
     * @param os the <code>OutputStream</code>.
     */
    public void setProcessInputStream(OutputStream os) {
        if (input != null) {
            inputThread = createPump(input, os, true, nonBlockingRead);
        } else {
            FileUtils.close(os);
        }
    }

    /**
     * Start the <code>Thread</code>s.
     */
    public void start() {
        start(outputThread);
        start(errorThread);
        start(inputThread);
    }

    /**
     * Stop pumping the streams.
     */
    public void stop() {
        finish(inputThread);

        try {
            err.flush();
        } catch (IOException e) {
            // ignore
        }
        try {
            out.flush();
        } catch (IOException e) {
            // ignore
        }
        finish(outputThread);
        finish(errorThread);
    }

    private static final long JOIN_TIMEOUT = 200;

    private void start(Thread t) {
        if (t != null) {
            t.start();
        }
    }

    /**
     * Waits for a thread to finish while trying to make it finish
     * quicker by stopping the pumper (if the thread is a {@link
     * ThreadWithPumper ThreadWithPumper} instance) or interrupting
     * the thread.
     *
     * @param t Thread
     * @since Ant 1.8.0
     */
    protected final void finish(Thread t) {
        if (t == null) {
            // nothing to terminate
            return;
        }
        try {
            StreamPumper s = null;
            if (t instanceof ThreadWithPumper) {
                s = ((ThreadWithPumper) t).getPumper();
            }
            if (s != null && s.isFinished()) {
                return;
            }
            if (!t.isAlive()) {
                return;
            }
            StreamPumper.PostStopHandle postStopHandle = null;
            if (s != null && !s.isFinished()) {
                postStopHandle = s.stop();
            }
            if (postStopHandle != null && postStopHandle.isInPostStopTasks()) {
                // the stream pumper is in post stop tasks (like flushing output), which
                // indicates that the stream pumper has respected the stop request and
                // is cleaning up before finishing. Give it some time to finish this
                // post stop activity, before trying to force interrupt the underlying thread
                // of the stream pumper
                postStopHandle.awaitPostStopCompletion(2, TimeUnit.SECONDS);
            }
            while ((s == null || !s.isFinished()) && t.isAlive()) {
                // we waited for the thread/stream pumper to finish, but it hasn't yet.
                // so we interrupt it
                t.interrupt();
                t.join(JOIN_TIMEOUT);
            }
        } catch (InterruptedException e) {
            // ignore
        }
    }

    /**
     * Get the error stream.
     * @return <code>OutputStream</code>.
     */
    protected OutputStream getErr() {
        return err;
    }

    /**
     * Get the output stream.
     * @return <code>OutputStream</code>.
     */
    protected OutputStream getOut() {
        return out;
    }

    /**
     * Create the pump to handle process output.
     * @param is the <code>InputStream</code>.
     * @param os the <code>OutputStream</code>.
     */
    protected void createProcessOutputPump(InputStream is, OutputStream os) {
        outputThread = createPump(is, os);
    }

    /**
     * Create the pump to handle error output.
     * @param is the input stream to copy from.
     * @param os the output stream to copy to.
     */
    protected void createProcessErrorPump(InputStream is, OutputStream os) {
        errorThread = createPump(is, os);
    }

    /**
     * Creates a stream pumper to copy the given input stream to the
     * given output stream.
     * @param is the input stream to copy from.
     * @param os the output stream to copy to.
     * @return a thread object that does the pumping.
     */
    protected Thread createPump(InputStream is, OutputStream os) {
        return createPump(is, os, false);
    }

    /**
     * Creates a stream pumper to copy the given input stream to the
     * given output stream.
     * @param is the input stream to copy from.
     * @param os the output stream to copy to.
     * @param closeWhenExhausted if true close the inputstream.
     * @return a thread object that does the pumping, subclasses
     * should return an instance of {@link ThreadWithPumper
     * ThreadWithPumper}.
     */
    protected Thread createPump(InputStream is, OutputStream os,
                                boolean closeWhenExhausted) {
        return createPump(is, os, closeWhenExhausted, true);
    }

    /**
     * Creates a stream pumper to copy the given input stream to the
     * given output stream.
     * @param is the input stream to copy from.
     * @param os the output stream to copy to.
     * @param closeWhenExhausted if true close the inputstream.
     * @param nonBlockingIO set it to <code>true</code> to use simulated non
     *                     blocking IO.
     * @return a thread object that does the pumping, subclasses
     * should return an instance of {@link ThreadWithPumper
     * ThreadWithPumper}.
     * @since Ant 1.8.2
     */
    protected Thread createPump(InputStream is, OutputStream os,
                                boolean closeWhenExhausted, boolean nonBlockingIO) {
        StreamPumper pumper = new StreamPumper(is, os, closeWhenExhausted, nonBlockingIO);
        pumper.setAutoflush(true);
        final Thread result = new ThreadWithPumper(pumper);
        result.setDaemon(true);
        return result;
    }

    /**
     * Specialized subclass that allows access to the running StreamPumper.
     *
     * @since Ant 1.8.0
     */
    protected static class ThreadWithPumper extends Thread {
        private final StreamPumper pumper;
        public ThreadWithPumper(StreamPumper p) {
            super(p);
            pumper = p;
        }
        protected StreamPumper getPumper() {
            return pumper;
        }
    }
}
