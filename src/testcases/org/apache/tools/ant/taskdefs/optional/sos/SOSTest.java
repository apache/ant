/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
package org.apache.tools.ant.taskdefs.optional.sos;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;

/**
 *  Testcase to ensure that command line generation and required attributes are
 *  correct.
 *
 * @author    <a href="mailto:jesse@cryptocard.com">Jesse Stockall</a>
 */
public class SOSTest extends BuildFileTest {

    private Project project;
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

    /**
     *  Constructor for the SOSTest object
     *
     * @param  s  Test name
     */
    public SOSTest(String s) {
        super(s);
    }

    /**
     *  The JUnit setup method
     *
     * @throws  Exception
     */
    protected void setUp()
        throws Exception {
        project = new Project();
        project.setBasedir(".");
    }

    /**
     *  The teardown method for JUnit
     *
     * @throws  Exception
     */
    protected void tearDown()
        throws Exception {
        File file = new File(project.getBaseDir(), LOCAL_PATH);
        if (file.exists()) {
            file.delete();
        }
    }

    /**  Test SOSGetFile flags & commandline generation  */
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
    public void testGetExceptions() {
        configureProject("src/etc/testcases/taskdefs/optional/sos/sos.xml");
        expectSpecificBuildException("sosget.1", "some cause", "sosserverpath attribute must be set!");
        expectSpecificBuildException("sosget.2", "some cause", "username attribute must be set!");
        expectSpecificBuildException("sosget.3", "some cause", "vssserverpath attribute must be set!");
        expectSpecificBuildException("sosget.4", "some cause", "projectpath attribute must be set!");
    }

    /**  Test CheckInFile option flags  */
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
    public void testCheckinProjectFlags() {
        String[] sTestCmdLine = {"soscmd", "-command", "CheckInProject",
                "-recursive", "-server", SOS_SERVER_PATH, "-name", SOS_USERNAME,
                "-password", "", "-database", VSS_SERVER_PATH, "-project",
                DS_VSS_PROJECT_PATH, "", "", "-soshome", SOS_HOME, "-workdir",
                project.getBaseDir().getAbsolutePath(), "-log", SRC_COMMENT,};

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
    public void testCheckinExceptions() {
        configureProject("src/etc/testcases/taskdefs/optional/sos/sos.xml");
        expectSpecificBuildException("soscheckin.1", "some cause", "sosserverpath attribute must be set!");
        expectSpecificBuildException("soscheckin.2", "some cause", "username attribute must be set!");
        expectSpecificBuildException("soscheckin.3", "some cause", "vssserverpath attribute must be set!");
        expectSpecificBuildException("soscheckin.4", "some cause", "projectpath attribute must be set!");
    }

    /**  Test CheckOutFile option flags  */
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
    public void testCheckoutExceptions() {
        configureProject("src/etc/testcases/taskdefs/optional/sos/sos.xml");
        expectSpecificBuildException("soscheckout.1", "some cause", "sosserverpath attribute must be set!");
        expectSpecificBuildException("soscheckout.2", "some cause", "username attribute must be set!");
        expectSpecificBuildException("soscheckout.3", "some cause", "vssserverpath attribute must be set!");
        expectSpecificBuildException("soscheckout.4", "some cause", "projectpath attribute must be set!");
    }

    /**  Test Label option flags  */
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
    public void testLabelExceptions() {
        configureProject("src/etc/testcases/taskdefs/optional/sos/sos.xml");
        expectSpecificBuildException("soslabel.1", "some cause", "sosserverpath attribute must be set!");
        expectSpecificBuildException("soslabel.2", "some cause", "username attribute must be set!");
        expectSpecificBuildException("soslabel.3", "some cause", "vssserverpath attribute must be set!");
        expectSpecificBuildException("soslabel.4", "some cause", "projectpath attribute must be set!");
        expectSpecificBuildException("soslabel.5", "some cause", "label attribute must be set!");
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
        for (int i = 0; i < length; i++) {
            try {
                assertEquals("arg # " + String.valueOf(i),
                        sTestCmdLine[i],
                        sGeneratedCmdLine[i]);
            } catch (ArrayIndexOutOfBoundsException aioob) {
                fail("missing arg " + sTestCmdLine[i]);
            }
        }
        if (sGeneratedCmdLine.length > sTestCmdLine.length) {
            // We have extra elements
            fail("extra args");
        }
    }
}
