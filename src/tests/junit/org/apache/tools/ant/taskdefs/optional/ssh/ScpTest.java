/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs.optional.ssh;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.FilesMatch;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.Sort;
import org.apache.tools.ant.types.resources.comparators.Name;
import org.apache.tools.ant.types.resources.comparators.Reverse;
import org.apache.tools.ant.types.selectors.FilenameSelector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

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
public class ScpTest {

    private Scp scpTask;
    private File tempDir;
    private String sshHostUri = System.getProperty("scp.host");
    private int port = Integer.parseInt(System.getProperty("scp.port", "22"));
    private String knownHosts = System.getProperty("scp.known.hosts");

    private List<File> cleanUpList = new ArrayList<>();

    @Before
    public void setUp() {
        scpTask = createTask();
        if (System.getProperty("scp.tmp") != null) {
            tempDir = new File(System.getProperty("scp.tmp"));
        }
        cleanUpList.clear();
    }

    @After
    public void tearDown() {
        cleanUpList.forEach(File::delete);
    }

    @Test
    public void testSingleFileUploadAndDownload() throws IOException {
        assumeNotNull("system property scp.host must be set", sshHostUri);
        assertNotNull("system property scp.tmp must be set", tempDir);
        File uploadFile = createTemporaryFile();

        // upload
        scpTask.setFile(uploadFile.getPath());
        scpTask.setTodir(sshHostUri);
        scpTask.execute();

        File testFile = new File(tempDir.getPath() + File.separator
                + "download-testSingleFileUploadAndDownload.test");
        addCleanup(testFile);
        assertFalse("Assert that the testFile does not exist.", testFile.exists());

        // download
        scpTask = createTask();
        scpTask.setFile(sshHostUri + "/" + uploadFile.getName());
        scpTask.setTodir(testFile.getPath());
        scpTask.execute();

        assertTrue("Assert that the testFile exists.", testFile.exists());
        compareFiles(uploadFile, testFile);
    }

    @Test
    public void testMultiUploadAndDownload() throws IOException {
        assumeNotNull("system property scp.host must be set", sshHostUri);
        assertNotNull("system property scp.tmp must be set", tempDir);
        List<File> uploadList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            uploadList.add(createTemporaryFile());
        }

        FilenameSelector selector = new FilenameSelector();
        selector.setName("scp*");
        FileSet fileset = new FileSet();
        fileset.setDir(tempDir);
        fileset.addFilename(selector);
        scpTask.addFileset(fileset);
        scpTask.setTodir(sshHostUri);
        scpTask.execute();

        File multi = new File(tempDir, "multi");
        multi.mkdir();
        addCleanup(multi);

        scpTask = createTask();
        scpTask.setFile(sshHostUri + "/scp*");
        scpTask.setTodir(multi.getPath());
        scpTask.execute();

        FilesMatch match = new FilesMatch();
        for (File f : uploadList) {
            match.setFile1(f);
            File f2 = new File(multi, f.getName());
            match.setFile2(f2);
            assertTrue("Assert file '" + f.getPath() + "' and file '"
                    + f2.getPath() + "'", match.eval());
        }
    }

    @Test
    public void testMultiResourceCollectionUpload() throws IOException {
        assumeNotNull("system property scp.host must be set", sshHostUri);
        assertNotNull("system property scp.tmp must be set", tempDir);
        List<File> uploadList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            uploadList.add(createTemporaryFile());
        }

        // reverse order resource collection
        Sort sort = new Sort();
        sort.setProject(scpTask.getProject());
        Reverse reverse = new Reverse();
        reverse.add(new Name());
        sort.add(reverse);

        FilenameSelector selector = new FilenameSelector();
        selector.setName("scp*");
        FileSet fileset = new FileSet();
        fileset.setProject(scpTask.getProject());
        fileset.setDir(tempDir);
        fileset.addFilename(selector);
        sort.add(fileset);
        scpTask.add(sort);

        scpTask.setTodir(sshHostUri);
        scpTask.execute();
    }

    /**
     * Expected failure due to invalid remoteToDir
     */
    @Test(expected = BuildException.class)
    public void testInvalidRemoteToDir() {
        scpTask.setRemoteTodir("host:/a/path/without/an/at");
    }

    @Test
    public void testRemoteToDir() {
        scpTask.setRemoteTodir("user:password@host:/a/path/with/an/at");
    }

    private void addCleanup(File file) {
        cleanUpList.add(file);
    }

    private void compareFiles(File src, File dest) {
        FilesMatch match = new FilesMatch();
        match.setFile1(src);
        match.setFile2(dest);

        assertTrue("Assert files are equal.", match.eval());
    }

    private File createTemporaryFile() throws IOException {
        File uploadFile;
        uploadFile = File.createTempFile("scp", "test", tempDir);
        FileWriter writer = new FileWriter(uploadFile);
        writer.write("Can you hear me now?\n");
        writer.close();
        addCleanup(uploadFile);
        return uploadFile;
    }

    private Scp createTask() {
        Scp scp = new Scp();
        Project p = new Project();
        p.init();
        scp.setProject(p);
        if (knownHosts != null) {
            scp.setKnownhosts(knownHosts);
        } else {
            scp.setTrust(true);
        }
        scp.setPort(port);
        return scp;
    }
}
