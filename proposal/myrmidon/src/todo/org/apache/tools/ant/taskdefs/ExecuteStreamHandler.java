/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Used by <code>Execute</code> to handle input and output stream of
 * subprocesses.
 *
 * @author thomas.haas@softwired-inc.com
 */
public interface ExecuteStreamHandler
{

    /**
     * Install a handler for the input stream of the subprocess.
     *
     * @param os output stream to write to the standard input stream of the
     *      subprocess
     * @exception IOException Description of Exception
     */
    void setProcessInputStream( OutputStream os )
        throws IOException;

    /**
     * Install a handler for the error stream of the subprocess.
     *
     * @param is input stream to read from the error stream from the subprocess
     * @exception IOException Description of Exception
     */
    void setProcessErrorStream( InputStream is )
        throws IOException;

    /**
     * Install a handler for the output stream of the subprocess.
     *
     * @param is input stream to read from the error stream from the subprocess
     * @exception IOException Description of Exception
     */
    void setProcessOutputStream( InputStream is )
        throws IOException;

    /**
     * Start handling of the streams.
     *
     * @exception IOException Description of Exception
     */
    void start()
        throws IOException;

    /**
     * Stop handling of the streams - will not be restarted.
     */
    void stop();
}
