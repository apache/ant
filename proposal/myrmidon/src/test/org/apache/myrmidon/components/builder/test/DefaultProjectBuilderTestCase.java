/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.builder.test;

import java.io.File;
import java.util.Arrays;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.myrmidon.AbstractMyrmidonTest;
import org.apache.myrmidon.components.builder.DefaultProjectBuilder;
import org.apache.myrmidon.components.builder.DefaultProject;
import org.apache.myrmidon.interfaces.builder.ProjectException;
import org.apache.myrmidon.interfaces.model.Project;

/**
 * Test cases for {@link org.apache.myrmidon.components.builder.DefaultProjectBuilder}.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public class DefaultProjectBuilderTestCase
    extends AbstractMyrmidonTest
{
    private final static Resources REZ = getResourcesForTested( DefaultProjectBuilderTestCase.class );

    private DefaultProjectBuilder m_builder;

    public DefaultProjectBuilderTestCase( String name )
    {
        super( name );
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        m_builder = new DefaultProjectBuilder();
        m_builder.enableLogging( getLogger() );
    }

    /**
     * Creates a project, with default values set.
     */
    private DefaultProject createProject( final File projFile )
    {
        final DefaultProject project = new DefaultProject();
        project.setProjectName( FileUtil.removeExtension( projFile.getName() ) );
        project.setBaseDirectory( getTestDirectory( "." ) );
        project.setDefaultTargetName( "main" );
        return project;
    }

    /**
     * Tests bad project file name.
     */
    public void testProjectFileName() throws Exception
    {
        // Test with a file that does not exist
        File projFile = getTestResource( "unknown.ant", false );

        try
        {
            m_builder.build( projFile.getAbsolutePath() );
            fail();
        }
        catch( ProjectException e )
        {
            final String[] messages =
            {
                REZ.getString( "ant.project-build.error", projFile.getAbsolutePath() ),
                REZ.getString( "ant.no-project-file.error" )
            };
            assertSameMessage( messages, e );
        }

        // Test with a directory
        projFile = getTestDirectory( "some-dir" );

        try
        {
            m_builder.build( projFile.getAbsolutePath() );
            fail();
        }
        catch( ProjectException e )
        {
            final String[] messages =
            {
                REZ.getString( "ant.project-build.error", projFile.getAbsolutePath() ),
                REZ.getString( "ant.no-project-file.error" )
            };
            assertSameMessage( messages, e );
        }
    }

    /**
     * Tests error reporting when the project file contains badly formed XML.
     */
    public void testBadlyFormedFile() throws Exception
    {
        final File projFile = getTestResource( "bad-xml.ant" );
        try
        {
            m_builder.build( projFile.getAbsolutePath() );
            fail();
        }
        catch( ProjectException e )
        {
            final String[] messages =
            {
                REZ.getString( "ant.project-build.error", projFile.getAbsolutePath() ),
                REZ.getString( "ant.project-parse.error" )
            };
            assertSameMessage( messages, e );
        }
    }

    /**
     * Tests building a project with default values for project name, base dir
     * and default target.
     */
    public void testDefaults() throws Exception
    {
        // Build project
        final File projFile = getTestResource( "defaults.ant" );
        Project project = m_builder.build( projFile.getAbsolutePath() );

        // Compare against expected project
        DefaultProject expected = createProject( projFile );
        assertSameProject( expected, project );
    }

    /**
     * Tests setting the project name.
     */
    public void testProjectName() throws Exception
    {
        // Build project
        final File projFile = getTestResource( "set-project-name.ant" );
        Project project = m_builder.build( projFile.getAbsolutePath() );

        // Compare against expected project
        DefaultProject expected = createProject( projFile );
        expected.setProjectName( "some-project" );
        assertSameProject( expected, project );
    }

    /**
     * Tests setting the base directory.
     */
    public void testBaseDirectory() throws Exception
    {
        // Build project
        final File projFile = getTestResource( "set-base-dir.ant" );
        Project project = m_builder.build( projFile.getAbsolutePath() );

        // Compare against expected project
        DefaultProject expected = createProject( projFile );
        final File baseDir = getTestDirectory( "other-base-dir" );
        expected.setBaseDirectory( baseDir );
        assertSameProject( expected, project );
    }

    /**
     * Tests setting the default target name.
     */
    public void testDefaultTarget() throws Exception
    {
        // Build project
        final File projFile = getTestResource( "set-default-target.ant" );
        Project project = m_builder.build( projFile.getAbsolutePath() );

        // Compare against expected project
        DefaultProject expected = createProject( projFile );
        expected.setDefaultTargetName( "some-target" );
        assertSameProject( expected, project );
    }

    /**
     * Tests missing, invalid and incompatible project version.
     */
    public void testProjectVersion() throws Exception
    {
        // No version
        File projFile = getTestResource( "no-version.ant" );
        try
        {
            m_builder.build( projFile.getAbsolutePath() );
            fail();
        }
        catch( ProjectException e )
        {
            final String[] messages =
            {
                REZ.getString( "ant.project-build.error", projFile.getAbsolutePath() ),
                REZ.getString( "ant.version-missing.error" )
            };
            assertSameMessage( messages, e );
        }

        // Badly formed version
        projFile = getTestResource( "bad-version.ant" );
        try
        {
            m_builder.build( projFile.getAbsolutePath() );
            fail();
        }
        catch( ProjectException e )
        {
            final String[] messages =
            {
                REZ.getString( "ant.project-build.error", projFile.getAbsolutePath() ),
                REZ.getString( "ant.malformed.version", "ant2" )
            };
            assertSameMessage( messages, e );
        }

        // Incompatible version
        projFile = getTestResource( "mismatched-version.ant" );
        try
        {
            m_builder.build( projFile.getAbsolutePath() );
            fail();
        }
        catch( ProjectException e )
        {
            final String[] messages =
            {
                REZ.getString( "ant.project-build.error", projFile.getAbsolutePath() ),
                REZ.getString( "ant.bad-version.error", "2.0.0", "1.0.2" )
            };
            assertSameMessage( messages, e );
        }
    }

    /**
     * Asserts that 2 projects are identical.
     */
    protected void assertSameProject( final Project expected,
                                      final Project project )
    {
        assertEquals( expected.getProjectName(), project.getProjectName() );
        assertEquals( expected.getBaseDirectory(), project.getBaseDirectory() );
        assertEquals( expected.getDefaultTargetName(), project.getDefaultTargetName() );

        // TODO - make sure each of the projects are the same
        assertTrue( Arrays.equals( expected.getProjectNames(), project.getProjectNames() ) );

        // TODO - make sure the implicit targets are the same

        // TODO - make sure each of the targets are the same
        assertTrue( Arrays.equals( expected.getTargetNames(), project.getTargetNames() ) );

        // TODO - implement TypeLib.equals(), or use a comparator
        assertTrue( Arrays.equals( expected.getTypeLibs(), project.getTypeLibs() ) );
    }

    /**
     * Tests validation of project and target names.
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
            final String[] messages =
            {
                REZ.getString( "ant.project-build.error", badProjectFile.getAbsolutePath() ),
                REZ.getString( "ant.project-bad-name.error" )
            };
            assertSameMessage( messages, e );
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
            final String[] messages =
            {
                REZ.getString( "ant.project-build.error", badTargetFile.getAbsolutePath() ),
                // TODO - check error message
                null
            };
            assertSameMessage( messages, e );
        }
    }
}
