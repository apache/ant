/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.embeddor.test;

import java.io.File;
import java.util.HashMap;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.logger.Logger;
import org.apache.myrmidon.AbstractProjectTest;
import org.apache.myrmidon.LogMessageTracker;
import org.apache.myrmidon.components.embeddor.DefaultEmbeddor;
import org.apache.myrmidon.interfaces.embeddor.Embeddor;
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
    extends AbstractProjectTest
{
    private DefaultEmbeddor m_embeddor;

    public DefaultEmbeddorTest( String name )
    {
        super( name );
    }

    /**
     * Tear-down the test.
     */
    protected void tearDown() throws Exception
    {
        if( m_embeddor != null )
        {
            m_embeddor.dispose();
            m_embeddor = null;
        }
    }

    /**
     * Returns an embeddor which can be used to build and execute projects.
     */
    protected Embeddor getEmbeddor() throws Exception
    {
        if( m_embeddor == null )
        {
            // Need to set the context classloader - The default embeddor uses it
            Thread.currentThread().setContextClassLoader( getClass().getClassLoader() );

            final Logger logger = getLogger();
            m_embeddor = new DefaultEmbeddor();
            m_embeddor.enableLogging( logger );

            final Parameters params = new Parameters();
            final File instDir = getInstallDirectory();
            params.setParameter( "myrmidon.home", instDir.getAbsolutePath() );
            m_embeddor.parameterize( params );
            m_embeddor.initialize();
            m_embeddor.start();
        }

        return m_embeddor;
    }

    /**
     * Tests that a project is successfully built from a file.
     */
    public void testProjectBuilder() throws Exception
    {
        // Build the project
        final File projectFile = getTestResource( "project-builder.ant" );
        final Project project = getEmbeddor().createProject( projectFile.getAbsolutePath(), null, null );

        // Verify the project.
        assertEquals( "test-project", project.getProjectName() );
        assertEquals( "main-target", project.getDefaultTargetName() );
        assertEquals( projectFile.getParentFile(), project.getBaseDirectory() );
        assertEquals( 0, project.getProjectNames().length );
        assertEquals( 0, project.getTypeLibs().length );
        assertEquals( 1, project.getTargetNames().length );

        final Target implicitTarget = project.getImplicitTarget();
        assertEquals( 1, implicitTarget.getTasks().length );
        assertEquals( "property", implicitTarget.getTasks()[ 0 ].getName() );

        final Target target = project.getTarget( "main-target" );
        assertEquals( 1, target.getTasks().length );
        assertEquals( "log", target.getTasks()[ 0 ].getName() );
    }

    /**
     * Tests that a listener can be created.
     */
    public void testCreateListener() throws Exception
    {
        final ProjectListener listener = getEmbeddor().createListener( "default" );
        assertNotNull( listener );
    }

    /**
     * Tests that a workspace can execute a project file.
     */
    public void testWorkspaceCreate() throws Exception
    {
        // Build the project
        final File projectFile = getTestResource( "project-builder.ant" );
        final Embeddor embeddor = getEmbeddor();
        final Project project = embeddor.createProject( projectFile.getAbsolutePath(), null, null );

        // Build the workspace
        final Workspace workspace = embeddor.createWorkspace( new HashMap() );

        // Install a listener
        final LogMessageTracker listener = new LogMessageTracker();
        workspace.addProjectListener( listener );

        listener.addExpectedMessage( "main-target", "A log message" );

        // Execute the default target
        final String target = project.getDefaultTargetName();
        workspace.executeProject( project, target );

        // Cleanup
        listener.assertComplete();
    }
}
