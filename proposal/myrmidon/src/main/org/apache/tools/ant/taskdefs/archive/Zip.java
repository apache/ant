/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.archive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Stack;
import java.util.zip.CRC32;
import java.util.zip.ZipInputStream;
import org.apache.aut.zip.ZipEntry;
import org.apache.aut.zip.ZipOutputStream;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.DirectoryScanner;
import org.apache.tools.ant.types.FileScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ScannerUtil;
import org.apache.tools.ant.types.SourceFileScanner;
import org.apache.tools.ant.util.mappers.MergingMapper;

/**
 * Create a ZIP archive.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Zip
    extends MatchingTask
{
    // For directories:
    private final static long EMPTY_CRC = new CRC32().getValue();
    private boolean m_compress = true;
    private boolean m_update;
    private boolean m_filesonly;
    protected String m_archiveType = "zip";
    protected String m_emptyBehavior = "skip";
    private ArrayList m_filesets = new ArrayList();
    protected Hashtable m_addedDirs = new Hashtable();
    private ArrayList m_addedFiles = new ArrayList();
    protected File m_file;

    /**
     * true when we are adding new files into the Zip file, as opposed to adding
     * back the unchanged files
     */
    private boolean m_addingNewFiles;
    private File m_baseDir;

    /**
     * Encoding to use for filenames, defaults to the platform's default
     * encoding.
     */
    private String m_encoding;

    private static String[][] grabFileNames( final FileScanner[] scanners )
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

    private static File[] grabFiles( final FileScanner[] scanners,
                                     final String[][] filenames )
    {
        final ArrayList files = new ArrayList();
        for( int i = 0; i < filenames.length; i++ )
        {
            final File baseDir = scanners[ i ].getBasedir();
            for( int j = 0; j < filenames[ i ].length; j++ )
            {
                files.add( new File( baseDir, filenames[ i ][ j ] ) );
            }
        }
        final File[] toret = new File[ files.size() ];
        return (File[])files.toArray( toret );
    }

    /**
     * This is the base directory to look in for things to zip.
     *
     * @param baseDir The new Basedir value
     */
    public void setBasedir( final File baseDir )
    {
        m_baseDir = baseDir;
    }

    /**
     * Sets whether we want to compress the files or only store them.
     *
     * @param compress The new Compress value
     */
    public void setCompress( final boolean compress )
    {
        m_compress = compress;
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
    public void setEncoding( final String encoding )
    {
        m_encoding = encoding;
    }

    /**
     * This is the name/location of where to create the .zip file.
     *
     * @param file The new File value
     */
    public void setFile( final File file )
    {
        m_file = file;
    }

    /**
     * Emulate Sun's jar utility by not adding parent dirs
     */
    public void setFilesonly( final boolean filesonly )
    {
        m_filesonly = filesonly;
    }

    /**
     * Sets whether we want to update the file (if it exists) or create a new
     * one.
     */
    public void setUpdate( final boolean update )
    {
        m_update = update;
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
    public void setWhenempty( final WhenEmpty we )
    {
        m_emptyBehavior = we.getValue();
    }

    /**
     * Are we updating an existing archive?
     *
     * @return The InUpdateMode value
     */
    protected final boolean isInUpdateMode()
    {
        return m_update;
    }

    /**
     * Adds a set of files (nested fileset attribute).
     */
    public void addFileset( final FileSet set )
    {
        m_filesets.add( set );
    }

    /**
     * Adds a set of files (nested zipfileset attribute) that can be read from
     * an archive and be given a prefix/fullpath.
     *
     * @param set The feature to be added to the Zipfileset attribute
     */
    public void addZipfileset( final ZipFileSet set )
    {
        m_filesets.add( set );
    }

    public void execute()
        throws TaskException
    {
        if( m_baseDir == null && m_filesets.size() == 0 &&
            "zip".equals( m_archiveType ) )
        {
            final String message = "basedir attribute must be set, or at least " +
                "one fileset must be given!";
            throw new TaskException( message );
        }

        if( m_file == null )
        {
            final String message = "You must specify the " +
                m_archiveType + " file to create!";
            throw new TaskException( message );
        }

        // Renamed version of original file, if it exists
        File renamedFile = null;
        // Whether or not an actual update is required -
        // we don't need to update if the original file doesn't exist

        m_addingNewFiles = true;
        m_update = m_update && m_file.exists();
        if( m_update )
        {
            try
            {
                renamedFile = File.createTempFile( "zip", ".tmp",
                                                   m_file.getParentFile() );
            }
            catch( final IOException ioe )
            {
                throw new TaskException( ioe.toString(), ioe );
            }

            try
            {
                if( !m_file.renameTo( renamedFile ) )
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
        ArrayList dss = new ArrayList();
        if( m_baseDir != null )
        {
            dss.add( getDirectoryScanner( m_baseDir ) );
        }
        final int size = m_filesets.size();
        for( int i = 0; i < size; i++ )
        {
            final FileSet fileSet = (FileSet)m_filesets.get( i );
            final DirectoryScanner scanner = getScanner( fileSet );
            dss.add( scanner );
        }
        int dssSize = dss.size();
        FileScanner[] scanners = new FileScanner[ dssSize ];
        scanners = (FileScanner[])dss.toArray( scanners );

        // quick exit if the target is up to date
        // can also handle empty archives
        if( isUpToDate( scanners, m_file ) )
        {
            return;
        }

        String action = m_update ? "Updating " : "Building ";

        getLogger().info( action + m_archiveType + ": " + m_file.getAbsolutePath() );

        boolean success = false;
        try
        {
            ZipOutputStream zOut =
                new ZipOutputStream( new FileOutputStream( m_file ) );
            zOut.setEncoding( m_encoding );
            try
            {
                if( m_compress )
                {
                    zOut.setMethod( ZipOutputStream.DEFLATED );
                }
                else
                {
                    zOut.setMethod( ZipOutputStream.STORED );
                }
                initZipOutputStream( zOut );

                // Add the implicit fileset to the archive.
                if( m_baseDir != null )
                {
                    addFiles( getDirectoryScanner( m_baseDir ), zOut, "", "" );
                }
                // Add the explicit filesets to the archive.
                addFiles( m_filesets, zOut );
                if( m_update )
                {
                    m_addingNewFiles = false;
                    ZipFileSet oldFiles = new ZipFileSet();
                    oldFiles.setSrc( renamedFile );

                    StringBuffer exclusionPattern = new StringBuffer();
                    final int addedFilesCount = m_addedFiles.size();
                    for( int i = 0; i < addedFilesCount; i++ )
                    {
                        if( i != 0 )
                        {
                            exclusionPattern.append( "," );
                        }
                        exclusionPattern.append( (String)m_addedFiles.get( i ) );
                    }
                    oldFiles.setExcludes( exclusionPattern.toString() );
                    ArrayList tmp = new ArrayList();
                    tmp.add( oldFiles );
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
                    {
                        throw ex;
                    }
                }
            }
        }
        catch( IOException ioe )
        {
            String msg = "Problem creating " + m_archiveType + ": " + ioe.getMessage();

            // delete a bogus ZIP file
            if( !m_file.delete() )
            {
                msg += " (and the archive is probably corrupt but I could not delete it)";
            }

            if( m_update )
            {
                if( !renamedFile.renameTo( m_file ) )
                {
                    msg += " (and I couldn't rename the temporary file " +
                        renamedFile.getName() + " back)";
                }
            }

            throw new TaskException( msg, ioe );
        }

        // If we've been successful on an update, delete the temporary file
        if( success && m_update )
        {
            if( !renamedFile.delete() )
            {
                final String message = "Warning: unable to delete temporary file " +
                    renamedFile.getName();
                getLogger().warn( message );
            }
        }
    }

    private DirectoryScanner getScanner( final FileSet fileSet )
        throws TaskException
    {
        if( fileSet instanceof ZipFileSet )
        {
            final ZipFileSet zipFileSet = (ZipFileSet)fileSet;
            return ScannerUtil.getZipScanner( zipFileSet );
        }
        else
        {
            return ScannerUtil.getDirectoryScanner( fileSet );
        }
    }

    protected void addFileAs( final File file, final String name )
        throws TaskException
    {
        // Create a ZipFileSet for this file, and pass it up.
        final ZipFileSet fs = new ZipFileSet();
        fs.setDir( file.getParentFile() );
        fs.setIncludes( file.getName() );
        fs.setFullpath( name );
        addFileset( fs );
    }

    /**
     * Indicates if the task is adding new files into the archive as opposed to
     * copying back unchanged files from the backup copy
     *
     * @return The AddingNewFiles value
     */
    protected final boolean isAddingNewFiles()
    {
        return m_addingNewFiles;
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
            if( m_emptyBehavior.equals( "skip" ) )
            {
                final String message = "Warning: skipping " + m_archiveType + " archive " + zipFile +
                    " because no files were included.";
                getLogger().warn( message );
                return true;
            }
            else if( m_emptyBehavior.equals( "fail" ) )
            {
                throw new TaskException( "Cannot create " + m_archiveType + " archive " + zipFile +
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
            {
                return false;
            }

            final SourceFileScanner scanner = new SourceFileScanner();
            setupLogger( scanner );
            MergingMapper mm = new MergingMapper();
            mm.setTo( zipFile.getAbsolutePath() );
            for( int i = 0; i < scanners.length; i++ )
            {
                if( scanner.restrict( fileNames[ i ], scanners[ i ].getBasedir(), null,
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
        {
            throw new TaskException( "Both prefix and fullpath attributes may not be set on the same fileset." );
        }

        File thisBaseDir = scanner.getBasedir();

        // directories that matched include patterns
        String[] dirs = scanner.getIncludedDirectories();
        if( dirs.length > 0 && fullpath.length() > 0 )
        {
            throw new TaskException( "fullpath attribute may only be specified for filesets that specify a single file." );
        }
        for( int i = 0; i < dirs.length; i++ )
        {
            final String dir = dirs[ i ];
            if( "".equals( dir ) )
            {
                continue;
            }
            final String name = getName( dir );
            addParentDirs( thisBaseDir, name, zOut, prefix );
        }

        // files that matched include patterns
        String[] files = scanner.getIncludedFiles();
        if( files.length > 1 && fullpath.length() > 0 )
        {
            throw new TaskException( "fullpath attribute may only be specified for filesets that specify a single file." );
        }
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

    private String getName( final String dir )
    {
        String name = dir.replace( File.separatorChar, '/' );
        if( !name.endsWith( "/" ) )
        {
            name += "/";
        }
        return name;
    }

    /**
     * Iterate over the given ArrayList of (zip)filesets and add all files to the
     * ZipOutputStream using the given prefix or fullpath.
     *
     * @param filesets The feature to be added to the Files attribute
     * @param zOut The feature to be added to the Files attribute
     * @exception IOException Description of Exception
     */
    protected void addFiles( ArrayList filesets, ZipOutputStream zOut )
        throws IOException, TaskException
    {
        // Add each fileset in the ArrayList.
        final int size = filesets.size();
        for( int i = 0; i < size; i++ )
        {
            FileSet fs = (FileSet)filesets.get( i );
            DirectoryScanner ds = getScanner( fs );

            String prefix = "";
            String fullpath = "";
            if( fs instanceof ZipFileSet )
            {
                ZipFileSet zfs = (ZipFileSet)fs;
                prefix = getPrefix( zfs.getPrefix() );
                fullpath = zfs.getFullpath();
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

    private String getPrefix( final String prefix )
    {
        String result = prefix;
        if( result.length() > 0
            && !result.endsWith( "/" )
            && !result.endsWith( "\\" ) )
        {
            result += "/";
        }
        return result;
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
        if( !m_filesonly )
        {
            Stack directories = new Stack();
            int slashPos = entry.length();

            while( ( slashPos = entry.lastIndexOf( '/', slashPos - 1 ) ) != -1 )
            {
                String dir = entry.substring( 0, slashPos + 1 );
                if( m_addedDirs.get( prefix + dir ) != null )
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
        {
            throw new TaskException( "Both prefix and fullpath attributes may not be set on the same fileset." );
        }

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
        getLogger().info( "Note: creating empty " + m_archiveType + " archive " + zipFile );
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
        if( m_addedDirs.get( vPath ) != null )
        {
            // don't add directories we've already added.
            // no warning if we try, it is harmless in and of itself
            return;
        }
        m_addedDirs.put( vPath, vPath );

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

    protected void zipFile( final InputStream input,
                            final ZipOutputStream output,
                            final String path,
                            final long lastModified )
        throws IOException, TaskException
    {
        final ZipEntry entry = new ZipEntry( path );
        entry.setTime( lastModified );

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

        InputStream inputToStore = input;
        if( !m_compress )
        {
            final CRC32 crc = new CRC32();
            long size = 0;
            if( !inputToStore.markSupported() )
            {
                // Store data into a byte[]
                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                byte[] buffer = new byte[ 8 * 1024 ];
                int count = 0;
                do
                {
                    size += count;
                    crc.update( buffer, 0, count );
                    bos.write( buffer, 0, count );
                    count = inputToStore.read( buffer, 0, buffer.length );
                } while( count != -1 );
                inputToStore = new ByteArrayInputStream( bos.toByteArray() );
            }
            else
            {
                inputToStore.mark( Integer.MAX_VALUE );
                byte[] buffer = new byte[ 8 * 1024 ];
                int count = 0;
                do
                {
                    size += count;
                    crc.update( buffer, 0, count );
                    count = inputToStore.read( buffer, 0, buffer.length );
                } while( count != -1 );
                inputToStore.reset();
            }
            entry.setSize( size );
            entry.setCrc( crc.getValue() );
        }

        output.putNextEntry( entry );

        IOUtil.copy( inputToStore, output );

        m_addedFiles.add( path );
    }

    protected void zipFile( final File file,
                            final ZipOutputStream zOut,
                            final String vPath )
        throws IOException, TaskException
    {
        if( file.equals( m_file ) )
        {
            final String message = "A zip file cannot include itself";
            throw new TaskException( message );
        }

        final FileInputStream fIn = new FileInputStream( file );
        try
        {
            zipFile( fIn, zOut, vPath, file.lastModified() );
        }
        finally
        {
            IOUtil.shutdownStream( fIn );
        }
    }
}
