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
 * Test cases for the <or> condition.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class OrConditionTestCase
    extends AbstractProjectTest
{
    public OrConditionTestCase( final String name )
    {
        super( name );
    }

    /**
     * Tests evaluation of the <or> condition.
     */
    public void testEvaluation() throws Exception
    {
        final File projectFile = getTestResource( "or.ant" );
        executeTarget( projectFile, "empty" );
        executeTarget( projectFile, "truth-table" );
        executeTarget( projectFile, "lazy" );
    }
}
