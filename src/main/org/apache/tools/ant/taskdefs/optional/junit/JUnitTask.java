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
import org.apache.tools.ant.types.EnumeratedAttribute;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import java.net.URL;

/**
 * Ant task to run JUnit tests.
 *
 * <p> JUnit is a framework to create unit test. It has been initially
 * created by Erich Gamma and Kent Beck.  JUnit can be found at <a
 * href="http://www.junit.org">http://www.junit.org</a>.
 *
 * <p> <code>JUnitTask</code> can run a single specific <code>JUnitTest</code> using the <code>test</code> element. 
 * For example, the following target <code><pre>
 *   &lt;target name="test-int-chars" depends="jar-test"&gt;
 *       &lt;echo message="testing international characters"/&gt;
 *       &lt;junit printsummary="no" haltonfailure="yes" fork="false"&gt;
 *           &lt;classpath refid="classpath"/&gt;
 *           &lt;formatter type="plain" usefile="false" /&gt;
 *           &lt;test name="org.apache.ecs.InternationalCharTest" /&gt;
 *       &lt;/junit&gt;
 *   &lt;/target&gt;
 * </pre></code> runs a single junit test (<code>org.apache.ecs.InternationalCharTest</code>) 
 * in the current VM using the path with id <code>classpath</code> as classpath 
 * and presents the results formatted using the standard <code>plain</code> formatter on the command line.
 *
 * <p> This task can also run batches of tests. 
 * The <code>batchtest</code> element creates a <code>BatchTest</code> based on a fileset. 
 * This allows, for example, all classes found in directory to be run as testcases. 
 * For example, <code><pre>
 * &lt;target name="run-tests" depends="dump-info,compile-tests" if="junit.present"&gt;
 *   &lt;junit printsummary="no" haltonfailure="yes" fork="${junit.fork}"&gt;
 *     &lt;jvmarg value="-classic"/&gt;
 *     &lt;classpath refid="tests-classpath"/&gt;
 *     &lt;sysproperty key="build.tests" value="${build.tests}"/&gt;
 *     &lt;formatter type="brief" usefile="false" /&gt;
 *     &lt;batchtest&gt;
 *       &lt;fileset dir="${tests.dir}"&gt;
 *         &lt;include name="**&#047;*Test*" /&gt;
 *       &lt;/fileset&gt;
 *     &lt;/batchtest&gt;
 *   &lt;/junit&gt;
 * &lt;/target&gt;
 * </pre></code> this target finds any classes with a <code>test</code> directory anywhere in their path
 * (under the top <code>${tests.dir}</code>, of course) and creates <code>JUnitTest</code>'s for each one.
 *
 * <p> Of course, <code>&lt;junit&gt;</code> and <code>&lt;batch&gt;</code> elements can be combined
 * for more complex tests. For an example, see the ant <code>build.xml</code> target <code>run-tests</code> 
 * (the second example is an edited version).
 * 
 * <p> To spawn a new Java VM to prevent interferences between
 * different testcases, you need to enable <code>fork</code>. 
 * A number of attributes and elements allow you to set up how this JVM runs.
 * <ul>
 * <li>{@link #setTimeout} property sets the maximum time allowed before a test is 'timed out'
 * <li>{@link #setMaxmemory} property sets memory assignment for the forked jvm
 * <li>{@link #setJvm} property allows the jvm to be specified
 * <li>The <code>&lt;jvmarg&gt;</code> element sets arguements to be passed to the forked jvm 
 * </ul>
 * @author Thomas Haas
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 * @author <a href="mailto:Gerrit.Riessen@web.de">Gerrit Riessen</a>
 * @author <a href="mailto:erik@hatcher.net">Erik Hatcher</a>
 *
 * @see JUnitTest
 * @see BatchTest
 */
public class JUnitTask extends Task {

    private CommandlineJava commandline = new CommandlineJava();
    private Vector tests = new Vector();
    private Vector batchTests = new Vector();
    private Vector formatters = new Vector();
    private File dir = null;

    private Integer timeout = null;
    private boolean summary = false;
    private String summaryValue = "";
    private JUnitTestRunner runner = null;
    
    /**
     * Tells this task to halt when there is an error in a test.
     * this property is applied on all BatchTest (batchtest) and JUnitTest (test)
     * however it can possibly be overridden by their own properties.
     * @param   value   <tt>true</tt> if it should halt, otherwise <tt>false</tt>
     */
    public void setHaltonerror(boolean value) {
        Enumeration enum = allTests();
        while (enum.hasMoreElements()) {
            BaseTest test = (BaseTest) enum.nextElement();
            test.setHaltonerror(value);
        }
    }

