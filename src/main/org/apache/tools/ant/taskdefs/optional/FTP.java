/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;
import java.io.*;
import java.net.*;
import java.util.*;
import com.oroinc.net.ftp.*;

/**
 * Sends files to an FTP server.  Also intended to retrieve remote files,
 * but this has not yet been implemented
 *
 * @author Roger Vaughn <a href="mailto:rvaughn@seaconinc.com">rvaughn@seaconinc.com</a>
 */
public class FTP
    extends Task
{
    protected final static int SEND_FILES   = 0;
    protected final static int GET_FILES    = 1;
    
    private String remotedir;
    private String server;
    private String userid;
    private String password;
    private boolean binary = true;
    private boolean verbose = false;
    private boolean newerOnly = false;
    private int action = SEND_FILES;
    private Vector filesets = new Vector();
    private Vector dirCache = new Vector();
    private int transferred = 0;
    private String remoteFileSep = "/";
    private int port = 21;

    /**
     * Sets the remote directory where files will be placed.  This may
     * be a relative or absolute path, and must be in the path syntax
     * expected by the remote server.  No correction of path syntax will
     * be performed.
     */
    public void setRemotedir(String dir)
    {
        this.remotedir = dir;
    }

    /**
     * Sets the FTP server to send files to.
     */
    public void setServer(String server)
    {
        this.server = server;
    }

    /**
     * Sets the FTP port used by the remote server.
     */
    public void setPort(int port)
    {
        this.port = port;
    }

    /**
     * Sets the login user id to use on the specified server.
     */
    public void setUserid(String userid)
    {
        this.userid = userid;
    }

    /**
     * Sets the login password for the given user id.
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * Specifies whether to use binary-mode or text-mode transfers.  Set
     * to true to send binary mode.  Binary mode is enabled by default.
     */
    public void setBinary(boolean binary)
    {
        this.binary = binary;
    }

    /**
     * Set to true to receive notification about each file as it is
     * transferred.
     */
    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }

    /**
     * Set to true to transmit only files that are new or changed from their
     * remote counterparts.  The default is to transmit all files.
     */
    public void setNewer(boolean newer)
    {
        this.newerOnly = newer;
    }

    /**
     * A synonym for setNewer.  Set to true to transmit only new or changed
     * files.
     */
    public void setDepends(boolean depends)
    {
        this.newerOnly = depends;
    }

    /**
     * Sets the remote file separator character.  This normally defaults to
     * the Unix standard forward slash, but can be manually overridden using
     * this call if the remote server requires some other separator.  Only
     * the first character of the string is used.
     */
    public void setSeparator(String separator)
    {
        remoteFileSep = separator;
    }

    /**
     * Adds a set of files (nested fileset attribute).
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Adds a reference to a set of files (nested filesetref element).
     */
    public void addFilesetref(Reference ref) {
        filesets.addElement(ref);
    }

    /**
     * Sets the FTP action to be taken.  Currently accepts "put" and "get".
     * "get" tasks are not yet supported and will effectively perform a noop.
     */
    public void setAction(String action) throws BuildException
    {
        if (action.toLowerCase().equals("send") ||
            action.toLowerCase().equals("put"))
        {
            this.action = SEND_FILES;
        }
        else if (action.toLowerCase().equals("recv") ||
                 action.toLowerCase().equals("get"))
        {
            this.action = GET_FILES;
        }
        else
        {
            throw new BuildException("action " + action + " is not supported");
        }
    }

    /**
     * Checks to see that all required parameters are set.
     */
    protected void checkConfiguration() throws BuildException
    {
        if (server == null)
        {
            throw new BuildException("server attribute must be set!");
        }
        if (userid == null)
        {
            throw new BuildException("userid attribute must be set!");
        }
        if (password == null)
        {
            throw new BuildException("password attribute must be set!");
        }
    }

    /**
     * Append all files found by a directory scanner to a vector.
     */
    protected int sendFiles(FTPClient ftp, DirectoryScanner ds)
        throws IOException, BuildException
    {
        String[] dsfiles = ds.getIncludedFiles();
        String dir = ds.getBasedir().getAbsolutePath();

        for (int i = 0; i < dsfiles.length; i++)
        {
            sendFile(ftp, dir, dsfiles[i]);
        }

        return dsfiles.length;
    }

    /**
     * Sends all files specified by the configured filesets to the remote
     * server.
     */
    protected void sendFiles(FTPClient ftp)
        throws IOException, BuildException
    {
        transferred = 0;
        
        if (filesets.size() == 0)
        {
            throw new BuildException("at least one fileset must be specified.");
        }
        else
        {
            // get files from filesets
            for (int i = 0; i < filesets.size(); i++)
            {
                Object o = filesets.elementAt(i);
                FileSet fs;
                if (o instanceof FileSet)
                {
                    fs = (FileSet)o;
                }
                else if (o instanceof Reference)
                {
                    Reference r = (Reference)o;
                    o = r.getReferencedObject(project);

                    if (o instanceof FileSet)
                    {
                        fs = (FileSet)o;
                    }
                    else
                    {
                        throw new BuildException(
                            r.getRefId() + " does not denote a fileset",
                            location);
                    }
                }
                else
                {
                    throw new BuildException(
                        "nested element is not a FileSet or Reference",
                        location);
                }

                if (fs != null)
                {
                    sendFiles(ftp, fs.getDirectoryScanner(project));
                }
            }
        }

        log(transferred + " files transferred");
    }

    /**
     * Correct a file path to correspond to the remote host requirements.
     * This implementation currently assumes that the remote end can
     * handle Unix-style paths with forward-slash separators.  This can
     * be overridden with the <code>separator</code> task parameter.  No
     * attempt is made to determine what syntax is appropriate for the
     * remote host.
     */
    protected String resolveFile(String file)
    {
        return file.replace(System.getProperty("file.separator").charAt(0),
                            remoteFileSep.charAt(0));
    }

    /**
     * Creates all parent directories specified in a complete relative
     * pathname.  Attempts to create existing directories will not cause
     * errors.
     */
    protected void createParents(FTPClient ftp, String filename)
        throws IOException, BuildException
    {
        Vector parents = new Vector();
        File dir = new File(filename);
        String dirname;

        while ((dirname = dir.getParent()) != null)
        {
            dir = new File(dirname);
            parents.addElement(dir);
        }

        for (int i = parents.size() - 1; i >= 0; i--)
        {
            dir = (File)parents.elementAt(i);
            if (!dirCache.contains(dir))
            {
                log("creating remote directory " + resolveFile(dir.getPath()),
                    Project.MSG_VERBOSE);
                ftp.makeDirectory(resolveFile(dir.getPath()));
                if (!FTPReply.isPositiveCompletion(ftp.getReplyCode()) &&
                    (ftp.getReplyCode() != 550))
                {
                    throw new BuildException(
                        "could not create directory: " +
                        ftp.getReplyString());
                }
                dirCache.addElement(dir);
            }
        }
    }

    /**
     * Checks to see if the remote file is current as compared with the
     * local file.  Returns true if the remote file is up to date.
     */
    protected boolean isUpToDate(FTPClient ftp, File localFile, String remoteFile)
        throws IOException, BuildException
    {
        log("checking date for " + remoteFile, Project.MSG_VERBOSE);
        
        FTPFile[] files = ftp.listFiles(remoteFile);
        if (!FTPReply.isPositiveCompletion(ftp.getReplyCode()))
        {
            throw new BuildException(
                "could not date test remote file: " +
                ftp.getReplyString());
        }

        if (files == null)
        {
            return false;
        }

        return files[0].getTimestamp().getTime().getTime() >
            localFile.lastModified();
    }

    /**
     * Sends a single file to the remote host.
     * <code>filename</code> may contain a relative path specification.
     * When this is the case, <code>sendFile</code> will attempt to create
     * any necessary parent directories before sending the file.  The file
     * will then be sent using the entire relative path spec - no attempt
     * is made to change directories.  It is anticipated that this may
     * eventually cause problems with some FTP servers, but it simplifies
     * the coding.
     */
    protected void sendFile(FTPClient ftp, String dir, String filename)
        throws IOException, BuildException
    {
        InputStream instream = null;
        try
        {
            File file = project.resolveFile(new File(dir, filename).getPath());

            if (newerOnly && isUpToDate(ftp, file, resolveFile(filename)))
                return;

            if (verbose)
            {
                log("transferring " + file.getAbsolutePath());
            }
            
            instream = new BufferedInputStream(new FileInputStream(file));
            
            createParents(ftp, filename);
            
            ftp.storeFile(resolveFile(filename), instream);
            
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode()))
            {
                throw new BuildException(
                    "could not transfer file: " +
                    ftp.getReplyString());
            }
            
            log("File " + file.getAbsolutePath() + " copied to " + server,
                Project.MSG_VERBOSE);

            transferred++;
        }
        finally
        {            
            if (instream != null)
            {
                try
                {
                    instream.close();
                }
                catch(IOException ex)
                {
                    // ignore it
                }
            }
        }
    }

    /**
     * Runs the task.
     */
    public void execute()
        throws BuildException
    {
        checkConfiguration();
        
        FTPClient ftp = null;

        try
        {
            log("Opening FTP connection to " + server, Project.MSG_VERBOSE);

            ftp = new FTPClient();
            
            ftp.connect(server, port);
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode()))
            {
                throw new BuildException("FTP connection failed: " + ftp.getReplyString());
            }

            log("connected", Project.MSG_VERBOSE);
            log("logging in to FTP server", Project.MSG_VERBOSE);

            if (!ftp.login(userid, password))
            {
                throw new BuildException("Could not login to FTP server");
            }

            log("login succeeded", Project.MSG_VERBOSE);
            
            if (binary)
            {
                ftp.setFileType(com.oroinc.net.ftp.FTP.IMAGE_FILE_TYPE);
                if (!FTPReply.isPositiveCompletion(ftp.getReplyCode()))
                {
                    throw new BuildException(
                        "could not set transfer type: " +
                        ftp.getReplyString());
                }
            }

            if (remotedir != null)
            {
                log("changing the remote directory", Project.MSG_VERBOSE);
                ftp.changeWorkingDirectory(remotedir);
                if (!FTPReply.isPositiveCompletion(ftp.getReplyCode()))
                {
                    throw new BuildException(
                        "could not change remote directory: " +
                        ftp.getReplyString());
                }
            }

            log("transferring files");

            if (action == SEND_FILES)
            {
                sendFiles(ftp);
            }
            else
            {
                throw new BuildException("getting files is not yet supported");
            }
        }
        catch(IOException ex)
        {
            throw new BuildException("error during FTP transfer: " + ex);
        }
        finally
        {
            /*
            if (ftp != null && ftp.isConnected())
            {
                try
                {
                    // this hangs - I don't know why.
                    ftp.disconnect();
                }
                catch(IOException ex)
                {
                    // ignore it
                }
            }
            */
        }
    }
}
