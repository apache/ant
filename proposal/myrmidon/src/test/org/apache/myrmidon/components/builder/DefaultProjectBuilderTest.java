/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.builder;

import java.io.File;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.AbstractMyrmidonTest;

/**
 * Test cases for {@link DefaultProjectBuilder}.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public class DefaultProjectBuilderTest
    extends AbstractMyrmidonTest
{
    private final static Resources REZ
        = ResourceManager.getPackageResources( DefaultProjectBuilder.class );

    private DefaultProjectBuilder m_builder;

    public DefaultProjectBuilderTest( String name )
    {
        super( name );
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        m_builder = new DefaultProjectBuilder();
        m_builder.enableLogging( createLogger() );
    }

    /**
     * Test validation of project and target names.
     */
    public void testNameValidation() throws Exception
    {
        // Check bad project name
        final File badProjectFile = getTestResource( "bad-project-name.ant" );
        try
        {
            m_builder.build( badProjectFile.getAbsolutePath() );
            fail();
        }
        catch( Exception e )
        {
            assertSameMessage( REZ.getString( "ant.project-bad-name.error" ), e );
        }

        // Check bad target name
        final File badTargetFile = getTestResource( "bad-target-name.ant" );
        try
        {
            m_builder.build( badTargetFile.getAbsolutePath() );
            fail();
        }
        catch( Exception e )
        {
            // TODO - check error message
        }
    }
}
