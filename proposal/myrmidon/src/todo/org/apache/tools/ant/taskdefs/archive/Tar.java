/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.aut.tar.TarEntry;
import org.apache.aut.tar.TarOutputStream;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.ScannerUtil;
import org.apache.tools.ant.types.SourceFileScanner;
import org.apache.tools.ant.util.mappers.MergingMapper;

/**
 * Creates a TAR archive.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">
 *      stefano@apache.org</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public class Tar
    extends MatchingTask
{
    private TarLongFileMode longFileMode = createMode();

    private TarLongFileMode createMode()
    {
        try
        {
            return new TarLongFileMode();
        }
        catch( TaskException e )
        {
            throw new IllegalStateException( e.getMessage() );
        }
    }

    ArrayList filesets = new ArrayList();
    ArrayList fileSetFiles = new ArrayList();

    /**
     * Indicates whether the user has been warned about long files already.
     */
    private boolean longWarningGiven = false;
    File baseDir;

    File tarFile;

    /**
     * This is the base directory to look in for things to tar.
     *
     * @param baseDir The new Basedir value
     */
    public void setBasedir( File baseDir )
    {
        this.baseDir = baseDir;
    }

    /**
     * Set how to handle long files. Allowable values are truncate - paths are
     * truncated to the maximum length fail - paths greater than the maximim
     * cause a build exception warn - paths greater than the maximum cause a
     * warning and GNU is used gnu - GNU extensions are used for any paths
     * greater than the maximum. omit - paths greater than the maximum are
     * omitted from the archive
     *
     * @param mode The new Longfile value
     */
    public void setLongfile( TarLongFileMode mode )
    {
        this.longFileMode = mode;
    }

    /**
     * This is the name/location of where to create the tar file.
     *
     * @param tarFile The new Tarfile value
     */
    public void setTarfile( File tarFile )
    {
        this.tarFile = tarFile;
    }

    public TarFileSet createTarFileSet()
    {
        TarFileSet fileset = new TarFileSet();
        filesets.add( fileset );
        return fileset;
    }

    public void execute()
        throws TaskException
    {
        if( tarFile == null )
        {
            throw new TaskException( "tarfile attribute must be set!" );
        }

        if( tarFile.exists() && tarFile.isDirectory() )
        {
            throw new TaskException( "tarfile is a directory!" );
        }

        if( tarFile.exists() && !tarFile.canWrite() )
        {
            throw new TaskException( "Can not write to the specified tarfile!" );
        }

        if( baseDir != null )
        {
            if( !baseDir.exists() )
            {
                throw new TaskException( "basedir does not exist!" );
            }

            // add the main fileset to the list of filesets to process.
            final TarFileSet mainFileSet = new TarFileSet( /*fileset*/ );
            mainFileSet.setDir( baseDir );
            filesets.add( mainFileSet );
        }

        if( filesets.size() == 0 )
        {
            throw new TaskException( "You must supply either a basdir attribute or some nested filesets." );
        }

        // check if tr is out of date with respect to each
        // fileset
        boolean upToDate = true;
        for( Iterator e = filesets.iterator(); e.hasNext(); )
        {
            TarFileSet fs = (TarFileSet)e.next();
            String[] files = ScannerUtil.getFiles( fs );

            if( !archiveIsUpToDate( files ) )
            {
                upToDate = false;
            }

            for( int i = 0; i < files.length; ++i )
            {
                if( tarFile.equals( new File( fs.getDir(), files[ i ] ) ) )
                {
                    throw new TaskException( "A tar file cannot include itself" );
                }
            }
        }

        if( upToDate )
        {
            getLogger().info( "Nothing to do: " + tarFile.getAbsolutePath() + " is up to date." );
            return;
        }

        getLogger().info( "Building tar: " + tarFile.getAbsolutePath() );

        TarOutputStream tOut = null;
        try
        {
            tOut = new TarOutputStream( new FileOutputStream( tarFile ) );
            tOut.setDebug( true );
            if( longFileMode.isTruncateMode() )
            {
                tOut.setLongFileMode( TarOutputStream.LONGFILE_TRUNCATE );
            }
            else if( longFileMode.isFailMode() ||
                longFileMode.isOmitMode() )
            {
                tOut.setLongFileMode( TarOutputStream.LONGFILE_ERROR );
            }
            else
            {
                // warn or GNU
                tOut.setLongFileMode( TarOutputStream.LONGFILE_GNU );
            }

            longWarningGiven = false;
            for( Iterator e = filesets.iterator(); e.hasNext(); )
            {
                TarFileSet fs = (TarFileSet)e.next();
                String[] files = ScannerUtil.getFiles( fs );
                for( int i = 0; i < files.length; i++ )
                {
                    File f = new File( fs.getDir(), files[ i ] );
                    String name = files[ i ].replace( File.separatorChar, '/' );
                    tarFile( f, tOut, name, fs );
                }
            }
        }
        catch( IOException ioe )
        {
            String msg = "Problem creating TAR: " + ioe.getMessage();
            throw new TaskException( msg, ioe );
        }
        finally
        {
            if( tOut != null )
            {
                try
                {
                    // close up
                    tOut.close();
                }
                catch( IOException e )
                {
                }
            }
        }
    }

    private boolean archiveIsUpToDate( final String[] files )
        throws TaskException
    {
        final SourceFileScanner scanner = new SourceFileScanner();
        setupLogger( scanner );
        final MergingMapper mapper = new MergingMapper();
        mapper.setTo( tarFile.getAbsolutePath() );
        return scanner.restrict( files, baseDir, null, mapper, getContext() ).length == 0;
    }

    private void tarFile( final File file,
                          final TarOutputStream output,
                          final String path,
                          final TarFileSet tarFileSet )
        throws IOException, TaskException
    {
        String storedPath = path;
        // don't add "" to the archive
        if( storedPath.length() <= 0 )
        {
            return;
        }

        if( file.isDirectory() && !storedPath.endsWith( "/" ) )
        {
            storedPath += "/";
        }

        if( storedPath.length() >= TarEntry.NAMELEN )
        {
            if( longFileMode.isOmitMode() )
            {
                final String message = "Omitting: " + storedPath;
                getLogger().info( message );
                return;
            }
            else if( longFileMode.isWarnMode() )
            {
                final String message = "Entry: " + storedPath + " longer than " +
                    TarEntry.NAMELEN + " characters.";
                getLogger().warn( message );
                if( !longWarningGiven )
                {
                    final String message2 = "Resulting tar file can only be processed successfully"
                        + " by GNU compatible tar commands";
                    getLogger().warn( message2 );
                    longWarningGiven = true;
                }
            }
            else if( longFileMode.isFailMode() )
            {
                final String message = "Entry: " + storedPath + " longer than " +
                    TarEntry.NAMELEN + "characters.";
                throw new TaskException( message );
            }
        }

        FileInputStream input = null;
        try
        {
            final TarEntry entry = new TarEntry( storedPath );
            entry.setModTime( file.lastModified() );
            if( !file.isDirectory() )
            {
                entry.setSize( file.length() );
                entry.setMode( tarFileSet.getMode() );
            }
            entry.setUserName( tarFileSet.getUserName() );
            entry.setGroupName( tarFileSet.getGroup() );

            output.putNextEntry( entry );

            if( !file.isDirectory() )
            {
                input = new FileInputStream( file );
                IOUtil.copy( input, output );
            }

            output.closeEntry();
        }
        finally
        {
            IOUtil.shutdownStream( input );
        }
    }
}
