/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.java;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.nativelib.Execute;

/**
 * An abstract compiler adaptor, that forks an external compiler.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public abstract class ExternalCompilerAdaptor
    extends JavaCompilerAdaptor
{
    /**
     * Compiles a set of files.
     */
    protected void compile( final File[] files )
        throws TaskException
    {
        // Write the file names to a temp file
        final File tempFile = createTempFile( files );
        try
        {
            final Execute exe = new Execute();

            // Build the command line
            buildCommandLine( exe, tempFile );

            // Execute
            exe.execute( getContext() );
        }
        finally
        {
            tempFile.delete();
        }
    }

    /**
     * Builds the command-line to execute the compiler.
     */
    protected abstract void buildCommandLine( final Execute exe, final File tempFile )
        throws TaskException;

    /**
     * Writes the temporary file containing the names of the files to compile.
     */
    private File createTempFile( final File[] files )
        throws TaskException
    {
        try
        {
            // Build the temp file name
            final File tmpFile = File.createTempFile( "javac", "", getContext().getBaseDirectory() );

            // Write file names to the temp file
            final FileWriter writer = new FileWriter( tmpFile );
            try
            {
                final PrintWriter pwriter = new PrintWriter( writer, false );
                for( int i = 0; i < files.length; i++ )
                {
                    File file = files[ i ];
                    pwriter.println( file.getAbsolutePath() );
                }
                pwriter.close();
            }
            finally
            {
                writer.close();
            }

            return tmpFile;
        }
        catch( final IOException e )
        {
            throw new TaskException( "Cannot write file list", e );
        }
    }

}
