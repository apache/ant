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
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.taskdefs.condition.JavaVersion;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.TeeOutputStream;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * stress out java task
 * */
public class JavaTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final int TIME_TO_WAIT = 1;
    // wait 1 second extra to allow for java to start ...
    // this time was OK on a Win NT machine and on nagoya
    private static final int SECURITY_MARGIN = 2000;

    /** Utilities used for file operations */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private boolean runFatalTests = false;


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
        //System.out.println("Test cp=" + testClasspath);
        String runFatal = System.getProperty("junit.run.fatal.tests");
        if (runFatal != null) {
            runFatalTests = true;
        }
    }

    @Test
    public void testNoJarNoClassname() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Classname must not be null.");
        buildRule.executeTarget("testNoJarNoClassname");
    }

    @Test
    public void testJarNoFork() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Cannot execute a jar in non-forked mode. Please set fork='true'. ");
        buildRule.executeTarget("testJarNoFork");
    }

    @Test
    public void testJarAndClassName() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Cannot use combination of ");
        buildRule.executeTarget("testJarAndClassName");
    }

    @Test
    public void testClassnameAndJar() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Cannot use combination of ");
        buildRule.executeTarget("testClassnameAndJar");
    }

    @Test
    public void testJarAndModule() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Cannot use combination of ");
        buildRule.executeTarget("testJarAndModule");
    }

    @Test
    public void testModuleAndJar() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Cannot use combination of ");
        buildRule.executeTarget("testModuleAndJar");
    }

    @Test
    public void testClassnameAndModule() {
        buildRule.executeTarget("testClassnameAndModule");
    }

    @Test
    public void testModuleAndClassname() {
        buildRule.executeTarget("testModuleAndClassname");
    }

    @Test
    public void testModule() {
        buildRule.executeTarget("testModule");
    }

    @Test
    public void testModuleCommandLine() {
        final String moduleName = "TestModule"; //NOI18N
        final String arg = "appArg";    //NOI18N
        final Java java = new Java();
        java.setFork(true);
        java.setModule(moduleName);
        java.setJvmargs("-Xmx128M");
        java.setArgs(arg);
        final String[] cmdLine = java.getCommandLine().getCommandline();
        assertNotNull("Has command line.", cmdLine);
        assertEquals("Command line should have 5 elements", 5, cmdLine.length);
        assertEquals("Last command line element should be java argument: " + arg,
                arg,
                cmdLine[cmdLine.length - 1]);
        assertEquals("The command line element at index 3 should be module name: " + moduleName,
                moduleName,
                cmdLine[cmdLine.length - 2]);
        assertEquals("The command line element at index 2 should be -m",
                "-m",
                cmdLine[cmdLine.length - 3]);
    }

    @Test
    public void testModuleAndClassnameCommandLine() {
        final String moduleName = "TestModule"; //NOI18N
        final String className = "org.apache.Test"; //NOI18N
        final String moduleClassPair = String.format("%s/%s", moduleName, className);
        final String arg = "appArg";    //NOI18N
        final Java java = new Java();
        java.setFork(true);
        java.setModule(moduleName);
        java.setClassname(className);
        java.setJvmargs("-Xmx128M");    //NOI18N
        java.setArgs(arg);
        final String[] cmdLine = java.getCommandLine().getCommandline();
        assertNotNull("Has command line.", cmdLine);
        assertEquals("Command line should have 5 elements", 5, cmdLine.length);
        assertEquals("Last command line element should be java argument: " + arg,
                arg,
                cmdLine[cmdLine.length - 1]);
        assertEquals("The command line element at index 3 should be module class pair: " + moduleClassPair,
                moduleClassPair,
                cmdLine[cmdLine.length - 2]);
        assertEquals("The command line element at index 2 should be -m",
                "-m",
                cmdLine[cmdLine.length - 3]);
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
        assumeTrue("Fatal tests have not been set to run", runFatalTests);
        buildRule.executeTarget("testRunFail");
    }

    @Test
    public void testRunFailFoe() {
        assumeTrue("Fatal tests have not been set to run", runFatalTests);
        thrown.expect(BuildException.class);
        thrown.expectMessage("Java returned:");
        buildRule.executeTarget("testRunFailFoe");
    }

    @Test
    public void testRunFailFoeFork() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Java returned:");
        buildRule.executeTarget("testRunFailFoeFork");
    }

    @Test
    public void testExcepting() {
        buildRule.executeTarget("testExcepting");
        assertThat(buildRule.getLog(), containsString("Exception raised inside called program"));
    }

    @Test
    public void testExceptingFork() {
        buildRule.executeTarget("testExceptingFork");
        assertThat(buildRule.getLog(), containsString("Java Result:"));
    }

    @Test
    public void testExceptingFoe() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Exception raised inside called program");
        buildRule.executeTarget("testExceptingFoe");
    }

    @Test
    public void testExceptingFoeFork() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Java returned:");
        buildRule.executeTarget("testExceptingFoeFork");
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
        thrown.expect(BuildException.class);
        thrown.expectMessage("Java returned:");
        buildRule.executeTarget("testRunFailWithFailOnError");
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
        assertFalse("log file not existing", logFile.exists());
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
        java.setClassname(MagicNames.ANT_CORE_PACKAGE + ".Main");
        java.setArgs("-version");
        java.setFork(true);
        // note: due to the missing classpath it will fail, but the input stream
        // reader will be read
        java.execute();

        Thread inputThread = new Thread(() -> {
            Input input = new Input();
            input.setProject(buildRule.getProject());
            input.setAddproperty("input.value");
            input.execute();
        });
        inputThread.start();

        // wait a little bit for the task to wait for input
        Thread.sleep(100);

        // write some stuff in the input stream to be caught by the input task
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

        Thread writingThread = new Thread(() -> {
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
        });
        writingThread.setDaemon(true);

        writingThread.start();
        buildRule.executeTarget("flushedInput");
    }

    /**
     * Test that the Java single file source program feature introduced in Java 11 works fine
     *
     * @throws Exception
     */
    @Test
    public void testSimpleSourceFile() {
        requireJava11();
        buildRule.executeTarget("simpleSourceFile");
    }

    /**
     * Test that the sourcefile option of the Java task can only be run when fork attribute is set
     *
     * @throws Exception
     */
    @Test
    public void testSourceFileRequiresFork() {
        requireJava11();
        thrown.expect(BuildException.class);
        thrown.expectMessage("Cannot execute sourcefile in non-forked mode. Please set fork='true'");
        buildRule.executeTarget("sourceFileRequiresFork");
    }

    /**
     * Tests that the sourcefile attribute and the classname attribute of the Java task cannot be used
     * together
     *
     * @throws Exception
     */
    @Test
    public void testSourceFileCantUseClassname() {
        requireJava11();
        thrown.expect(BuildException.class);
        thrown.expectMessage("Cannot use 'sourcefile' in combination with");
        buildRule.executeTarget("sourceFileCantUseClassname");
    }

    /**
     * Tests that the sourcefile attribute and the jar attribute of the Java task cannot be used
     * together
     *
     * @throws Exception
     */
    @Test
    public void testSourceFileCantUseJar() {
        requireJava11();
        thrown.expect(BuildException.class);
        thrown.expectMessage("Cannot use 'sourcefile' in combination with");
        buildRule.executeTarget("sourceFileCantUseJar");
    }

    /**
     * Tests that the sourcefile attribute and the module attribute of the Java task cannot be used
     * together
     *
     * @throws Exception
     */
    @Test
    public void testSourceFileCantUseModule() {
        requireJava11();
        thrown.expect(BuildException.class);
        thrown.expectMessage("Cannot use 'sourcefile' in combination with");
        buildRule.executeTarget("sourceFileCantUseModule");
    }

    private static void requireJava11() {
        final JavaVersion javaVersion = new JavaVersion();
        javaVersion.setAtLeast("11");
        Assume.assumeTrue("Skipping test which requires a minimum of Java 11 runtime", javaVersion.eval());
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
            int exitCode = 0;
            if (argv.length > 0) {
                try {
                    exitCode = Integer.parseInt(argv[0]);
                } catch (NumberFormatException nfe) {
                    exitCode = -1;
                }
            }
            if (argv.length > 1) {
                System.out.println(argv[1]);
            }
            if (argv.length > 2) {
                System.err.println(argv[2]);
            }
            if (exitCode != 0) {
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
        public static void main(String[] argv) throws InterruptedException {
            int sleepTime = 10;
            String logFile = "spawn.log";
            if (argv.length >= 1) {
                sleepTime = Integer.parseInt(argv[0]);
            }
            if (argv.length >= 2) {
                logFile = argv[1];
            }
            Thread.sleep(sleepTime * 1000);

            File dest = new File(logFile);
            try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(dest))) {
                out.write("bye bye\n");
            } catch (Exception ex) {
            }
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
