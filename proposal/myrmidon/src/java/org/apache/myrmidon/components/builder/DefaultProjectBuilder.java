/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.builder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.avalon.framework.ExceptionUtil;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.SAXConfigurationHandler;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.log.Logger;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.components.model.Condition;
import org.apache.myrmidon.components.model.DefaultProject;
import org.apache.myrmidon.components.model.Import;
import org.apache.myrmidon.components.model.Project;
import org.apache.myrmidon.components.model.Target;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Default implementation to construct project from a build file.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultProjectBuilder
    extends AbstractLoggable
    implements ProjectBuilder
{
    private final static int    PROJECT_REFERENCES  = 0;
    private final static int    LIBRARY_IMPORTS     = 1;
    private final static int    IMPLICIT_TASKS      = 2;
    private final static int    TARGETS             = 3;

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
        final String systemID = file.toURL().toString();
        final Project result = (Project)projects.get( systemID );
        if( null != result )
        {
            return result;
        }

        final SAXConfigurationHandler handler = new SAXConfigurationHandler();

        process( systemID, handler );

        final Configuration configuration = handler.getConfiguration();
        final DefaultProject project = buildProject( file, configuration );

        projects.put( systemID, project );

        //build using all top-level attributes
        buildTopLevelProject( project, configuration, projects );

        return project;
    }

    protected void process( final String systemID, 
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
        parser.parse( systemID );
/*
        // Create a transform factory instance.
        final TransformerFactory factory = TransformerFactory.newInstance();
    
        // Create a transformer for the stylesheet.
        final Transformer transformer = factory.newTransformer( new StreamSource(xslID) );

        final Result result = new SAXResult( handler );

        // Transform the source XML to System.out.
        transformer.transform( new StreamSource(sourceID), result );
*/
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
            throw new Exception( "Project file must be enclosed in project element" );
        }

        //get project-level attributes
        final String baseDirectoryName = configuration.getAttribute( "basedir" );
        final String defaultTarget = configuration.getAttribute( "default" );
        //final String name = configuration.getAttribute( "name" );

        //determine base directory for project
        final File baseDirectory =
            (new File( file.getParentFile(), baseDirectoryName )).getAbsoluteFile();

        getLogger().debug( "Project " + file + " base directory: " + baseDirectory );

        //create project and ...
        final DefaultProject project = new DefaultProject();
        project.setDefaultTargetName( defaultTarget );
        project.setBaseDirectory( baseDirectory );
        //project.setName( name );

        return project;
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
                    buildImport( project, element );
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

            if( name.equals( "target" ) ) buildTarget( project, element );
            else
            {
                throw new Exception( "Unknown top-level element " + name +
                                     " at " + element.getLocation() +
                                     ". Expecting target" );
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
            throw new Exception( "Malformed projectref without a name attribute at " +
                                 element.getLocation() );
        }

        if( !validName( name ) )
        {
            throw new Exception( "Projectref with an invalid name attribute at " +
                                 element.getLocation() );
        }

        if( null == location )
        {
            throw new Exception( "Malformed projectref without a location attribute at " +
                                 element.getLocation() );
        }

        final File baseDirectory = project.getBaseDirectory();

        //TODO: standardize and migrate to Avalon-Excalibur.io
        final File file = new File( baseDirectory, location );

        final String systemID = file.toURL().toString();
        Project other = (Project)projects.get( systemID );

        if( null == other )
        {
            other = build( file, projects );
        }

        project.addProject( name, other );
    }

    private void buildImport( final DefaultProject project,
                              final Configuration element )
        throws Exception
    {
        final String library = element.getAttribute( "library", null );
        final String name = element.getAttribute( "name", null );
        final String type = element.getAttribute( "type", null );

        if( null == library )
        {
            throw new Exception( "Malformed import without a library attribute at " +
                                 element.getLocation() );
        }

        if( null == name || null == type )
        {
            if( null != name || null != type )
            {
                throw new Exception( "Malformed import at " + element.getLocation() +
                                     ". If name or type attribute is specified, both " +
                                     "attributes must be specified." );
            }
        }

        project.addImport( new Import( library, type, name ) );
    }

    /**
     * Build a target from configuration.
     *
     * @param project the project
     * @param task the Configuration
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
            throw new Exception( "Discovered un-named target at " +
                                 target.getLocation() );
        }

        if( !validName( name ) )
        {
            throw new Exception( "Target with an invalid name at " +
                                 target.getLocation() );
        }

        getLogger().debug( "Parsing target: " + name );

        if( null != ifCondition && null != unlessCondition )
        {
            throw new Exception( "Discovered invalid target that has both a if and " +
                                    "unless condition at " + target.getLocation() );
        }

        Condition condition = null;

        if( null != ifCondition )
        {
            getLogger().debug( "Target if condition: " + ifCondition );
            condition = new Condition( true, ifCondition );
        }
        else if( null != unlessCondition )
        {
            getLogger().debug( "Target unless condition: " + unlessCondition );
            condition = new Condition( false, unlessCondition );
        }

        String[] dependencies = null;

        //apply depends attribute
        if( null != depends )
        {
            final String[] elements = ExceptionUtil.splitString( depends, "," );
            final ArrayList dependsList = new ArrayList();

            for( int i = 0; i < elements.length; i++ )
            {
                final String dependency = elements[ i ].trim();

                if( 0 == dependency.length() )
                {
                    throw new Exception( "Discovered empty dependency in target " +
                                         target.getName() + " at " + target.getLocation() );
                }

                getLogger().debug( "Target dependency: " + dependency );
                dependsList.add( dependency );
                //defaultTarget.addDependency( dependency );
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
        if( -1 != name.indexOf( "->" ) ) return false;
        else return true;
    }
}
