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

import junit.framework.TestCase;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.FilesMatch;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.selectors.FilenameSelector;

/**
 * This is a unit test for the Scp task in Ant.  It must be
 * configured with command line options in order for it to work.
 * Here are the options:
 *
 * scp.tmp              This is a local path to a temporary
 *                      directory for this task to use.
 * scp.host             This is the remote location of the form:
 *                      "user:password@host:/path/to/directory"
 * scp.port             The port of the listening SSH service.
 *                      Defaults to 22.  (optional)
 * scp.known.hosts      The file containing the public keys of known
 *                      hosts.  Must be a SSH2 version file, but
 *                      supports RSA and DSA keys. If it is not present
 *                      this task setTrust() to true.  (optional)
 */
public class ScpTest extends TestCase {

    private File tempDir = new File( System.getProperty("scp.tmp") );
    private String sshHostUri = System.getProperty("scp.host");
    private int port = Integer.parseInt( System.getProperty( "scp.port", "22" ) );
    private String knownHosts = System.getProperty("scp.known.hosts");

    private List cleanUpList = new ArrayList();

    public ScpTest(String testname) {
        super(testname);
    }

    protected void setUp() {
        cleanUpList.clear();
    }

    protected void tearDown() {
        for( Iterator i = cleanUpList.iterator(); i.hasNext(); ) {
            File file = (File) i.next();
            file.delete();
        }
    }

    public void testSingleFileUploadAndDownload() throws IOException {
        File uploadFile = createTemporaryFile();

        Scp scpTask = createTask();
        scpTask.setFile( uploadFile.getPath() );
        scpTask.setTodir( sshHostUri );
        scpTask.execute();

        File testFile = new File( tempDir.getPath() + File.separator +
                "download-testSingleFileUploadAndDownload.test" );
        addCleanup( testFile );
        assertTrue( "Assert that the testFile does not exist.",
                !testFile.exists() );

        scpTask.setFile( sshHostUri + "/" + uploadFile.getName() );
        scpTask.setTodir( testFile.getPath() );
        scpTask.execute();

        assertTrue( "Assert that the testFile exists.", testFile.exists() );
        compareFiles( uploadFile, testFile );
    }

    public void testMultiUploadAndDownload() throws IOException {
        List uploadList = new ArrayList();
        for( int i = 0; i < 5; i++ ) {
            uploadList.add( createTemporaryFile() );
        }

        Scp scp = createTask();
        FilenameSelector selector = new FilenameSelector();
        selector.setName( "scp*" );
        FileSet fileset = new FileSet();
        fileset.setDir( tempDir );
        fileset.addFilename( selector );
        scp.addFileset( fileset );
        scp.setTodir( sshHostUri );
        scp.execute();

        File multi = new File( tempDir, "multi" );
        multi.mkdir();
        addCleanup( multi );

        Scp scp2 = createTask();
        scp2.setFile( sshHostUri + "/scp*" );
        scp2.setTodir( multi.getPath() );
        scp2.execute();

        FilesMatch match = new FilesMatch();
        for( Iterator i = uploadList.iterator(); i.hasNext(); ) {
            File f = (File)i.next();
            match.setFile1( f );
            File f2 = new File( multi, f.getName() );
            match.setFile2( f2 );
            assertTrue("Assert file '" + f.getPath() + "' and file '" +
                    f2.getPath() + "'", match.eval() );
        }
    }

    public void addCleanup( File file ) {
        cleanUpList.add( file );
    }

    private void compareFiles(File src, File dest) {
        FilesMatch match = new FilesMatch();
        match.setFile1( src );
        match.setFile2( dest );

        assertTrue( "Assert files are equal.", match.eval() );
    }

    private File createTemporaryFile() throws IOException {
        File uploadFile;
        uploadFile = File.createTempFile( "scp", "test", tempDir );
        FileWriter writer = new FileWriter( uploadFile );
        writer.write("Can you hear me now?\n");
        writer.close();
        addCleanup( uploadFile );
        return uploadFile;
    }

    private Scp createTask() {
        Scp scp = new Scp();
        Project p = new Project();
        p.init();
        scp.setProject( p );
        if( knownHosts != null ) {
            scp.setKnownhosts( knownHosts );
        } else {
            scp.setTrust( true );
        }
        scp.setPort( port );
        return scp;
    }
}
