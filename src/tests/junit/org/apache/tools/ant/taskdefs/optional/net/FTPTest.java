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
package org.apache.tools.ant.taskdefs.optional.net;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.RetryHandler;
import org.apache.tools.ant.util.Retryable;
import org.apache.tools.ant.util.regexp.RegexpMatcher;
import org.apache.tools.ant.util.regexp.RegexpMatcherFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

//FIXME these tests are more integration than unit tests and report errors badly
public class FTPTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    // keep track of what operating systems are supported here.
    private boolean supportsSymlinks = Os.isFamily("unix");

    private FTPClient ftp;

    private boolean loginSucceeded = false;

    private String loginFailureMessage;

    private String remoteTmpDir = null;

    private myFTP myFTPTask = new myFTP();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/net/ftp.xml");
        Project project = buildRule.getProject();
        project.executeTarget("setup");
        String tmpDir = project.getProperty("tmp.dir");
        ftp = new FTPClient();
        String ftpFileSep = project.getProperty("ftp.filesep");
        myFTPTask.setSeparator(ftpFileSep);
        myFTPTask.setProject(project);
        remoteTmpDir = myFTPTask.resolveFile(tmpDir);
        String remoteHost = project.getProperty("ftp.host");
        int port = Integer.parseInt(project.getProperty("ftp.port"));
        String remoteUser = project.getProperty("ftp.user");
        String password = project.getProperty("ftp.password");
        boolean connectionSucceeded = false;
        try {
            ftp.connect(remoteHost, port);
            connectionSucceeded = true;
        } catch (Exception ex) {
            loginFailureMessage = "could not connect to host " + remoteHost + " on port " + port;
        }
        if (connectionSucceeded) {
            try {
                ftp.login(remoteUser, password);
                loginSucceeded = true;
            } catch (IOException ioe) {
                loginFailureMessage = "could not log on to " + remoteHost + " as user " + remoteUser;
            }
        }
    }

    @After
    public void tearDown() {
        try {
            if (ftp != null) {
                ftp.disconnect();
            }
        } catch (IOException ioe) {
            // do nothing
        }
        buildRule.getProject().executeTarget("cleanup");
    }

    private boolean changeRemoteDir(String remoteDir) {
        boolean result = true;
        try {
            ftp.cwd(remoteDir);
        } catch (Exception ex) {
            System.out.println("could not change directory to " + remoteTmpDir);
            result = false;
        }
        return result;
    }

    @Test
    public void test1() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));

        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha"});
        ds.scan();
        compareFiles(ds, new String[] {}, new String[] {"alpha"});
    }

    @Test
    public void test2() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/"});
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/beta.xml", "alpha/beta/gamma/gamma.xml"},
            new String[] {"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    @Test
    public void test3() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/beta.xml", "alpha/beta/gamma/gamma.xml"},
            new String[] {"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    @Test
    public void testFullPathMatchesCaseSensitive() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/beta/gamma/GAMMA.XML"});
        ds.scan();
        compareFiles(ds, new String[] {}, new String[] {});
    }

    @Test
    public void testFullPathMatchesCaseInsensitive() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setCaseSensitive(false);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/beta/gamma/GAMMA.XML"});
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/gamma/gamma.xml"}, new String[] {});
    }

    @Test
    public void test2ButCaseInsensitive() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"ALPHA/"});
        ds.setCaseSensitive(false);
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/beta.xml", "alpha/beta/gamma/gamma.xml"},
            new String[] {"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    @Test
    public void test2bisButCaseInsensitive() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/BETA/gamma/"});
        ds.setCaseSensitive(false);
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/gamma/gamma.xml"},
            new String[] {"alpha/beta/gamma"});
    }

    @Test
    public void testGetWithSelector() {
        buildRule.executeTarget("ftp-get-with-selector");
        assertThat(buildRule.getLog(), containsString("selectors are not supported in remote filesets"));
        FileSet fsDestination = buildRule.getProject().getReference("fileset-destination-without-selector");
        DirectoryScanner dsDestination = fsDestination.getDirectoryScanner(buildRule.getProject());
        dsDestination.scan();
        String[] sortedDestinationDirectories = dsDestination.getIncludedDirectories();
        String[] sortedDestinationFiles = dsDestination.getIncludedFiles();
        for (int counter = 0; counter < sortedDestinationDirectories.length; counter++) {
            sortedDestinationDirectories[counter] =
                sortedDestinationDirectories[counter].replace(File.separatorChar, '/');
        }
        for (int counter = 0; counter < sortedDestinationFiles.length; counter++) {
            sortedDestinationFiles[counter] =
                sortedDestinationFiles[counter].replace(File.separatorChar, '/');
        }
        FileSet fsSource = buildRule.getProject().getReference("fileset-source-without-selector");
        DirectoryScanner dsSource = fsSource.getDirectoryScanner(buildRule.getProject());
        dsSource.scan();
        compareFiles(dsSource, sortedDestinationFiles, sortedDestinationDirectories);
    }

    @Test
    public void testGetFollowSymlinksTrue() {
        assumeTrue("System does not support Symlinks", supportsSymlinks);
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        buildRule.getProject().executeTarget("ftp-get-directory-symbolic-link");
        FileSet fsDestination = buildRule.getProject().getReference("fileset-destination-without-selector");
        DirectoryScanner dsDestination = fsDestination.getDirectoryScanner(buildRule.getProject());
        dsDestination.scan();
        compareFiles(dsDestination, new String[] {"alpha/beta/gamma/gamma.xml"},
            new String[] {"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    @Test
    public void testGetFollowSymlinksFalse() {
        assumeTrue("System does not support Symlinks", supportsSymlinks);
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        buildRule.getProject().executeTarget("ftp-get-directory-no-symbolic-link");
        FileSet fsDestination = buildRule.getProject().getReference("fileset-destination-without-selector");
        DirectoryScanner dsDestination = fsDestination.getDirectoryScanner(buildRule.getProject());
        dsDestination.scan();
        compareFiles(dsDestination, new String[] {}, new String[] {});
    }

    @Test
    public void testAllowSymlinks() {
        assumeTrue("System does not support Symlinks", supportsSymlinks);
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        buildRule.getProject().executeTarget("symlink-setup");
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/beta/gamma/"});
        ds.setFollowSymlinks(true);
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/gamma/gamma.xml"},
                     new String[] {"alpha/beta/gamma"});
    }

    @Test
    public void testProhibitSymlinks() {
        assumeTrue("System does not support Symlinks", supportsSymlinks);
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        buildRule.getProject().executeTarget("symlink-setup");
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/beta/gamma/"});
        ds.setFollowSymlinks(false);
        ds.scan();
        compareFiles(ds, new String[] {}, new String[] {});
    }

    @Test
    public void testFileSymlink() {
        assumeTrue("System does not support Symlinks", supportsSymlinks);
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        buildRule.getProject().executeTarget("symlink-file-setup");
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/beta/gamma/"});
        ds.setFollowSymlinks(true);
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/gamma/gamma.xml"},
                     new String[] {"alpha/beta/gamma"});
    }

    // father and child pattern test
    @Test
    public void testOrderOfIncludePatternsIrrelevant() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        String[] expectedFiles = {"alpha/beta/beta.xml", "alpha/beta/gamma/gamma.xml"};
        String[] expectedDirectories = {"alpha/beta", "alpha/beta/gamma" };
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/be?a/**", "alpha/beta/gamma/"});
        ds.scan();
        compareFiles(ds, expectedFiles, expectedDirectories);
        // redo the test, but the 2 include patterns are inverted
        ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/beta/gamma/", "alpha/be?a/**"});
        ds.scan();
        compareFiles(ds, expectedFiles, expectedDirectories);
    }

    @Test
    public void testPatternsDifferInCaseScanningSensitive() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/", "ALPHA/"});
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/beta.xml", "alpha/beta/gamma/gamma.xml"},
                     new String[] {"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    @Test
    public void testPatternsDifferInCaseScanningInsensitive() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/", "ALPHA/"});
        ds.setCaseSensitive(false);
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/beta.xml", "alpha/beta/gamma/gamma.xml"},
                     new String[] {"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    @Test
    public void testFullpathDiffersInCaseScanningSensitive() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/beta/gamma/gamma.xml", "alpha/beta/gamma/GAMMA.XML"});
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/gamma/gamma.xml"}, new String[] {});
    }

    @Test
    public void testFullpathDiffersInCaseScanningInsensitive() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {
            "alpha/beta/gamma/gamma.xml",
            "alpha/beta/gamma/GAMMA.XML"
        });
        ds.setCaseSensitive(false);
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/gamma/gamma.xml"},
                     new String[] {});
    }

    @Test
    public void testParentDiffersInCaseScanningSensitive() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/", "ALPHA/beta/"});
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/beta.xml",
                                       "alpha/beta/gamma/gamma.xml"},
                     new String[] {"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    @Test
    public void testParentDiffersInCaseScanningInsensitive() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/", "ALPHA/beta/"});
        ds.setCaseSensitive(false);
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/beta.xml", "alpha/beta/gamma/gamma.xml"},
                new String[] {"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    @Test
    public void testExcludeOneFile() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"**/*.xml"});
        ds.setExcludes(new String[] {"alpha/beta/b*xml"});
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/gamma/gamma.xml"}, new String[] {});
    }

    @Test
    public void testExcludeHasPrecedence() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/**"});
        ds.setExcludes(new String[] {"alpha/**"});
        ds.scan();
        compareFiles(ds, new String[] {}, new String[] {});
    }

    @Test
    public void testAlternateIncludeExclude() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setIncludes(new String[] {"alpha/**", "alpha/beta/gamma/**"});
        ds.setExcludes(new String[] {"alpha/beta/**"});
        ds.scan();
        compareFiles(ds, new String[] {}, new String[] {"alpha"});
    }

    @Test
    public void testAlternateExcludeInclude() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setExcludes(new String[] {"alpha/**", "alpha/beta/gamma/**"});
        ds.setIncludes(new String[] {"alpha/beta/**"});
        ds.scan();
        compareFiles(ds, new String[] {}, new String[] {});
    }

    /**
     * Test inspired by Bug#1415.
     */
    @Test
    public void testChildrenOfExcludedDirectory() {
        assumeTrue(loginFailureMessage, loginSucceeded);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        buildRule.getProject().executeTarget("children-of-excluded-dir-setup");
        FTP.FTPDirectoryScanner ds = myFTPTask.newScanner(ftp);
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setExcludes(new String[] {"alpha/**"});
        ds.scan();
        compareFiles(ds, new String[] {"delta/delta.xml"}, new String[] {"delta"});

        ds = myFTPTask.newScanner(ftp);
        assumeTrue("Could not change remote directory", changeRemoteDir(remoteTmpDir));
        ds.setBasedir(new File(buildRule.getProject().getBaseDir(), "tmp"));
        ds.setExcludes(new String[] {"alpha"});
        ds.scan();
        compareFiles(ds, new String[] {"alpha/beta/beta.xml", "alpha/beta/gamma/gamma.xml",
                        "delta/delta.xml"},
                     new String[] {"alpha/beta", "alpha/beta/gamma", "delta"});
    }

    /**
     * This class enables the use of the log messages as a way of testing
     * the number of files actually transferred.
     * It uses the ant regular expression mechanism to get a regex parser
     * to parse the log output.
     */
    private class CountLogListener extends DefaultLogger {
        private Vector<String> lastMatchGroups = null;
        private RegexpMatcher matcher = new RegexpMatcherFactory().newRegexpMatcher();

        /**
         * The only constructor for a CountLogListener
         * @param pattern a regular expression pattern.  It should have
         * one parenthesized group and that group should contain the
         * number desired.
         */
        public CountLogListener(String pattern) {
            super();
            this.matcher.setPattern(pattern);
        }


        /*
         * @param event the build event that is being logged.
         */
        public void messageLogged(BuildEvent event) {
            String message = event.getMessage();
            if (this.matcher.matches(message)) {
                lastMatchGroups = this.matcher.getGroups(message);
            }
            super.messageLogged(event);
        }

        /**
         * returns the desired number that results from parsing the log
         * message
         * @return the number of files indicated in the desired message or -1
         * if a matching log message was never found.
         */
        public int getCount() {
            if (this.lastMatchGroups == null) {
                return -1;
            }
            return Integer.parseInt(this.lastMatchGroups.get(1));
        }
    }

    /**
     * This class enables the use of the log to count the number
     * of times a message has been emitted.
     */
    private class LogCounter extends DefaultLogger {
        private Map<String, Integer> searchMap = new HashMap<>();

        public void addLogMessageToSearch(String message) {
            searchMap.put(message, 0);
        }

        /*
         * @param event the build event that is being logged.
         */
        public void messageLogged(BuildEvent event) {
            String message = event.getMessage();
            Integer mcnt = searchMap.get(message);
            if (null != mcnt) {
                searchMap.put(message, mcnt + 1);
            }
            super.messageLogged(event);
        }

        /**
         * @return the number of times that the looked for message was sent
         * to the log
         */
        public int getMatchCount(String message) {
            Integer mcnt = searchMap.get(message);
            if (null != mcnt) {
                return mcnt;
            }
            return 0;
        }
    }
    /**
     * Tests the combination of the newer parameter and the
     * serverTimezoneConfig  parameter in the PUT action.  The default
     * configuration is an ftp server on localhost which formats
     * timestamps as GMT.
     */
    @Test
    public void testTimezonePut() {
        CountLogListener log = new CountLogListener("(\\d+) files? sent");
        buildRule.getProject().executeTarget("timed.test.setup");
        buildRule.getProject().addBuildListener(log);
        buildRule.getProject().executeTarget("timed.test.put.older");
        assertEquals(1, log.getCount());
    }

    /**
     * Tests the combination of the newer parameter and the
     * serverTimezoneConfig  parameter in the GET action.  The default
     * configuration is an ftp server on localhost which formats
     * timestamps as GMT.
     */
    @Test
    public void testTimezoneGet() {
        CountLogListener log = new CountLogListener("(\\d+) files? retrieved");
        buildRule.getProject().executeTarget("timed.test.setup");
        buildRule.getProject().addBuildListener(log);
        buildRule.getProject().executeTarget("timed.test.get.older");
        assertEquals(3, log.getCount());
    }


    /**
     * Tests that the presence of one of the server config params forces
     * the system type to Unix if not specified.
     */
    @Test
    public void testConfiguration1() {
        int[] expectedCounts = {1, 1, 0, 1, 0, 0, 0};
        performConfigTest("configuration.1", expectedCounts);
    }

    /**
     * Tests the systemTypeKey attribute.
     */
    @Test
    public void testConfiguration2() {
        int[] expectedCounts = {1, 0, 0, 1, 1, 0, 0};
        performConfigTest("configuration.2", expectedCounts);
    }

    /**
     * Tests the systemTypeKey attribute with UNIX specified.
     */
    @Test
    public void testConfiguration3() {
        int[] expectedCounts = {1, 0, 1, 0, 0, 1, 0};
        performConfigTest("configuration.3", expectedCounts);
    }

    @Test
    public void testConfigurationGoodLang() {
        int[] expectedCounts = {1, 1, 0, 0, 0, 0, 1};
        performConfigTest("configuration.lang.good", expectedCounts);
    }

    @Test(expected = BuildException.class)
    public void testConfigurationBadLang() {
        int[] expectedCounts = {1, 1, 0, 0, 0, 0, 1};
        performConfigTest("configuration.lang.bad", expectedCounts);
    }
    /**
     * Tests the systemTypeKey attribute.
     */
    @Test
    public void testConfigurationNone() {
        int[] expectedCounts = {0, 0, 0, 0, 0, 0, 0};
        performConfigTest("configuration.none", expectedCounts);

    }

    private void performConfigTest(String target, int[] expectedCounts) {
        String[] messages = new String[]{
                "custom configuration",
                "custom config: system key = default (UNIX)",
                "custom config: system key = UNIX",
                "custom config: server time zone ID = " + buildRule.getProject().getProperty("ftp.server.timezone"),
                "custom config: system key = WINDOWS",
                "custom config: default date format = yyyy/MM/dd HH:mm",
                "custom config: server language code = de"
        };
        LogCounter counter = new LogCounter();
        Arrays.stream(messages).forEach(counter::addLogMessageToSearch);

        buildRule.getProject().addBuildListener(counter);
        buildRule.getProject().executeTarget(target);
        for (int i = 0; i < messages.length; i++) {
            assertEquals("target " + target + ":message " + i,
                    expectedCounts[i], counter.getMatchCount(messages[i]));
        }

    }


    /**
     *  this test is inspired by a user reporting that deletions of directories with the ftp task do not work
     */
    @Test
    public void testFTPDelete() {
        buildRule.getProject().executeTarget("ftp-delete");
    }

    private void compareFiles(DirectoryScanner ds, String[] expectedFiles,
                              String[] expectedDirectories) {
        String[] includedFiles = ds.getIncludedFiles();
        String[] includedDirectories = ds.getIncludedDirectories();
        assertEquals("file present: ", expectedFiles.length,
                     includedFiles.length);
        assertEquals("directories present: ", expectedDirectories.length,
                     includedDirectories.length);

        for (int counter = 0; counter < includedFiles.length; counter++) {
            includedFiles[counter] = includedFiles[counter].replace(File.separatorChar, '/');
        }
        Arrays.sort(includedFiles);
        for (int counter = 0; counter < includedDirectories.length; counter++) {
            includedDirectories[counter] = includedDirectories[counter]
                            .replace(File.separatorChar, '/');
        }
        Arrays.sort(includedDirectories);
        for (int counter = 0; counter < includedFiles.length; counter++) {
            assertEquals(expectedFiles[counter], includedFiles[counter]);
        }
        for (int counter = 0; counter < includedDirectories.length; counter++) {
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


    public abstract static class myRetryableFTP extends FTP {
        private final int numberOfFailuresToSimulate;
        private int simulatedFailuresLeft;

        protected myRetryableFTP(int numberOfFailuresToSimulate) {
            this.numberOfFailuresToSimulate = numberOfFailuresToSimulate;
            this.simulatedFailuresLeft = numberOfFailuresToSimulate;
        }

        protected void getFile(FTPClient ftp, String dir, String filename)
                throws IOException, BuildException {
            if (this.simulatedFailuresLeft > 0) {
                this.simulatedFailuresLeft--;
                throw new IOException("Simulated failure for testing");
            }
           super.getFile(ftp, dir, filename);
        }

        protected void executeRetryable(RetryHandler h, Retryable r,
                String filename) throws IOException {

            this.simulatedFailuresLeft = this.numberOfFailuresToSimulate;
            super.executeRetryable(h, r, filename);
        }
    }

    public static class oneFailureFTP extends myRetryableFTP {
        public oneFailureFTP() {
            super(1);
        }
    }

    public static class twoFailureFTP extends myRetryableFTP {
        public twoFailureFTP() {
            super(2);
        }
    }

    public static class threeFailureFTP extends myRetryableFTP {
        public threeFailureFTP() {
            super(3);
        }
    }

    public static class randomFailureFTP extends myRetryableFTP {
        public randomFailureFTP() {
            super(new Random().nextInt(Short.MAX_VALUE));
        }
    }

    @Test
    public void testGetWithSelectorRetryable1() {
        buildRule.getProject().addTaskDefinition("ftp", oneFailureFTP.class);
        buildRule.getProject().executeTarget("ftp-get-with-selector-retryable");
    }

    @Test
    public void testGetWithSelectorRetryable2() {
        buildRule.getProject().addTaskDefinition("ftp", twoFailureFTP.class);
        buildRule.getProject().executeTarget("ftp-get-with-selector-retryable");
    }

    /**
     * Two retries expected, continued after two.
     */
    @Test(expected = BuildException.class)
    public void testGetWithSelectorRetryable3() {
        buildRule.getProject().addTaskDefinition("ftp", threeFailureFTP.class);
        buildRule.getProject().executeTarget("ftp-get-with-selector-retryable");
    }

    @Test
    public void testGetWithSelectorRetryableRandom() {
        buildRule.getProject().addTaskDefinition("ftp", randomFailureFTP.class);
        buildRule.getProject().setProperty("ftp.retries", "forever");
        buildRule.getProject().executeTarget("ftp-get-with-selector-retryable");
    }

    @Test
    public void testInitialCommand() {
        performCommandTest("test-initial-command", new int[] {1, 0});
    }

    @Test
    public void testSiteAction() {
        performCommandTest("test-site-action", new int[] {1, 0});
    }

    private void performCommandTest(String target, int[] expectedCounts) {
        String[] messages = new String[]{
                "Doing Site Command: umask 222",
                "Failed to issue Site Command: umask 222",
        };
        LogCounter counter = new LogCounter();
        Arrays.stream(messages).forEach(counter::addLogMessageToSearch);

        buildRule.getProject().addBuildListener(counter);
        buildRule.getProject().executeTarget(target);
        for (int i = 0; i < messages.length; i++) {
            assertEquals("target " + target + ":message " + i,
                    expectedCounts[i], counter.getMatchCount(messages[i]));
        }

    }

}