    /**
     * Tells this task to set the named property to "true" when there is a error in a test.
     * This property is applied on all BatchTest (batchtest) and JUnitTest (test),
     * however, it can possibly be overriden by their own properties.
     * @param  value  the name of the property to set in the event of an error.
     */
    public void setErrorProperty(String propertyName) {
        Enumeration enum = allTests();
        while (enum.hasMoreElements()) {
            BaseTest test = (BaseTest) enum.nextElement();
            test.setErrorProperty(propertyName);
        }
    }

    /**
     * Tells this task to halt when there is a failure in a test.
     * this property is applied on all BatchTest (batchtest) and JUnitTest (test)
     * however it can possibly be overridden by their own properties.
     * @param   value   <tt>true</tt> if it should halt, otherwise <tt>false</tt>
     */
    public void setHaltonfailure(boolean value) {
        Enumeration enum = allTests();
        while (enum.hasMoreElements()) {
            BaseTest test = (BaseTest) enum.nextElement();
            test.setHaltonfailure(value);
        }
    }

    /**
     * Tells this task to set the named property to "true" when there is a failure in a test.
     * This property is applied on all BatchTest (batchtest) and JUnitTest (test),
     * however, it can possibly be overriden by their own properties.
     * @param  value  the name of the property to set in the event of an failure.
     */
    public void setFailureProperty(String propertyName) {
        Enumeration enum = allTests();
        while (enum.hasMoreElements()) {
            BaseTest test = (BaseTest) enum.nextElement();
            test.setFailureProperty(propertyName);
        }
    }

    /**
     * Tells whether a JVM should be forked for each testcase. It avoids interference
     * between testcases and possibly avoids hanging the build.
     * this property is applied on all BatchTest (batchtest) and JUnitTest (test)
     * however it can possibly be overridden by their own properties.
     * @param   value   <tt>true</tt> if a JVM should be forked, otherwise <tt>false</tt>
     * @see #setTimeout
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
     * @param value <tt>true</tt> to print a summary,
     *   <tt>withOutAndErr</tt> to include the test&apos;s output as
     *   well, <tt>false</tt> otherwise.
     * @see SummaryJUnitResultFormatter
     */
    public void setPrintsummary(SummaryAttribute value) {
        summaryValue = value.getValue();
        summary = value.asBoolean();
    }

