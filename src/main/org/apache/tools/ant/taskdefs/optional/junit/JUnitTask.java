/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.junit;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.taskdefs.ExecuteWatchdog;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;


import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import java.util.Enumeration;
import java.util.Vector;

/**
 * Ant task to run JUnit tests.
 *
 * <p>JUnit is a framework to create unit test. It has been initially
 * created by Erich Gamma and Kent Beck.  JUnit can be found at <a
 * href="http://www.junit.org">http://www.junit.org</a>.
 *
 * <p> To spawn a new Java VM to prevent interferences between
 * different testcases, you need to enable <code>fork</code>.
 *
 * @author Thomas Haas
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class JUnitTask extends Task {

    private CommandlineJava commandline = new CommandlineJava();
    private Vector tests = new Vector();
    private Vector batchTests = new Vector();
    private Vector formatters = new Vector();
    private File dir = null;

    private Integer timeout = null;
    private boolean summary = false;

    /**
     * Tells this task to halt when there is an error in a test.
     * this property is applied on all BatchTest (batchtest) and JUnitTest (test)
     * however it can possibly be overridden by their own properties.
     * @param   value   <tt>true</tt> if it should halt, otherwise <tt>false<tt>
     */
    public void setHaltonerror(boolean value) {
        Enumeration enum = allTests();
        while (enum.hasMoreElements()) {
            BaseTest test = (BaseTest) enum.nextElement();
            test.setHaltonerror(value);
        }
    }

    /**
     * Tells this task to halt when there is a failure in a test.
     * this property is applied on all BatchTest (batchtest) and JUnitTest (test)
     * however it can possibly be overridden by their own properties.
     * @param   value   <tt>true</tt> if it should halt, otherwise <tt>false<tt>
     */
    public void setHaltonfailure(boolean value) {
        Enumeration enum = allTests();
        while (enum.hasMoreElements()) {
            BaseTest test = (BaseTest) enum.nextElement();
            test.setHaltonfailure(value);
        }
    }

    /**
     * Tells whether a JVM should be forked for each testcase. It avoids interference
     * between testcases and possibly avoids hanging the build.
     * this property is applied on all BatchTest (batchtest) and JUnitTest (test)
     * however it can possibly be overridden by their own properties.
     * @param   value   <tt>true</tt> if a JVM should be forked, otherwise <tt>false<tt>
     * @see #setTimeout(Integer)
     * @see #haltOntimeout(boolean)
     */
    public void setFork(boolean value) {
        Enumeration enum = allTests();
        while (enum.hasMoreElements()) {
            BaseTest test = (BaseTest) enum.nextElement();
            test.setFork(value);
        }
    }

    /**
     * Tells whether the task should print a short summary of the task.
     * @param   value   <tt>true</tt> to print a summary, <tt>false</tt> otherwise.
     * @see SummaryJUnitResultFormatter
     */
    public void setPrintsummary(boolean value) {
        summary = value;
    }

    /**
     * Set the timeout value (in milliseconds). If the test is running for more than this
     * value, the test will be canceled. (works only when in 'fork' mode).
     * @param   value   the maximum time (in milliseconds) allowed before declaring the test
     *                  as 'timed-out'
     * @see #setFork(boolean)
     * @see #haltOnTimeout(boolean)
     */
    public void setTimeout(Integer value) {
        timeout = value;
    }

    /**
     * Set the maximum memory to be used by all forked JVMs.
     * @param   max     the value as defined by <tt>-mx</tt> or <tt>-Xmx</tt>
     *                  in the java command line options.
     */
    public void setMaxmemory(String max) {
        if (Project.getJavaVersion().startsWith("1.1")) {
            createJvmarg().setValue("-mx"+max);
        } else {
            createJvmarg().setValue("-Xmx"+max);
        }
    }

    /**
     * Set a new VM to execute the testcase. Default is <tt>java</tt>. Ignored if no JVM is forked.
     * @param   value   the new VM to use instead of <tt>java</tt>
     * @see #setFork(boolean)
     */
    public void setJvm(String value) {
        commandline.setVm(value);
    }

    /**
     * Create a new JVM argument. Ignored if no JVM is forked.
     * @return  create a new JVM argument so that any argument can be passed to the JVM.
     * @see #setFork(boolean)
     */
    public Commandline.Argument createJvmarg() {
        return commandline.createVmArgument();
    }

    /**
     * The directory to invoke the VM in. Ignored if no JVM is forked.
     * @param   dir     the directory to invoke the JVM from.
     * @see #setFork(boolean)
     */
    public void setDir(File dir) {
        this.dir = dir;
    }

    /**
     * Add a nested sysproperty element. This might be useful to tranfer
     * Ant properties to the testcases when JVM forking is not enabled.
     */
    public void addSysproperty(Environment.Variable sysp) {
        commandline.addSysproperty(sysp);
    }
    
    public Path createClasspath() {
        return commandline.createClasspath(project).createPath();
    }

    /**
     * Add a new single testcase.
     * @param   test    a new single testcase
     * @see JUnitTest
     */
    public void addTest(JUnitTest test) {
        tests.addElement(test);
    }

    /**
     * Create a new set of testcases (also called ..batchtest) and add it to the list.
     * @return  a new instance of a batch test.
     * @see BatchTest
     */
    public BatchTest createBatchTest() {
        BatchTest test = new BatchTest(project);
        batchTests.addElement(test);
        return test;
    }

    /**
     * Add a new formatter to all tests of this task.
     */
    public void addFormatter(FormatterElement fe) {
        formatters.addElement(fe);
    }

    /**
     * Creates a new JUnitRunner and enables fork of a new Java VM.
     */
    public JUnitTask() throws Exception {
        commandline.setClassname("org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner");
    }

    /**
     * Runs the testcase.
     */
    public void execute() throws BuildException {
        Enumeration list = getIndividualTests();
        while (list.hasMoreElements()) {
            JUnitTest test = (JUnitTest)list.nextElement();
            if ( test.shouldRun(project)) {
                execute(test);
            }
        }
    }

    protected void execute(JUnitTest test) throws BuildException {
        // set the default values if not specified
        //@todo should be moved to the test class instead.
        if (test.getTodir() == null) {
            test.setTodir(project.resolveFile("."));
        }

        if (test.getOutfile() == null) {
            test.setOutfile( "TEST-" + test.getName() );
        }

        // execute the test and get the return code
        int exitValue = JUnitTestRunner.ERRORS;
        boolean wasKilled = false;
        if (!test.getFork()) {
            exitValue = executeInVM(test);
        } else {
            ExecuteWatchdog watchdog = createWatchdog();
            exitValue = executeAsForked(test, watchdog);
            // null watchdog means no timeout, you'd better not check with null
            if (watchdog != null) {
                //info will be used in later version do nothing for now
                //wasKilled = watchdog.killedProcess();
            }
        }

        // if there is an error/failure and that it should halt, stop everything otherwise
        // just log a statement
        boolean errorOccurredHere = exitValue == JUnitTestRunner.ERRORS;
        boolean failureOccurredHere = exitValue != JUnitTestRunner.SUCCESS;
        if (errorOccurredHere && test.getHaltonerror()
            || failureOccurredHere && test.getHaltonfailure()) {
            throw new BuildException("Test "+test.getName()+" failed",
                                     location);
        } else if (errorOccurredHere || failureOccurredHere) {
            log("TEST "+test.getName()+" FAILED", Project.MSG_ERR);
        }
    }

    /**
     * Execute a testcase by forking a new JVM. The command will block until
     * it finishes. To know if the process was destroyed or not, use the
     * <tt>killedProcess()</tt> method of the watchdog class.
     * @param  test       the testcase to execute.
     * @param  watchdog   the watchdog in charge of cancelling the test if it
     * exceeds a certain amount of time. Can be <tt>null</tt>, in this case
     * the test could probably hang forever.
     */
    private int executeAsForked(JUnitTest test, ExecuteWatchdog watchdog) throws BuildException {
        CommandlineJava cmd = (CommandlineJava) commandline.clone();

        cmd.setClassname("org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner");
        cmd.createArgument().setValue(test.getName());
        cmd.createArgument().setValue("haltOnError=" + test.getHaltonerror());
        cmd.createArgument().setValue("haltOnFailure=" + test.getHaltonfailure());
        if (summary) {
            log("Running " + test.getName(), Project.MSG_INFO);
            cmd.createArgument().setValue("formatter=org.apache.tools.ant.taskdefs.optional.junit.SummaryJUnitResultFormatter");
        }

        StringBuffer formatterArg = new StringBuffer(128);
        final FormatterElement[] feArray = mergeFormatters(test);
        for (int i = 0; i < feArray.length; i++) {
            FormatterElement fe = feArray[i];
            formatterArg.append("formatter=");
            formatterArg.append(fe.getClassname());
            File outFile = getOutput(fe,test);
            if (outFile != null) {
                formatterArg.append(",");
                formatterArg.append( outFile );
            }
            cmd.createArgument().setValue(formatterArg.toString());
            formatterArg.setLength(0);
        }

        Execute execute = new Execute(new LogStreamHandler(this, Project.MSG_INFO, Project.MSG_WARN), watchdog);
        execute.setCommandline(cmd.getCommandline());
        if (dir != null) {
            execute.setWorkingDirectory(dir);
            execute.setAntRun(project);
        }

        log("Executing: "+cmd.toString(), Project.MSG_VERBOSE);
        try {
            return execute.execute();
        } catch (IOException e) {
            throw new BuildException("Process fork failed.", e, location);
        }
    }

    // in VM is not very nice since it could probably hang the
    // whole build. IMHO this method should be avoided and it would be best
    // to remove it in future versions. TBD. (SBa)
        
    /**
     * Execute inside VM.
     */
    private int executeInVM(JUnitTest test) throws BuildException {
        if (dir != null) {
            log("dir attribute ignored if running in the same VM", Project.MSG_WARN);
        }

        CommandlineJava.SysProperties sysProperties = commandline.getSystemProperties();
        if (sysProperties != null) {
            sysProperties.setSystem();
        }
        try {
            log("Using System properties " + System.getProperties(), Project.MSG_VERBOSE);
            AntClassLoader cl = null;
            Path classpath = commandline.getClasspath();
            if (classpath != null) {
                log("Using CLASSPATH " + classpath, Project.MSG_VERBOSE);

                cl = new AntClassLoader(project, classpath, false);
                // make sure the test will be accepted as a TestCase
                cl.addSystemPackageRoot("junit");
                // will cause trouble in JDK 1.1 if omitted
                cl.addSystemPackageRoot("org.apache.tools.ant");
            }
            JUnitTestRunner runner = new JUnitTestRunner(test, test.getHaltonerror(), test.getHaltonfailure(), cl);

            if (summary) {
                log("Running " + test.getName(), Project.MSG_INFO);

                SummaryJUnitResultFormatter f = new SummaryJUnitResultFormatter();
                f.setOutput( getDefaultOutput() );
                runner.addFormatter(f);
            }

            final FormatterElement[] feArray = mergeFormatters(test);
            for (int i = 0; i < feArray.length; i++) {
                FormatterElement fe = feArray[i];
                File outFile = getOutput(fe,test);
                if (outFile != null) {
                    fe.setOutfile(outFile);
                } else {
                    fe.setOutput( getDefaultOutput() );
                }
                runner.addFormatter(fe.createFormatter());
            }

            runner.run();
            return runner.getRetCode();
        } finally{
            if (sysProperties != null) {
                sysProperties.restoreSystem();
            }
        }
    }

    /**
     * @return <tt>null</tt> if there is a timeout value, otherwise the
     * watchdog instance.
     */
    protected ExecuteWatchdog createWatchdog() throws BuildException {
        if (timeout == null){
            return null;
        }
        return new ExecuteWatchdog(timeout.intValue());
    }

    /**
     * get the default output for a formatter.
     */
    protected OutputStream getDefaultOutput(){
        return new LogOutputStream(this, Project.MSG_INFO);
    }

    /**
     * Merge all individual tests from the batchtest with all individual tests
     * and return an enumeration over all <tt>JUnitTest</tt>.
     */
    protected Enumeration getIndividualTests(){
        Enumeration[] enums = new Enumeration[ batchTests.size() + 1];
        for (int i = 0; i < batchTests.size(); i++) {
            BatchTest batchtest = (BatchTest)batchTests.elementAt(i);
            enums[i] = batchtest.elements();
        }
        enums[enums.length - 1] = tests.elements();
        return Enumerations.fromCompound(enums);
    }

    protected Enumeration allTests() {
        Enumeration[] enums = { tests.elements(), batchTests.elements() };
        return Enumerations.fromCompound(enums);
    }

    private FormatterElement[] mergeFormatters(JUnitTest test){
        Vector feVector = (Vector)formatters.clone();
        test.addFormattersTo(feVector);
        FormatterElement[] feArray = new FormatterElement[feVector.size()];
        feVector.copyInto(feArray);
        return feArray;
    }

    /** return the file or null if does not use a file */
    protected File getOutput(FormatterElement fe, JUnitTest test){
        if (fe.getUseFile()) {
            String filename = test.getOutfile() + fe.getExtension();
            File destFile = new File( test.getTodir(), filename );
            String absFilename = destFile.getAbsolutePath();
            return project.resolveFile(absFilename);
        }
        return null;
    }

}
