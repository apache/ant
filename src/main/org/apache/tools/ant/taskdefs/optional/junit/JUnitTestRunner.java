/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.junit;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.StringUtils;

import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.AssertionFailedError;
import java.lang.reflect.Method;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

/**
 * Simple Testrunner for JUnit that runs all tests of a testsuite.
 *
 * <p>This TestRunner expects a name of a TestCase class as its
 * argument. If this class provides a static suite() method it will be
 * called and the resulting Test will be run. So, the signature should be 
 * <pre><code>
 *     public static junit.framework.Test suite()
 * </code></pre>
 *
 * <p> If no such method exists, all public methods starting with
 * "test" and taking no argument will be run.
 *
 * <p> Summary output is generated at the end. 
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:ehatcher@apache.org">Erik Hatcher</a>
 *
 * @since Ant 1.2
 */

public class JUnitTestRunner implements TestListener {

    /**
     * No problems with this test.
     */
    public static final int SUCCESS = 0;

    /**
     * Some tests failed.
     */
    public static final int FAILURES = 1;

    /**
     * An error occurred.
     */
    public static final int ERRORS = 2;

    /**
     * Holds the registered formatters.
     */
    private Vector formatters = new Vector();

    /**
     * Collects TestResults.
     */
    private TestResult res;

    /**
     * Do we filter junit.*.* stack frames out of failure and error exceptions.
     */
    private static boolean filtertrace = true;
    
    /**
     * Do we send output to System.out/.err in addition to the formatters?
     */
    private boolean showOutput = false;

    private static final String[] DEFAULT_TRACE_FILTERS = new String[] {
                "junit.framework.TestCase",
                "junit.framework.TestResult",
                "junit.framework.TestSuite",
                "junit.framework.Assert.", // don't filter AssertionFailure
                "junit.swingui.TestRunner",
                "junit.awtui.TestRunner",
                "junit.textui.TestRunner",
                "java.lang.reflect.Method.invoke(",
                "org.apache.tools.ant."
        };

    
    /**
     * Do we stop on errors.
     */
    private boolean haltOnError = false;

    /**
     * Do we stop on test failures.
     */
    private boolean haltOnFailure = false;

    /**
     * The corresponding testsuite.
     */
    private Test suite = null;

    /**
     * Exception caught in constructor.
     */
    private Exception exception;

    /**
     * Returncode
     */
    private int retCode = SUCCESS;

    /**
     * The TestSuite we are currently running.
     */
    private JUnitTest junitTest;

    /** output written during the test */
    private PrintStream systemError;
    
    /** Error output during the test */
    private PrintStream systemOut;    
    
    /** is this runner running in forked mode? */
    private boolean forked = false;

    /**
     * Constructor for fork=true or when the user hasn't specified a
     * classpath.  
     */
    public JUnitTestRunner(JUnitTest test, boolean haltOnError, 
                           boolean filtertrace, boolean haltOnFailure) {
        this(test, haltOnError, filtertrace, haltOnFailure, false);
    }

    /**
     * Constructor for fork=true or when the user hasn't specified a
     * classpath.  
     */
    public JUnitTestRunner(JUnitTest test, boolean haltOnError, 
                           boolean filtertrace, boolean haltOnFailure,
                           boolean showOutput) {
        this(test, haltOnError, filtertrace, haltOnFailure, showOutput, null);
    }

    /**
     * Constructor to use when the user has specified a classpath.
     */
    public JUnitTestRunner(JUnitTest test, boolean haltOnError, 
                           boolean filtertrace, boolean haltOnFailure, 
                           ClassLoader loader) {
        this(test, haltOnError, filtertrace, haltOnFailure, false, loader);
    }

    /**
     * Constructor to use when the user has specified a classpath.
     */
    public JUnitTestRunner(JUnitTest test, boolean haltOnError, 
                           boolean filtertrace, boolean haltOnFailure, 
                           boolean showOutput, ClassLoader loader) {
        this.filtertrace = filtertrace;
        this.junitTest = test;
        this.haltOnError = haltOnError;
        this.haltOnFailure = haltOnFailure;
        this.showOutput = showOutput;

        try {
            Class testClass = null;
            if (loader == null) {
                testClass = Class.forName(test.getName());
            } else {
                testClass = loader.loadClass(test.getName());
                AntClassLoader.initializeClass(testClass);
            }
            
            Method suiteMethod = null;
            try {
                // check if there is a suite method
                suiteMethod = testClass.getMethod("suite", new Class[0]);
            } catch (Exception e) {
                // no appropriate suite method found. We don't report any
                // error here since it might be perfectly normal. We don't
                // know exactly what is the cause, but we're doing exactly
                // the same as JUnit TestRunner do. We swallow the exceptions.
            }
            if (suiteMethod != null){
                // if there is a suite method available, then try
                // to extract the suite from it. If there is an error
                // here it will be caught below and reported.
                suite = (Test) suiteMethod.invoke(null, new Class[0]);
            } else {
                // try to extract a test suite automatically
                // this will generate warnings if the class is no suitable Test
                suite = new TestSuite(testClass);
            }
            
        } catch (Exception e) {
            retCode = ERRORS;
            exception = e;
        }
    }

