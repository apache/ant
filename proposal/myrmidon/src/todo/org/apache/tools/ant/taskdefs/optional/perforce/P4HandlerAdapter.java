/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.perforce;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import org.apache.tools.ant.BuildException;

public abstract class P4HandlerAdapter implements P4Handler
{

    String p4input = "";//Input
    InputStream es;//OUtput
    InputStream is;

    OutputStream os;

    //set any data to be written to P4's stdin - messy, needs work
    public void setOutput( String p4Input )
    {
        this.p4input = p4Input;
    }

    public void setProcessErrorStream( InputStream is )
        throws IOException
    {
        this.es = is;
    }//Error

    public void setProcessInputStream( OutputStream os )
        throws IOException
    {
        this.os = os;
    }

    public void setProcessOutputStream( InputStream is )
        throws IOException
    {
        this.is = is;
    }

    public abstract void process( String line );


    public void start()
        throws BuildException
    {

        try
        {
            //First write any output to P4
            if( p4input != null && p4input.length() > 0 && os != null )
            {
                os.write( p4input.getBytes() );
                os.flush();
                os.close();
            }

            //Now read any input and process

            BufferedReader input = new BufferedReader(
                new InputStreamReader(
                new SequenceInputStream( is, es ) ) );

            String line;
            while( ( line = input.readLine() ) != null )
            {
                process( line );
            }

            input.close();

        }
        catch( Exception e )
        {
            throw new BuildException( e );
        }
    }

    public void stop() { }
}
