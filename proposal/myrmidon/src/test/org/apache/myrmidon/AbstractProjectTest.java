/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon;

import java.io.File;
import org.apache.myrmidon.frontends.EmbeddedAnt;
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
    public AbstractProjectTest( final String name )
    {
        super( name );
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
        final EmbeddedAnt embeddor = new EmbeddedAnt();
        embeddor.setHomeDirectory( getInstallDirectory() );
        embeddor.enableLogging( getLogger() );
        embeddor.setSharedClassLoader( getClass().getClassLoader() );
        embeddor.setProjectFile( projectFile.getAbsolutePath() );
        embeddor.setProjectListener( null );

        // Add a listener to make sure all is good
        final TrackingProjectListener tracker = new TrackingProjectListener();
        embeddor.addProjectListener( tracker );

        // Add supplied listener
        if( listener != null )
        {
            embeddor.addProjectListener( listener );
        }

        // Now execute the target
        embeddor.executeTargets( new String[] { targetName } );

        embeddor.stop();

        // Make sure all expected events were delivered
        tracker.assertComplete();
        if( listener instanceof TrackingProjectListener )
        {
            ( (TrackingProjectListener)listener ).assertComplete();
        }
    }
}
