/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.junit;

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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.apache.avalon.framework.ExceptionUtil;
import org.apache.myrmidon.api.TaskException;

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
 * @author <a href="mailto:ehatcher@apache.org">Erik Hatcher</a>
 */
public class JUnitTestRunner
    implements TestListener
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

    private final static String[] DEFAULT_TRACE_FILTERS = new String[]
    {
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

    private static ArrayList m_fromCmdLine = new ArrayList();

    /**
     * Holds the registered formatters.
     */
    private ArrayList m_formatters = new ArrayList();

    /**
     * Do we stop on errors.
     */
    private boolean m_haltOnError;

    /**
     * Do we stop on test failures.
     */
    private boolean m_haltOnFailure;

    /**
     * The corresponding testsuite.
     */
    private Test m_suite;

    /**
     * Returncode
     */
    private int m_retCode = SUCCESS;

    /**
     * Exception caught in constructor.
     */
    private Exception m_exception;

    /**
     * The TestSuite we are currently running.
     */
    private JUnitTest m_junitTest;

    /**
     * Collects TestResults.
     */
    private TestResult m_res;

    /**
     * output written during the test
     */
    private PrintStream m_systemError;

    /**
     * Error output during the test
     */
    private PrintStream m_systemOut;

    /**
     * Constructor for fork=true or when the user hasn't specified a classpath.
     *
     * @param test Description of Parameter
     * @param haltOnError Description of Parameter
     * @param filtertrace Description of Parameter
     * @param haltOnFailure Description of Parameter
     */
    public JUnitTestRunner( final JUnitTest test,
                            final boolean haltOnError,
                            final boolean filtertrace,
                            final boolean haltOnFailure )
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
        this.m_junitTest = test;
        this.m_haltOnError = haltOnError;
        this.m_haltOnFailure = haltOnFailure;

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
                m_suite = (Test)suiteMethod.invoke( null, new Class[ 0 ] );
            }
            else
            {
                // try to extract a test suite automatically
                // this will generate warnings if the class is no suitable Test
                m_suite = new TestSuite( testClass );
            }

        }
        catch( Exception e )
        {
            m_retCode = ERRORS;
            m_exception = e;
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
        final String trace = ExceptionUtil.printStackTrace( t );
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
        throws IOException, TaskException
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
                haltError = "true".equals( args[ i ].substring( 12 ) );
            }
            else if( args[ i ].startsWith( "haltOnFailure=" ) )
            {
                haltFail = "true".equals( args[ i ].substring( 14 ) );
            }
            else if( args[ i ].startsWith( "filtertrace=" ) )
            {
                stackfilter = "true".equals( args[ i ].substring( 12 ) );
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
        props.putAll( p );
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
        m_fromCmdLine.add( fe.createFormatter() );
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
        for( int i = 0; i < m_fromCmdLine.size(); i++ )
        {
            runner.addFormatter( (JUnitResultFormatter)m_fromCmdLine.get( i ) );
        }
    }

    /**
     * Returns what System.exit() would return in the standalone version.
     *
     * @return 2 if errors occurred, 1 if tests failed else 0.
     */
    public int getRetCode()
    {
        return m_retCode;
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
        if( m_haltOnError )
        {
            m_res.stop();
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
        if( m_haltOnFailure )
        {
            m_res.stop();
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
        m_formatters.add( f );
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
        throws TaskException
    {
        m_res = new TestResult();
        m_res.addListener( this );
        for( int i = 0; i < m_formatters.size(); i++ )
        {
            final TestListener listener = (TestListener)m_formatters.get( i );
            m_res.addListener( listener );
        }

        long start = System.currentTimeMillis();

        fireStartTestSuite();
        if( m_exception != null )
        {// had an exception in the constructor
            for( int i = 0; i < m_formatters.size(); i++ )
            {
                ( (TestListener)m_formatters.get( i ) ).addError( null,
                                                                  m_exception );
            }
            m_junitTest.setCounts( 1, 0, 1 );
            m_junitTest.setRunTime( 0 );
        }
        else
        {

            ByteArrayOutputStream errStrm = new ByteArrayOutputStream();
            m_systemError = new PrintStream( errStrm );

            ByteArrayOutputStream outStrm = new ByteArrayOutputStream();
            m_systemOut = new PrintStream( outStrm );

            try
            {
                m_suite.run( m_res );
            }
            finally
            {
                m_systemError.close();
                m_systemError = null;
                m_systemOut.close();
                m_systemOut = null;
                sendOutAndErr( new String( outStrm.toByteArray() ),
                               new String( errStrm.toByteArray() ) );

                m_junitTest.setCounts( m_res.runCount(), m_res.failureCount(),
                                       m_res.errorCount() );
                m_junitTest.setRunTime( System.currentTimeMillis() - start );
            }
        }
        fireEndTestSuite();

        if( m_retCode != SUCCESS || m_res.errorCount() != 0 )
        {
            m_retCode = ERRORS;
        }
        else if( m_res.failureCount() != 0 )
        {
            m_retCode = FAILURES;
        }
    }

    /**
     * Interface TestListener. <p>
     *
     * A new Test is started.
     */
    public void startTest( Test t )
    {
    }

    protected void handleErrorOutput( String line )
    {
        if( m_systemError != null )
        {
            m_systemError.println( line );
        }
    }

    protected void handleOutput( String line )
    {
        if( m_systemOut != null )
        {
            m_systemOut.println( line );
        }
    }

    private void fireEndTestSuite()
        throws TaskException
    {
        final int size = m_formatters.size();
        for( int i = 0; i < size; i++ )
        {
            final JUnitResultFormatter formatter =
                (JUnitResultFormatter)m_formatters.get( i );
            formatter.endTestSuite( m_junitTest );
        }
    }

    private void fireStartTestSuite()
        throws TaskException
    {
        final int size = m_formatters.size();
        for( int i = 0; i < size; i++ )
        {
            final JUnitResultFormatter formatter = (JUnitResultFormatter)m_formatters.get( i );
            formatter.startTestSuite( m_junitTest );
        }
    }

    private void sendOutAndErr( String out, String err )
    {
        final int size = m_formatters.size();
        for( int i = 0; i < size; i++ )
        {
            final JUnitResultFormatter formatter =
                (JUnitResultFormatter)m_formatters.get( i );

            formatter.setSystemOutput( out );
            formatter.setSystemError( err );
        }
    }
}
