/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.frontends;

import java.io.File;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.embeddor.Embeddor;
import org.apache.myrmidon.interfaces.model.Project;
import org.apache.myrmidon.interfaces.workspace.Workspace;
import org.apache.myrmidon.listeners.ProjectListener;

/**
 * A utility class, that takes care of launching Myrmidon, and building and
 * executing a project.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class EmbeddedAnt
    extends AbstractLogEnabled
{
    private final static Resources REZ
        = ResourceManager.getPackageResources( EmbeddedAnt.class );

    private static final String DEFAULT_EMBEDDOR_CLASS =
        "org.apache.myrmidon.components.embeddor.DefaultEmbeddor";

    private String m_projectFile = "build.ant";
    private Project m_project;
    private String m_listenerName = "default";
    private ProjectListener m_listener;
    private Parameters m_workspaceProps = new Parameters();
    private Parameters m_builderProps = new Parameters();
    private Parameters m_embeddorProps = new Parameters();
    private ClassLoader m_sharedClassLoader;
    private Embeddor m_embeddor;

    /**
     * Sets the logger to use.
     */
    public void setLogger( final Logger logger )
    {
        enableLogging( logger );
    }

    /**
     * Sets the project file to execute.  Default is 'build.ant'.
     */
    public void setProjectFile( final String projectFile )
    {
        m_projectFile = projectFile;
        m_project = null;
    }

    /**
     * Sets the project to execute.  This method can be used instead of
     * {@link #setProjectFile}, for projects models that are built
     * programmatically.
     */
    public void setProject( final Project project )
    {
        m_projectFile = null;
        m_project = project;
    }

    /**
     * Sets the name of the project listener to use.
     */
    public void setListener( final String listener )
    {
        m_listenerName = listener;
        m_listener = null;
    }

    /**
     * Sets the project listener to use.
     */
    public void setListener( final ProjectListener listener )
    {
        m_listenerName = null;
        m_listener = listener;
    }

    /**
     * Sets a workspace property.  These are inherited by all projects executed
     * by this embeddor.
     */
    public void setWorkspaceProperty( final String name, final Object value )
    {
        // TODO - Make properties Objects, not Strings
        m_workspaceProps.setParameter( name, (String)value );
    }

    /**
     * Sets a project builder property.  These are used by the project builder
     * when it is parsing the project file.
     */
    public void setBuilderProperty( final String name, final Object value )
    {
        // TODO - Make properties Objects, not Strings
        m_builderProps.setParameter( name, (String)value );
    }

    /**
     * Sets a task engine property.  These are used to configure the task engine.
     */
    public void setEmbeddorProperty( final String name, final Object value )
    {
        // TODO - Make properties Objects, not Strings
        m_embeddorProps.setParameter( name, (String)value );
    }

    /**
     * Sets the shared classloader, which is used as the parent classloader
     * for all antlibs.  Default is to use the context classloader.
     */
    public void setSharedClassLoader( final ClassLoader classLoader )
    {
        m_sharedClassLoader = classLoader;
    }

    /**
     * Executes a set of targets in the project.  This method may be called
     * multiple times.
     */
    public void executeTargets( final String[] targets ) throws Exception
    {
        if( m_sharedClassLoader != null )
        {
            Thread.currentThread().setContextClassLoader( m_sharedClassLoader );
        }

        checkHomeDir();

        // Prepare the embeddor, project listener and project model
        final Embeddor embeddor = prepareEmbeddor();
        final ProjectListener listener = prepareListener( embeddor );
        final Project project = prepareProjectModel( embeddor );

        // Create a new workspace
        final Workspace workspace = embeddor.createWorkspace( m_workspaceProps );
        workspace.addProjectListener( listener );

        //execute the project
        executeTargets( workspace, project, targets );
    }

    /**
     * Shuts down the task engine, after the project has been executed.
     */
    public void stop() throws Exception
    {
        try
        {
            if( m_embeddor != null )
            {
                m_embeddor.stop();
                m_embeddor.dispose();
            }
        }
        finally
        {
            m_embeddor = null;
            m_project = null;
            m_listener = null;
        }
    }

    /**
     * Actually do the build.
     */
    private void executeTargets( final Workspace workspace,
                                 final Project project,
                                 final String[] targets )
        throws TaskException
    {
        //if we didn't specify a target, then choose default
        if( targets == null || targets.length == 0 )
        {
            workspace.executeProject( project, project.getDefaultTargetName() );
        }
        else
        {
            for( int i = 0; i < targets.length; i++ )
            {
                workspace.executeProject( project, targets[ i ] );
            }
        }
    }

    /**
     * Make sure myrmidon home directory has been specified, and is a
     * directory.
     */
    private void checkHomeDir() throws Exception
    {
        final String home = m_embeddorProps.getParameter( "myrmidon.home" );
        final File homeDir = ( new File( home ) ).getAbsoluteFile();
        if( !homeDir.isDirectory() )
        {
            final String message = REZ.getString( "home-not-dir.error", homeDir );
            throw new Exception( message );
        }

        if( getLogger().isInfoEnabled() )
        {
            final String message = REZ.getString( "homedir.notice", homeDir );
            getLogger().info( message );
        }
    }

    /**
     * Prepares and returns the embeddor to use.
     */
    private Embeddor prepareEmbeddor()
        throws Exception
    {
        if( m_embeddor == null )
        {
            m_embeddor = createEmbeddor();
            setupLogger( m_embeddor );
            m_embeddor.parameterize( m_embeddorProps );
            m_embeddor.initialize();
            m_embeddor.start();
        }
        return m_embeddor;
    }

    /**
     * Creates the embeddor.
     */
    private Embeddor createEmbeddor()
        throws Exception
    {
        final Class clazz = Class.forName( DEFAULT_EMBEDDOR_CLASS );
        return (Embeddor)clazz.newInstance();
    }

    /**
     * Prepares and returns the project listener to use.
     */
    private ProjectListener prepareListener( final Embeddor embeddor )
        throws Exception
    {
        if( m_listener == null )
        {
            m_listener = embeddor.createListener( m_listenerName );
        }
        return m_listener;
    }

    /**
     * Prepares and returns the project model.
     */
    private Project prepareProjectModel( final Embeddor embeddor ) throws Exception
    {
        if( m_project == null )
        {
            final File buildFile = getProjectFile();
            m_project = embeddor.createProject( buildFile.toString(), null, m_builderProps );
        }
        return m_project;
    }

    /**
     * Locates the project file
     */
    private File getProjectFile() throws Exception
    {
        final File projectFile = ( new File( m_projectFile ) ).getCanonicalFile();
        if( !projectFile.isFile() )
        {
            final String message = REZ.getString( "bad-file.error", projectFile );
            throw new Exception( message );
        }

        if( getLogger().isInfoEnabled() )
        {
            final String message = REZ.getString( "buildfile.notice", projectFile );
            getLogger().info( message );
        }

        return projectFile;
    }
}
