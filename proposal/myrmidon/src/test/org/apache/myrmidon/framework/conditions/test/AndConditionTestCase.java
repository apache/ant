/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.conditions.test;

import java.io.File;
import org.apache.myrmidon.AbstractProjectTest;

/**
 * Test cases for the <and> condition.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class AndConditionTestCase
    extends AbstractProjectTest
{
    public AndConditionTestCase( final String name )
    {
        super( name );
    }

    /**
     * Tests evaluation of the <and> condition.
     */
    public void testEvaluation() throws Exception
    {
        final File projectFile = getTestResource( "and.ant" );
        executeTarget( projectFile, "empty" );
        executeTarget( projectFile, "truth-table" );
        executeTarget( projectFile, "lazy" );
    }
}
