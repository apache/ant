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

import com.jcraft.jsch.*;

import java.io.*;
import java.util.StringTokenizer;

public class ScpFromMessage extends AbstractSshMessage {

    private String remoteFile;
    private File localFile;
    private boolean isRecursive = false;

    public ScpFromMessage( Session session,
                           String aRemoteFile,
                           File aLocalFile,
                           boolean recursive ) {
        super(session);
        this.remoteFile = aRemoteFile;
        this.localFile = aLocalFile;
        this.isRecursive = recursive;
    }

    public void execute() throws IOException, JSchException {
        String command = "scp -f ";
        if( isRecursive )
            command += "-r ";
        command += remoteFile;
        Channel channel = openExecChannel( command );
        try {
            // get I/O streams for remote scp
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            sendAck(out);
            startRemoteCpProtocol( in, out, localFile);
        } finally {
            if( channel != null )
                channel.disconnect();
        }
        log( "done\n" );
    }

    private void startRemoteCpProtocol(InputStream in,
                                       OutputStream out,
                                       File localFile) throws IOException {
        File startFile = localFile;
        while (true) {
            // C0644 filesize filename - header for a regular file
            // T time 0 time 0\n - present if perserve time.
            // D directory - this is the header for a directory.
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            while( true ) {
                int read = in.read();
                if( read < 0 ) return;
                if( (byte)read == (byte)0x0a ) break;
                stream.write( read );
            }
            String serverResponse = stream.toString("UTF-8");
            if( serverResponse.charAt(0) == 'C' ) {
                parseAndFetchFile( serverResponse, startFile, out, in );
            } else if( serverResponse.charAt( 0 ) == 'D' ) {
                startFile = parseAndCreateDirectory( serverResponse,
                        startFile );
                sendAck( out );
            } else if( serverResponse.charAt(0) == 'E' ) {
                startFile = startFile.getParentFile();
                sendAck( out );
            } else if( serverResponse.charAt( 0 ) == '\01'
                    || serverResponse.charAt( 0 ) == '\02' ) {
                // this indicates an error.
                throw new IOException( serverResponse.substring(1) );
            }
        }
    }

    private File parseAndCreateDirectory(String serverResponse,
                                         File localFile) {
        StringTokenizer token = new StringTokenizer( serverResponse );
        String command = token.nextToken();
        token.nextToken();  // appears that this is not used and it's zero.
        String directoryName = token.nextToken();
        if( localFile.isDirectory() ) {
            File dir = new File( localFile, directoryName );
            dir.mkdir();

            return dir;
        }
        return null;
    }

    private void parseAndFetchFile(String serverResponse,
                                   File localFile,
                                   OutputStream out,
                                   InputStream in) throws IOException {
        StringTokenizer token = new StringTokenizer( serverResponse );
        String command = token.nextToken();
        int filesize = Integer.parseInt( token.nextToken() );
        String filename = token.nextToken();
        log( "Receiving: " + filename + " : " + filesize);
        File transferFile = ( localFile.isDirectory() )
                ? new File( localFile, filename )
                : localFile;
        fetchFile( transferFile, filesize, out, in);
        waitForAck(in);
        sendAck(out);
    }

    private void fetchFile( File localFile,
                            int filesize,
                            OutputStream out,
                            InputStream in) throws IOException {
        byte[] buf = new byte[1024];
        sendAck(out);

        // read a content of lfile
        FileOutputStream fos = new FileOutputStream( localFile );
        int length;
        int totalLength = 0;
        long startTime = System.currentTimeMillis();
        try {
            while (true) {
                length = in.read( buf, 0,
                        (buf.length < filesize) ? buf.length : filesize );
                if( length < 0 )
                    throw new EOFException("Unexpected end of stream.");
                fos.write( buf, 0, length );
                filesize -= length;
                totalLength += length;
                if (filesize == 0) break;
            }
        } finally {
            long endTime = System.currentTimeMillis();
            logStats( startTime, endTime, totalLength );
            fos.flush();
            fos.close();
        }
    }

}
