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
import org.apache.myrmidon.components.workspace.DefaultTaskContext;

/**
 * Test cases for <property> task.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class PropertyTest
    extends AbstractProjectTest
{
    private final static Resources REZ
        = ResourceManager.getPackageResources( PropertyTest.class );

    public PropertyTest( final String name )
    {
        super( name );
    }

    /**
     * Tests setting a property, using an attribute, text content, and
     * nested element.
     */
    public void testSetProperty()
        throws Exception
    {
        final File projectFile = getTestResource( "property.ant" );

        // Set by attribute
        LogMessageTracker tracker = new LogMessageTracker();
        tracker.addExpectedMessage( "set-attr", "test-prop = [some value]" );
        executeTarget( projectFile, "set-attr", tracker );

        // Set by text content
        tracker = new LogMessageTracker();
        tracker.addExpectedMessage( "set-content", "test-prop2 = [some value]" );
        executeTarget( projectFile, "set-content", tracker );

        // Set by nested element
        tracker = new LogMessageTracker();
        tracker.addExpectedMessage( "set-element", "test-prop3 = [value=[some value]]" );
        executeTarget( projectFile, "set-element", tracker );
    }

    /**
     * Tests the validation performed by the propery task.
     */
    public void testValidation()
        throws Exception
    {
        final File projectFile = getTestResource( "property.ant" );

        // Missing name
        String[] messages =
        {
            null,
            REZ.getString( "property.no-name.error" )
        };
        executeTargetExpectError( projectFile, "missing-name", messages );

        // Missing value
        messages = new String[]
        {
            null,
            REZ.getString( "property.no-value.error" )
        };
        executeTargetExpectError( projectFile, "missing-value", messages );

        // Too many values
        messages = new String[]
        {
            null,
            null,
            REZ.getString( "property.multi-set.error" )
        };
        executeTargetExpectError( projectFile, "too-many-values1", messages );
        executeTargetExpectError( projectFile, "too-many-values2", messages );
        executeTargetExpectError( projectFile, "too-many-values3", messages );
    }

    /**
     * Tests basic validation of property names.
     */
    public void testNameValidation() throws Exception
    {
        final File projectFile = getTestResource( "property.ant" );

        final Resources contextResources
            = ResourceManager.getPackageResources( DefaultTaskContext.class );

        // Invalid names
        String[] messages = new String[]
        {
            null,
            contextResources.getString( "bad-property-name.error" ),
            null
        };
        executeTargetExpectError( projectFile, "bad-prop-name1", messages );
        executeTargetExpectError( projectFile, "bad-prop-name2", messages );
        executeTargetExpectError( projectFile, "bad-prop-name3", messages );
    }

}
