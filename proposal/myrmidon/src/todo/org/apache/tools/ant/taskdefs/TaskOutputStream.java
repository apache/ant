/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.tools.ant.Task;

/**
 * Redirects text written to a stream thru the standard ant logging mechanism.
 * This class is useful for integrating with tools that write to System.out and
 * System.err. For example, the following will cause all text written to
 * System.out to be logged with "info" priority: <pre>System.setOut(new PrintStream(new TaskOutputStream(project, Project.MSG_INFO)));</pre>
 *
 * @author James Duncan Davidson (duncan@x180.com)
 * @deprecated use LogOutputStream instead.
 */

public class TaskOutputStream extends OutputStream
{
    private StringBuffer line;
    private int msgOutputLevel;

    private Task task;

    /**
     * Constructs a new JavacOutputStream with the given project as the output
     * source for messages.
     *
     * @param task Description of Parameter
     * @param msgOutputLevel Description of Parameter
     */

    TaskOutputStream( Task task, int msgOutputLevel )
    {
        this.task = task;
        this.msgOutputLevel = msgOutputLevel;

        line = new StringBuffer();
    }

    /**
     * Write a character to the output stream. This method looks to make sure
     * that there isn't an error being reported and will flush each line of
     * input out to the project's log stream.
     *
     * @param c Description of Parameter
     * @exception IOException Description of Exception
     */

    public void write( int c )
        throws IOException
    {
        char cc = ( char )c;
        if( cc == '\r' || cc == '\n' )
        {
            // line feed
            if( line.length() > 0 )
            {
                processLine();
            }
        }
        else
        {
            line.append( cc );
        }
    }

    /**
     * Processes a line of input and determines if an error occured.
     */

    private void processLine()
    {
        String s = line.toString();
        task.log( s, msgOutputLevel );
        line = new StringBuffer();
    }
}

