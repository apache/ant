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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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
 * href="http://www.xprogramming.com/software.htm">http://www.xprogramming.com/software.htm</a>.
 *
 * <p> This ant task runs a single TestCase. By default it spans a new
 * Java VM to prevent interferences between different testcases,
 * unless <code>fork</code> has been disabled.
 *
 * @author Thomas Haas 
 * @author <a href="mailto:stefan.bodewig@megabit.net">Stefan Bodewig</a> 
 */
public class JUnitTask extends Task {

    private CommandlineJava commandline = new CommandlineJava();
    private Vector tests = new Vector();
    private Vector batchTests = new Vector();
    private Vector formatters = new Vector();

    private JUnitTest defaults = new JUnitTest();
    private Integer timeout = null;
    private boolean summary = false;

    public void setHaltonerror(boolean value) {
        defaults.setHaltonerror(value);
    }

    public void setHaltonfailure(boolean value) {
        defaults.setHaltonfailure(value);
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
        defaults.setFork(value);
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
        test.setHaltonerror(defaults.getHaltonerror());
        test.setHaltonfailure(defaults.getHaltonfailure());
        test.setFork(defaults.getFork());
        tests.addElement(test);
    }

    public BatchTest createBatchTest() {
        BatchTest test = new BatchTest(project);
        test.setHaltonerror(defaults.getHaltonerror());
        test.setHaltonfailure(defaults.getHaltonfailure());
        test.setFork(defaults.getFork());
        batchTests.addElement(test);
        return test;
    }

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
        boolean errorOccurred = false;
        boolean failureOccurred = false;

        Enumeration list = batchTests.elements();
        while (list.hasMoreElements()) {
            BatchTest test = (BatchTest)list.nextElement();
            Enumeration list2 = test.elements();
            while (list2.hasMoreElements()) {
                tests.addElement(list2.nextElement());
            }
        }

        list = tests.elements();
        while (list.hasMoreElements()) {
            JUnitTest test = (JUnitTest)list.nextElement();

            if (!test.shouldRun(project)) {
                continue;
            }

            if (test.getOutfile() == null) {
                test.setOutfile(project.resolveFile("TEST-" + test.getName()));
            }

            int exitValue = JUnitTestRunner.ERRORS;
            
            System.err.println(test.getFork());

            if (!test.getFork()) {
                JUnitTestRunner runner = 
                    new JUnitTestRunner(test, test.getHaltonerror(),
                                        test.getHaltonfailure());
                if (summary) {
                    log("Running " + test.getName(), Project.MSG_INFO);
                    
                    SummaryJUnitResultFormatter f = 
                        new SummaryJUnitResultFormatter();
                    f.setOutput(new LogOutputStream(this, Project.MSG_INFO));
                    runner.addFormatter(f);
                }

                for (int i=0; i<formatters.size(); i++) {
                    FormatterElement fe = (FormatterElement) formatters.elementAt(i);
                    if (fe.getUseFile()) {
                        fe.setOutfile(project.resolveFile(test.getOutfile()
                                                          +fe.getExtension()));
                    } else {
                        fe.setOutput(new LogOutputStream(this, Project.MSG_INFO));
                    }
                    runner.addFormatter(fe.createFormatter());
                }
                FormatterElement[] add = test.getFormatters();
                for (int i=0; i<add.length; i++) {
                    if (add[i].getUseFile()) {
                        add[i].setOutfile(project.resolveFile(test.getOutfile()
                                                              +add[i].getExtension()));
                    } else {
                        add[i].setOutput(new LogOutputStream(this, Project.MSG_INFO));
                    }
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
                        formatterArg.append(project.resolveFile(test.getOutfile()
                                                                +fe.getExtension())
                                            .getAbsolutePath());
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
                        formatterArg.append(project.resolveFile(test.getOutfile()
                                                                +add[i].getExtension())
                                            .getAbsolutePath());
                    }
                    cmd.createArgument().setValue(formatterArg.toString());
                    formatterArg.setLength(0);
                }

                Execute execute = new Execute(new LogStreamHandler(this, Project.MSG_INFO, Project.MSG_WARN), createWatchdog());
                execute.setCommandline(cmd.getCommandline());
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
}
