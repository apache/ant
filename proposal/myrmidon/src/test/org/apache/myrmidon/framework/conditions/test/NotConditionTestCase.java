/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.conditions.test;

import org.apache.myrmidon.AbstractProjectTest;
import java.io.File;

/**
 * Test cases for the <not> condition.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class NotConditionTestCase
    extends AbstractProjectTest
{
    public NotConditionTestCase( final String name )
    {
        super( name );
    }

    /**
     * Tests evaluation of <not>.
     */
    public void testEvaluation() throws Exception
    {
        final File projectFile = getTestResource( "not.ant" );
        executeTarget( projectFile, "truth-table" );

        // TODO - check error messages
        executeTargetExpectError( projectFile, "empty", new String[0] );
        executeTargetExpectError( projectFile, "too-many-nested", new String[0] );
    }
}
