/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

package org.apache.tools.ant.taskdefs.optional.ssh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.JSchException;

import java.io.*;
import java.util.*;
import java.text.NumberFormat;

public class ScpToMessage extends AbstractSshMessage {

    private File localFile;
    private String remotePath;
    private List directoryList;

    public ScpToMessage(Session session,
                        File aLocalFile,
                        String aRemotePath ) {
        super(session);

        this.localFile = aLocalFile;
        this.remotePath = aRemotePath;
    }

    public ScpToMessage( Session session,
                         List aDirectoryList,
                         String aRemotePath ) {
        super( session );

        this.directoryList = aDirectoryList;
        this.remotePath = aRemotePath;
    }

    public void execute() throws IOException, JSchException {
        if( directoryList != null ) {
            doMultipleTransfer();
        }
        if( localFile != null ) {
            doSingleTransfer();
        }
        log("done.\n");
    }

    private void doSingleTransfer() throws IOException, JSchException {
        String cmd = "scp -t " + remotePath;
        Channel channel = openExecChannel( cmd );
        try {

            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            waitForAck(in);
            sendFileToRemote(localFile, in, out);
            waitForAck(in);
        } finally {
            if( channel != null )
                channel.disconnect();
        }
    }

    private void doMultipleTransfer() throws IOException, JSchException {
        Channel channel = openExecChannel( "scp -d -t " + remotePath );
        try {
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            waitForAck(in);
            for( Iterator i = directoryList.iterator(); i.hasNext(); ) {
                Directory current = (Directory)i.next();
                sendDirectory( current, in, out );
            }
            waitForAck(in);
        } finally {
            if( channel != null )
                channel.disconnect();
        }
    }

    private void sendDirectory(Directory current,
                               InputStream in,
                               OutputStream out) throws IOException {
        for( Iterator fileIt = current.filesIterator(); fileIt.hasNext(); ) {
            sendFileToRemote( (File)fileIt.next(), in, out );
        }
        for( Iterator dirIt = current.directoryIterator(); dirIt.hasNext(); ) {
            Directory dir = (Directory) dirIt.next();
            sendDirectoryToRemote( dir, in ,out );
        }
    }

    private void sendDirectoryToRemote( Directory directory,
                                        InputStream in,
                                        OutputStream out ) throws IOException {
        String command = "D0755 0 ";
        command += directory.getDirectory().getName();
        command += "\n";

        out.write( command.getBytes() );
        out.flush();

        waitForAck(in);
        sendDirectory( directory, in, out );
        out.write( "E\n".getBytes() );
    }

    private void sendFileToRemote( File localFile,
                                   InputStream in,
                                   OutputStream out ) throws IOException {
        // send "C0644 filesize filename", where filename should not include '/'
        int filesize = (int) localFile.length();
        String command = "C0644 " + filesize + " ";
        command += localFile.getName();
        command += "\n";

        out.write( command.getBytes() );
        out.flush();

        waitForAck(in);

        // send a content of lfile
        FileInputStream fis = new FileInputStream(localFile);
        byte[] buf = new byte[1024];
        long startTime = System.currentTimeMillis();
        int totalLength = 0;
        try {
            NumberFormat formatter = NumberFormat.getIntegerInstance();
            log( "Sending: " + localFile.getName() + " : " +
                    formatter.format( localFile.length() ) );
            while (true) {
                int len = fis.read(buf, 0, buf.length);
                if (len <= 0) break;
                out.write(buf, 0, len);
                totalLength += len;
            }
            out.flush();
            sendAck(out);
        } finally {
            long endTime = System.currentTimeMillis();
            logStats( startTime, endTime, totalLength );
            fis.close();
        }
    }

    public File getLocalFile() {
        return localFile;
    }

    public String getRemotePath() {
        return remotePath;
    }
}
