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
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

    public void setHaltonerror(boolean value) {
        Enumeration enum = allTests();
        while (enum.hasMoreElements()) {
            BaseTest test = (BaseTest) enum.nextElement();
            test.setHaltonerror(value);
        }
    }

    public void setHaltonfailure(boolean value) {
        Enumeration enum = allTests();
        while (enum.hasMoreElements()) {
            BaseTest test = (BaseTest) enum.nextElement();
            test.setHaltonfailure(value);
        }
    }

    public void setPrintsummary(boolean value) {
        summary = value;
    }

    public void setMaxmemory(String max) {
        if (Project.getJavaVersion().startsWith("1.1")) {
            createJvmarg().setValue("-mx"+max);
        } else {
            createJvmarg().setValue("-Xmx"+max);
        }
    }

    public void setTimeout(Integer value) {
        timeout = value;
    }

    public void setFork(boolean value) {
        Enumeration enum = allTests();
        while (enum.hasMoreElements()) {
            BaseTest test = (BaseTest) enum.nextElement();
            test.setFork(value);
        }
    }

    public void setJvm(String value) {
        commandline.setVm(value);
    }

    public Commandline.Argument createJvmarg() {
        return commandline.createVmArgument();
    }

    public Path createClasspath() {
        return commandline.createClasspath(project).createPath();
    }

    public void addTest(JUnitTest test) {
        tests.addElement(test);
    }

    public BatchTest createBatchTest() {
        BatchTest test = new BatchTest(project);
        batchTests.addElement(test);
        return test;
    }

    public void addFormatter(FormatterElement fe) {
        formatters.addElement(fe);
    }

    /**
     * The directory to invoke the VM in.
     *
     * <p>Ignored if fork=false.
     */
    public void setDir(File dir) {
        this.dir = dir;
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
        boolean errorOccurred = false;
        boolean failureOccurred = false;

        Vector runTests = (Vector) tests.clone();

        Enumeration list = batchTests.elements();
        while (list.hasMoreElements()) {
            BatchTest test = (BatchTest)list.nextElement();
            Enumeration list2 = test.elements();
            while (list2.hasMoreElements()) {
                runTests.addElement(list2.nextElement());
            }
        }

        list = runTests.elements();
        while (list.hasMoreElements()) {
            JUnitTest test = (JUnitTest)list.nextElement();

            if (!test.shouldRun(project)) {
                continue;
            }

            if (test.getTodir() == null){
                test.setTodir(project.resolveFile("."));
            }

            if (test.getOutfile() == null) {
                test.setOutfile( "TEST-" + test.getName() );
            }

            int exitValue = JUnitTestRunner.ERRORS;
            
            if (!test.getFork()) {

                if (dir != null) {
                    log("dir attribute ignored if running in the same VM",
                        Project.MSG_WARN);
                }

                JUnitTestRunner runner = null;

                Path classpath = commandline.getClasspath();
                if (classpath != null) {
                    log("Using CLASSPATH " + classpath, Project.MSG_VERBOSE);
                    AntClassLoader l = new AntClassLoader(project, classpath, 
                                                          false);
                    // make sure the test will be accepted as a TestCase
                    l.addSystemPackageRoot("junit");
                    // will cause trouble in JDK 1.1 if omitted
                    l.addSystemPackageRoot("org.apache.tools.ant");
                    runner = new JUnitTestRunner(test, test.getHaltonerror(),
                                                 test.getHaltonfailure(), l);
                } else {
                    runner = new JUnitTestRunner(test, test.getHaltonerror(),
                                                 test.getHaltonfailure());
                }

                if (summary) {
                    log("Running " + test.getName(), Project.MSG_INFO);
                    
                    SummaryJUnitResultFormatter f = 
                        new SummaryJUnitResultFormatter();
                    f.setOutput(new LogOutputStream(this, Project.MSG_INFO));
                    runner.addFormatter(f);
                }

                for (int i=0; i<formatters.size(); i++) {
                    FormatterElement fe = (FormatterElement) formatters.elementAt(i);
                    setOutput(fe, test);
                    runner.addFormatter(fe.createFormatter());
                }
                FormatterElement[] add = test.getFormatters();
                for (int i=0; i<add.length; i++) {
                    setOutput(add[i], test);
                    runner.addFormatter(add[i].createFormatter());
                }

                runner.run();
                exitValue = runner.getRetCode();

            } else {
                CommandlineJava cmd = (CommandlineJava) commandline.clone();
                
                cmd.setClassname("org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner");
                cmd.createArgument().setValue(test.getName());
                cmd.createArgument().setValue("haltOnError=" 
                                              + test.getHaltonerror());
                cmd.createArgument().setValue("haltOnFailure="
                                              + test.getHaltonfailure());
                if (summary) {
                    log("Running " + test.getName(), Project.MSG_INFO);
                    
                    cmd.createArgument().setValue("formatter=org.apache.tools.ant.taskdefs.optional.junit.SummaryJUnitResultFormatter");
                }

                StringBuffer formatterArg = new StringBuffer();
                for (int i=0; i<formatters.size(); i++) {
                    FormatterElement fe = (FormatterElement) formatters.elementAt(i);
                    formatterArg.append("formatter=");
                    formatterArg.append(fe.getClassname());
                    if (fe.getUseFile()) {
                        formatterArg.append(",");
                    	File destFile = new File( test.getTodir(),
                                                  test.getOutfile() + fe.getExtension() );
                        String filename = destFile.getAbsolutePath();
                        formatterArg.append( project.resolveFile(filename) );
                    }
                    cmd.createArgument().setValue(formatterArg.toString());
                    formatterArg.setLength(0);
                }
                
                FormatterElement[] add = test.getFormatters();
                for (int i=0; i<add.length; i++) {
                    formatterArg.append("formatter=");
                    formatterArg.append(add[i].getClassname());
                    if (add[i].getUseFile()) {
                        formatterArg.append(",");
                    	File destFile = new File( test.getTodir(),
                                                  test.getOutfile() + add[i].getExtension() );
                        String filename = destFile.getAbsolutePath();
                        formatterArg.append( project.resolveFile(filename) );
                    }
                    cmd.createArgument().setValue(formatterArg.toString());
                    formatterArg.setLength(0);
                }

                Execute execute = new Execute(new LogStreamHandler(this, Project.MSG_INFO, Project.MSG_WARN), createWatchdog());
                execute.setCommandline(cmd.getCommandline());
                if (dir != null) {
                    execute.setWorkingDirectory(dir);
                    execute.setAntRun(project);
                }
                
                log("Executing: "+cmd.toString(), Project.MSG_VERBOSE);
                try {
                    exitValue = execute.execute();
                } catch (IOException e) {
                    throw new BuildException("Process fork failed.", e, 
                                             location);
                }
            }

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
    }

    protected ExecuteWatchdog createWatchdog() throws BuildException {
        if (timeout == null) return null;
        return new ExecuteWatchdog(timeout.intValue());
    }

    private void rename(String source, String destination) throws BuildException {
        final File src = new File(source);
        final File dest = new File(destination);

        if (dest.exists()) dest.delete();
        src.renameTo(dest);
    }

    protected Enumeration allTests() {

        return new Enumeration() {
                private Enumeration testEnum = tests.elements();
                private Enumeration batchEnum = batchTests.elements();
                
                public boolean hasMoreElements() {
                    return testEnum.hasMoreElements() ||
                        batchEnum.hasMoreElements();
                }
                
                public Object nextElement() {
                    if (testEnum.hasMoreElements()) {
                        return testEnum.nextElement();
                    }
                    return batchEnum.nextElement();
                }
            };
    }

    protected void setOutput(FormatterElement fe, JUnitTest test) {
        if (fe.getUseFile()) {
            File destFile = new File( test.getTodir(),
                                      test.getOutfile() + fe.getExtension() );
            String filename = destFile.getAbsolutePath();
            fe.setOutfile( project.resolveFile(filename) );
        } else {
            fe.setOutput(new LogOutputStream(this, Project.MSG_INFO));
        }
    }
}
