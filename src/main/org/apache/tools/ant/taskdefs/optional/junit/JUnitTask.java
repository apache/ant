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
 */
public class JUnitTask extends Task {

    // <XXX> private final static int HALT_NOT = 1;
    // <XXX> private final static int HALT_IMMEDIATELY = 2;
    // <XXX> private final static int HALT_AT_END = 3;

    private CommandlineJava commandline = new CommandlineJava();
    private Vector tests = new Vector();

    private JUnitTest defaults = new JUnitTest();
    private boolean defaultOutfile = true;
    private Integer timeout = null;

    // <XXX> private int haltOnError = HALT_AT_END;
    // <XXX> private int haltOnFailure = HALT_AT_END;

    /**
     * Set the path to junit classes.
     * @param junit path to junit classes.
     */
    public void setJunit(String junit) {
        commandline.createClasspath(project).createPathElement().setLocation(junit);
    }

    public void setDefaulthaltonerror(boolean value) {
        defaults.setHaltonerror(value);
    }

    public void setDefaulthaltonfailure(boolean value) {
        defaults.setHaltonfailure(value);
    }

    public void setDefaultprintsummary(boolean value) {
        defaults.setPrintsummary(value);
    }

    public void setDefaultprintxml(boolean value) {
        defaults.setPrintxml(value);
    }

    public void setDefaultOutFile(boolean value) {
        defaultOutfile = value;
    }

    public void setTimeout(Integer value) {
        timeout = value;
    }

    public void setFork(boolean value) {
        defaults.setFork(value);
    }

    public void setJvm(String value) {
        defaults.setName(value);
	commandline.setVm(value);
    }

    public void setJvmargs(String value) {
	commandline.createVmArgument().setLine(value);
    }


    /**
     * Set the classpath to be used by the testcase.
     * @param classpath the classpath used to run the testcase.
     */
    /*public void setClasspath(String classpath) {
        this.classpath = classpath;
    }*/


    public Path createClasspath() {
        return commandline.createClasspath(project);
    }

    public JUnitTest createTest() {
        final JUnitTest result;
        result = new JUnitTest(
            defaults.getFork(),
            defaults.getHaltonerror(),
            defaults.getHaltonfailure(),
            defaults.getPrintsummary(),
            defaults.getPrintxml(),
            null, null);

        tests.addElement(result);
        return result;
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

        final String oldclasspath = System.getProperty("java.class.path");

        commandline.createClasspath(project).createPathElement().setPath(oldclasspath);
        /*
         * This doesn't work on JDK 1.1, should use a Classloader of our own 
         * anyway --SB
         *
         * System.setProperty("java.class.path", commandline.createClasspath().toString());
         */

        Enumeration list = tests.elements();
        while (list.hasMoreElements()) {
            final JUnitTest test = (JUnitTest)list.nextElement();

            final String filename = "TEST-" + test.getName() + ".xml";
// removed --SB
//            if (new File(filename).exists()) {
//                project.log("Skipping " + test.getName());
//                continue;
//            }
            project.log("Running " + test.getName());

            if (defaultOutfile && (test.getOutfile() == null ||
                test.getOutfile().length() == 0)) {

// removed --SB
//                test.setOutfile("RUNNING-" + filename);
                test.setOutfile(filename);
            }

            int exitValue = 2;

            if (test.getFork()) {
                try {
		    // Create a watchdog based on the timeout attribute
                    final Execute execute = new Execute(new PumpStreamHandler(), createWatchdog());
                    final Commandline cmdl = new Commandline();
                    cmdl.addLine(commandline.getCommandline());
                    cmdl.addLine(test.getCommandline());
                    execute.setCommandline(cmdl.getCommandline());
                    log("Execute JUnit: " + cmdl, Project.MSG_VERBOSE);
                    exitValue = execute.execute();
                }
                catch (IOException e) {
                    throw new BuildException("Process fork failed.", e, 
                                             location);
                }
            } else {
                final Object[] arg = { test };
                final Class[] argType = { arg[0].getClass() };
                try {
                    final Class target = Class.forName("org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner");
                    final Method main = target.getMethod("runTest", argType);
                    project.log("Load JUnit: " + test, Project.MSG_VERBOSE);
                    exitValue = ((Integer)main.invoke(null, arg)).intValue();
                } catch (InvocationTargetException e) {
                    Throwable t = e.getTargetException();
                    String msg = "Running test failed: " + t.getMessage();
                    throw new BuildException(msg, t, location);
                } catch (Exception e) {
                    String msg = "Running test failed: " + e.getMessage();
                    throw new BuildException(msg, e, location);
                }
            }

            boolean errorOccurredHere = exitValue == 2;
            boolean failureOccurredHere = exitValue == 1;
// removed --SB
//            if (exitValue != 0) {
//                rename("RUNNING-" + filename, "ERROR-" + filename);
//            } else {
//                rename("RUNNING-" + filename, filename);
//            }
	    // <XXX> later distinguish HALT_AT_END case
            if (errorOccurredHere && test.getHaltonerror()
                || failureOccurredHere && test.getHaltonfailure()) {
                throw new BuildException("JUNIT FAILED", location);
	    } else if (errorOccurredHere || failureOccurredHere) {
                log("JUNIT FAILED", Project.MSG_ERR);
            }

	    // Update overall test status
            errorOccurred = errorOccurred || errorOccurredHere ;
            failureOccurred = failureOccurred || failureOccurredHere ;
        }

	// <XXX> later add HALT_AT_END option
        // Then test errorOccurred and failureOccurred here.
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
