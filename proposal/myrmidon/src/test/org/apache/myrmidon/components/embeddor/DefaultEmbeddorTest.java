/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.embeddor;

import java.io.File;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.myrmidon.AbstractMyrmidonTest;
import org.apache.myrmidon.TrackingProjectListener;
import org.apache.myrmidon.interfaces.model.Project;
import org.apache.myrmidon.interfaces.model.Target;
import org.apache.myrmidon.interfaces.workspace.Workspace;
import org.apache.myrmidon.listeners.ProjectListener;

/**
 * Test cases for the default embeddor.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class DefaultEmbeddorTest
    extends AbstractMyrmidonTest
{
    private DefaultEmbeddor m_embeddor;

    public DefaultEmbeddorTest( String name )
    {
        super( name );
    }

    /**
     * Setup the test, by creating and initialising the embeddor.
     */
    protected void setUp() throws Exception
    {
        final Logger logger = createLogger();
        m_embeddor = new DefaultEmbeddor();
        m_embeddor.enableLogging( logger );

        final Parameters params = new Parameters();
        final File instDir = getHomeDirectory();
        params.setParameter( "myrmidon.home", instDir.getAbsolutePath() );
        m_embeddor.parameterize( params );
        m_embeddor.initialize();
        m_embeddor.start();
    }

    /**
     * Tear-down the test.
     */
    protected void tearDown() throws Exception
    {
        m_embeddor.dispose();
        m_embeddor = null;
    }

    /**
     * Tests that a project is successfully built from a file.
     */
    public void testProjectBuilder() throws Exception
    {
        final File projectFile = getTestResource( "project-builder.ant" );
        assertTrue( "Project file \"" + projectFile + "\" does not exist.", projectFile.exists() );

        // Build the project
        final Project project = m_embeddor.createProject( projectFile.getAbsolutePath(), null, null );

        // Verify the project.
        assertEquals( "test-project", project.getProjectName() );
        assertEquals( "main-target", project.getDefaultTargetName() );
        assertEquals( projectFile.getParentFile(), project.getBaseDirectory() );
        assertEquals( 0, project.getProjectNames().length );
        assertEquals( 0, project.getTypeLibs().length );
        assertEquals( 1, project.getTargetNames().length );

        final Target implicitTarget = project.getImplicitTarget();
        assertEquals( 1, implicitTarget.getTasks().length );
        assertEquals( "property", implicitTarget.getTasks()[0].getName() );

        final Target target = project.getTarget( "main-target" );
        assertEquals( 1, target.getTasks().length );
        assertEquals( "log", target.getTasks()[0].getName() );
    }

    /**
     * Tests that a listener can be created.
     */
    public void testCreateListener() throws Exception
    {
        final ProjectListener listener = m_embeddor.createListener( "default" );
    }

    /**
     * Tests that a workspace can execute a project file.
     */
    public void testWorkspaceCreate() throws Exception
    {
        // Build the project
        final File projectFile = getTestResource( "project-builder.ant" );
        final Project project = m_embeddor.createProject( projectFile.getAbsolutePath(), null, null );

        // Build the workspace
        final Workspace workspace = m_embeddor.createWorkspace( new Parameters() );

        // Install a listener
        final TrackingProjectListener listener = new TrackingProjectListener();
        workspace.addProjectListener( listener );

        listener.addExpectedMessage( "main-target", "A log message" );

        // Execute the default target
        final String target = project.getDefaultTargetName();
        workspace.executeProject( project, target );

        // Cleanup
        listener.assertComplete();
    }
}
