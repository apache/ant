/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.junit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import org.apache.myrmidon.api.TaskContext;

/**
 * <p>
 *
 * Run a single JUnit test. <p>
 *
 * The JUnit test is actually run by {@link JUnitTestRunner}. So read the doc
 * comments for that class :)
 *
 * @author Thomas Haas
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> ,
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 * @see JUnitTask
 * @see JUnitTestRunner
 */
public class JUnitTest extends BaseTest
{
    /**
     * the name of the test case
     */
    private String m_name;

    /**
     * the name of the result file
     */
    private String m_outfile;

    // Snapshot of the system properties
    private Properties m_props;

    private long m_runTime;

    // @todo this is duplicating TestResult information. Only the time is not
    // part of the result. So we'd better derive a new class from TestResult
    // and deal with it. (SB)
    private long m_runs;
    private long m_failures;
    private long m_errors;

    public JUnitTest()
    {
    }

    public JUnitTest( String name )
    {
        m_name = name;
    }

    public JUnitTest( final String name,
                      final boolean haltOnError,
                      final boolean haltOnFailure,
                      final boolean filtertrace )
    {
        m_name = name;
        m_haltOnError = haltOnError;
        m_haltOnFail = haltOnFailure;
        m_filtertrace = filtertrace;
    }

    public void setCounts( long runs, long failures, long errors )
    {
        m_runs = runs;
        m_failures = failures;
        m_errors = errors;
    }

    /**
     * Set the name of the test class.
     */
    public void setName( final String value )
    {
        m_name = value;
    }

    /**
     * Set the name of the output file.
     */
    public void setOutfile( final String value )
    {
        m_outfile = value;
    }

    public void setProperties( final Map properties )
    {
        m_props = new Properties();
        final Iterator enum = properties.keySet().iterator();
        while( enum.hasNext() )
        {
            final Object key = enum.next();
            final Object value = properties.get( key );
            m_props.put( key, value );
        }
    }

    public void setRunTime( final long runTime )
    {
        m_runTime = runTime;
    }

    public FormatterElement[] getFormatters()
    {
        return (FormatterElement[])formatters.toArray( new FormatterElement[ formatters.size() ] );
    }

    /**
     * Get the name of the test class.
     *
     * @return The Name value
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * Get the name of the output file
     *
     * @return the name of the output file.
     */
    public String getOutfile()
    {
        return m_outfile;
    }

    public Properties getProperties()
    {
        return m_props;
    }

    public long getRunTime()
    {
        return m_runTime;
    }

    public long errorCount()
    {
        return m_errors;
    }

    public long failureCount()
    {
        return m_failures;
    }

    public long runCount()
    {
        return m_runs;
    }

    public boolean shouldRun( final TaskContext context )
    {
        if( ifProperty != null && context.getProperty( ifProperty ) == null )
        {
            return false;
        }
        else if( unlessProperty != null &&
            context.getProperty( unlessProperty ) != null )
        {
            return false;
        }

        return true;
    }

    /**
     * Convenient method to add formatters to a vector
     */
    void addFormattersTo( ArrayList v )
    {
        v.addAll( formatters );
    }
}
