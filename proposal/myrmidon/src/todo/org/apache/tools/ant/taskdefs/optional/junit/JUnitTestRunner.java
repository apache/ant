/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.StringUtils;

/**
 * Simple Testrunner for JUnit that runs all tests of a testsuite. <p>
 *
 * This TestRunner expects a name of a TestCase class as its argument. If this
 * class provides a static suite() method it will be called and the resulting
 * Test will be run. So, the signature should be <pre><code>
 *     public static junit.framework.Test suite()
 * </code></pre> <p>
 *
 * If no such method exists, all public methods starting with "test" and taking
 * no argument will be run. <p>
 *
 * Summary output is generated at the end.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:erik@hatcher.net">Erik Hatcher</a>
 */

public class JUnitTestRunner implements TestListener
{

    /**
     * No problems with this test.
     */
    public final static int SUCCESS = 0;

    /**
     * Some tests failed.
     */
    public final static int FAILURES = 1;

    /**
     * An error occured.
     */
    public final static int ERRORS = 2;

    /**
     * Do we filter junit.*.* stack frames out of failure and error exceptions.
     */
    private static boolean filtertrace = true;

    private final static String[] DEFAULT_TRACE_FILTERS = new String[]{
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

    private static Vector fromCmdLine = new Vector();

    /**
     * Holds the registered formatters.
     */
    private Vector formatters = new Vector();

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
     * Returncode
     */
    private int retCode = SUCCESS;

    /**
     * Exception caught in constructor.
     */
    private Exception exception;

    /**
     * The TestSuite we are currently running.
     */
    private JUnitTest junitTest;

    /**
     * Collects TestResults.
     */
    private TestResult res;

    /**
     * output written during the test
     */
    private PrintStream systemError;

    /**
     * Error output during the test
     */
    private PrintStream systemOut;

    /**
     * Constructor for fork=true or when the user hasn't specified a classpath.
     *
     * @param test Description of Parameter
     * @param haltOnError Description of Parameter
     * @param filtertrace Description of Parameter
     * @param haltOnFailure Description of Parameter
     */
    public JUnitTestRunner( JUnitTest test, boolean haltOnError, boolean filtertrace,
                            boolean haltOnFailure )
    {
        this( test, haltOnError, filtertrace, haltOnFailure, null );
    }

    /**
     * Constructor to use when the user has specified a classpath.
     *
     * @param test Description of Parameter
     * @param haltOnError Description of Parameter
     * @param filtertrace Description of Parameter
     * @param haltOnFailure Description of Parameter
     * @param loader Description of Parameter
     */
    public JUnitTestRunner( JUnitTest test, boolean haltOnError, boolean filtertrace,
                            boolean haltOnFailure, ClassLoader loader )
    {
        //JUnitTestRunner.filtertrace = filtertrace;
        this.filtertrace = filtertrace;
        this.junitTest = test;
        this.haltOnError = haltOnError;
        this.haltOnFailure = haltOnFailure;

        try
        {
            Class testClass = null;
            if( loader == null )
            {
                testClass = Class.forName( test.getName() );
            }
            else
            {
                testClass = loader.loadClass( test.getName() );
                AntClassLoader.initializeClass( testClass );
            }

            Method suiteMethod = null;
            try
            {
                // check if there is a suite method
                suiteMethod = testClass.getMethod( "suite", new Class[ 0 ] );
            }
            catch( Exception e )
            {
                // no appropriate suite method found. We don't report any
                // error here since it might be perfectly normal. We don't
                // know exactly what is the cause, but we're doing exactly
                // the same as JUnit TestRunner do. We swallow the exceptions.
            }
            if( suiteMethod != null )
            {
                // if there is a suite method available, then try
                // to extract the suite from it. If there is an error
                // here it will be caught below and reported.
                suite = (Test)suiteMethod.invoke( null, new Class[ 0 ] );
            }
            else
            {
                // try to extract a test suite automatically
                // this will generate warnings if the class is no suitable Test
                suite = new TestSuite( testClass );
            }

        }
        catch( Exception e )
        {
            retCode = ERRORS;
            exception = e;
        }
    }

    /**
     * Returns a filtered stack trace. This is ripped out of
     * junit.runner.BaseTestRunner. Scott M. Stirling.
     *
     * @param t Description of Parameter
     * @return The FilteredTrace value
     */
    public static String getFilteredTrace( Throwable t )
    {
        String trace = StringUtils.getStackTrace( t );
        return JUnitTestRunner.filterStack( trace );
    }

    /**
     * Filters stack frames from internal JUnit and Ant classes
     *
     * @param stack Description of Parameter
     * @return Description of the Returned Value
     */
    public static String filterStack( String stack )
    {
        if( !filtertrace )
        {
            return stack;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter( sw );
        StringReader sr = new StringReader( stack );
        BufferedReader br = new BufferedReader( sr );

        String line;
        try
        {
            while( ( line = br.readLine() ) != null )
            {
                if( !filterLine( line ) )
                {
                    pw.println( line );
                }
            }
        }
        catch( Exception IOException )
        {
            return stack;// return the stack unfiltered
        }
        return sw.toString();
    }

    /**
     * Entry point for standalone (forked) mode. Parameters: testcaseclassname
     * plus parameters in the format key=value, none of which is required.
     *
     * <tablecols="4" border="1">
     *
     *   <tr>
     *
     *     <th>
     *       key
     *     </th>
     *
     *     <th>
     *       description
     *     </th>
     *
     *     <th>
     *       default value
     *     </th>
     *
     *   </tr>
     *
     *   <tr>
     *
     *     <td>
     *       haltOnError
     *     </td>
     *
     *     <td>
     *       halt test on errors?
     *     </td>
     *
     *     <td>
     *       false
     *     </td>
     *
     *   </tr>
     *
     *   <tr>
     *
     *     <td>
     *       haltOnFailure
     *     </td>
     *
     *     <td>
     *       halt test on failures?
     *     </td>
     *
     *     <td>
     *       false
     *     </td>
     *
     *   </tr>
     *
     *   <tr>
     *
     *     <td>
     *       formatter
     *     </td>
     *
     *     <td>
     *       A JUnitResultFormatter given as classname,filename. If filename is
     *       ommitted, System.out is assumed.
     *     </td>
     *
     *     <td>
     *       none
     *     </td>
     *
     *   </tr>
     *
     * </table>
     *
     *
     * @param args The command line arguments
     * @exception IOException Description of Exception
     */
    public static void main( String[] args )
        throws IOException
    {
        boolean exitAtEnd = true;
        boolean haltError = false;
        boolean haltFail = false;
        boolean stackfilter = true;
        Properties props = new Properties();

        if( args.length == 0 )
        {
            System.err.println( "required argument TestClassName missing" );
            System.exit( ERRORS );
        }

        for( int i = 1; i < args.length; i++ )
        {
            if( args[ i ].startsWith( "haltOnError=" ) )
            {
                haltError = Project.toBoolean( args[ i ].substring( 12 ) );
            }
            else if( args[ i ].startsWith( "haltOnFailure=" ) )
            {
                haltFail = Project.toBoolean( args[ i ].substring( 14 ) );
            }
            else if( args[ i ].startsWith( "filtertrace=" ) )
            {
                stackfilter = Project.toBoolean( args[ i ].substring( 12 ) );
            }
            else if( args[ i ].startsWith( "formatter=" ) )
            {
                try
                {
                    createAndStoreFormatter( args[ i ].substring( 10 ) );
                }
                catch( TaskException be )
                {
                    System.err.println( be.getMessage() );
                    System.exit( ERRORS );
                }
            }
            else if( args[ i ].startsWith( "propsfile=" ) )
            {
                FileInputStream in = new FileInputStream( args[ i ].substring( 10 ) );
                props.load( in );
                in.close();
            }
        }

        JUnitTest t = new JUnitTest( args[ 0 ] );

        // Add/overlay system properties on the properties from the Ant project
        Hashtable p = System.getProperties();
        for( Enumeration enum = p.keys(); enum.hasMoreElements(); )
        {
            Object key = enum.nextElement();
            props.put( key, p.get( key ) );
        }
        t.setProperties( props );

        JUnitTestRunner runner = new JUnitTestRunner( t, haltError, stackfilter, haltFail );
        transferFormatters( runner );
        runner.run();
        System.exit( runner.getRetCode() );
    }

    /**
     * Line format is: formatter=<classname>(,<pathname>
     *
     * )?
     *
     * @param line Description of Parameter
     * @exception TaskException Description of Exception
     */
    private static void createAndStoreFormatter( String line )
        throws TaskException
    {
        FormatterElement fe = new FormatterElement();
        int pos = line.indexOf( ',' );
        if( pos == -1 )
        {
            fe.setClassname( line );
        }
        else
        {
            fe.setClassname( line.substring( 0, pos ) );
            fe.setOutfile( new File( line.substring( pos + 1 ) ) );
        }
        fromCmdLine.addElement( fe.createFormatter() );
    }

    private static boolean filterLine( String line )
    {
        for( int i = 0; i < DEFAULT_TRACE_FILTERS.length; i++ )
        {
            if( line.indexOf( DEFAULT_TRACE_FILTERS[ i ] ) > 0 )
            {
                return true;
            }
        }
        return false;
    }

    private static void transferFormatters( JUnitTestRunner runner )
    {
        for( int i = 0; i < fromCmdLine.size(); i++ )
        {
            runner.addFormatter( (JUnitResultFormatter)fromCmdLine.elementAt( i ) );
        }
    }

    /**
     * Returns what System.exit() would return in the standalone version.
     *
     * @return 2 if errors occurred, 1 if tests failed else 0.
     */
    public int getRetCode()
    {
        return retCode;
    }

    /**
     * Interface TestListener. <p>
     *
     * An error occured while running the test.
     *
     * @param test The feature to be added to the Error attribute
     * @param t The feature to be added to the Error attribute
     */
    public void addError( Test test, Throwable t )
    {
        if( haltOnError )
        {
            res.stop();
        }
    }

    /**
     * Interface TestListener for JUnit &lt;= 3.4. <p>
     *
     * A Test failed.
     *
     * @param test The feature to be added to the Failure attribute
     * @param t The feature to be added to the Failure attribute
     */
    public void addFailure( Test test, Throwable t )
    {
        if( haltOnFailure )
        {
            res.stop();
        }
    }

    /**
     * Interface TestListener for JUnit &gt; 3.4. <p>
     *
     * A Test failed.
     *
     * @param test The feature to be added to the Failure attribute
     * @param t The feature to be added to the Failure attribute
     */
    public void addFailure( Test test, AssertionFailedError t )
    {
        addFailure( test, (Throwable)t );
    }

    public void addFormatter( JUnitResultFormatter f )
    {
        formatters.addElement( f );
    }

    /**
     * Interface TestListener. <p>
     *
     * A Test is finished.
     *
     * @param test Description of Parameter
     */
    public void endTest( Test test )
    {
    }

    public void run()
    {
        res = new TestResult();
        res.addListener( this );
        for( int i = 0; i < formatters.size(); i++ )
        {
            res.addListener( (TestListener)formatters.elementAt( i ) );
        }

        long start = System.currentTimeMillis();

        fireStartTestSuite();
        if( exception != null )
        {// had an exception in the constructor
            for( int i = 0; i < formatters.size(); i++ )
            {
                ( (TestListener)formatters.elementAt( i ) ).addError( null,
                                                                      exception );
            }
            junitTest.setCounts( 1, 0, 1 );
            junitTest.setRunTime( 0 );
        }
        else
        {

            ByteArrayOutputStream errStrm = new ByteArrayOutputStream();
            systemError = new PrintStream( errStrm );

            ByteArrayOutputStream outStrm = new ByteArrayOutputStream();
            systemOut = new PrintStream( outStrm );

            try
            {
                suite.run( res );
            }
            finally
            {
                systemError.close();
                systemError = null;
                systemOut.close();
                systemOut = null;
                sendOutAndErr( new String( outStrm.toByteArray() ),
                               new String( errStrm.toByteArray() ) );

                junitTest.setCounts( res.runCount(), res.failureCount(),
                                     res.errorCount() );
                junitTest.setRunTime( System.currentTimeMillis() - start );
            }
        }
        fireEndTestSuite();

        if( retCode != SUCCESS || res.errorCount() != 0 )
        {
            retCode = ERRORS;
        }
        else if( res.failureCount() != 0 )
        {
            retCode = FAILURES;
        }
    }

    /**
     * Interface TestListener. <p>
     *
     * A new Test is started.
     *
     * @param t Description of Parameter
     */
    public void startTest( Test t )
    {
    }

    protected void handleErrorOutput( String line )
    {
        if( systemError != null )
        {
            systemError.println( line );
        }
    }

    protected void handleOutput( String line )
    {
        if( systemOut != null )
        {
            systemOut.println( line );
        }
    }

    private void fireEndTestSuite()
    {
        for( int i = 0; i < formatters.size(); i++ )
        {
            ( (JUnitResultFormatter)formatters.elementAt( i ) ).endTestSuite( junitTest );
        }
    }

    private void fireStartTestSuite()
    {
        for( int i = 0; i < formatters.size(); i++ )
        {
            ( (JUnitResultFormatter)formatters.elementAt( i ) ).startTestSuite( junitTest );
        }
    }

    private void sendOutAndErr( String out, String err )
    {
        for( int i = 0; i < formatters.size(); i++ )
        {
            JUnitResultFormatter formatter =
                ( (JUnitResultFormatter)formatters.elementAt( i ) );

            formatter.setSystemOutput( out );
            formatter.setSystemError( err );
        }
    }

}// JUnitTestRunner
