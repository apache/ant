/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Encapsulates a Jikes compiler, by directly executing an external process.
 *
 * @author skanthak@muehlheim.de
 * @deprecated merged into the class Javac.
 */
public class Jikes
{
    protected String command;
    protected JikesOutputParser jop;
    protected Project project;

    /**
     * Constructs a new Jikes obect.
     *
     * @param jop - Parser to send jike's output to
     * @param command - name of jikes executeable
     * @param project Description of Parameter
     */
    protected Jikes( JikesOutputParser jop, String command, Project project )
    {
        super();
        this.jop = jop;
        this.command = command;
        this.project = project;
    }

    /**
     * Do the compile with the specified arguments.
     *
     * @param args - arguments to pass to process on command line
     */
    protected void compile( String[] args )
    {
        String[] commandArray = null;
        File tmpFile = null;

        try
        {
            String myos = System.getProperty( "os.name" );

            // Windows has a 32k limit on total arg size, so
            // create a temporary file to store all the arguments

            // There have been reports that 300 files could be compiled
            // so 250 is a conservative approach
            if( myos.toLowerCase().indexOf( "windows" ) >= 0
                 && args.length > 250 )
            {
                PrintWriter out = null;
                try
                {
                    tmpFile = new File( "jikes" + ( new Random( System.currentTimeMillis() ) ).nextLong() );
                    out = new PrintWriter( new FileWriter( tmpFile ) );
                    for( int i = 0; i < args.length; i++ )
                    {
                        out.println( args[i] );
                    }
                    out.flush();
                    commandArray = new String[]{command,
                        "@" + tmpFile.getAbsolutePath()};
                }
                catch( IOException e )
                {
                    throw new BuildException( "Error creating temporary file", e );
                }
                finally
                {
                    if( out != null )
                    {
                        try
                        {
                            out.close();
                        }
                        catch( Throwable t )
                        {}
                    }
                }
            }
            else
            {
                commandArray = new String[args.length + 1];
                commandArray[0] = command;
                System.arraycopy( args, 0, commandArray, 1, args.length );
            }

            // We assume, that everything jikes writes goes to
            // standard output, not to standard error. The option
            // -Xstdout that is given to Jikes in Javac.doJikesCompile()
            // should guarantee this. At least I hope so. :)
            try
            {
                Execute exe = new Execute( jop );
                exe.setAntRun( project );
                exe.setWorkingDirectory( project.getBaseDir() );
                exe.setCommandline( commandArray );
                exe.execute();
            }
            catch( IOException e )
            {
                throw new BuildException( "Error running Jikes compiler", e );
            }
        }
        finally
        {
            if( tmpFile != null )
            {
                tmpFile.delete();
            }
        }
    }
}
