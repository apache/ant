/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.OutputStream;
import junit.framework.TestListener;
import org.apache.myrmidon.api.TaskException;

/**
 * This Interface describes classes that format the results of a JUnit testrun.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public interface JUnitResultFormatter extends TestListener
{
    /**
     * The whole testsuite started.
     *
     * @param suite Description of Parameter
     * @exception TaskException Description of Exception
     */
    void startTestSuite( JUnitTest suite )
        throws TaskException;

    /**
     * The whole testsuite ended.
     *
     * @param suite Description of Parameter
     * @exception TaskException Description of Exception
     */
    void endTestSuite( JUnitTest suite )
        throws TaskException;

    /**
     * Sets the stream the formatter is supposed to write its results to.
     *
     * @param out The new Output value
     */
    void setOutput( OutputStream out );

    /**
     * This is what the test has written to System.out
     *
     * @param out The new SystemOutput value
     */
    void setSystemOutput( String out );

    /**
     * This is what the test has written to System.err
     *
     * @param err The new SystemError value
     */
    void setSystemError( String err );
}
