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
package org.apache.tools.ant.taskdefs.optional.sos;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.taskdefs.optional.sos.*;
import java.io.File;

/**
 * Basic testcase to ensure that command line generation is ok
 * @author <a href="mailto:jesse@cryptocard.com">Jesse Stockall</a>
 */
public class SOSTest extends TestCase {

    private SOSGet sosGet;
    private SOSCheckin sosCheckin;
    private SOSCheckout sosCheckout;
    private SOSLabel sosLabel;
    private Project project;
    private Commandline commandline;
    private FileUtils fileUtils;

    private static final String VSS_SERVER_PATH = "\\\\server\\vss\\srcsafe.ini";
    private static final String VSS_PROJECT_PATH = "/SourceRoot/Project";
    private static final String SOS_SERVER_PATH = "192.168.0.1:8888";
    private static final String SOS_USERNAME = "ant";
    private static final String SOS_PASSWORD = "rocks";
    private static final String LOCAL_PATH = "testdir";
    private static final String SRC_FILE = "Class1.java";
    private static final String SRC_LABEL = "label1";
    private static final String SRC_COMMENT = "I fixed a bug";
    private static final String SOS_HOME = "/home/user/.sos";
    private static final String VERSION = "007";

    public SOSTest(String s) {
        super(s);
    }

    protected void setUp() throws Exception {
        sosGet = new SOSGet();
        sosCheckin = new SOSCheckin();
        sosCheckout = new SOSCheckout();
        sosLabel = new SOSLabel();
        project = new Project();
        project.setBasedir(".");
        fileUtils = FileUtils.newFileUtils();
    }

    protected void tearDown() throws Exception {
        File file = new File(project.getBaseDir(), LOCAL_PATH);
        if (file.exists()) {
            file.delete();
        }
	}

    /**
     * Test SOSGetFile flags & commandline generation
     */
    public void testGetFileFlags() {
        String[] sTestCmdLine = { "soscmd", "-command", "GetFile", "-file",
            SRC_FILE, "-revision", "007", "-server", SOS_SERVER_PATH, "-name",
            SOS_USERNAME, "-password", SOS_PASSWORD, "-database", VSS_SERVER_PATH,
            "-project", "$"+VSS_PROJECT_PATH, "-verbose", "-nocompress",
            "-nocache", "-workdir", project.getBaseDir().getAbsolutePath()
			+ File.separator + LOCAL_PATH };

        Path path = new Path(project, LOCAL_PATH);

        // Set up a SOSGet task
        sosGet.setProject(project);
        sosGet.setVssServerPath(VSS_SERVER_PATH);
        sosGet.setSosServerPath(SOS_SERVER_PATH);
        sosGet.setProjectPath(VSS_PROJECT_PATH);
        sosGet.setFile(SRC_FILE);
        sosGet.setUsername(SOS_USERNAME);
        sosGet.setPassword(SOS_PASSWORD);
        sosGet.setVersion(VERSION);
        sosGet.setLocalPath(path);
        sosGet.setNoCache(true);
        sosGet.setNoCompress(true);
        sosGet.setVerbose(true);
        sosGet.setRecursive(true);

        commandline = sosGet.buildCmdLine();
        String[] sGeneratedCmdLine = commandline.getCommandline();
        int i = 0;
        while (i < sTestCmdLine.length) {
            try {
                assertEquals("GetFile arg # " + String.valueOf(i),
                                sTestCmdLine[i],
                                sGeneratedCmdLine[i]);
                i++;
            } catch (ArrayIndexOutOfBoundsException aioob) {
               fail("GetFile missing arg");
            }

        }
        if (sGeneratedCmdLine.length > sTestCmdLine.length) {
            // We have extra elements
            fail("GetFile extra args");
        }
    }

