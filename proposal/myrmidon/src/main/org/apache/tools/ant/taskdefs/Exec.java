/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Executes a given command if the os platform is appropriate.
 *
 * @author duncan@x180.com
 * @author rubys@us.ibm.com
 * @deprecated Instead of using this class, please extend ExecTask or delegate
 *      to Execute.
 */
public class Exec extends Task
{

    private final static int BUFFER_SIZE = 512;
    protected PrintWriter fos = null;
    private boolean failOnError = false;
    private String command;
    private File dir;
    private String os;
    private String out;

    public void setCommand( String command )
    {
        this.command = command;
    }

    public void setDir( String d )
    {
        this.dir = project.resolveFile( d );
    }

    public void setFailonerror( boolean fail )
    {
        failOnError = fail;
    }

    public void setOs( String os )
    {
        this.os = os;
    }

    public void setOutput( String out )
    {
        this.out = out;
    }

    public void execute()
        throws BuildException
    {
        run( command );
    }

    protected void logFlush()
    {
        if( fos != null )
            fos.close();
    }

    protected void outputLog( String line, int messageLevel )
    {
        if( fos == null )
        {
            log( line, messageLevel );
        }
        else
        {
            fos.println( line );
        }
    }

    protected int run( String command )
        throws BuildException
    {

        int err = -1;// assume the worst

        // test if os match
        String myos = System.getProperty( "os.name" );
        log( "Myos = " + myos, Project.MSG_VERBOSE );
        if( ( os != null ) && ( os.indexOf( myos ) < 0 ) )
        {
            // this command will be executed only on the specified OS
            log( "Not found in " + os, Project.MSG_VERBOSE );
            return 0;
        }

        // default directory to the project's base directory
        if( dir == null )
            dir = project.getBaseDir();

        if( myos.toLowerCase().indexOf( "windows" ) >= 0 )
        {
            if( !dir.equals( project.resolveFile( "." ) ) )
            {
                if( myos.toLowerCase().indexOf( "nt" ) >= 0 )
                {
                    command = "cmd /c cd " + dir + " && " + command;
                }
                else
                {
                    String ant = project.getProperty( "ant.home" );
                    if( ant == null )
                    {
                        throw new BuildException( "Property 'ant.home' not found", location );
                    }

                    String antRun = project.resolveFile( ant + "/bin/antRun.bat" ).toString();
                    command = antRun + " " + dir + " " + command;
                }
            }
        }
        else
        {
            String ant = project.getProperty( "ant.home" );
            if( ant == null )
                throw new BuildException( "Property 'ant.home' not found", location );
            String antRun = project.resolveFile( ant + "/bin/antRun" ).toString();

            command = antRun + " " + dir + " " + command;
        }

        try
        {
            // show the command
            log( command, Project.MSG_VERBOSE );

            // exec command on system runtime
            Process proc = Runtime.getRuntime().exec( command );

            if( out != null )
            {
                fos = new PrintWriter( new FileWriter( out ) );
                log( "Output redirected to " + out, Project.MSG_VERBOSE );
            }

            // copy input and error to the output stream
            StreamPumper inputPumper =
                new StreamPumper( proc.getInputStream(), Project.MSG_INFO, this );
            StreamPumper errorPumper =
                new StreamPumper( proc.getErrorStream(), Project.MSG_WARN, this );

            // starts pumping away the generated output/error
            inputPumper.start();
            errorPumper.start();

            // Wait for everything to finish
            proc.waitFor();
            inputPumper.join();
            errorPumper.join();
            proc.destroy();

            // close the output file if required
            logFlush();

            // check its exit value
            err = proc.exitValue();
            if( err != 0 )
            {
                if( failOnError )
                {
                    throw new BuildException( "Exec returned: " + err, location );
                }
                else
                {
                    log( "Result: " + err, Project.MSG_ERR );
                }
            }
        }
        catch( IOException ioe )
        {
            throw new BuildException( "Error exec: " + command, ioe, location );
        }
        catch( InterruptedException ex )
        {}

        return err;
    }

    // Inner class for continually pumping the input stream during
    // Process's runtime.
    class StreamPumper extends Thread
    {
        private boolean endOfStream = false;
        private int SLEEP_TIME = 5;
        private BufferedReader din;
        private int messageLevel;
        private Exec parent;

        public StreamPumper( InputStream is, int messageLevel, Exec parent )
        {
            this.din = new BufferedReader( new InputStreamReader( is ) );
            this.messageLevel = messageLevel;
            this.parent = parent;
        }

        public void pumpStream()
            throws IOException
        {
            byte[] buf = new byte[BUFFER_SIZE];
            if( !endOfStream )
            {
                String line = din.readLine();

                if( line != null )
                {
                    outputLog( line, messageLevel );
                }
                else
                {
                    endOfStream = true;
                }
            }
        }

        public void run()
        {
            try
            {
                try
                {
                    while( !endOfStream )
                    {
                        pumpStream();
                        sleep( SLEEP_TIME );
                    }
                }
                catch( InterruptedException ie )
                {}
                din.close();
            }
            catch( IOException ioe )
            {}
        }
    }
}
