/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;

/**
 * Copies a directory.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @deprecated The copydir task is deprecated. Use copy instead.
 */

public class Copydir extends MatchingTask
{
    private boolean filtering = false;
    private boolean flatten = false;
    private boolean forceOverwrite = false;
    private Hashtable filecopyList = new Hashtable();
    private File destDir;

    private File srcDir;

    public void setDest( File dest )
    {
        destDir = dest;
    }

    public void setFiltering( boolean filter )
    {
        filtering = filter;
    }

    public void setFlatten( boolean flatten )
    {
        this.flatten = flatten;
    }

    public void setForceoverwrite( boolean force )
    {
        forceOverwrite = force;
    }

    public void setSrc( File src )
    {
        srcDir = src;
    }

    public void execute()
        throws BuildException
    {
        log( "DEPRECATED - The copydir task is deprecated.  Use copy instead." );

        if( srcDir == null )
        {
            throw new BuildException( "src attribute must be set!",
                location );
        }

        if( !srcDir.exists() )
        {
            throw new BuildException( "srcdir " + srcDir.toString()
                 + " does not exist!", location );
        }

        if( destDir == null )
        {
            throw new BuildException( "The dest attribute must be set.", location );
        }

        if( srcDir.equals( destDir ) )
        {
            log( "Warning: src == dest" );
        }

        DirectoryScanner ds = super.getDirectoryScanner( srcDir );

        String[] files = ds.getIncludedFiles();
        scanDir( srcDir, destDir, files );
        if( filecopyList.size() > 0 )
        {
            log( "Copying " + filecopyList.size() + " file"
                 + ( filecopyList.size() == 1 ? "" : "s" )
                 + " to " + destDir.getAbsolutePath() );
            Enumeration enum = filecopyList.keys();
            while( enum.hasMoreElements() )
            {
                String fromFile = ( String )enum.nextElement();
                String toFile = ( String )filecopyList.get( fromFile );
                try
                {
                    project.copyFile( fromFile, toFile, filtering,
                        forceOverwrite );
                }
                catch( IOException ioe )
                {
                    String msg = "Failed to copy " + fromFile + " to " + toFile
                         + " due to " + ioe.getMessage();
                    throw new BuildException( msg, ioe, location );
                }
            }
        }
    }

    private void scanDir( File from, File to, String[] files )
    {
        for( int i = 0; i < files.length; i++ )
        {
            String filename = files[i];
            File srcFile = new File( from, filename );
            File destFile;
            if( flatten )
            {
                destFile = new File( to, new File( filename ).getName() );
            }
            else
            {
                destFile = new File( to, filename );
            }
            if( forceOverwrite ||
                ( srcFile.lastModified() > destFile.lastModified() ) )
            {
                filecopyList.put( srcFile.getAbsolutePath(),
                    destFile.getAbsolutePath() );
            }
        }
    }
}