    /**
     * Test SOSGetProject flags & commandline generation
     */
    public void testGetProjectFlags() {
        String[] sTestCmdLine = { "soscmd", "-command", "GetProject", "-recursive",
            "-label", SRC_LABEL, "-server", SOS_SERVER_PATH, "-name", SOS_USERNAME,
            "-password", "", "-database", VSS_SERVER_PATH , "-project",
            "$"+VSS_PROJECT_PATH, "", "", "-soshome", SOS_HOME, "-workdir",
            project.getBaseDir().getAbsolutePath() };
        // Set up a SOSGet task
        sosGet.setProject(project);
        sosGet.setVssServerPath(VSS_SERVER_PATH);
        sosGet.setSosServerPath(SOS_SERVER_PATH);
        sosGet.setProjectPath(VSS_PROJECT_PATH);
        sosGet.setLabel(SRC_LABEL);
        sosGet.setUsername(SOS_USERNAME);
        sosGet.setSosHome(SOS_HOME);
        sosGet.setNoCache(true);
        sosGet.setNoCompress(false);
        sosGet.setVerbose(false);
        sosGet.setRecursive(true);

        commandline = sosGet.buildCmdLine();
        String[] sGeneratedCmdLine = commandline.getCommandline();

        int i = 0;
        while (i < sTestCmdLine.length) {
            try {
                assertEquals("GetProject arg # " + String.valueOf(i),
                                sTestCmdLine[i],
                                sGeneratedCmdLine[i]);
                i++;
            } catch (ArrayIndexOutOfBoundsException aioob) {
                fail("GetProject missing arg");
            }

        }
        if (sGeneratedCmdLine.length > sTestCmdLine.length) {
            // We have extra elements
            fail("GetProject extra args");
        }
    }

    /**
     * Test SOSGet required attributes 1 by 1
     */
    public void testGetExceptions() {
        boolean buildEx = false;

        // Set up a SOSGet task
        sosGet.setProject(project);
        // No options set - SosServerPath should fail
        try {
            commandline = sosGet.buildCmdLine();
        } catch (BuildException be) {
            if (be.getMessage().compareTo("sosserverpath attribute must be set!") == 0) {
                buildEx = true;
            }
        }
        assertTrue("GetException SosServerPath", buildEx);
        buildEx = false;

        // Set SosServerPath - Username should fail
        sosGet.setSosServerPath(SOS_SERVER_PATH);
        try {
            commandline = sosGet.buildCmdLine();
        } catch (BuildException be) {
            if (be.getMessage().compareTo("username attribute must be set!") == 0) {
                buildEx = true;
            }
        }
        assertTrue("GetException Username", buildEx);
        buildEx = false;

        // Set Username - VssServerPath should fail
        sosGet.setUsername(SOS_USERNAME);
        try {
            commandline = sosGet.buildCmdLine();
        } catch (BuildException be) {
            if (be.getMessage().compareTo("vssserverpath attribute must be set!") == 0) {
                buildEx = true;
            }
        }
        assertTrue("GetException VssServerPath", buildEx);
        buildEx = false;

        // Set VssServerPath - ProjectPath should fail
        sosGet.setVssServerPath(VSS_SERVER_PATH);
        try {
            commandline = sosGet.buildCmdLine();
        } catch (BuildException be) {
            if (be.getMessage().compareTo("projectpath attribute must be set!") == 0) {
                buildEx = true;
            }
        }
        assertTrue("GetException ProjectPath", buildEx);

        // Set ProjectPath - All required options set
        sosGet.setProjectPath(VSS_PROJECT_PATH);
        try {
            commandline = sosGet.buildCmdLine();
            buildEx = true;
        } catch (BuildException be) {
            buildEx = false;
        }
        assertTrue("GetException All required options set", buildEx);
    }

    /**
     * Test CheckInFile option flags
     */
    public void testCheckinFileFlags() {
        String[] sTestCmdLine = { "soscmd", "-command", "CheckInFile", "-file",
            SRC_FILE, "-server", SOS_SERVER_PATH, "-name", SOS_USERNAME,
            "-password", SOS_PASSWORD, "-database", VSS_SERVER_PATH, "-project",
            "$"+VSS_PROJECT_PATH, "-verbose", "-nocompress", "-nocache",
            "-workdir", project.getBaseDir().getAbsolutePath() + File.separator
			+ LOCAL_PATH, "-log", SRC_COMMENT };

        Path path = new Path(project, LOCAL_PATH);

        // Set up a SOSCheckin task
        sosCheckin.setProject(project);
        sosCheckin.setVssServerPath(VSS_SERVER_PATH);
        sosCheckin.setSosServerPath(SOS_SERVER_PATH);
        sosCheckin.setProjectPath(VSS_PROJECT_PATH);
        sosCheckin.setFile(SRC_FILE);
        sosCheckin.setComment(SRC_COMMENT);
        sosCheckin.setUsername(SOS_USERNAME);
        sosCheckin.setPassword(SOS_PASSWORD);
        sosCheckin.setLocalPath(path);
        sosCheckin.setNoCache(true);
        sosCheckin.setNoCompress(true);
        sosCheckin.setVerbose(true);
        sosCheckin.setRecursive(true);

        commandline = sosCheckin.buildCmdLine();
        String[] sGeneratedCmdLine = commandline.getCommandline();

        int i = 0;
        while (i < sTestCmdLine.length) {
            try {
                assertEquals("CheckInFile arg # " + String.valueOf(i),
                                sTestCmdLine[i],
                                sGeneratedCmdLine[i]);
                i++;
            } catch (ArrayIndexOutOfBoundsException aioob) {
                fail("CheckInFile missing arg");
            }

        }
        if (sGeneratedCmdLine.length > sTestCmdLine.length) {
            // We have extra elements
            fail("CheckInFile extra args");
        }
    }

