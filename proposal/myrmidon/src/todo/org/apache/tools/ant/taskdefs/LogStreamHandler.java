/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.IOException;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Task;

/**
 * Logs standard output and error of a subprocess to the log system of ant.
 *
 * @author thomas.haas@softwired-inc.com
 */
public class LogStreamHandler extends PumpStreamHandler
{

    /**
     * Creates a new instance of this class.
     *
     * @param task the task for whom to log
     * @param outlevel the loglevel used to log standard output
     * @param errlevel the loglevel used to log standard error
     */
    public LogStreamHandler( Task task, int outlevel, int errlevel )
    {
        super( new LogOutputStream( task, outlevel ),
               new LogOutputStream( task, errlevel ) );
    }

    public void stop()
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
