/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs;

import junit.framework.*;
import java.io.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.util.FileUtils;

/**
 * stress out java task
 * @author steve loughran
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 * @author <a href="mailto:donal@savvion.com">Donal Quinlan</a>
 * @author <a href="mailto:martijn@kruithof.xs4all.nl">Martijn Kruithof</a>
 * */
public class JavaTest extends BuildFileTest {

    private static final int TIME_TO_WAIT = 4;
    // wait 1 second extra to allow for java to start ...
    // this time was OK on a Win NT machine and on nagoya
    private static final int SECURITY_MARGIN = 1000;

    private boolean runFatalTests=false;

    public JavaTest(String name) {
        super(name);
    }

    /**
     * configure the project.
     * if the property junit.run.fatal.tests is set we run
     * the fatal tests
     */
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/java.xml");

        //final String propname="tests-classpath.value";
        //String testClasspath=System.getProperty(propname);
        //System.out.println("Test cp="+testClasspath);
        String propname="tests-classpath.value";
        String runFatal=System.getProperty("junit.run.fatal.tests");
        if(runFatal!=null)
            runFatalTests=true;
    }

    public void tearDown() {
        // remove log file from testSpawn
        project.executeTarget("cleanup");
    }

    public void testNoJarNoClassname(){
        expectBuildExceptionContaining("testNoJarNoClassname",
            "parameter validation",
            "Classname must not be null.");
    }

    public void testJarNoFork() {
        expectBuildExceptionContaining("testJarNoFork",
            "parameter validation",
            "Cannot execute a jar in non-forked mode. "
                + "Please set fork='true'. ");
    }

    public void testJarAndClassName() {
        expectBuildException("testJarAndClassName",
            "Should not be able to set both classname AND jar");
    }


    public void testClassnameAndJar() {
        expectBuildException("testClassnameAndJar",
            "Should not be able to set both classname AND jar");
    }

    public void testRun() {
        executeTarget("testRun");
    }



    /** this test fails but we ignore the return value;
     *  we verify that failure only matters when failonerror is set
     */
    public void testRunFail() {
        if(runFatalTests) {
            executeTarget("testRunFail");
        }
    }

    public void testRunFailFoe() {
        if(runFatalTests) {
            expectBuildExceptionContaining("testRunFailFoe",
                "java failures being propagated",
                "Java returned:");
        }
}

    public void testRunFailFoeFork() {
        expectBuildExceptionContaining("testRunFailFoeFork",
            "java failures being propagated",
            "Java returned:");
    }

    public void testExcepting() {
        expectLogContaining("testExcepting",
                            "Exception raised inside called program");
    }

    public void testExceptingFork() {
        expectLogContaining("testExceptingFork",
                            "Java Result:");
    }

    public void testExceptingFoe() {
        expectBuildExceptionContaining("testExceptingFoe",
            "passes exception through",
            "Exception raised inside called program");
    }

    public void testExceptingFoeFork() {
        expectBuildExceptionContaining("testExceptingFoeFork",
            "exceptions turned into error codes",
            "Java returned:");
    }

    public void testResultPropertyZero() {
        executeTarget("testResultPropertyZero");
        assertEquals("0",project.getProperty("exitcode"));
    }

    public void testResultPropertyNonZero() {
        executeTarget("testResultPropertyNonZero");
        assertEquals("2",project.getProperty("exitcode"));
    }

    public void testResultPropertyZeroNoFork() {
        executeTarget("testResultPropertyZeroNoFork");
        assertEquals("0",project.getProperty("exitcode"));
    }

    public void testResultPropertyNonZeroNoFork() {
        executeTarget("testResultPropertyNonZeroNoFork");
         assertEquals("-1",project.getProperty("exitcode"));
     }

    public void testRunFailWithFailOnError() {
        expectBuildExceptionContaining("testRunFailWithFailOnError",
            "non zero return code",
            "Java returned:");
    }

    public void testRunSuccessWithFailOnError() {
        executeTarget("testRunSuccessWithFailOnError");
    }

    public void testSpawn() {
        FileUtils fileutils  = FileUtils.newFileUtils();
        File logFile = fileutils.createTempFile("spawn","log", project.getBaseDir());
        // this is guaranteed by FileUtils#createTempFile
        assertTrue("log file not existing", !logFile.exists());
        project.setProperty("logFile", logFile.getAbsolutePath());
        project.setProperty("timeToWait", Long.toString(TIME_TO_WAIT));
        project.executeTarget("testSpawn");
        try {
            Thread.sleep(TIME_TO_WAIT * 1000 + SECURITY_MARGIN);
        } catch (Exception ex) {
            System.out.println("my sleep was interrupted");
        }
        // let's be nice with the next generation of developers
        if (!logFile.exists()) {
            System.out.println("suggestion: increase the constant"
            + " SECURITY_MARGIN to give more time for java to start.");
        }
        assertTrue("log file exists", logFile.exists());
    }

    /**
     * entry point class with no dependencies other
     * than normal JRE runtime
     */
    public static class EntryPoint {

    /**
     * this entry point is used by the java.xml tests to
     * generate failure strings to handle
     * argv[0] = exit code (optional)
     * argv[1] = string to print to System.out (optional)
     * argv[1] = string to print to System.err (optional)
     */
        public static void main(String[] argv) {
            int exitCode=0;
            if(argv.length>0) {
                try {
                    exitCode=Integer.parseInt(argv[0]);
                } catch(NumberFormatException nfe) {
                    exitCode=-1;
                }
            }
            if(argv.length>1) {
                System.out.println(argv[1]);
            }
            if(argv.length>2) {
                System.err.println(argv[2]);
            }
            if(exitCode!=0) {
                System.exit(exitCode);
            }
        }
    }

    /**
     * entry point class with no dependencies other
     * than normal JRE runtime
     */
    public static class ExceptingEntryPoint {

        /**
         * throw a run time exception which does not need
         * to be in the signature of the entry point
         */
        public static void main(String[] argv) {
            throw new NullPointerException("Exception raised inside called program");
        }
    }
    /**
     * test class for spawn
     */
    public static class SpawnEntryPoint {
        public static void main(String [] argv) {
            int sleepTime = 10;
            String logFile = "spawn.log";
            if (argv.length >= 1) {
                sleepTime = Integer.parseInt(argv[0]);
            }
            if (argv.length >= 2)
            {
                logFile = argv[1];
            }
            OutputStreamWriter out = null;
            try {
                Thread.sleep(sleepTime * 1000);
            } catch (InterruptedException ex) {
                System.out.println("my sleep was interrupted");
            }

            try {
                File dest = new File(logFile);
                FileOutputStream fos = new FileOutputStream(dest);
                out = new OutputStreamWriter(fos);
                out.write("bye bye\n");
            } catch (Exception ex) {}
            finally {
                try {out.close();} catch (IOException ioe) {}}

        }
    }
}
