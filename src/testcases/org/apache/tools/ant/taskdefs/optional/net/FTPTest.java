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
