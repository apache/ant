/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Java;

/**
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class Test
     extends Java
{

    protected Vector m_tests = new Vector();

    public Test()
    {
        setClassname( "org.apache.testlet.engine.TextTestEngine" );
    }

    public void setForceShowTrace( final boolean forceShowTrace )
    {
        createArg().setValue( "-f=" + forceShowTrace );
    }

    public void setShowBanner( final String showBanner )
    {
        createArg().setValue( "-b=" + showBanner );
    }

    public void setShowSuccess( final boolean showSuccess )
    {
        createArg().setValue( "-s=" + showSuccess );
    }

    public void setShowTrace( final boolean showTrace )
    {
        createArg().setValue( "-t=" + showTrace );
    }

    public TestletEntry createTestlet()
    {
        final TestletEntry entry = new TestletEntry();
        m_tests.addElement( entry );
        return entry;
    }

    public void execute()
        throws BuildException
    {

        final int size = m_tests.size();

        for( int i = 0; i < size; i++ )
        {
            createArg().setValue( m_tests.elementAt( i ).toString() );
        }

        super.execute();
    }

    protected final static class TestletEntry
    {

        protected String m_testname = "";

        public void addText( final String testname )
        {
            m_testname += testname;
        }

        public String toString()
        {
            return m_testname;
        }
    }
}