    /**
     * Test CheckInProject option flags
     */
    public void testCheckinProjectFlags() {
        String[] sTestCmdLine = { "soscmd", "-command", "CheckInProject",
            "-recursive", "-server", SOS_SERVER_PATH, "-name", SOS_USERNAME,
            "-password", "", "-database", VSS_SERVER_PATH , "-project",
            "$"+VSS_PROJECT_PATH, "", "", "-soshome", SOS_HOME, "-workdir",
            project.getBaseDir().getAbsolutePath(), "-log", SRC_COMMENT,  };

        // Set up a SOSCheckin task
        sosCheckin.setProject(project);
        sosCheckin.setVssServerPath(VSS_SERVER_PATH);
        sosCheckin.setSosServerPath(SOS_SERVER_PATH);
        sosCheckin.setProjectPath(VSS_PROJECT_PATH);
        sosCheckin.setComment(SRC_COMMENT);
        sosCheckin.setUsername(SOS_USERNAME);
        sosCheckin.setSosHome(SOS_HOME);
        sosCheckin.setNoCache(true);
        sosCheckin.setNoCompress(false);
        sosCheckin.setVerbose(false);
        sosCheckin.setRecursive(true);

        commandline = sosCheckin.buildCmdLine();
        String[] sGeneratedCmdLine = commandline.getCommandline();

        int i = 0;
        while (i < sTestCmdLine.length) {
            try {
                assertEquals("CheckInProject arg # " + String.valueOf(i),
                                sTestCmdLine[i],
                                sGeneratedCmdLine[i]);
                i++;
            } catch (ArrayIndexOutOfBoundsException aioob) {
                fail("CheckInProject missing arg");
            }

        }
        if (sGeneratedCmdLine.length > sTestCmdLine.length) {
            // We have extra elements
            fail("CheckInProject extra args");
        }
    }

    /**
     * Test SOSCheckIn required attributes 1 by 1
     */
    public void testCheckinExceptions() {
        boolean buildEx = false;

        // Set up a sosCheckin task
        sosCheckin.setProject(project);
        // No options set - SosServerPath should fail
        try {
            commandline = sosCheckin.buildCmdLine();
        } catch (BuildException be) {
            if (be.getMessage().compareTo("sosserverpath attribute must be set!") == 0) {
                buildEx = true;
            }
        }
        assertTrue("CheckinException SosServerPath", buildEx);
        buildEx = false;

        // Set SosServerPath - Username should fail
        sosCheckin.setSosServerPath(SOS_SERVER_PATH);
        try {
            commandline = sosCheckin.buildCmdLine();
        } catch (BuildException be) {
            if (be.getMessage().compareTo("username attribute must be set!") == 0) {
                buildEx = true;
            }
        }
        assertTrue("CheckinException Username", buildEx);
        buildEx = false;

        // Set Username - VssServerPath should fail
        sosCheckin.setUsername(SOS_USERNAME);
        try {
            commandline = sosCheckin.buildCmdLine();
        } catch (BuildException be) {
            if (be.getMessage().compareTo("vssserverpath attribute must be set!") == 0) {
                buildEx = true;
            }
        }
        assertTrue("CheckinException VssServerPath", buildEx);
        buildEx = false;

        // Set VssServerPath - ProjectPath should fail
        sosCheckin.setVssServerPath(VSS_SERVER_PATH);
        try {
            commandline = sosCheckin.buildCmdLine();
        } catch (BuildException be) {
            if (be.getMessage().compareTo("projectpath attribute must be set!") == 0) {
                buildEx = true;
            }
        }
        assertTrue("CheckinException ProjectPath", buildEx);

        // Set ProjectPath - All required options set
        sosCheckin.setProjectPath(VSS_PROJECT_PATH);
        try {
            commandline = sosCheckin.buildCmdLine();
            buildEx = true;
        } catch (BuildException be) {
            buildEx = false;
        }
        assertTrue("CheckinException All required options set", buildEx);
    }

