/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Parses output from jikes and passes errors and warnings into the right
 * logging channels of Project. TODO: Parsing could be much better
 *
 * @author skanthak@muehlheim.de
 * @deprecated use Jikes' exit value to detect compilation failure.
 */
public class JikesOutputParser implements ExecuteStreamHandler
{
    protected boolean errorFlag = false;
    protected boolean error = false;

    protected BufferedReader br;
    protected boolean emacsMode;// no errors so far
    protected int errors, warnings;
    protected Task task;

    /**
     * Construct a new Parser object
     *
     * @param task - task in whichs context we are called
     * @param emacsMode Description of Parameter
     */
    protected JikesOutputParser( Task task, boolean emacsMode )
    {
        super();
        this.task = task;
        this.emacsMode = emacsMode;
    }

    /**
     * Ignore.
     *
     * @param is The new ProcessErrorStream value
     */
    public void setProcessErrorStream( InputStream is ) { }

    /**
     * Ignore.
     *
     * @param os The new ProcessInputStream value
     */
    public void setProcessInputStream( OutputStream os ) { }

    /**
     * Set the inputstream
     *
     * @param is The new ProcessOutputStream value
     * @exception IOException Description of Exception
     */
    public void setProcessOutputStream( InputStream is )
        throws IOException
    {
        br = new BufferedReader( new InputStreamReader( is ) );
    }

    /**
     * Invokes parseOutput.
     *
     * @exception IOException Description of Exception
     */
    public void start()
        throws IOException
    {
        parseOutput( br );
    }

    /**
     * Ignore.
     */
    public void stop() { }

    /**
     * Indicate if there were errors during the compile
     *
     * @return if errors ocured
     */
    protected boolean getErrorFlag()
    {
        return errorFlag;
    }

    /**
     * Parse the output of a jikes compiler
     *
     * @param reader - Reader used to read jikes's output
     * @exception IOException Description of Exception
     */
    protected void parseOutput( BufferedReader reader )
        throws IOException
    {
        if( emacsMode )
            parseEmacsOutput( reader );
        else
            parseStandardOutput( reader );
    }

    private void setError( boolean err )
    {
        error = err;
        if( error )
            errorFlag = true;
    }

    private void log( String line )
    {
        if( !emacsMode )
        {
            task.log( "", ( error ? Project.MSG_ERR : Project.MSG_WARN ) );
        }
        task.log( line, ( error ? Project.MSG_ERR : Project.MSG_WARN ) );
    }

    private void parseEmacsOutput( BufferedReader reader )
        throws IOException
    {
        // This may change, if we add advanced parsing capabilities.
        parseStandardOutput( reader );
    }

    private void parseStandardOutput( BufferedReader reader )
        throws IOException
    {
        String line;
        String lower;
        // We assume, that every output, jike does, stands for an error/warning
        // XXX
        // Is this correct?

        // TODO:
        // A warning line, that shows code, which contains a variable
        // error will cause some trouble. The parser should definitely
        // be much better.

        while( ( line = reader.readLine() ) != null )
        {
            lower = line.toLowerCase();
            if( line.trim().equals( "" ) )
                continue;
            if( lower.indexOf( "error" ) != -1 )
                setError( true );
            else if( lower.indexOf( "warning" ) != -1 )
                setError( false );
            else
            {
                // If we don't know the type of the line
                // and we are in emacs mode, it will be
                // an error, because in this mode, jikes won't
                // always print "error", but sometimes other
                // keywords like "Syntax". We should look for
                // all those keywords.
                if( emacsMode )
                    setError( true );
            }
            log( line );
        }
    }
}
