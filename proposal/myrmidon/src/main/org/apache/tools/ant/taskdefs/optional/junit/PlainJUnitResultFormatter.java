/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.Hashtable;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import org.apache.avalon.excalibur.util.StringUtil;
import org.apache.myrmidon.api.TaskException;

/**
 * Prints plain text output of the test to a specified Writer.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */

public class PlainJUnitResultFormatter implements JUnitResultFormatter
{

    /**
     * Formatter for timings.
     */
    private NumberFormat nf = NumberFormat.getInstance();
    /**
     * Timing helper.
     */
    private Hashtable testStarts = new Hashtable();
    /**
     * Suppress endTest if testcase failed.
     */
    private Hashtable failed = new Hashtable();

    private String systemOutput = null;
    private String systemError = null;
    /**
     * Helper to store intermediate output.
     */
    private StringWriter inner;
    /**
     * Where to write the log to.
     */
    private OutputStream out;
    /**
     * Convenience layer on top of {@link #inner inner}.
     */
    private PrintWriter wri;

    public PlainJUnitResultFormatter()
    {
        inner = new StringWriter();
        wri = new PrintWriter( inner );
    }

    public void setOutput( OutputStream out )
    {
        this.out = out;
    }

    public void setSystemError( String err )
    {
        systemError = err;
    }

    public void setSystemOutput( String out )
    {
        systemOutput = out;
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
        formatError( "\tCaused an ERROR", test, t );
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
        formatError( "\tFAILED", test, t );
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

    /**
     * Interface TestListener. <p>
     *
     * A Test is finished.
     *
     * @param test Description of Parameter
     */
    public void endTest( Test test )
    {
        synchronized( wri )
        {
            wri.print( "Testcase: "
                       + JUnitVersionHelper.getTestCaseName( test ) );
            if( Boolean.TRUE.equals( failed.get( test ) ) )
            {
                return;
            }
            Long l = (Long)testStarts.get( test );
            wri.println( " took "
                         + nf.format( ( System.currentTimeMillis() - l.longValue() )
                                      / 1000.0 )
                         + " sec" );
        }
    }

    /**
     * The whole testsuite ended.
     */
    public void endTestSuite( JUnitTest suite )
        throws TaskException
    {
        StringBuffer sb = new StringBuffer( "Testsuite: " );
        sb.append( suite.getName() );
        sb.append( StringUtil.LINE_SEPARATOR );
        sb.append( "Tests run: " );
        sb.append( suite.runCount() );
        sb.append( ", Failures: " );
        sb.append( suite.failureCount() );
        sb.append( ", Errors: " );
        sb.append( suite.errorCount() );
        sb.append( ", Time elapsed: " );
        sb.append( nf.format( suite.getRunTime() / 1000.0 ) );
        sb.append( " sec" );
        sb.append( StringUtil.LINE_SEPARATOR );

        // append the err and output streams to the log
        if( systemOutput != null && systemOutput.length() > 0 )
        {
            sb.append( "------------- Standard Output ---------------" )
                .append( StringUtil.LINE_SEPARATOR )
                .append( systemOutput )
                .append( "------------- ---------------- ---------------" )
                .append( StringUtil.LINE_SEPARATOR );
        }

        if( systemError != null && systemError.length() > 0 )
        {
            sb.append( "------------- Standard Error -----------------" )
                .append( StringUtil.LINE_SEPARATOR )
                .append( systemError )
                .append( "------------- ---------------- ---------------" )
                .append( StringUtil.LINE_SEPARATOR );
        }

        sb.append( StringUtil.LINE_SEPARATOR );

        if( out != null )
        {
            try
            {
                out.write( sb.toString().getBytes() );
                wri.close();
                out.write( inner.toString().getBytes() );
                out.flush();
            }
            catch( IOException ioex )
            {
                throw new TaskException( "Unable to write output", ioex );
            }
            finally
            {
                if( out != System.out && out != System.err )
                {
                    try
                    {
                        out.close();
                    }
                    catch( IOException e )
                    {
                    }
                }
            }
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
        testStarts.put( t, new Long( System.currentTimeMillis() ) );
        failed.put( t, Boolean.FALSE );
    }

    /**
     * Empty.
     *
     * @param suite Description of Parameter
     */
    public void startTestSuite( JUnitTest suite )
    {
    }

    private void formatError( String type, Test test, Throwable t )
    {
        synchronized( wri )
        {
            if( test != null )
            {
                endTest( test );
                failed.put( test, Boolean.TRUE );
            }

            wri.println( type );
            wri.println( t.getMessage() );
            String strace = JUnitTestRunner.getFilteredTrace( t );
            wri.print( strace );
            wri.println( "" );
        }
    }

}// PlainJUnitResultFormatter
