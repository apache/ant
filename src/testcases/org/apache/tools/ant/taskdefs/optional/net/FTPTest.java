/*
 * Copyright  2003-2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs.optional.net;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.taskdefs.optional.net.FTP;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.taskdefs.condition.Os;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.net.ftp.FTPClient;

public class FTPTest extends BuildFileTest{
    // keep track of what operating systems are supported here.
    private boolean supportsSymlinks = Os.isFamily("unix")
        && !JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1);

    private FTPClient ftp;
    private boolean connectionSucceeded = true;
    private boolean loginSuceeded = true;
    private String tmpDir = null;
    private String remoteTmpDir = null;
    private String ftpFileSep = null;
    private myFTP myFTPTask = new myFTP();

    public FTPTest(String name) {
        super(name);
    }
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/optional/net/ftp.xml");
        getProject().executeTarget("setup");
        tmpDir = getProject().getProperty("tmp.dir");
        ftp = new FTPClient();
        ftpFileSep = getProject().getProperty("ftp.filesep");
        myFTPTask.setSeparator(ftpFileSep);
        myFTPTask.setProject(getProject());
        remoteTmpDir = myFTPTask.resolveFile(tmpDir);
        String remoteHost = getProject().getProperty("ftp.host");
        int port = Integer.parseInt(getProject().getProperty("ftp.port"));
        String remoteUser = getProject().getProperty("ftp.user");
        String password = getProject().getProperty("ftp.password");
        try {
            ftp.connect(remoteHost, port);
        } catch (Exception ex) {
            connectionSucceeded = false;
            loginSuceeded = false;
            System.out.println("could not connect to host " + remoteHost + " on port " + port);
        }
        if (connectionSucceeded) {
            try {
                ftp.login(remoteUser, password);
            } catch (IOException ioe) {
                loginSuceeded = false;
                System.out.println("could not log on to " + remoteHost + " as user " + remoteUser);
            }
        }
    }

    public void tearDown() {
        try {
            ftp.disconnect();
        } catch (IOException ioe) {
            // do nothing
        }
        getProject().executeTarget("cleanup");
    }
    private boolean changeRemoteDir(String remoteDir) {
        boolean result = true;
        try {
            ftp.cwd(remoteDir);
        }
        catch (Exception ex) {
            System.out.println("could not change directory to " + remoteTmpDir);
            result = false;
        }
        return result;
    }
    public void test1() {
        if (loginSuceeded) {
            if (changeRemoteDir(remoteTmpDir))  {
                FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
                ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
                ds.setIncludes(new String[] {"alpha"});
                ds.scan();
                compareFiles(ds, new String[] {} ,new String[] {"alpha"});
            }
        }
    }

    public void test2() {
        if (loginSuceeded) {
            if (changeRemoteDir(remoteTmpDir)) {
                FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
                ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
                ds.setIncludes(new String[] {"alpha/"});
                ds.scan();
                compareFiles(ds, new String[] {"alpha/beta/beta.xml",
                                               "alpha/beta/gamma/gamma.xml"},
                    new String[] {"alpha", "alpha/beta", "alpha/beta/gamma"});
            }
        }
    }

    public void test3() {
        if (loginSuceeded) {
            if (changeRemoteDir(remoteTmpDir)) {
                FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
                ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
                ds.scan();
                compareFiles(ds, new String[] {"alpha/beta/beta.xml",
                                               "alpha/beta/gamma/gamma.xml"},
                    new String[] {"alpha", "alpha/beta",
                                  "alpha/beta/gamma"});
            }
        }
    }

    public void testFullPathMatchesCaseSensitive() {
        if (loginSuceeded) {
            if (changeRemoteDir(remoteTmpDir)) {
                FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
                ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
                ds.setIncludes(new String[] {"alpha/beta/gamma/GAMMA.XML"});
                ds.scan();
                compareFiles(ds, new String[] {}, new String[] {});
            }
        }
    }

    public void testFullPathMatchesCaseInsensitive() {
        if (loginSuceeded) {
            if (changeRemoteDir(remoteTmpDir)) {
                FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
                ds.setCaseSensitive(false);
                ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
                ds.setIncludes(new String[] {"alpha/beta/gamma/GAMMA.XML"});
                ds.scan();
                compareFiles(ds, new String[] {"alpha/beta/gamma/gamma.xml"},
                    new String[] {});
            }
        }
    }

    public void test2ButCaseInsensitive() {
        if (loginSuceeded) {
            if (changeRemoteDir(remoteTmpDir)) {
                FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
                ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
                ds.setIncludes(new String[] {"ALPHA/"});
                ds.setCaseSensitive(false);
                ds.scan();
                compareFiles(ds, new String[] {"alpha/beta/beta.xml",
                                               "alpha/beta/gamma/gamma.xml"},
                    new String[] {"alpha", "alpha/beta", "alpha/beta/gamma"});
            }
        }
    }
    public void test2bisButCaseInsensitive() {
        if (loginSuceeded) {
            if (changeRemoteDir(remoteTmpDir)) {
                FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
                ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
                ds.setIncludes(new String[] {"alpha/BETA/gamma/"});
                ds.setCaseSensitive(false);
                ds.scan();
                compareFiles(ds, new String[] {"alpha/beta/gamma/gamma.xml"},
                    new String[] {"alpha/beta/gamma"});
            }
        }
    }
    public void testGetWithSelector() {
        expectLogContaining("ftp-get-with-selector",
            "selectors are not supported in remote filesets");
        FileSet fsDestination = (FileSet) getProject().getReference("fileset-destination-without-selector");
        DirectoryScanner dsDestination = fsDestination.getDirectoryScanner(getProject());
        dsDestination.scan();
        String [] sortedDestinationDirectories = dsDestination.getIncludedDirectories();
        String [] sortedDestinationFiles = dsDestination.getIncludedFiles();
        for (int counter = 0; counter < sortedDestinationDirectories.length; counter++) {
            sortedDestinationDirectories[counter] =
                sortedDestinationDirectories[counter].replace(File.separatorChar, '/');
        }
        for (int counter = 0; counter < sortedDestinationFiles.length; counter++) {
            sortedDestinationFiles[counter] =
                sortedDestinationFiles[counter].replace(File.separatorChar, '/');
        }
        FileSet fsSource =  (FileSet) getProject().getReference("fileset-source-without-selector");
        DirectoryScanner dsSource = fsSource.getDirectoryScanner(getProject());
        dsSource.scan();
        compareFiles(dsSource, sortedDestinationFiles, sortedDestinationDirectories);
    }
    public void testGetFollowSymlinksTrue() {
        if (!supportsSymlinks) {
            return;
        }
        if (!loginSuceeded) {
            return;
        }
        if (!changeRemoteDir(remoteTmpDir)) {
            return;
        }
        getProject().executeTarget("ftp-get-directory-symbolic-link");
        FileSet fsDestination = (FileSet) getProject().getReference("fileset-destination-without-selector");
        DirectoryScanner dsDestination = fsDestination.getDirectoryScanner(getProject());
        dsDestination.scan();
        compareFiles(dsDestination, new String[] {"alpha/beta/gamma/gamma.xml"},
            new String[] {"alpha", "alpha/beta", "alpha/beta/gamma"});
    }
    public void testGetFollowSymlinksFalse() {
        if (!supportsSymlinks) {
            return;
        }
        if (!loginSuceeded) {
            return;
        }
        if (!changeRemoteDir(remoteTmpDir)) {
            return;
        }
        getProject().executeTarget("ftp-get-directory-no-symbolic-link");
        FileSet fsDestination = (FileSet) getProject().getReference("fileset-destination-without-selector");
        DirectoryScanner dsDestination = fsDestination.getDirectoryScanner(getProject());
        dsDestination.scan();
        compareFiles(dsDestination, new String[] {},
            new String[] {});
    }
    public void testAllowSymlinks() {
        if (!supportsSymlinks) {
            return;
        }
        if (!loginSuceeded) {
            return;
        }
        if (!changeRemoteDir(remoteTmpDir)) {
            return;
        }
        getProject().executeTarget("symlink-setup");
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/beta/gamma/"});
        ds.setFollowSymlinks(true);
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/gamma/gamma.xml"},
                     new String[] {"alpha/beta/gamma"});
    }

    public void testProhibitSymlinks() {
        if (!supportsSymlinks) {
            return;
        }
        if (!loginSuceeded) {
            return;
        }
        if (!changeRemoteDir(remoteTmpDir)) {
            return;
        }
        getProject().executeTarget("symlink-setup");
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/beta/gamma/"});
        ds.setFollowSymlinks(false);
        ds.scan();
        compareFiles(ds, new String[] {}, new String[] {});
    }
    public void testFileSymlink() {
        if (!supportsSymlinks) {
            return;
        }
        if (!loginSuceeded) {
            return;
        }
        if (!changeRemoteDir(remoteTmpDir)) {
            return;
        }
        getProject().executeTarget("symlink-file-setup");
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/beta/gamma/"});
        ds.setFollowSymlinks(true);
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/gamma/gamma.xml"},
                     new String[] {"alpha/beta/gamma"});
    }
    // father and child pattern test
    public void testOrderOfIncludePatternsIrrelevant() {
        if (!loginSuceeded) {
            return;
        }
        if (!changeRemoteDir(remoteTmpDir)) {
            return;
        }
        String [] expectedFiles = {"alpha/beta/beta.xml",
                                   "alpha/beta/gamma/gamma.xml"};
        String [] expectedDirectories = {"alpha/beta", "alpha/beta/gamma" };
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/be?a/**", "alpha/beta/gamma/"});
        ds.scan();
        compareFiles(ds, expectedFiles, expectedDirectories);
        // redo the test, but the 2 include patterns are inverted
        ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/beta/gamma/", "alpha/be?a/**"});
        ds.scan();
        compareFiles(ds, expectedFiles, expectedDirectories);
    }

    public void testPatternsDifferInCaseScanningSensitive() {
        if (!loginSuceeded) {
            return;
        }
        if (!changeRemoteDir(remoteTmpDir)) {
            return;
        }
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/", "ALPHA/"});
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/beta.xml",
                                       "alpha/beta/gamma/gamma.xml"},
                     new String[] {"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    public void testPatternsDifferInCaseScanningInsensitive() {
        if (!loginSuceeded) {
            return;
        }
        if (!changeRemoteDir(remoteTmpDir)) {
            return;
        }
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/", "ALPHA/"});
        ds.setCaseSensitive(false);
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/beta.xml",
                                       "alpha/beta/gamma/gamma.xml"},
                     new String[] {"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    public void testFullpathDiffersInCaseScanningSensitive() {
        if (!loginSuceeded) {
            return;
        }
        if (!changeRemoteDir(remoteTmpDir)) {
            return;
        }
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {
            "alpha/beta/gamma/gamma.xml",
            "alpha/beta/gamma/GAMMA.XML"
        });
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/gamma/gamma.xml"},
                     new String[] {});
    }

    public void testFullpathDiffersInCaseScanningInsensitive() {
        if (!loginSuceeded) {
            return;
        }
        if (!changeRemoteDir(remoteTmpDir)) {
            return;
        }
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {
            "alpha/beta/gamma/gamma.xml",
            "alpha/beta/gamma/GAMMA.XML"
        });
        ds.setCaseSensitive(false);
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/gamma/gamma.xml"},
                     new String[] {});
    }

    public void testParentDiffersInCaseScanningSensitive() {
        if (!loginSuceeded) {
            return;
        }
        if (!changeRemoteDir(remoteTmpDir)) {
            return;
        }
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/", "ALPHA/beta/"});
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/beta.xml",
                                       "alpha/beta/gamma/gamma.xml"},
                     new String[] {"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    public void testParentDiffersInCaseScanningInsensitive() {
        if (!loginSuceeded) {
            return;
        }
        if (!changeRemoteDir(remoteTmpDir)) {
            return;
        }
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/", "ALPHA/beta/"});
        ds.setCaseSensitive(false);
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/beta.xml",
                                       "alpha/beta/gamma/gamma.xml"},
                     new String[] {"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    public void testExcludeOneFile() {
        if (!loginSuceeded) {
            return;
        }
        if (!changeRemoteDir(remoteTmpDir)) {
            return;
        }
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {
            "**/*.xml"
        });
        ds.setExcludes(new String[] {
            "alpha/beta/b*xml"
        });
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/gamma/gamma.xml"},
                     new String[] {});
    }
    public void testExcludeHasPrecedence() {
        if (!loginSuceeded) {
            return;
        }
        if (!changeRemoteDir(remoteTmpDir)) {
            return;
        }
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {
            "alpha/**"
        });
        ds.setExcludes(new String[] {
            "alpha/**"
        });
        ds.scan();
        compareFiles(ds, new String[] {},
                     new String[] {});

    }
    public void testAlternateIncludeExclude() {
        if (!loginSuceeded) {
            return;
        }
        if (!changeRemoteDir(remoteTmpDir)) {
            return;
        }
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {
            "alpha/**",
            "alpha/beta/gamma/**"
        });
        ds.setExcludes(new String[] {
            "alpha/beta/**"
        });
        ds.scan();
        compareFiles(ds, new String[] {},
                     new String[] {"alpha"});

    }
    public void testAlternateExcludeInclude() {
        if (!loginSuceeded) {
            return;
        }
        if (!changeRemoteDir(remoteTmpDir)) {
            return;
        }
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setExcludes(new String[] {
            "alpha/**",
            "alpha/beta/gamma/**"
        });
        ds.setIncludes(new String[] {
            "alpha/beta/**"
        });
        ds.scan();
        compareFiles(ds, new String[] {},
                     new String[] {});

    }
    /**
     * Test inspired by Bug#1415.
     */
    public void testChildrenOfExcludedDirectory() {
        if (!loginSuceeded) {
            return;
        }
        if (!changeRemoteDir(remoteTmpDir)) {
            return;
        }
        getProject().executeTarget("children-of-excluded-dir-setup");
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setExcludes(new String[] {"alpha/**"});
        ds.scan();
        compareFiles(ds, new String[] {"delta/delta.xml"},
                    new String[] {"delta"});

        ds = myFTPTask.newScanner(ftp);
        if (!changeRemoteDir(remoteTmpDir)) {
            return;
        }
        ds.setBasedir(new File(getProject().getBaseDir(), "tmp"));
        ds.setExcludes(new String[] {"alpha"});
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/beta.xml",
                                       "alpha/beta/gamma/gamma.xml",
                                        "delta/delta.xml"},
                     new String[] {"alpha/beta", "alpha/beta/gamma", "delta"});

    }
    /**
     *  this test is inspired by a user reporting that deletions of directories with the ftp task do not work
     */
    public void testFTPDelete() {
        getProject().executeTarget("ftp-delete");
    }
    private void compareFiles(DirectoryScanner ds, String[] expectedFiles,
                              String[] expectedDirectories) {
        String includedFiles[] = ds.getIncludedFiles();
        String includedDirectories[] = ds.getIncludedDirectories();
        assertEquals("file present: ", expectedFiles.length,
                     includedFiles.length);
        assertEquals("directories present: ", expectedDirectories.length,
                     includedDirectories.length);

        for (int counter=0; counter < includedFiles.length; counter++) {
            includedFiles[counter] = includedFiles[counter].replace(File.separatorChar, '/');
        }
        Arrays.sort(includedFiles);
        for (int counter=0; counter < includedDirectories.length; counter++) {
            includedDirectories[counter] = includedDirectories[counter]
                            .replace(File.separatorChar, '/');
        }
        Arrays.sort(includedDirectories);
        for (int counter=0; counter < includedFiles.length; counter++) {
            assertEquals(expectedFiles[counter], includedFiles[counter]);
        }
        for (int counter=0; counter < includedDirectories.length; counter++) {
            assertEquals(expectedDirectories[counter], includedDirectories[counter]);
            counter++;
        }
    }
    private static class myFTP extends FTP {
        public FTP.FTPDirectoryScanner newScanner(FTPClient client) {
            return new FTP.FTPDirectoryScanner(client);
        }
        // provide public visibility
        public String resolveFile(String file) {
            return super.resolveFile(file);
        }

    }
}
