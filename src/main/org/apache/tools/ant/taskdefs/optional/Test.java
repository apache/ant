/* 
 * Copyright (C) The Apache Software Foundation. All rights reserved. 
 * 
 * This software is published under the terms of the Apache Software License 
 * version 1.1, a copy of which has been included  with this distribution in 
 * the LICENSE file. 
 */ 
package org.apache.tools.ant.taskdefs.optional;
 
import org.apache.tools.ant.BuildException; 
import org.apache.tools.ant.taskdefs.Java; 
import java.util.Vector; 
 
/** 
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a> 
 */ 
public class Test 
    extends Java {
 
    protected Vector                          m_tests            = new Vector();

    protected static final class TestletEntry {

        protected String m_testname = "";

        public void addText( final String testname ) {
            m_testname += testname;
        }

        public String toString() {
            return m_testname;
        }
    }

    public Test() {
        setClassname( "org.apache.testlet.engine.TextTestEngine" );
    }

    public TestletEntry createTestlet() {
        final TestletEntry entry = new TestletEntry();
        m_tests.addElement( entry );
        return entry;
    }
 
    public void setShowSuccess( final boolean showSuccess ) {
        createArg().setValue( "-s=" + showSuccess );
    } 
 
    public void setShowBanner( final String showBanner ) { 
        createArg().setValue( "-b=" + showBanner );
    } 
 
    public void setShowTrace( final String showTrace ) {
         createArg().setValue( "-t=" + showTrace );
    } 
 
    public void setForceShowTrace( final String forceShowTrace ) { 
        createArg().setValue( "-f=" + forceShowTrace );
    } 
 
    public void execute() 
        throws BuildException  { 

        final int size = m_tests.size();

        for( int i = 0; i < size; i ++ ) {
            createArg().setValue( m_tests.elementAt( i ).toString() );
        }

        super.execute();
    } 
} 
 
 
