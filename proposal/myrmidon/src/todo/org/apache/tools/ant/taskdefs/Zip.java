/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import java.util.zip.CRC32;
import java.util.zip.ZipInputStream;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.types.ZipScanner;
import org.apache.tools.ant.util.MergingMapper;
import org.apache.tools.ant.util.SourceFileScanner;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;

/**
 * Create a ZIP archive.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Zip extends MatchingTask
{

    // For directories:
    private final static long EMPTY_CRC = new CRC32().getValue();
    private boolean doCompress = true;
    private boolean doUpdate = false;
    private boolean doFilesonly = false;
    protected String archiveType = "zip";
    protected String emptyBehavior = "skip";
    private Vector filesets = new Vector();
    protected Hashtable addedDirs = new Hashtable();
    private Vector addedFiles = new Vector();

    protected File zipFile;

    /**
     * true when we are adding new files into the Zip file, as opposed to adding
     * back the unchanged files
     */
    private boolean addingNewFiles;
    private File baseDir;

    /**
     * Encoding to use for filenames, defaults to the platform's default
     * encoding.
     */
    private String encoding;

    protected static String[][] grabFileNames( FileScanner[] scanners )
        throws TaskException
    {
        String[][] result = new String[ scanners.length ][];
        for( int i = 0; i < scanners.length; i++ )
        {
            String[] files = scanners[ i ].getIncludedFiles();
            String[] dirs = scanners[ i ].getIncludedDirectories();
            result[ i ] = new String[ files.length + dirs.length ];
            System.arraycopy( files, 0, result[ i ], 0, files.length );
            System.arraycopy( dirs, 0, result[ i ], files.length, dirs.length );
        }
        return result;
    }

    protected static File[] grabFiles( FileScanner[] scanners,
                                       String[][] fileNames )
    {
        Vector files = new Vector();
        for( int i = 0; i < fileNames.length; i++ )
        {
            File thisBaseDir = scanners[ i ].getBasedir();
            for( int j = 0; j < fileNames[ i ].length; j++ )
                files.addElement( new File( thisBaseDir, fileNames[ i ][ j ] ) );
        }
        File[] toret = new File[ files.size() ];
        files.copyInto( toret );
        return toret;
    }

    /**
     * This is the base directory to look in for things to zip.
     *
     * @param baseDir The new Basedir value
     */
    public void setBasedir( File baseDir )
    {
        this.baseDir = baseDir;
    }

    /**
     * Sets whether we want to compress the files or only store them.
     *
     * @param c The new Compress value
     */
    public void setCompress( boolean c )
    {
        doCompress = c;
    }

    /**
     * Encoding to use for filenames, defaults to the platform's default
     * encoding. <p>
     *
     * For a list of possible values see <a
     * href="http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html">
     * http://java.sun.com/products/jdk/1.2/docs/guide/internat/encoding.doc.html
     * </a>.</p>
     *
     * @param encoding The new Encoding value
     */
    public void setEncoding( String encoding )
    {
        this.encoding = encoding;
    }

    /**
     * This is the name/location of where to create the .zip file.
     *
     * @param file The new File value
     */
    public void setFile( File file )
    {
        this.zipFile = file;
    }

    /**
     * Emulate Sun's jar utility by not adding parent dirs
     *
     * @param f The new Filesonly value
     */
    public void setFilesonly( boolean f )
    {
        doFilesonly = f;
    }

    /**
     * Sets whether we want to update the file (if it exists) or create a new
     * one.
     *
     * @param c The new Update value
     */
    public void setUpdate( boolean c )
    {
        doUpdate = c;
    }

    /**
     * Sets behavior of the task when no files match. Possible values are:
     * <code>fail</code> (throw an exception and halt the build); <code>skip</code>
     * (do not create any archive, but issue a warning); <code>create</code>
     * (make an archive with no entries). Default for zip tasks is <code>skip</code>
     * ; for jar tasks, <code>create</code>.
     *
     * @param we The new Whenempty value
     */
    public void setWhenempty( WhenEmpty we )
    {
        emptyBehavior = we.getValue();
    }

    /**
     * Are we updating an existing archive?
     *
     * @return The InUpdateMode value
     */
    public boolean isInUpdateMode()
    {
        return doUpdate;
    }

    /**
     * Adds a set of files (nested fileset attribute).
     *
     * @param set The feature to be added to the Fileset attribute
     */
    public void addFileset( FileSet set )
    {
        filesets.addElement( set );
    }

    /**
     * Adds a set of files (nested zipfileset attribute) that can be read from
     * an archive and be given a prefix/fullpath.
     *
     * @param set The feature to be added to the Zipfileset attribute
     */
    public void addZipfileset( ZipFileSet set )
    {
        filesets.addElement( set );
    }

    public void execute()
        throws TaskException
    {
        if( baseDir == null && filesets.size() == 0 && "zip".equals( archiveType ) )
        {
            throw new TaskException( "basedir attribute must be set, or at least " +
                                     "one fileset must be given!" );
        }

        if( zipFile == null )
        {
            throw new TaskException( "You must specify the " + archiveType + " file to create!" );
        }

        // Renamed version of original file, if it exists
        File renamedFile = null;
        // Whether or not an actual update is required -
        // we don't need to update if the original file doesn't exist

        addingNewFiles = true;
        doUpdate = doUpdate && zipFile.exists();
        if( doUpdate )
        {
            try
            {
                renamedFile = File.createTempFile( "zip", ".tmp",
                                                   zipFile.getParentFile() );
            }
            catch( final IOException ioe )
            {
                throw new TaskException( ioe.toString(), ioe );
            }

            try
            {
                if( !zipFile.renameTo( renamedFile ) )
                {
                    throw new TaskException( "Unable to rename old file to temporary file" );
                }
            }
            catch( SecurityException e )
            {
                throw new TaskException( "Not allowed to rename old file to temporary file" );
            }
        }

        // Create the scanners to pass to isUpToDate().
        Vector dss = new Vector();
        if( baseDir != null )
        {
            dss.addElement( getDirectoryScanner( baseDir ) );
        }
        for( int i = 0; i < filesets.size(); i++ )
        {
            FileSet fs = (FileSet)filesets.elementAt( i );
            dss.addElement( fs.getDirectoryScanner( project ) );
        }
        int dssSize = dss.size();
        FileScanner[] scanners = new FileScanner[ dssSize ];
        dss.copyInto( scanners );

        // quick exit if the target is up to date
        // can also handle empty archives
        if( isUpToDate( scanners, zipFile ) )
        {
            return;
        }

        String action = doUpdate ? "Updating " : "Building ";

        getLogger().info( action + archiveType + ": " + zipFile.getAbsolutePath() );

        boolean success = false;
        try
        {
            ZipOutputStream zOut =
                new ZipOutputStream( new FileOutputStream( zipFile ) );
            zOut.setEncoding( encoding );
            try
            {
                if( doCompress )
                {
                    zOut.setMethod( ZipOutputStream.DEFLATED );
                }
                else
                {
                    zOut.setMethod( ZipOutputStream.STORED );
                }
                initZipOutputStream( zOut );

                // Add the implicit fileset to the archive.
                if( baseDir != null )
                {
                    addFiles( getDirectoryScanner( baseDir ), zOut, "", "" );
                }
                // Add the explicit filesets to the archive.
                addFiles( filesets, zOut );
                if( doUpdate )
                {
                    addingNewFiles = false;
                    ZipFileSet oldFiles = new ZipFileSet();
                    oldFiles.setSrc( renamedFile );

                    StringBuffer exclusionPattern = new StringBuffer();
                    for( int i = 0; i < addedFiles.size(); i++ )
                    {
                        if( i != 0 )
                        {
                            exclusionPattern.append( "," );
                        }
                        exclusionPattern.append( (String)addedFiles.elementAt( i ) );
                    }
                    oldFiles.setExcludes( exclusionPattern.toString() );
                    Vector tmp = new Vector();
                    tmp.addElement( oldFiles );
                    addFiles( tmp, zOut );
                }
                finalizeZipOutputStream( zOut );
                success = true;
            }
            finally
            {
                // Close the output stream.
                try
                {
                    if( zOut != null )
                    {
                        zOut.close();
                    }
                }
                catch( IOException ex )
                {
                    // If we're in this finally clause because of an exception, we don't
                    // really care if there's an exception when closing the stream. E.g. if it
                    // throws "ZIP file must have at least one entry", because an exception happened
                    // before we added any files, then we must swallow this exception. Otherwise,
                    // the error that's reported will be the close() error, which is not the real
                    // cause of the problem.
                    if( success )
                        throw ex;
                }
            }
        }
        catch( IOException ioe )
        {
            String msg = "Problem creating " + archiveType + ": " + ioe.getMessage();

            // delete a bogus ZIP file
            if( !zipFile.delete() )
            {
                msg += " (and the archive is probably corrupt but I could not delete it)";
            }

            if( doUpdate )
            {
                if( !renamedFile.renameTo( zipFile ) )
                {
                    msg += " (and I couldn't rename the temporary file " +
                        renamedFile.getName() + " back)";
                }
            }

            throw new TaskException( msg, ioe );
        }
        finally
        {
            cleanUp();
        }

        // If we've been successful on an update, delete the temporary file
        if( success && doUpdate )
        {
            if( !renamedFile.delete() )
            {
                log( "Warning: unable to delete temporary file " +
                     renamedFile.getName(), Project.MSG_WARN );
            }
        }
    }

    /**
     * Indicates if the task is adding new files into the archive as opposed to
     * copying back unchanged files from the backup copy
     *
     * @return The AddingNewFiles value
     */
    protected boolean isAddingNewFiles()
    {
        return addingNewFiles;
    }

    /**
     * Check whether the archive is up-to-date; and handle behavior for empty
     * archives.
     *
     * @param scanners list of prepared scanners containing files to archive
     * @param zipFile intended archive file (may or may not exist)
     * @return true if nothing need be done (may have done something already);
     *      false if archive creation should proceed
     * @exception TaskException if it likes
     */
    protected boolean isUpToDate( FileScanner[] scanners, File zipFile )
        throws TaskException
    {
        String[][] fileNames = grabFileNames( scanners );
        File[] files = grabFiles( scanners, fileNames );
        if( files.length == 0 )
        {
            if( emptyBehavior.equals( "skip" ) )
            {
                log( "Warning: skipping " + archiveType + " archive " + zipFile +
                     " because no files were included.", Project.MSG_WARN );
                return true;
            }
            else if( emptyBehavior.equals( "fail" ) )
            {
                throw new TaskException( "Cannot create " + archiveType + " archive " + zipFile +
                                         ": no files were included." );
            }
            else
            {
                // Create.
                return createEmptyZip( zipFile );
            }
        }
        else
        {
            for( int i = 0; i < files.length; ++i )
            {
                if( files[ i ].equals( zipFile ) )
                {
                    throw new TaskException( "A zip file cannot include itself" );
                }
            }

            if( !zipFile.exists() )
                return false;

            SourceFileScanner sfs = new SourceFileScanner( this );
            MergingMapper mm = new MergingMapper();
            mm.setTo( zipFile.getAbsolutePath() );
            for( int i = 0; i < scanners.length; i++ )
            {
                if( sfs.restrict( fileNames[ i ], scanners[ i ].getBasedir(), null,
                                  mm ).length > 0 )
                {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Add all files of the given FileScanner to the ZipOutputStream prependig
     * the given prefix to each filename. <p>
     *
     * Ensure parent directories have been added as well.
     *
     * @param scanner The feature to be added to the Files attribute
     * @param zOut The feature to be added to the Files attribute
     * @param prefix The feature to be added to the Files attribute
     * @param fullpath The feature to be added to the Files attribute
     * @exception IOException Description of Exception
     */
    protected void addFiles( FileScanner scanner, ZipOutputStream zOut,
                             String prefix, String fullpath )
        throws IOException, TaskException
    {
        if( prefix.length() > 0 && fullpath.length() > 0 )
            throw new TaskException( "Both prefix and fullpath attributes may not be set on the same fileset." );

        File thisBaseDir = scanner.getBasedir();

        // directories that matched include patterns
        String[] dirs = scanner.getIncludedDirectories();
        if( dirs.length > 0 && fullpath.length() > 0 )
            throw new TaskException( "fullpath attribute may only be specified for filesets that specify a single file." );
        for( int i = 0; i < dirs.length; i++ )
        {
            if( "".equals( dirs[ i ] ) )
            {
                continue;
            }
            String name = dirs[ i ].replace( File.separatorChar, '/' );
            if( !name.endsWith( "/" ) )
            {
                name += "/";
            }
            addParentDirs( thisBaseDir, name, zOut, prefix );
        }

        // files that matched include patterns
        String[] files = scanner.getIncludedFiles();
        if( files.length > 1 && fullpath.length() > 0 )
            throw new TaskException( "fullpath attribute may only be specified for filesets that specify a single file." );
        for( int i = 0; i < files.length; i++ )
        {
            File f = new File( thisBaseDir, files[ i ] );
            if( fullpath.length() > 0 )
            {
                // Add this file at the specified location.
                addParentDirs( null, fullpath, zOut, "" );
                zipFile( f, zOut, fullpath );
            }
            else
            {
                // Add this file with the specified prefix.
                String name = files[ i ].replace( File.separatorChar, '/' );
                addParentDirs( thisBaseDir, name, zOut, prefix );
                zipFile( f, zOut, prefix + name );
            }
        }
    }

    /**
     * Iterate over the given Vector of (zip)filesets and add all files to the
     * ZipOutputStream using the given prefix or fullpath.
     *
     * @param filesets The feature to be added to the Files attribute
     * @param zOut The feature to be added to the Files attribute
     * @exception IOException Description of Exception
     */
    protected void addFiles( Vector filesets, ZipOutputStream zOut )
        throws IOException, TaskException
    {
        // Add each fileset in the Vector.
        for( int i = 0; i < filesets.size(); i++ )
        {
            FileSet fs = (FileSet)filesets.elementAt( i );
            DirectoryScanner ds = fs.getDirectoryScanner( project );

            String prefix = "";
            String fullpath = "";
            if( fs instanceof ZipFileSet )
            {
                ZipFileSet zfs = (ZipFileSet)fs;
                prefix = zfs.getPrefix();
                fullpath = zfs.getFullpath();
            }

            if( prefix.length() > 0
                && !prefix.endsWith( "/" )
                && !prefix.endsWith( "\\" ) )
            {
                prefix += "/";
            }

            // Need to manually add either fullpath's parent directory, or
            // the prefix directory, to the archive.
            if( prefix.length() > 0 )
            {
                addParentDirs( null, prefix, zOut, "" );
                zipDir( null, zOut, prefix );
            }
            else if( fullpath.length() > 0 )
            {
                addParentDirs( null, fullpath, zOut, "" );
            }

            if( fs instanceof ZipFileSet
                && ( (ZipFileSet)fs ).getSrc() != null )
            {
                addZipEntries( (ZipFileSet)fs, ds, zOut, prefix, fullpath );
            }
            else
            {
                // Add the fileset.
                addFiles( ds, zOut, prefix, fullpath );
            }
        }
    }

    /**
     * Ensure all parent dirs of a given entry have been added.
     *
     * @param baseDir The feature to be added to the ParentDirs attribute
     * @param entry The feature to be added to the ParentDirs attribute
     * @param zOut The feature to be added to the ParentDirs attribute
     * @param prefix The feature to be added to the ParentDirs attribute
     * @exception IOException Description of Exception
     */
    protected void addParentDirs( File baseDir, String entry,
                                  ZipOutputStream zOut, String prefix )
        throws IOException
    {
        if( !doFilesonly )
        {
            Stack directories = new Stack();
            int slashPos = entry.length();

            while( ( slashPos = entry.lastIndexOf( (int)'/', slashPos - 1 ) ) != -1 )
            {
                String dir = entry.substring( 0, slashPos + 1 );
                if( addedDirs.get( prefix + dir ) != null )
                {
                    break;
                }
                directories.push( dir );
            }

            while( !directories.isEmpty() )
            {
                String dir = (String)directories.pop();
                File f = null;
                if( baseDir != null )
                {
                    f = new File( baseDir, dir );
                }
                else
                {
                    f = new File( dir );
                }
                zipDir( f, zOut, prefix + dir );
            }
        }
    }

    protected void addZipEntries( ZipFileSet fs, DirectoryScanner ds,
                                  ZipOutputStream zOut, String prefix, String fullpath )
        throws IOException, TaskException
    {
        if( prefix.length() > 0 && fullpath.length() > 0 )
            throw new TaskException( "Both prefix and fullpath attributes may not be set on the same fileset." );

        ZipScanner zipScanner = (ZipScanner)ds;
        File zipSrc = fs.getSrc();

        ZipEntry entry;
        java.util.zip.ZipEntry origEntry;
        ZipInputStream in = null;
        try
        {
            in = new ZipInputStream( new FileInputStream( zipSrc ) );

            while( ( origEntry = in.getNextEntry() ) != null )
            {
                entry = new ZipEntry( origEntry );
                String vPath = entry.getName();
                if( zipScanner.match( vPath ) )
                {
                    if( fullpath.length() > 0 )
                    {
                        addParentDirs( null, fullpath, zOut, "" );
                        zipFile( in, zOut, fullpath, entry.getTime() );
                    }
                    else
                    {
                        addParentDirs( null, vPath, zOut, prefix );
                        if( !entry.isDirectory() )
                        {
                            zipFile( in, zOut, prefix + vPath, entry.getTime() );
                        }
                    }
                }
            }
        }
        finally
        {
            if( in != null )
            {
                in.close();
            }
        }
    }

    /**
     * Do any clean up necessary to allow this instance to be used again. <p>
     *
     * When we get here, the Zip file has been closed and all we need to do is
     * to reset some globals.</p>
     */
    protected void cleanUp()
    {
        addedDirs = new Hashtable();
        addedFiles = new Vector();
        filesets = new Vector();
        zipFile = null;
        baseDir = null;
        doCompress = true;
        doUpdate = false;
        doFilesonly = false;
        addingNewFiles = false;
        encoding = null;
    }

    /**
     * Create an empty zip file
     *
     * @param zipFile Description of Parameter
     * @return true if the file is then considered up to date.
     */
    protected boolean createEmptyZip( File zipFile )
        throws TaskException
    {
        // In this case using java.util.zip will not work
        // because it does not permit a zero-entry archive.
        // Must create it manually.
        log( "Note: creating empty " + archiveType + " archive " + zipFile, Project.MSG_INFO );
        try
        {
            OutputStream os = new FileOutputStream( zipFile );
            try
            {
                // Cf. PKZIP specification.
                byte[] empty = new byte[ 22 ];
                empty[ 0 ] = 80;// P
                empty[ 1 ] = 75;// K
                empty[ 2 ] = 5;
                empty[ 3 ] = 6;
                // remainder zeros
                os.write( empty );
            }
            finally
            {
                os.close();
            }
        }
        catch( IOException ioe )
        {
            throw new TaskException( "Could not create empty ZIP archive", ioe );
        }
        return true;
    }

    protected void finalizeZipOutputStream( ZipOutputStream zOut )
        throws IOException, TaskException
    {
    }

    protected void initZipOutputStream( ZipOutputStream zOut )
        throws IOException, TaskException
    {
    }

    protected void zipDir( File dir, ZipOutputStream zOut, String vPath )
        throws IOException
    {
        if( addedDirs.get( vPath ) != null )
        {
            // don't add directories we've already added.
            // no warning if we try, it is harmless in and of itself
            return;
        }
        addedDirs.put( vPath, vPath );

        ZipEntry ze = new ZipEntry( vPath );
        if( dir != null && dir.exists() )
        {
            ze.setTime( dir.lastModified() );
        }
        else
        {
            ze.setTime( System.currentTimeMillis() );
        }
        ze.setSize( 0 );
        ze.setMethod( ZipEntry.STORED );
        // This is faintly ridiculous:
        ze.setCrc( EMPTY_CRC );

        // this is 040775 | MS-DOS directory flag in reverse byte order
        ze.setExternalAttributes( 0x41FD0010L );

        zOut.putNextEntry( ze );
    }

    protected void zipFile( InputStream in, ZipOutputStream zOut, String vPath,
                            long lastModified )
        throws IOException, TaskException
    {
        ZipEntry ze = new ZipEntry( vPath );
        ze.setTime( lastModified );

        /*
         * XXX ZipOutputStream.putEntry expects the ZipEntry to know its
         * size and the CRC sum before you start writing the data when using
         * STORED mode.
         *
         * This forces us to process the data twice.
         *
         * I couldn't find any documentation on this, just found out by try
         * and error.
         */
        if( !doCompress )
        {
            long size = 0;
            CRC32 cal = new CRC32();
            if( !in.markSupported() )
            {
                // Store data into a byte[]
                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                byte[] buffer = new byte[ 8 * 1024 ];
                int count = 0;
                do
                {
                    size += count;
                    cal.update( buffer, 0, count );
                    bos.write( buffer, 0, count );
                    count = in.read( buffer, 0, buffer.length );
                } while( count != -1 );
                in = new ByteArrayInputStream( bos.toByteArray() );

            }
            else
            {
                in.mark( Integer.MAX_VALUE );
                byte[] buffer = new byte[ 8 * 1024 ];
                int count = 0;
                do
                {
                    size += count;
                    cal.update( buffer, 0, count );
                    count = in.read( buffer, 0, buffer.length );
                } while( count != -1 );
                in.reset();
            }
            ze.setSize( size );
            ze.setCrc( cal.getValue() );
        }

        zOut.putNextEntry( ze );

        byte[] buffer = new byte[ 8 * 1024 ];
        int count = 0;
        do
        {
            if( count != 0 )
            {
                zOut.write( buffer, 0, count );
            }
            count = in.read( buffer, 0, buffer.length );
        } while( count != -1 );
        addedFiles.addElement( vPath );
    }

    protected void zipFile( File file, ZipOutputStream zOut, String vPath )
        throws IOException, TaskException
    {
        if( file.equals( zipFile ) )
        {
            throw new TaskException( "A zip file cannot include itself" );
        }

        FileInputStream fIn = new FileInputStream( file );
        try
        {
            zipFile( fIn, zOut, vPath, file.lastModified() );
        }
        finally
        {
            fIn.close();
        }
    }

    /**
     * Possible behaviors when there are no matching files for the task.
     *
     * @author RT
     */
    public static class WhenEmpty extends EnumeratedAttribute
    {
        public String[] getValues()
        {
            return new String[]{"fail", "skip", "create"};
        }
    }
}
