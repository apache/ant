/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import java.io.File;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.AbstractProjectTest;
import org.apache.myrmidon.LogMessageTracker;

/**
 * Test cases for the <if> task.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class IfTest
    extends AbstractProjectTest
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( IfTest.class );

    public IfTest( String name )
    {
        super( name );
    }

    /**
     * Test checking whether a property is set and not 'false'.
     */
    public void testConditions()
        throws Exception
    {
        final File projectFile = getTestResource( "if.ant" );

        // Test when property is set to 'true'
        LogMessageTracker listener = new LogMessageTracker();
        listener.addExpectedMessage( "true-prop", "test-prop is set" );
        executeTarget( projectFile, "true-prop", listener );

        // Test when property is set to a value other than 'true' or 'false'
        listener = new LogMessageTracker();
        listener.addExpectedMessage( "set-prop", "test-prop is set" );
        executeTarget( projectFile, "set-prop", listener );

        // Test when property is set to 'false'
        listener = new LogMessageTracker();
        listener.addExpectedMessage( "false-prop", "test-prop is not set" );
        executeTarget( projectFile, "false-prop", listener );

        // Test when property is not set
        listener = new LogMessageTracker();
        listener.addExpectedMessage( "not-set-prop", "test-prop is not set" );
        executeTarget( projectFile, "not-set-prop", listener );
    }

    /**
     * Tests that the <if> task can handle multiple nested tasks.
     */
    public void testMultipleTasks() throws Exception
    {
        final File projectFile = getTestResource( "if.ant" );

        // Test when property is not set
        LogMessageTracker listener = new LogMessageTracker();
        listener.addExpectedMessage( "multiple-nested-tasks", "task 1" );
        listener.addExpectedMessage( "multiple-nested-tasks", "task 2" );
        listener.addExpectedMessage( "multiple-nested-tasks", "task 3" );
        listener.addExpectedMessage( "multiple-nested-tasks", "task 4" );
        executeTarget( projectFile, "multiple-nested-tasks", listener );
    }

    /**
     * Tests validation.
     */
    public void testValidation() throws Exception
    {
        final File projectFile = getTestResource( "if.ant" );

        // Check for missing condition
        String[] messages = {
            null,
            REZ.getString( "if.no-condition.error" )
        };
        executeTargetExpectError( projectFile, "no-condition", messages );

        // Check for too many conditions
        messages = new String[]
        {
            null,
            null,
            REZ.getString( "if.ifelse-duplicate.error" )
        };
        executeTargetExpectError( projectFile, "too-many-conditions", messages );
    }

}
