/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.builder;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.util.StringUtil;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.avalon.framework.CascadingException;
import org.apache.avalon.framework.Version;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.SAXConfigurationHandler;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.myrmidon.framework.Condition;
import org.apache.myrmidon.interfaces.builder.ProjectBuilder;
import org.apache.myrmidon.interfaces.model.Project;
import org.apache.myrmidon.interfaces.model.Target;
import org.apache.myrmidon.interfaces.model.TypeLib;
import org.xml.sax.XMLReader;

/**
 * Default implementation to construct project from a build file.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class DefaultProjectBuilder
    extends AbstractLogEnabled
    implements ProjectBuilder
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultProjectBuilder.class );

    private final static Version VERSION = new Version( 2, 0, 0 );

    private final static int PROJECT_REFERENCES = 0;
    private final static int LIBRARY_IMPORTS = 1;
    private final static int IMPLICIT_TASKS = 2;
    private final static int TARGETS = 3;

    /**
     * build a project from file.
     *
     * @param source the source
     * @return the constructed Project
     * @exception IOException if an error occurs
     * @exception Exception if an error occurs
     */
    public Project build( final String source )
        throws Exception
    {
        final File file = new File( source );
        return build( file, new HashMap() );
    }

    private Project build( final File file, final HashMap projects )
        throws Exception
    {
        final URL systemID = file.toURL();
        final Project result = (Project)projects.get( systemID.toString() );
        if( null != result )
        {
            return result;
        }

        final SAXConfigurationHandler handler = new SAXConfigurationHandler();

        process( systemID, handler );

        final Configuration configuration = handler.getConfiguration();

        final DefaultProject project = buildProject( file, configuration );

        projects.put( systemID.toString(), project );

        //build using all top-level attributes
        buildTopLevelProject( project, configuration, projects );

        return project;
    }

    protected void process( final URL systemID,
                            final SAXConfigurationHandler handler )
        throws Exception
    {
        final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        final SAXParser saxParser = saxParserFactory.newSAXParser();
        final XMLReader parser = saxParser.getXMLReader();
        parser.setFeature( "http://xml.org/sax/features/namespace-prefixes", false );
        parser.setFeature( "http://xml.org/sax/features/namespaces", false );
        //parser.setFeature( "http://xml.org/sax/features/validation", false );

        parser.setContentHandler( handler );
        parser.setErrorHandler( handler );
        parser.parse( systemID.toString() );
    }

    /**
     * build project from configuration.
     *
     * @param file the file from which configuration was loaded
     * @param configuration the configuration loaded
     * @return the created Project
     * @exception IOException if an error occurs
     * @exception Exception if an error occurs
     * @exception ConfigurationException if an error occurs
     */
    private DefaultProject buildProject( final File file,
                                         final Configuration configuration )
        throws Exception
    {
        if( !configuration.getName().equals( "project" ) )
        {
            final String message = REZ.getString( "ant.no-project-element.error" );
            throw new Exception( message );
        }

        //get project-level attributes
        final String projectName = configuration.getAttribute( "name",
                                                               FileUtil.removeExtension(file.getName()) );
        final String baseDirectoryName = configuration.getAttribute( "basedir", null );
        final String defaultTarget = configuration.getAttribute( "default", "main" );
        final Version version = getVersion( configuration );

        if( !VERSION.complies( version ) )
        {
            final String message =
                REZ.getString( "ant.bad-version.error", VERSION, version );
            throw new Exception( message );
        }

        //determine base directory for project.  Use the directory containing
        //the build file as the default.
        File baseDirectory = file.getParentFile();
        if( baseDirectoryName != null )
        {
            baseDirectory = new File( baseDirectory, baseDirectoryName );
        }
        baseDirectory = baseDirectory.getAbsoluteFile();

        if( getLogger().isDebugEnabled() )
        {
            final String message = REZ.getString( "ant.project-banner.notice", file, baseDirectory );
            getLogger().debug( message );
        }

        //create project and ...
        final DefaultProject project = new DefaultProject();
        project.setProjectName( projectName );
        project.setDefaultTargetName( defaultTarget );
        project.setBaseDirectory( baseDirectory );

        return project;
    }

    /**
     * Retrieve the version attribute from the specified configuration element.
     * Throw exceptions with meaningful errors if malformed or missing.
     */
    private Version getVersion( final Configuration configuration )
        throws CascadingException
    {
        try
        {
            final String versionString = configuration.getAttribute( "version" );
            return parseVersion( versionString );
        }
        catch( final ConfigurationException ce )
        {
            final String message = REZ.getString( "ant.version-missing.error" );
            throw new CascadingException( message, ce );
        }
    }

    /**
     * Utility function to extract version
     */
    private Version parseVersion( final String versionString )
        throws CascadingException
    {

        try
        {
            return Version.getVersion( versionString );
        }
        catch( final Exception e )
        {
            final String message =
                REZ.getString( "ant.malformed.version", versionString );
            getLogger().warn( message );
            throw new CascadingException( message, e );
        }
    }

    /**
     * Handle all top level elements in configuration.
     *
     * @param project the project
     * @param configuration the Configuration
     * @exception Exception if an error occurs
     */
    private void buildTopLevelProject( final DefaultProject project,
                                       final Configuration configuration,
                                       final HashMap projects )
        throws Exception
    {
        final ArrayList implicitTaskList = new ArrayList();
        final Configuration[] children = configuration.getChildren();

        int state = PROJECT_REFERENCES;

        for( int i = 0; i < children.length; i++ )
        {
            final Configuration element = children[ i ];
            final String name = element.getName();

            if( PROJECT_REFERENCES == state )
            {
                if( name.equals( "projectref" ) )
                {
                    buildProjectRef( project, element, projects );
                    continue;
                }
                else
                {
                    state = LIBRARY_IMPORTS;
                }
            }

            if( LIBRARY_IMPORTS == state )
            {
                if( name.equals( "import" ) )
                {
                    buildTypeLib( project, element );
                    continue;
                }
                else
                {
                    state = IMPLICIT_TASKS;
                }
            }

            if( IMPLICIT_TASKS == state )
            {
                //Check for any implicit tasks here
                if( !name.equals( "target" ) )
                {
                    implicitTaskList.add( element );
                    continue;
                }
                else
                {
                    state = TARGETS;
                }
            }

            if( name.equals( "target" ) )
            {
                buildTarget( project, element );
            }
            else
            {
                final String message =
                    REZ.getString( "ant.unknown-toplevel-element.error", name, element.getLocation() );
                throw new Exception( message );
            }
        }

        final Configuration[] implicitTasks =
            (Configuration[])implicitTaskList.toArray( new Configuration[ 0 ] );

        final Target implicitTarget = new Target( null, implicitTasks, null );
        project.setImplicitTarget( implicitTarget );
    }

    private void buildProjectRef( final DefaultProject project,
                                  final Configuration element,
                                  final HashMap projects )
        throws Exception
    {
        final String name = element.getAttribute( "name", null );
        final String location = element.getAttribute( "location", null );

        if( null == name )
        {
            final String message =
                REZ.getString( "ant.projectref-no-name.error", element.getLocation() );
            throw new Exception( message );
        }

        if( !validName( name ) )
        {
            final String message =
                REZ.getString( "ant.projectref-bad-name.error", element.getLocation() );
            throw new Exception( message );
        }

        if( null == location )
        {
            final String message =
                REZ.getString( "ant.projectref-no-location.error", element.getLocation() );
            throw new Exception( message );
        }

        // Build the URL of the referenced projects
        final File baseDirectory = project.getBaseDirectory();
        final File file = FileUtil.resolveFile( baseDirectory, location );
        final String systemID = file.toURL().toString();

        // Locate the referenced project, building it if necessary
        Project other = (Project)projects.get( systemID );
        if( null == other )
        {
            other = build( file, projects );
        }

        // Add the reference
        project.addProject( name, other );
    }

    private void buildTypeLib( final DefaultProject project,
                               final Configuration element )
        throws Exception
    {
        final String library = element.getAttribute( "library", null );
        final String name = element.getAttribute( "name", null );
        final String type = element.getAttribute( "type", null );

        if( null == library )
        {
            final String message =
                REZ.getString( "ant.import-no-library.error", element.getLocation() );
            throw new Exception( message );
        }

        if( null == name || null == type )
        {
            if( null != name || null != type )
            {
                final String message =
                    REZ.getString( "ant.import-malformed.error", element.getLocation() );
                throw new Exception( message );
            }
        }

        project.addTypeLib( new TypeLib( library, type, name ) );
    }

    /**
     * Build a target from configuration.
     *
     * @param project the project
     * @param target the Configuration
     */
    private void buildTarget( final DefaultProject project, final Configuration target )
        throws Exception
    {
        final String name = target.getAttribute( "name", null );
        final String depends = target.getAttribute( "depends", null );
        final String ifCondition = target.getAttribute( "if", null );
        final String unlessCondition = target.getAttribute( "unless", null );

        if( null == name )
        {
            final String message =
                REZ.getString( "ant.target-noname.error", target.getLocation() );
            throw new Exception( message );
        }

        if( !validName( name ) )
        {
            final String message =
                REZ.getString( "ant.target-bad-name.error", target.getLocation() );
            throw new Exception( message );
        }

        if( getLogger().isDebugEnabled() )
        {
            final String message = REZ.getString( "ant.target-parse.notice", name );
            getLogger().debug( message );
        }

        if( null != ifCondition && null != unlessCondition )
        {
            final String message =
                REZ.getString( "ant.target-bad-logic.error", target.getLocation() );
            throw new Exception( message );
        }

        Condition condition = null;

        if( null != ifCondition )
        {
            if( getLogger().isDebugEnabled() )
            {
                final String message = REZ.getString( "ant.target-if.notice", ifCondition );
                getLogger().debug( message );
            }
            condition = new Condition( true, ifCondition );
        }
        else if( null != unlessCondition )
        {
            if( getLogger().isDebugEnabled() )
            {
                final String message = REZ.getString( "ant.target-unless.notice", unlessCondition );
                getLogger().debug( message );
            }
            condition = new Condition( false, unlessCondition );
        }

        String[] dependencies = null;

        //apply depends attribute
        if( null != depends )
        {
            final String[] elements = StringUtil.split( depends, "," );
            final ArrayList dependsList = new ArrayList();

            for( int i = 0; i < elements.length; i++ )
            {
                final String dependency = elements[ i ].trim();

                if( 0 == dependency.length() )
                {
                    final String message = REZ.getString( "ant.target-bad-dependency.error",
                                                          target.getName(),
                                                          target.getLocation() );
                    throw new Exception( message );
                }

                if( getLogger().isDebugEnabled() )
                {
                    final String message = REZ.getString( "ant.target-dependency.notice", dependency );
                    getLogger().debug( message );
                }

                dependsList.add( dependency );
            }

            dependencies = (String[])dependsList.toArray( new String[ 0 ] );
        }

        final Target defaultTarget =
            new Target( condition, target.getChildren(), dependencies );

        //add target to project
        project.addTarget( name, defaultTarget );
    }

    protected boolean validName( final String name )
    {
        if( -1 != name.indexOf( "->" ) )
        {
            return false;
        }
        else
        {
            return true;
        }
    }
}
