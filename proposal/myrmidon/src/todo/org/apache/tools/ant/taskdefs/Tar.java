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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.MergingMapper;
import org.apache.tools.ant.util.SourceFileScanner;
import org.apache.tools.tar.TarConstants;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;

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
            TarFileSet mainFileSet = new TarFileSet( fileset );
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
            String[] files = fs.getFiles( getProject() );

            if( !archiveIsUpToDate( files ) )
            {
                upToDate = false;
            }

            for( int i = 0; i < files.length; ++i )
            {
                if( tarFile.equals( new File( fs.getDir( getProject() ), files[ i ] ) ) )
                {
                    throw new TaskException( "A tar file cannot include itself" );
                }
            }
        }

        if( upToDate )
        {
            log( "Nothing to do: " + tarFile.getAbsolutePath() + " is up to date.",
                 Project.MSG_INFO );
            return;
        }

        log( "Building tar: " + tarFile.getAbsolutePath(), Project.MSG_INFO );

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
                String[] files = fs.getFiles( getProject() );
                for( int i = 0; i < files.length; i++ )
                {
                    File f = new File( fs.getDir( getProject() ), files[ i ] );
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

    protected boolean archiveIsUpToDate( String[] files )
        throws TaskException
    {
        SourceFileScanner sfs = new SourceFileScanner( this );
        MergingMapper mm = new MergingMapper();
        mm.setTo( tarFile.getAbsolutePath() );
        return sfs.restrict( files, baseDir, null, mm ).length == 0;
    }

    protected void tarFile( File file, TarOutputStream tOut, String vPath,
                            TarFileSet tarFileSet )
        throws IOException, TaskException
    {
        FileInputStream fIn = null;

        // don't add "" to the archive
        if( vPath.length() <= 0 )
        {
            return;
        }

        if( file.isDirectory() && !vPath.endsWith( "/" ) )
        {
            vPath += "/";
        }

        try
        {
            if( vPath.length() >= TarConstants.NAMELEN )
            {
                if( longFileMode.isOmitMode() )
                {
                    log( "Omitting: " + vPath, Project.MSG_INFO );
                    return;
                }
                else if( longFileMode.isWarnMode() )
                {
                    log( "Entry: " + vPath + " longer than " +
                         TarConstants.NAMELEN + " characters.", Project.MSG_WARN );
                    if( !longWarningGiven )
                    {
                        log( "Resulting tar file can only be processed successfully"
                             + " by GNU compatible tar commands", Project.MSG_WARN );
                        longWarningGiven = true;
                    }
                }
                else if( longFileMode.isFailMode() )
                {
                    throw new TaskException(
                        "Entry: " + vPath + " longer than " +
                        TarConstants.NAMELEN + "characters." );
                }
            }

            TarEntry te = new TarEntry( vPath );
            te.setModTime( file.lastModified() );
            if( !file.isDirectory() )
            {
                te.setSize( file.length() );
                te.setMode( tarFileSet.getMode() );
            }
            te.setUserName( tarFileSet.getUserName() );
            te.setGroupName( tarFileSet.getGroup() );

            tOut.putNextEntry( te );

            if( !file.isDirectory() )
            {
                fIn = new FileInputStream( file );

                byte[] buffer = new byte[ 8 * 1024 ];
                int count = 0;
                do
                {
                    tOut.write( buffer, 0, count );
                    count = fIn.read( buffer, 0, buffer.length );
                } while( count != -1 );
            }

            tOut.closeEntry();
        }
        finally
        {
            if( fIn != null )
                fIn.close();
        }
    }

    public static class TarFileSet extends FileSet
    {
        private String[] files = null;

        private int mode = 0100644;

        private String userName = "";
        private String groupName = "";

        public TarFileSet( FileSet fileset )
        {
            super( fileset );
        }

        public TarFileSet()
        {
            super();
        }

        public void setGroup( String groupName )
        {
            this.groupName = groupName;
        }

        public void setMode( String octalString )
        {
            this.mode = 0100000 | Integer.parseInt( octalString, 8 );
        }

        public void setUserName( String userName )
        {
            this.userName = userName;
        }

        /**
         * Get a list of files and directories specified in the fileset.
         *
         * @param p Description of Parameter
         * @return a list of file and directory names, relative to the baseDir
         *      for the project.
         */
        public String[] getFiles( Project p )
            throws TaskException
        {
            if( files == null )
            {
                DirectoryScanner ds = getDirectoryScanner( p );
                String[] directories = ds.getIncludedDirectories();
                String[] filesPerSe = ds.getIncludedFiles();
                files = new String[ directories.length + filesPerSe.length ];
                System.arraycopy( directories, 0, files, 0, directories.length );
                System.arraycopy( filesPerSe, 0, files, directories.length,
                                  filesPerSe.length );
            }

            return files;
        }

        public String getGroup()
        {
            return groupName;
        }

        public int getMode()
        {
            return mode;
        }

        public String getUserName()
        {
            return userName;
        }

    }

    /**
     * Valid Modes for LongFile attribute to Tar Task
     *
     * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
     */
    public static class TarLongFileMode extends EnumeratedAttribute
    {
        // permissable values for longfile attribute
        public final static String WARN = "warn";
        public final static String FAIL = "fail";
        public final static String TRUNCATE = "truncate";
        public final static String GNU = "gnu";
        public final static String OMIT = "omit";

        private final String[] validModes = {WARN, FAIL, TRUNCATE, GNU, OMIT};

        public TarLongFileMode()
            throws TaskException
        {
            super();
            setValue( WARN );
        }

        public String[] getValues()
        {
            return validModes;
        }

        public boolean isFailMode()
        {
            return FAIL.equalsIgnoreCase( getValue() );
        }

        public boolean isGnuMode()
        {
            return GNU.equalsIgnoreCase( getValue() );
        }

        public boolean isOmitMode()
        {
            return OMIT.equalsIgnoreCase( getValue() );
        }

        public boolean isTruncateMode()
        {
            return TRUNCATE.equalsIgnoreCase( getValue() );
        }

        public boolean isWarnMode()
        {
            return WARN.equalsIgnoreCase( getValue() );
        }
    }
}
