/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;

/**
 * Unzip a file.
 *
 * @author costin@dnt.ro
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */
public class Expand extends MatchingTask
{// req
    private boolean overwrite = true;
    private ArrayList patternsets = new ArrayList();
    private ArrayList filesets = new ArrayList();
    private File dest;//req
    private File source;

    /**
     * Set the destination directory. File will be unzipped into the destination
     * directory.
     *
     * @param d Path to the directory.
     */
    public void setDest( File d )
    {
        this.dest = d;
    }

    /**
     * Should we overwrite files in dest, even if they are newer than the
     * corresponding entries in the archive?
     *
     * @param b The new Overwrite value
     */
    public void setOverwrite( boolean b )
    {
        overwrite = b;
    }

    /**
     * Set the path to zip-file.
     *
     * @param s Path to zip-file.
     */
    public void setSrc( File s )
    {
        this.source = s;
    }

    /**
     * Add a fileset
     *
     * @param set The feature to be added to the Fileset attribute
     */
    public void addFileset( FileSet set )
    {
        filesets.add( set );
    }

    /**
     * Add a patternset
     *
     * @param set The feature to be added to the Patternset attribute
     */
    public void addPatternset( PatternSet set )
    {
        patternsets.add( set );
    }

    /**
     * Do the work.
     *
     * @exception TaskException Thrown in unrecoverable error.
     */
    public void execute()
        throws TaskException
    {
        if( source == null && filesets.size() == 0 )
        {
            throw new TaskException( "src attribute and/or filesets must be specified" );
        }

        if( dest == null )
        {
            throw new TaskException(
                "Dest attribute must be specified" );
        }

        if( dest.exists() && !dest.isDirectory() )
        {
            throw new TaskException( "Dest must be a directory." );
        }

        if( source != null )
        {
            if( source.isDirectory() )
            {
                throw new TaskException( "Src must not be a directory." +
                                         " Use nested filesets instead." );
            }
            else
            {
                expandFile( source, dest );
            }
        }
        if( filesets.size() > 0 )
        {
            for( int j = 0; j < filesets.size(); j++ )
            {
                FileSet fs = (FileSet)filesets.get( j );
                DirectoryScanner ds = fs.getDirectoryScanner( getProject() );
                File fromDir = fs.getDir( getProject() );

                String[] files = ds.getIncludedFiles();
                for( int i = 0; i < files.length; ++i )
                {
                    File file = new File( fromDir, files[ i ] );
                    expandFile( file, dest );
                }
            }
        }
    }

    /*
     * This method is to be overridden by extending unarchival tasks.
     */
    protected void expandFile( File srcF, File dir )
        throws TaskException
    {
        ZipInputStream zis = null;
        try
        {
            // code from WarExpand
            zis = new ZipInputStream( new FileInputStream( srcF ) );
            ZipEntry ze = null;

            while( ( ze = zis.getNextEntry() ) != null )
            {
                extractFile( srcF, dir, zis,
                             ze.getName(),
                             new Date( ze.getTime() ),
                             ze.isDirectory() );
            }

            log( "expand complete", Project.MSG_VERBOSE );
        }
        catch( IOException ioe )
        {
            throw new TaskException( "Error while expanding " + srcF.getPath(), ioe );
        }
        finally
        {
            if( zis != null )
            {
                try
                {
                    zis.close();
                }
                catch( IOException e )
                {
                }
            }
        }
    }

    protected void extractFile( File srcF, File dir,
                                InputStream compressedInputStream,
                                String entryName,
                                Date entryDate, boolean isDirectory )
        throws IOException, TaskException
    {

        if( patternsets != null && patternsets.size() > 0 )
        {
            String name = entryName;
            boolean included = false;
            for( int v = 0; v < patternsets.size(); v++ )
            {
                PatternSet p = (PatternSet)patternsets.get( v );
                String[] incls = p.getIncludePatterns( getProject() );
                if( incls != null )
                {
                    for( int w = 0; w < incls.length; w++ )
                    {
                        boolean isIncl = DirectoryScanner.match( incls[ w ], name );
                        if( isIncl )
                        {
                            included = true;
                            break;
                        }
                    }
                }
                String[] excls = p.getExcludePatterns( getProject() );
                if( excls != null )
                {
                    for( int w = 0; w < excls.length; w++ )
                    {
                        boolean isExcl = DirectoryScanner.match( excls[ w ], name );
                        if( isExcl )
                        {
                            included = false;
                            break;
                        }
                    }
                }
            }
            if( !included )
            {
                //Do not process this file
                return;
            }
        }

        File f = FileUtil.resolveFile( dir, entryName );
        try
        {
            if( !overwrite && f.exists()
                && f.lastModified() >= entryDate.getTime() )
            {
                log( "Skipping " + f + " as it is up-to-date",
                     Project.MSG_DEBUG );
                return;
            }

            log( "expanding " + entryName + " to " + f,
                 Project.MSG_VERBOSE );
            // create intermediary directories - sometimes zip don't add them
            File dirF = f.getParentFile();
            dirF.mkdirs();

            if( isDirectory )
            {
                f.mkdirs();
            }
            else
            {
                byte[] buffer = new byte[ 1024 ];
                int length = 0;
                FileOutputStream fos = null;
                try
                {
                    fos = new FileOutputStream( f );

                    while( ( length =
                        compressedInputStream.read( buffer ) ) >= 0 )
                    {
                        fos.write( buffer, 0, length );
                    }

                    fos.close();
                    fos = null;
                }
                finally
                {
                    if( fos != null )
                    {
                        try
                        {
                            fos.close();
                        }
                        catch( IOException e )
                        {
                        }
                    }
                }
            }

            f.setLastModified( entryDate.getTime() );
        }
        catch( FileNotFoundException ex )
        {
            log( "Unable to expand to file " + f.getPath(), Project.MSG_WARN );
        }

    }
}
