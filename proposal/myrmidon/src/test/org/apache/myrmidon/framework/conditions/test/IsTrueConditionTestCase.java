/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.conditions.test;

import java.io.File;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.AbstractProjectTest;
import org.apache.myrmidon.framework.conditions.IsTrueCondition;

/**
 * Test cases for the <is-true> condition.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class IsTrueConditionTestCase
    extends AbstractProjectTest
{
    public IsTrueConditionTestCase( final String name )
    {
        super( name );
    }

    /**
     * Test cases for <is-true> evaluation.
     */
    public void testEvaluation() throws Exception
    {
        final File projectFile = getTestResource( "istrue.ant" );
        executeTarget( projectFile, "set2true" );
        executeTarget( projectFile, "set2false" );
        executeTarget( projectFile, "not-set" );

        // TODO - check error message
        String[] messages = {};
        executeTargetExpectError( projectFile, "set", messages );

        final Resources res = getResourcesForTested( IsTrueCondition.class );
        messages = new String[] {
            null,
            res.getString( "istrue.no-property.error" )
        };
        executeTargetExpectError( projectFile, "no-prop-name", messages );
    }

}
