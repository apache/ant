/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.excalibur.io.FileUtil;
import org.apache.avalon.excalibur.util.StringUtil;
import org.apache.avalon.framework.Version;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.SAXConfigurationHandler;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.myrmidon.interfaces.builder.ProjectBuilder;
import org.apache.myrmidon.interfaces.builder.ProjectException;
import org.apache.myrmidon.interfaces.model.DefaultNameValidator;
import org.apache.myrmidon.interfaces.model.Dependency;
import org.apache.myrmidon.interfaces.model.Project;
import org.apache.myrmidon.interfaces.model.Target;
import org.apache.myrmidon.interfaces.model.TypeLib;
import org.xml.sax.XMLReader;

/**
 * Default implementation to construct project from a build file.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 *
 * @ant:type type="project-builder" name="ant"
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

    // Use a name validator with the default rules.
    private DefaultNameValidator m_nameValidator = new DefaultNameValidator();

    /**
     * build a project from file.
     *
     * @param source the source
     * @return the constructed Project
     * @exception ProjectException if an error occurs
     */
    public Project build( final String source )
        throws ProjectException
    {
        final File file = new File( source );
        return build( file, new HashMap() );
    }

    private Project build( final File file, final HashMap projects )
        throws ProjectException
    {
        try
        {
            // Check for cached project
            final String systemID = extractURL( file );
            final Project result = (Project)projects.get( systemID );
            if( null != result )
            {
                return result;
            }

            // Parse the project file
            final Configuration configuration = parseProject( systemID );

            // Build the project model and add to cache
            final DefaultProject project = buildProject( file, configuration );
            projects.put( systemID, project );

            // Build using all top-level attributes
            buildTopLevelProject( project, configuration, projects );

            return project;
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "ant.project-build.error", file.getAbsolutePath() );
            throw new ProjectException( message, e );
        }
    }

    /**
     * Parses the project.
     */
    protected Configuration parseProject( final String systemID )
        throws ProjectException
    {
        try
        {
            final SAXConfigurationHandler handler = new SAXConfigurationHandler();
            final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            final SAXParser saxParser = saxParserFactory.newSAXParser();
            final XMLReader parser = saxParser.getXMLReader();
            parser.setFeature( "http://xml.org/sax/features/namespace-prefixes", false );
            parser.setFeature( "http://xml.org/sax/features/namespaces", false );
            //parser.setFeature( "http://xml.org/sax/features/validation", false );

            parser.setContentHandler( handler );
            parser.setErrorHandler( handler );
            parser.parse( systemID );

            return handler.getConfiguration();
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "ant.project-parse.error" );
            throw new ProjectException( message, e );
        }
    }

    /**
     * build project from configuration.
     *
     * @param file the file from which configuration was loaded
     * @param configuration the configuration loaded
     * @return the created Project
     * @exception ProjectException if an error occurs building the project
     */
    private DefaultProject buildProject( final File file,
                                         final Configuration configuration )
        throws ProjectException
    {
        if( !configuration.getName().equals( "project" ) )
        {
            final String message = REZ.getString( "ant.no-project-element.error" );
            throw new ProjectException( message );
        }

        //get project-level attributes
        final String projectName = getProjectName( configuration, file );
        final String baseDirectoryName = configuration.getAttribute( "basedir", null );
        final String defaultTarget = configuration.getAttribute( "default", "main" );
        final Version version = getVersion( configuration );

        if( !VERSION.complies( version ) )
        {
            final String message =
                REZ.getString( "ant.bad-version.error", VERSION, version );
            throw new ProjectException( message );
        }

        //determine base directory for project.  Use the directory containing
        //the build file as the default.
        File baseDirectory = file.getParentFile();
        if( baseDirectoryName != null )
        {
            baseDirectory = FileUtil.resolveFile( baseDirectory, baseDirectoryName );
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
     * Get the project name from the configuration, or create a default name if none
     * was supplied.
     */
    private String getProjectName( final Configuration configuration, final File file )
        throws ProjectException
    {
        String projectName = configuration.getAttribute( "name", null );

        if( projectName == null )
        {
            // Create a name based on the file name.
            String fileNameBase = FileUtil.removeExtension( file.getName() );
            try
            {
                projectName = m_nameValidator.makeValidName( fileNameBase );
            }
            catch( Exception e )
            {
                String message = REZ.getString( "ant.project-create-name.error" );
                throw new ProjectException( message, e );
            }
        }
        else
        {
            // Make sure the supplied name is valid.
            try
            {
                m_nameValidator.validate( projectName );
            }
            catch( Exception e )
            {
                String message = REZ.getString( "ant.project-bad-name.error" );
                throw new ProjectException( message, e );
            }
        }
        return projectName;

    }

    /**
     * Retrieve the version attribute from the specified configuration element.
     * Throw exceptions with meaningful errors if malformed or missing.
     */
    private Version getVersion( final Configuration configuration )
        throws ProjectException
    {
        try
        {
            final String versionString = configuration.getAttribute( "version" );
            return parseVersion( versionString );
        }
        catch( final ConfigurationException ce )
        {
            final String message = REZ.getString( "ant.version-missing.error" );
            throw new ProjectException( message, ce );
        }
    }

    /**
     * Utility function to extract version
     */
    private Version parseVersion( final String versionString )
        throws ProjectException
    {

        try
        {
            return Version.getVersion( versionString );
        }
        catch( final Exception e )
        {
            final String message =
                REZ.getString( "ant.malformed.version", versionString );
            throw new ProjectException( message );
        }
    }

    /**
     * Handle all top level elements in configuration.
     *
     * @param project the project
     * @param configuration the Configuration
     * @exception ProjectException if an error occurs
     */
    private void buildTopLevelProject( final DefaultProject project,
                                       final Configuration configuration,
                                       final HashMap projects )
        throws ProjectException
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
                throw new ProjectException( message );
            }
        }

        final Configuration[] implicitTasks =
            (Configuration[])implicitTaskList.toArray( new Configuration[ 0 ] );

        final Target implicitTarget = new Target( implicitTasks, null );
        project.setImplicitTarget( implicitTarget );
    }

    private void buildProjectRef( final DefaultProject project,
                                  final Configuration element,
                                  final HashMap projects )
        throws ProjectException
    {
        final String name = element.getAttribute( "name", null );
        final String location = element.getAttribute( "location", null );

        if( null == name )
        {
            final String message =
                REZ.getString( "ant.projectref-no-name.error", element.getLocation() );
            throw new ProjectException( message );
        }

        try
        {
            m_nameValidator.validate( name );
        }
        catch( Exception e )
        {
            final String message =
                REZ.getString( "ant.projectref-bad-name.error", element.getLocation() );
            throw new ProjectException( message, e );
        }

        if( null == location )
        {
            final String message =
                REZ.getString( "ant.projectref-no-location.error", element.getLocation() );
            throw new ProjectException( message );
        }

        // Build the URL of the referenced projects
        final File baseDirectory = project.getBaseDirectory();
        final File file = FileUtil.resolveFile( baseDirectory, location );

        // Locate the referenced project, building it if necessary
        final Project other = build( file, projects );

        // Add the reference
        project.addProject( name, other );
    }

    /**
     * Validates a project file name, and builds the canonical URL for it.
     */
    private String extractURL( final File file ) throws ProjectException
    {
        if( ! file.isFile() )
        {
            final String message = REZ.getString( "ant.no-project-file.error" );
            throw new ProjectException( message );
        }

        try
        {
            return file.getCanonicalFile().toURL().toString();
        }
        catch( Exception e )
        {
            final String message = REZ.getString( "ant.project-unexpected.error" );
            throw new ProjectException( message, e );
        }
    }

    private void buildTypeLib( final DefaultProject project,
                               final Configuration element )
        throws ProjectException
    {
        final String library = element.getAttribute( "library", null );
        final String name = element.getAttribute( "name", null );
        final String type = element.getAttribute( "type", null );

        if( null == library )
        {
            final String message =
                REZ.getString( "ant.import-no-library.error", element.getLocation() );
            throw new ProjectException( message );
        }

        if( null == name || null == type )
        {
            if( null != name || null != type )
            {
                final String message =
                    REZ.getString( "ant.import-malformed.error", element.getLocation() );
                throw new ProjectException( message );
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
        throws ProjectException
    {
        final String name = target.getAttribute( "name", null );
        final String depends = target.getAttribute( "depends", null );

        verifyTargetName( name, target );

        if( getLogger().isDebugEnabled() )
        {
            final String message = REZ.getString( "ant.target-parse.notice", name );
            getLogger().debug( message );
        }

        final Dependency[] dependencies = buildDependsList( depends, target );
        final Target defaultTarget = new Target( target.getChildren(), dependencies );

        //add target to project
        project.addTarget( name, defaultTarget );
    }

    private void verifyTargetName( final String name, final Configuration target )
        throws ProjectException
    {
        if( null == name )
        {
            final String message =
                REZ.getString( "ant.target-noname.error", target.getLocation() );
            throw new ProjectException( message );
        }

        try
        {
            m_nameValidator.validate( name );
        }
        catch( Exception e )
        {
            final String message =
                REZ.getString( "ant.target-bad-name.error", target.getLocation() );
            throw new ProjectException( message, e );
        }
    }

    private Dependency[] buildDependsList( final String depends, final Configuration target )
        throws ProjectException
    {
        //apply depends attribute
        if( null == depends )
        {
            return null;
        }

        final String[] elements = StringUtil.split( depends, "," );
        final ArrayList dependsList = new ArrayList();

        for( int i = 0; i < elements.length; i++ )
        {
            final String dependency = elements[ i ].trim();

            if( getLogger().isDebugEnabled() )
            {
                final String message = REZ.getString( "ant.target-dependency.notice", dependency );
                getLogger().debug( message );
            }

            // Split project->target dependencies
            final int sep = dependency.indexOf( "->" );
            final String projectName;
            final String targetName;
            if( sep != -1 )
            {
                projectName = dependency.substring( 0, sep );
                targetName = dependency.substring( sep + 2 );
            }
            else
            {
                projectName = null;
                targetName = dependency;
            }

            if( targetName.length() == 0 || ( projectName != null && projectName.length() == 0 ) )
            {
                final String message = REZ.getString( "ant.target-bad-dependency.error",
                                                      target.getName(),
                                                      target.getLocation() );
                throw new ProjectException( message );
            }

            dependsList.add( new Dependency( projectName, targetName ) );
        }

        return (Dependency[])dependsList.toArray( new Dependency[dependsList.size() ] );
    }
}
