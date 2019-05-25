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
package org.apache.tools.ant.taskdefs.optional.sos;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

/**
 *  Testcase to ensure that command line generation and required attributes are
 *  correct.
 *
 */
public class SOSTest {

    private Commandline commandline;

    private static final String VSS_SERVER_PATH = "\\\\server\\vss\\srcsafe.ini";
    private static final String VSS_PROJECT_PATH = "/SourceRoot/Project";
    private static final String DS_VSS_PROJECT_PATH = "$/SourceRoot/Project";
    private static final String SOS_SERVER_PATH = "192.168.0.1:8888";
    private static final String SOS_USERNAME = "ant";
    private static final String SOS_PASSWORD = "rocks";
    private static final String LOCAL_PATH = "testdir";
    private static final String SRC_FILE = "Class1.java";
    private static final String SRC_LABEL = "label1";
    private static final String SRC_COMMENT = "I fixed a bug";
    private static final String SOS_HOME = "/home/user/.sos";
    private static final String VERSION = "007";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    private Project project;

    @Before
    public void setUp() {
        project = new Project();
        project.init();
        project.setBasedir(".");
    }

    @After
    public void tearDown() {
        File file = new File(project.getBaseDir(), LOCAL_PATH);
        if (file.exists()) {
            file.delete();
        }
    }

    /**  Test SOSGetFile flags & commandline generation  */
    @Test
    public void testGetFileFlags() {
        String[] sTestCmdLine = {"soscmd", "-command", "GetFile", "-file",
                SRC_FILE, "-revision", "007", "-server", SOS_SERVER_PATH, "-name",
                SOS_USERNAME, "-password", SOS_PASSWORD, "-database", VSS_SERVER_PATH,
                "-project", DS_VSS_PROJECT_PATH, "-verbose", "-nocompress",
                "-nocache", "-workdir", project.getBaseDir().getAbsolutePath()
                 + File.separator + LOCAL_PATH};

        // Set up a SOSGet task
        SOSGet sosGet = new SOSGet();
        sosGet.setProject(project);
        sosGet.setVssServerPath(VSS_SERVER_PATH);
        sosGet.setSosServerPath(SOS_SERVER_PATH);
        sosGet.setProjectPath(VSS_PROJECT_PATH);
        sosGet.setFile(SRC_FILE);
        sosGet.setUsername(SOS_USERNAME);
        sosGet.setPassword(SOS_PASSWORD);
        sosGet.setVersion(VERSION);
        sosGet.setLocalPath(new Path(project, LOCAL_PATH));
        sosGet.setNoCache(true);
        sosGet.setNoCompress(true);
        sosGet.setVerbose(true);
        sosGet.setRecursive(true);

        commandline = sosGet.buildCmdLine();

        checkCommandLines(sTestCmdLine, commandline.getCommandline());
    }

    /**  Test SOSGetProject flags & commandline generation  */
    @Test
    public void testGetProjectFlags() {
        String[] sTestCmdLine = {"soscmd", "-command", "GetProject", "-recursive",
                "-label", SRC_LABEL, "-server", SOS_SERVER_PATH, "-name", SOS_USERNAME,
                "-password", "", "-database", VSS_SERVER_PATH, "-project",
                DS_VSS_PROJECT_PATH, "", "", "-soshome", SOS_HOME, "-workdir",
                project.getBaseDir().getAbsolutePath()};

        // Set up a SOSGet task
        SOSGet sosGet = new SOSGet();
        sosGet.setProject(project);
        sosGet.setVssServerPath(VSS_SERVER_PATH);
        sosGet.setSosServerPath(SOS_SERVER_PATH);
        sosGet.setProjectPath(DS_VSS_PROJECT_PATH);
        sosGet.setLabel(SRC_LABEL);
        sosGet.setUsername(SOS_USERNAME);
        sosGet.setSosHome(SOS_HOME);
        sosGet.setNoCache(true);
        sosGet.setNoCompress(false);
        sosGet.setVerbose(false);
        sosGet.setRecursive(true);

        commandline = sosGet.buildCmdLine();

        checkCommandLines(sTestCmdLine, commandline.getCommandline());
    }

