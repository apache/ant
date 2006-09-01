/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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
package org.apache.tools.ant.taskdefs.optional.vss;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Tstamp;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

/**
 *  Testcase to ensure that command line generation and required attributes are correct.
 *
 */
public class MSVSSTest extends BuildFileTest implements MSVSSConstants {

    private Commandline commandline;

    private static final String VSS_SERVER_PATH = "\\\\server\\vss\\srcsafe.ini";
    private static final String VSS_PROJECT_PATH = "/SourceRoot/Project";
    private static final String DS_VSS_PROJECT_PATH = "$/SourceRoot/Project";
    private static final String VSS_USERNAME = "ant";
    private static final String VSS_PASSWORD = "rocks";
    private static final String LOCAL_PATH = "testdir";
    private static final String SRC_FILE = "Class1.java";
    private static final String SRC_LABEL = "label1";
    private static final String LONG_LABEL = "123456789012345678901234567890";
    private static final String SRC_COMMENT = "I fixed a bug";
    private static final String VERSION = "007";
    private static final String DATE = "00-00-00";
    private static final String DATE2 = "01-01-01";
    private static final String OUTPUT = "output.log";
    private static final String SS_DIR = "c:/winnt".replace('/', File.separatorChar);

    /**
     *  Constructor for the MSVSSTest object
     *
     * @param  s  Test name
     */
    public MSVSSTest(String s) {
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

    /**  Tests VSSGet commandline generation.  */
    public void testGetCommandLine() {
        String[] sTestCmdLine = {MSVSS.SS_EXE, MSVSS.COMMAND_GET, DS_VSS_PROJECT_PATH,
                MSVSS.FLAG_OVERRIDE_WORKING_DIR + project.getBaseDir().getAbsolutePath()
                 + File.separator + LOCAL_PATH, MSVSS.FLAG_AUTORESPONSE_DEF,
                MSVSS.FLAG_RECURSION, MSVSS.FLAG_VERSION + VERSION, MSVSS.FLAG_LOGIN
                 + VSS_USERNAME + "," + VSS_PASSWORD, FLAG_FILETIME_UPDATED, FLAG_SKIP_WRITABLE};

        // Set up a VSSGet task
        MSVSSGET vssGet = new MSVSSGET();
        vssGet.setProject(project);
        vssGet.setRecursive(true);
        vssGet.setLocalpath(new Path(project, LOCAL_PATH));
        vssGet.setLogin(VSS_USERNAME + "," + VSS_PASSWORD);
        vssGet.setVersion(VERSION);
        vssGet.setQuiet(false);
        vssGet.setDate(DATE);
        vssGet.setLabel(SRC_LABEL);
        vssGet.setVsspath(VSS_PROJECT_PATH);
        MSVSS.CurrentModUpdated cmu = new MSVSS.CurrentModUpdated();
        cmu.setValue(TIME_UPDATED);
        vssGet.setFileTimeStamp(cmu);
        MSVSS.WritableFiles wf = new MSVSS.WritableFiles();
        wf.setValue(WRITABLE_SKIP);
        vssGet.setWritableFiles(wf);

        commandline = vssGet.buildCmdLine();

        checkCommandLines(sTestCmdLine, commandline.getCommandline());
    }

    /**  Tests VSSGet required attributes.  */
    public void testGetExceptions() {
        configureProject("src/etc/testcases/taskdefs/optional/vss/vss.xml");
        expectSpecificBuildException("vssget.1", "some cause", "vsspath attribute must be set!");
    }

    /**  Tests Label commandline generation.  */
    public void testLabelCommandLine1() {
        String[] sTestCmdLine = {MSVSS.SS_EXE, MSVSS.COMMAND_LABEL, DS_VSS_PROJECT_PATH,
                MSVSS.FLAG_COMMENT + SRC_COMMENT, MSVSS.FLAG_AUTORESPONSE_YES,
                MSVSS.FLAG_LABEL + SRC_LABEL, MSVSS.FLAG_VERSION + VERSION, MSVSS.FLAG_LOGIN
                 + VSS_USERNAME + "," + VSS_PASSWORD};

        // Set up a VSSLabel task
        MSVSSLABEL vssLabel = new MSVSSLABEL();
        vssLabel.setProject(project);
        vssLabel.setComment(SRC_COMMENT);
        vssLabel.setLogin(VSS_USERNAME + "," + VSS_PASSWORD);
        vssLabel.setVersion(VERSION);
        vssLabel.setAutoresponse("Y");
        vssLabel.setLabel(SRC_LABEL);
        vssLabel.setVsspath(VSS_PROJECT_PATH);

        commandline = vssLabel.buildCmdLine();

        checkCommandLines(sTestCmdLine, commandline.getCommandline());
    }

    /**  Tests Label commandline generation with a label of more than 31 chars.  */
    public void testLabelCommandLine2() {
        String[] sTestCmdLine = {MSVSS.SS_EXE, MSVSS.COMMAND_LABEL, DS_VSS_PROJECT_PATH,
                MSVSS.FLAG_COMMENT + SRC_COMMENT, MSVSS.FLAG_AUTORESPONSE_DEF,
                MSVSS.FLAG_LABEL + LONG_LABEL,
                MSVSS.FLAG_LOGIN + VSS_USERNAME + "," + VSS_PASSWORD};

        // Set up a VSSLabel task
        MSVSSLABEL vssLabel = new MSVSSLABEL();
        vssLabel.setProject(project);
        vssLabel.setComment(SRC_COMMENT);
        vssLabel.setLogin(VSS_USERNAME + "," + VSS_PASSWORD);
        vssLabel.setLabel(LONG_LABEL + "blahblah");
        vssLabel.setVsspath(VSS_PROJECT_PATH);

        commandline = vssLabel.buildCmdLine();

        checkCommandLines(sTestCmdLine, commandline.getCommandline());
    }

    /**
     * Test VSSLabel required attributes.
     */
    public void testLabelExceptions() {
        configureProject("src/etc/testcases/taskdefs/optional/vss/vss.xml");
        expectSpecificBuildException("vsslabel.1", "some cause", "vsspath attribute must be set!");
        expectSpecificBuildException("vsslabel.2", "some cause", "label attribute must be set!");
    }

    /**  Tests VSSHistory commandline generation with from label.  */
    public void testHistoryCommandLine1() {
        String[] sTestCmdLine = {MSVSS.SS_EXE, MSVSS.COMMAND_HISTORY, DS_VSS_PROJECT_PATH,
                MSVSS.FLAG_AUTORESPONSE_DEF, MSVSS.FLAG_VERSION_LABEL + LONG_LABEL
                 + MSVSS.VALUE_FROMLABEL + SRC_LABEL, MSVSS.FLAG_LOGIN + VSS_USERNAME
                 + "," + VSS_PASSWORD, MSVSS.FLAG_OUTPUT + project.getBaseDir().getAbsolutePath()
                 + File.separator + OUTPUT};

        // Set up a VSSHistory task
        MSVSSHISTORY vssHistory = new MSVSSHISTORY();
        vssHistory.setProject(project);

        vssHistory.setLogin(VSS_USERNAME + "," + VSS_PASSWORD);

        vssHistory.setFromLabel(SRC_LABEL);
        vssHistory.setToLabel(LONG_LABEL + "blahblah");
        vssHistory.setVsspath(VSS_PROJECT_PATH);
        vssHistory.setRecursive(false);
        vssHistory.setOutput(new File(project.getBaseDir().getAbsolutePath(), OUTPUT));

        commandline = vssHistory.buildCmdLine();

        checkCommandLines(sTestCmdLine, commandline.getCommandline());
    }

    /**  Tests VSSHistory commandline generation with from date.  */
    public void testHistoryCommandLine2() {
        String[] sTestCmdLine = {MSVSS.SS_EXE, MSVSS.COMMAND_HISTORY, DS_VSS_PROJECT_PATH,
                MSVSS.FLAG_AUTORESPONSE_DEF, MSVSS.FLAG_VERSION_DATE + DATE + MSVSS.VALUE_FROMDATE
                + DATE2, MSVSS.FLAG_RECURSION,  MSVSS.FLAG_LOGIN + VSS_USERNAME + "," + VSS_PASSWORD};

        // Set up a VSSHistory task
        MSVSSHISTORY vssHistory = new MSVSSHISTORY();
        vssHistory.setProject(project);
        vssHistory.setLogin(VSS_USERNAME + "," + VSS_PASSWORD);
        vssHistory.setFromDate(DATE2);
        vssHistory.setToDate(DATE);
        vssHistory.setVsspath(VSS_PROJECT_PATH);
        vssHistory.setRecursive(true);

        commandline = vssHistory.buildCmdLine();

        checkCommandLines(sTestCmdLine, commandline.getCommandline());
    }

    /**  Tests VSSHistory commandline generation with date calculation.  */
    public void testHistoryCommandLine3() {
        // Set up a Timestamp
        Tstamp tstamp = new Tstamp();
        Location location = new Location("src/etc/testcases/taskdefs/optional/vss/vss.xml");
        tstamp.setLocation(location);
        tstamp.setProject(project);
        Tstamp.CustomFormat format = tstamp.createFormat();
        format.setProperty("today");
        format.setPattern("HH:mm:ss z");
        format.setTimezone("GMT");
        Date date = Calendar.getInstance().getTime();
        format.execute(project, date, location);
        String today = project.getProperty("today");

        // Get today's date
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss z");
        sdf.setTimeZone( TimeZone.getTimeZone("GMT") );
        String expected = sdf.format(date);

        // Set up a VSSHistory task
        MSVSSHISTORY vssHistory = new MSVSSHISTORY();
        vssHistory.setProject(project);
        vssHistory.setLogin(VSS_USERNAME);
        vssHistory.setToDate(today);
        vssHistory.setVsspath(VSS_PROJECT_PATH);

        String[] sTestCmdLine = {MSVSS.SS_EXE, MSVSS.COMMAND_HISTORY, DS_VSS_PROJECT_PATH,
        MSVSS.FLAG_AUTORESPONSE_DEF, MSVSS.FLAG_VERSION_DATE + expected, MSVSS.FLAG_LOGIN + VSS_USERNAME};

        commandline = vssHistory.buildCmdLine();

        checkCommandLines(sTestCmdLine, commandline.getCommandline());
    }

    /**
     * Tests VSSHistory required attributes.
     */
    public void testHistoryExceptions() {
        configureProject("src/etc/testcases/taskdefs/optional/vss/vss.xml");
        expectSpecificBuildException("vsshistory.1", "some cause", "vsspath attribute must be set!");
    }

    /**  Tests CheckIn commandline generation.  */
    public void testCheckinCommandLine() {
        String[] sTestCmdLine = {MSVSS.SS_EXE, MSVSS.COMMAND_CHECKIN, DS_VSS_PROJECT_PATH,
                MSVSS.FLAG_AUTORESPONSE_NO, MSVSS.FLAG_WRITABLE, MSVSS.FLAG_LOGIN + VSS_USERNAME,
                MSVSS.FLAG_COMMENT + SRC_COMMENT};

        // Set up a VSSCheckIn task
        MSVSSCHECKIN vssCheckin = new MSVSSCHECKIN();
        vssCheckin.setProject(project);
        vssCheckin.setComment(SRC_COMMENT);
        vssCheckin.setLogin(VSS_USERNAME);
        vssCheckin.setAutoresponse("N");
        vssCheckin.setVsspath(VSS_PROJECT_PATH);
        vssCheckin.setWritable(true);

        commandline = vssCheckin.buildCmdLine();

        checkCommandLines(sTestCmdLine, commandline.getCommandline());
    }

    /**
     * Test VSSCheckIn required attributes.
     */
    public void testCheckinExceptions() {
        configureProject("src/etc/testcases/taskdefs/optional/vss/vss.xml");
        expectSpecificBuildException("vsscheckin.1", "some cause", "vsspath attribute must be set!");
    }

    /**  Tests CheckOut commandline generation.  */
    public void testCheckoutCommandLine() {
        String[] sTestCmdLine = {SS_DIR + File.separator + MSVSS.SS_EXE, MSVSS.COMMAND_CHECKOUT,
                DS_VSS_PROJECT_PATH, MSVSS.FLAG_AUTORESPONSE_DEF, MSVSS.FLAG_RECURSION,
                MSVSS.FLAG_VERSION_DATE + DATE, MSVSS.FLAG_LOGIN + VSS_USERNAME,
                FLAG_FILETIME_MODIFIED, FLAG_NO_GET};

        // Set up a VSSCheckOut task
        MSVSSCHECKOUT vssCheckout = new MSVSSCHECKOUT();
        vssCheckout.setProject(project);
        vssCheckout.setLogin(VSS_USERNAME);
        vssCheckout.setVsspath(DS_VSS_PROJECT_PATH);
        vssCheckout.setRecursive(true);
        vssCheckout.setDate(DATE);
        vssCheckout.setLabel(SRC_LABEL);
        vssCheckout.setSsdir(SS_DIR);
        MSVSS.CurrentModUpdated cmu = new MSVSS.CurrentModUpdated();
        cmu.setValue(TIME_MODIFIED);
        vssCheckout.setFileTimeStamp(cmu);
        vssCheckout.setGetLocalCopy(false);

        commandline = vssCheckout.buildCmdLine();

        checkCommandLines(sTestCmdLine, commandline.getCommandline());
    }

    /**
     * Test VSSCheckout required attributes.
     */
    public void testCheckoutExceptions() {
        configureProject("src/etc/testcases/taskdefs/optional/vss/vss.xml");
        expectSpecificBuildException("vsscheckout.1", "some cause", "vsspath attribute must be set!");
        expectSpecificBuildException("vsscheckout.2", "some cause", "blah is not a legal value for this attribute");
    }

    /**  Tests Add commandline generation.  */
    public void testAddCommandLine() {
        String[] sTestCmdLine = {SS_DIR + File.separator + MSVSS.SS_EXE, MSVSS.COMMAND_ADD,
                project.getBaseDir().getAbsolutePath() + File.separator + LOCAL_PATH,
                MSVSS.FLAG_AUTORESPONSE_DEF, MSVSS.FLAG_RECURSION,
                MSVSS.FLAG_LOGIN + VSS_USERNAME + "," + VSS_PASSWORD, MSVSS.FLAG_COMMENT + "-"};

        // Set up a VSSAdd task
        MSVSSADD vssAdd = new MSVSSADD();
        vssAdd.setProject(project);
        vssAdd.setLogin(VSS_USERNAME + "," + VSS_PASSWORD);
        vssAdd.setVsspath(DS_VSS_PROJECT_PATH);
        vssAdd.setRecursive(true);
        vssAdd.setSsdir(SS_DIR);
        vssAdd.setWritable(false);
        vssAdd.setLocalpath(new Path(project, LOCAL_PATH));

        commandline = vssAdd.buildCmdLine();

        checkCommandLines(sTestCmdLine, commandline.getCommandline());
    }

    /**
     * Test VSSAdd required attributes.
     */
    public void testAddExceptions() {
        configureProject("src/etc/testcases/taskdefs/optional/vss/vss.xml");
        expectSpecificBuildException("vssadd.1", "some cause", "localPath attribute must be set!");
    }

    /**  Tests CP commandline generation.  */
    public void testCpCommandLine() {
        String[] sTestCmdLine = {MSVSS.SS_EXE, MSVSS.COMMAND_CP,
                DS_VSS_PROJECT_PATH, MSVSS.FLAG_AUTORESPONSE_DEF, MSVSS.FLAG_LOGIN +
                VSS_USERNAME};

        // Set up a VSSCp task
        MSVSSCP vssCp = new MSVSSCP();
        vssCp.setProject(project);
        vssCp.setLogin(VSS_USERNAME);
        vssCp.setVsspath(DS_VSS_PROJECT_PATH);

        commandline = vssCp.buildCmdLine();

        checkCommandLines(sTestCmdLine, commandline.getCommandline());
    }

    /**
     * Test VSSCP required attributes.
     */
    public void testCpExceptions() {
        configureProject("src/etc/testcases/taskdefs/optional/vss/vss.xml");
        expectSpecificBuildException("vsscp.1", "some cause", "vsspath attribute must be set!");
    }

    /**  Tests Create commandline generation.  */
    public void testCreateCommandLine() {
        String[] sTestCmdLine = { MSVSS.SS_EXE, MSVSS.COMMAND_CREATE,
                DS_VSS_PROJECT_PATH, MSVSS.FLAG_COMMENT + SRC_COMMENT, MSVSS.FLAG_AUTORESPONSE_NO,
                MSVSS.FLAG_QUIET, MSVSS.FLAG_LOGIN + VSS_USERNAME};

        // Set up a VSSCreate task
        MSVSSCREATE vssCreate = new MSVSSCREATE();
        vssCreate.setProject(project);
        vssCreate.setComment(SRC_COMMENT);
        vssCreate.setLogin(VSS_USERNAME);
        vssCreate.setVsspath(DS_VSS_PROJECT_PATH);
        vssCreate.setFailOnError(true);
        vssCreate.setAutoresponse("N");
        vssCreate.setQuiet(true);

        commandline = vssCreate.buildCmdLine();

        checkCommandLines(sTestCmdLine, commandline.getCommandline());
    }

    /**
     * Test VSSCreate required attributes.
     */
    public void testCreateExceptions() {
        configureProject("src/etc/testcases/taskdefs/optional/vss/vss.xml");
        expectSpecificBuildException("vsscreate.1", "some cause", "vsspath attribute must be set!");
    }

    /**
     * Iterate through the generated command line comparing it to reference one.
     * @param sTestCmdLine          The reference command line;
     * @param sGeneratedCmdLine     The generated command line;
     */
    private void checkCommandLines(String[] sTestCmdLine, String[] sGeneratedCmdLine) {
        int testLength = sTestCmdLine.length;
        int genLength = sGeneratedCmdLine.length;

        int genIndex = 0;
        int testIndex = 0;

        while (testIndex < testLength) {
            try {
                if (sGeneratedCmdLine[genIndex] == "") {
                    genIndex++;
                    continue;
                }
                assertEquals("arg # " + testIndex,
                        sTestCmdLine[testIndex],
                        sGeneratedCmdLine[genIndex]);
                testIndex++;
                genIndex++;
            } catch (ArrayIndexOutOfBoundsException aioob) {
                fail("missing arg " + sTestCmdLine[testIndex]);
            }
        }

        // Count the number of empty strings
        int cnt = 0;
        for (int i = 0; i < genLength; i++) {
            if (sGeneratedCmdLine[i] == "") {
                cnt++;
            }
        }
        if (genLength - cnt > sTestCmdLine.length) {
            // We have extra elements
            fail("extra args");
        }
    }
}
