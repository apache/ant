/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.net;

import com.oroinc.net.ftp.FTPClient;
import com.oroinc.net.ftp.FTPFile;
import com.oroinc.net.ftp.FTPReply;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ScannerUtil;

/**
 * Basic FTP client that performs the following actions:
 * <ul>
 *   <li> <strong>send</strong> - send files to a remote server. This is the
 *   default action.</li>
 *   <li> <strong>get</strong> - retrive files from a remote server.</li>
 *   <li> <strong>del</strong> - delete files from a remote server.</li>
 *   <li> <strong>list</strong> - create a file listing.</li>
 * </ul>
 * <strong>Note:</strong> Some FTP servers - notably the Solaris server - seem
 * to hold data ports open after a "retr" operation, allowing them to timeout
 * instead of shutting them down cleanly. This happens in active or passive
 * mode, and the ports will remain open even after ending the FTP session. FTP
 * "send" operations seem to close ports immediately. This behavior may cause
 * problems on some systems when downloading large sets of files.
 *
 * @author Roger Vaughn <a href="mailto:rvaughn@seaconinc.com">
 *      rvaughn@seaconinc.com</a>
 * @author Glenn McAllister <a href="mailto:glennm@ca.ibm.com">glennm@ca.ibm.com
 *      </a>
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 */
public class FTP
    extends Task
{
    protected final static int SEND_FILES = 0;
    protected final static int GET_FILES = 1;
    protected final static int DEL_FILES = 2;
    protected final static int LIST_FILES = 3;
    protected final static int MK_DIR = 4;

    protected final static String[] ACTION_STRS = new String[]
    {
        "sending",
        "getting",
        "deleting",
        "listing",
        "making directory"
    };

    protected final static String[] COMPLETED_ACTION_STRS = new String[]
    {
        "sent",
        "retrieved",
        "deleted",
        "listed",
        "created directory"
    };

    private boolean m_binary = true;
    private boolean m_passive;
    private boolean m_verbose;
    private boolean m_newerOnly;
    private int m_action = SEND_FILES;
    private ArrayList m_filesets = new ArrayList();
    private ArrayList m_dirCache = new ArrayList();
    private int m_transferred;
    private String m_remoteFileSep = "/";
    private int m_port = 21;
    private boolean m_skipFailedTransfers;
    private int m_skipped;
    private boolean m_ignoreNoncriticalErrors;
    private File m_listing;
    private String m_password;
    private String m_remotedir;
    private String m_server;
    private String m_userid;

    /**
     * Sets the FTP action to be taken. Currently accepts "put", "get", "del",
     * "mkdir" and "list".
     *
     * @param action The new Action value
     * @exception TaskException Description of Exception
     */
    public void setAction( Action action )
        throws TaskException
    {
        m_action = action.getAction();
    }

    /**
     * Specifies whether to use binary-mode or text-mode transfers. Set to true
     * to send binary mode. Binary mode is enabled by default.
     *
     * @param binary The new Binary value
     */
    public void setBinary( boolean binary )
    {
        m_binary = binary;
    }

    /**
     * A synonym for setNewer. Set to true to transmit only new or changed
     * files.
     *
     * @param depends The new Depends value
     */
    public void setDepends( boolean depends )
    {
        m_newerOnly = depends;
    }

    /**
     * set the flag to skip errors on dir creation (and maybe later other server
     * specific errors)
     *
     * @param ignoreNoncriticalErrors The new IgnoreNoncriticalErrors value
     */
    public void setIgnoreNoncriticalErrors( boolean ignoreNoncriticalErrors )
    {
        m_ignoreNoncriticalErrors = ignoreNoncriticalErrors;
    }

    /**
     * The output file for the "list" action. This attribute is ignored for any
     * other actions.
     *
     * @param listing The new Listing value
     * @exception TaskException Description of Exception
     */
    public void setListing( File listing )
        throws TaskException
    {
        m_listing = listing;
    }

    /**
     * Set to true to transmit only files that are new or changed from their
     * remote counterparts. The default is to transmit all files.
     *
     * @param newer The new Newer value
     */
    public void setNewer( boolean newer )
    {
        m_newerOnly = newer;
    }

    /**
     * Specifies whether to use passive mode. Set to true if you are behind a
     * firewall and cannot connect without it. Passive mode is disabled by
     * default.
     *
     * @param passive The new Passive value
     */
    public void setPassive( boolean passive )
    {
        m_passive = passive;
    }

    /**
     * Sets the login password for the given user id.
     *
     * @param password The new Password value
     */
    public void setPassword( String password )
    {
        m_password = password;
    }

    /**
     * Sets the FTP port used by the remote server.
     *
     * @param port The new Port value
     */
    public void setPort( int port )
    {
        m_port = port;
    }

    /**
     * Sets the remote directory where files will be placed. This may be a
     * relative or absolute path, and must be in the path syntax expected by the
     * remote server. No correction of path syntax will be performed.
     *
     * @param dir The new Remotedir value
     */
    public void setRemotedir( String dir )
    {
        m_remotedir = dir;
    }

    /**
     * Sets the remote file separator character. This normally defaults to the
     * Unix standard forward slash, but can be manually overridden using this
     * call if the remote server requires some other separator. Only the first
     * character of the string is used.
     *
     * @param separator The new Separator value
     */
    public void setSeparator( String separator )
    {
        m_remoteFileSep = separator;
    }

    /**
     * Sets the FTP server to send files to.
     *
     * @param server The new Server value
     */
    public void setServer( String server )
    {
        m_server = server;
    }

    /**
     * set the failed transfer flag
     *
     * @param skipFailedTransfers The new SkipFailedTransfers value
     */
    public void setSkipFailedTransfers( boolean skipFailedTransfers )
    {
        m_skipFailedTransfers = skipFailedTransfers;
    }

    /**
     * Sets the login user id to use on the specified server.
     *
     * @param userid The new Userid value
     */
    public void setUserid( String userid )
    {
        m_userid = userid;
    }

    /**
     * Set to true to receive notification about each file as it is transferred.
     *
     * @param verbose The new Verbose value
     */
    public void setVerbose( boolean verbose )
    {
        m_verbose = verbose;
    }

    /**
     * Adds a set of files (nested fileset attribute).
     *
     * @param set The feature to be added to the Fileset attribute
     */
    public void addFileset( FileSet set )
    {
        m_filesets.add( set );
    }

    /**
     * Runs the task.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        validate();

        FTPClient ftp = null;

        try
        {
            getLogger().debug( "Opening FTP connection to " + m_server );

            ftp = new FTPClient();

            ftp.connect( m_server, m_port );
            if( !FTPReply.isPositiveCompletion( ftp.getReplyCode() ) )
            {
                throw new TaskException( "FTP connection failed: " + ftp.getReplyString() );
            }

            getLogger().debug( "connected" );
            getLogger().debug( "logging in to FTP server" );

            if( !ftp.login( m_userid, m_password ) )
            {
                throw new TaskException( "Could not login to FTP server" );
            }

            getLogger().debug( "login succeeded" );

            if( m_binary )
            {
                ftp.setFileType( com.oroinc.net.ftp.FTP.IMAGE_FILE_TYPE );
                if( !FTPReply.isPositiveCompletion( ftp.getReplyCode() ) )
                {
                    throw new TaskException(
                        "could not set transfer type: " +
                        ftp.getReplyString() );
                }
            }

            if( m_passive )
            {
                getLogger().debug( "entering passive mode" );
                ftp.enterLocalPassiveMode();
                if( !FTPReply.isPositiveCompletion( ftp.getReplyCode() ) )
                {
                    throw new TaskException(
                        "could not enter into passive mode: " +
                        ftp.getReplyString() );
                }
            }

            // If the action is MK_DIR, then the specified remote directory is the
            // directory to create.

            if( m_action == MK_DIR )
            {

                makeRemoteDir( ftp, m_remotedir );

            }
            else
            {
                if( m_remotedir != null )
                {
                    getLogger().debug( "changing the remote directory" );
                    ftp.changeWorkingDirectory( m_remotedir );
                    if( !FTPReply.isPositiveCompletion( ftp.getReplyCode() ) )
                    {
                        throw new TaskException(
                            "could not change remote directory: " +
                            ftp.getReplyString() );
                    }
                }
                getLogger().info( ACTION_STRS[ m_action ] + " files" );
                transferFiles( ftp );
            }

        }
        catch( IOException ex )
        {
            throw new TaskException( "error during FTP transfer: " + ex );
        }
        finally
        {
            if( ftp != null && ftp.isConnected() )
            {
                try
                {
                    getLogger().debug( "disconnecting" );
                    ftp.logout();
                    ftp.disconnect();
                }
                catch( IOException ex )
                {
                    // ignore it
                }
            }
        }
    }

    /**
     * Retrieve a single file to the remote host. <code>filename</code> may
     * contain a relative path specification. The file will then be retreived
     * using the entire relative path spec - no attempt is made to change
     * directories. It is anticipated that this may eventually cause problems
     * with some FTP servers, but it simplifies the coding.
     *
     * @param ftp Description of Parameter
     * @param dir Description of Parameter
     * @param filename Description of Parameter
     * @exception IOException Description of Exception
     * @exception TaskException Description of Exception
     */
    protected void getFile( FTPClient ftp, String dir, String filename )
        throws IOException, TaskException
    {
        OutputStream outstream = null;
        try
        {
            final String filename1 = dir;
            File result;
            result = getContext().resolveFile( filename1 );
            final File file = FileUtil.resolveFile( result, filename );

            if( m_newerOnly && isUpToDate( ftp, file, remoteResolveFile( filename ) ) )
            {
                return;
            }

            if( m_verbose )
            {
                getLogger().info( "transferring " + filename + " to " + file.getAbsolutePath() );
            }

            final File parent = file.getParentFile();
            if( !parent.exists() )
            {
                parent.mkdirs();
            }
            outstream = new BufferedOutputStream( new FileOutputStream( file ) );
            ftp.retrieveFile( remoteResolveFile( filename ), outstream );

            if( !FTPReply.isPositiveCompletion( ftp.getReplyCode() ) )
            {
                String s = "could not get file: " + ftp.getReplyString();
                if( m_skipFailedTransfers == true )
                {
                    getLogger().warn( s );
                    m_skipped++;
                }
                else
                {
                    throw new TaskException( s );
                }

            }
            else
            {
                getLogger().debug( "File " + file.getAbsolutePath() + " copied from " + m_server );
                m_transferred++;
            }
        }
        finally
        {
            if( outstream != null )
            {
                try
                {
                    outstream.close();
                }
                catch( IOException ex )
                {
                    // ignore it
                }
            }
        }
    }

    /**
     * Checks to see if the remote file is current as compared with the local
     * file. Returns true if the remote file is up to date.
     */
    protected boolean isUpToDate( FTPClient ftp, File localFile, String remoteFile )
        throws IOException, TaskException
    {
        getLogger().debug( "checking date for " + remoteFile );

        FTPFile[] files = ftp.listFiles( remoteFile );

        // For Microsoft's Ftp-Service an Array with length 0 is
        // returned if configured to return listings in "MS-DOS"-Format
        if( files == null || files.length == 0 )
        {
            // If we are sending files, then assume out of date.
            // If we are getting files, then throw an error

            if( m_action == SEND_FILES )
            {
                getLogger().debug( "Could not date test remote file: " + remoteFile + "assuming out of date." );
                return false;
            }
            else
            {
                throw new TaskException( "could not date test remote file: " +
                                         ftp.getReplyString() );
            }
        }

        long remoteTimestamp = files[ 0 ].getTimestamp().getTime().getTime();
        long localTimestamp = localFile.lastModified();
        if( m_action == SEND_FILES )
        {
            return remoteTimestamp > localTimestamp;
        }
        else
        {
            return localTimestamp > remoteTimestamp;
        }
    }

    /**
     * Checks to see that all required parameters are set.
     *
     * @exception TaskException Description of Exception
     */
    private void validate()
        throws TaskException
    {
        if( m_server == null )
        {
            throw new TaskException( "server attribute must be set!" );
        }
        if( m_userid == null )
        {
            throw new TaskException( "userid attribute must be set!" );
        }
        if( m_password == null )
        {
            throw new TaskException( "password attribute must be set!" );
        }

        if( ( m_action == LIST_FILES ) && ( m_listing == null ) )
        {
            throw new TaskException( "listing attribute must be set for list action!" );
        }

        if( m_action == MK_DIR && m_remotedir == null )
        {
            throw new TaskException( "remotedir attribute must be set for mkdir action!" );
        }
    }

    /**
     * Creates all parent directories specified in a complete relative pathname.
     * Attempts to create existing directories will not cause errors.
     */
    protected void createParents( FTPClient ftp, String filename )
        throws IOException, TaskException
    {
        ArrayList parents = new ArrayList();
        File dir = new File( filename );
        String dirname;

        while( ( dirname = dir.getParent() ) != null )
        {
            dir = new File( dirname );
            parents.add( dir );
        }

        for( int i = parents.size() - 1; i >= 0; i-- )
        {
            dir = (File)parents.get( i );
            if( !m_dirCache.contains( dir ) )
            {
                getLogger().debug( "creating remote directory " + remoteResolveFile( dir.getPath() ) );
                ftp.makeDirectory( remoteResolveFile( dir.getPath() ) );
                // Both codes 550 and 553 can be produced by FTP Servers
                //  to indicate that an attempt to create a directory has
                //  failed because the directory already exists.
                int result = ftp.getReplyCode();
                if( !FTPReply.isPositiveCompletion( result ) &&
                    ( result != 550 ) && ( result != 553 ) &&
                    !m_ignoreNoncriticalErrors )
                {
                    throw new TaskException(
                        "could not create directory: " +
                        ftp.getReplyString() );
                }
                m_dirCache.add( dir );
            }
        }
    }

    /**
     * Delete a file from the remote host.
     */
    protected void delFile( FTPClient ftp, String filename )
        throws IOException, TaskException
    {
        if( m_verbose )
        {
            getLogger().info( "deleting " + filename );
        }

        if( !ftp.deleteFile( remoteResolveFile( filename ) ) )
        {
            String s = "could not delete file: " + ftp.getReplyString();
            if( m_skipFailedTransfers == true )
            {
                getLogger().warn( s );
                m_skipped++;
            }
            else
            {
                throw new TaskException( s );
            }
        }
        else
        {
            getLogger().debug( "File " + filename + " deleted from " + m_server );
            m_transferred++;
        }
    }

    /**
     * List information about a single file from the remote host. <code>filename</code>
     * may contain a relative path specification. The file listing will then be
     * retrieved using the entire relative path spec - no attempt is made to
     * change directories. It is anticipated that this may eventually cause
     * problems with some FTP servers, but it simplifies the coding.
     */
    protected void listFile( FTPClient ftp, BufferedWriter bw, String filename )
        throws IOException, TaskException
    {
        if( m_verbose )
        {
            getLogger().info( "listing " + filename );
        }

        FTPFile ftpfile = ftp.listFiles( remoteResolveFile( filename ) )[ 0 ];
        bw.write( ftpfile.toString() );
        bw.newLine();

        m_transferred++;
    }

    /**
     * Create the specified directory on the remote host.
     *
     * @param ftp The FTP client connection
     * @param dir The directory to create (format must be correct for host type)
     * @exception IOException Description of Exception
     * @exception TaskException Description of Exception
     */
    protected void makeRemoteDir( FTPClient ftp, String dir )
        throws IOException, TaskException
    {
        if( m_verbose )
        {
            getLogger().info( "creating directory: " + dir );
        }

        if( !ftp.makeDirectory( dir ) )
        {
            // codes 521, 550 and 553 can be produced by FTP Servers
            //  to indicate that an attempt to create a directory has
            //  failed because the directory already exists.

            int rc = ftp.getReplyCode();
            if( !( m_ignoreNoncriticalErrors && ( rc == 550 || rc == 553 || rc == 521 ) ) )
            {
                throw new TaskException( "could not create directory: " +
                                         ftp.getReplyString() );
            }

            if( m_verbose )
            {
                getLogger().info( "directory already exists" );
            }
        }
        else
        {
            if( m_verbose )
            {
                getLogger().info( "directory created OK" );
            }
        }
    }

    /**
     * Correct a file path to correspond to the remote host requirements. This
     * implementation currently assumes that the remote end can handle
     * Unix-style paths with forward-slash separators. This can be overridden
     * with the <code>separator</code> task parameter. No attempt is made to
     * determine what syntax is appropriate for the remote host.
     *
     * @param file Description of Parameter
     * @return Description of the Returned Value
     */
    protected String remoteResolveFile( final String file )
    {
        return file.replace( System.getProperty( "file.separator" ).charAt( 0 ),
                             m_remoteFileSep.charAt( 0 ) );
    }

    /**
     * Sends a single file to the remote host. <code>filename</code> may contain
     * a relative path specification. When this is the case, <code>sendFile</code>
     * will attempt to create any necessary parent directories before sending
     * the file. The file will then be sent using the entire relative path spec
     * - no attempt is made to change directories. It is anticipated that this
     * may eventually cause problems with some FTP servers, but it simplifies
     * the coding.
     *
     * @param ftp Description of Parameter
     * @param dir Description of Parameter
     * @param filename Description of Parameter
     * @exception IOException Description of Exception
     * @exception TaskException Description of Exception
     */
    protected void sendFile( FTPClient ftp, final String dir, final String filename )
        throws IOException, TaskException
    {
        InputStream instream = null;
        try
        {
            File file = getContext().resolveFile( new File( dir, filename ).getPath() );

            if( m_newerOnly && isUpToDate( ftp, file, remoteResolveFile( filename ) ) )
            {
                return;
            }

            if( m_verbose )
            {
                getLogger().info( "transferring " + file.getAbsolutePath() );
            }

            instream = new BufferedInputStream( new FileInputStream( file ) );

            createParents( ftp, filename );

            ftp.storeFile( remoteResolveFile( filename ), instream );
            boolean success = FTPReply.isPositiveCompletion( ftp.getReplyCode() );
            if( !success )
            {
                String s = "could not put file: " + ftp.getReplyString();
                if( m_skipFailedTransfers == true )
                {
                    getLogger().warn( s );
                    m_skipped++;
                }
                else
                {
                    throw new TaskException( s );
                }

            }
            else
            {

                getLogger().debug( "File " + file.getAbsolutePath() + " copied to " + m_server );
                m_transferred++;
            }
        }
        finally
        {
            if( instream != null )
            {
                try
                {
                    instream.close();
                }
                catch( IOException ex )
                {
                    // ignore it
                }
            }
        }
    }

    /**
     * For each file in the fileset, do the appropriate action: send, get,
     * delete, or list.
     *
     * @param ftp Description of Parameter
     * @param fs Description of Parameter
     * @return Description of the Returned Value
     * @exception IOException Description of Exception
     * @exception TaskException Description of Exception
     */
    protected int transferFiles( FTPClient ftp, FileSet fs )
        throws IOException, TaskException
    {
        FileScanner ds;

        if( m_action == SEND_FILES )
        {
            ds = ScannerUtil.getDirectoryScanner( fs );
        }
        else
        {
            ds = new FTPDirectoryScanner( ftp );
            final FileScanner ds1 = ds;
            final TaskContext context = getContext();
            ScannerUtil.setupDirectoryScanner( fs, ds1, context );
            ds.scan();
        }

        String[] dsfiles = ds.getIncludedFiles();
        String dir = null;
        if( ( ds.getBasedir() == null ) && ( ( m_action == SEND_FILES ) || ( m_action == GET_FILES ) ) )
        {
            throw new TaskException( "the dir attribute must be set for send and get actions" );
        }
        else
        {
            if( ( m_action == SEND_FILES ) || ( m_action == GET_FILES ) )
            {
                dir = ds.getBasedir().getAbsolutePath();
            }
        }

        // If we are doing a listing, we need the output stream created now.
        BufferedWriter bw = null;
        if( m_action == LIST_FILES )
        {
            File pd = new File( m_listing.getParent() );
            if( !pd.exists() )
            {
                pd.mkdirs();
            }
            bw = new BufferedWriter( new FileWriter( m_listing ) );
        }

        for( int i = 0; i < dsfiles.length; i++ )
        {
            switch( m_action )
            {
                case SEND_FILES:
                    {
                        sendFile( ftp, dir, dsfiles[ i ] );
                        break;
                    }

                case GET_FILES:
                    {
                        getFile( ftp, dir, dsfiles[ i ] );
                        break;
                    }

                case DEL_FILES:
                    {
                        delFile( ftp, dsfiles[ i ] );
                        break;
                    }

                case LIST_FILES:
                    {
                        listFile( ftp, bw, dsfiles[ i ] );
                        break;
                    }

                default:
                    {
                        throw new TaskException( "unknown ftp action " + m_action );
                    }
            }
        }

        if( m_action == LIST_FILES )
        {
            bw.close();
        }

        return dsfiles.length;
    }

    /**
     * Sends all files specified by the configured filesets to the remote
     * server.
     *
     * @param ftp Description of Parameter
     * @exception IOException Description of Exception
     * @exception TaskException Description of Exception
     */
    protected void transferFiles( FTPClient ftp )
        throws IOException, TaskException
    {
        m_transferred = 0;
        m_skipped = 0;

        if( m_filesets.size() == 0 )
        {
            throw new TaskException( "at least one fileset must be specified." );
        }
        else
        {
            // get files from filesets
            for( int i = 0; i < m_filesets.size(); i++ )
            {
                FileSet fs = (FileSet)m_filesets.get( i );
                if( fs != null )
                {
                    transferFiles( ftp, fs );
                }
            }
        }

        getLogger().info( m_transferred + " files " + COMPLETED_ACTION_STRS[ m_action ] );
        if( m_skipped != 0 )
        {
            getLogger().info( m_skipped + " files were not successfully " + COMPLETED_ACTION_STRS[ m_action ] );
        }
    }
}
