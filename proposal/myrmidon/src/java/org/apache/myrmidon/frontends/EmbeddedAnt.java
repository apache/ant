/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.frontends;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.avalon.excalibur.util.StringUtil;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.activity.Startable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.context.DefaultContext;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
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
 * <p>To use this class, create an instance and configure.  To execute
 * targets in a project, use the {@link #executeTargets} method.  This can
 * be done one or more times.  Finally, call the {@link #stop} method to
 * clean-up.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class EmbeddedAnt
    extends AbstractLogEnabled
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( EmbeddedAnt.class );

    private static final String DEFAULT_EMBEDDOR_CLASS =
        "org.apache.myrmidon.components.embeddor.DefaultEmbeddor";

    private final ArrayList m_listeners = new ArrayList();
    private final Parameters m_builderProps = new Parameters();
    private final Map m_embeddorParameters = new HashMap();
    private final Map m_workspaceProperties = new HashMap();

    private String m_projectFile = "build.ant";
    private Project m_project;
    private String m_listenerName = "default";
    private ClassLoader m_sharedClassLoader;
    private Embeddor m_embeddor;
    private File m_homeDir;
    private String m_projectType;

    /**
     * Sets the Myrmidon home directory.  Default is to use the current
     * directory.
     *
     * @todo Autodetect myrmidon home, rather than using current directory
     *       as the default (which is a dud default).
     */
    public void setHomeDirectory( final File homeDir )
    {
        m_homeDir = homeDir.getAbsoluteFile();
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
     * Sets the project file type.  Ignored if {@link #setProject} is used.
     * Set to null to use the default project type.
     */
    public void setProjectType( final String projectType )
    {
        m_projectType = projectType;
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
     * Sets the name of the project listener to use.  Set to null to disable
     * the project listener.
     */
    public void setProjectListener( final String listener )
    {
        m_listenerName = listener;
    }

    /**
     * Adds a project listener.
     */
    public void addProjectListener( final ProjectListener listener )
    {
        m_listeners.add( listener );
    }

    /**
     * Sets a workspace property.  These are inherited by all projects executed
     * by this embeddor.
     */
    public void setWorkspaceProperty( final String name, final Object value )
    {
        m_workspaceProperties.put( name, value );
    }

    /**
     * Sets a project builder property.  These are used by the project builder
     * when it is parsing the project file.
     */
    public void setBuilderProperty( final String name, final Object value )
    {
        // TODO - Make properties Objects, not Strings
        m_builderProps.setParameter( name, value.toString() );
    }

    /**
     * Sets a task engine property.  These are used to configure the task engine.
     *
     * @todo Make this method actually work with objects...
     */
    public void setEmbeddorProperty( final String name, final Object value )
    {
        m_embeddorParameters.put( name, value.toString() );
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
        Map embeddorParameters = new HashMap( m_embeddorParameters );
        setupPaths( embeddorParameters );

        if( m_sharedClassLoader != null )
        {
            embeddorParameters.put( "myrmidon.shared.classloader", m_sharedClassLoader );
        }

        // Prepare the embeddor, and project model
        final Embeddor embeddor = prepareEmbeddor( embeddorParameters );
        final Project project = prepareProjectModel( embeddor );

        // Create a new workspace
        final Workspace workspace = embeddor.createWorkspace( m_workspaceProperties );
        prepareListeners( embeddor, workspace );

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
                if( m_embeddor instanceof Startable )
                {
                    ( (Startable)m_embeddor ).stop();
                }
                if( m_embeddor instanceof Disposable )
                {
                    ( (Disposable)m_embeddor ).dispose();
                }
            }
        }
        finally
        {
            m_embeddor = null;
            m_project = null;
            m_listeners.clear();
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
     * directory.  Set the paths that the embeddor expects.
     */
    private void setupPaths(  Map parameters ) throws Exception
    {
        if( m_homeDir == null )
        {
            m_homeDir = new File( "." ).getAbsoluteFile();
        }
        checkDirectory( m_homeDir, "home-dir.name" );
        parameters.put( "myrmidon.home", m_homeDir );

        if( getLogger().isInfoEnabled() )
        {
            final String message = REZ.getString( "homedir.notice", m_homeDir );
            getLogger().info( message );
        }

        // Build the lib path
        String path = (String)parameters.get( "myrmidon.lib.path" );
        File[] dirs = buildPath( m_homeDir, path, "lib", "lib-dir.name" );
        parameters.put( "myrmidon.lib.path", dirs );

        // Build the antlib search path
        path = (String)parameters.get( "myrmidon.antlib.path" );
        dirs = buildPath( m_homeDir, path, "ext", "task-lib-dir.name" );
        parameters.put( "myrmidon.antlib.path", dirs );

        // Build the extension search path
        path = (String)parameters.get( "myrmidon.ext.path" );
        dirs = buildPath( m_homeDir, path, "ext", "ext-dir.name" );
        parameters.put( "myrmidon.ext.path", dirs );
    }

    /**
     * Prepares and returns the embeddor to use.
     */
    private Embeddor prepareEmbeddor( final Map parameters )
        throws Exception
    {
        if( m_embeddor == null )
        {
            m_embeddor = createEmbeddor();
            setupLogger( m_embeddor );
            if( m_embeddor instanceof Contextualizable )
            {
                final Context context = new DefaultContext( parameters );
                ( (Contextualizable)m_embeddor ).contextualize( context );
            }
            if( m_embeddor instanceof Initializable )
            {
                ( (Initializable)m_embeddor ).initialize();
            }
            if( m_embeddor instanceof Startable )
            {
                ( (Startable)m_embeddor ).start();
            }
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
    private void prepareListeners( final Embeddor embeddor,
                                   final Workspace workspace )
        throws Exception
    {
        if( m_listenerName != null )
        {
            final ProjectListener listener = embeddor.createListener( m_listenerName );
            workspace.addProjectListener( listener );
        }
        final int count = m_listeners.size();
        for( int i = 0; i < count; i++ )
        {
            final ProjectListener listener = (ProjectListener)m_listeners.get( i );
            workspace.addProjectListener( listener );
        }
    }

    /**
     * Prepares and returns the project model.
     */
    private Project prepareProjectModel( final Embeddor embeddor ) throws Exception
    {
        if( m_project == null )
        {
            final File buildFile = getProjectFile();
            m_project = embeddor.createProject( buildFile.toString(), m_projectType, m_builderProps );
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

    /**
     * Resolve a directory relative to another base directory.
     */
    private File[] buildPath( final File baseDir,
                              final String path,
                              final String defaultPath,
                              final String name )
        throws Exception
    {
        // Build the canonical list of files
        final ArrayList files = new ArrayList();

        // Add the default path
        files.add( FileUtil.resolveFile( baseDir, defaultPath ) );

        // Add the additional path
        if( path != null )
        {
            final String[] split = StringUtil.split( path, File.pathSeparator );
            for( int i = 0; i < split.length; i++ )
            {
                final String s = split[ i ];
                final File file = new File( s ).getAbsoluteFile();
                files.add( file );
            }
        }

        // Check each one
        for( int i = 0; i < files.size(); i++ )
        {
            File file = (File)files.get( i );
            checkDirectory( file, name );
        }

        return (File[])files.toArray( new File[ files.size() ] );
    }

    /**
     * Verify file is a directory else throw an exception.
     */
    private void checkDirectory( final File file, final String name )
        throws Exception
    {
        if( !file.exists() )
        {
            final String nameStr = REZ.getString( name );
            final String message = REZ.getString( "file-no-exist.error", nameStr, file );
            throw new Exception( message );
        }
        else if( !file.isDirectory() )
        {
            final String nameStr = REZ.getString( name );
            final String message = REZ.getString( "file-not-dir.error", nameStr, file );
            throw new Exception( message );
        }
    }
}
