/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

package org.apache.tools.ant.taskdefs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Copies standard output and error of subprocesses to standard output and
 * error of the parent process.
 *
 * @since Ant 1.2
 */
public class PumpStreamHandler implements ExecuteStreamHandler {

    private Thread outputThread;
    private Thread errorThread;
    private StreamPumper inputPump;

    private OutputStream out;
    private OutputStream err;
    private InputStream input;

    /**
     * Construct a new <code>PumpStreamHandler</code>.
     * @param out the output <code>OutputStream</code>.
     * @param err the error <code>OutputStream</code>.
     * @param input the input <code>InputStream</code>.
     */
    public PumpStreamHandler(OutputStream out, OutputStream err,
                             InputStream input) {
        this.out = out;
        this.err = err;
        this.input = input;
    }

    /**
     * Construct a new <code>PumpStreamHandler</code>.
     * @param out the output <code>OutputStream</code>.
     * @param err the error <code>OutputStream</code>.
     */
    public PumpStreamHandler(OutputStream out, OutputStream err) {
        this(out, err, null);
    }

    /**
     * Construct a new <code>PumpStreamHandler</code>.
     * @param outAndErr the output/error <code>OutputStream</code>.
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
        if (err != null) {
            createProcessErrorPump(is, err);
        }
    }

    /**
     * Set the <code>OutputStream</code> by means of which
     * input can be sent to the process.
     * @param os the <code>OutputStream</code>.
     */
    public void setProcessInputStream(OutputStream os) {
        if (input != null) {
            inputPump = createInputPump(input, os, true);
        } else {
            try {
                os.close();
            } catch (IOException e) {
                //ignore
            }
        }
    }

    /**
     * Start the <code>Thread</code>s.
     */
    public void start() {
        outputThread.start();
        errorThread.start();
        if (inputPump != null) {
            Thread inputThread = new Thread(inputPump);
            inputThread.setDaemon(true);
            inputThread.start();
        }
    }

    /**
     * Stop pumping the streams.
     */
    public void stop() {
        try {
            outputThread.join();
        } catch (InterruptedException e) {
            // ignore
        }
        try {
            errorThread.join();
        } catch (InterruptedException e) {
            // ignore
        }

        if (inputPump != null) {
            inputPump.stop();
        }

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
     * @return a thread object that does the pumping.
     */
    protected Thread createPump(InputStream is, OutputStream os,
                                boolean closeWhenExhausted) {
        final Thread result
            = new Thread(new StreamPumper(is, os, closeWhenExhausted));
        result.setDaemon(true);
        return result;
    }

    /**
     * Creates a stream pumper to copy the given input stream to the
     * given output stream. Used for standard input.
     * @since Ant 1.6.3
     */
    /*protected*/ StreamPumper createInputPump(InputStream is, OutputStream os,
                                boolean closeWhenExhausted) {
        StreamPumper pumper = new StreamPumper(is, os, closeWhenExhausted);
        pumper.setAutoflush(true);
        return pumper;
    }

}
