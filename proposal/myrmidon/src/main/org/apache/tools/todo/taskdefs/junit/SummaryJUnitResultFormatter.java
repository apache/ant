/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.junit;

import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import org.apache.avalon.excalibur.util.StringUtil;
import org.apache.myrmidon.api.TaskException;

/**
 * Prints short summary output of the test to Ant's logging system.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */

public class SummaryJUnitResultFormatter implements JUnitResultFormatter
{

    /**
     * Formatter for timings.
     */
    private NumberFormat nf = NumberFormat.getInstance();

    private boolean withOutAndErr = false;
    private String systemOutput = null;
    private String systemError = null;
    /**
     * OutputStream to write to.
     */
    private OutputStream out;

    /**
     * Empty
     */
    public SummaryJUnitResultFormatter()
    {
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
     * Should the output to System.out and System.err be written to the summary.
     *
     * @param value The new WithOutAndErr value
     */
    public void setWithOutAndErr( boolean value )
    {
        withOutAndErr = value;
    }

    /**
     * Empty
     *
     * @param test The feature to be added to the Error attribute
     * @param t The feature to be added to the Error attribute
     */
    public void addError( Test test, Throwable t )
    {
    }

    /**
     * Empty
     *
     * @param test The feature to be added to the Failure attribute
     * @param t The feature to be added to the Failure attribute
     */
    public void addFailure( Test test, Throwable t )
    {
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
     * Empty
     *
     * @param test Description of Parameter
     */
    public void endTest( Test test )
    {
    }

    /**
     * The whole testsuite ended.
     *
     * @param suite Description of Parameter
     * @exception TaskException Description of Exception
     */
    public void endTestSuite( JUnitTest suite )
        throws TaskException
    {
        StringBuffer sb = new StringBuffer( "Tests run: " );
        sb.append( suite.runCount() );
        sb.append( ", Failures: " );
        sb.append( suite.failureCount() );
        sb.append( ", Errors: " );
        sb.append( suite.errorCount() );
        sb.append( ", Time elapsed: " );
        sb.append( nf.format( suite.getRunTime() / 1000.0 ) );
        sb.append( " sec" );
        sb.append( StringUtil.LINE_SEPARATOR );

        if( withOutAndErr )
        {
            if( systemOutput != null && systemOutput.length() > 0 )
            {
                sb.append( "Output:" ).append( StringUtil.LINE_SEPARATOR ).append( systemOutput )
                    .append( StringUtil.LINE_SEPARATOR );
            }

            if( systemError != null && systemError.length() > 0 )
            {
                sb.append( "Error: " ).append( StringUtil.LINE_SEPARATOR ).append( systemError )
                    .append( StringUtil.LINE_SEPARATOR );
            }
        }

        try
        {
            out.write( sb.toString().getBytes() );
            out.flush();
        }
        catch( IOException ioex )
        {
            throw new TaskException( "Unable to write summary output", ioex );
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

    /**
     * Empty
     *
     * @param t Description of Parameter
     */
    public void startTest( Test t )
    {
    }

    /**
     * Empty
     *
     * @param suite Description of Parameter
     */
    public void startTestSuite( JUnitTest suite )
    {
    }
}
