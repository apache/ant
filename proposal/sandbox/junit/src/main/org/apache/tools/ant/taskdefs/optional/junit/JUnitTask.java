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
package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import junit.runner.TestCollector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.taskdefs.optional.junit.formatter.Formatter;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * The core JUnit task.
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class JUnitTask extends Task {

    private final static Resources RES =
        ResourceManager.getPackageResources( JUnitTask.class );

    /** port to run the server on */
    private int port = -1;

    /** timeout period in ms */
    private long timeout = -1;

    /** formatters that write the tests results */
    private Vector formatters = new Vector();

    /** test collector elements */
    private Vector testCollectors = new Vector();

    /** stop the test run if a failure occurs */
    private boolean haltOnFailure = false;

    /** stop the test run if an error occurs */
    private boolean haltOnError = false;

    /** the command line to launch the TestRunner */
    private CommandlineJava cmd = new CommandlineJava();

// task implementation

    public void execute() throws BuildException {
        File tmp = configureTestRunner();
        Execute execute = new Execute(new LogStreamHandler(this, Project.MSG_INFO, Project.MSG_WARN));
        execute.setCommandline(cmd.getCommandline());
        execute.setAntRun(project);

        log(RES.getString("task.process-cmdline.log", cmd.toString()), Project.MSG_VERBOSE);
        int retVal;
        try {
            retVal = execute.execute();
        } catch (IOException e) {
            String msg = RES.getString("task.process-failed.error");
            throw new BuildException(msg, e, location);
        } finally {
            tmp.delete();
        }

    }

    /**
     * Configure the runner with the appropriate configuration file.
     * @return the reference to the temporary configuration file
     * to be deleted once the TestRunner has ended.
     */
    public File configureTestRunner() {
        Properties props = new Properties();
        props.setProperty("debug", "true");
        props.setProperty("host", "127.0.0.1");
        props.setProperty("port", String.valueOf(port));
        // get all test classes to run...
        StringBuffer buf = new StringBuffer(10240);
        Enumeration classnames = collectTests();
        while (classnames.hasMoreElements()) {
            String classname = (String) classnames.nextElement();
            buf.append(classname).append(" ");
        }
        props.setProperty("classnames", buf.toString());

        // dump the properties to a temporary file.
        FileUtils futils = FileUtils.newFileUtils();
        File f = futils.createTempFile("junit-antrunner", "tmp", new File("."));
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(f));
            props.store(os, "JUnit Ant Runner configuration file");
        } catch (IOException e) {
            throw new BuildException(e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
        }

        // configure the runner
        cmd.createArgument().setValue("-file");
        cmd.createArgument().setValue(f.getAbsolutePath());

        return f;
    }

    /**
     * @return all collected tests specified with test elements.
     */
    protected Enumeration collectTests() {
        Enumeration[] tests = new Enumeration[testCollectors.size()];
        for (int i = 0; i < testCollectors.size(); i++) {
            TestCollector te = (TestCollector) testCollectors.elementAt(i);
            tests[i] = te.collectTests();
        }
        return Enumerations.fromCompound(tests);
    }

// Ant bean accessors

    public void setPort(int port) {
        this.port = port;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void setHaltOnFailure(boolean haltOnFailure) {
        this.haltOnFailure = haltOnFailure;
    }

    public void setHaltOnError(boolean haltOnError) {
        this.haltOnError = haltOnError;
    }

    /** add a new formatter element */
    public void addFormatter(FormatterElement fe) {
        Formatter f = fe.createFormatter();
        this.formatters.addElement(f);
    }

    /** add a single test element */
    public void addTest(TestElement te) {
        this.testCollectors.addElement(te);
    }

    /** add a batch test element */
    public void addBatchTest(BatchTestElement bte) {
        this.testCollectors.addElement(bte);
    }

    /**
     * Set the maximum memory to be used by the TestRunner
     * @param   max     the value as defined by <tt>-mx</tt> or <tt>-Xmx</tt>
     *                  in the java command line options.
     */
    public void setMaxmemory(String max) {
        if (Project.getJavaVersion().startsWith("1.1")) {
            createJvmarg().setValue("-mx" + max);
        } else {
            createJvmarg().setValue("-Xmx" + max);
        }
    }

    /**
     * Create a new JVM argument. Ignored if no JVM is forked.
     * @return  create a new JVM argument so that any argument can be passed to the JVM.
     * @see #setFork(boolean)
     */
    public Commandline.Argument createJvmarg() {
        return cmd.createVmArgument();
    }

    /**
     * <tt>&lt;classpath&gt;</tt> allows classpath to be set for tests.
     */
    public Path createClasspath() {
        return cmd.createClasspath(project).createPath();
    }

    /**
     * Creates a new JUnitRunner and enables fork of a new Java VM.
     */
    public JUnitTask() throws Exception {
        cmd.setClassname("org.apache.tools.ant.taskdefs.optional.junit.remote.TestRunner");
    }

    /**
     * Adds the jars or directories containing Ant, this task and
     * JUnit to the classpath - this should make the forked JVM work
     * without having to specify them directly.
     */
    public void init() {
        addClasspathEntry("/junit/framework/TestCase.class");
        addClasspathEntry("/org/apache/tools/ant/Task.class");
        addClasspathEntry("/org/apache/tools/ant/taskdefs/optional/junit/JUnitTestRunner.class");
    }

    /**
     * Add the directory or archive containing the resource to
     * the command line classpath.
     * @param the resource to look for.
     */
    protected void addClasspathEntry(String resource) {
        File f = JUnitHelper.getResourceEntry(resource);
        if (f != null) {
            createClasspath().setLocation(f);
        }
    }
}