    public void run() {
        res = new TestResult();
        res.addListener(this);
        for (int i = 0; i < formatters.size(); i++) {
            res.addListener((TestListener) formatters.elementAt(i));
        }

        long start = System.currentTimeMillis();

        fireStartTestSuite();
        if (exception != null) { // had an exception in the constructor
            for (int i = 0; i < formatters.size(); i++) {
                ((TestListener) formatters.elementAt(i)).addError(null, 
                                                                 exception);
            }
            junitTest.setCounts(1, 0, 1);
            junitTest.setRunTime(0);
        } else {


            ByteArrayOutputStream errStrm = new ByteArrayOutputStream();
            systemError = new PrintStream(errStrm);
            
            ByteArrayOutputStream outStrm = new ByteArrayOutputStream();
            systemOut = new PrintStream(outStrm);

            PrintStream savedOut = null;
            PrintStream savedErr = null;

            if (forked) {
                savedOut = System.out;
                savedErr = System.err;
                if (!showOutput) {
                    System.setOut(systemOut);
                    System.setErr(systemError);
                } else {
                    System.setOut(new PrintStream(
                                      new TeeOutputStream(
                                          new OutputStream[] {savedOut, 
                                                              systemOut}
                                          )
                                      )
                                  );
                    System.setErr(new PrintStream(
                                      new TeeOutputStream(
                                          new OutputStream[] {savedErr, 
                                                              systemError}
                                          )
                                      )
                                  );
                }
            }
            

            try {
                suite.run(res);
            } finally {
                if (savedOut != null) {
                    System.setOut(savedOut);
                }
                if (savedErr != null) {
                    System.setErr(savedErr);
                }
                
                systemError.close();
                systemError = null;
                systemOut.close();
                systemOut = null;
                sendOutAndErr(new String(outStrm.toByteArray()),
                              new String(errStrm.toByteArray()));

                junitTest.setCounts(res.runCount(), res.failureCount(), 
                                    res.errorCount());
                junitTest.setRunTime(System.currentTimeMillis() - start);
            }
        }
        fireEndTestSuite();

        if (retCode != SUCCESS || res.errorCount() != 0) {
            retCode = ERRORS;
        } else if (res.failureCount() != 0) {
            retCode = FAILURES;
        }
    }

    /**
     * Returns what System.exit() would return in the standalone version.
     *
     * @return 2 if errors occurred, 1 if tests failed else 0.
     */
    public int getRetCode() {
        return retCode;
    }

    /**
     * Interface TestListener.
     *
     * <p>A new Test is started.
     */
    public void startTest(Test t) {}

    /**
     * Interface TestListener.
     *
     * <p>A Test is finished.
     */
    public void endTest(Test test) {}

    /**
     * Interface TestListener for JUnit &lt;= 3.4.
     *
     * <p>A Test failed.
     */
    public void addFailure(Test test, Throwable t) {
        if (haltOnFailure) {
            res.stop();
        }
    }

    /**
     * Interface TestListener for JUnit &gt; 3.4.
     *
     * <p>A Test failed.
     */
    public void addFailure(Test test, AssertionFailedError t) {
        addFailure(test, (Throwable) t);
    }

    /**
     * Interface TestListener.
     *
     * <p>An error occurred while running the test.
     */
    public void addError(Test test, Throwable t) {
        if (haltOnError) {
            res.stop();
        }
    }

    protected void handleOutput(String line) {
        if (systemOut != null) {
            systemOut.println(line);
        }
    }
    
    protected void handleErrorOutput(String line) {
        if (systemError != null) {
            systemError.println(line);
        }
    }
    
    protected void handleFlush(String line) {
        if (systemOut != null) {
            systemOut.print(line);
        }
    }
    
    protected void handleErrorFlush(String line) {
        if (systemError != null) {
            systemError.print(line);
        }
    }
    
    private void sendOutAndErr(String out, String err) {
        for (int i = 0; i < formatters.size(); i++) {
            JUnitResultFormatter formatter = 
                ((JUnitResultFormatter) formatters.elementAt(i));
            
            formatter.setSystemOutput(out);
            formatter.setSystemError(err);
        }
    }

    private void fireStartTestSuite() {
        for (int i = 0; i < formatters.size(); i++) {
            ((JUnitResultFormatter) formatters.elementAt(i))
                .startTestSuite(junitTest);
        }
    }

    private void fireEndTestSuite() {
        for (int i = 0; i < formatters.size(); i++) {
            ((JUnitResultFormatter) formatters.elementAt(i))
                .endTestSuite(junitTest);
        }
    }

    public void addFormatter(JUnitResultFormatter f) {
        formatters.addElement(f);
    }

