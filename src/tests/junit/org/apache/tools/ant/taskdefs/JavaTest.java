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

package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.TeeOutputStream;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * stress out java task
 * */
public class JavaTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    private static final int TIME_TO_WAIT = 1;
    // wait 1 second extra to allow for java to start ...
    // this time was OK on a Win NT machine and on nagoya
    private static final int SECURITY_MARGIN = 2000;

    /** Utilities used for file operations */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private boolean runFatalTests=false;


    /**
     * configure the project.
     * if the property junit.run.fatal.tests is set we run
     * the fatal tests
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/java.xml");
        buildRule.executeTarget("setUp");

        //final String propname="tests-classpath.value";
        //String testClasspath=System.getProperty(propname);
        //System.out.println("Test cp="+testClasspath);
        String runFatal=System.getProperty("junit.run.fatal.tests");
        if(runFatal!=null)
            runFatalTests=true;
    }

    @Test
    public void testNoJarNoClassname(){
        try {
            buildRule.executeTarget("testNoJarNoClassname");
            fail("Build exception should have been thrown - parameter validation");
        } catch (BuildException ex) {
            assertContains("Classname must not be null.", ex.getMessage());
        }
    }

    @Test
    public void testJarNoFork() {
        try {
            buildRule.executeTarget("testJarNoFork");
            fail("Build exception should have been thrown - parameter validation");
        } catch (BuildException ex) {
            assertContains("Cannot execute a jar in non-forked mode. Please set fork='true'. ", ex.getMessage());
        }
    }

    @Test
    public void testJarAndClassName() {
        try {
            buildRule.executeTarget("testJarAndClassName");
            fail("Build exception should have been thrown - both classname and JAR are not allowed");
        } catch (BuildException ex) {
            assertEquals("Cannot use 'jar' and 'classname' attributes in same command", ex.getMessage());
        }
    }

    @Test
    public void testClassnameAndJar() {
        try {
            buildRule.executeTarget("testClassnameAndJar");
            fail("Build exception should have been thrown - both classname and JAR are not allowed");
        } catch (BuildException ex) {
            assertEquals("Cannot use 'jar' and 'classname' attributes in same command.", ex.getMessage());
        }
    }

    @Test
    public void testRun() {
        buildRule.executeTarget("testRun");
    }



    /** this test fails but we ignore the return value;
     *  we verify that failure only matters when failonerror is set
     */
    @Test
    public void testRunFail() {
        Assume.assumeTrue("Fatal tests have not been set to run", runFatalTests);
        buildRule.executeTarget("testRunFail");
    }

    @Test
    public void testRunFailFoe() {
        Assume.assumeTrue("Fatal tests have not been set to run", runFatalTests);
        try {
            buildRule.executeTarget("testRunFailFoe");
            fail("Build exception should have been thrown - " + "java failures being propagated");
        } catch (BuildException ex) {
            assertContains("Java returned:", ex.getMessage());
        }
    }

    @Test
    public void testRunFailFoeFork() {
        try {
            buildRule.executeTarget("testRunFailFoeFork");
            fail("Build exception should have been thrown - " + "java failures being propagated");
        } catch (BuildException ex) {
            assertContains("Java returned:", ex.getMessage());
        }
    }

    @Test
    public void testExcepting() {
        buildRule.executeTarget("testExcepting");
        assertContains("Exception raised inside called program", buildRule.getLog());
    }

    @Test
    public void testExceptingFork() {
        buildRule.executeTarget("testExceptingFork");
        assertContains("Java Result:", buildRule.getLog());
    }

    @Test
    public void testExceptingFoe() {
        try {
            buildRule.executeTarget("testExceptingFoe");
            fail("Build exception should have been thrown - " + "passes exception through");
        } catch (BuildException ex) {
            assertContains("Exception raised inside called program", ex.getMessage());
        }
    }

    @Test
    public void testExceptingFoeFork() {
        try {
            buildRule.executeTarget("testExceptingFoeFork");
            fail("Build exception should have been thrown - " + "exceptions turned into error codes");
        } catch (BuildException ex) {
            assertContains("Java returned:", ex.getMessage());
        }
    }

    @Test
    public void testResultPropertyZero() {
        buildRule.executeTarget("testResultPropertyZero");
        assertEquals("0", buildRule.getProject().getProperty("exitcode"));
    }

    @Test
    public void testResultPropertyNonZero() {
        buildRule.executeTarget("testResultPropertyNonZero");
        assertEquals("2", buildRule.getProject().getProperty("exitcode"));
    }

    @Test
    public void testResultPropertyZeroNoFork() {
        buildRule.executeTarget("testResultPropertyZeroNoFork");
        assertEquals("0", buildRule.getProject().getProperty("exitcode"));
    }

    @Test
    public void testResultPropertyNonZeroNoFork() {
        buildRule.executeTarget("testResultPropertyNonZeroNoFork");
         assertEquals("-1", buildRule.getProject().getProperty("exitcode"));
     }

    @Test
    public void testRunFailWithFailOnError() {
        try {
            buildRule.executeTarget("testRunFailWithFailOnError");
            fail("Build exception should have been thrown - " + "non zero return code");
        } catch (BuildException ex) {
            assertContains("Java returned:", ex.getMessage());
        }
    }

    @Test
    public void testRunSuccessWithFailOnError() {
        buildRule.executeTarget("testRunSuccessWithFailOnError");
    }

    @Test
    public void testSpawn() throws InterruptedException {
        File logFile = FILE_UTILS.createTempFile("spawn", "log",
                new File(buildRule.getProject().getProperty("output")), false, false);
        // this is guaranteed by FileUtils#createTempFile
        assertTrue("log file not existing", !logFile.exists());
        buildRule.getProject().setProperty("logFile", logFile.getAbsolutePath());
        buildRule.getProject().setProperty("timeToWait", Long.toString(TIME_TO_WAIT));
        buildRule.getProject().executeTarget("testSpawn");

        Thread.sleep(TIME_TO_WAIT * 1000 + SECURITY_MARGIN);


        // let's be nice with the next generation of developers
        if (!logFile.exists()) {
            System.out.println("suggestion: increase the constant"
            + " SECURITY_MARGIN to give more time for java to start.");
        }
        assertTrue("log file exists", logFile.exists());
    }

    @Test
    public void testRedirect1() {
        buildRule.executeTarget("redirect1");
    }

    @Test
    public void testRedirect2() {
        buildRule.executeTarget("redirect2");
    }

    @Test
    public void testRedirect3() {
        buildRule.executeTarget("redirect3");
    }

    @Test
    public void testRedirector1() {
        buildRule.executeTarget("redirector1");
    }

    @Test
    public void testRedirector2() {
        buildRule.executeTarget("redirector2");
    }

    @Test
    public void testReleasedInput() throws Exception {
        PipedOutputStream out = new PipedOutputStream();
        final PipedInputStream in = new PipedInputStream(out);
        buildRule.getProject().setInputHandler(new DefaultInputHandler() {
            protected InputStream getInputStream() {
                return in;
            }
        });
        buildRule.getProject().setDefaultInputStream(in);

        Java java = new Java();
        java.setProject(buildRule.getProject());
        java.setClassname("org.apache.tools.ant.Main");
        java.setArgs("-version");
        java.setFork(true);
        // note: due to the missing classpath it will fail, but the input stream
        // reader will be read
        java.execute();

        Thread inputThread = new Thread(new Runnable() {
            public void run() {
                Input input = new Input();
                input.setProject(buildRule.getProject());
                input.setAddproperty("input.value");
                input.execute();
            }
        });
        inputThread.start();

        // wait a little bit for the task to wait for input
        Thread.sleep(100);

        // write some stuff in the input stream to be catched by the input task
        out.write("foo\n".getBytes());
        out.flush();
        try {
            out.write("bar\n".getBytes());
            out.flush();
        } catch (IOException x) {
            // "Pipe closed" on XP; ignore?
        }

        inputThread.join(2000);

        assertEquals("foo", buildRule.getProject().getProperty("input.value"));
    }

    @Test
    public void testFlushedInput() throws Exception {
        final PipedOutputStream out = new PipedOutputStream();
        final PipedInputStream in = new PipedInputStream(out);
        buildRule.getProject().setInputHandler(new DefaultInputHandler() {
            protected InputStream getInputStream() {
                return in;
            }
        });
        buildRule.getProject().setDefaultInputStream(in);

        final boolean[] timeout = new boolean[1];
        timeout[0] = false;

        Thread writingThread = new Thread(new Runnable() {
            public void run() {
                try {
                    // wait a little bit to have the target executed
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new AssumptionViolatedException("Thread interrupted", e);
                }
                try {
                    out.write("foo-FlushedInput\n".getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        writingThread.setDaemon(true);

        writingThread.start();
        buildRule.executeTarget("flushedInput");
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
        public static void main(String [] argv) throws InterruptedException {
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
            Thread.sleep(sleepTime * 1000);

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

    /**
     * entry point class to pipe System.in to the specified stream:
     * "out", "err", or "both".  If none specified, swallow the input.
     */
    public static class PipeEntryPoint {

        /**
         * pipe input to specified output
         */
        public static void main(String[] args) throws InterruptedException {
            OutputStream os = null;
            if (args.length > 0) {
                if ("out".equalsIgnoreCase(args[0])) {
                    os = System.out;
                } else if ("err".equalsIgnoreCase(args[0])) {
                    os = System.err;
                } else if ("both".equalsIgnoreCase(args[0])) {
                    os = new TeeOutputStream(System.out, System.err);
                }
            }
            if (os != null) {
                Thread t = new Thread(new StreamPumper(System.in, os, true));
                t.setName("PipeEntryPoint " + args[0]);
                t.start();
                t.join();
            }
        }
    }

    public static class ReadPoint {
        public static void main(String[] args) throws IOException {
            String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
            System.out.println(line);
        }
    }

}
