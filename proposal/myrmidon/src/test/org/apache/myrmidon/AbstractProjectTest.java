/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon;

import java.io.File;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.myrmidon.components.embeddor.DefaultEmbeddor;
import org.apache.myrmidon.interfaces.embeddor.Embeddor;
import org.apache.myrmidon.interfaces.model.Project;
import org.apache.myrmidon.interfaces.workspace.Workspace;
import org.apache.myrmidon.listeners.ProjectListener;

/**
 * A base class for test cases which need to execute projects.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class AbstractProjectTest
    extends AbstractMyrmidonTest
{
    private DefaultEmbeddor m_embeddor;

    public AbstractProjectTest( final String name )
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
            final File instDir = getHomeDirectory();
            params.setParameter( "myrmidon.home", instDir.getAbsolutePath() );
            m_embeddor.parameterize( params );
            m_embeddor.initialize();
            m_embeddor.start();
        }

        return m_embeddor;
    }

    /**
     * Executes a target in a project, and asserts that it fails with the
     * given error message.
     */
    protected void executeTargetExpectError( final File projectFile,
                                             final String targetName,
                                             final String message )
    {
        executeTargetExpectError( projectFile, targetName, new String[]{message} );
    }

    /**
     * Executes a target in a project, and asserts that it fails with the
     * given error messages.
     */
    protected void executeTargetExpectError( final File projectFile,
                                             final String targetName,
                                             final String[] messages )
    {
        try
        {
            executeTarget( projectFile, targetName, null );
            fail( "target execution did not fail" );
        }
        catch( Exception e )
        {
            assertSameMessage( messages, e );
        }
    }

    /**
     * Executes a target in a project, and asserts that it does not fail.
     */
    protected void executeTarget( final File projectFile, final String targetName )
        throws Exception
    {
        executeTarget( projectFile, targetName, null );
    }

    /**
     * Executes a target in a project, and asserts that it does not fail.
     */
    protected void executeTarget( final File projectFile,
                                  final String targetName,
                                  final ProjectListener listener )
        throws Exception
    {
        // Create the project and workspace
        final Embeddor embeddor = getEmbeddor();
        final Project project = embeddor.createProject( projectFile.getAbsolutePath(), null, null );
        final Workspace workspace = embeddor.createWorkspace( new Parameters() );

        // Add a listener to make sure all is good
        final TrackingProjectListener tracker = new TrackingProjectListener();
        workspace.addProjectListener( tracker );

        // Add supplied listener
        if( listener != null )
        {
            workspace.addProjectListener( listener );
        }

        // Now execute the target
        workspace.executeProject( project, targetName );

        // Make sure all expected events were delivered
        tracker.assertComplete();
        if( listener instanceof TrackingProjectListener )
        {
            ( (TrackingProjectListener)listener ).assertComplete();
        }
    }
}
