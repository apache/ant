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
 * Serves as an output stream to Javac. This let's us print messages out to the
 * log and detect whether or not Javac had an error while compiling.
 *
 * @author James Duncan Davidson (duncan@x180.com)
 * @deprecated use returnvalue of compile to detect compilation failure.
 */

class JavacOutputStream extends OutputStream
{
    private boolean errorFlag = false;
    private StringBuffer line;

    private Task task;

    /**
     * Constructs a new JavacOutputStream with the given task as the output
     * source for messages.
     *
     * @param task Description of Parameter
     */

    JavacOutputStream( Task task )
    {
        this.task = task;
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
     * Returns the error status of the compile. If no errors occured, this
     * method will return false, else this method will return true.
     *
     * @return The ErrorFlag value
     */

    boolean getErrorFlag()
    {
        return errorFlag;
    }

    /**
     * Processes a line of input and determines if an error occured.
     */

    private void processLine()
    {
        String s = line.toString();
        if( s.indexOf( "error" ) > -1 )
        {
            errorFlag = true;
        }
        task.log( s );
        line = new StringBuffer();
    }
}