    /** 
     * Print summary enumeration values.
     */
    public static class SummaryAttribute extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"true", "yes", "false", "no", 
                                 "on", "off", "withOutAndErr"};
        }

        public boolean asBoolean() {
            return "true".equals(value)
                || "on".equals(value)
                || "yes".equals(value)
                || "withOutAndErr".equals(value);
        }
    }

    /**
     * Set the timeout value (in milliseconds). If the test is running for more than this
     * value, the test will be canceled. (works only when in 'fork' mode).
     * @param   value   the maximum time (in milliseconds) allowed before declaring the test
     *                  as 'timed-out'
     * @see #setFork(boolean)
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
    
    /**
     * <code>&lt;classpath&gt;</code> allows classpath to be set for tests.
     */
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
     * Adds the jars or directories containing Ant, this task and
     * JUnit to the classpath - this should make the forked JVM work
     * without having to specify the directly.
     */
    public void init() {
        addClasspathEntry("/junit/framework/TestCase.class");
        addClasspathEntry("/org/apache/tools/ant/Task.class");
        addClasspathEntry("/org/apache/tools/ant/taskdefs/optional/junit/JUnitTestRunner.class");
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

    /**
     * Run the tests.
     */
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
                wasKilled = watchdog.killedProcess();
            }
        }

        // if there is an error/failure and that it should halt, stop everything otherwise
        // just log a statement
        boolean errorOccurredHere = exitValue == JUnitTestRunner.ERRORS;
        boolean failureOccurredHere = exitValue != JUnitTestRunner.SUCCESS;
        if (errorOccurredHere || failureOccurredHere) {
            if (errorOccurredHere && test.getHaltonerror()
                || failureOccurredHere && test.getHaltonfailure()) {
                throw new BuildException("Test "+test.getName()+" failed"
                                         +(wasKilled ? " (timeout)" : ""),
                                         location);
            } else {
                log("TEST "+test.getName()+" FAILED" 
                    + (wasKilled ? " (timeout)" : ""), Project.MSG_ERR);
                if (errorOccurredHere && test.getErrorProperty() != null) {
                    project.setProperty(test.getErrorProperty(), "true");
                }
                if (failureOccurredHere && test.getFailureProperty() != null) {
                    project.setProperty(test.getFailureProperty(), "true");
                }
            }
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

        // Create a temporary file to pass the Ant properties to the forked test
        File propsFile = new File("junit" + (new Random(System.currentTimeMillis())).nextLong() + ".properties");
        cmd.createArgument().setValue("propsfile=" + propsFile.getAbsolutePath());
        Hashtable p = project.getProperties(); 
        Properties props = new Properties();
        for (Enumeration enum = p.keys(); enum.hasMoreElements(); ) {
            Object key = enum.nextElement();
            props.put(key, p.get(key));
        }
        try {
            FileOutputStream outstream = new FileOutputStream(propsFile);
            props.save(outstream,"Ant JUnitTask generated properties file");
            outstream.close();
        } catch (java.io.IOException e) {
            throw new BuildException("Error creating temporary properties file.", e, location);
        }

        Execute execute = new Execute(new LogStreamHandler(this, Project.MSG_INFO, Project.MSG_WARN), watchdog);
        execute.setCommandline(cmd.getCommandline());
        execute.setAntRun(project);
        if (dir != null) {
            execute.setWorkingDirectory(dir);
        }

        log("Executing: "+cmd.toString(), Project.MSG_VERBOSE);
        int retVal; 
        try {
            retVal = execute.execute();
        } catch (IOException e) {
            throw new BuildException("Process fork failed.", e, location);
        } finally {
          if (! propsFile.delete()) throw new BuildException("Could not delete temporary properties file.");
        }

        return retVal;
    }

    // in VM is not very nice since it could probably hang the
    // whole build. IMHO this method should be avoided and it would be best
    // to remove it in future versions. TBD. (SBa)
        

    protected void handleOutput(String line) {
        if (runner != null) {
            runner.handleOutput(line);
        }
        else {
            super.handleOutput(line);
        }
    }
    
    protected void handleErrorOutput(String line) {
        if (runner != null) {
            runner.handleErrorOutput(line);
        }
        else {
            super.handleErrorOutput(line);
        }
    }
    
    /**
     * Execute inside VM.
     */
    private int executeInVM(JUnitTest test) throws BuildException {
        test.setProperties(project.getProperties());
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

                cl = new AntClassLoader(null, project, classpath, false);
                // make sure the test will be accepted as a TestCase
                cl.addSystemPackageRoot("junit");
                // will cause trouble in JDK 1.1 if omitted
                cl.addSystemPackageRoot("org.apache.tools.ant");
            }
            runner = new JUnitTestRunner(test, test.getHaltonerror(), test.getHaltonfailure(), cl);

            if (summary) {
                log("Running " + test.getName(), Project.MSG_INFO);

                SummaryJUnitResultFormatter f = 
                  new SummaryJUnitResultFormatter();
                f.setWithOutAndErr( "withoutanderr".equalsIgnoreCase( summaryValue ));
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
     * Get the default output for a formatter.
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

    /**
     * Search for the given resource and add the directory or archive
     * that contains it to the classpath.
     *
     * <p>Doesn't work for archives in JDK 1.1 as the URL returned by
     * getResource doesn't contain the name of the archive.</p>
     */
    protected void addClasspathEntry(String resource) {
        URL url = getClass().getResource(resource);
        if (url != null) {
            String u = url.toString();
            if (u.startsWith("jar:file:")) {
                int pling = u.indexOf("!");
                String jarName = u.substring(9, pling);
                log("Implicitly adding "+jarName+" to classpath", 
                    Project.MSG_DEBUG);
                createClasspath().setLocation(new File((new File(jarName)).getAbsolutePath()));
            } else if (u.startsWith("file:")) {
                int tail = u.indexOf(resource);
                String dirName = u.substring(5, tail);
                log("Implicitly adding "+dirName+" to classpath", 
                    Project.MSG_DEBUG);
                createClasspath().setLocation(new File((new File(dirName)).getAbsolutePath()));
            } else {
                log("Don\'t know how to handle resource URL "+u, 
                    Project.MSG_DEBUG);
            }
        } else {
            log("Couldn\'t find "+resource, Project.MSG_DEBUG);
        }
    }

}