    /**  Tests SOSGet required attributes.  */
    @Test
    public void testGetExceptions() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/sos/sos.xml");
        expectSpecificBuildException("sosget.1", "sosserverpath attribute must be set!");
        expectSpecificBuildException("sosget.2", "username attribute must be set!");
        expectSpecificBuildException("sosget.3", "vssserverpath attribute must be set!");
        expectSpecificBuildException("sosget.4", "projectpath attribute must be set!");
    }

    /**  Test CheckInFile option flags  */
    @Test
    public void testCheckinFileFlags() {
        String[] sTestCmdLine = {"soscmd", "-command", "CheckInFile", "-file",
                SRC_FILE, "-server", SOS_SERVER_PATH, "-name", SOS_USERNAME,
                "-password", SOS_PASSWORD, "-database", VSS_SERVER_PATH, "-project",
                DS_VSS_PROJECT_PATH, "-verbose", "-nocompress", "-nocache",
                "-workdir", project.getBaseDir().getAbsolutePath() + File.separator
                 + LOCAL_PATH, "-log", SRC_COMMENT};

        // Set up a SOSCheckin task
        SOSCheckin sosCheckin = new SOSCheckin();
        sosCheckin.setProject(project);
        sosCheckin.setVssServerPath(VSS_SERVER_PATH);
        sosCheckin.setSosServerPath(SOS_SERVER_PATH);
        sosCheckin.setProjectPath(VSS_PROJECT_PATH);
        sosCheckin.setFile(SRC_FILE);
        sosCheckin.setComment(SRC_COMMENT);
        sosCheckin.setUsername(SOS_USERNAME);
        sosCheckin.setPassword(SOS_PASSWORD);
        sosCheckin.setLocalPath(new Path(project, LOCAL_PATH));
        sosCheckin.setNoCache(true);
        sosCheckin.setNoCompress(true);
        sosCheckin.setVerbose(true);
        sosCheckin.setRecursive(true);

        commandline = sosCheckin.buildCmdLine();

        checkCommandLines(sTestCmdLine, commandline.getCommandline());
    }

    /**  Test CheckInProject option flags  */
    @Test
    public void testCheckinProjectFlags() {
        String[] sTestCmdLine = {"soscmd", "-command", "CheckInProject",
                "-recursive", "-server", SOS_SERVER_PATH, "-name", SOS_USERNAME,
                "-password", "", "-database", VSS_SERVER_PATH, "-project",
                DS_VSS_PROJECT_PATH, "", "", "-soshome", SOS_HOME, "-workdir",
                project.getBaseDir().getAbsolutePath(), "-log", SRC_COMMENT};

        // Set up a SOSCheckin task
        SOSCheckin sosCheckin = new SOSCheckin();
        sosCheckin.setProject(project);
        sosCheckin.setVssServerPath(VSS_SERVER_PATH);
        sosCheckin.setSosServerPath(SOS_SERVER_PATH);
        sosCheckin.setProjectPath(DS_VSS_PROJECT_PATH);
        sosCheckin.setComment(SRC_COMMENT);
        sosCheckin.setUsername(SOS_USERNAME);
        sosCheckin.setSosHome(SOS_HOME);
        sosCheckin.setNoCache(true);
        sosCheckin.setNoCompress(false);
        sosCheckin.setVerbose(false);
        sosCheckin.setRecursive(true);

        commandline = sosCheckin.buildCmdLine();

        checkCommandLines(sTestCmdLine, commandline.getCommandline());
    }

    /**  Test SOSCheckIn required attributes.  */
    @Test
    public void testCheckinExceptions() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/sos/sos.xml");
        expectSpecificBuildException("soscheckin.1", "sosserverpath attribute must be set!");
        expectSpecificBuildException("soscheckin.2", "username attribute must be set!");
        expectSpecificBuildException("soscheckin.3", "vssserverpath attribute must be set!");
        expectSpecificBuildException("soscheckin.4", "projectpath attribute must be set!");
    }

    /**  Test CheckOutFile option flags  */
    @Test
    public void testCheckoutFileFlags() {
        String[] sTestCmdLine = {"soscmd", "-command", "CheckOutFile", "-file",
                SRC_FILE, "-server", SOS_SERVER_PATH, "-name", SOS_USERNAME,
                "-password", SOS_PASSWORD, "-database", VSS_SERVER_PATH, "-project",
                DS_VSS_PROJECT_PATH, "-verbose", "-nocompress", "-nocache",
                "-workdir", project.getBaseDir().getAbsolutePath()
                 + File.separator + LOCAL_PATH};

        // Set up a SOSCheckout task
        SOSCheckout sosCheckout = new SOSCheckout();
        sosCheckout.setProject(project);
        sosCheckout.setVssServerPath(VSS_SERVER_PATH);
        sosCheckout.setSosServerPath(SOS_SERVER_PATH);
        sosCheckout.setProjectPath(DS_VSS_PROJECT_PATH);
        sosCheckout.setFile(SRC_FILE);
        sosCheckout.setUsername(SOS_USERNAME);
        sosCheckout.setPassword(SOS_PASSWORD);
        sosCheckout.setLocalPath(new Path(project, LOCAL_PATH));
        sosCheckout.setNoCache(true);
        sosCheckout.setNoCompress(true);
        sosCheckout.setVerbose(true);
        sosCheckout.setRecursive(true);

        commandline = sosCheckout.buildCmdLine();

        checkCommandLines(sTestCmdLine, commandline.getCommandline());
    }

    /**  Test CheckOutProject option flags  */
    @Test
    public void testCheckoutProjectFlags() {
        String[] sTestCmdLine = {"soscmd", "-command", "CheckOutProject",
                "-recursive", "-server", SOS_SERVER_PATH, "-name", SOS_USERNAME,
                "-password", "", "-database", VSS_SERVER_PATH, "-project",
                DS_VSS_PROJECT_PATH, "", "", "-soshome", SOS_HOME, "-workdir",
                project.getBaseDir().getAbsolutePath()};

        // Set up a sosCheckout task
        SOSCheckout sosCheckout = new SOSCheckout();
        sosCheckout.setProject(project);
        sosCheckout.setVssServerPath(VSS_SERVER_PATH);
        sosCheckout.setSosServerPath(SOS_SERVER_PATH);
        sosCheckout.setProjectPath(VSS_PROJECT_PATH);
        sosCheckout.setUsername(SOS_USERNAME);
        sosCheckout.setSosHome(SOS_HOME);
        sosCheckout.setNoCache(true);
        sosCheckout.setNoCompress(false);
        sosCheckout.setVerbose(false);
        sosCheckout.setRecursive(true);

        commandline = sosCheckout.buildCmdLine();

        checkCommandLines(sTestCmdLine, commandline.getCommandline());
    }

    /**  Test SOSCheckout required attributes.  */
    @Test
    public void testCheckoutExceptions() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/sos/sos.xml");
        expectSpecificBuildException("soscheckout.1", "sosserverpath attribute must be set!");
        expectSpecificBuildException("soscheckout.2", "username attribute must be set!");
        expectSpecificBuildException("soscheckout.3", "vssserverpath attribute must be set!");
        expectSpecificBuildException("soscheckout.4", "projectpath attribute must be set!");
    }

    /**  Test Label option flags  */
    @Test
    public void testLabelFlags() {
        String[] sTestCmdLine = {"soscmd", "-command", "AddLabel", "-server",
                SOS_SERVER_PATH, "-name", SOS_USERNAME, "-password", "", "-database",
                VSS_SERVER_PATH, "-project", DS_VSS_PROJECT_PATH, "-label",
                SRC_LABEL, "-verbose", "-log", SRC_COMMENT};

        // Set up a sosCheckout task
        SOSLabel sosLabel = new SOSLabel();
        sosLabel.setVssServerPath(VSS_SERVER_PATH);
        sosLabel.setSosServerPath(SOS_SERVER_PATH);
        sosLabel.setProjectPath(DS_VSS_PROJECT_PATH);
        sosLabel.setUsername(SOS_USERNAME);
        sosLabel.setSosHome(SOS_HOME);
        sosLabel.setComment(SRC_COMMENT);
        sosLabel.setLabel(SRC_LABEL);
        sosLabel.setNoCache(true);
        sosLabel.setNoCompress(false);
        sosLabel.setVerbose(true);

        commandline = sosLabel.buildCmdLine();

        checkCommandLines(sTestCmdLine, commandline.getCommandline());
    }

    /**  Test SOSLabel required attributes.  */
    @Test
    public void testLabelExceptions() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/sos/sos.xml");
        expectSpecificBuildException("soslabel.1", "sosserverpath attribute must be set!");
        expectSpecificBuildException("soslabel.2", "username attribute must be set!");
        expectSpecificBuildException("soslabel.3", "vssserverpath attribute must be set!");
        expectSpecificBuildException("soslabel.4", "projectpath attribute must be set!");
        expectSpecificBuildException("soslabel.5", "label attribute must be set!");
    }

    private void expectSpecificBuildException(String target,
                                              String exceptionMessage) {
        thrown.expect(BuildException.class);
        thrown.expectMessage(exceptionMessage);
        buildRule.executeTarget(target);
    }

    /**
     *  Iterate through the generated command line comparing it to reference
     *  one.
     *
     * @param  sTestCmdLine       The reference command line;
     * @param  sGeneratedCmdLine  The generated command line;
     */
    private void checkCommandLines(String[] sTestCmdLine, String[] sGeneratedCmdLine) {
        int length = sTestCmdLine.length;
        assertEquals("number of arguments doesn't match", length, sGeneratedCmdLine.length);
        for (int i = 0; i < length; i++) {
            assertEquals("arg # " + String.valueOf(i),
                    sTestCmdLine[i], sGeneratedCmdLine[i]);
        }
    }
}
