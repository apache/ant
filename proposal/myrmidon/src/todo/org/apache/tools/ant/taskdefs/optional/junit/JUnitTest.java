/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.junit;

import java.util.Iterator;
import java.util.Hashtable;
import java.util.Properties;
import java.util.ArrayList;
import org.apache.tools.ant.Project;

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
    private String name = null;

    /**
     * the name of the result file
     */
    private String outfile = null;

    // Snapshot of the system properties
    private Properties props = null;
    private long runTime;

    // @todo this is duplicating TestResult information. Only the time is not
    // part of the result. So we'd better derive a new class from TestResult
    // and deal with it. (SB)
    private long runs, failures, errors;

    public JUnitTest()
    {
    }

    public JUnitTest( String name )
    {
        this.name = name;
    }

    public JUnitTest( String name, boolean haltOnError, boolean haltOnFailure, boolean filtertrace )
    {
        this.name = name;
        this.haltOnError = haltOnError;
        this.haltOnFail = haltOnFailure;
        this.filtertrace = filtertrace;
    }

    public void setCounts( long runs, long failures, long errors )
    {
        this.runs = runs;
        this.failures = failures;
        this.errors = errors;
    }

    /**
     * Set the name of the test class.
     *
     * @param value The new Name value
     */
    public void setName( String value )
    {
        name = value;
    }

    /**
     * Set the name of the output file.
     *
     * @param value The new Outfile value
     */
    public void setOutfile( String value )
    {
        outfile = value;
    }

    public void setProperties( Hashtable p )
    {
        props = new Properties();
        for( Iterator enum = p.keys(); enum.hasNext(); )
        {
            Object key = enum.next();
            props.put( key, p.get( key ) );
        }
    }

    public void setRunTime( long runTime )
    {
        this.runTime = runTime;
    }

    public FormatterElement[] getFormatters()
    {
        FormatterElement[] fes = new FormatterElement[ formatters.size() ];
        formatters.copyInto( fes );
        return fes;
    }

    /**
     * Get the name of the test class.
     *
     * @return The Name value
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get the name of the output file
     *
     * @return the name of the output file.
     */
    public String getOutfile()
    {
        return outfile;
    }

    public Properties getProperties()
    {
        return props;
    }

    public long getRunTime()
    {
        return runTime;
    }

    public long errorCount()
    {
        return errors;
    }

    public long failureCount()
    {
        return failures;
    }

    public long runCount()
    {
        return runs;
    }

    public boolean shouldRun( Project p )
    {
        if( ifProperty != null && p.getProperty( ifProperty ) == null )
        {
            return false;
        }
        else if( unlessProperty != null &&
            p.getProperty( unlessProperty ) != null )
        {
            return false;
        }

        return true;
    }

    /**
     * Convenient method to add formatters to a vector
     *
     * @param v The feature to be added to the FormattersTo attribute
     */
    void addFormattersTo( ArrayList v )
    {
        final int count = formatters.size();
        for( int i = 0; i < count; i++ )
        {
            v.add( formatters.get( i ) );
        }
    }
}