    /**
     * Test CheckOutFile option flags
     */
    public void testCheckoutFileFlags() {
        String[] sTestCmdLine = { "soscmd", "-command", "CheckOutFile", "-file",
            SRC_FILE, "-server", SOS_SERVER_PATH, "-name", SOS_USERNAME,
            "-password", SOS_PASSWORD, "-database", VSS_SERVER_PATH, "-project",
            "$"+VSS_PROJECT_PATH, "-verbose", "-nocompress", "-nocache",
            "-workdir", project.getBaseDir().getAbsolutePath()
			+ File.separator + LOCAL_PATH };

        Path path = new Path(project, LOCAL_PATH);

        // Set up a SOSCheckout task
        sosCheckout.setProject(project);
        sosCheckout.setVssServerPath(VSS_SERVER_PATH);
        sosCheckout.setSosServerPath(SOS_SERVER_PATH);
        sosCheckout.setProjectPath(VSS_PROJECT_PATH);
        sosCheckout.setFile(SRC_FILE);
        sosCheckout.setUsername(SOS_USERNAME);
        sosCheckout.setPassword(SOS_PASSWORD);
        sosCheckout.setLocalPath(path);
        sosCheckout.setNoCache(true);
        sosCheckout.setNoCompress(true);
        sosCheckout.setVerbose(true);
        sosCheckout.setRecursive(true);

        commandline = sosCheckout.buildCmdLine();
        String[] sGeneratedCmdLine = commandline.getCommandline();

        int i = 0;
        while (i < sTestCmdLine.length) {
            try {
                assertEquals("CheckOutFile arg # " + String.valueOf(i),
                                sTestCmdLine[i],
                                sGeneratedCmdLine[i]);
                i++;
            } catch (ArrayIndexOutOfBoundsException aioob) {
                fail("CheckOutFile missing arg");
            }

        }
        if (sGeneratedCmdLine.length > sTestCmdLine.length) {
            // We have extra elements
            fail("CheckOutFile extra args");
        }
    }

    /**
     * Test CheckOutProject option flags
     */
    public void testCheckoutProjectFlags() {
        String[] sTestCmdLine = { "soscmd", "-command", "CheckOutProject",
            "-recursive", "-server", SOS_SERVER_PATH, "-name", SOS_USERNAME,
            "-password", "", "-database", VSS_SERVER_PATH , "-project",
            "$"+VSS_PROJECT_PATH, "", "", "-soshome", SOS_HOME, "-workdir",
            project.getBaseDir().getAbsolutePath() };

        // Set up a sosCheckout task
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
        String[] sGeneratedCmdLine = commandline.getCommandline();

        int i = 0;
        while (i < sTestCmdLine.length) {
            try {
                assertEquals("CheckOutProject arg # " + String.valueOf(i),
                                sTestCmdLine[i],
                                sGeneratedCmdLine[i]);
                i++;
            } catch (ArrayIndexOutOfBoundsException aioob) {
                fail("CheckOutProject missing arg");
            }

        }
        if (sGeneratedCmdLine.length > sTestCmdLine.length) {
            // We have extra elements
            fail("CheckOutProject extra args");
        }
    }

    /**
     * Test SOSCheckout required attributes 1 by 1
     */
    public void testCheckoutExceptions() {
        boolean buildEx = false;

        // Set up a sosCheckout task
        sosCheckout.setProject(project);
        // No options set - SosServerPath should fail
        try {
            commandline = sosCheckout.buildCmdLine();
        } catch (BuildException be) {
            if (be.getMessage().compareTo("sosserverpath attribute must be set!") == 0) {
                buildEx = true;
            }
        }
        assertTrue("CheckoutException SosServerPath", buildEx);
        buildEx = false;

        // Set SosServerPath - Username should fail
        sosCheckout.setSosServerPath(SOS_SERVER_PATH);
        try {
            commandline = sosCheckout.buildCmdLine();
        } catch (BuildException be) {
            if (be.getMessage().compareTo("username attribute must be set!") == 0) {
                buildEx = true;
            }
        }
        assertTrue("CheckoutException Username", buildEx);
        buildEx = false;

        // Set Username - VssServerPath should fail
        sosCheckout.setUsername(SOS_USERNAME);
        try {
            commandline = sosCheckout.buildCmdLine();
        } catch (BuildException be) {
            if (be.getMessage().compareTo("vssserverpath attribute must be set!") == 0) {
                buildEx = true;
            }
        }
        assertTrue("CheckoutException VssServerPath", buildEx);
        buildEx = false;

        // Set VssServerPath - ProjectPath should fail
        sosCheckout.setVssServerPath(VSS_SERVER_PATH);
        try {
            commandline = sosCheckout.buildCmdLine();
        } catch (BuildException be) {
            if (be.getMessage().compareTo("projectpath attribute must be set!") == 0) {
                buildEx = true;
            }
        }
        assertTrue("CheckoutException ProjectPath", buildEx);

        // Set ProjectPath - All required options set
        sosCheckout.setProjectPath(VSS_PROJECT_PATH);
        try {
            commandline = sosCheckout.buildCmdLine();
            buildEx = true;
        } catch (BuildException be) {
            buildEx = false;
        }
        assertTrue("CheckoutException All required options set", buildEx);
    }

