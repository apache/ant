/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.nativelib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Interface via which clients can request that a native
 * process be executed. This manages all aspects of running
 * a native command including such things as;
 *
 * <ul>
 *   <li>Destroying a process if it times out</li>
 *   <li>Reading data from supplied input stream and
 *       writing it to processes standard input</li>
 *   <li>Reading data from processes standard output
 *       and error streams and writing it to supplied
 *       streams</li>
 * </ul>
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public interface ExecManager
{
    /**
     * Retrieve a properties object that contains a list of
     * all the native environment variables.
     */
    Properties getNativeEnvironment()
        throws ExecException;

    /**
     * Execute a process and wait for it to finish before
     * returning.
     *
     * @param  execMetaData the metaData for native command to execute
     * @param  input the stream to read from and write to standard input.
     *         May be null in which case nothing will be written to standard.
     *         input
     * @param  output the stream to write the processes standard output to.
     *         May be null in which case that output will go into the void.
     * @param  error the stream to write the processes standard error to.
     *         May be null in which case that error will go into the void.
     * @param  timeout the maximum duration in milliseconds that a process
     *         can execute. The value must be positive or zero. If it is zero
     *         then the process will not timeout. If the process times out it
     *         will be forcibly shutdown and a TimeoutException thrown
     */
    int execute( ExecMetaData execMetaData,
                 InputStream input,
                 OutputStream output,
                 OutputStream error,
                 long timeout )
        throws IOException, ExecException /*TimeoutException*/;

    /**
     * Execute a process and wait for it to finish before
     * returning. Note that this version of execute() does not allow you
     * to specify input.
     *
     * @param  execMetaData the metaData for native command to execute
     * @param  handler the handler to which line-orientated output of
     *         process is directed for standard output and standard error
     * @param  timeout the maximum duration in milliseconds that a process
     *         can execute. The value must be positive or zero. If it is zero
     *         then the process will not timeout. If the process times out it
     *         will be forcibly shutdown and a TimeoutException thrown
     */
    int execute( ExecMetaData execMetaData,
                 ExecOutputHandler handler,
                 long timeout )
        throws IOException, ExecException /*TimeoutException*/;
}
