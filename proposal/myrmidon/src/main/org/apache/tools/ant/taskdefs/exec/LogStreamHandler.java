/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.exec;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.myrmidon.api.TaskException;

/**
 * Logs standard output and error of a subprocess to the log system of ant.
 *
 * @author thomas.haas@softwired-inc.com
 */
public class LogStreamHandler
    extends PumpStreamHandler
{
    public LogStreamHandler( final OutputStream output, final OutputStream error )
    {
        super( output, error );
    }

    public void stop()
        throws TaskException
    {
        super.stop();
        try
        {
            getErr().close();
            getOut().close();
        }
        catch( IOException e )
        {
            // plain impossible
            throw new TaskException( "Error", e );
        }
    }
}