    /**
     * Test Label option flags
     */
    public void testLabelFlags() {
        String[] sTestCmdLine = { "soscmd", "-command", "AddLabel", "-server",
            SOS_SERVER_PATH, "-name", SOS_USERNAME, "-password", "", "-database",
            VSS_SERVER_PATH , "-project", "$"+VSS_PROJECT_PATH, "-label",
            SRC_LABEL, "-verbose", "-log", SRC_COMMENT };

        // Set up a sosCheckout task
        sosLabel.setVssServerPath(VSS_SERVER_PATH);
        sosLabel.setSosServerPath(SOS_SERVER_PATH);
        sosLabel.setProjectPath(VSS_PROJECT_PATH);
        sosLabel.setUsername(SOS_USERNAME);
        sosLabel.setSosHome(SOS_HOME);
        sosLabel.setComment(SRC_COMMENT);
        sosLabel.setLabel(SRC_LABEL);
        sosLabel.setNoCache(true);
        sosLabel.setNoCompress(false);
        sosLabel.setVerbose(true);

        commandline = sosLabel.buildCmdLine();
        String[] sGeneratedCmdLine = commandline.getCommandline();

        int i = 0;
        while (i < sTestCmdLine.length) {
            try {
                assertEquals("AddLabel arg # " + String.valueOf(i),
                                sTestCmdLine[i],
                                sGeneratedCmdLine[i]);
                i++;
            } catch (ArrayIndexOutOfBoundsException aioob) {
                fail("AddLabel missing arg");
            }

        }
        if (sGeneratedCmdLine.length > sTestCmdLine.length) {
            // We have extra elements
            fail("AddLabel extra args");
        }
    }

    /**
     * Test SOSLabel required attributes 1 by 1
     */
    public void testLabelExceptions() {
        boolean buildEx = false;

        // Set up a sosLabel task
        sosLabel.setProject(project);
        // No options set - SosServerPath should fail
        try {
            commandline = sosLabel.buildCmdLine();
        } catch (BuildException be) {
            if (be.getMessage().compareTo("sosserverpath attribute must be set!") == 0) {
                buildEx = true;
            }
        }
        assertTrue("LabelException SosServerPath", buildEx);
        buildEx = false;

        // Set SosServerPath - Username should fail
        sosLabel.setSosServerPath(SOS_SERVER_PATH);
        try {
            commandline = sosLabel.buildCmdLine();
        } catch (BuildException be) {
            if (be.getMessage().compareTo("username attribute must be set!") == 0) {
                buildEx = true;
            }
        }
        assertTrue("LabelException Username", buildEx);
        buildEx = false;

        // Set Username - VssServerPath should fail
        sosLabel.setUsername(SOS_USERNAME);
        try {
            commandline = sosLabel.buildCmdLine();
        } catch (BuildException be) {
            if (be.getMessage().compareTo("vssserverpath attribute must be set!") == 0) {
                buildEx = true;
            }
        }
        assertTrue("LabelException VssServerPath", buildEx);
        buildEx = false;

        // Set VssServerPath - ProjectPath should fail
        sosLabel.setVssServerPath(VSS_SERVER_PATH);
        try {
            commandline = sosLabel.buildCmdLine();
        } catch (BuildException be) {
            if (be.getMessage().compareTo("projectpath attribute must be set!") == 0) {
                buildEx = true;
            }
        }
        assertTrue("LabelException ProjectPath", buildEx);

        // Set ProjectPath - Label should fail
        sosLabel.setProjectPath(VSS_PROJECT_PATH);
        try {
            commandline = sosLabel.buildCmdLine();
        } catch (BuildException be) {
            if (be.getMessage().compareTo("label attribute must be set!") == 0) {
                buildEx = true;
            }
        }
        assertTrue("LabelException Label", buildEx);

        // Set Label - All required options set
        sosLabel.setLabel(SRC_LABEL);
        try {
            commandline = sosLabel.buildCmdLine();
            buildEx = true;
        } catch (BuildException be) {
            buildEx = false;
        }
        assertTrue("LabelException All required options set", buildEx);
    }

}