    /**
     * Entry point for standalone (forked) mode.
     *
     * Parameters: testcaseclassname plus parameters in the format
     * key=value, none of which is required.
     *
     * <table cols="4" border="1">
     * <tr><th>key</th><th>description</th><th>default value</th></tr>
     *
     * <tr><td>haltOnError</td><td>halt test on
     * errors?</td><td>false</td></tr>
     *
     * <tr><td>haltOnFailure</td><td>halt test on
     * failures?</td><td>false</td></tr>
     *
     * <tr><td>formatter</td><td>A JUnitResultFormatter given as
     * classname,filename. If filename is ommitted, System.out is
     * assumed.</td><td>none</td></tr>
     *
     * <tr><td>showoutput</td><td>send output to System.err/.out as
     * well as to the formatters?</td><td>false</td></tr>
     *
     * </table> 
     */
    public static void main(String[] args) throws IOException {
        boolean exitAtEnd = true;
        boolean haltError = false;
        boolean haltFail = false;
        boolean stackfilter = true;
        Properties props = new Properties();
        boolean showOut = false;

        if (args.length == 0) {
            System.err.println("required argument TestClassName missing");
            System.exit(ERRORS);
        }

        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("haltOnError=")) {
                haltError = Project.toBoolean(args[i].substring(12));
            } else if (args[i].startsWith("haltOnFailure=")) {
                haltFail = Project.toBoolean(args[i].substring(14));
            } else if (args[i].startsWith("filtertrace=")) {
                stackfilter = Project.toBoolean(args[i].substring(12));
            } else if (args[i].startsWith("formatter=")) {
                try {
                    createAndStoreFormatter(args[i].substring(10));
                } catch (BuildException be) {
                    System.err.println(be.getMessage());
                    System.exit(ERRORS);
                }
            } else if (args[i].startsWith("propsfile=")) {
                FileInputStream in = new FileInputStream(args[i]
                                                         .substring(10));
                props.load(in);
                in.close();
            } else if (args[i].startsWith("showoutput=")) {
                showOut = Project.toBoolean(args[i].substring(11));
            }
        }
        
        JUnitTest t = new JUnitTest(args[0]);
        
        // Add/overlay system properties on the properties from the Ant project
        Hashtable p = System.getProperties();
        for (Enumeration enum = p.keys(); enum.hasMoreElements();) {
            Object key = enum.nextElement();
            props.put(key, p.get(key));
        }
        t.setProperties(props);

        JUnitTestRunner runner = new JUnitTestRunner(t, haltError, stackfilter,
                                                     haltFail, showOut);
        runner.forked = true;
        transferFormatters(runner);
        runner.run();
        System.exit(runner.getRetCode());
    }

    private static Vector fromCmdLine = new Vector();

    private static void transferFormatters(JUnitTestRunner runner) {
        for (int i = 0; i < fromCmdLine.size(); i++) {
            runner.addFormatter((JUnitResultFormatter) fromCmdLine
                                .elementAt(i));
        }
    }

    /**
     * Line format is: formatter=<classname>(,<pathname>)?
     */
    private static void createAndStoreFormatter(String line)
        throws BuildException {
        FormatterElement fe = new FormatterElement();
        int pos = line.indexOf(',');
        if (pos == -1) {
            fe.setClassname(line);
        } else {
            fe.setClassname(line.substring(0, pos));
            fe.setOutfile(new File(line.substring(pos + 1)));
        }
        fromCmdLine.addElement(fe.createFormatter());
    }
    
    /**
     * Returns a filtered stack trace.
     * This is ripped out of junit.runner.BaseTestRunner.
     * Scott M. Stirling.
     */
    public static String getFilteredTrace(Throwable t) {
        String trace = StringUtils.getStackTrace(t);
        return JUnitTestRunner.filterStack(trace);
    }

    /**
     * Filters stack frames from internal JUnit and Ant classes
     */
    public static String filterStack(String stack) {
        if (!filtertrace) {
            return stack;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        StringReader sr = new StringReader(stack);
        BufferedReader br = new BufferedReader(sr);

        String line;
        try {
            while ((line = br.readLine()) != null) {
                if (!filterLine(line)) {
                    pw.println(line);
                }
            }
        } catch (Exception IOException) {
            return stack; // return the stack unfiltered
        }
        return sw.toString();
    }

    private static boolean filterLine(String line) {
        for (int i = 0; i < DEFAULT_TRACE_FILTERS.length; i++) {
            if (line.indexOf(DEFAULT_TRACE_FILTERS[i]) > 0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Helper class that sends output sent to multiple streams.
     *
     * @since Ant 1.5
     */
    private class TeeOutputStream extends OutputStream {

        private OutputStream[] outs;

        private TeeOutputStream(OutputStream[] outs) {
            this.outs = outs;
        }

        public void write(int b) throws IOException {
            for (int i = 0; i  < outs.length; i++) {
                outs[i].write(b);
            }
        }

    }

} // JUnitTestRunner
